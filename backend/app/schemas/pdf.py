from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime


class PDFUploadResponse(BaseModel):
    message: str
    pdf_id: int
    file_name: str
    file_size: int
    page_count: Optional[int] = None
    status: str = Field(..., description="Status: 'processing', 'completed', 'failed'")


class PDFGenerateResponse(BaseModel):
    message: str
    pdf_id: int
    file_name: str
    summary: Optional[str] = None
    key_points: Optional[str] = None
    bullet_notes: Optional[str] = None
    status: str = Field(..., description="Status: 'processing', 'completed', 'failed'")


class PDFResponse(BaseModel):
    id: int
    file_name: str
    file_size: Optional[int] = None
    page_count: Optional[int] = None
    summary: Optional[str] = None
    key_points: Optional[str] = None
    bullet_notes: Optional[str] = None
    is_saved: bool
    created_at: datetime

    class Config:
        from_attributes = True


class PDFListResponse(BaseModel):
    pdfs: List[PDFResponse]
    total: int


class PDFChatMessageRequest(BaseModel):
    message: str = Field(..., description="User's chat message/question", min_length=1, max_length=2000)


class PDFChatMessageResponse(BaseModel):
    id: int
    message: str
    response: Optional[str] = None
    is_user_message: bool
    created_at: datetime

    class Config:
        from_attributes = True


class PDFChatHistoryResponse(BaseModel):
    messages: List[PDFChatMessageResponse]
    total: int


class PDFChatHistoryItem(BaseModel):
    """PDF chat history item"""
    pdf_id: int
    pdf_name: str
    last_message: Optional[str] = None
    last_message_time: Optional[datetime] = None
    message_count: int

    class Config:
        from_attributes = True


class PDFChatHistoryListResponse(BaseModel):
    """List of PDF chat histories"""
    histories: List[PDFChatHistoryItem]
    total: int

