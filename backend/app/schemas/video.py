from pydantic import BaseModel, Field, HttpUrl
from typing import Optional, List
from datetime import datetime


class VideoGenerateRequest(BaseModel):
    video_url: str = Field(..., description="YouTube video URL", examples=["https://www.youtube.com/watch?v=dQw4w9WgXcQ"])


class VideoGenerateResponse(BaseModel):
    message: str
    video_id: int
    youtube_video_id: str
    title: str
    thumbnail_url: Optional[str] = None
    summary: Optional[str] = None
    key_points: Optional[str] = None
    bullet_notes: Optional[str] = None
    status: str = Field(..., description="Status: 'processing', 'completed', 'failed'")


class VideoResponse(BaseModel):
    id: int
    video_id: str
    video_url: str
    title: str
    thumbnail_url: Optional[str] = None
    duration: Optional[str] = None
    summary: Optional[str] = None
    key_points: Optional[str] = None
    bullet_notes: Optional[str] = None
    is_saved: bool
    created_at: datetime

    class Config:
        from_attributes = True


class VideoListResponse(BaseModel):
    videos: List[VideoResponse]
    total: int


class NoteResponse(BaseModel):
    id: int
    video_id: int
    content: str
    note_type: str
    created_at: datetime

    class Config:
        from_attributes = True


class ChatMessageRequest(BaseModel):
    message: str = Field(..., description="User's chat message/question", min_length=1, max_length=2000)


class ChatMessageResponse(BaseModel):
    id: int
    message: str
    response: Optional[str] = None
    is_user_message: bool
    created_at: datetime

    class Config:
        from_attributes = True


class ChatHistoryResponse(BaseModel):
    messages: List[ChatMessageResponse]
    total: int

