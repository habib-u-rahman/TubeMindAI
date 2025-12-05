"""
YouTube Video Service
Handles YouTube video information extraction using yt-dlp
"""
import re
import json
from typing import Optional, Dict
from app.config import settings
import requests
import yt_dlp


def extract_video_id(url: str) -> Optional[str]:
    """
    Extract YouTube video ID from URL
    Supports various YouTube URL formats
    """
    if not url:
        return None
    
    # Pattern for various YouTube URL formats
    patterns = [
        r'(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})',
        r'youtube\.com\/watch\?.*v=([a-zA-Z0-9_-]{11})',
    ]
    
    for pattern in patterns:
        match = re.search(pattern, url)
        if match:
            return match.group(1)
    
    return None


def get_video_info(video_id: str) -> Optional[Dict]:
    """
    Get video information from YouTube using yt-dlp
    This extracts video metadata and transcript
    """
    if not video_id:
        return None
    
    try:
        video_url = f"https://www.youtube.com/watch?v={video_id}"
        
        # Configure yt-dlp options
        ydl_opts = {
            'quiet': True,
            'no_warnings': True,
            'extract_flat': False,
            'writesubtitles': True,
            'writeautomaticsub': True,  # Get auto-generated subtitles if manual ones don't exist
            'subtitleslangs': ['en', 'en-US', 'en-GB'],  # Try multiple English variants
            'subtitlesformat': 'vtt',
            'skip_download': True,  # We only want metadata and transcript
            'listformats': False,
        }
        
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            # Extract video info
            info = ydl.extract_info(video_url, download=False)
            
            if settings.DEBUG:
                print(f"DEBUG: Video info extracted - Title: {info.get('title', 'N/A')}")
                print(f"DEBUG: Available subtitle languages: {list(info.get('subtitles', {}).keys())}")
                print(f"DEBUG: Available auto-caption languages: {list(info.get('automatic_captions', {}).keys())}")
            
            # Get video details
            title = info.get('title', f'Video {video_id}')
            thumbnail = info.get('thumbnail', f'https://img.youtube.com/vi/{video_id}/maxresdefault.jpg')
            duration = info.get('duration', 0)
            
            # Format duration (seconds to MM:SS or HH:MM:SS)
            duration_str = None
            if duration:
                hours = duration // 3600
                minutes = (duration % 3600) // 60
                seconds = duration % 60
                if hours > 0:
                    duration_str = f"{hours:02d}:{minutes:02d}:{seconds:02d}"
                else:
                    duration_str = f"{minutes:02d}:{seconds:02d}"
            
            # Try to get transcript
            transcript = None
            try:
                # Get automatic subtitles - yt-dlp provides subtitles in different format
                subtitles = info.get('subtitles', {})
                auto_caps = info.get('automatic_captions', {})
                
                if settings.DEBUG:
                    print(f"DEBUG: Subtitles available: {list(subtitles.keys()) if subtitles else 'None'}")
                    print(f"DEBUG: Auto captions available: {list(auto_caps.keys()) if auto_caps else 'None'}")
                
                # Try to get English subtitles (try multiple variants)
                subtitle_url = None
                subtitle_lang = None
                
                # Try different English language codes
                for lang_code in ['en', 'en-US', 'en-GB', 'en-CA', 'en-AU']:
                    if lang_code in subtitles and len(subtitles[lang_code]) > 0:
                        subtitle_url = subtitles[lang_code][0].get('url')
                        subtitle_lang = lang_code
                        if settings.DEBUG:
                            print(f"DEBUG: Found {lang_code} subtitle URL")
                        break
                
                # If no manual subtitles, try auto-captions
                if not subtitle_url:
                    for lang_code in ['en', 'en-US', 'en-GB', 'en-CA', 'en-AU']:
                        if lang_code in auto_caps and len(auto_caps[lang_code]) > 0:
                            subtitle_url = auto_caps[lang_code][0].get('url')
                            subtitle_lang = lang_code
                            if settings.DEBUG:
                                print(f"DEBUG: Found {lang_code} auto-caption URL")
                            break
                
                # If still no English, try any available language
                if not subtitle_url and subtitles:
                    first_lang = list(subtitles.keys())[0]
                    if len(subtitles[first_lang]) > 0:
                        subtitle_url = subtitles[first_lang][0].get('url')
                        subtitle_lang = first_lang
                        if settings.DEBUG:
                            print(f"DEBUG: Using {first_lang} subtitle (English not available)")
                
                if not subtitle_url and auto_caps:
                    first_lang = list(auto_caps.keys())[0]
                    if len(auto_caps[first_lang]) > 0:
                        subtitle_url = auto_caps[first_lang][0].get('url')
                        subtitle_lang = first_lang
                        if settings.DEBUG:
                            print(f"DEBUG: Using {first_lang} auto-caption (English not available)")
                
                if subtitle_url:
                    if settings.DEBUG:
                        print(f"DEBUG: Downloading subtitle from: {subtitle_url[:50]}...")
                    # Download and parse subtitle
                    subtitle_response = requests.get(subtitle_url, timeout=10)
                    if subtitle_response.status_code == 200:
                        # Parse VTT format (simplified)
                        vtt_content = subtitle_response.text
                        transcript = parse_vtt_subtitle(vtt_content)
                        if settings.DEBUG:
                            print(f"DEBUG: Transcript extracted, length: {len(transcript) if transcript else 0}")
                    else:
                        if settings.DEBUG:
                            print(f"DEBUG: Failed to download subtitle, status: {subtitle_response.status_code}")
                else:
                    if settings.DEBUG:
                        print("DEBUG: No subtitle URL found")
            except Exception as e:
                if settings.DEBUG:
                    print(f"DEBUG: Could not extract transcript: {str(e)}")
                    import traceback
                    print(f"DEBUG: Traceback: {traceback.format_exc()}")
            
            return {
                "video_id": video_id,
                "title": title,
                "thumbnail_url": thumbnail,
                "duration": duration_str,
                "transcript": transcript,
                "description": info.get('description', '')
            }
            
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: Error extracting video info with yt-dlp: {str(e)}")
        
        # Fallback to basic info
        return {
            "video_id": video_id,
            "title": f"Video {video_id}",
            "thumbnail_url": f"https://img.youtube.com/vi/{video_id}/maxresdefault.jpg",
            "duration": None,
            "transcript": None
        }


def parse_vtt_subtitle(vtt_content: str) -> Optional[str]:
    """
    Parse VTT subtitle format and extract text
    Converts VTT timestamps and formatting to plain text
    """
    if not vtt_content:
        return None
    
    lines = vtt_content.split('\n')
    transcript_lines = []
    
    for line in lines:
        line = line.strip()
        # Skip empty lines, timestamps, and VTT headers
        if not line or line.startswith('WEBVTT') or '-->' in line or line.startswith('NOTE'):
            continue
        
        # Remove HTML tags if present
        line = re.sub(r'<[^>]+>', '', line)
        
        if line:
            transcript_lines.append(line)
    
    # Join all transcript lines
    transcript = ' '.join(transcript_lines)
    
    # Clean up multiple spaces
    transcript = ' '.join(transcript.split())
    
    return transcript if transcript else None


def get_video_info_with_api(video_id: str) -> Optional[Dict]:
    """
    Get video information using YouTube Data API v3
    Requires YOUTUBE_API_KEY in settings
    """
    if not settings.YOUTUBE_API_KEY:
        return None
    
    try:
        url = f"https://www.googleapis.com/youtube/v3/videos"
        params = {
            "id": video_id,
            "key": settings.YOUTUBE_API_KEY,
            "part": "snippet,contentDetails,statistics"
        }
        
        response = requests.get(url, params=params, timeout=10)
        if response.status_code == 200:
            data = response.json()
            if data.get("items"):
                item = data["items"][0]
                snippet = item.get("snippet", {})
                content_details = item.get("contentDetails", {})
                
                return {
                    "video_id": video_id,
                    "title": snippet.get("title", ""),
                    "description": snippet.get("description", ""),
                    "thumbnail_url": snippet.get("thumbnails", {}).get("high", {}).get("url", ""),
                    "duration": content_details.get("duration", ""),
                    "channel_title": snippet.get("channelTitle", ""),
                    "published_at": snippet.get("publishedAt", "")
                }
    except Exception as e:
        if settings.DEBUG:
            print(f"DEBUG: Error fetching YouTube video info: {str(e)}")
    
    return None

