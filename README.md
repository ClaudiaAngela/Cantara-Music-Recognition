# Cantara – Multimodal Music Recognition System

Cantara is a robust, client-server music identification platform developed as a Bachelor's Degree thesis. The system bridges the gap between traditional acoustic fingerprinting and human-centric queries by identifying tracks from clean studio recordings, hummed or sung melodies, covers, or noisy radio broadcasts with up to 96% accuracy.

The project features a native **Android client (Kotlin)** and an asynchronous **FastAPI backend (Python)** implementing advanced Digital Signal Processing (DSP) and multi-engine API orchestration.

---

## Architecture Overview

Cantara decouples audio acquisition and user interaction from heavy computational workloads through a distributed client-server model:
```text
[ Android Client (Kotlin) ]
│
│  HTTP POST (Multipart FLAC, 44.1 kHz, 16-bit mono)
▼
[ FastAPI Backend (Python) ]
│
├──► Dual-Path DSP Preprocessing
│      ├── Music Path (Wiener Statistical Filtering)
│      └── Voice Path (High-Pass, Compressor, Noise Gate, Pre-emphasis, HPSS, Low-Pass)
│
├──► Multi-Engine Recognition Pipeline
│      ├── ACRCloud Fingerprint Engine (Original studio playback)
│      ├── ACRCloud Humming Engine (Query-by-Humming / Pitch tracking)
│      ├── ACRCloud Cover Engine (Harmonic structure analysis)
│      └── OpenAI Whisper + Genius API (ASR Lyric transcription & fuzzy text matching)
│
├──► Decision & Ranking Engine (Score fusion, deduplication, confidence gap analysis)
│
└──► Metadata Enrichment (Spotify Web API)
│
▼ JSON Response (Track metadata, artwork, preview link, confidence)
[ Android Client ] ──► UI Display & Persistent Local History
```

## Key Features

* **Multi-Engine Orchestration:** Concurrently queries ACRCloud, Whisper AI, and Genius to mitigate single-point recognition failures across diverse acoustic environments.
* **Dual-Path DSP Preprocessing:**
  * **Music Path:** Applies Wiener filtering to suppress broadband noise while preserving high-frequency acoustic fingerprints.
  * **Voice Path:** Features a 100 Hz high-pass filter, dynamic compression (4:1 ratio), noise gate (-45 dB threshold), pre-emphasis ($y[n] - 0.85 y[n-1]$), Harmonic-Percussive Source Separation (HPSS blended 70/30), and an 8 kHz low-pass filter to maximize vocal formant intelligibility.
* **Fallback Lyric Transcription:** Uses server-hosted OpenAI Whisper to transcribe spoken or sung lyrics into text, querying the Genius API via multi-probe search and RapidFuzz partial string matching.
* **Confidence & Decision Logic:** Deduplicates candidates by normalized title/artist pairs and evaluates confidence gaps ($gap \ge 15\%$) to deliver unambiguous results or top-ranked alternatives.
* **Native Android Client:** Built with Kotlin, Material Design, View Binding, Retrofit/OkHttp for resilient network communication, FFmpegKit for raw PCM-to-FLAC transcoding, Glide for artwork caching, and SharedPreferences with DiffUtil for local search history.

---

## Repository Structure

```text
Cantara-Music-Recognition/
├── frontend/                  # Native Android application (Kotlin)
│   ├── app/                   # Source code, UI layouts, and resources
│   ├── gradle/                # Gradle wrapper configuration files
│   ├── .gitignore             # Android-specific git ignore rules
│   ├── build.gradle.kts       # Project-level Gradle build configuration
│   ├── gradle.properties      # JVM parameters and project settings
│   ├── gradlew                # Gradle wrapper script (Unix/macOS)
│   ├── gradlew.bat            # Gradle wrapper script (Windows)
│   └── settings.gradle.kts    # Module settings and dependency resolution
│
├── backend/                   # Python FastAPI recognition service
│   ├── arhive_dsp/            # Archived signal processing scripts and filters
│   ├── acr_utils.py           # ACRCloud API integration (Fingerprint, Humming, Cover)
│   ├── dsp_utils.py           # Digital Signal Processing (Wiener, HPSS, audio filtering)
│   ├── lyrics_utils.py        # OpenAI Whisper speech-to-text inference
│   ├── lyricsgenius_utils.py  # Genius API integration & fuzzy search matching
│   ├── main.py                # FastAPI entry point & pipeline orchestration
│   ├── requirements.txt       # Python backend dependencies
│   ├── signal_test.py         # Waveform and spectrogram visualization utilities
│   └── spotify_utils.py       # Spotify Web API metadata fetching & enrichment
│
└── README.md                  # Project documentation
```
Tech Stack
Client (Android)
* Language & SDK: Kotlin, Android SDK (API 26+)
* UI & Architecture: Material Design Components, View Binding, AndroidX
* Audio Capture & Codec: Android AudioRecord (PCM 16-bit, 44.1 kHz mono) and FFmpegKit (FLAC compression)
* Networking & Data: Retrofit 2, OkHttp 3, Gson, Glide

Backend (Python)
* Framework: FastAPI, Uvicorn (ASGI)
* DSP & Audio: Librosa, SciPy, Pedalboard, SoundFile
* Machine Learning: OpenAI Whisper (Speech-to-Text)
* APIs & Search: ACRCloud Identification API, Genius API (via lyricsgenius), RapidFuzz, Spotify Web API

Getting Started
1. Backend Setup
Navigate to the backend directory:

```Bash
cd backend
```
Create and activate a virtual environment:

```Bash
python -m venv .venv
# On Windows:
.venv\Scripts\activate
# On macOS/Linux:
source .venv/bin/activate
```

Install dependencies:

```Bash
pip install -r requirements.txt
```
Configure Environment Variables:
Create a .env file in the backend/ directory with the following keys:

```Python
SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
GENIUS_API_TOKEN=your_genius_access_token
ACR_HOST=your_acrcloud_host
ACR_ACCESS_KEY=your_acrcloud_key
ACR_ACCESS_SECRET=your_acrcloud_secret
```
Run the server:

```Bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
2. Android Client Setup
**Open the frontend/ directory in Android Studio.
**Ensure your Android device and development machine are connected to the same local network.
**Update the backend base URL in your networking configuration to your machine's local IP address:

```Kotlin
private const val BASE_URL = "http://<YOUR_LOCAL_IP>:8000/"
```
