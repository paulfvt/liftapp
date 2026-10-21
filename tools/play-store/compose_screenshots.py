# Usage : python compose_screenshots.py <captures brutes> <dossier de sortie> <dossier des polices res/font>
# Met en forme les captures du Play Store : légende en haut, capture réduite aux coins arrondis, format 1080 x 2160
# (Google refuse un grand côté de plus du double du petit, les captures brutes en 1080 x 2400 le dépassent).
import os
import sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

shots, out, fonts = sys.argv[1], sys.argv[2], sys.argv[3]
os.makedirs(out, exist_ok=True)
W, H = 1080, 2160
CAPTION_TOP, CAPTION_HEIGHT = 80, 300
BG, RED, TEXT, MUTED, OUTLINE = (13, 10, 10), (255, 116, 104), (243, 234, 232), (210, 194, 191), (76, 64, 63)
title_font = ImageFont.truetype(os.path.join(fonts, 'barlow_condensed_bold.ttf'), 84)
sub_font = ImageFont.truetype(os.path.join(fonts, 'barlow_regular.ttf'), 42)

PAGES = [
    ('2-workout', '01-seance', 'Ta perf de la dernière fois sous les yeux',
     'Valeurs pré-remplies, minuteur de repos'),
    ('4-progress', '02-progression', 'Regarde-toi progresser', 'Records et courbes pour chaque exercice'),
    ('1-templates', '03-templates', 'Tes séances prêtes à lancer', 'Templates rangés en dossiers'),
    ('3-gym-mode', '04-mode-salle', 'Mode salle', 'Gros boutons, une série à la fois, écran allumé'),
    ('5-history', '05-historique', 'Tout ton historique', 'Chaque séance, modifiable après coup'),
    ('6-exercise', '06-exercices', 'Plus de 750 exercices', 'Images et instructions en français'),
]


def wrap(draw, text, font, width):
    lines, line = [], ''
    for word in text.split():
        candidate = (line + ' ' + word).strip()
        if draw.textlength(candidate, font=font) <= width:
            line = candidate
        else:
            lines.append(line)
            line = word
    return lines + [line]


def wash():
    # Lueur rouge discrète en haut, comme le dégradé « wash » du guide de style.
    layer = Image.new('RGB', (W, H), BG)
    glow = Image.new('L', (W, H), 0)
    ImageDraw.Draw(glow).ellipse((-200, -700, W + 200, 700), fill=70)
    glow = glow.filter(ImageFilter.GaussianBlur(160))
    return Image.composite(Image.new('RGB', (W, H), (217, 36, 27)), layer, glow)


for source, name, title, subtitle in PAGES:
    canvas = wash()
    draw = ImageDraw.Draw(canvas)
    # Légende centrée dans une zone fixe : toutes les captures gardent la même taille et la même position.
    titles = wrap(draw, title.upper(), title_font, W - 120)
    subtitles = wrap(draw, subtitle, sub_font, W - 140)
    block = len(titles) * 92 + 14 + len(subtitles) * 54
    y = CAPTION_TOP + (CAPTION_HEIGHT - block) // 2
    for line in titles:
        draw.text((W // 2, y), line, font=title_font, fill=RED, anchor='mt')
        y += 92
    y += 14
    for line in subtitles:
        draw.text((W // 2, y), line, font=sub_font, fill=TEXT, anchor='mt')
        y += 54

    shot = Image.open(os.path.join(shots, source + '.png')).convert('RGB')
    # Barre d'état : on ne garde que l'heure et les icônes de droite (icônes système parasites de l'émulateur).
    ImageDraw.Draw(shot).rectangle((130, 0, 640, 78), fill=BG)
    top = CAPTION_TOP + CAPTION_HEIGHT + 40
    height = H - top - 80
    width = round(shot.width * height / shot.height)
    shot = shot.resize((width, height), Image.LANCZOS)
    left = (W - width) // 2
    radius = 44
    mask = Image.new('L', (width, height), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width - 1, height - 1), radius=radius, fill=255)
    draw.rounded_rectangle((left - 3, top - 3, left + width + 2, top + height + 2), radius=radius + 3, outline=OUTLINE,
                           width=3)
    canvas.paste(shot, (left, top), mask)
    canvas.save(os.path.join(out, name + '.png'), optimize=True)
    print(name, canvas.size, 'capture', (width, height))
