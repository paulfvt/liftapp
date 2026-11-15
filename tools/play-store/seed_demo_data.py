# Données de démonstration pour les captures du README : 10 semaines de Push / Pull / Jambes qui se terminent cette
# semaine, et 4 semaines de repas et de pesées pour la nutrition.
# Usage : python seed_demo_data.py lift.db [AAAA-MM-JJ] (copie de la base de l'émulateur, schéma Room v6 ; la date,
# aujourd'hui par défaut, est le jour des captures).
# Efface templates, séances, records, objectifs, journal et pesées de cette copie avant d'écrire la démonstration.
# Les records ne sont pas écrits ici : supprimer dans l'app la séance « À supprimer » les fait recalculer.
#
# Captures du README (docs/screenshots/, 15 novembre 2026) :
# 1. Récupérer la base de l'émulateur, app arrêtée (voir « Problèmes connus » dans PROGRESS.md), lancer ce script sur
#    la copie, la remettre en place sans ses fichiers -wal et -shm.
# 2. Dans l'app, supprimer la séance « À supprimer » de l'historique (onglet Séances).
# 3. App en français (`adb shell cmd locale set-app-locales com.paulfvt.liftapp --locales fr-FR`), thème sombre
#    (`adb shell cmd uimode night yes`), heure sur 24 h et barre d'état en mode démo à 18:30.
# 4. Capturer avec `adb exec-out screencap -p`, puis réduire de moitié (540 × 1200).
# Captures du Play Store (abandonné) : docs/fiche-play-store.md.
import random
import sqlite3
import sys
from datetime import date, datetime, time, timedelta, timezone

db = sys.argv[1]
today = date.fromisoformat(sys.argv[2]) if len(sys.argv) > 2 else date.today()
c = sqlite3.connect(db)
c.execute('pragma foreign_keys = on')
random.seed(7)

for table in ['personal_record', 'workout_set', 'workout_exercise', 'workout', 'template_set', 'template_exercise',
              'workout_template', 'template_folder', 'goal', 'weekly_goal', 'food_entry', 'saved_meal_item',
              'saved_meal', 'nutrition_goal']:
    c.execute(f'delete from {table}')
c.execute("delete from body_measurement where type = 'BODY_WEIGHT'")
c.execute('update food set isFavorite = 0')

# (id, repos en s, séries de travail, reps cibles, charge de départ, charge d'arrivée, pas, échauffements)
PUSH = [
    (36, 180, 3, 8, 70, 85, 2.5, [(40, 10), (60, 5)]),     # Développé couché à la barre
    (304, 120, 3, 10, 24, 30, 2, []),                      # Développé incliné aux haltères
    (668, 150, 3, 8, 40, 50, 2.5, []),                     # Développé militaire debout
    (575, 90, 3, 12, 8, 10, 1, []),                        # Élévations latérales aux haltères
    (713, 90, 3, 12, 20, 27.5, 2.5, []),                   # Extension triceps à la corde
]
PULL = [
    (39, 180, 3, 6, 110, 135, 2.5, [(60, 8), (100, 3)]),   # Soulevé de terre à la barre
    (490, 150, 3, 8, None, None, None, []),                # Traction (poids du corps)
    (71, 150, 3, 8, 60, 75, 2.5, []),                      # Rowing barre buste penché
    (741, 120, 3, 10, 55, 65, 2.5, []),                    # Tirage vertical prise large
    (277, 90, 3, 12, 14, 18, 2, []),                       # Curl marteau
]
LEGS = [
    (56, 180, 3, 6, 90, 110, 2.5, [(60, 8)]),              # Squat à la barre
    (529, 150, 3, 8, 80, 97.5, 2.5, []),                   # Soulevé de terre roumain
    (366, 120, 3, 10, 160, 200, 10, []),                   # Presse à cuisses
    (559, 90, 3, 12, 40, 50, 2.5, []),                     # Leg curl assis
    (654, 90, 4, 12, 60, 80, 5, []),                       # Mollets debout à la machine
]
FULL_BODY = [(56, 180, 3, 8), (36, 180, 3, 8), (490, 150, 3, 8), (71, 150, 3, 10)]

ms = lambda dt: int(dt.timestamp() * 1000)
epoch_day = lambda d: (d - date(1970, 1, 1)).days
# L'émulateur est réglé sur GMT : une heure UTC est aussi l'heure affichée.
at = lambda day, hour, minute=0: datetime.combine(day, time(hour, minute), tzinfo=timezone.utc)

folder = c.execute("insert into template_folder(name, sortOrder) values ('Push Pull Jambes', 0)").lastrowid


def template(name, folder_id, order, exercises):
    tid = c.execute('insert into workout_template(folderId, name, sortOrder) values (?, ?, ?)',
                    (folder_id, name, order)).lastrowid
    for i, ex in enumerate(exercises):
        ex_id, rest, work_sets, target = ex[0], ex[1], ex[2], ex[3]
        warmups = ex[7] if len(ex) > 7 else []
        te = c.execute('insert into template_exercise(templateId, exerciseId, sortOrder, restSeconds) values (?,?,?,?)',
                       (tid, ex_id, i, rest)).lastrowid
        sets = [('WARMUP', reps) for _, reps in warmups] + [('WORK', target)] * work_sets
        for j, (kind, reps) in enumerate(sets):
            c.execute('insert into template_set(templateExerciseId, sortOrder, type, targetReps) values (?,?,?,?)',
                      (te, j, kind, reps))
    return tid


full_body_id = template('Full body', None, 0, FULL_BODY)
plans = {
    'Push': (template('Push', folder, 0, PUSH), PUSH),
    'Pull': (template('Pull', folder, 1, PULL), PULL),
    'Jambes': (template('Jambes', folder, 2, LEGS), LEGS),
}


def progress(t, start, end, step):
    # Progression qui ralentit avec le temps, arrondie au pas de charge.
    value = start + (end - start) * (1 - (1 - t) ** 1.7)
    return round(value / step) * step


# Lundi, mercredi et vendredi des 10 dernières semaines (la semaine en cours comprise, jusqu'à hier), en tournant
# Jambes → Push → Pull. Une séance manquée il y a 7 semaines : la streak repart de là, comme dans la vraie vie.
monday = today - timedelta(days=today.weekday())
first_monday = monday - timedelta(weeks=9)
days = [first_monday + timedelta(days=k) for k in range((today - first_monday).days)]
days = [d for d in days if d.weekday() in (0, 2, 4)]
skipped = {first_monday + timedelta(weeks=2, days=2)}
order = ['Jambes', 'Push', 'Pull']
sessions = [(day, order[k % 3]) for k, day in enumerate(days) if day not in skipped]
counts = {name: sum(1 for _, n in sessions if n == name) for name in order}
seen = {name: 0 for name in order}

for day, name in sessions:
    template_id, exercises = plans[name]
    t = seen[name] / max(counts[name] - 1, 1)
    seen[name] += 1
    start = at(day, 18, random.choice([5, 15, 30, 40]))
    clock = start + timedelta(minutes=4)
    wid = c.execute("insert into workout(templateId, name, startedAt, endedAt, status) values (?,?,?,?,'FINISHED')",
                    (template_id, name, ms(start), 0)).lastrowid
    for i, ex in enumerate(exercises):
        ex_id, rest, work_sets, target, w0, w1, step, warmups = ex
        we = c.execute('insert into workout_exercise(workoutId, exerciseId, sortOrder, restSeconds) values (?,?,?,?)',
                       (wid, ex_id, i, rest)).lastrowid
        rows = [('WARMUP', w, r, r) for w, r in warmups]
        if w0 is None:
            base = round(6 + 4 * (1 - (1 - t) ** 1.5))
            rows += [('WORK', None, max(base - k + random.choice([0, 0, 1]), 4), target) for k in range(work_sets)]
        else:
            weight = progress(t, w0, w1, step)
            for k in range(work_sets):
                reps = target - (1 if k == work_sets - 1 and random.random() < 0.45 else 0)
                rows.append(('WORK', float(weight), reps, target))
        for j, (kind, weight, reps, tgt) in enumerate(rows):
            clock += timedelta(seconds=(60 if kind == 'WARMUP' else rest) + random.randint(20, 50))
            c.execute('insert into workout_set(workoutExerciseId, sortOrder, type, weightKg, reps, isCompleted, '
                      'completedAt, targetReps) values (?,?,?,?,?,1,?,?)', (we, j, kind, weight, reps, ms(clock), tgt))
    c.execute('update workout set endedAt = ? where id = ?', (ms(clock + timedelta(minutes=2)), wid))

# Séance factice avec tous les exercices : la supprimer dans l'app recalcule tous les records.
dummy_start = at(today, 8)
wid = c.execute("insert into workout(templateId, name, startedAt, endedAt, status) values "
                "(NULL, 'À supprimer', ?, ?, 'FINISHED')",
                (ms(dummy_start), ms(dummy_start + timedelta(minutes=5)))).lastrowid
all_ids = [ex[0] for plan in (PUSH, PULL, LEGS) for ex in plan]
for i, ex_id in enumerate(all_ids):
    we = c.execute('insert into workout_exercise(workoutId, exerciseId, sortOrder, restSeconds) values (?,?,?,90)',
                   (wid, ex_id, i)).lastrowid
    c.execute("insert into workout_set(workoutExerciseId, sortOrder, type, weightKg, reps, isCompleted, completedAt) "
              "values (?, 0, 'WORK', ?, 1, 1, ?)", (we, None if ex_id == 490 else 1.0, ms(dummy_start)))

# Objectif de 3 séances par semaine depuis le début, et des paliers au développé couché créés il y a 6 semaines
# (l'app coche au lancement ceux déjà atteints).
c.execute('insert into weekly_goal(goal, effectiveFrom) values (3, ?)', (epoch_day(first_monday),))
c.execute("insert into goal(exerciseId, metric, bronzeValue, silverValue, goldValue, createdAt) "
          "values (36, 'ESTIMATED_1RM', 100, 110, 120, ?)", (ms(at(today - timedelta(weeks=6), 19)),))

# Nutrition : prise de masse à 2 650 kcal depuis 5 semaines, repas notés 25 jours sur 28, pesées 2 fois par semaine.
# Le poids monte lentement : la Tendance estime la dépense réelle et propose un objectif plus haut.
goal_start = today - timedelta(weeks=5)
c.execute("insert into nutrition_goal(effectiveFrom, kcal, proteinGrams, carbsGrams, fatGrams, activityLevel, "
          "objective, proteinPerKg, fatPerKg, isKcalManual) "
          "values (?, 2650, 150, 344, 75, 'ACTIVE', 'BULK', 2.0, 1.0, 1)", (epoch_day(goal_start),))

foods = {code: row for code, *row in c.execute('select ciqualCode, id, kcal, protein, carbs, fat from food '
                                                'where ciqualCode is not null')}
# (code Ciqual, grammes) par repas.
MEALS = {
    'BREAKFAST': [(9311, 90), (19041, 250), (13005, 120), (15202, 20)],
    'LUNCH': [(36018, 180), (9104, 300), (20006, 150), (13004, 60)],
    'SNACKS': [(19501, 200), (15000, 30), (31008, 15)],
    'DINNER': [(25996, 150), (9811, 300), (20030, 150), (7110, 60)],
}
DINNER_ALT = [(26039, 140), (4014, 350), (20030, 150), (7110, 60)]
for code in (9311, 36018, 9104):
    c.execute('update food set isFavorite = 1 where ciqualCode = ?', (code,))

noted, total = 0, 0.0
for back in range(27, -1, -1):
    day = today - timedelta(days=back)
    if back in (5, 12, 19):
        continue  # jours sans rien noter
    meals = dict(MEALS, DINNER=DINNER_ALT if back % 3 == 0 else MEALS['DINNER'])
    if back == 0:
        meals.pop('DINNER')  # captures en fin d'après-midi : le dîner n'est pas encore noté
    factor = random.uniform(0.9, 1.08)
    created = at(day, 7, 30)
    for meal, items in meals.items():
        for code, grams in items:
            food_id, kcal, protein, carbs, fat = foods[code]
            grams = round(grams * factor / 5) * 5
            created += timedelta(minutes=random.randint(1, 3))
            c.execute('insert into food_entry(date, meal, foodId, grams, kcal, protein, carbs, fat, createdAt) '
                      'values (?,?,?,?,?,?,?,?,?)', (epoch_day(day), meal, food_id, grams, kcal, protein, carbs, fat,
                                                     ms(created)))
            if back > 0:
                total += kcal * grams / 100
        created += timedelta(hours=4)
    if back > 0:
        noted += 1

saved = c.execute("insert into saved_meal(name) values ('Petit-déj habituel')").lastrowid
for i, (code, grams) in enumerate(MEALS['BREAKFAST']):
    c.execute('insert into saved_meal_item(savedMealId, foodId, grams, sortOrder) values (?,?,?,?)',
              (saved, foods[code][0], grams, i))

for back in range(34, -1, -1):
    if back % 7 not in (0, 3):
        continue
    weight = 74.4 + 0.5 * (34 - back) / 34 + random.uniform(-0.25, 0.25)
    c.execute("insert into body_measurement(type, value, measuredAt) values ('BODY_WEIGHT', ?, ?)",
              (round(weight, 1), ms(at(today - timedelta(days=back), 7, 15))))

c.commit()
c.execute('pragma wal_checkpoint(TRUNCATE)')
c.execute('pragma journal_mode = DELETE')
print('séances :', len(sessions), counts, '| full body :', full_body_id)
print('repas : moyenne', round(total / noted), 'kcal sur', noted, 'jours terminés')
print(c.execute("select count(*) from workout_set").fetchone(), c.execute('pragma foreign_key_check').fetchall())
c.close()
