"""
Simple test to check if note generation works
"""
import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

# Set UTF-8 encoding for Windows
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

from app.config import settings
from app.core.ai_service import generate_notes_from_transcript
from app.core.youtube_service import get_video_info, extract_video_id

print("Testing note generation...")
print(f"AI_API_KEY present: {bool(settings.AI_API_KEY)}")
print()

# Test with a simple video
test_video_url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
video_id = extract_video_id(test_video_url)
print(f"Video ID: {video_id}")

video_info = get_video_info(video_id)
if not video_info:
    print("ERROR: Could not get video info")
    sys.exit(1)

transcript = video_info.get('transcript', '')
if not transcript:
    print("ERROR: No transcript available")
    sys.exit(1)

print(f"Transcript length: {len(transcript)} characters")
print("Generating notes...")

notes = generate_notes_from_transcript(transcript, video_info.get('title', ''))

if notes:
    print("\n[SUCCESS] Notes generated!")
    print(f"Summary: {len(notes.get('summary', ''))} chars")
    print(f"Key points: {len(notes.get('key_points', ''))} chars")
    print(f"Bullet notes: {len(notes.get('bullet_notes', ''))} chars")
    print("\nFirst 100 chars of summary:")
    print(notes.get('summary', '')[:100])
else:
    print("\n[ERROR] Notes generation returned None")
    print("Check backend logs for details")
    sys.exit(1)

