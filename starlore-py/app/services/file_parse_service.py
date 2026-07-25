"""Text extraction for AI file attachments."""

from io import BytesIO
from pathlib import Path

from fastapi import UploadFile

MAX_FILE_SIZE = 10 * 1024 * 1024
TEXT_EXTENSIONS = {".txt", ".md", ".markdown", ".csv", ".json", ".xml", ".yaml", ".yml"}


async def extract_text(file: UploadFile) -> str:
    filename = file.filename or ""
    if not filename:
        raise ValueError("无法识别文件名")
    content = await file.read(MAX_FILE_SIZE + 1)
    if not content:
        raise ValueError("文件为空")
    if len(content) > MAX_FILE_SIZE:
        raise ValueError("文件过大（最大 10MB）")

    extension = Path(filename).suffix.lower()
    if extension in TEXT_EXTENSIONS:
        return content.decode("utf-8-sig", errors="ignore")

    if extension == ".docx":
        try:
            from docx import Document
            document = Document(BytesIO(content))
            return "\n".join(p.text for p in document.paragraphs if p.text.strip()).strip()
        except Exception as e:
            raise ValueError(f"DOCX 文件解析失败: {e}")

    if extension == ".pdf":
        text = ""
        try:
            from pypdf import PdfReader
            reader = PdfReader(BytesIO(content))
            text = "\n".join(page.extract_text() or "" for page in reader.pages).strip()
        except ImportError:
            try:
                from PyPDF2 import PdfReader
                reader = PdfReader(BytesIO(content))
                text = "\n".join(page.extract_text() or "" for page in reader.pages).strip()
            except ImportError:
                raise ValueError("缺失 PDF 解析依赖库 pypdf，请运行: pip install pypdf")
        except Exception as e:
            raise ValueError(f"PDF 文件解析失败: {e}")

        if not text.strip():
            raise ValueError("PDF 纯文本提取为空（可能是纯图片扫描件，请提供含文字文本的 PDF）")
        return text

    raise ValueError(f"不支持的文件格式: {extension.lstrip('.')}")
