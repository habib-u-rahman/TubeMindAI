"""
Quick test script to verify Gemini API key is working
Run this with: python test_gemini_api.py
"""
import sys
sys.path.insert(0, '.')

from app.config import settings

print("=" * 50)
print("Testing Gemini API Configuration")
print("=" * 50)
print(f"AI_API_KEY present: {bool(settings.AI_API_KEY)}")
print(f"AI_API_KEY length: {len(settings.AI_API_KEY) if settings.AI_API_KEY else 0}")
print(f"AI_API_KEY preview: {settings.AI_API_KEY[:20]}...{settings.AI_API_KEY[-5:] if settings.AI_API_KEY and len(settings.AI_API_KEY) > 25 else ''}")
print()

if not settings.AI_API_KEY:
    print("ERROR: AI_API_KEY is not set!")
    sys.exit(1)

try:
    import google.generativeai as genai
    print("[OK] google.generativeai imported successfully")
    
    genai.configure(api_key=settings.AI_API_KEY)
    print("[OK] Gemini API configured successfully")
    
    # Try to initialize a model
    model = genai.GenerativeModel('gemini-1.5-flash')
    print("[OK] Model initialized successfully")
    
    # Try a simple test generation
    print("\nTesting API with a simple prompt...")
    response = model.generate_content("Say 'Hello, API is working!' in one sentence.")
    
    if response and response.text:
        print(f"[OK] API Response: {response.text}")
        print("\n" + "=" * 50)
        print("SUCCESS: Gemini API is working correctly!")
        print("=" * 50)
    else:
        print("[ERROR] API returned empty response")
        sys.exit(1)
        
except ImportError as e:
    print(f"ERROR: Failed to import google.generativeai: {e}")
    print("Install it with: pip install google-generativeai")
    sys.exit(1)
except Exception as e:
    print(f"ERROR: {str(e)}")
    print(f"Error type: {type(e).__name__}")
    import traceback
    print("\nFull traceback:")
    print(traceback.format_exc())
    sys.exit(1)

