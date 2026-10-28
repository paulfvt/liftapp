# Avancement

## Prochaine étape : phase 9 — Tableau de bord et widget (plan proposé le 28 octobre 2026, feu vert attendu)

Plan proposé (un commit par étape) :
1. Prochaine séance, calcul testé : template suivant le dernier utilisé dans son dossier, sinon le dernier utilisé ;
   template supprimé, dossier d'un seul template, aucun historique (premier template de la liste), aucun template
   (« Séance vide »).
2. Dernier record, calcul testé : le dernier record battu tous exercices confondus, avec sa date.
3. Écran Accueil (package `home`) : semaine (anneau et streak), prochaine séance avec « Démarrer », dernier record,
   souvenir. Onglet Accueil rétabli en premier, l'app s'ouvre dessus.
4. Widget hors séance (Glance) : anneau dessiné en image, streak, prochaine séance avec « Démarrer », dernier record ;
   petite et moyenne taille.
5. Widget en séance : exercice, série, poids et reps, compte à rebours du repos par le `Chronometer` d'Android
   (`AndroidRemoteViews`, sans rafraîchir le widget chaque seconde), « Série validée » par le récepteur de la
   notification.
6. Mises à jour du widget : changements de la séance et fin de repos (`WorkoutSessionSync`), fin ou modification
   d'une séance, mise à jour périodique de secours pour le changement de semaine.
7. Aperçu dans le sélecteur de widgets, politique de confidentialité, fiche Play Store, tests, émulateur,
   `PROGRESS.md`.

Décidé le 28 octobre 2026 (détail dans `CLAUDE.md`) : répartition Accueil et Progrès selon le cahier, « Démarrer »
du widget lance la séance, Glance 1.2.0 (`glance-appwidget` et `glance-material3`) validé.

## Phase 8 — Motivation : terminée le 28 octobre 2026

Testée et validée par le développeur sur le S25+ le 28 octobre 2026.

Plan validé le 28 octobre 2026, fait en une traite à la demande du développeur (un commit par étape).
Règles métier tranchées le même jour : voir `CLAUDE.md`. Pas de nouvelle dépendance ni de changement de schéma :
`goal`, `weekly_goal` et `badge` existent depuis la v1 (seuls un DAO et des requêtes ont été ajoutés).

Fait :
- Objectif hebdomadaire historisé (choix sur l'écran de bienvenue, puis depuis la carte de la semaine, valable dès la
  semaine en cours) et streak en semaines ; anneau `WeeklyRing` du guide de style.
- Records battus calculés en rejouant l'historique (`recordBreaks`, `liveRecords`) : célébration pendant la séance
  (bandeau ambre avec rebond, vibration, annonce TalkBack), jamais à la première séance sur un exercice.
- Résumé de fin de séance : records battus avec l'ancienne valeur, volume comparé à la dernière séance du même
  template, paliers et badges débloqués.
- Carte du corps (7 jours glissants, cardio exclu) avec les tracés MuscleMap, silhouette de femme si le profil
  l'indique, liste des muscles en texte. Crédit et licence MIT dans « À propos ».
- Objectifs à paliers sur le 1RM estimé (bronze et argent proposés, modifiables), badges (séances, semaines
  d'affilée, clubs), souvenir (12, 6, 3 ou 1 mois), récap mensuel partageable en image, projection en fourchette.
- Tout s'affiche en haut de l'onglet Progrès en attendant l'accueil (phase 9) ; objectif et projection aussi sur la
  progression d'un exercice.
- Politique de confidentialité (partage volontaire du récap) et fiche Play Store mises à jour.
- Vérifié sur l'émulateur avec les données de démonstration : anneau, souvenir, carte du corps, badges rattrapés à
  leur date d'origine, célébration d'un record, résumé, objectif, récap et menu de partage, projection, thème clair.
- 213 tests JVM et 39 tests instrumentés verts (dont `MotivationDataTest`). Lint sans nouvel avertissement.
- Corrigé avant commit : `LocalDate.EPOCH` (Android 14 et plus seulement) aurait planté sur Android 8 à 13, repéré
  par lint ; suppression d'un objectif désormais confirmée (ses paliers atteints seraient perdus).

Le test fermé reste en pause (compte Google Play plus tard) : un build issu de la phase 7 peut lui servir.

## En pause : préparation du test fermé (une partie de la phase 11)

Le 21 octobre 2026, le développeur a choisi de passer à la phase 7 et de créer le compte Google Play plus tard.
Envoyer sur le Play Store un build issu d'une phase terminée.

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
- Étape 7 : `docs/fiche-play-store.md` (textes, réponses aux formulaires), image de présentation et six captures
  légendées dans `docs/play-store/`, faites sur l'émulateur avec des données de démonstration
  (`tools/play-store/`).
- Corrigé : l'aperçu d'un template cachait « Démarrer » sous le bord de l'écran à partir de 5 exercices. La feuille
  s'ouvre maintenant en entier et seule la liste défile, boutons fixés en bas (vérifié avec 4, 5 et 12 exercices).
- Reste : mise en ligne de la politique de confidentialité, test sur le S25+.

## Phase 7 — Profil, mesures et photos : terminée le 22 octobre 2026

- Profil fixe (prénom, taille, date de naissance, sexe, tous facultatifs) dans le DataStore, écran de modification.
- Mesures : poids, tours (bras, poitrine, taille, hanches, cuisse, mollet) et masse grasse avec sa méthode. Chaque
  saisie crée une entrée, suppression possible. Une courbe par mesure ; pour le poids, les pesées en points et la
  moyenne mobile sur 7 jours en ligne. Dernière valeur et écart dans l'onglet Profil.
- Photos : copie réduite et redressée dans `files/photos/`, timeline par mois filtrable par pose, import par le
  sélecteur de photos d'Android (date lue dans la photo), écran d'une photo avec suppression.
- Prise de photo avec CameraX : aperçu 3:4, silhouette de la dernière photo de la même pose, caméra avant ou arrière.
- Comparaison avant/après avec curseur, choix des deux photos, écart en jours.
- Verrouillage facultatif : PIN, visage ou empreinte, code oublié par le verrouillage du téléphone.
- Politique de confidentialité et fiche Play Store mises à jour (profil, mesures, photos, caméra, biométrie).
- 151 tests JVM et 35 tests instrumentés verts. Lint sans nouvel avertissement. Vérifié sur l'émulateur en debug et
  en release (R8) : profil, mesures, import, caméra avant et arrière, comparaison, verrou (mauvais code, attente,
  reverrouillage en quittant l'app, galerie ouverte sans reverrouiller).

Testé et validé par le développeur sur le S25+ le 22 octobre 2026 : caméra réelle et silhouette, visage ou
empreinte, « Code oublié ? » avec le verrouillage du téléphone, reverrouillage au retour, rotation sans reverrouillage.

Dépendances validées le 21 octobre 2026 : CameraX 1.6.2 (`camera-core`, `camera-camera2`, `camera-lifecycle`,
`camera-compose`), androidx.biometric 1.1.0, et androidx.exifinterface 1.4.2, déjà apportée par CameraX et déclarée
car utilisée directement. Pas de changement de schéma : `body_measurement` et `progress_photo` existent depuis la v1.
Reportés en phase 10 (Réglages, cahier § 4.11) : unités lb et in, masquage du poids et de la section photos.

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

- **Courbes** : `ProgressChart` prend une valeur par jour (`core/domain/DailyValue`) et une tendance facultative.
  Séances : meilleure valeur du jour ; mesures : moyenne du jour. La moyenne mobile est calculée sur toute la série
  puis découpée à la période, pour que les premiers points tiennent compte des jours d'avant.
- **Mesures** : affichées au dixième ; une mesure d'un jour passé est enregistrée à midi (heure exacte inconnue).
- **Photos** : copie JPEG de 2048 px au plus, redressée selon l'EXIF puis enregistrée sans orientation. La prise de vue
  CameraX écrit d'abord dans le cache, puis passe par la même copie. Silhouette à 35 % d'opacité, retournée en miroir
  pour la caméra avant (son aperçu est un miroir, la photo enregistrée ne l'est pas).
- **Verrou des photos** : PIN de 4 à 8 chiffres, empreinte PBKDF2-SHA256 (120 000 tours, sel de 16 octets) dans le
  DataStore. Après 5 erreurs, attente de 30 s qui double à chaque erreur (15 min au plus), compteur gardé après un
  redémarrage. Reverrouillage dans `MainActivity.onStop` (sauf rotation), sauf juste après avoir ouvert le sélecteur
  de photos ou les réglages d'Android depuis l'app (`PhotoLockSession.keepUnlockedWhileAway`). Visage ou empreinte en
  classe « faible » (`BIOMETRIC_WEAK`), la plupart des visages en font partie. `PhotoLockGate` entoure chaque écran
  de photos dans la navigation : l'écran et son ViewModel ne sont créés qu'une fois déverrouillé.
- **Clés du SavedStateHandle** : jamais le nom d'un argument de la route (bug rencontré : la clé « pose » lisait
  l'argument vide de `PhotoCaptureRoute`).

- **Motivation dans le package `progress`** (pas de nouveau package) : calculs purs dans `progress/domain`
  (`WeeklyGoals`, `RecordBreaks`, `BodyMap`, `Goals`, `Badges`, `Flashback`, `MonthlyRecap`, `Projection`), un DAO
  `MotivationDao` pour `weekly_goal`, `goal` et `badge`. Textes dans `values/strings_motivation.xml`.
- **Objectif hebdomadaire** : le choix de la bienvenue est enregistré au 1er janvier 1970 (il vaut pour toutes les
  semaines) ; un changement ajoute une ligne au lundi de la semaine en cours, qui remplace un changement fait plus tôt
  dans la même semaine (index unique). Sans ligne (installations d'avant la phase 8), 3 séances par défaut.
- **Records battus** : le cache `personal_record` ne garde que le meilleur actuel. `recordBreaks` rejoue l'historique
  séance par séance (résumé, récap) ; `liveRecords` compare la séance en cours au cache et aux séries déjà validées.
  Les couples « série, mesure » déjà célébrés sont gardés (`rememberSaveable`) : pas de nouvelle célébration après une
  rotation ; une série validée depuis la notification est célébrée au retour dans l'app.
- **Paliers et badges** : `AchievementRepository.refresh()` après une fin de séance (dans sa transaction, résultat
  montré dans le résumé), une séance modifiée ou supprimée, et au lancement (rattrapage silencieux de l'historique,
  badges datés au moment où ils ont été mérités). Il n'ajoute jamais que ce qui manque.
- **Clubs** : liste des clés free-exercise-db dans `CLUBS` (`Badges.kt`) ; un exercice personnalisé ne compte pas.
- **Carte du corps** : `app/src/main/assets/body-map/body_map.json` (230 Ko) produit par
  `tools/body-map/build_body_map.py` (commit MuscleMap figé, option `--source` pour une copie locale). Tracés convertis
  une fois en `Path` Compose par le ViewModel, hors du fil principal ; face et dos dans un cadre commun.
- **Image du récap** : la carte est dessinée dans un `GraphicsLayer` puis convertie en image (ni capture d'écran ni
  permission), PNG dans `cache/share/`, partagé via le `FileProvider` `${applicationId}.fileprovider`.
- **Projection** : régression de y = a + b × ln(1 + jours / 30) sur les 6 derniers mois, horizon 90 jours affiché en
  période (début, mi, fin de mois). Fourchette d'environ 80 % (±1,28 écart-type des résidus, au moins ±1,25 kg),
  borne basse jamais sous le meilleur déjà atteint ; pas de projection au-delà de ±10 % d'incertitude.
- **Ressources texte** : une apostrophe doit être écrite `\'`. Un script Python ou un heredoc du shell peut perdre la
  barre oblique : aapt refuse alors le fichier (« Invalid unicode escape »).

## Questions ouvertes
- Code des photos oublié sur un téléphone sans verrouillage d'écran : l'app explique qu'il faut d'abord en activer un
  dans Android (aucune autre issue inventée). À confirmer par le développeur.
- Politique de confidentialité : nom affiché et adresse de contact (publics), nom du dépôt public qui l'hébergera.

## Problèmes connus
- Anneau et streak sont recalculés quand les données changent : si l'app reste ouverte au passage du dimanche au
  lundi, la nouvelle semaine n'apparaît qu'à la prochaine séance ou à la réouverture de l'onglet.
- Lint signale des versions plus récentes (Gradle 9.8.2, DataStore 1.3.0, Kotlin 2.4.21, kotlinx.serialization
  1.12.0, kotlinx-coroutines-test 1.11.0). Mises à jour à décider à part ; coroutines-test doit rester alignée sur
  1.10.2 (voir plus bas).
- `connectedDebugAndroidTest` désinstalle l'app de l'émulateur, données comprises. Sauvegarder avant :
  `adb exec-out run-as com.paulfvt.liftapp tar -cf - databases files > sauvegarde.tar`, puis restaurer avec la version
  debug installée (`run-as … tar -xf` depuis `/data/local/tmp`) avant de remettre une release par-dessus.
- Captures du Play Store : elles ne montrent ni les mesures ni les photos, à refaire avant la production.
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
