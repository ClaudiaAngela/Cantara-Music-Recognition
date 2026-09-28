from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
import os
import shutil
import uvicorn
from dsp_utils import dsp
from acr_utils import acr_fingerprint, acr_humming, acr_cover
from lyrics_utils import get_lyrics
from lyricsgenius_utils import genius_identify_track
from spotify_utils import spotify_datas
from signal_test import signal_debug
import json

# Inițializarea aplicației FastAPI.
app = FastAPI(title="Music ID - Universal Orchestrator")
# Configurarea middleware-ului CORS.
# Această setare permite cereri din orice origine
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
# Endpoint-ul principal al backend-ului.
@app.post("/identify")
async def identify_v3(file: UploadFile = File(...), response_body=None):

    temp_path = f"temp_{file.filename}"
    path_fp = None
    path_voice = None
    whisper_text = None

    # Salvarea fișierului brut primit de la client
    with open(temp_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    ARCHIVE_DIR = "arhive_dsp"
    if not os.path.exists(ARCHIVE_DIR):
        os.makedirs(ARCHIVE_DIR)
    try:
        # Pipeline de Procesare Semnal (DSP)
        # mode='music'-variantă optimizată pentru fingerprinting
        path_fp = dsp(temp_path, mode='music')

        # mode='voice'-variantă optimizată pentru recunoaștere vocală
        path_voice = dsp(temp_path, mode='voice')

        # Salvăm copiile
        shutil.copy(path_fp, os.path.join(ARCHIVE_DIR, f"fingerprint.flac"))
        shutil.copy(path_voice, os.path.join(ARCHIVE_DIR, f"voice.flac"))

        if not path_fp or not path_voice:
            return {"status": "Error", "details": "DSP processing failed !"}

        # Generarea de debug vizual pentru semnalul audio.
        signal_debug(temp_path,"01_Original_track")
        signal_debug(path_fp,"02_Processed_Fingerprint")
        signal_debug(path_voice,"03_Processed_Voice")

        # Lista în care sunt colectate toate rezultatele obținute
        all_candidates = []

        print("\n" + "═" * 60)
        print("Searching the best result ")

        #  ACR Fingerprint
        res_fp = acr_fingerprint(path_fp)
        if res_fp:
            res_fp['method'] = 'Fingerprint'
            all_candidates.append(res_fp)


            print(f"[Fingerprint]: {res_fp['title']} - {res_fp['artist']} (Scor: {res_fp['score']})")

        # ACR Humming
        res_hm = acr_humming(path_voice)
        if res_hm:
            res_hm['method'] = 'Humming'
            all_candidates.append(res_hm)
            print(f"[Humming]: {res_hm['title']} - {res_hm['artist']} (Scor: {res_hm['score']})")

        # ACR Cover
        result_cover = acr_cover(path_voice)
        if result_cover:
            result_cover['method'] = 'Cover'
            all_candidates.append(result_cover)
            print(f"[Cover]: {result_cover['title']} - {result_cover['artist']} (Scor: {result_cover['score']})")

        #  Whisper + Genius (Analiză Versuri)
        whisper_text = get_lyrics(path_voice)
        if whisper_text and len(whisper_text) > 5:
            print(f"[Lyrics]: {whisper_text}...")
            result_genius = genius_identify_track(whisper_text)

            if result_genius:

                # Adăugăm în lista de melodii găsite și afișăm melodia cu scorul cel mai mare

                if isinstance(result_genius, dict): # o melodie = un dictionar
                    result_genius['method'] = 'Lyrics'
                    all_candidates.append(result_genius)
                    best_genius_result=result_genius
                elif isinstance(result_genius, list): # returneaza o lista de dictionare
                    for g in result_genius:
                        g['method'] = 'Lyrics'
                        all_candidates.append(g)
                    best_genius_result=max(result_genius, key=lambda x: x['score'])
                print(f"[Best Lyrics Genius identification ]: {best_genius_result['title']}-{best_genius_result['artist']}| Scrore{best_genius_result['score']}.")

        print("═" * 60 + "\n")

        # Logică de Deduplicare și Sortare
        if not all_candidates:
            return {
                "status": "No Match",
                "whisper_text": whisper_text,
                "message": "Nicio potrivire găsită."
            }

        # Eliminarea dublurilor:
        # Pentru aceeași pereche titlu-artist se păstrează doar
        # rezultatul cu scorul cel mai bun.
        unique_matches = {}
        for cand in all_candidates:
            key = (cand['title'].lower().strip(), cand['artist'].lower().strip())
            if key not in unique_matches or cand['score'] > unique_matches[key]['score']:
                unique_matches[key] = cand

        # Sortarea rezultatelor după scor descrescător
        sorted_results = sorted(unique_matches.values(), key=lambda x: x['score'], reverse=True)

        # Alegerea rezultatului final
        final_selection = []
        show_all = False
        if len(sorted_results) > 1:
            gap = sorted_results[0]['score'] - sorted_results[1]['score']
            # Dacă scorurile sunt apropiate, oferim opțiuni
            # altfel se returnează doar cea mai bună potrivire
            if gap < 15:
                show_all = True
                final_selection = sorted_results[:5]
            else:
                final_selection = [sorted_results[0]]
        else:
            final_selection = [sorted_results[0]]

        # Îmbogățire cu date din Spotify
        print("JSON înainte de Spotify:")
        print(json.dumps(final_selection, indent=2, ensure_ascii=False))
        for item in final_selection:
            item['spotify'] = spotify_datas(item['artist'], item['title'])

        # Format răspuns pentru Android
        best_result = final_selection[0]
        spotify_data = best_result.get('spotify')

        # Construim răspunsul final
        response = {
            "status": "success",
            "title": best_result.get('title', 'Unknown'),
            "artist": best_result.get('artist', 'Unknown'),
            "confidence": best_result.get('score', 0) / 100
        }

        # Dacă avem date de Spotify, le adaugăm
        if spotify_data:
            response.update({
                "album": spotify_data.get('album', 'Unknown'),
                "image_url": spotify_data.get('image_url'),
                "spotify_url": spotify_data.get('spotify_url')
            })
        else:
            response.update({
                "album": "Unknown",
                "image_url": None,
                "spotify_url": None
            })
        print("JSON după Spotify:")
        print(json.dumps(response, indent=2, ensure_ascii=False))

        print(f"[BACKEND] Final response: {response}")
        return response


    except Exception as e:
        print(f"ERROR: {str(e)}")
        return {"status": "Error", "details": str(e)}

    finally:
        # Curățenie fișiere (Prevenirea umplerii spațiului de stocare)
        if os.path.exists(temp_path):
            os.remove(temp_path)
        if path_fp and os.path.exists(path_fp):
            os.remove(path_fp)
        if path_voice and os.path.exists(path_voice):
            os.remove(path_voice)
# Pornirea aplicației backend cu Uvicorn.
if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)