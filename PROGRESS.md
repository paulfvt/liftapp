# Avancement

## En cours : préparation du test fermé (une partie de la phase 11)

Décidé le 21 octobre 2026 : le test fermé passe avant la phase 7, car Google exige 12 testeurs inscrits pendant
14 jours d'affilée avant la production. Les phases 7 à 9 avancent pendant le test. Nom et identifiant gardés :
« LiftApp », `com.paulfvt.liftapp` (l'identifiant ne pourra plus changer après le premier envoi).

Plan validé le 21 octobre 2026 :
1. Version de release signée : clé d'envoi créée par le développeur, mots de passe dans `keystore.properties`
   (hors Git), Play App Signing. Release testée sur l'émulateur (R8).
2. Pendant le test, l'app s'ouvre sur Séances et l'onglet Accueil est masqué (retour en phase 9).
3. Onglet Profil réduit pour l'instant à « À propos » : version et licences (free-exercise-db, Barlow, bibliothèques
   Apache 2.0), écrit à la main sans nouvelle dépendance.
4. Premier lancement : un seul écran de bienvenue (ce que fait l'app, données gardées sur le téléphone,
   « Commencer »). Unités (phase 10) et objectif hebdomadaire (phase 8) s'y ajouteront.
5. Icône ON AIR : maquette dans `design-system/` à valider, puis icône adaptative en vecteur et version 512 px.
6. Politique de confidentialité en français, hébergée sur un petit dépôt public à part (le dépôt de l'app est privé).
7. Fiche Play Store : descriptions, captures, bannière, réponses préparées pour « Sécurité des données » et la
   classification du contenu.

À faire par le développeur : compte développeur Google Play (25 $, vérification d'identité), 12 testeurs avec une
adresse Gmail, clé d'envoi et `keystore.properties` (commande dans le README).

Fait le 21 octobre 2026 :
- Étape 1 : signature configurée. Release minifiée par R8 testée sur l'émulateur (catalogue et images, template,
  séance, minuteur, fin de séance, historique, courbes) sans plantage.
- Étapes 2 à 4 : app ouverte sur Séances (4 onglets), Profil → « À propos » → texte des licences, écran de bienvenue.
- Étape 5 : icône « Néon » (piste B de `design-system/icon.html`, choisie par le développeur) en icône adaptative
  avec couche monochrome, vérifiée sur l'émulateur ; version Play Store dans `docs/play-store/icon-512.png`.
- Étape 6 : brouillon `docs/politique-de-confidentialite.md`, nom et adresse de contact à compléter.
- Étape 7 : brouillon `docs/fiche-play-store.md` (textes, réponses aux formulaires).
- Reste : mise en ligne de la politique, image de présentation et captures, test sur le S25+.

## Phase 6 — Historique, records et courbes : terminée le 21 octobre 2026
- Onglet Historique : séances par mois, détail, modification (en mémoire jusqu'à « Enregistrer ») et suppression.
- Records recalculés en entier pour les exercices touchés : fin de séance, modification, suppression.
- Onglet Progrès et écran Progression d'un exercice : records datés, courbes (Vico) sur 1 mois, 3 mois, 1 an ou tout,
  reps max par charge. Accessible aussi depuis le détail d'une séance et la fiche d'un exercice.
- 118 tests JVM et 31 tests instrumentés verts. Vérifié sur l'émulateur, validé par le développeur le 21 octobre 2026.

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
- **Signature de la release** : `keystore.properties` absent (CI, autre PC) → release signée avec la clé de debug,
  installable pour tester R8 mais refusée par Google Play, donc impossible à publier par erreur.
- **Package `onboarding`** ajouté à la liste du cahier des charges (écran de bienvenue, enrichi en phases 8 et 10).
  La préférence `welcome_done` est lue avant d'afficher quoi que ce soit : tant qu'elle ne l'est pas (quelques
  millisecondes), seul le fond de la fenêtre est visible.
- **Licences** : textes officiels Apache 2.0 et OFL 1.1 copiés depuis les notices d'Android Studio, copyright Barlow
  lu dans les fichiers de police. Rangés dans `res/raw` avec un paragraphe par ligne pour se replier à l'écran.
  Le catalogue est dans le domaine public (Unlicense) : crédité, sans texte de licence à joindre.
- **`BuildConfig` activé** pour afficher la version dans « À propos ».
- **Icône en vecteurs** : la lueur du néon est obtenue en repassant le même tracé quatre fois (halo large et
  transparent, puis cœur clair), car les vecteurs Android n'ont pas de flou. Version Play Store rendue depuis le même
  SVG par Chrome sans fenêtre (`--headless --screenshot`), sans nouvel outil à installer.
- **Aucune permission internet** dans l'app (vérifié dans le manifeste final) : argument de la politique de
  confidentialité et du formulaire « Sécurité des données » (aucune donnée collectée).

## Questions ouvertes
- Politique de confidentialité : nom affiché et adresse de contact (publics), nom du dépôt public qui l'hébergera.

## Problèmes connus
- `kotlinx-coroutines-test` doit rester à la même version que `kotlinx-coroutines-core` utilisé par l'app
  (1.10.2, imposé par AndroidX), sinon les tests instrumentés échouent avec un `NoSuchMethodError`.
- Avertissement lint `OldTargetApi` (targetSdk 36 alors que l'API 37 existe) : volontaire, c'est le niveau exigé
  par Google Play. À revoir avant la publication.
- Tests instrumentés : Gradle peut afficher `BUILD SUCCESSFUL` alors que l'APK de test ne s'est pas installé
  (émulateur pas encore prêt). Toujours vérifier le nombre de tests passés dans
  `app/build/outputs/androidTest-results/connected/debug/*.xml` (31 au 21 octobre 2026).
- Le filtre `-Pandroid.testInstrumentationRunnerArguments.class=…` n'a lancé aucun test : lancer toute la suite.
- Émulateur `Medium_Phone_API_37.0` sans accès root ni `sqlite3` : pour préparer des données, récupérer la base avec
  `adb exec-out run-as com.paulfvt.liftapp cat databases/lift.db`, la modifier sur le PC, puis la remettre avec
  `run-as … cp` depuis `/data/local/tmp` (app arrêtée, fichiers `-wal` et `-shm` supprimés).
