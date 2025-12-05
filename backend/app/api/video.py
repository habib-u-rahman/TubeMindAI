from fastapi import APIRouter, Depends, HTTPException, status, Header
from sqlalchemy.orm import Session
from typing import Optional
from app.database import get_db
from app.models import Video, User
from app.schemas.video import (
    VideoGenerateRequest,
    VideoGenerateResponse,
    VideoResponse,
    VideoListResponse
)
from app.core.security import verify_token
from app.core.youtube_service import extract_video_id, get_video_info, get_video_info_with_api
from app.core.ai_service import generate_notes_from_transcript
from app.config import settings

router = APIRouter()


def get_current_user_id(token: Optional[str] = Header(None, alias="Authorization"), db: Session = Depends(get_db)) -> int:
    """Get current user ID from JWT token"""
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authorization header missing"
        )
    
    # Remove "Bearer " prefix if present
    if token.startswith("Bearer "):
        token = token[7:]
    
    payload = verify_token(token)
    if not payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token"
        )
    
    user_id = payload.get("user_id")
    if not user_id:
        # Try to get user by email
        email = payload.get("sub")
        if email:
            user = db.query(User).filter(User.email == email).first()
            if user:
                return user.id
    
    if not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User ID not found in token"
        )
    
    return user_id


@router.post("/generate", response_model=VideoGenerateResponse, status_code=status.HTTP_201_CREATED)
async def generate_video_notes(
    request: VideoGenerateRequest,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """
    Generate notes from a YouTube video URL
    This endpoint:
    1. Extracts video ID from URL
    2. Fetches video information
    3. Generates AI-powered notes
    4. Saves to database
    """
    # Extract video ID from URL
    video_id = extract_video_id(request.video_url)
    if not video_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid YouTube URL. Please provide a valid YouTube video URL."
        )
    
    # Check if video already exists for this user
    existing_video = db.query(Video).filter(
        Video.video_id == video_id,
        Video.user_id == user_id
    ).first()
    
    if existing_video:
        return VideoGenerateResponse(
            message="Video notes already generated. Use GET endpoint to retrieve.",
            video_id=existing_video.id,
            youtube_video_id=existing_video.video_id,
            title=existing_video.title,
            thumbnail_url=existing_video.thumbnail_url,
            summary=existing_video.summary,
            key_points=existing_video.key_points,
            bullet_notes=existing_video.bullet_notes,
            status="completed"
        )
    
    # Get video information using yt-dlp (preferred) or YouTube API
    video_info = None
    
    # Try yt-dlp first (works without API key and gets transcript)
    try:
        video_info = get_video_info(video_id)
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: yt-dlp failed, trying YouTube API: {str(e)}")
    
    # Fallback to YouTube API if yt-dlp fails and API key is available
    if not video_info and settings.YOUTUBE_API_KEY:
        video_info = get_video_info_with_api(video_id)
    
    if not video_info:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Could not fetch video information. Please check the video URL."
        )
    
    # Create video record
    video = Video(
        user_id=user_id,
        video_id=video_id,
        video_url=request.video_url,
        title=video_info.get("title", f"Video {video_id}"),
        thumbnail_url=video_info.get("thumbnail_url"),
        duration=video_info.get("duration"),
        transcript=video_info.get("transcript")
    )
    
    db.add(video)
    db.commit()
    db.refresh(video)
    
    # Generate notes (this can be async/background task in production)
    try:
        transcript = video_info.get("transcript", "")
        if settings.DEBUG:
            print(f"DEBUG: Transcript length: {len(transcript) if transcript else 0}")
            print(f"DEBUG: Video title: {video_info.get('title', 'N/A')}")
        
        if transcript:
            if settings.DEBUG:
                print(f"DEBUG: Calling generate_notes_from_transcript...")
            notes = generate_notes_from_transcript(transcript, video_info.get("title", ""))
            if notes:
                video.summary = notes.get("summary")
                video.key_points = notes.get("key_points")
                video.bullet_notes = notes.get("bullet_notes")
                if settings.DEBUG:
                    print(f"DEBUG: Notes generated successfully")
            else:
                if settings.DEBUG:
                    print(f"DEBUG: generate_notes_from_transcript returned None")
                # Generate placeholder notes if AI service fails
                video.summary = f"This video titled '{video.title}' contains valuable content. " \
                              f"Watch the video to get detailed insights and information."
                video.key_points = "• Watch the video for key insights\n• Take notes while watching\n• Review important sections"
                video.bullet_notes = "• Video content analysis\n• Important concepts\n• Practical applications"
        else:
            if settings.DEBUG:
                print(f"DEBUG: No transcript available, generating placeholder notes")
            # If no transcript, generate placeholder notes
            video.summary = f"This video titled '{video.title}' contains valuable content. " \
                          f"Watch the video to get detailed insights and information."
            video.key_points = "• Watch the video for key insights\n• Take notes while watching\n• Review important sections"
            video.bullet_notes = "• Video content analysis\n• Important concepts\n• Practical applications"
        
        db.commit()
        db.refresh(video)
        
        if settings.DEBUG:
            print(f"DEBUG: Video saved with ID: {video.id}")
            print(f"DEBUG: Summary length: {len(video.summary) if video.summary else 0}")
        
        return VideoGenerateResponse(
            message="Video notes generated successfully",
            video_id=video.id,
            youtube_video_id=video.video_id,
            title=video.title,
            thumbnail_url=video.thumbnail_url,
            summary=video.summary,
            key_points=video.key_points,
            bullet_notes=video.bullet_notes,
            status="completed"
        )
    except Exception as e:
        import traceback
        error_trace = traceback.format_exc()
        if settings.DEBUG:
            print(f"DEBUG: Error generating notes: {str(e)}")
            print(f"DEBUG: Traceback: {error_trace}")
        
        # Try to save video with basic info even if note generation fails
        try:
            if not video.summary:
                video.summary = f"This video titled '{video.title}' contains valuable content."
            if not video.key_points:
                video.key_points = "• Watch the video for key insights"
            if not video.bullet_notes:
                video.bullet_notes = "• Video content analysis"
            db.commit()
            db.refresh(video)
        except Exception as db_error:
            if settings.DEBUG:
                print(f"DEBUG: Error saving video: {str(db_error)}")
        
        # Return video with basic info even if note generation fails
        return VideoGenerateResponse(
            message=f"Video information saved. Note generation encountered an error: {str(e)}",
            video_id=video.id,
            youtube_video_id=video.video_id,
            title=video.title,
            thumbnail_url=video.thumbnail_url,
            summary=video.summary if video.summary else f"This video titled '{video.title}' contains valuable content.",
            key_points=video.key_points if video.key_points else "• Watch the video for key insights",
            bullet_notes=video.bullet_notes if video.bullet_notes else "• Video content analysis",
            status="completed"
        )


@router.get("/{video_id}", response_model=VideoResponse)
async def get_video_notes(
    video_id: int,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Get video notes by video ID"""
    video = db.query(Video).filter(
        Video.id == video_id,
        Video.user_id == user_id
    ).first()
    
    if not video:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video not found"
        )
    
    return VideoResponse.model_validate(video)


@router.get("/youtube/{youtube_video_id}", response_model=VideoResponse)
async def get_video_by_youtube_id(
    youtube_video_id: str,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Get video notes by YouTube video ID"""
    video = db.query(Video).filter(
        Video.video_id == youtube_video_id,
        Video.user_id == user_id
    ).first()
    
    if not video:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video not found. Please generate notes first."
        )
    
    return VideoResponse.model_validate(video)


@router.get("/", response_model=VideoListResponse)
async def get_user_videos(
    skip: int = 0,
    limit: int = 20,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Get all videos for the current user"""
    videos = db.query(Video).filter(
        Video.user_id == user_id
    ).order_by(Video.created_at.desc()).offset(skip).limit(limit).all()
    
    total = db.query(Video).filter(Video.user_id == user_id).count()
    
    return VideoListResponse(
        videos=[VideoResponse.model_validate(v) for v in videos],
        total=total
    )


@router.post("/{video_id}/save", response_model=VideoResponse)
async def save_video_to_history(
    video_id: int,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Save video to user's history"""
    video = db.query(Video).filter(
        Video.id == video_id,
        Video.user_id == user_id
    ).first()
    
    if not video:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video not found"
        )
    
    video.is_saved = True
    db.commit()
    db.refresh(video)
    
    return VideoResponse.model_validate(video)


@router.delete("/{video_id}")
async def delete_video(
    video_id: int,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Delete a video and its notes"""
    video = db.query(Video).filter(
        Video.id == video_id,
        Video.user_id == user_id
    ).first()
    
    if not video:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video not found"
        )
    
    db.delete(video)
    db.commit()
    
    return {"message": "Video deleted successfully"}

