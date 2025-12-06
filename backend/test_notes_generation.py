"""
Test script to directly test note generation and see exact errors
Run this with: python test_notes_generation.py
"""
import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from app.config import settings
from app.core.ai_service import generate_notes_from_transcript
from app.core.youtube_service import get_video_info, extract_video_id

print("=" * 60)
print("Testing Video Notes Generation")
print("=" * 60)
print(f"AI_API_KEY present: {bool(settings.AI_API_KEY)}")
print(f"AI_API_KEY length: {len(settings.AI_API_KEY) if settings.AI_API_KEY else 0}")
if settings.AI_API_KEY:
    print(f"AI_API_KEY preview: {settings.AI_API_KEY[:15]}...{settings.AI_API_KEY[-5:]}")
print()

# Test with a sample video
test_video_url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"  # Rick Roll - has subtitles
print(f"Testing with video: {test_video_url}")
print()

try:
    # Extract video ID
    video_id = extract_video_id(test_video_url)
    print(f"Extracted video ID: {video_id}")
    
    # Get video info and transcript
    print("\nStep 1: Getting video info and transcript...")
    video_info = get_video_info(video_id)
    
    if video_info:
        print(f"[OK] Video title: {video_info.get('title', 'N/A')}")
        transcript = video_info.get('transcript', '')
        print(f"[OK] Transcript length: {len(transcript)} characters")
        
        if transcript:
            print(f"[OK] Transcript extracted successfully")
            print()
            
            # Test note generation
            print("Step 2: Generating notes with AI...")
            print("-" * 60)
            # Set encoding for Windows console
            import sys
            import io
            if sys.stdout.encoding != 'utf-8':
                sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
            
            notes = generate_notes_from_transcript(transcript, video_info.get('title', ''))
            
            if notes:
                print("\n[SUCCESS] Notes generated!")
                print("-" * 60)
                print(f"Summary length: {len(notes.get('summary', ''))} chars")
                print(f"Key points length: {len(notes.get('key_points', ''))} chars")
                print(f"Bullet notes length: {len(notes.get('bullet_notes', ''))} chars")
                print()
                print("Summary preview (first 300 chars):")
                summary_preview = notes.get('summary', '')[:300]
                try:
                    print(summary_preview + "...")
                except:
                    print("(Contains special characters)")
                print()
                print("Key points preview (first 200 chars):")
                key_points_preview = notes.get('key_points', '')[:200]
                try:
                    print(key_points_preview + "...")
                except:
                    print("(Contains special characters)")
            else:
                print("\n[ERROR] Notes generation returned None!")
                print("Check the debug logs above for details.")
        else:
            print("[ERROR] No transcript available for this video")
    else:
        print("[ERROR] Could not get video info")
        
except Exception as e:
    print(f"\n[ERROR] Exception occurred: {str(e)}")
    import traceback
    print("\nFull traceback:")
    print(traceback.format_exc())

print("\n" + "=" * 60)

