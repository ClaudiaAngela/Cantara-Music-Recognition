import requests
from dotenv import load_dotenv
import os
import base64
import json
# Încărcarea variabilelor de mediu din fișierul .env.
load_dotenv()

CLIENT_ID = os.getenv("SPOTIFY_CLIENT_ID") # getenv ia variabila din fis .env
CLIENT_SECRET = os.getenv("SPOTIFY_CLIENT_SECRET")

# Funcție care obține un token de acces de tip Client Credentials
def get_token():
    ## Construirea șirului de autentificare în format CLIENT_ID:CLIENT_SECRET
    auth_string=CLIENT_ID+":"+CLIENT_SECRET
    # Conversia șirului în bytes pentru codificare Base64
    auth_bytes=auth_string.encode("utf-8")
    # base64.b64encode(auth_bytes) returneza un obiect base64 dupa care str il face string
    auth_base64=str(base64.b64encode(auth_bytes),"utf-8")
    # Endpoint-ul Spotify pentru obținerea token-ului
    url="https://accounts.spotify.com/api/token"
    # Headerele cerute de API-ul Spotify pentru autentificare
    headers={
        "Authorization": f"Basic {auth_base64}",
        "Content-Type": "application/x-www-form-urlencoded"
    }
    # Parametrii necesari pentru fluxul client_credentials
    data={
        "grant_type": "client_credentials"
    }
    # Trimiterea cererii POST către Spotify Accounts API
    result = requests.post(url, data=data, headers=headers)
    # Conversie json într-un dicționar .content
    json_result = json.loads(result.content)
    # Extragerea token-ul de acces
    token = json_result["access_token"]
    return token

# Funcție care caută pe Spotify o piesă după titlu și artist.
def spotify_datas(artist_raw, title_raw):
    #Obținerea token-ului de la funcția get_token()
    token = get_token()

    # Construire cererii de căutare
    # Combinarea artistul și titlul pt un rezultat cât mai precis
    query=title_raw+" "+artist_raw
    # Endpoint-ul de căutare din Spotify Web API
    url_search="https://api.spotify.com/v1/search"
    # Headerele cererii, incluzând Bearer token
    headers = {
        "Authorization": f"Bearer {token}",
    }
    params = {
        "q": query,
        "type": "track",
        "limit": 1, #prima varianta
    }

    #trimitem cererea catre Spotify
    response = requests.get(url_search, params=params, headers=headers)

    if response.status_code == 200:
        data = response.json()
        items=data.get('tracks',{}).get("items",[])

        if items:
            track=items[0]
            # Construirea listei de artiști separați prin virgulă
            all_artists=", ".join([artist['name'] for artist in track['artists']])
            # Returnarea informațiilor relevante despre piesă
            return{
                "name":track['name'],
                "artist":all_artists,
                "album":track['album']['name'],
                "image_url":track['album']['images'][0]['url'] if track['album']['images'] else None,
                "spotify_url":track['external_urls']['spotify'],

            }
        return None

