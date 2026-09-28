import matplotlib.pyplot as plt
import librosa
import librosa.display
import numpy as np
import os


def signal_debug(file_path, output_label):
    """
    Generează Waveform și Spectrogramă și le salvează într-un folder de debug.
    """
    # Creăm folderul dacă nu există
    output_folder = "debug_plots"
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    try:
        # Încărcare audio
        y, sr = librosa.load(file_path, sr=None)

        plt.figure(figsize=(10, 6))

        # 1. Waveform
        plt.subplot(2, 1, 1)
        librosa.display.waveshow(y, sr=sr, alpha=0.8)
        plt.title(f"Waveform: {output_label}")

        # 2. Spectrogramă
        plt.subplot(2, 1, 2)
        S = librosa.stft(y)
        S_db = librosa.amplitude_to_db(np.abs(S), ref=np.max)
        librosa.display.specshow(S_db, sr=sr, x_axis='time', y_axis='hz')
        plt.colorbar(format='%+2.0f dB')
        plt.title(f"Spectrogram: {output_label}")

        plt.tight_layout()

        # Salvarea imaginii finale
        plot_path = os.path.join(output_folder, f"{output_label}.png")
        plt.savefig(plot_path)
        plt.close()
        return plot_path

    except Exception as e:
        print(f"❌ Eroare la generarea graficului pentru {output_label}: {e}")
        return None