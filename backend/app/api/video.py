from fastapi import APIRouter, Depends, HTTPException, status, Header
from sqlalchemy.orm import Session
from typing import Optional
from app.database import get_db
from app.models import Video, User, Chat
from app.schemas.video import (
    VideoGenerateRequest,
    VideoGenerateResponse,
    VideoResponse,
    VideoListResponse,
    ChatMessageRequest,
    ChatMessageResponse,
    ChatHistoryResponse
)
from app.core.security import verify_token
from app.core.youtube_service import extract_video_id, get_video_info, get_video_info_with_api
from app.core.ai_service import generate_notes_from_transcript, generate_chat_response
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
        
        if transcript and transcript.strip():
            if settings.DEBUG:
                print(f"DEBUG: Calling generate_notes_from_transcript...")
                print(f"DEBUG: Transcript preview (first 500 chars): {transcript[:500]}")
            
            notes = generate_notes_from_transcript(transcript, video_info.get("title", ""))
            
            if notes and notes.get("summary") and notes.get("key_points") and notes.get("bullet_notes"):
                # Validate notes are not placeholder
                summary = notes.get("summary", "")
                if "contains valuable content" in summary.lower() and "watch the video" in summary.lower():
                    if settings.DEBUG:
                        print(f"DEBUG: WARNING - Received placeholder notes, this should not happen!")
                    # This shouldn't happen, but if it does, retry or fail
                    raise Exception("AI service returned placeholder notes instead of real notes")
                
                video.summary = notes.get("summary")
                video.key_points = notes.get("key_points")
                video.bullet_notes = notes.get("bullet_notes")
                if settings.DEBUG:
                    print(f"DEBUG: Notes generated successfully")
                    print(f"DEBUG: Summary preview: {video.summary[:200]}...")
            else:
                if settings.DEBUG:
                    print(f"DEBUG: ERROR - generate_notes_from_transcript returned None or incomplete notes")
                    print(f"DEBUG: This usually means:")
                    print(f"DEBUG: 1. AI_API_KEY is not set in .env file")
                    print(f"DEBUG: 2. Gemini API is failing (check API key validity)")
                    print(f"DEBUG: 3. Network issues preventing API calls")
                
                # Don't save placeholder notes - raise error instead
                raise HTTPException(
                    status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                    detail="Failed to generate notes. Please ensure AI_API_KEY is set correctly in backend configuration. Check backend logs for details."
                )
        else:
            if settings.DEBUG:
                print(f"DEBUG: ERROR - No transcript available for video")
                print(f"DEBUG: Video may not have subtitles/captions enabled")
            
            # Don't save placeholder notes - raise error instead
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="This video does not have subtitles or captions available. Please try a different video that has English subtitles enabled."
            )
        
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
    except HTTPException:
        # Re-raise HTTP exceptions (these are intentional errors with proper messages)
        raise
    except Exception as e:
        import traceback
        error_trace = traceback.format_exc()
        if settings.DEBUG:
            print(f"DEBUG: Error generating notes: {str(e)}")
            print(f"DEBUG: Traceback: {error_trace}")
        
        # Delete the video record if note generation failed (cleanup)
        try:
            db.delete(video)
            db.commit()
        except Exception as db_error:
            if settings.DEBUG:
                print(f"DEBUG: Error deleting video record: {str(db_error)}")
        
        # Raise proper error instead of returning placeholder notes
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to generate notes: {str(e)}. Please check backend logs and ensure AI_API_KEY is configured correctly."
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


@router.post("/{video_id}/chat", response_model=ChatMessageResponse, status_code=status.HTTP_201_CREATED)
async def send_chat_message(
    video_id: int,
    request: ChatMessageRequest,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Send a chat message about a video and get AI response"""
    # Verify video exists and belongs to user
    video = db.query(Video).filter(
        Video.id == video_id,
        Video.user_id == user_id
    ).first()
    
    if not video:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video not found"
        )
    
    # Save user message to database
    chat_message = Chat(
        video_id=video_id,
        user_id=user_id,
        message=request.message,
        is_user_message=True
    )
    db.add(chat_message)
    db.commit()
    db.refresh(chat_message)
    
    # Get conversation history for context
    previous_chats = db.query(Chat).filter(
        Chat.video_id == video_id,
        Chat.user_id == user_id
    ).order_by(Chat.created_at.asc()).all()
    
    # Build conversation history
    conversation_history = []
    for chat in previous_chats[-10:]:  # Last 10 messages for context
        if chat.is_user_message:
            conversation_history.append({"role": "user", "content": chat.message})
        else:
            conversation_history.append({"role": "assistant", "content": chat.response or ""})
    
    # Generate AI response
    ai_response = None
    if video.transcript:
        try:
            ai_response = generate_chat_response(
                user_message=request.message,
                video_transcript=video.transcript,
                video_title=video.title,
                conversation_history=conversation_history
            )
        except Exception as e:
            if settings.DEBUG:
                print(f"DEBUG: Error generating chat response: {str(e)}")
                import traceback
                print(f"DEBUG: Traceback: {traceback.format_exc()}")
    
    # If AI response generation failed, provide fallback
    if not ai_response:
        ai_response = "I apologize, but I'm having trouble generating a response right now. Please try again later or check if the video transcript is available."
    
    # Save AI response to database
    chat_message.response = ai_response
    db.commit()
    db.refresh(chat_message)
    
    return ChatMessageResponse.model_validate(chat_message)


@router.get("/{video_id}/chat", response_model=ChatHistoryResponse)
async def get_chat_history(
    video_id: int,
    skip: int = 0,
    limit: int = 50,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Get chat history for a video"""
    # Verify video exists and belongs to user
    video = db.query(Video).filter(
        Video.id == video_id,
        Video.user_id == user_id
    ).first()
    
    if not video:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video not found"
        )
    
    # Get chat messages
    chats = db.query(Chat).filter(
        Chat.video_id == video_id,
        Chat.user_id == user_id
    ).order_by(Chat.created_at.asc()).offset(skip).limit(limit).all()
    
    total = db.query(Chat).filter(
        Chat.video_id == video_id,
        Chat.user_id == user_id
    ).count()
    
    return ChatHistoryResponse(
        messages=[ChatMessageResponse.model_validate(chat) for chat in chats],
        total=total
    )

