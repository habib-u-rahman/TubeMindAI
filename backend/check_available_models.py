"""
Check what Gemini models are available with the current API key
"""
import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from app.config import settings
import google.generativeai as genai

print("Checking available Gemini models...")
print(f"AI_API_KEY present: {bool(settings.AI_API_KEY)}")
print(f"AI_API_KEY preview: {settings.AI_API_KEY[:15]}...{settings.AI_API_KEY[-5:] if len(settings.AI_API_KEY) > 20 else ''}")
print()

if not settings.AI_API_KEY:
    print("ERROR: No API key found")
    sys.exit(1)

try:
    genai.configure(api_key=settings.AI_API_KEY)
    print("API configured successfully")
    print()
    
    # List available models
    print("Fetching available models...")
    models = genai.list_models()
    
    print("\nAvailable models:")
    print("-" * 60)
    for model in models:
        if 'generateContent' in model.supported_generation_methods:
            print(f"Model: {model.name}")
            print(f"  Display Name: {model.display_name}")
            print(f"  Description: {model.description}")
            print()
    
    # Try to find gemini models
    print("\nGemini models that support generateContent:")
    print("-" * 60)
    gemini_models = []
    for model in models:
        if 'generateContent' in model.supported_generation_methods:
            if 'gemini' in model.name.lower():
                gemini_models.append(model.name)
                print(f"  - {model.name}")
    
    if not gemini_models:
        print("  No Gemini models found!")
        print("\nAll available models:")
        for model in models:
            if 'generateContent' in model.supported_generation_methods:
                print(f"  - {model.name}")
    
except Exception as e:
    print(f"ERROR: {str(e)}")
    import traceback
    print("\nFull traceback:")
    print(traceback.format_exc())

