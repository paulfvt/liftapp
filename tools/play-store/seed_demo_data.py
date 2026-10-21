# Données de démonstration pour les captures du Play Store : 8 semaines de Push / Pull / Jambes.
# Usage : python seed_demo_data.py lift.db (copie de la base de l'émulateur, schéma Room v4).
# Efface templates, séances et records de cette copie avant d'écrire les données de démonstration.
# Les records ne sont pas écrits ici : supprimer dans l'app la séance « À supprimer » les fait recalculer.
# Procédure complète dans docs/fiche-play-store.md.
import random
import sqlite3
import sys
from datetime import datetime, timedelta, timezone

db = sys.argv[1]
c = sqlite3.connect(db)
c.execute('pragma foreign_keys = on')
random.seed(7)

for table in ['personal_record', 'workout_set', 'workout_exercise', 'workout', 'template_set', 'template_exercise',
              'workout_template', 'template_folder']:
    c.execute(f'delete from {table}')

# (id, repos en s, séries de travail, reps cibles, charge de départ, charge d'arrivée, pas, échauffements)
PUSH = [
    (36, 180, 3, 8, 70, 82.5, 2.5, [(40, 10), (60, 5)]),   # Développé couché à la barre
    (304, 120, 3, 10, 24, 28, 2, []),                      # Développé incliné aux haltères
    (668, 150, 3, 8, 40, 47.5, 2.5, []),                   # Développé militaire debout
    (575, 90, 3, 12, 8, 10, 1, []),                        # Élévations latérales aux haltères
    (713, 90, 3, 12, 20, 25, 2.5, []),                     # Extension triceps à la corde
]
PULL = [
    (490, 150, 3, 8, None, None, None, []),                # Traction (poids du corps)
    (71, 150, 3, 8, 60, 72.5, 2.5, []),                    # Rowing barre buste penché
    (741, 120, 3, 10, 55, 62.5, 2.5, []),                  # Tirage vertical prise large
    (37, 90, 3, 10, 30, 35, 2.5, []),                      # Curl à la barre
    (277, 90, 3, 12, 14, 16, 2, []),                       # Curl marteau
]
LEGS = [
    (56, 180, 3, 6, 90, 107.5, 2.5, [(60, 8)]),            # Squat à la barre
    (529, 150, 3, 8, 80, 95, 2.5, []),                     # Soulevé de terre roumain
    (366, 120, 3, 10, 160, 190, 10, []),                   # Presse à cuisses
    (559, 90, 3, 12, 40, 47.5, 2.5, []),                   # Leg curl assis
    (654, 90, 4, 12, 60, 75, 5, []),                       # Mollets debout à la machine
]
FULL_BODY = [(56, 180, 3, 8), (36, 180, 3, 8), (490, 150, 3, 8), (71, 150, 3, 10)]

ms = lambda dt: int(dt.timestamp() * 1000)

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


# Lundi, mercredi, vendredi du 24 août au 19 octobre 2026, en tournant Jambes → Push → Pull.
days = []
d = datetime(2026, 8, 24, tzinfo=timezone.utc)
while d <= datetime(2026, 10, 19, tzinfo=timezone.utc):
    if d.weekday() in (0, 2, 4):
        days.append(d)
    d += timedelta(days=1)
skipped = {days[10], days[17]}  # deux séances manquées, comme dans la vraie vie
order = ['Jambes', 'Push', 'Pull']
sessions = [(day, order[k % 3]) for k, day in enumerate(days) if day not in skipped]
counts = {name: sum(1 for _, n in sessions if n == name) for name in order}
seen = {name: 0 for name in order}

for day, name in sessions:
    template_id, exercises = plans[name]
    t = seen[name] / max(counts[name] - 1, 1)
    seen[name] += 1
    start = day.replace(hour=17, minute=random.choice([5, 15, 30, 40]))
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
dummy_start = datetime(2026, 10, 21, 8, 0, tzinfo=timezone.utc)
wid = c.execute("insert into workout(templateId, name, startedAt, endedAt, status) values "
                "(NULL, 'À supprimer', ?, ?, 'FINISHED')", (ms(dummy_start), ms(dummy_start + timedelta(minutes=5)))).lastrowid
all_ids = [ex[0] for plan in (PUSH, PULL, LEGS) for ex in plan]
for i, ex_id in enumerate(all_ids):
    we = c.execute('insert into workout_exercise(workoutId, exerciseId, sortOrder, restSeconds) values (?,?,?,90)',
                   (wid, ex_id, i)).lastrowid
    c.execute("insert into workout_set(workoutExerciseId, sortOrder, type, weightKg, reps, isCompleted, completedAt) "
              "values (?, 0, 'WORK', ?, 1, 1, ?)", (we, None if ex_id == 490 else 1.0, ms(dummy_start)))

c.commit()
c.execute('pragma wal_checkpoint(TRUNCATE)')
c.execute('pragma journal_mode = DELETE')
print('séances :', len(sessions), counts, '| full body :', full_body_id)
print(c.execute("select count(*) from workout_set").fetchone(), c.execute('pragma foreign_key_check').fetchall())
c.close()
