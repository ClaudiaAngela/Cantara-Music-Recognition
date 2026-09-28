import os
import time
import base64
import hmac
import hashlib
import requests
from dotenv import load_dotenv
# Încărcarea variabilelor de mediu din fișierul .env.
load_dotenv()

# Folosim aceleași chei pentru toate, deoarece proiectul e hibrid
ACR_KEY = os.getenv("ACR_KEY")
ACR_SECRET = os.getenv("ACR_SECRET")
ACR_HOST = os.getenv("ACR_HOST")


def generate_signature(key, secret, timestamp):
    # Generează semnătura HMAC-SHA1 necesară autentificării la ACRCloud.
    string_to_sign = f"POST\n/v1/identify\n{key}\naudio\n1\n{str(timestamp)}"
    # Calculul semnăturii și codarea rezultatelor în Base64
    sign = base64.b64encode(
        hmac.new(secret.encode('ascii'), string_to_sign.encode('ascii'), digestmod=hashlib.sha1).digest()
    ).decode('ascii')
    return sign

#Motorul de bază care interoghează ACRCloud.
def acr_identify_core(file_path, search_type="General"):
    # Verificarea existenței cheilor de configurare
    if not ACR_KEY or not ACR_HOST:
        print(f" [ACR {search_type}]: Lipsesc cheile de configurare!")
        return None

    # Endpoint-ul ACRCloud pentru identificare
    requrl = f"https://{ACR_HOST}/v1/identify"
    # Generarea timestamp-ului și a semnăturii de autentificare
    timestamp = time.time()
    signature = generate_signature(ACR_KEY, ACR_SECRET, timestamp)
    # Parametrii necesari cererii către API

    data = {
        'access_key': ACR_KEY,
        'sample_bytes': os.path.getsize(file_path),
        'timestamp': str(timestamp),
        'signature': signature,
        'data_type': 'audio',
        "signature_version": "1"
    }

    try:
        # Deschiderea fișierului audio și trimiterea lui către ACRCloud
        with open(file_path, 'rb') as f:
            files = [('sample', (os.path.basename(file_path), f, 'audio/mpeg'))]
            response = requests.post(requrl, files=files, data=data, timeout=20)
            # Procesarea răspunsului doar dacă cererea a fost executată cu succes
            if response.status_code == 200:
                res = response.json()

                # Verificăm dacă am găsit ceva (Cod 0 = Succes)
                if res.get("status", {}).get("code") == 0:
                    metadata = res.get("metadata", {})

                    # Motorul hibrid poate pune rezultatul în 'music' sau în 'humming'
                    # Prioritizăm 'music' pentru Fingerprint/Cover și 'humming' pentru fredonat
                    song_data = None
                    if "music" in metadata:
                        song_data = metadata["music"][0]
                    elif "humming" in metadata:
                        song_data = metadata["humming"][0]

                    # Dacă există date despre melodie, acestea sunt extrase și returnate
                    if song_data:
                        return {
                            "title": song_data.get("title"),
                            "artist": song_data["artists"][0].get("name") if song_data.get("artists") else "Unknown",
                            "score": int(float(song_data.get("score"))),
                            "method": search_type

                        }

                # Dacă codul e 1001, înseamnă pur și simplu că nu a găsit piesa
                return None
    except Exception as e:
        print(f" Eroare API ACR ({search_type}): {e}")
        return None
    return None

def acr_fingerprint(file_path):
    #Identifică piesa originală (Radio/Boxă).
    return acr_identify_core(file_path, search_type="Fingerprint")


def acr_humming(file_path):
    #Identifică fredonatul sau fluieratul.
    return acr_identify_core(file_path, search_type="Humming")


def acr_cover(file_path):
    #Identifică variante cover (alt artist sau live).
    return acr_identify_core(file_path, search_type="Cover")