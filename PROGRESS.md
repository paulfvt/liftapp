# Avancement

## Phase en cours : 6 — Historique, records et courbes

Plan validé le 21 octobre 2026.

- [x] Logique pure testée : 1RM, records, points des courbes, périodes, regroupement par mois, échelle des courbes.
- [x] Records recalculés en fin de séance, après modification et après suppression d'une séance (tests instrumentés).
- [x] Onglet Historique : liste par mois, détail, suppression.
- [x] Modification d'une séance passée.
- [x] Onglet Progrès et écran Progression d'un exercice (records, courbes avec Vico). Vérifié sur l'émulateur.
- [ ] Relancer les tests instrumentés après les derniers ajustements d'affichage, et la suppression sur l'émulateur.
- [ ] Test sur le S25+.

## Phase 5 — Minuteur et notification : terminée le 21 octobre 2026
- Sans service de premier plan : notification avec compte à rebours dessiné par le système et alarme exacte pour
  l'alerte de fin de repos (voir « Décisions techniques »).
- Base v4 (début et fin du repos dans la séance), migration testée.
- Valider une série lance le repos ; −15 s, +30 s, Passer ; actions « Série validée » et « +30 s » dans la
  notification, Live Update sur Android 16.
- Permissions demandées à la première séance ; son et vibration de fin de repos désactivables.
- Mode salle : écran allumé, une série à la fois, gros boutons.
- 97 tests JVM et 25 tests instrumentés verts. Vérifié sur l'émulateur (alarme à l'heure écran verrouillé, son joué
  par le système), validé par le développeur le 21 octobre 2026.

## Phase 4 — Séance en cours : terminée le 21 octobre 2026
- Base v3 (repos et reps cibles dans la séance), migration testée.
- Logique pure testée : performance précédente, pré-remplissage, saisie des valeurs, résumé.
- `WorkoutDao`, `WorkoutRepository`, test de reprise après redémarrage.
- Démarrage (aperçu du template, séance vide), écran de séance, bandeau EN DIRECT, reprise automatique
  au lancement.
- Fin de séance : résumé, mise à jour du template, abandon. Thèmes clair et sombre.
- 79 tests JVM et 20 tests instrumentés, testé sur le S25+.
- Choix kg/lb reporté en phase 10.

## Phase 3 — Templates : terminée le 11 octobre 2026
- Design system « ON AIR » validé (`design-system/`) et appliqué au thème Compose.
- Onglet Séances : templates hors dossier puis dossiers repliables ; créer, renommer, dupliquer, déplacer,
  supprimer ; glisser-déposer dans une même section.
- Éditeur de template : séries détaillées (type et reps cibles par série), repos par exercice, catalogue en mode
  sélection multiple. Base en version 2 avec la première migration, testée.
- 53 tests JVM et 12 tests instrumentés verts, testé sur le S25+.
- Supersets : hors V1 (décision du 6 octobre 2026), donc absents de cette phase.

## Phase 2 — Modèle de données et catalogue : terminée le 7 octobre 2026
- Base Room v1 avec toutes les tables du modèle (schéma exporté dans `app/schemas/`).
- Catalogue : 753 exercices de free-exercise-db, traduits en français, 1 500 images WebP (~17 Mo).
  Import au premier lancement, réimport sans perte de note ni de masquage quand `CATALOG_VERSION` augmente.
- Écrans : liste avec recherche sans accents et filtres, fiche avec images animées, note permanente et masquage,
  exercices personnalisés.
- Tests JVM et instrumentés verts, testé sur le S25+.
- Reste ouvert, non bloquant : relecture des noms traduits (`tools/catalog/noms-a-relire.csv`).

## Phase 1 — Mise en place : terminée le 6 octobre 2026

## Décisions techniques
- **Dossier hors OneDrive** (`C:\Users\paulf\dev\liftapp`) : la synchronisation verrouille les fichiers de build Gradle.
- **Kotlin intégré à AGP 9** : le plugin `org.jetbrains.kotlin.android` n'est plus appliqué.
- **KSP plutôt que kapt** pour Hilt et Room : plus rapide, et kapt est en fin de vie.
- **Icônes en vecteurs XML** (Material Icons, Apache 2.0) plutôt que la bibliothèque `material-icons-extended`,
  qui n'est plus mise à jour et pèse lourd.
- **ktlint** (plugin `org.jlleitschuh.gradle.ktlint`) pour le style, en plus d'Android Lint.
- **Java 25 pour Gradle** : fichier `gradle/gradle-daemon-jvm.properties` créé par Android Studio ; la CI utilise
  aussi Java 25.
- **ktlint en style `android_studio`** : pas de virgule finale, signatures sur une ligne si elles tiennent.
  `./gradlew ktlintFormat` corrige automatiquement.
- **Packages `home`, `template` et `navigation`** ajoutés à la liste du cahier des charges (accueil, templates et
  squelette de navigation n'avaient pas de package dédié).

- **Catégorie déduite du matériel**, avec une catégorie « Autre » en plus des quatre du cahier des charges
  (élastiques, swiss ball, traîneau…).
- **Type de suivi** : répétitions seules pour le poids du corps, l'élastique et le swiss ball ; distance ou durée
  choisies à la main pour une trentaine d'exercices (cardio, gainage, strongman).
- **Recherche faite en mémoire** (753 exercices) plutôt qu'en SQL : plus simple, et la normalisation des accents
  est testée en Kotlin.
- **Note éditée dans une boîte de dialogue** avec Enregistrer/Annuler, plutôt qu'en enregistrement automatique.
- **Design « ON AIR »** : `design-system/styles.css` est la référence, reportée dans `core/designsystem/theme/`.
  Polices Barlow et Barlow Condensed embarquées dans `res/font` (licence OFL, à citer dans « À propos ») plutôt que
  téléchargées, pour fonctionner hors connexion.
- **Capitales uniquement pour les titres d'écran** (`ScreenTitle`) : Compose ne sait pas mettre un style en
  capitales, donc les boutons restent en minuscules, contrairement au guide de style.
- **Éditeur de template avec « Enregistrer »** : les changements restent en mémoire jusqu'à l'enregistrement, et
  quitter avec des changements demande confirmation. Les exercices du template sont réécrits en entier à chaque
  enregistrement.
- **Glisser-déposer limité à une section** : un template change de dossier par le menu « Déplacer vers un dossier ».
  Les dossiers se réordonnent par « Monter » / « Descendre » dans leur menu, pas par glisser-déposer.
- **Accessibilité du glisser-déposer** : chaque carte propose « Monter » et « Descendre » au lecteur d'écran.
- **Retour du catalogue en mode sélection** par le `SavedStateHandle` de l'entrée de navigation précédente (clé
  `PICKED_EXERCISE_IDS_KEY`), mécanisme standard de Navigation pour renvoyer un résultat. Attention : ce n'est pas
  le `SavedStateHandle` reçu par le ViewModel ; le résultat est lu dans `templateScreens` puis transmis au ViewModel.
- **Suppression d'un dossier** : ses templates rejoignent la fin des templates hors dossier.
- **Séries détaillées dans les templates** (écart avec le cahier, validé le 8 octobre 2026) : la table `template_set`
  remplace `setCount` et `targetReps` de `template_exercise`. La migration 1 → 2 recrée `template_exercise` (SQLite
  d'Android 8 ne sait pas supprimer une colonne) et convertit « 4 séries de 8 » en 4 séries de travail à 8 reps.
  Testée avec `MigrationTestHelper` (dépendance de test `room-testing`, validée).
- **Numérotation des séries** : seules les séries de travail sont numérotées ; échauffement « É », dégressive « D ».
- **Écriture continue de la séance** : chaque saisie part dans une file d'écritures qui tourne dans le scope de
  l'application (`WorkoutViewModel`). Une seule coroutine la vide, donc l'ordre de frappe est respecté, et une saisie
  faite juste avant de quitter l'écran n'est pas perdue.
- **Champs de saisie de la séance** : le texte tapé n'est jamais remplacé pendant que le champ a le focus (la base
  renvoie la valeur avec un temps de retard) ; il est resynchronisé quand le champ perd le focus.
- **Séance en plein écran** : pas de barre du bas sur la séance ni sur le catalogue ouvert depuis elle. Ailleurs,
  un bandeau EN DIRECT ramène à la séance. Au lancement, une séance encore en cours est rouverte directement.
- **Séance abandonnée** : gardée avec le statut `DISCARDED` (modèle du cahier), ignorée par « Précédent ».
- **Couleur « success »** du guide de style ajoutée au thème Compose (`ExtendedColors`), Material 3 n'en ayant pas.
- **Minuteur sans service de premier plan** (validé le 21 octobre 2026) : aucun type de service ne convient
  proprement (`health` exige une permission de capteur ou « Activité physique » inutile ici, `specialUse` est
  examiné par Google, `shortService` est limité à 3 min). L'heure de fin du repos est enregistrée dans la séance ;
  la notification affiche un compte à rebours dessiné par Android à partir de cette heure, donc juste même quand
  l'app ne tourne pas. Une alarme exacte (`SCHEDULE_EXACT_ALARM`, accordée par l'utilisateur) déclenche l'alerte de
  fin de repos ; sans elle, alarme inexacte qui peut arriver en retard écran verrouillé.
- **Règles du repos** : valider une série lance le repos de son exercice (2 min si non réglé, rien si 0 s) ; pas de
  repos après la dernière série de la séance ; décocher une série ne touche pas au repos ; +30 s sur un repos terminé
  relance 30 s. L'alerte n'est jouée que si l'alarme arrive moins d'une minute après la fin du repos.
- **Alerte de fin de repos** : la vibration est jouée par l'app (usage « alarme », rien en silencieux), car un canal
  de notification garde la vibration choisie à sa création. Le son passe par une notification brève « Repos terminé »
  sur un canal à part, publiée seulement si le son est activé : un son joué directement par l'app est coupé par
  Android quand elle est en arrière-plan (constaté sur l'émulateur écran verrouillé). Vibreur, silencieux et « Ne pas
  déranger » sont alors gérés par Android.
- **Son et vibration réglés depuis le menu de la séance** (écart temporaire avec le cahier § 4.5 / § 4.10) : ils
  rejoindront l'écran Réglages en phase 10. Stockés dans DataStore (`WorkoutSettingsRepository`).
- **Après un redémarrage du téléphone**, notification et alarme reviennent à la réouverture de l'app (pas de
  récepteur de démarrage, qui demanderait une permission de plus). La séance et l'heure de fin du repos sont, elles,
  gardées en base.
- **Notification** : canal sans son ni vibration, visible écran verrouillé ; hors repos, elle affiche la durée de la
  séance. « Série validée » n'est proposée que si la série suivante a ses valeurs. Un appui ouvre la séance.
- **Mode salle** : interrupteur mémorisé dans le menu de la séance. Écran maintenu allumé, seule la série à faire est
  affichée, boutons − et + de 72 dp (2,5 kg, 1 rep, 15 s, 100 m en km ou 5 m), valeurs aussi modifiables au clavier.
  Les pas sont appliqués à la valeur en base pour que des appuis rapides s'additionnent.
- **Records** (validé le 21 octobre 2026) : les séries dégressives comptent, seuls les échauffements sont exclus.
  Une série à 1 rep a un 1RM égal au poids soulevé ; Epley au-delà, jusqu'à 12 reps. Les records sont toujours
  recalculés en entier pour un exercice (fin de séance, modification, suppression), jamais modifiés au coup par coup.
- **Records des exercices sans charge** (écart avec le cahier § 4.6, validé le 21 octobre 2026) : poids du corps =
  reps max et lest max, durée = durée max, distance = distance max, chacun avec sa courbe.
- **Graphiques avec Vico** (`compose-m3`, Apache 2.0, validé le 21 octobre 2026) : axes, zoom et valeur au toucher
  sans les recoder ; servira aussi aux mesures (phase 7) et à la projection (phase 8).

## Questions ouvertes

## Problèmes connus
- `kotlinx-coroutines-test` doit rester à la même version que `kotlinx-coroutines-core` utilisé par l'app
  (1.10.2, imposé par AndroidX), sinon les tests instrumentés échouent avec un `NoSuchMethodError`.
- Avertissement lint `OldTargetApi` (targetSdk 36 alors que l'API 37 existe) : volontaire, c'est le niveau exigé
  par Google Play. À revoir avant la publication.
