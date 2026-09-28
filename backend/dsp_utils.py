import librosa
import soundfile as sf
import os
import numpy as np
from scipy import signal
from pedalboard import Pedalboard, NoiseGate, LowpassFilter, HighpassFilter,Compressor


def dsp(input_path, mode):
    # Funcție responsabilă de prelucrarea audio înainte de identificare.
    try:
        # Alegerea frecvenței de eșantionare în funcție de tipul semnalului
        # music-fingerprinting / identificare muzicală
        # voice-recunoaștere vocală / versuri / humming
        target_sr = 44100 if mode == 'music' else 32000
        # Încărcarea semnalului audio și conversia la mono
        y, sr = librosa.load(input_path, sr=target_sr, mono=True)

        if mode == 'music':
            # Se aplică filtrul Wiener
            y_processed = signal.wiener(y, mysize=3)
        elif mode == 'voice':
            # Construcția Board-ului

            pre_board = Pedalboard([
                HighpassFilter(cutoff_frequency_hz=100),
                Compressor(threshold_db=-20, ratio=4),
                NoiseGate(threshold_db=-45, ratio=10)
            ])

            y_pre = pre_board(y, sr)

            # PRE-EMPHASIS
            y_emph = np.append(y_pre[0], y_pre[1:] - 0.85 * y_pre[:-1])

            # HPSS
            y_harmonic, y_percussive = librosa.effects.hpss(y_emph, margin=(1.5, 5.0))
            y_mixed = 0.7 * y_harmonic + 0.3 * y_percussive

            # POST-PROCESARE
            post_board = Pedalboard([
                LowpassFilter(cutoff_frequency_hz=8000)
            ])
            y_processed = post_board(y_mixed, sr)

        else:
            y_processed = y
        # Normalizarea semnalului înainte de salvare
        y_final = librosa.util.normalize(y_processed)

        # Salvarea fișierului audio procesat în format FLAC
        base_name = os.path.splitext(os.path.basename(input_path))[0]
        output_filename = f"processed_{mode}_{base_name}.flac"
        output_path = os.path.join(os.getcwd(), output_filename)
        sf.write(output_path, y_final, target_sr, format='FLAC')

        return output_path

    except Exception as e:
        print(f" DSP ERROR: {e}")
        return None