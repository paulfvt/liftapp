# Cahier des charges — App de suivi muscu (Android)

Oct 6, 2026 · @Paul Fievet

## 1. Contexte et objectif

L'objectif est une app Android gratuite de suivi de musculation, publiée sur Google Play pour de vrais utilisateurs. Elle est inspirée de Strong et Hevy, et se démarque par une progression visible et motivante, puis plus tard par une IA d'optimisation des séances.

Le porteur du projet est un étudiant en Data & IA qui débute en développement mobile. Le code est produit avec Claude Code, mais il doit rester compris, relu et maîtrisé par le développeur.

**Consignes pour Claude Code à la lecture de ce document**

- Ce document est la référence fonctionnelle. Les règles des sections 3, 6 et 7 sont à copier dans le fichier `CLAUDE.md` à la racine du projet.
- Explique chaque choix technique non évident en une ou deux phrases, car le développeur apprend Android en même temps.
- En cas d'ambiguïté ou de point non couvert, pose la question au lieu d'inventer une règle métier.
- Le nom de l'app n'est pas encore choisi : utilise le nom de travail `LiftApp` et le package `com.paulfvt.liftapp`, faciles à renommer.

## 2. Périmètre et versions

Seule la V1 est à développer maintenant. Les versions suivantes sont listées pour que l'architecture ne les bloque pas, sans les implémenter.

- **V1 (MVP publié)** : catalogue d'exercices, templates de séance, séance en cours, minuteur avec notification écran verrouillé, historique, records et courbes, profil et mesures, timeline photos, fonctionnalités de motivation, widget, export CSV.
- **V1.x** : programmes prêts à l'emploi (PPL, full body débutant, 5/3/1), calculateur de disques (plate calculator), synchronisation Health Connect, partage de template par lien ou QR code.
- **V2 — Nutrition** : suivi alimentaire et besoins caloriques, en s'appuyant sur le profil déjà en place.
- **V3 — IA** : recommandations de charges et de séances via un backend Python (par exemple FastAPI), à partir de l'historique, du RPE et du volume.

**Hors périmètre V1** : comptes utilisateurs, backend, synchronisation cloud, fonctions sociales, app pour montre (Wear OS), iOS, monétisation.

## 3. Contraintes techniques et stack

L'app est native Android, hors-ligne d'abord (offline-first), sans backend en V1. Toutes les données restent sur le téléphone.

- **Langage et interface** : Kotlin, Jetpack Compose, Material 3, Navigation Compose.
- **Données** : Room (SQLite) pour les données métier, DataStore pour les réglages, coroutines et Flow pour la réactivité.
- **Injection de dépendances** : Hilt.
- **Widget** : Jetpack Glance.
- **Photos** : CameraX pour la prise de vue avec silhouette fantôme, Coil pour l'affichage, androidx.biometric pour le verrouillage optionnel.
- **Graphiques** : bibliothèque Compose légère (par exemple Vico) ou dessin sur Canvas. Claude Code propose et justifie son choix.
- **Versions** : minSdk 26 (Android 8.0). targetSdk = le niveau d'API exigé par Google Play au moment de la publication.
- **Langue et unités** : interface en français, textes dans `strings.xml` pour permettre la traduction. Unités kg/lb et cm/in au choix.

**Points techniques à traiter avec soin**

- La permission de notification (POST\_NOTIFICATIONS) est à demander sur Android 13 et plus, au moment où elle sert (première séance), pas au lancement.
- Le minuteur doit survivre à l'écran verrouillé, à la rotation et à la mise en arrière-plan. Sur Android 14 et plus, un service de premier plan (foreground service) doit déclarer un type autorisé. Claude Code doit proposer la solution la plus simple et conforme (service de premier plan ou notification chronomètre + alarme), et expliquer pourquoi. Point à vérifier dans la documentation Android à jour.
- Les notifications Live Updates d'Android 16 sont à utiliser si disponibles, avec une notification classique en secours.

## 4. Fonctionnalités V1

Chaque bloc liste le comportement attendu, puis les critères d'acceptation qui permettent de dire qu'il est terminé.

### 4.1 Écran d'accueil (tableau de bord)

- L'app s'ouvre sur un tableau de bord : anneau de l'objectif hebdomadaire, streak en semaines, prochaine séance ou dernier template utilisé avec un bouton « Démarrer », dernier record, carte souvenir (flashback) si disponible.
- Navigation principale par barre du bas : Accueil, Séances (templates), Historique, Progression, Profil.
- Si une séance est en cours, un bandeau permanent permet d'y revenir depuis n'importe quel écran.

### 4.2 Catalogue d'exercices

- Catalogue pré-rempli à partir d'une base ouverte (par exemple free-exercise-db). Licence à vérifier et à citer dans l'app avant import.
- Chaque exercice a : nom, muscles principaux, muscles secondaires, matériel, catégorie (poids libre, machine, poids du corps, cardio), type de suivi (poids × reps, reps seules, durée, distance).
- Recherche par nom et filtres par muscle et matériel.
- L'utilisateur peut créer, modifier et masquer ses propres exercices. Les exercices du catalogue ne sont pas supprimés, seulement masqués, pour ne pas casser l'historique.
- **Note permanente par exercice** (réglage de machine, placement, consignes), visible à chaque séance.
- Critère : chercher « squat », filtrer par « jambes », ajouter une note et la retrouver lors de la séance suivante.

### 4.3 Templates de séance

- Créer, modifier, dupliquer, supprimer et réordonner des templates, rangés dans des dossiers optionnels.
- Par exercice du template : nombre de séries, reps cibles, temps de repos par défaut, regroupement en superset.
- Démarrer une séance depuis un template ou une séance vide.
- En fin de séance, si la structure a changé (exercice ajouté, séries en plus), proposer de mettre à jour le template.

### 4.4 Séance en cours

- Pour chaque série, afficher la performance de la dernière fois sur cet exercice (« 80 kg × 8 ») et pré-remplir avec ces valeurs.
- Types de série : normale, échauffement, dégressive (dropset), échec. Les séries d'échauffement sont exclues des records et des statistiques.
- RPE optionnel par série (de 6 à 10, par demi-points).
- Valider une série lance automatiquement le minuteur de repos.
- Ajouter, retirer, remplacer et réordonner des exercices pendant la séance. Note libre optionnelle propre à cette séance.
- Mode salle : écran maintenu allumé, gros boutons, utilisable d'une main.
- Terminer (avec résumé) ou abandonner (avec confirmation).
- Critère clé : la séance est enregistrée en continu. Si l'app est tuée ou si le téléphone redémarre, la séance reprend exactement où elle en était, sans perte.

### 4.5 Minuteur de repos et notification

- Temps de repos par défaut réglable par exercice. Boutons « -15 s », « +30 s » et « Passer ».
- Notification persistante visible écran verrouillé pendant toute la séance : exercice en cours, série, poids et reps, compte à rebours. Actions : « Série validée » et « +30 s ».
- Alerte en fin de repos : vibration et son, chacun désactivable.
- Critère : le minuteur reste juste après 3 minutes écran verrouillé, une rotation et un passage en arrière-plan.

### 4.6 Historique, records et courbes

- Historique des séances par date, avec le détail de chaque séance. Modification et suppression possibles après coup.
- Records par exercice : charge maximale, 1RM estimé maximal, meilleur volume sur une série, maximum de reps à une charge donnée.
- 1RM estimé par la formule d'Epley, 1RM = poids × (1 + reps / 30), calculé uniquement pour les séries de 12 reps ou moins.
- Courbes par exercice : 1RM estimé, charge maximale et volume, sur 1 mois, 3 mois, 1 an ou tout.
- Critère : modifier une ancienne séance recalcule correctement les records.

### 4.7 Progression visible et motivante

- **Objectif hebdomadaire et streak** : l'utilisateur fixe un nombre de séances par semaine (3 proposé par défaut). Une semaine compte dans la streak si l'objectif est atteint. Pas de streak quotidienne : les jours de repos ne doivent jamais être pénalisés.
- **Anneau hebdomadaire** : se remplit à chaque séance (par exemple 2/3).
- **Résumé de fin de séance** : durée, tonnage, nombre de séries, records battus, comparaison avec la dernière exécution du même template (+6 % de volume).
- **Célébration des records** : animation discrète au moment où un record est battu pendant la séance.
- **Souvenirs (flashbacks)** : carte sur l'accueil comparant une performance actuelle à celle d'il y a 1, 3, 6 ou 12 mois sur le même exercice.
- **Carte du corps (muscle heatmap)** : silhouette avant et arrière colorée selon le nombre de séries effectives par muscle sur 7 jours (muscle principal = 1 série, muscle secondaire = 0,5). On compte des séries plutôt que du tonnage, car le tonnage favorise les exercices lourds comme la presse à cuisses.
- **Récap mensuel** : séances, tonnage traduit en équivalence parlante, records du mois, exercice favori. Export en image partageable via le menu de partage Android.
- **Objectifs à paliers** : objectif chiffré sur un exercice (par exemple 100 kg au squat) avec paliers bronze, argent et or, et barre de progression basée sur le 1RM estimé.
- **Badges** : jalons comme 1re, 10e, 50e et 100e séance, 4, 12 et 52 semaines d'objectif tenu, clubs des 100 kg au développé couché, 140 au squat et 180 au soulevé de terre.
- **Projection** : « à ce rythme, environ X kg vers telle période », affichée en fourchette et seulement avec au moins 6 séances sur 4 semaines pour l'exercice. Le modèle doit tenir compte du ralentissement de la progression. Jamais de date précise présentée comme certaine.
- Aucun système ne récompense le volume brut (points ou XP au tonnage), pour ne pas pousser au surentraînement.

### 4.8 Profil et mesures

- Informations fixes, toutes facultatives : prénom, taille, date de naissance, sexe.
- Mesures datées : poids, tour de bras, poitrine, taille, hanches, cuisses, mollets, taux de masse grasse avec sa méthode (balance, pince, estimation). Chaque saisie crée une nouvelle entrée, on n'écrase jamais l'ancienne valeur.
- Courbe de poids avec moyenne mobile sur 7 jours en plus des points bruts.
- Pas d'IMC (indice de masse corporelle), trompeur pour les pratiquants musclés.
- Option pour masquer entièrement le poids de corps dans toute l'app.

### 4.9 Timeline photos

- Prise de photo dans l'app avec la silhouette fantôme (ghost overlay) de la photo précédente en transparence, pour garder le même cadrage. Import depuis la galerie possible.
- Photos stockées dans l'espace privé de l'app, invisibles dans la galerie du téléphone.
- Timeline par date et comparaison avant/après avec curseur.
- Verrouillage optionnel par empreinte ou code. Option pour masquer la section.

### 4.10 Widget d'écran d'accueil

- Un seul widget qui change d'état automatiquement, décliné en petite et moyenne taille.
- **État hors séance** : anneau hebdomadaire, streak, prochaine séance avec un bouton « Démarrer », dernier record.
- **État en séance** : exercice en cours, série, poids et reps, minuteur de repos, action « Série validée ».
- Le compte à rebours utilise le composant chronomètre du système, sans rafraîchir le widget chaque seconde.

### 4.11 Réglages et données

- Unités, temps de repos par défaut, son et vibration, thème clair ou sombre, objectif hebdomadaire, masquage du poids et des photos.
- Export CSV de l'historique et des mesures.
- Sauvegarde et restauration complètes dans un fichier local, pour qu'un changement de téléphone ne fasse pas perdre l'historique.

## 5. Modèle de données (proposition à valider)

Ce modèle est une base de départ que le développeur relit et valide avant tout code. Toutes les statistiques, les records et la future IA en dépendent.

| Entité (table Room) | Rôle | Champs clés |
| --- | --- | --- |
| Exercise | Exercice du catalogue ou créé | id, name, equipment, category, trackingType, isCustom, isHidden, note |
| ExerciseMuscle | Lien exercice ↔ muscle | exerciseId, muscle, role (primary ou secondary) |
| TemplateFolder | Dossier de templates | id, name, sortOrder |
| WorkoutTemplate | Template de séance | id, folderId, name, sortOrder |
| TemplateExercise | Exercice planifié dans un template | id, templateId, exerciseId, sortOrder, setCount, targetReps, restSeconds, supersetGroup |
| Workout | Séance réalisée ou en cours | id, templateId (nullable), name, startedAt, endedAt, status (in\_progress, finished, discarded), note |
| WorkoutExercise | Exercice effectué dans une séance | id, workoutId, exerciseId, sortOrder, supersetGroup, note |
| WorkoutSet | Série effectuée | id, workoutExerciseId, sortOrder, type, weightKg, reps, durationSec, distanceM, rpe, isCompleted, completedAt |
| PersonalRecord | Cache des records | exerciseId, recordType, value, setId, achievedAt |
| BodyMeasurement | Mesure corporelle datée | id, type, value, method, measuredAt |
| ProgressPhoto | Photo de progression | id, filePath, pose (face, profil, dos), takenAt |
| Goal | Objectif à paliers | id, exerciseId, metric, bronzeValue, silverValue, goldValue, createdAt, achieved dates par palier |
| WeeklyGoal | Historique de l'objectif hebdo | goal, effectiveFrom |
| Badge | Badge débloqué | code, unlockedAt |

Le profil fixe (taille, date de naissance, sexe) et les réglages vont dans DataStore.

**Règles du modèle**

- Valeurs stockées en unités métriques (kg, cm, mètres, secondes), conversion uniquement à l'affichage.
- Horodatages en UTC (Instant), affichage dans le fuseau du téléphone.
- Les records sont un cache recalculable à partir des séries, jamais la source de vérité.
- L'objectif hebdomadaire est historisé : si l'utilisateur le change, les semaines passées gardent l'objectif de l'époque pour le calcul de la streak.
- Clés étrangères et index sur les colonnes de jointure. Un exercice utilisé dans l'historique n'est jamais supprimé, seulement masqué.
- Le modèle doit pouvoir accueillir la nutrition (V2) et l'IA (V3) par ajout de tables, sans modifier les tables existantes.

## 6. Architecture et règles de qualité

L'architecture suit les recommandations officielles Android : une couche interface, une couche données, et une couche domaine pour les calculs métier.

- **Interface (UI)** : écrans Compose sans logique métier, un ViewModel par écran qui expose un état (StateFlow).
- **Domaine** : calculs purs en Kotlin sans dépendance Android (1RM, records, streak, carte du corps, projection, badges). Ils sont faciles à tester.
- **Données** : DAO Room, repositories comme unique point d'accès aux données.
- Organisation des packages par fonctionnalité (`workout`, `exercise`, `history`, `progress`, `profile`, `widget`), avec un package `core` partagé.

**Règles non négociables**

- **Migrations Room** : `exportSchema = true`, schémas versionnés dans Git, une migration écrite et testée pour chaque changement de schéma. `fallbackToDestructiveMigration()` est interdit : il efface les données des utilisateurs.
- **Tests unitaires obligatoires** sur toute la couche domaine (1RM, records, streak, heatmap, projection, conversions d'unités), cas limites compris : 0 rep, série d'échauffement, semaine à cheval sur deux mois, changement d'objectif hebdo.
- **Tests de migration** Room et test de reprise d'une séance en cours après redémarrage de l'app.
- **Pas de nouvelle dépendance** sans l'expliquer et demander validation.
- **Accessibilité** : descriptions des éléments pour les lecteurs d'écran, zones tactiles d'au moins 48 dp, contrastes suffisants, thèmes clair et sombre.
- **Code lisible** : noms en anglais, commentaires en français sur les parties non évidentes, pas de code mort.
- **Lint** : le projet compile sans avertissement bloquant (Android Lint, ktlint ou detekt).

## 7. Méthode de travail et plan par phases

On avance une phase à la fois, dans l'ordre. Une phase n'est terminée que quand ses tests passent et que le développeur l'a testée sur un vrai téléphone.

**Règles de travail pour Claude Code**

- Avant chaque phase, présenter un plan court (fichiers créés ou modifiés, choix techniques) et attendre la validation.
- Avancer par petites étapes, avec un commit Git par étape et des messages de commit clairs.
- Tenir à jour un fichier `PROGRESS.md` : phase en cours, ce qui est fait, décisions prises, problèmes connus.
- Signaler tout écart avec ce cahier des charges au lieu de le faire en silence.

**Phases**

1. **Mise en place** : projet Android, Hilt, Room, Navigation, thème Material 3, dépôt Git, `CLAUDE.md`, build et tests automatiques avec GitHub Actions.
2. **Modèle de données et catalogue** : entités, DAO, import du catalogue d'exercices, recherche, filtres, exercices personnalisés, notes.
3. **Templates** : création, édition, dossiers, supersets.
4. **Séance en cours** : saisie, performance précédente, types de série, RPE, enregistrement continu et reprise après redémarrage.
5. **Minuteur et notification** : repos automatique, notification écran verrouillé avec actions, mode salle.
6. **Historique, records et courbes** : liste, détail, édition, recalcul des records, graphiques.
7. **Profil, mesures et photos** : mesures datées, moyenne mobile, timeline photos avec silhouette fantôme et verrouillage.
8. **Motivation** : objectif hebdo et streak, anneau, résumé de fin de séance, célébration des records, souvenirs, carte du corps, objectifs à paliers, badges, récap mensuel, projection.
9. **Tableau de bord et widget** : écran d'accueil final, widget à deux états.
10. **Réglages et données** : unités, options de masquage, export CSV, sauvegarde et restauration.
11. **Préparation à la publication** : icône, écran d'accueil au premier lancement (onboarding), politique de confidentialité, version de release signée.

## 8. Publication Google Play

La publication demande plusieurs étapes administratives à anticiper dès le début du développement. Les règles ci-dessous sont à revérifier sur la documentation Google Play au moment de publier.

- Compte développeur Google Play : environ 25 $ en paiement unique, avec vérification d'identité.
- Test fermé (closed testing) obligatoire pour un compte personnel récent : au moins 12 testeurs pendant 14 jours consécutifs avant l'accès à la production. Recruter les testeurs avant la fin de la phase 11.
- Politique de confidentialité hébergée en ligne (une page GitHub Pages suffit), même si aucune donnée ne quitte le téléphone.
- Formulaire de sécurité des données (Data safety) rempli de façon exacte.
- Déclaration et justification du type de service de premier plan si l'app en utilise un.
- Fiche Play Store : description, captures d'écran, icône, bannière.
- Mention de la licence du catalogue d'exercices dans l'app si elle l'exige.

## 9. Hypothèses à valider et backlog

Ces points ont été tranchés par défaut pour ne pas bloquer le travail. Ils sont à confirmer par le développeur.

- [ ] L'app s'ouvre sur le tableau de bord plutôt que sur la liste des templates.
- [ ] L'objectif hebdomadaire est fixé par l'utilisateur, avec 3 séances proposées par défaut.
- [ ] Le catalogue part de free-exercise-db, sous réserve de sa licence.
- [ ] Le modèle de données de la section 5 est relu et validé avant la phase 2.
- [ ] Le nom définitif de l'app et le nom de package.

**Backlog (idées à évaluer, hors V1)**

- Profil de salle avec le matériel disponible, et remplacement d'un exercice par un équivalent si une machine est prise.
- Progression automatique par règle simple (double progression), qui servira aussi de référence (baseline) pour juger l'IA de la V3.
- Détection de stagnation et suggestion de semaine allégée (deload).
- Saisie vocale des séries.
