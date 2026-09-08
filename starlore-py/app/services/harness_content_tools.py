"""云端内容生成与检索工具集。

包含：
1. BaseHarnessTool 基类与 ToolResult
2. KnowledgeSearchTool (知识库检索适配)
3. PPTGeneratorTool (python-pptx 16:9 幻灯片构建并上传 MinIO)
4. DocGeneratorTool (python-docx 结构化文档构建并上传 MinIO)
"""

import asyncio
import io
import json
import logging
import re
import sys
import tempfile
from abc import ABC, abstractmethod
from pathlib import Path
from typing import Any

import yaml
from docx import Document
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor as DocxRGBColor
try:
    from pptx import Presentation
    from pptx.dml.color import RGBColor as PptxRGBColor
    from pptx.enum.text import PP_ALIGN
    from pptx.util import Inches as PptxInches, Pt as PptxPt
    HAS_PPTX = True
except ImportError:
    HAS_PPTX = False
    Presentation = None  # type: ignore
    PptxRGBColor = None  # type: ignore
    PP_ALIGN = None  # type: ignore
    PptxInches = None  # type: ignore
    PptxPt = None  # type: ignore
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession

from app.services import blog_tools, file_service

logger = logging.getLogger(__name__)


class ToolResult(BaseModel):
    """工具执行结果。"""
    success: bool
    label: str               # 纯文字动作名，如: "检索了知识库"
    summary: str             # 动作简述或命中数，如: "命中 3 篇相关文章"
    data: Any | None = None  # 回传给大模型的上下文数据
    artifact: dict | None = None # MinIO 交付物元数据
    citations: list[str] = []   # 引用列表（用于文字缩进展示）


class BaseHarnessTool(ABC):
    """Harness 统一工具基类。"""
    name: str
    description: str
    parameters: dict

    @abstractmethod
    async def execute(self, db: AsyncSession, user_id: int, **kwargs) -> ToolResult:
        """异步执行工具。"""
        pass

    def to_openai_tool(self) -> dict:
        """转换为 OpenAI Function Calling 声明规范。"""
        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": self.description,
                "parameters": self.parameters,
            },
        }


class KnowledgeSearchTool(BaseHarnessTool):
    """知识库检索工具：桥接现有的向量相似度与关键词检索。"""
    name = "search_knowledge"
    description = (
        "检索 Starlore 知识库与文章库，返回相关的技术文章、架构方案、操作指南等真实切片与摘要。"
    )
    parameters = {
        "type": "object",
        "properties": {
            "keyword": {
                "type": "string",
                "description": "检索关键词或语义查询语句，如'微服务治理'、'Docker部署'等",
            },
            "category": {
                "type": "string",
                "description": "可选的文章分类名称筛选",
            },
        },
        "required": ["keyword"],
    }

    async def execute(self, db: AsyncSession, user_id: int, **kwargs) -> ToolResult:
        keyword = kwargs.get("keyword", "").strip()
        category = kwargs.get("category")
        try:
            content = await blog_tools.search_articles_impl(
                db=db, user_id=user_id, keyword=keyword, category=category
            )
            # 提取命中的文章标题用于前端缩进展示
            citations = []
            try:
                raw_items = json.loads(content)
                if isinstance(raw_items, list):
                    for it in raw_items:
                        if isinstance(it, dict) and it.get("title"):
                            citations.append(str(it["title"]))
            except Exception:
                pass

            if not citations:
                for line in content.split("\n"):
                    if line.strip().startswith("- ") or line.strip().startswith("1. "):
                        title_clean = line.strip().lstrip("- 1234567890. ").split("(")[0].strip()
                        if title_clean and len(title_clean) > 2:
                            citations.append(title_clean)

            citations = citations[:5]
            summary = f"命中 {len(citations)} 篇相关文章" if citations else "未命中相关文章"
            return ToolResult(
                success=True,
                label="检索了知识库",
                summary=summary,
                data=content,
                citations=citations,
            )
        except Exception as e:
            logger.exception("KnowledgeSearchTool execute failed: %s", e)
            return ToolResult(
                success=False,
                label="检索知识库异常",
                summary=f"检索异常: {e}",
                data=f"检索知识库遇到错误: {e}",
            )


def _format_size(size_bytes: int) -> str:
    """格式化文件字节大小。"""
    if size_bytes < 1024:
        return f"{size_bytes} B"
    if size_bytes < 1024 * 1024:
        return f"{size_bytes / 1024:.1f} KB"
    return f"{size_bytes / (1024 * 1024):.1f} MB"


OPEN_PPT_SKILL_SCRIPT = Path(r"C:\Users\stone\Downloads\open-ppt-skill-main\skills\open-kimi-ppt\scripts\export_pptx.py")


def _build_pptd_project(
    tmp_path: Path,
    title: str,
    subtitle: str,
    slides_data: list[dict],
    theme_name: str = "blue",
) -> Path:
    """构建符合 open-kimi-ppt 规范的 PPTD 项目结构。"""
    pages_dir = tmp_path / "pages"
    pages_dir.mkdir(parents=True, exist_ok=True)

    # 配色与设计主题规范
    themes = {
        "blue": {
            "primary": "#1A56DB",
            "secondary": "#3B82F6",
            "bg": "#F8FAFC",
            "card_bg": "#FFFFFF",
            "card_border": "#E2E8F0",
            "text": "#1E293B",
            "muted": "#64748B",
        },
        "orange": {
            "primary": "#DE4331",
            "secondary": "#FCC841",
            "bg": "#FCF9F5",
            "card_bg": "#FFFFFF",
            "card_border": "#EFE6DF",
            "text": "#1A1410",
            "muted": "#7A6D63",
        },
        "dark": {
            "primary": "#38BDF8",
            "secondary": "#818CF8",
            "bg": "#0B0F19",
            "card_bg": "#111827",
            "card_border": "#1F2937",
            "text": "#F3F4F6",
            "muted": "#9CA3AF",
        },
    }
    th = themes.get(theme_name, themes["blue"])

    # 1. 组装清单 deck.pptd
    page_files = ["pages/1_cover.page"]
    for idx in range(len(slides_data)):
        page_files.append(f"pages/{idx + 2}_slide.page")

    manifest = {
        "version": "v2",
        "title": title,
        "size": [960, 540],
        "theme": {
            "colors": {
                "primary": th["primary"],
                "secondary": th["secondary"],
                "bg": th["bg"],
                "cardBg": th["card_bg"],
                "cardBorder": th["card_border"],
                "text": th["text"],
                "muted": th["muted"],
                "white": "#FFFFFF",
            },
            "textStyles": {
                "coverTitle": {
                    "fontSize": 36,
                    "color": "$primary",
                    "bold": True,
                    "letterSpacing": 1,
                },
                "coverSub": {
                    "fontSize": 17,
                    "color": "$muted",
                },
                "pageTitle": {
                    "fontSize": 24,
                    "color": "$primary",
                    "bold": True,
                },
                "cardHeader": {
                    "fontSize": 16,
                    "color": "$text",
                    "bold": True,
                },
                "cardBody": {
                    "fontSize": 13,
                    "color": "$muted",
                    "lineHeight": 1.5,
                },
                "bodyText": {
                    "fontSize": 15,
                    "color": "$text",
                    "lineHeight": 1.6,
                },
            },
        },
        "pages": page_files,
    }

    manifest_path = tmp_path / "deck.pptd"
    manifest_path.write_text(yaml.dump(manifest, allow_unicode=True, sort_keys=False), encoding="utf-8")

    # 2. 封面页 1_cover.page
    cover_page = {
        "pageType": "cover",
        "background": {"type": "solid", "color": "$bg"},
        "elements": [
            {
                "elementId": "accent-bar",
                "elementType": "shape",
                "bounds": [70, 150, 48, 4],
                "shapeName": "rect",
                "fill": {"type": "solid", "color": "$primary"},
            },
            {
                "elementId": "cover-title",
                "elementType": "text",
                "bounds": [70, 170, 820, 80],
                "content": {"style": "$coverTitle", "text": title},
            },
            {
                "elementId": "cover-sub",
                "elementType": "text",
                "bounds": [70, 260, 820, 40],
                "content": {"style": "$coverSub", "text": subtitle},
            },
            {
                "elementId": "footer-meta",
                "elementType": "text",
                "bounds": [70, 460, 500, 24],
                "content": {"fontSize": 12, "color": "$muted", "text": "Starlore 智能体交付 · 16:9 现代演示文稿"},
            },
            {
                "elementId": "page-num",
                "elementType": "text",
                "bounds": [800, 460, 90, 24],
                "content": {
                    "fontSize": 12,
                    "color": "$muted",
                    "align": ["right", "middle"],
                    "text": f"01 / {len(page_files):02d}",
                },
            },
        ],
    }
    (pages_dir / "1_cover.page").write_text(yaml.dump(cover_page, allow_unicode=True, sort_keys=False), encoding="utf-8")

    # 3. 正文页
    for idx, slide in enumerate(slides_data):
        page_no = idx + 2
        s_title = slide.get("title", f"第 {idx + 1} 部分")
        cards = slide.get("cards", [])
        bullets = slide.get("bullets", [])

        elements = [
            {
                "elementId": "page-title",
                "elementType": "text",
                "bounds": [60, 42, 840, 40],
                "content": {"style": "$pageTitle", "text": s_title},
            },
            {
                "elementId": "title-divider",
                "elementType": "line",
                "bounds": [60, 90, 840, 1],
                "viewBox": [840, 1],
                "points": "0,0.5 840,0.5",
                "border": {"style": "solid", "width": 1, "color": "$cardBorder"},
            },
            {
                "elementId": "footer-idx",
                "elementType": "text",
                "bounds": [800, 490, 100, 20],
                "content": {
                    "fontSize": 11,
                    "color": "$muted",
                    "align": ["right", "middle"],
                    "text": f"{page_no:02d} / {len(page_files):02d}",
                },
            },
        ]

        if cards:
            count = min(len(cards), 3)
            gap = 20
            total_w = 840
            card_w = int((total_w - (count - 1) * gap) / count)
            for c_i, card in enumerate(cards[:count]):
                left = 60 + c_i * (card_w + gap)
                elements.extend([
                    {
                        "elementId": f"card-bg-{c_i}",
                        "elementType": "shape",
                        "bounds": [left, 120, card_w, 340],
                        "shapeName": "rect",
                        "fill": {"type": "solid", "color": "$cardBg"},
                        "border": {"style": "solid", "width": 1, "color": "$cardBorder"},
                    },
                    {
                        "elementId": f"card-top-stripe-{c_i}",
                        "elementType": "shape",
                        "bounds": [left, 120, card_w, 4],
                        "shapeName": "rect",
                        "fill": {"type": "solid", "color": "$primary"},
                    },
                    {
                        "elementId": f"card-header-{c_i}",
                        "elementType": "text",
                        "bounds": [left + 20, 140, card_w - 40, 32],
                        "content": {"style": "$cardHeader", "text": card.get("header", "")},
                    },
                    {
                        "elementId": f"card-body-{c_i}",
                        "elementType": "text",
                        "bounds": [left + 20, 182, card_w - 40, 250],
                        "content": {"style": "$cardBody", "text": card.get("content", "")},
                    },
                ])
        else:
            elements.append({
                "elementId": "content-card-bg",
                "elementType": "shape",
                "bounds": [60, 115, 840, 355],
                "shapeName": "rect",
                "fill": {"type": "solid", "color": "$cardBg"},
                "border": {"style": "solid", "width": 1, "color": "$cardBorder"},
            })
            b_list = bullets or ["暂无详细内容"]
            for b_i, b_text in enumerate(b_list[:5]):
                y_pos = 140 + b_i * 55
                elements.extend([
                    {
                        "elementId": f"bullet-dot-{b_i}",
                        "elementType": "shape",
                        "bounds": [90, y_pos + 6, 8, 8],
                        "shapeName": "circle",
                        "fill": {"type": "solid", "color": "$primary"},
                    },
                    {
                        "elementId": f"bullet-txt-{b_i}",
                        "elementType": "text",
                        "bounds": [115, y_pos, 750, 45],
                        "content": {"style": "$bodyText", "text": b_text},
                    },
                ])

        page_doc = {
            "pageType": "content",
            "background": {"type": "solid", "color": "$bg"},
            "elements": elements,
        }
        (pages_dir / f"{page_no}_slide.page").write_text(
            yaml.dump(page_doc, allow_unicode=True, sort_keys=False), encoding="utf-8"
        )

    return manifest_path


def _generate_fallback_pptx(title: str, subtitle: str, slides_data: list[dict]) -> bytes:
    """使用 python-pptx 作为兜底生成器。"""
    if not HAS_PPTX or Presentation is None:
        raise RuntimeError("python-pptx 依赖尚未安装，无法生成演示文稿")
    prs = Presentation()
    prs.slide_width = PptxInches(13.333)
    prs.slide_height = PptxInches(7.5)
    blank_slide_layout = prs.slide_layouts[6]

    color_primary = PptxRGBColor(26, 86, 219)
    color_text = PptxRGBColor(30, 41, 59)
    color_muted = PptxRGBColor(100, 116, 139)
    color_bg_card = PptxRGBColor(241, 245, 249)

    # 封面
    cover_slide = prs.slides.add_slide(blank_slide_layout)
    tx_box = cover_slide.shapes.add_textbox(PptxInches(1.5), PptxInches(2.4), PptxInches(10.333), PptxInches(2.5))
    tf = tx_box.text_frame
    tf.word_wrap = True
    p_title = tf.paragraphs[0]
    p_title.text = title
    p_title.font.size = PptxPt(40)
    p_title.font.bold = True
    p_title.font.color.rgb = color_primary

    p_sub = tf.add_paragraph()
    p_sub.text = subtitle
    p_sub.font.size = PptxPt(20)
    p_sub.font.color.rgb = color_muted
    p_sub.space_before = PptxPt(16)

    # 正文
    for slide_item in slides_data:
        slide = prs.slides.add_slide(blank_slide_layout)
        s_title = slide_item.get("title", "Untitled")

        header_box = slide.shapes.add_textbox(PptxInches(1.0), PptxInches(0.8), PptxInches(11.333), PptxInches(0.9))
        h_tf = header_box.text_frame
        h_tf.word_wrap = True
        hp = h_tf.paragraphs[0]
        hp.text = s_title
        hp.font.size = PptxPt(26)
        hp.font.bold = True
        hp.font.color.rgb = color_primary

        cards = slide_item.get("cards", [])
        bullets = slide_item.get("bullets", [])

        if cards:
            card_count = max(1, min(len(cards), 3))
            card_width = (11.333 - (card_count - 1) * 0.4) / card_count
            for idx, card in enumerate(cards[:card_count]):
                left = 1.0 + idx * (card_width + 0.4)
                shape = slide.shapes.add_shape(
                    1, PptxInches(left), PptxInches(2.0), PptxInches(card_width), PptxInches(4.5)
                )
                shape.fill.solid()
                shape.fill.fore_color.rgb = color_bg_card
                shape.line.color.rgb = PptxRGBColor(226, 232, 240)

                c_box = slide.shapes.add_textbox(
                    PptxInches(left + 0.2), PptxInches(2.2), PptxInches(card_width - 0.4), PptxInches(4.1)
                )
                c_tf = c_box.text_frame
                c_tf.word_wrap = True
                cp_head = c_tf.paragraphs[0]
                cp_head.text = card.get("header", "")
                cp_head.font.size = PptxPt(18)
                cp_head.font.bold = True
                cp_head.font.color.rgb = color_text

                cp_body = c_tf.add_paragraph()
                cp_body.text = card.get("content", "")
                cp_body.font.size = PptxPt(14)
                cp_body.font.color.rgb = color_muted
                cp_body.space_before = PptxPt(10)
        else:
            body_box = slide.shapes.add_textbox(PptxInches(1.0), PptxInches(2.0), PptxInches(11.333), PptxInches(4.8))
            b_tf = body_box.text_frame
            b_tf.word_wrap = True
            for b_idx, bullet_text in enumerate(bullets):
                bp = b_tf.paragraphs[0] if b_idx == 0 else b_tf.add_paragraph()
                bp.text = f"•  {bullet_text}"
                bp.font.size = PptxPt(18)
                bp.font.color.rgb = color_text
                bp.space_before = PptxPt(12)

    out_buf = io.BytesIO()
    prs.save(out_buf)
    return out_buf.getvalue()


class PPTGeneratorTool(BaseHarnessTool):
    """PPT 演示文稿生成工具：基于 open-ppt-skill (PPTD/WASM) 驱动生成并自动上传 MinIO。"""
    name = "generate_ppt"
    description = (
        "基于 Open PPT 引擎根据结构化大纲生成现代 16:9 演示文稿（原生支持淡入淡出翻页动画、"
        "卡片并列栅格与要点排版），并自动交付至 MinIO 对象存储，返回直接下载链接。"
    )
    parameters = {
        "type": "object",
        "properties": {
            "title": {
                "type": "string",
                "description": "PPT 主标题，如'云原生微服务治理架构方案'",
            },
            "subtitle": {
                "type": "string",
                "description": "PPT 副标题或汇报人/日期",
            },
            "theme": {
                "type": "string",
                "enum": ["blue", "orange", "dark"],
                "description": "视觉色彩风格：blue (现代商务蓝，默认), orange (星域暖橙), dark (科技极客黑)",
            },
            "slides": {
                "type": "array",
                "description": "幻灯片内容页列表",
                "items": {
                    "type": "object",
                    "properties": {
                        "title": {"type": "string", "description": "单页标题"},
                        "bullets": {
                            "type": "array",
                            "items": {"type": "string"},
                            "description": "要点列表",
                        },
                        "layout": {
                            "type": "string",
                            "enum": ["bullets", "two_columns", "cards", "summary"],
                            "description": "页面版式",
                        },
                        "cards": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "header": {"type": "string"},
                                    "content": {"type": "string"},
                                },
                            },
                            "description": "多卡片并列内容",
                        },
                    },
                    "required": ["title"],
                },
            },
        },
        "required": ["title", "slides"],
    }

    async def execute(self, db: AsyncSession, user_id: int, **kwargs) -> ToolResult:
        title = kwargs.get("title", "演示文稿").strip()
        subtitle = kwargs.get("subtitle", "基于 Starlore 智能体生成").strip()
        slides_data = kwargs.get("slides", [])
        theme = kwargs.get("theme", "blue")

        data_bytes: bytes | None = None
        used_engine = "python-pptx"

        # 1. 优先尝试使用 open-ppt-skill 编译高质量 PPTX
        if OPEN_PPT_SKILL_SCRIPT.exists():
            try:
                with tempfile.TemporaryDirectory(prefix="starlore_pptd_") as tmpdir:
                    tmp_path = Path(tmpdir)
                    manifest_path = _build_pptd_project(
                        tmp_path=tmp_path,
                        title=title,
                        subtitle=subtitle,
                        slides_data=slides_data,
                        theme_name=theme,
                    )
                    out_pptx_path = tmp_path / "presentation.pptx"
                    cmd = [
                        sys.executable,
                        str(OPEN_PPT_SKILL_SCRIPT),
                        str(manifest_path),
                        "-o",
                        str(out_pptx_path),
                        "--force",
                    ]
                    proc = await asyncio.create_subprocess_exec(
                        *cmd,
                        stdout=asyncio.subprocess.PIPE,
                        stderr=asyncio.subprocess.PIPE,
                    )
                    stdout, stderr = await asyncio.wait_for(proc.communicate(), timeout=40)
                    if proc.returncode == 0 and out_pptx_path.exists():
                        data_bytes = out_pptx_path.read_bytes()
                        used_engine = "open-ppt-skill (WASM & Fade Transitions)"
                        logger.info("Successfully exported PPTX via open-ppt-skill: %s bytes", len(data_bytes))
                    else:
                        logger.warning(
                            "open-ppt-skill export returned code %s: stdout=%s stderr=%s",
                            proc.returncode,
                            stdout.decode("utf-8", errors="replace"),
                            stderr.decode("utf-8", errors="replace"),
                        )
            except Exception as ex:
                logger.warning("open-ppt-skill execution failed, falling back to python-pptx: %s", ex)

        # 2. 若 open-ppt-skill 未生成成功，走 python-pptx 兜底
        if data_bytes is None:
            try:
                data_bytes = _generate_fallback_pptx(title, subtitle, slides_data)
                used_engine = "python-pptx (fallback)"
            except Exception as e:
                logger.exception("Fallback PPT generator also failed: %s", e)
                return ToolResult(
                    success=False,
                    label="渲染演示文稿异常",
                    summary=str(e),
                    data=f"PPT 生成失败: {e}",
                )

        try:
            safe_name = re.sub(r'[\\/*?:"<>| ]', '_', title)[:30] or "presentation"
            filename = f"{safe_name}.pptx"

            # 上传 MinIO
            download_url = await file_service.upload_file(
                filename=filename,
                data=data_bytes,
                content_type="application/vnd.openxmlformats-officedocument.presentationml.presentation",
                user_id=user_id,
            )
            size_str = _format_size(len(data_bytes))
            slide_count = len(slides_data) + 1
            artifact = {
                "file_id": filename,
                "name": filename,
                "file_type": "pptx",
                "size_str": size_str,
                "download_url": download_url,
            }
            return ToolResult(
                success=True,
                label=f"渲染了演示文稿（共 {slide_count} 页）",
                summary=f"已采用 {used_engine} 渲染现代 16:9 幻灯片，文件大小 {size_str}",
                data={"download_url": download_url, "filename": filename, "slides_count": slide_count},
                artifact=artifact,
            )
        except Exception as e:
            logger.exception("PPTGeneratorTool post-processing failed: %s", e)
            return ToolResult(
                success=False,
                label="渲染演示文稿异常",
                summary=str(e),
                data=f"生成失败: {e}",
            )


class DocGeneratorTool(BaseHarnessTool):
    """Word 文档生成工具：基于 python-docx 自动排版并上传 MinIO。"""
    name = "generate_document"
    description = (
        "根据结构化章节生成规范的 Word (.docx) 技术文档/分析报告，"
        "包含标题、多级大纲、正文与列表，并自动上传 MinIO 获得下载链接。"
    )
    parameters = {
        "type": "object",
        "properties": {
            "title": {
                "type": "string",
                "description": "文档大标题，如'微服务治理技术演进报告'",
            },
            "sections": {
                "type": "array",
                "description": "文档各章节内容",
                "items": {
                    "type": "object",
                    "properties": {
                        "heading": {"type": "string", "description": "章节标题"},
                        "level": {"type": "integer", "enum": [1, 2, 3], "description": "标题层级"},
                        "paragraphs": {
                            "type": "array",
                            "items": {"type": "string"},
                            "description": "段落正文",
                        },
                        "bullets": {
                            "type": "array",
                            "items": {"type": "string"},
                            "description": "列表要点",
                        },
                    },
                    "required": ["heading"],
                },
            },
        },
        "required": ["title", "sections"],
    }

    async def execute(self, db: AsyncSession, user_id: int, **kwargs) -> ToolResult:
        title = kwargs.get("title", "文档报告").strip()
        sections = kwargs.get("sections", [])

        try:
            doc = Document()
            # 设置页边距
            for s in doc.sections:
                s.top_margin = Inches(1.0)
                s.bottom_margin = Inches(1.0)
                s.left_margin = Inches(1.0)
                s.right_margin = Inches(1.0)

            # 统一中文字体与颜色常量，彻底消除 WPS / Word "缺失字体"及深浅色不一致
            FONT_NAME = "Microsoft YaHei"
            FONT_EAST_ASIA = "微软雅黑"
            COLOR_TITLE = DocxRGBColor(26, 26, 26)       # 主标题：深墨黑 #1A1A1A
            COLOR_H1 = DocxRGBColor(30, 41, 59)          # 一级大纲：稳重藏青深蓝 #1E293B
            COLOR_H2 = DocxRGBColor(51, 65, 85)          # 二级大纲：石墨深灰 #334155
            COLOR_BODY = DocxRGBColor(38, 38, 38)        # 正文与列表：统一深炭黑 #262626，绝不发灰

            def _apply_run_style(run, size_pt: float, bold: bool = False, color: DocxRGBColor = COLOR_BODY):
                run.font.name = FONT_NAME
                run._element.rPr.rFonts.set(qn("w:eastAsia"), FONT_EAST_ASIA)
                run.font.size = Pt(size_pt)
                run.font.bold = bold
                run.font.color.rgb = color

            # 1. 大标题 (22pt 加粗)
            p_title = doc.add_paragraph()
            p_title.paragraph_format.space_before = Pt(4)
            p_title.paragraph_format.space_after = Pt(18)
            p_title.paragraph_format.line_spacing = 1.3
            r_title = p_title.add_run(title)
            _apply_run_style(r_title, size_pt=22, bold=True, color=COLOR_TITLE)

            # 2. 遍历章节
            for sec in sections:
                heading = sec.get("heading", "")
                level = sec.get("level", 1)

                p_head = doc.add_paragraph()
                p_head.paragraph_format.space_before = Pt(14)
                p_head.paragraph_format.space_after = Pt(6)
                p_head.paragraph_format.keep_with_next = True
                r_head = p_head.add_run(heading)

                if level == 1:
                    _apply_run_style(r_head, size_pt=15, bold=True, color=COLOR_H1)
                elif level == 2:
                    _apply_run_style(r_head, size_pt=13, bold=True, color=COLOR_H2)
                else:
                    _apply_run_style(r_head, size_pt=12, bold=True, color=COLOR_H2)

                # 正文段落
                for para in sec.get("paragraphs", []):
                    if not para or not para.strip():
                        continue
                    p = doc.add_paragraph()
                    p.paragraph_format.line_spacing = 1.35
                    p.paragraph_format.space_after = Pt(8)
                    r = p.add_run(para)
                    _apply_run_style(r, size_pt=10.5, bold=False, color=COLOR_BODY)

                # 列表要点
                for b in sec.get("bullets", []):
                    if not b or not b.strip():
                        continue
                    bp = doc.add_paragraph(style="List Bullet")
                    bp.paragraph_format.line_spacing = 1.3
                    bp.paragraph_format.space_after = Pt(4)
                    r_b = bp.add_run(b)
                    _apply_run_style(r_b, size_pt=10.5, bold=False, color=COLOR_BODY)

            out_buf = io.BytesIO()
            doc.save(out_buf)
            data_bytes = out_buf.getvalue()
            safe_name = re.sub(r'[\\/*?:"<>| ]', '_', title)[:30] or "document"
            filename = f"{safe_name}.docx"

            download_url = await file_service.upload_file(
                filename=filename,
                data=data_bytes,
                content_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                user_id=user_id,
            )
            size_str = _format_size(len(data_bytes))
            artifact = {
                "file_id": filename,
                "name": filename,
                "file_type": "docx",
                "size_str": size_str,
                "download_url": download_url,
            }
            return ToolResult(
                success=True,
                label="生成了技术文档",
                summary=f"已排版并生成 {len(sections)} 个章节并上传完成",
                data={"download_url": download_url, "filename": filename, "sections_count": len(sections)},
                artifact=artifact,
            )
        except Exception as e:
            logger.exception("DocGeneratorTool execute failed: %s", e)
            return ToolResult(
                success=False,
                label="生成技术文档异常",
                summary=str(e),
                data=f"生成失败: {e}",
            )


# 工具统一注册实例列表
HARNESS_TOOLS: list[BaseHarnessTool] = [
    KnowledgeSearchTool(),
    PPTGeneratorTool(),
    DocGeneratorTool(),
]

HARNESS_TOOL_MAP: dict[str, BaseHarnessTool] = {tool.name: tool for tool in HARNESS_TOOLS}
