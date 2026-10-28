"""Prépare les tracés de la carte du corps embarqués dans l'app à partir de MuscleMap (licence MIT).

Étapes :
1. télécharge les quatre silhouettes de MuscleMap (homme et femme, face et dos) à un commit figé ;
2. extrait la silhouette et les zones musculaires (tracés SVG) ;
3. associe chaque zone de MuscleMap à un muscle du catalogue (énumération Kotlin Muscle) ;
4. écrit app/src/main/assets/body-map/body_map.json.

Usage (depuis la racine du projet) :
    python tools/body-map/build_body_map.py
    python tools/body-map/build_body_map.py --source chemin/vers/MuscleMap   # copie locale, au même commit

Sans dépendance en dehors de la bibliothèque standard.
"""

import argparse
import json
import re
import urllib.request
from pathlib import Path

SOURCE_REPO = "Jsplice/MuscleMap"
SOURCE_SHA = "170cfacda79376dd59ac0cf2f7dfab8ff9f4443a"
RAW_URL = f"https://raw.githubusercontent.com/{SOURCE_REPO}/{SOURCE_SHA}/packages/assets/src"

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "body-map" / "body_map.json"

DIAGRAMS = [
    ("male-front", "MALE", "FRONT"),
    ("male-back", "MALE", "BACK"),
    ("female-front", "FEMALE", "FRONT"),
    ("female-back", "FEMALE", "BACK"),
]

# Zones de MuscleMap → muscles de free-exercise-db. Le cou n'a pas de zone : il n'apparaît que dans la liste.
MUSCLES = {
    "CHEST": "CHEST",
    "BACK_LOWER": "LOWER_BACK",
    "TRAPEZIUS": "TRAPS",
    "RHOMBOIDS": "MIDDLE_BACK",
    "LATS": "LATS",
    "SHOULDERS_FRONT": "SHOULDERS",
    "SHOULDERS_SIDE": "SHOULDERS",
    "SHOULDERS_REAR": "SHOULDERS",
    "BICEPS": "BICEPS",
    "TRICEPS": "TRICEPS",
    "FOREARMS": "FOREARMS",
    "CORE": "ABDOMINALS",
    "OBLIQUES": "ABDOMINALS",
    "GLUTES": "GLUTES",
    "QUADS": "QUADRICEPS",
    "HAMSTRINGS": "HAMSTRINGS",
    "CALVES": "CALVES",
    "ADDUCTORS": "ADDUCTORS",
    "ABDUCTORS": "ABDUCTORS",
}

# Marge autour du corps, en unités du dessin, pour ne pas coller les mains au bord.
MARGIN = 12

FIELD = re.compile(r'(\w+):\s*"([^"]*)"')
ENTRY = re.compile(r"\{([^{}]*)\}")


def read_source(name: str, local: Path | None) -> str:
    if local is not None:
        return (local / "packages" / "assets" / "src" / f"{name}.ts").read_text(encoding="utf-8")
    with urllib.request.urlopen(f"{RAW_URL}/{name}.ts") as response:
        return response.read().decode("utf-8")


def block(source: str, start: str, end: str) -> str:
    begin = source.index(start)
    return source[begin:source.index(end, begin)]


def entries(text: str) -> list[dict[str, str]]:
    return [dict(FIELD.findall(match)) for match in ENTRY.findall(text)]


def check_center(entry: dict[str, str]) -> None:
    # Les zones « LEFT » seraient à dupliquer en miroir : la version figée n'en a pas, on vérifie qu'il en reste ainsi.
    if entry["side"] != "CENTER":
        raise SystemExit(f"Zone non gérée (côté {entry['side']}) : {entry}")


def bounds(source: str) -> list[float]:
    # Le corps entier : union des cadrages « haut du corps » et « bas du corps » fournis par MuscleMap.
    boxes = [
        [float(v) for v in re.search(rf'{region}:\s*"([^"]+)"', source).group(1).split()]
        for region in ("UPPER_BODY", "LOWER_BODY")
    ]
    left = min(b[0] for b in boxes) - MARGIN
    top = min(b[1] for b in boxes) - MARGIN
    right = max(b[0] + b[2] for b in boxes) + MARGIN
    bottom = max(b[1] + b[3] for b in boxes) + MARGIN
    return [left, top, right - left, bottom - top]


def diagram(name: str, sex: str, view: str, local: Path | None) -> dict:
    source = read_source(name, local)
    outline = entries(block(source, "const OUTLINE", "];"))
    muscles = entries(block(source, "const MUSCLES", "];"))
    for entry in outline + muscles:
        check_center(entry)
    unknown = {m["group"] for m in muscles} - MUSCLES.keys()
    if unknown:
        raise SystemExit(f"Zones sans muscle associé dans {name} : {sorted(unknown)}")
    return {
        "sex": sex,
        "view": view,
        "bounds": bounds(source),
        "outline": [entry["d"] for entry in outline],
        "muscles": [{"muscle": MUSCLES[m["group"]], "path": m["d"]} for m in muscles],
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--source", type=Path, help=f"copie locale de {SOURCE_REPO} au commit {SOURCE_SHA[:7]}")
    args = parser.parse_args()
    data = {
        "source": f"https://github.com/{SOURCE_REPO} ({SOURCE_SHA[:7]}), licence MIT",
        "diagrams": [diagram(name, sex, view, args.source) for name, sex, view in DIAGRAMS],
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    zones = sum(len(d["muscles"]) for d in data["diagrams"])
    print(f"{OUTPUT.relative_to(ROOT)} : {len(data['diagrams'])} silhouettes, {zones} zones")


if __name__ == "__main__":
    main()
