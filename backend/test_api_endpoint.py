"""
Test the video notes generation API endpoint directly
"""
import requests
import json

# Test configuration
BASE_URL = "http://localhost:8000"
TEST_VIDEO_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"  # Rick Roll - has subtitles

# You'll need to get a valid token first by logging in
# For now, this will show you what error you get
print("Testing Video Notes Generation API")
print("=" * 60)
print(f"Base URL: {BASE_URL}")
print(f"Test Video: {TEST_VIDEO_URL}")
print()

# Test without token first to see the error
print("1. Testing endpoint without token (should get 401):")
try:
    response = requests.post(
        f"{BASE_URL}/api/video/generate",
        json={"video_url": TEST_VIDEO_URL},
        timeout=30
    )
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.text[:500]}")
except Exception as e:
    print(f"Error: {str(e)}")

print()
print("=" * 60)
print("To test with authentication:")
print("1. First login to get a token:")
print("   POST /api/auth/login with email and password")
print("2. Then use that token in Authorization header:")
print("   Authorization: Bearer <token>")
print("3. Call POST /api/video/generate with video_url")

