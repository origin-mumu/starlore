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
        return content.decode("utf-8-sig")
    if extension == ".docx":
        from docx import Document

        document = Document(BytesIO(content))
        return "\n".join(p.text for p in document.paragraphs if p.text.strip()).strip()
    if extension == ".pdf":
        from pypdf import PdfReader

        reader = PdfReader(BytesIO(content))
        return "\n".join(page.extract_text() or "" for page in reader.pages).strip()
    raise ValueError(f"不支持的文件格式: {extension.lstrip('.')}")
