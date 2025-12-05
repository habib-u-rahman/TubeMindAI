from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, Boolean
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from app.database import Base


class Video(Base):
    __tablename__ = "videos"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    video_id = Column(String(100), nullable=False, index=True)  # YouTube video ID
    video_url = Column(String(500), nullable=False)
    title = Column(String(500), nullable=False)
    thumbnail_url = Column(String(500), nullable=True)
    duration = Column(String(20), nullable=True)  # e.g., "15:30"
    summary = Column(Text, nullable=True)
    key_points = Column(Text, nullable=True)  # JSON string or text
    bullet_notes = Column(Text, nullable=True)  # JSON string or text
    transcript = Column(Text, nullable=True)
    is_saved = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

    # Relationship
    notes = relationship("Note", back_populates="video", cascade="all, delete-orphan")
    chats = relationship("Chat", back_populates="video", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<Video(id={self.id}, video_id={self.video_id}, title={self.title})>"


class Note(Base):
    __tablename__ = "notes"

    id = Column(Integer, primary_key=True, index=True)
    video_id = Column(Integer, ForeignKey("videos.id"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    content = Column(Text, nullable=False)
    note_type = Column(String(50), nullable=False)  # 'summary', 'key_points', 'bullet_notes', 'custom'
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

    # Relationship
    video = relationship("Video", back_populates="notes")

    def __repr__(self):
        return f"<Note(id={self.id}, video_id={self.video_id}, type={self.note_type})>"


class Chat(Base):
    __tablename__ = "chats"

    id = Column(Integer, primary_key=True, index=True)
    video_id = Column(Integer, ForeignKey("videos.id"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    message = Column(Text, nullable=False)
    response = Column(Text, nullable=True)
    is_user_message = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationship
    video = relationship("Video", back_populates="chats")

    def __repr__(self):
        return f"<Chat(id={self.id}, video_id={self.video_id}, is_user={self.is_user_message})>"

