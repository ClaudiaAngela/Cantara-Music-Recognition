import os
import lyricsgenius
from rapidfuzz import fuzz
# Citirea tokenului de acces pentru Genius din variabilele de mediu.
GENIUS_API_TOKEN = os.getenv("GENIUS_CLIENT_ACCESS_TOKEN")

# Inițializarea clientului Genius.
genius = lyricsgenius.Genius(GENIUS_API_TOKEN)
genius.verbose = False #dezactivează mesajele suplimentare
genius.remove_section_headers = True #elimină antetele de secțiune din versuri
genius.skip_non_songs = True #ignoră rezultatele care nu sunt melodii


def genius_identify_track(text_whisper):
    # Dacă textul este prea scurt, nu există suficiente informații pentru identificare
    if not text_whisper or len(text_whisper) < 10:
        return None
    # Conversia textului la litere mici
    text_lower = text_whisper.lower()
    # Împărțirea textului în cuvinte pentru a construi fragmente de căutare
    words = text_whisper.split()

    probes = [
        " ".join(words[:8]),
        " ".join(words[8:16]),
        " ".join(words[-8:]),
    ]
    # Lista în care sunt adunate toate hit-urile găsite în Genius
    hits = []
    # Căutarea fragmentelor în Genius.
    for probe in probes:
        if not probe.strip():
            continue
        res = genius.search_lyrics(probe)
        if res and res.get("sections") and res["sections"]:
            hits.extend(res["sections"][0].get("hits", []))

    if not hits:
        return None
    # Se calculează un scor de similaritate între textul transcris
    # și combinația titlu + artist.
    # Se păstrează rezultatul cu scorul cel mai bun.
    best = None
    best_score = 0
    seen_ids = set()

    for h in hits:
        r = (h.get("result") or {})
        song_id = r.get("id")
        title = r.get("title")
        artist = (r.get("primary_artist") or {}).get("name")
        url = r.get("url", "")
        # Se ignoră rezultatele incomplete
        if not song_id or not title or not artist:
            continue
        # Evitarea duplicatelor
        if song_id in seen_ids:
            continue
        seen_ids.add(song_id)
        # Se acceptă doar paginile de tip lyrics
        if not url.endswith("-lyrics"):
            continue
        # Calculul scorului de similaritate
        score = fuzz.partial_ratio(f"{title} {artist}".lower(), text_lower)
        if score > best_score:
            best_score = score
            best = r
    # Dacă scorul este prea mic, rezultatul este considerat nesigur
    if not best or best_score <= 35:
        return None
    # Titlul ales este normalizat pentru compararea cu alte rezultate
    target_title = (best.get("title") or "").strip().lower()
    if not target_title:
        return None
    # Dintre toate hit-urile cu același titlu, se alege versiunea
    # care are cele mai multe pageviews
    chosen = best
    chosen_pageviews = -1

    for h in hits:
        r = (h.get("result") or {})
        song_id = r.get("id")
        title = r.get("title")
        artist = (r.get("primary_artist") or {}).get("name")
        url = r.get("url", "")

        if not song_id or not title or not artist:
            continue
        if not url.endswith("-lyrics"):
            continue
        if title.strip().lower() != target_title:
            continue

        stats = r.get("stats") or {}
        pageviews = stats.get("pageviews", 0)

        if pageviews > chosen_pageviews:
            chosen_pageviews = pageviews
            chosen = r
    # Extragem datele finale ale piesei alese
    chosen_id = chosen.get("id")
    chosen_title = chosen.get("title")
    chosen_artist = (chosen.get("primary_artist") or {}).get("name")

    if not chosen_id or not chosen_title or not chosen_artist:
        return None

    # Obținerea metadatelor complete pentru piesa identificată.
    # Se încearcă interogarea finală pe baza titlului, artistului și id-ului.
    try:
        song = genius.search_song(
            title=chosen_title,
            artist=chosen_artist,
            song_id=chosen_id,
        )

        return {
            "title": chosen_title,
            "artist": chosen_artist,
            "lyrics": song.lyrics if song and song.lyrics else "Lyrics not found",
            "score": best_score,
            "url": chosen.get("url", ""),
        }
    except Exception:
        return None