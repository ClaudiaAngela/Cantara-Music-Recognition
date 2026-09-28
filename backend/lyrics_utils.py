import whisper
import os

from numba.cuda import fp16

# Încărcarea modelului Whisper la nivel global.
model=whisper.load_model("medium")
def get_lyrics(cale_fisier):

    if not os.path.exists(cale_fisier):
        return "Erorr: File not found."

    try:
        # Transcrierea audio-ului în text.
        # fp16=False asigură compatibilitatea pe sisteme fără suport CUDA/FP16
        lyrics = model.transcribe(cale_fisier, fp16=False)
        return lyrics["text"].strip()
    except Exception as e:
        return f"Erorr: {str(e)}"


