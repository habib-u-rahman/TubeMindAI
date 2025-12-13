from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, Boolean
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from app.database import Base


class PDF(Base):
    __tablename__ = "pdfs"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    file_name = Column(String(500), nullable=False)
    file_path = Column(String(1000), nullable=False)  # Path to stored PDF file
    file_size = Column(Integer, nullable=True)  # File size in bytes
    page_count = Column(Integer, nullable=True)
    summary = Column(Text, nullable=True)
    key_points = Column(Text, nullable=True)
    bullet_notes = Column(Text, nullable=True)
    extracted_text = Column(Text, nullable=True)  # Full extracted text from PDF
    is_saved = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

    # Relationship
    chats = relationship("PDFChat", back_populates="pdf", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<PDF(id={self.id}, file_name={self.file_name})>"


class PDFChat(Base):
    __tablename__ = "pdf_chats"

    id = Column(Integer, primary_key=True, index=True)
    pdf_id = Column(Integer, ForeignKey("pdfs.id"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    message = Column(Text, nullable=False)
    response = Column(Text, nullable=True)
    is_user_message = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationship
    pdf = relationship("PDF", back_populates="chats")

    def __repr__(self):
        return f"<PDFChat(id={self.id}, pdf_id={self.pdf_id}, is_user={self.is_user_message})>"

