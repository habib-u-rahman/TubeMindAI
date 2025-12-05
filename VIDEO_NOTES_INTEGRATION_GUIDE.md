# Video Notes Generation API Integration Guide

## What Has Been Done

### Backend (FastAPI)
1. ✅ **Database Models Created** (`backend/app/models/video.py`):
   - `Video` model - stores video information and notes
   - `Note` model - stores individual notes
   - `Chat` model - stores chat messages about videos

2. ✅ **API Endpoints Created** (`backend/app/api/video.py`):
   - `POST /api/video/generate` - Generate notes from YouTube URL
   - `GET /api/video/{video_id}` - Get video notes by ID
   - `GET /api/video/youtube/{youtube_video_id}` - Get video by YouTube ID
   - `GET /api/video/` - Get all user's videos
   - `POST /api/video/{video_id}/save` - Save video to history
   - `DELETE /api/video/{video_id}` - Delete video

3. ✅ **Services Created**:
   - `youtube_service.py` - YouTube video information extraction
   - `ai_service.py` - AI-powered note generation (placeholder)

4. ✅ **Schemas Created** (`backend/app/schemas/video.py`):
   - Request/Response models for video operations

### Android App
1. ✅ **API Models Created**:
   - `VideoGenerateRequest.java`
   - `VideoGenerateResponse.java`
   - `VideoResponse.java`

2. ✅ **API Service Updated**:
   - Added video endpoints to `ApiService.java`

3. ⚠️ **Activities - IN PROGRESS**:
   - `HomeActivity.java` - Partially updated (needs completion)
   - `NotesActivity.java` - Needs API integration

## What You Need to Do for Proper Functionality

### 1. **Install Backend Dependencies**
```bash
cd backend
pip install -r requirements.txt
```

### 2. **Run Database Migrations**
The new database tables (videos, notes, chats) need to be created:
```bash
# The tables will be created automatically when you start the server
# Or you can run:
python -c "from app.database import engine, Base; from app.models import *; Base.metadata.create_all(bind=engine)"
```

### 3. **Configure API Keys (IMPORTANT!)**

#### Option A: YouTube Data API v3 (Recommended)
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable "YouTube Data API v3"
4. Create credentials (API Key)
5. Add to `backend/app/config.py` or `.env` file:
   ```python
   YOUTUBE_API_KEY = "your-api-key-here"
   ```

#### Option B: Use yt-dlp for Transcript (Alternative)
1. Install yt-dlp:
   ```bash
   pip install yt-dlp
   ```
2. Update `youtube_service.py` to use yt-dlp for transcript extraction

### 4. **Configure AI Service (IMPORTANT!)**

#### Option A: OpenAI GPT (Recommended)
1. Get API key from [OpenAI](https://platform.openai.com/)
2. Install OpenAI library:
   ```bash
   pip install openai
   ```
3. Add to config:
   ```python
   AI_API_KEY = "your-openai-api-key"
   ```
4. Update `ai_service.py` to use OpenAI API

#### Option B: Other AI Services
- Anthropic Claude
- Google Gemini
- Or use a local LLM

### 5. **Complete Android Integration**
The following still needs to be completed:
- ✅ Update `HomeActivity.generateNotes()` to call API
- ✅ Update `NotesActivity` to load notes from API
- ✅ Add loading indicators
- ✅ Add error handling
- ✅ Update recent videos list from API

### 6. **Start Backend Server**
```bash
cd backend
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Current Status

### ✅ Completed:
- Backend API structure
- Database models
- API endpoints
- Android API models
- API service interface

### ⚠️ Needs Completion:
- Android activity integration (HomeActivity, NotesActivity)
- AI service implementation (currently placeholder)
- YouTube transcript extraction (currently placeholder)
- Error handling and loading states

### 🔧 Needs Configuration:
- YouTube API key
- AI API key
- Database migration

## Next Steps

1. **Complete the Android integration** - I can finish this now
2. **Set up API keys** - You need to do this
3. **Test the integration** - After API keys are configured
4. **Improve AI note generation** - Based on your chosen AI service

Would you like me to:
1. Complete the Android integration now?
2. Help you set up the API keys?
3. Implement a specific AI service integration?

