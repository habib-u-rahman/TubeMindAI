from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import func, desc
from typing import List, Optional
from datetime import datetime, timedelta

from app.database import get_db
from app.models import User, Video, Chat, PDF, PDFChat
from app.core.security import verify_password, create_access_token
from app.api.video import get_current_user_id
from app.schemas.auth import UserLogin, Token
from app.config import settings

router = APIRouter(prefix="/admin", tags=["Admin"])


def get_current_admin_user_id(
    current_user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db)
) -> int:
    """Verify that the current user is an admin"""
    user = db.query(User).filter(User.id == current_user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found"
        )
    if not user.is_admin:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin access required"
        )
    return current_user_id


@router.post("/login", response_model=Token)
async def admin_login(credentials: UserLogin, db: Session = Depends(get_db)):
    """Admin login endpoint - only allows admin users"""
    # Normalize email
    email_clean = str(credentials.email).strip().lower()
    password = str(credentials.password).strip()
    
    # Find user
    user = db.query(User).filter(User.email == email_clean).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password"
        )
    
    # Verify password
    if not verify_password(password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password"
        )
    
    # Check if user is admin
    if not user.is_admin:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin access required. This account is not an admin."
        )
    
    # Check if user is active
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin account is inactive"
        )
    
    # Create access token
    access_token = create_access_token(
        data={"sub": user.email, "user_id": user.id}
    )
    
    return Token(
        access_token=access_token,
        token_type="bearer",
        user_id=user.id,
        email=user.email,
        name=user.name,
        is_admin=True
    )


@router.get("/dashboard/stats")
async def get_dashboard_stats(
    db: Session = Depends(get_db),
    admin_id: int = Depends(get_current_admin_user_id)
):
    """Get dashboard statistics"""
    # Total users
    total_users = db.query(func.count(User.id)).scalar()
    active_users = db.query(func.count(User.id)).filter(User.is_active == True).scalar()
    verified_users = db.query(func.count(User.id)).filter(User.is_verified == True).scalar()
    new_users_today = db.query(func.count(User.id)).filter(
        func.date(User.created_at) == func.current_date()
    ).scalar()
    
    # Total videos
    total_videos = db.query(func.count(Video.id)).scalar()
    videos_with_notes = db.query(func.count(Video.id)).filter(
        Video.summary.isnot(None),
        Video.key_points.isnot(None),
        Video.bullet_notes.isnot(None)
    ).scalar()
    videos_today = db.query(func.count(Video.id)).filter(
        func.date(Video.created_at) == func.current_date()
    ).scalar()
    
    # Total chats
    total_chats = db.query(func.count(Chat.id)).scalar()
    chats_today = db.query(func.count(Chat.id)).filter(
        func.date(Chat.created_at) == func.current_date()
    ).scalar()
    
    # Total PDFs
    total_pdfs = db.query(func.count(PDF.id)).scalar()
    pdfs_with_notes = db.query(func.count(PDF.id)).filter(
        PDF.summary.isnot(None),
        PDF.key_points.isnot(None),
        PDF.bullet_notes.isnot(None)
    ).scalar()
    pdfs_today = db.query(func.count(PDF.id)).filter(
        func.date(PDF.created_at) == func.current_date()
    ).scalar()
    
    # Total PDF chats
    total_pdf_chats = db.query(func.count(PDFChat.id)).scalar()
    pdf_chats_today = db.query(func.count(PDFChat.id)).filter(
        func.date(PDFChat.created_at) == func.current_date()
    ).scalar()
    
    # Recent activity (last 7 days)
    seven_days_ago = datetime.now() - timedelta(days=7)
    users_last_7_days = db.query(func.count(User.id)).filter(
        User.created_at >= seven_days_ago
    ).scalar()
    videos_last_7_days = db.query(func.count(Video.id)).filter(
        Video.created_at >= seven_days_ago
    ).scalar()
    pdfs_last_7_days = db.query(func.count(PDF.id)).filter(
        PDF.created_at >= seven_days_ago
    ).scalar()
    
    return {
        "users": {
            "total": total_users,
            "active": active_users,
            "verified": verified_users,
            "new_today": new_users_today,
            "new_last_7_days": users_last_7_days
        },
        "videos": {
            "total": total_videos,
            "with_notes": videos_with_notes,
            "new_today": videos_today,
            "new_last_7_days": videos_last_7_days
        },
        "chats": {
            "total": total_chats,
            "new_today": chats_today
        },
        "pdfs": {
            "total": total_pdfs,
            "with_notes": pdfs_with_notes,
            "new_today": pdfs_today,
            "new_last_7_days": pdfs_last_7_days
        },
        "pdf_chats": {
            "total": total_pdf_chats,
            "new_today": pdf_chats_today
        }
    }


@router.get("/users")
async def get_all_users(
    skip: int = 0,
    limit: int = 50,
    search: Optional[str] = None,
    is_active: Optional[bool] = None,
    is_verified: Optional[bool] = None,
    db: Session = Depends(get_db),
    admin_id: int = Depends(get_current_admin_user_id)
):
    """Get all users with filters"""
    query = db.query(User)
    
    # Apply filters
    if search:
        search_term = f"%{search}%"
        query = query.filter(
            (User.name.ilike(search_term)) | (User.email.ilike(search_term))
        )
    
    if is_active is not None:
        query = query.filter(User.is_active == is_active)
    
    if is_verified is not None:
        query = query.filter(User.is_verified == is_verified)
    
    # Get total count
    total = query.count()
    
    # Get users
    users = query.order_by(desc(User.created_at)).offset(skip).limit(limit).all()
    
    # Format response
    users_list = []
    for user in users:
        # Count user's videos and chats
        video_count = db.query(func.count(Video.id)).filter(Video.user_id == user.id).scalar()
        chat_count = db.query(func.count(Chat.id)).filter(Chat.user_id == user.id).scalar()
        
        users_list.append({
            "id": user.id,
            "name": user.name,
            "email": user.email,
            "is_active": user.is_active,
            "is_verified": user.is_verified,
            "is_admin": user.is_admin,
            "created_at": user.created_at.isoformat() if user.created_at else None,
            "video_count": video_count,
            "chat_count": chat_count
        })
    
    return {
        "users": users_list,
        "total": total,
        "skip": skip,
        "limit": limit
    }


@router.put("/users/{user_id}/activate")
async def activate_user(
    user_id: int,
    db: Session = Depends(get_db),
    admin_id: int = Depends(get_current_admin_user_id)
):
    """Activate or deactivate a user"""
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )
    
    # Prevent deactivating yourself
    if user_id == admin_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot deactivate your own account"
        )
    
    user.is_active = not user.is_active
    db.commit()
    db.refresh(user)
    
    return {
        "message": f"User {'activated' if user.is_active else 'deactivated'} successfully",
        "user_id": user.id,
        "is_active": user.is_active
    }


@router.get("/videos")
async def get_all_videos(
    skip: int = 0,
    limit: int = 50,
    search: Optional[str] = None,
    user_id: Optional[int] = None,
    db: Session = Depends(get_db),
    admin_id: int = Depends(get_current_admin_user_id)
):
    """Get all videos with filters"""
    query = db.query(Video)
    
    # Apply filters
    if search:
        search_term = f"%{search}%"
        query = query.filter(Video.title.ilike(search_term))
    
    if user_id:
        query = query.filter(Video.user_id == user_id)
    
    # Get total count
    total = query.count()
    
    # Get videos
    videos = query.order_by(desc(Video.created_at)).offset(skip).limit(limit).all()
    
    # Format response
    videos_list = []
    for video in videos:
        # Get user info
        user = db.query(User).filter(User.id == video.user_id).first()
        # Count chats
        chat_count = db.query(func.count(Chat.id)).filter(Chat.video_id == video.id).scalar()
        
        videos_list.append({
            "id": video.id,
            "video_id": video.video_id,
            "title": video.title,
            "thumbnail_url": video.thumbnail_url,
            "user_id": video.user_id,
            "user_name": user.name if user else "Unknown",
            "user_email": user.email if user else "Unknown",
            "has_notes": video.summary is not None and video.key_points is not None and video.bullet_notes is not None,
            "chat_count": chat_count,
            "created_at": video.created_at.isoformat() if video.created_at else None
        })
    
    return {
        "videos": videos_list,
        "total": total,
        "skip": skip,
        "limit": limit
    }


@router.delete("/videos/{video_id}")
async def delete_video(
    video_id: int,
    db: Session = Depends(get_db),
    admin_id: int = Depends(get_current_admin_user_id)
):
    """Delete a video and all associated data"""
    video = db.query(Video).filter(Video.id == video_id).first()
    if not video:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video not found"
        )
    
    # Delete video (cascade will delete chats and notes)
    db.delete(video)
    db.commit()
    
    return {
        "message": "Video deleted successfully",
        "video_id": video_id
    }


@router.get("/pdfs")
async def get_all_pdfs(
    skip: int = 0,
    limit: int = 50,
    search: Optional[str] = None,
    user_id: Optional[int] = None,
    db: Session = Depends(get_db),
    admin_id: int = Depends(get_current_admin_user_id)
):
    """Get all PDFs with filters"""
    query = db.query(PDF)
    
    # Apply filters
    if search:
        search_term = f"%{search}%"
        query = query.filter(PDF.file_name.ilike(search_term))
    
    if user_id:
        query = query.filter(PDF.user_id == user_id)
    
    # Get total count
    total = query.count()
    
    # Get PDFs
    pdfs = query.order_by(desc(PDF.created_at)).offset(skip).limit(limit).all()
    
    # Format response
    pdfs_list = []
    for pdf in pdfs:
        # Get user info
        user = db.query(User).filter(User.id == pdf.user_id).first()
        # Count chats
        chat_count = db.query(func.count(PDFChat.id)).filter(PDFChat.pdf_id == pdf.id).scalar()
        
        pdfs_list.append({
            "id": pdf.id,
            "file_name": pdf.file_name,
            "file_size": pdf.file_size,
            "page_count": pdf.page_count,
            "user_id": pdf.user_id,
            "user_name": user.name if user else "Unknown",
            "user_email": user.email if user else "Unknown",
            "has_notes": pdf.summary is not None and pdf.key_points is not None and pdf.bullet_notes is not None,
            "chat_count": chat_count,
            "created_at": pdf.created_at.isoformat() if pdf.created_at else None
        })
    
    return {
        "pdfs": pdfs_list,
        "total": total,
        "skip": skip,
        "limit": limit
    }


@router.delete("/pdfs/{pdf_id}")
async def delete_pdf_admin(
    pdf_id: int,
    db: Session = Depends(get_db),
    admin_id: int = Depends(get_current_admin_user_id)
):
    """Delete a PDF and all associated data"""
    pdf = db.query(PDF).filter(PDF.id == pdf_id).first()
    if not pdf:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="PDF not found"
        )
    
    # Delete file from disk
    from app.core.pdf_service import delete_pdf_file
    if pdf.file_path:
        delete_pdf_file(pdf.file_path)
    
    # Delete PDF (cascade will delete chats)
    db.delete(pdf)
    db.commit()
    
    return {
        "message": "PDF deleted successfully",
        "pdf_id": pdf_id
    }

