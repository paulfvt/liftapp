"""Prépare la base d'aliments embarquée dans l'app à partir de la table Ciqual de l'Anses.

Étapes :
1. télécharge la table Ciqual 2020 en XML (copie déposée par l'Anses sur Zenodo, empreinte vérifiée) ;
2. lit les aliments et, pour chacun, l'énergie, les protéines, les glucides et les lipides pour 100 g ;
3. écrit app/src/main/assets/nutrition/foods.json et version.txt.

Usage (depuis la racine du projet) :
    python tools/nutrition/build_foods.py
    python tools/nutrition/build_foods.py --source chemin/vers/XML.zip   # autre version de la table

Aucune dépendance en dehors de la bibliothèque standard de Python.

Licence des données : Licence Ouverte (data.gouv.fr) et CC-BY 4.0 (Zenodo). Citer « Anses. Table de composition
nutritionnelle des aliments Ciqual 2020 » dans l'app (« À propos ») et le README.
"""

import argparse
import hashlib
import json
import re
import sys
import urllib.request
import zipfile
from pathlib import Path

# Le site ciqual.anses.fr était en maintenance le 3 novembre 2026 : même fichier, déposé par l'Anses sur Zenodo.
SOURCE_URL = "https://zenodo.org/api/records/4770600/files/XML_2020_07_07.zip/content"
SOURCE_SHA256 = "e6374c1cd241da825503c7d4f797d36e6c0f0cd4faa5e4db6f1f175a9d7b85da"
SOURCE_NAME = "Ciqual 2020"

# À incrémenter à chaque modification de la base : l'app réimporte alors les aliments de la table.
FOODS_VERSION = 1

ROOT = Path(__file__).resolve().parents[2]
CACHE_DIR = Path(__file__).resolve().parent / ".cache"
ASSETS_DIR = ROOT / "app" / "src" / "main" / "assets" / "nutrition"

# Codes des constituants Ciqual (fichier const_*.xml).
ENERGY_KCAL = "328"  # Énergie, règlement UE n° 1169/2011 (kcal/100 g)
PROTEIN = "25000"  # Protéines, N x facteur de Jones (g/100 g), la valeur des étiquettes
CARBS = "31000"  # Glucides (g/100 g)
FAT = "40000"  # Lipides (g/100 g)
FIBER = "34100"  # Fibres alimentaires (g/100 g), seulement pour calculer une énergie manquante
ALCOHOL = "60000"  # Alcool (g/100 g), idem
WANTED = {ENERGY_KCAL, PROTEIN, CARBS, FAT, FIBER, ALCOHOL}

# Coefficients du règlement UE n° 1169/2011 (kcal par gramme), pour les aliments dont l'énergie n'est pas donnée.
KCAL_PER_GRAM = {PROTEIN: 4, CARBS: 4, FAT: 9, FIBER: 2, ALCOHOL: 7}


def download(url: str, target: Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    print(f"Téléchargement de {url}")
    with urllib.request.urlopen(url, timeout=120) as response:
        target.write_bytes(response.read())


def read_source(path: Path) -> tuple[str, str]:
    """Renvoie le texte des fichiers des aliments et des compositions contenus dans le ZIP."""
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()

        def read(prefix: str) -> str:
            name = next(n for n in names if Path(n).name.startswith(prefix) and n.endswith(".xml"))
            return archive.read(name).decode("cp1252")

        return read("alim_2"), read("compo_")


def parse_value(raw: str) -> float | None:
    """Teneur Ciqual : « 12,3 », « traces », « < 0,5 » ou « - » (inconnue)."""
    text = raw.strip()
    if text in ("", "-"):
        return None
    if text == "traces":
        return 0.0
    # « < 0,5 » : sous la limite de détection. On prend la moitié de la limite, comme on le fait souvent.
    if text.startswith("<"):
        return float(text[1:].strip().replace(",", ".")) / 2
    return float(text.replace(",", "."))


def clean_name(raw: str) -> str:
    return re.sub(r"\s+", " ", raw).strip()


def build(alim_xml: str, compo_xml: str) -> list[dict]:
    # Le fichier des compositions n'est pas du XML valide (« < 0,5 » brut dans les teneurs) : lecture par motifs.
    values: dict[int, dict[str, float | None]] = {}
    for block in re.finditer(r"<COMPO>(.*?)</COMPO>", compo_xml, re.S):
        text = block.group(1)
        code = re.search(r"<const_code>\s*(\d+)", text).group(1)
        if code not in WANTED:
            continue
        food = int(re.search(r"<alim_code>\s*(\d+)", text).group(1))
        values.setdefault(food, {})[code] = parse_value(re.search(r"<teneur>(.*?)</teneur>", text, re.S).group(1))

    foods = []
    skipped = 0
    for block in re.finditer(r"<ALIM>(.*?)</ALIM>", alim_xml, re.S):
        text = block.group(1)
        code = int(re.search(r"<alim_code>\s*(\d+)", text).group(1))
        name = clean_name(re.search(r"<alim_nom_fr>(.*?)</alim_nom_fr>", text, re.S).group(1))
        nutrients = values.get(code, {})
        macros = [nutrients.get(c) for c in (PROTEIN, CARBS, FAT)]
        if any(m is None for m in macros):
            skipped += 1
            continue
        kcal = nutrients.get(ENERGY_KCAL)
        if kcal is None:
            kcal = sum(KCAL_PER_GRAM[c] * (nutrients.get(c) or 0.0) for c in KCAL_PER_GRAM)
        foods.append({
            "code": code,
            "name": name,
            "kcal": round(kcal),
            "protein": round(macros[0], 1),
            "carbs": round(macros[1], 1),
            "fat": round(macros[2], 1),
        })
    print(f"{len(foods)} aliments gardés, {skipped} écartés (protéines, glucides ou lipides inconnus)")
    return sorted(foods, key=lambda f: f["code"])


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--source", type=Path, help="ZIP XML de la table Ciqual déjà téléchargé")
    args = parser.parse_args()

    source = args.source
    if source is None:
        source = CACHE_DIR / "ciqual_xml.zip"
        if not source.exists():
            download(SOURCE_URL, source)
        digest = hashlib.sha256(source.read_bytes()).hexdigest()
        if digest != SOURCE_SHA256:
            print(f"Empreinte inattendue pour {source} : {digest}", file=sys.stderr)
            return 1

    foods = build(*read_source(source))
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    payload = {"version": FOODS_VERSION, "source": SOURCE_NAME, "foods": foods}
    (ASSETS_DIR / "foods.json").write_text(
        json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8"
    )
    (ASSETS_DIR / "version.txt").write_text(f"{FOODS_VERSION}\n", encoding="utf-8")
    print(f"Écrit dans {ASSETS_DIR}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
