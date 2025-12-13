# API Key Setup Instructions

## ⚠️ IMPORTANT: Your Current API Key is Not Working

The current API key in `backend/app/config.py` has been reported as leaked and is not functional.

## How to Get a New Google Gemini API Key

1. **Go to Google AI Studio:**
   - Visit: https://makersuite.google.com/app/apikey
   - Or: https://aistudio.google.com/app/apikey

2. **Sign in with your Google account**

3. **Create a new API key:**
   - Click "Create API Key"
   - Select your Google Cloud project (or create a new one)
   - Copy the generated API key

4. **Update the API Key in the code:**
   - Open `backend/app/config.py`
   - Find the line: `AI_API_KEY: str = "AIzaSyCb0aDJEbHHwZMehwhnNjONX8Aq64MxAVs"`
   - Replace it with your new API key:
     ```python
     AI_API_KEY: str = "YOUR_NEW_API_KEY_HERE"
     ```

5. **Restart your backend server:**
   ```bash
   cd backend
   python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
   ```

## Testing the API Key

After updating, test it with:
```bash
cd backend
python test_notes_simple.py
```

If it works, you should see:
```
[SUCCESS] Notes generated!
```

## Security Note

For production, consider using environment variables or a `.env` file instead of hardcoding the API key.

