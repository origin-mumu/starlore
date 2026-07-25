from datetime import datetime
from sqlalchemy import BigInteger, String, Text, Integer, DateTime, Float
from sqlalchemy.orm import Mapped, mapped_column
from app.database import Base

class KnowledgeDocument(Base):
    __tablename__ = "knowledge_documents"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    userId: Mapped[int] = mapped_column("user_id", BigInteger, nullable=False)
    fileName: Mapped[str] = mapped_column("file_name", String(255), nullable=False)
    fileType: Mapped[str] = mapped_column("file_type", String(50), nullable=False)
    fileSize: Mapped[int] = mapped_column("file_size", BigInteger, default=0)
    fileUrl: Mapped[str] = mapped_column("file_url", String(500), default="")
    extractedText: Mapped[str] = mapped_column("extracted_text", Text, nullable=True)
    status: Mapped[str] = mapped_column(String(50), default="indexed")
    chunkCount: Mapped[int] = mapped_column("chunk_count", Integer, default=0)
    createdAt: Mapped[datetime] = mapped_column("created_at", DateTime, default=datetime.utcnow)
    updatedAt: Mapped[datetime] = mapped_column("updated_at", DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

class KnowledgeDocumentChunk(Base):
    __tablename__ = "knowledge_document_chunks"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    documentId: Mapped[int] = mapped_column("document_id", BigInteger, nullable=False)
    chunkIndex: Mapped[int] = mapped_column("chunk_index", Integer, default=0)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    tokenCount: Mapped[int] = mapped_column("token_count", Integer, default=0)
    isEnabled: Mapped[int] = mapped_column("is_enabled", Integer, default=1)
    createdAt: Mapped[datetime] = mapped_column("created_at", DateTime, default=datetime.utcnow)
