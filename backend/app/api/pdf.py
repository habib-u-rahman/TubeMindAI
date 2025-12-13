from fastapi import APIRouter, Depends, HTTPException, status, Header, UploadFile, File
from sqlalchemy.orm import Session
from sqlalchemy import func, desc
from typing import Optional, List
from datetime import datetime, timedelta

from app.database import get_db
from app.models import PDF, PDFChat, User
from app.api.video import get_current_user_id
from app.schemas.pdf import (
    PDFUploadResponse,
    PDFGenerateResponse,
    PDFResponse,
    PDFListResponse,
    PDFChatMessageRequest,
    PDFChatMessageResponse,
    PDFChatHistoryResponse,
    PDFChatHistoryItem,
    PDFChatHistoryListResponse
)
from app.core.pdf_service import extract_text_from_pdf, save_uploaded_pdf, delete_pdf_file
from app.core.ai_service import generate_notes_from_transcript
from app.config import settings

router = APIRouter()


@router.post("/upload", response_model=PDFUploadResponse, status_code=status.HTTP_201_CREATED)
async def upload_pdf(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """
    Upload a PDF file
    """
    # Validate file type
    if not file.filename.endswith('.pdf'):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only PDF files are allowed"
        )
    
    # Read file content
    try:
        file_content = await file.read()
        file_size = len(file_content)
        
        # Check file size (50MB max)
        if file_size > settings.MAX_FILE_SIZE:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"File size exceeds maximum allowed size ({settings.MAX_FILE_SIZE / (1024*1024)}MB)"
            )
        
        # Save file
        file_path = save_uploaded_pdf(file_content, file.filename, user_id)
        if not file_path:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to save PDF file"
            )
        
        # Extract text and page count
        extraction_result = extract_text_from_pdf(file_path)
        page_count = None
        extracted_text = None
        
        if extraction_result:
            extracted_text, page_count = extraction_result
        
        # Create PDF record
        pdf = PDF(
            user_id=user_id,
            file_name=file.filename,
            file_path=file_path,
            file_size=file_size,
            page_count=page_count,
            extracted_text=extracted_text
        )
        
        db.add(pdf)
        db.commit()
        db.refresh(pdf)
        
        return PDFUploadResponse(
            message="PDF uploaded successfully",
            pdf_id=pdf.id,
            file_name=pdf.file_name,
            file_size=pdf.file_size,
            page_count=pdf.page_count,
            status="processing" if extracted_text else "failed"
        )
        
    except HTTPException:
        raise
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: Error uploading PDF: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error uploading PDF: {str(e)}"
        )


@router.post("/{pdf_id}/generate", response_model=PDFGenerateResponse)
async def generate_pdf_notes(
    pdf_id: int,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """
    Generate notes from uploaded PDF
    """
    # Get PDF
    pdf = db.query(PDF).filter(PDF.id == pdf_id, PDF.user_id == user_id).first()
    if not pdf:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="PDF not found"
        )
    
    # Check if notes already exist
    if pdf.summary and pdf.key_points and pdf.bullet_notes:
        return PDFGenerateResponse(
            message="Notes already generated",
            pdf_id=pdf.id,
            file_name=pdf.file_name,
            summary=pdf.summary,
            key_points=pdf.key_points,
            bullet_notes=pdf.bullet_notes,
            status="completed"
        )
    
    # Extract text if not already extracted
    if not pdf.extracted_text:
        extraction_result = extract_text_from_pdf(pdf.file_path)
        if not extraction_result:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to extract text from PDF"
            )
        extracted_text, page_count = extraction_result
        pdf.extracted_text = extracted_text
        if page_count:
            pdf.page_count = page_count
        db.commit()
    
    # Generate notes using AI
    notes = generate_notes_from_transcript(pdf.extracted_text, pdf.file_name)
    
    if not notes:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to generate notes. Please try again."
        )
    
    # Save notes to database
    pdf.summary = notes.get("summary")
    pdf.key_points = notes.get("key_points")
    pdf.bullet_notes = notes.get("bullet_notes")
    
    db.commit()
    db.refresh(pdf)
    
    return PDFGenerateResponse(
        message="Notes generated successfully",
        pdf_id=pdf.id,
        file_name=pdf.file_name,
        summary=pdf.summary,
        key_points=pdf.key_points,
        bullet_notes=pdf.bullet_notes,
        status="completed"
    )


@router.get("/{pdf_id}", response_model=PDFResponse)
async def get_pdf_notes(
    pdf_id: int,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """
    Get PDF notes by ID
    """
    pdf = db.query(PDF).filter(PDF.id == pdf_id, PDF.user_id == user_id).first()
    if not pdf:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="PDF not found"
        )
    
    # Only return PDFs with complete notes
    if not pdf.summary or not pdf.key_points or not pdf.bullet_notes:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="PDF notes not generated yet. Please generate notes first."
        )
    
    return PDFResponse(
        id=pdf.id,
        file_name=pdf.file_name,
        file_size=pdf.file_size,
        page_count=pdf.page_count,
        summary=pdf.summary,
        key_points=pdf.key_points,
        bullet_notes=pdf.bullet_notes,
        is_saved=pdf.is_saved,
        created_at=pdf.created_at
    )


@router.get("/", response_model=PDFListResponse)
async def get_user_pdfs(
    skip: int = 0,
    limit: int = 50,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """
    Get all PDFs for the current user (only with complete notes)
    """
    pdfs = db.query(PDF).filter(
        PDF.user_id == user_id,
        PDF.summary.isnot(None),
        PDF.key_points.isnot(None),
        PDF.bullet_notes.isnot(None)
    ).order_by(desc(PDF.created_at)).offset(skip).limit(limit).all()
    
    total = db.query(func.count(PDF.id)).filter(
        PDF.user_id == user_id,
        PDF.summary.isnot(None),
        PDF.key_points.isnot(None),
        PDF.bullet_notes.isnot(None)
    ).scalar()
    
    pdf_list = [
        PDFResponse(
            id=pdf.id,
            file_name=pdf.file_name,
            file_size=pdf.file_size,
            page_count=pdf.page_count,
            summary=pdf.summary,
            key_points=pdf.key_points,
            bullet_notes=pdf.bullet_notes,
            is_saved=pdf.is_saved,
            created_at=pdf.created_at
        )
        for pdf in pdfs
    ]
    
    return PDFListResponse(pdfs=pdf_list, total=total)


@router.post("/{pdf_id}/chat", response_model=PDFChatMessageResponse, status_code=status.HTTP_201_CREATED)
async def send_pdf_chat_message(
    pdf_id: int,
    request: PDFChatMessageRequest,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """
    Send a chat message about a PDF
    """
    # Get PDF
    pdf = db.query(PDF).filter(PDF.id == pdf_id, PDF.user_id == user_id).first()
    if not pdf:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="PDF not found"
        )
    
    # Check if PDF has notes
    if not pdf.summary or not pdf.key_points or not pdf.bullet_notes:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="PDF notes not generated yet. Please generate notes first."
        )
    
    # Save user message
    user_chat = PDFChat(
        pdf_id=pdf_id,
        user_id=user_id,
        message=request.message,
        is_user_message=True
    )
    db.add(user_chat)
    db.commit()
    db.refresh(user_chat)
    
    # Generate AI response
    from app.core.ai_service import generate_chat_response
    
    context = f"PDF Title: {pdf.file_name}\n\nSummary: {pdf.summary}\n\nKey Points: {pdf.key_points}\n\nNotes: {pdf.bullet_notes}"
    ai_response = generate_chat_response(request.message, context)
    
    # Save AI response
    ai_chat = PDFChat(
        pdf_id=pdf_id,
        user_id=user_id,
        message="",  # Empty for AI messages
        response=ai_response if ai_response else "I apologize, but I couldn't generate a response. Please try again.",
        is_user_message=False
    )
    db.add(ai_chat)
    db.commit()
    db.refresh(ai_chat)
    
    return PDFChatMessageResponse(
        id=user_chat.id,
        message=user_chat.message,
        response=ai_chat.response,
        is_user_message=True,
        created_at=user_chat.created_at
    )


@router.get("/{pdf_id}/chat", response_model=PDFChatHistoryResponse)
async def get_pdf_chat_history(
    pdf_id: int,
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """
    Get chat history for a PDF
    """
    # Verify PDF belongs to user
    pdf = db.query(PDF).filter(PDF.id == pdf_id, PDF.user_id == user_id).first()
    if not pdf:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="PDF not found"
        )
    
    # Get chat messages
    chats = db.query(PDFChat).filter(
        PDFChat.pdf_id == pdf_id,
        PDFChat.user_id == user_id
    ).order_by(PDFChat.created_at).offset(skip).limit(limit).all()
    
    total = db.query(func.count(PDFChat.id)).filter(
        PDFChat.pdf_id == pdf_id,
        PDFChat.user_id == user_id
    ).scalar()
    
    messages = [
        PDFChatMessageResponse(
            id=chat.id,
            message=chat.message if chat.is_user_message else "",
            response=chat.response if not chat.is_user_message else None,
            is_user_message=chat.is_user_message,
            created_at=chat.created_at
        )
        for chat in chats
    ]
    
    return PDFChatHistoryResponse(messages=messages, total=total)


@router.get("/chat/histories", response_model=PDFChatHistoryListResponse)
async def get_all_pdf_chat_histories(
    skip: int = 0,
    limit: int = 50,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """
    Get all PDF chat histories grouped by PDF
    """
    # Get PDFs that have complete notes OR have existing chat messages
    pdfs_with_notes = db.query(PDF.id).filter(
        PDF.user_id == user_id,
        PDF.summary.isnot(None),
        PDF.key_points.isnot(None),
        PDF.bullet_notes.isnot(None)
    ).subquery()
    
    # Get PDFs with chat messages
    pdfs_with_chats = db.query(
        PDFChat.pdf_id,
        func.max(PDFChat.created_at).label('last_message_time'),
        func.count(PDFChat.id).label('message_count')
    ).filter(
        PDFChat.user_id == user_id,
        PDFChat.is_user_message == True
    ).group_by(PDFChat.pdf_id).subquery()
    
    # Get all PDFs that have notes OR have chats
    results = db.query(
        PDF.id,
        PDF.file_name,
        PDF.created_at,
        pdfs_with_chats.c.last_message_time,
        pdfs_with_chats.c.message_count
    ).outerjoin(
        pdfs_with_chats, PDF.id == pdfs_with_chats.c.pdf_id
    ).filter(
        PDF.user_id == user_id,
        PDF.summary.isnot(None),
        PDF.key_points.isnot(None),
        PDF.bullet_notes.isnot(None)
    ).order_by(
        desc(func.coalesce(pdfs_with_chats.c.last_message_time, PDF.created_at))
    ).offset(skip).limit(limit).all()
    
    # Get last message for each PDF
    histories = []
    for result in results:
        last_user_chat = db.query(PDFChat).filter(
            PDFChat.pdf_id == result.id,
            PDFChat.user_id == user_id,
            PDFChat.is_user_message == True
        ).order_by(PDFChat.created_at.desc()).first()
        
        last_message = None
        last_message_time = result.created_at
        
        if last_user_chat:
            last_message = last_user_chat.message
            last_message_time = last_user_chat.created_at
        else:
            last_ai_chat = db.query(PDFChat).filter(
                PDFChat.pdf_id == result.id,
                PDFChat.user_id == user_id,
                PDFChat.is_user_message == False
            ).order_by(PDFChat.created_at.desc()).first()
            if last_ai_chat:
                last_message = last_ai_chat.response
                last_message_time = last_ai_chat.created_at
        
        if not last_message:
            last_message = "Click to start chatting about this PDF"
        
        message_count = result.message_count if result.message_count else 0
        
        histories.append(PDFChatHistoryItem(
            pdf_id=result.id,
            pdf_name=result.file_name,
            last_message=last_message,
            last_message_time=last_message_time,
            message_count=message_count
        ))
    
    total = db.query(func.count(PDF.id)).filter(
        PDF.user_id == user_id,
        PDF.summary.isnot(None),
        PDF.key_points.isnot(None),
        PDF.bullet_notes.isnot(None)
    ).scalar()
    
    return PDFChatHistoryListResponse(
        histories=histories,
        total=total
    )


@router.delete("/{pdf_id}")
async def delete_pdf(
    pdf_id: int,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """
    Delete a PDF and all associated data
    """
    pdf = db.query(PDF).filter(PDF.id == pdf_id, PDF.user_id == user_id).first()
    if not pdf:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="PDF not found"
        )
    
    # Delete file from disk
    if pdf.file_path:
        delete_pdf_file(pdf.file_path)
    
    # Delete PDF (cascade will delete chats)
    db.delete(pdf)
    db.commit()
    
    return {"message": "PDF deleted successfully", "pdf_id": pdf_id}

