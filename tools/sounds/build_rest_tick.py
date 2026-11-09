"""Synthétise le petit ding du décompte de fin de repos (app/src/main/res/raw/rest_tick.wav).

Même timbre que le ding ding de fin (rest_end_chime.wav) : un coup de cloche à 1 318 Hz (mi), avec ses harmoniques
à 2 et 2,76 fois la fréquence, mais un seul coup, bref et un peu moins fort.
Lancement : python tools/sounds/build_rest_tick.py
"""
import math
import struct
import wave
from pathlib import Path

OUTPUT = Path(__file__).resolve().parents[2] / "app/src/main/res/raw/rest_tick.wav"
SAMPLE_RATE = 44_100
DURATION_S = 0.18
FUNDAMENTAL_HZ = 1_318.5
# (rapport à la fondamentale, amplitude relative) : les mêmes que dans le son de fin.
PARTIALS = [(1.0, 1.0), (2.0, 0.3), (2.76, 0.13)]
PEAK = 0.55
ATTACK_S = 0.002
DECAY_PER_S = 22.0


def sample(t: float) -> float:
    tone = sum(level * math.sin(2 * math.pi * FUNDAMENTAL_HZ * ratio * t) for ratio, level in PARTIALS)
    attack = min(1.0, t / ATTACK_S)
    # Fondu final sur les 20 dernières millisecondes : pas de clic à la coupure.
    fade = min(1.0, (DURATION_S - t) / 0.02)
    return tone * attack * fade * math.exp(-DECAY_PER_S * t)


def main() -> None:
    count = int(SAMPLE_RATE * DURATION_S)
    samples = [sample(i / SAMPLE_RATE) for i in range(count)]
    scale = PEAK * 32_767 / max(abs(s) for s in samples)
    with wave.open(str(OUTPUT), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(SAMPLE_RATE)
        out.writeframes(b"".join(struct.pack("<h", round(s * scale)) for s in samples))
    print(OUTPUT)


if __name__ == "__main__":
    main()
