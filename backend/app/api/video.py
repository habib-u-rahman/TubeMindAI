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
    ChatHistoryResponse,
    ChatHistoryItem,
    ChatHistoryListResponse
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
    
    # Check if video already exists for this user WITH COMPLETE NOTES
    existing_video = db.query(Video).filter(
        Video.video_id == video_id,
        Video.user_id == user_id
    ).first()
    
    # Only return existing video if it has complete notes (summary, key_points, bullet_notes)
    if existing_video and existing_video.summary and existing_video.key_points and existing_video.bullet_notes:
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
    
    # If existing video has incomplete notes, delete it and regenerate
    if existing_video:
        if settings.DEBUG:
            print(f"DEBUG: Existing video found but notes are incomplete. Deleting and regenerating...")
        db.delete(existing_video)
        db.commit()
    
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
    
    # Generate notes FIRST before creating video record
    # This ensures we only save videos with successfully generated notes
    try:
        transcript = video_info.get("transcript", "")
        if settings.DEBUG:
            print(f"DEBUG: Transcript length: {len(transcript) if transcript else 0}")
            print(f"DEBUG: Video title: {video_info.get('title', 'N/A')}")
        
        if not transcript or not transcript.strip():
            if settings.DEBUG:
                print(f"DEBUG: ERROR - No transcript available for video")
                print(f"DEBUG: Video may not have subtitles/captions enabled")
            
            # Don't create video record - raise error instead
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="This video does not have subtitles or captions available. Please try a different video that has English subtitles enabled."
            )
        
        if settings.DEBUG:
            print(f"DEBUG: Calling generate_notes_from_transcript...")
            print(f"DEBUG: Transcript preview (first 500 chars): {transcript[:500]}")
        
        # Generate notes BEFORE creating video record
        notes = generate_notes_from_transcript(transcript, video_info.get("title", ""))
        
        # Validate notes are complete and not placeholder
        if not notes or not notes.get("summary") or not notes.get("key_points") or not notes.get("bullet_notes"):
            if settings.DEBUG:
                print(f"DEBUG: ERROR - generate_notes_from_transcript returned None or incomplete notes")
                print(f"DEBUG: This usually means:")
                print(f"DEBUG: 1. AI_API_KEY is not set in .env file")
                print(f"DEBUG: 2. Gemini API is failing (check API key validity)")
                print(f"DEBUG: 3. Network issues preventing API calls")
                print(f"DEBUG: 4. API quota exceeded")
            
            # Don't create video record - raise error instead
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to generate notes. Please ensure AI_API_KEY is set correctly in backend configuration. Check backend logs for details."
            )
        
        # Validate notes are not placeholder
        summary = notes.get("summary", "")
        if "contains valuable content" in summary.lower() and "watch the video" in summary.lower():
            if settings.DEBUG:
                print(f"DEBUG: WARNING - Received placeholder notes, this should not happen!")
            # Don't create video record - raise error instead
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="AI service returned invalid notes. Please try again."
            )
        
        # Only create video record AFTER notes are successfully generated
        if settings.DEBUG:
            print(f"DEBUG: Notes generated successfully, creating video record...")
            print(f"DEBUG: Summary preview: {notes.get('summary', '')[:200]}...")
        
        video = Video(
            user_id=user_id,
            video_id=video_id,
            video_url=request.video_url,
            title=video_info.get("title", f"Video {video_id}"),
            thumbnail_url=video_info.get("thumbnail_url"),
            duration=video_info.get("duration"),
            transcript=video_info.get("transcript"),
            summary=notes.get("summary"),
            key_points=notes.get("key_points"),
            bullet_notes=notes.get("bullet_notes")
        )
        
        db.add(video)
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
        # No video record was created, so no cleanup needed
        raise
    except Exception as e:
        import traceback
        error_trace = traceback.format_exc()
        if settings.DEBUG:
            print(f"DEBUG: Error generating notes: {str(e)}")
            print(f"DEBUG: Traceback: {error_trace}")
        
        # No video record was created (we generate notes first), so no cleanup needed
        # Check for specific error types to provide better error messages
        error_message = str(e).lower()
        error_type_str = str(type(e)).lower()
        
        if "resourceexhausted" in error_type_str or "429" in error_message or "quota" in error_message or "exceeded" in error_message:
            detail = "API quota exceeded. The free tier quota has been reached. Please wait a few minutes and try again, or upgrade your Google Gemini API plan. Visit https://ai.google.dev/pricing for more information."
        elif "permissiondenied" in error_type_str or "leaked" in error_message or "permission denied" in error_message:
            detail = "API key is invalid or has been reported as leaked. Please get a new Google Gemini API key from https://makersuite.google.com/app/apikey and update it in backend/app/config.py"
        elif "api key" in error_message or "authentication" in error_message:
            detail = f"API key authentication failed: {str(e)}. Please check your AI_API_KEY in backend configuration."
        else:
            detail = f"Failed to generate notes: {str(e)}. Please check backend logs for details."
        
        # Raise proper error - no video record was created, so nothing to clean up
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=detail
        )


@router.get("/{video_id}", response_model=VideoResponse)
async def get_video_notes(
    video_id: int,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Get video notes by video ID - only returns videos with complete notes"""
    video = db.query(Video).filter(
        Video.id == video_id,
        Video.user_id == user_id
    ).first()
    
    if not video:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video not found"
        )
    
    # Check if video has complete notes
    if not video.summary or not video.key_points or not video.bullet_notes:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Video notes are incomplete. Please regenerate notes for this video."
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
    """Get all videos for the current user - only videos with complete notes"""
    # Only return videos that have complete notes (summary, key_points, bullet_notes)
    videos = db.query(Video).filter(
        Video.user_id == user_id,
        Video.summary.isnot(None),
        Video.key_points.isnot(None),
        Video.bullet_notes.isnot(None)
    ).order_by(Video.created_at.desc()).offset(skip).limit(limit).all()
    
    # Count only videos with complete notes
    total = db.query(Video).filter(
        Video.user_id == user_id,
        Video.summary.isnot(None),
        Video.key_points.isnot(None),
        Video.bullet_notes.isnot(None)
    ).count()
    
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


@router.get("/chat/histories", response_model=ChatHistoryListResponse)
async def get_all_chat_histories(
    skip: int = 0,
    limit: int = 50,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Get all chat histories grouped by video for the current user - shows videos with notes (ready for chat) or existing chats"""
    from sqlalchemy import func, desc, or_
    
    # Get videos that have complete notes (ready for chat) OR have existing chat messages
    # This allows users to see videos they've generated notes for and start chatting
    videos_with_notes = db.query(Video.id).filter(
        Video.user_id == user_id,
        Video.summary.isnot(None),
        Video.key_points.isnot(None),
        Video.bullet_notes.isnot(None)
    ).subquery()
    
    # Get videos with chat messages
    videos_with_chats = db.query(
        Chat.video_id,
        func.max(Chat.created_at).label('last_message_time'),
        func.count(Chat.id).label('message_count')
    ).filter(
        Chat.user_id == user_id,
        Chat.is_user_message == True
    ).group_by(Chat.video_id).subquery()
    
    # Get all videos that have notes OR have chats
    # Use outer join to include videos with notes but no chats yet
    results = db.query(
        Video.id,
        Video.title,
        Video.thumbnail_url,
        Video.video_id,
        Video.created_at,
        videos_with_chats.c.last_message_time,
        videos_with_chats.c.message_count
    ).outerjoin(
        videos_with_chats, Video.id == videos_with_chats.c.video_id
    ).filter(
        Video.user_id == user_id,
        Video.summary.isnot(None),
        Video.key_points.isnot(None),
        Video.bullet_notes.isnot(None)
    ).order_by(
        desc(func.coalesce(videos_with_chats.c.last_message_time, Video.created_at))
    ).offset(skip).limit(limit).all()
    
    # Get last message for each video
    histories = []
    for result in results:
        # Get the last user message first, if not available, get last AI response
        last_user_chat = db.query(Chat).filter(
            Chat.video_id == result.id,
            Chat.user_id == user_id,
            Chat.is_user_message == True
        ).order_by(Chat.created_at.desc()).first()
        
        last_message = None
        # Use last_message_time from chats if available, otherwise use video creation time
        last_message_time = result.last_message_time if result.last_message_time else result.created_at
        
        if last_user_chat:
            last_message = last_user_chat.message
            last_message_time = last_user_chat.created_at
        else:
            # Fallback to last AI response if no user message found
            last_ai_chat = db.query(Chat).filter(
                Chat.video_id == result.id,
                Chat.user_id == user_id,
                Chat.is_user_message == False
            ).order_by(Chat.created_at.desc()).first()
            if last_ai_chat:
                last_message = last_ai_chat.response
                last_message_time = last_ai_chat.created_at
        
        # If no chat messages, set a default message
        if not last_message:
            last_message = "Click to start chatting about this video"
        
        # Use message_count from subquery, or 0 if no chats
        message_count = result.message_count if result.message_count else 0
        
        histories.append(ChatHistoryItem(
            video_id=result.id,
            video_title=result.title,
            video_thumbnail_url=result.thumbnail_url,
            last_message=last_message,
            last_message_time=last_message_time,
            message_count=message_count,
            youtube_video_id=result.video_id
        ))
    
    # Get total count - videos with complete notes (ready for chat)
    total = db.query(Video).filter(
        Video.user_id == user_id,
        Video.summary.isnot(None),
        Video.key_points.isnot(None),
        Video.bullet_notes.isnot(None)
    ).count()
    
    return ChatHistoryListResponse(
        histories=histories,
        total=total
    )


@router.delete("/chat/{chat_id}")
async def delete_chat_message(
    chat_id: int,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Delete a specific chat message"""
    chat = db.query(Chat).filter(
        Chat.id == chat_id,
        Chat.user_id == user_id
    ).first()
    
    if not chat:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Chat message not found"
        )
    
    db.delete(chat)
    db.commit()
    
    return {"message": "Chat message deleted successfully"}


@router.delete("/{video_id}/chat")
async def delete_video_chat_history(
    video_id: int,
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Delete all chat messages for a specific video"""
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
    
    # Delete all chats for this video
    deleted_count = db.query(Chat).filter(
        Chat.video_id == video_id,
        Chat.user_id == user_id
    ).delete()
    
    db.commit()
    
    return {"message": f"Deleted {deleted_count} chat message(s) successfully"}


@router.delete("/chat/all")
async def delete_all_chat_history(
    db: Session = Depends(get_db),
    user_id: int = Depends(get_current_user_id)
):
    """Delete all chat messages for the current user"""
    deleted_count = db.query(Chat).filter(
        Chat.user_id == user_id
    ).delete()
    
    db.commit()
    
    return {"message": f"Deleted {deleted_count} chat message(s) successfully"}

