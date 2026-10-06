"""Prépare le catalogue d'exercices embarqué dans l'app à partir de free-exercise-db.

Étapes :
1. télécharge la base source à un commit figé (reproductible) ;
2. retire les étirements et les exercices au rouleau de massage ;
3. associe matériel, catégorie, type de suivi et muscles aux énumérations Kotlin ;
4. fusionne les traductions françaises de translations/fr/*.json ;
5. convertit les images en WebP réduites ;
6. écrit app/src/main/assets/catalog/exercises.json.

Usage (depuis la racine du projet) :
    python tools/catalog/build_catalog.py               # échoue si une traduction manque
    python tools/catalog/build_catalog.py --allow-missing   # garde l'anglais pour les manquantes
    python tools/catalog/build_catalog.py --list-untranslated

Dépendance : Pillow (pip install pillow).
"""

import argparse
import io
import json
import sys
import urllib.request
from pathlib import Path

from PIL import Image

SOURCE_REPO = "yuhonas/free-exercise-db"
SOURCE_SHA = "f00c92c7dcf1216a928a52c3706c7ce8e2f71ed5"
RAW_URL = f"https://raw.githubusercontent.com/{SOURCE_REPO}/{SOURCE_SHA}"

# À incrémenter à chaque modification du catalogue : l'app réimporte alors les exercices du catalogue.
CATALOG_VERSION = 2

IMAGE_WIDTH = 480
IMAGE_QUALITY = 70

ROOT = Path(__file__).resolve().parents[2]
TOOL_DIR = Path(__file__).resolve().parent
CACHE_DIR = TOOL_DIR / ".cache"
TRANSLATIONS_DIR = TOOL_DIR / "translations" / "fr"
ASSETS_DIR = ROOT / "app" / "src" / "main" / "assets" / "catalog"
REVIEW_FILE = TOOL_DIR / "noms-a-relire.csv"

EQUIPMENT = {
    "barbell": "BARBELL",
    "dumbbell": "DUMBBELL",
    "e-z curl bar": "EZ_BAR",
    "kettlebells": "KETTLEBELL",
    "cable": "CABLE",
    "machine": "MACHINE",
    "bands": "BAND",
    "medicine ball": "MEDICINE_BALL",
    "exercise ball": "EXERCISE_BALL",
    "body only": "BODYWEIGHT",
    "other": "OTHER",
    None: "BODYWEIGHT",
}

# La catégorie est déduite du matériel, sauf pour le cardio qui vient de la source.
CATEGORY_BY_EQUIPMENT = {
    "BARBELL": "FREE_WEIGHT",
    "DUMBBELL": "FREE_WEIGHT",
    "EZ_BAR": "FREE_WEIGHT",
    "KETTLEBELL": "FREE_WEIGHT",
    "MEDICINE_BALL": "FREE_WEIGHT",
    "CABLE": "MACHINE",
    "MACHINE": "MACHINE",
    "BODYWEIGHT": "BODYWEIGHT",
    "BAND": "OTHER",
    "EXERCISE_BALL": "OTHER",
    "OTHER": "OTHER",
}

# Sans charge mesurable en kg, on ne suit que les répétitions.
REPS_ONLY_EQUIPMENT = {"BODYWEIGHT", "BAND", "EXERCISE_BALL"}

# Exercices suivis à la distance (avec la durée) ou à la durée seule, choisis à la main.
DISTANCE_EXERCISES = {
    "Bicycling", "Jogging_Treadmill", "Running_Treadmill", "Rowing_Stationary", "Trail_Running_Walking",
    "Walking_Treadmill", "Skating", "Prowler_Sprint", "Wind_Sprints", "Farmers_Walk", "Yoke_Walk",
    "Rickshaw_Carry", "Sled_Push", "Sled_Drag_-_Harness", "Bear_Crawl_Sled_Drags",
}
DURATION_EXERCISES = {
    "Bicycling_Stationary", "Elliptical_Trainer", "Recumbent_Bike", "Rope_Jumping", "Stairmaster", "Step_Mill",
    "Plank", "Side_Bridge", "Isometric_Chest_Squeezes", "Isometric_Neck_Exercise_-_Front_And_Back",
    "Isometric_Neck_Exercise_-_Sides", "Crucifix", "Plate_Pinch", "Downward_Facing_Balance",
}

MUSCLES = {
    "abdominals": "ABDOMINALS",
    "abductors": "ABDUCTORS",
    "adductors": "ADDUCTORS",
    "biceps": "BICEPS",
    "calves": "CALVES",
    "chest": "CHEST",
    "forearms": "FOREARMS",
    "glutes": "GLUTES",
    "hamstrings": "HAMSTRINGS",
    "lats": "LATS",
    "lower back": "LOWER_BACK",
    "middle back": "MIDDLE_BACK",
    "neck": "NECK",
    "quadriceps": "QUADRICEPS",
    "shoulders": "SHOULDERS",
    "traps": "TRAPS",
    "triceps": "TRICEPS",
}


def fetch(url: str) -> bytes:
    with urllib.request.urlopen(url, timeout=60) as response:
        return response.read()


def load_source() -> list[dict]:
    cached = CACHE_DIR / f"exercises-{SOURCE_SHA[:7]}.json"
    if not cached.exists():
        CACHE_DIR.mkdir(parents=True, exist_ok=True)
        cached.write_bytes(fetch(f"{RAW_URL}/dist/exercises.json"))
    return json.loads(cached.read_text(encoding="utf-8"))


def keep(exercise: dict) -> bool:
    return exercise["category"] != "stretching" and exercise.get("equipment") != "foam roll"


def load_translations() -> dict:
    translations = {}
    for file in sorted(TRANSLATIONS_DIR.glob("*.json")):
        for key, value in json.loads(file.read_text(encoding="utf-8")).items():
            if key in translations:
                sys.exit(f"Traduction en double pour {key} ({file.name})")
            translations[key] = value
    return translations


def clean_steps(steps: list[str]) -> list[str]:
    # L'app sépare les étapes par des retours à la ligne : on retire ceux de la source et les étapes vides.
    cleaned = (" ".join(step.split()) for step in steps)
    return [step for step in cleaned if step]


def tracking_type(key: str, equipment: str) -> str:
    if key in DISTANCE_EXERCISES:
        return "DISTANCE"
    if key in DURATION_EXERCISES:
        return "DURATION"
    if equipment in REPS_ONLY_EQUIPMENT:
        return "REPS"
    return "WEIGHT_REPS"


def convert_images(exercise: dict) -> int:
    target_dir = ASSETS_DIR / "images" / exercise["id"]
    for index, path in enumerate(exercise["images"]):
        target = target_dir / f"{index}.webp"
        if target.exists():
            continue
        image = Image.open(io.BytesIO(fetch(f"{RAW_URL}/exercises/{path}"))).convert("RGB")
        height = round(image.height * IMAGE_WIDTH / image.width)
        target_dir.mkdir(parents=True, exist_ok=True)
        image.resize((IMAGE_WIDTH, height), Image.LANCZOS).save(target, "WEBP", quality=IMAGE_QUALITY, method=6)
    return len(exercise["images"])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--allow-missing", action="store_true")
    parser.add_argument("--list-untranslated", action="store_true")
    args = parser.parse_args()

    source = [e for e in load_source() if keep(e)]
    translations = load_translations()
    missing = [e["id"] for e in source if e["id"] not in translations]

    if args.list_untranslated:
        print("\n".join(missing))
        return
    if missing and not args.allow_missing:
        sys.exit(f"{len(missing)} exercices sans traduction (voir --list-untranslated)")

    exercises = []
    review_rows = ["cle;nom anglais;nom francais"]
    for index, source_exercise in enumerate(source, start=1):
        key = source_exercise["id"]
        translation = translations.get(key)
        instructions = source_exercise["instructions"]
        # Même nombre d'étapes que la source, sauf quand la source n'en a aucune et que la traduction les rédige.
        if translation and instructions and len(translation["instructions"]) != len(instructions):
            sys.exit(f"{key} : {len(translation['instructions'])} étapes traduites au lieu de {len(instructions)}")

        equipment = EQUIPMENT[source_exercise.get("equipment")]
        category = "CARDIO" if source_exercise["category"] == "cardio" else CATEGORY_BY_EQUIPMENT[equipment]
        name = translation["name"] if translation else source_exercise["name"]
        exercises.append(
            {
                "key": key,
                "name": name,
                "equipment": equipment,
                "category": category,
                "trackingType": tracking_type(key, equipment),
                "primaryMuscles": [MUSCLES[m] for m in source_exercise["primaryMuscles"]],
                "secondaryMuscles": [MUSCLES[m] for m in source_exercise["secondaryMuscles"]],
                "instructions": clean_steps(translation["instructions"] if translation else instructions),
                "imageCount": convert_images(source_exercise),
            }
        )
        review_rows.append(f"{key};{source_exercise['name']};{name}")
        if index % 100 == 0:
            print(f"{index}/{len(source)} exercices traités")

    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    catalog = {
        "version": CATALOG_VERSION,
        "source": f"{SOURCE_REPO}@{SOURCE_SHA[:7]} (Unlicense)",
        "exercises": exercises,
    }
    (ASSETS_DIR / "exercises.json").write_text(
        json.dumps(catalog, ensure_ascii=False, separators=(",", ":")), encoding="utf-8"
    )
    # Lu par l'app à chaque lancement pour savoir s'il faut réimporter, sans ouvrir le gros fichier.
    (ASSETS_DIR / "version.txt").write_text(f"{CATALOG_VERSION}\n", encoding="utf-8")
    REVIEW_FILE.write_text("\n".join(review_rows) + "\n", encoding="utf-8-sig")
    print(f"{len(exercises)} exercices écrits, {len(missing)} sans traduction.")


if __name__ == "__main__":
    main()
