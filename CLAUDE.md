# LiftApp — règles du projet

App Android gratuite de suivi de musculation, hors-ligne d'abord, sans backend en V1.
Référence fonctionnelle : [docs/cahier-des-charges.md](docs/cahier-des-charges.md). Avancement : [PROGRESS.md](PROGRESS.md).

Le développeur débute en Android : explique chaque choix technique non évident en une ou deux phrases.
En cas d'ambiguïté ou de point non couvert, pose la question au lieu d'inventer une règle métier.

## Stack (section 3 du cahier des charges)

- Kotlin, Jetpack Compose, Material 3, Navigation Compose (routes typées avec kotlinx.serialization).
- Room (SQLite) pour les données métier, DataStore pour les réglages et le profil fixe, coroutines et Flow.
- Hilt pour l'injection de dépendances (KSP, jamais kapt).
- Vico pour les courbes (`progress/ui/ProgressChart.kt`), reorderable pour le glisser-déposer.
- Glance pour le widget, CameraX + Coil pour les photos, androidx.biometric pour le verrouillage.
- AGP 9 avec Kotlin intégré : ne pas appliquer `org.jetbrains.kotlin.android`. Versions dans `gradle/libs.versions.toml`.
- minSdk 26, targetSdk 36. Plus de publication : targetSdk n'a plus à suivre les exigences de Google Play.
- Interface en français, tous les textes dans `strings.xml`. Unités kg/lb et cm/in au choix.
- POST_NOTIFICATIONS demandée au moment où elle sert (première séance), jamais au lancement.
- Minuteur : doit survivre à l'écran verrouillé, à la rotation et à l'arrière-plan. Vérifier la doc Android à jour
  pour les types de service de premier plan (Android 14+). Live Updates d'Android 16 si disponibles.

## Architecture (section 6)

- **UI** : écrans Compose sans logique métier, un ViewModel par écran qui expose un `StateFlow` d'état.
- **Domaine** : calculs purs en Kotlin sans dépendance Android (1RM, records, streak, heatmap, projection, badges).
- **Données** : DAO Room, repositories comme unique point d'accès aux données.
- Packages par fonctionnalité sous `com.paulfvt.liftapp` : `home`, `template`, `workout`, `exercise`, `history`,
  `progress`, `profile`, `widget`, plus `core` (partagé) et `navigation` (squelette de l'app).

## Règles non négociables

- **Migrations Room** : `exportSchema = true`, schémas dans `app/schemas/` versionnés dans Git, une migration écrite
  et testée par changement de schéma. `fallbackToDestructiveMigration()` est interdit.
- **Tests unitaires** sur toute la couche domaine, cas limites compris : 0 rep, série d'échauffement,
  semaine à cheval sur deux mois, changement d'objectif hebdo.
- **Tests de migration** Room et test de reprise d'une séance en cours après redémarrage.
- **Pas de nouvelle dépendance** sans l'expliquer et demander validation.
- **Accessibilité** : descriptions pour les lecteurs d'écran, zones tactiles d'au moins 48 dp, contrastes suffisants,
  thèmes clair et sombre.
- **Code lisible** : noms en anglais, commentaires en français sur les parties non évidentes, pas de code mort.
- **Lint** : `./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug` doit passer (c'est ce que lance la CI).

## Modèle de données

- Valeurs stockées en métrique (kg en Double, cm, m, s), conversion et arrondi uniquement à l'affichage.
- Horodatages en UTC (`Instant`), affichage dans le fuseau du téléphone.
- Les records sont un cache recalculable, jamais la source de vérité.
- Un exercice utilisé dans l'historique n'est jamais supprimé, seulement masqué.
- Nutrition (V2) et IA (V3) s'ajouteront par nouvelles tables, sans modifier les tables existantes.

## Décisions prises avec le développeur (6 octobre 2026)

- Publication en test fermé après la phase 6 ; motivation, photos et widget ajoutés pendant le test (abandonné le
  31 octobre 2026, voir « Pas de publication »).
- Catalogue free-exercise-db (Unlicense), traduit en français, images incluses.
- Exercices au poids du corps : `weightKg` = charge ajoutée (négative = assistance). Le poids du corps n'entre ni dans
  le tonnage ni dans le 1RM.
- Arrondi d'affichage : pas de 0,5 kg en kilos, 1,25 lb en livres.
- Pas de superset en V1.
- Types de série : échauffement, travail, dégressive (pas de type « échec »). Les échauffements sont affichés mais
  exclus des records et des statistiques.
- Performance précédente : dernière séance de cet exercice **dans le même template**, sinon dernière fois tous
  templates confondus.
- Catalogue : sans étirements ni rouleau de massage (753 exercices), 2 images WebP 480 px par exercice, noms et
  instructions traduits en français (tutoiement). Toute modification passe par `tools/catalog/build_catalog.py`
  et une hausse de `CATALOG_VERSION`.
- Onglet « Progrès » plutôt que « Progression » (libellé plus court, mieux centré).
- Prochaine séance sur l'accueil : template suivant le dernier utilisé dans son dossier ; hors dossier, le suivant des
  templates sans dossier dans l'ordre de la liste (1er novembre 2026). Après le dernier, on repart du premier.
- Semaine du lundi au dimanche, fuseau du téléphone. La semaine en cours ne casse jamais la streak.
  Une séance compte pour sa date de début.
- Pas de célébration de record lors de la première séance sur un exercice.
- Clubs 100/140/180 kg : charge réellement soulevée sur au moins une rep. Badges et paliers acquis définitivement.
- Photos : PIN propre à l'app, avec option visage ou empreinte. Exclues de la sauvegarde cloud Android.
- Carte du corps : tracés SVG de Jsplice/MuscleMap (licence MIT, à citer dans « À propos »).
- Templates (7 octobre 2026) : 3 séries par défaut, reps cibles facultatives (case vide), 2 min de repos par défaut,
  un exercice au plus une fois par template.
- Séries détaillées dans les templates (8 octobre 2026, écart avec le cahier § 4.3 et § 5) : chaque série prévue a
  son type et ses reps cibles (table `template_set`, schéma v2). Une série ajoutée est une série de travail sans cible.
- Séance en cours (11 octobre 2026) :
  - pré-remplissage par type : la n-ième série d'un type reprend la n-ième série de ce type la dernière fois,
    sinon la dernière série de ce type ;
  - reps et poids pré-remplis depuis la dernière fois, la cible du template est seulement affichée ;
  - exercices au poids du corps : champ lest facultatif (`weightKg`, négatif = assistance) ;
  - un exercice ajouté pendant la séance reprend les séries de la dernière fois, sinon 3 séries de travail vides ;
  - en fin de séance, les séries non validées sont retirées, après un avertissement.
- Design « ON AIR » (7 octobre 2026) : référence dans `design-system/`, rouge signal (`primaryContainer`) pour les
  actions principales, lueur réservée aux moments forts. Polices Barlow embarquées (OFL, à citer dans « À propos »).
- Minuteur de repos (21 octobre 2026) :
  - sans service de premier plan : notification avec compte à rebours dessiné par Android et alarme exacte
    (`SCHEDULE_EXACT_ALARM`) pour l'alerte de fin de repos ;
  - valider une série lance le repos de son exercice (2 min par défaut, rien si 0 s), pas de repos après la dernière
    série de la séance ; −15 s, +30 s, Passer ;
  - son et vibration réglés dans le menu de la séance en attendant l'écran Réglages (phase 10) ;
  - (1er novembre 2026) vibration dès 3 s avant la fin, une impulsion par seconde, puis pendant le son ; son « ding
    ding » d'1 s qui s'arrête seul, joué comme une alarme : entendu même en vibreur ou en silencieux ;
  - mode salle : interrupteur dans le menu de la séance, écran allumé, une série à la fois, gros boutons.
- Records (21 octobre 2026) :
  - les séries dégressives comptent, seuls les échauffements sont exclus ; une série à 0 rep ne compte pas ;
  - 1RM d'Epley de 2 à 12 reps, une série à 1 rep vaut son poids ;
  - exercices sans charge : poids du corps = reps max (sans assistance) et lest max, durée max, distance max ;
  - toujours recalculés en entier pour un exercice (fin, modification, suppression d'une séance).
- Graphiques avec Vico (`com.patrykandpatrick.vico:compose`, Apache 2.0), validé le 21 octobre 2026.
- Test fermé avant la phase 7 (21 octobre 2026) : nom « LiftApp » et identifiant `com.paulfvt.liftapp` définitifs,
  onglet Accueil masqué jusqu'à la phase 9 (l'app s'ouvre sur Séances), Profil réduit à « À propos », écran de
  bienvenue unique. Icône : piste B « Néon » de `design-system/icon.html` (barre en tube de néon rouge sur fond noir).
- Phase 7 (21 octobre 2026) : CameraX et androidx.biometric validés. Verrouillage des photos facultatif ; une fois
  déverrouillées, elles le restent tant que l'app est au premier plan et se reverrouillent dès qu'elle passe en
  arrière-plan. PIN oublié : confirmation par le verrouillage du téléphone, puis nouveau PIN, photos gardées.
  Mesures : chaque saisie crée une entrée, suppression possible, pas de modification ; kg et cm jusqu'à la phase 10.
- Verrou des photos (22 octobre 2026) : PIN de 4 à 8 chiffres jamais stocké en clair, attente croissante après
  5 erreurs, déverrouillage par visage ou empreinte en option. Le sélecteur de photos et les réglages d'Android ouverts
  depuis l'app ne reverrouillent pas. Code oublié sans verrouillage d'écran : explication seulement (confirmé le
  31 octobre 2026).
- Phase 8, motivation (28 octobre 2026) :
  - anneau, streak, carte du corps, souvenir, objectifs, badges et récap en haut de l'onglet Progrès tant que
    l'accueil est masqué ; ils rejoignent l'accueil en phase 9 ;
  - un changement d'objectif hebdo compte dès la semaine en cours, les semaines passées gardent le leur ;
  - badges 4, 12 et 52 semaines : semaines d'affilée (la streak) ;
  - records célébrés et « battus » : charge max, 1RM estimé, meilleur volume d'une série ; sans charge : reps, lest,
    durée, distance. Pas les reps à une charge donnée ;
  - paliers : l'utilisateur saisit l'or, bronze et argent proposés à 1/3 et 2/3 de l'écart depuis son 1RM actuel,
    modifiables ;
  - clubs : développé couché prise moyenne, large et powerlifting ; squat à la barre, complet et pieds écartés ;
    soulevé de terre classique et sumo. Pas de Smith machine ;
  - projection : au moins 6 séances étalées sur 4 semaines ou plus, la dernière il y a moins de 4 semaines ;
  - souvenir : toujours affiché, formulation neutre sans progrès ;
  - carte du corps : 7 jours glissants, cardio exclu.
- Phase 9, tableau de bord et widget (28 octobre 2026) :
  - l'onglet Accueil revient en premier et l'app s'ouvre dessus ;
  - Accueil : semaine (anneau et streak), prochaine séance avec « Démarrer », dernier record, souvenir. Progrès garde
    la carte du corps, les objectifs, les badges, le récap et la liste des exercices ;
  - « Démarrer » du widget lance la séance et ouvre l'app dessus ;
  - Glance 1.2.0 validé : `glance-appwidget` et `glance-material3` (couleurs du thème ON AIR).
- Phase 10, réglages et données (28 octobre 2026) :
  - export CSV au format Excel français : point-virgule, virgule décimale, en-têtes en français ;
  - photos dans la sauvegarde au choix, cochées par défaut, avec un avertissement (fichier non chiffré, photos
    lisibles sans le code) ;
  - restauration par remplacement de toutes les données, après confirmation (pas de fusion) ;
  - (31 octobre 2026) masquer le poids de corps cache aussi la masse grasse, les tours restent visibles ;
  - exports CSV et sauvegarde complets, même ce qui est masqué (le masquage ne concerne que l'affichage) ;
  - une sauvegarde sans photos efface les photos actuelles à la restauration (remplacement complet, signalé dans la
    confirmation) ;
  - restauration proposée seulement dans les Réglages, pas sur l'écran de bienvenue.
- Retouche visuelle ON AIR (1er novembre 2026), d'après `design-system/concept.html` :
  - boutons du guide de style partout : coins de 8 dp (12 dp en grand), libellés en capitales espacées ;
  - accueil : date et « SEMAINE n » (numéro ISO), enseigne « HORS ANTENNE » ou « EN DIRECT », rangée des jours ;
    prochaine séance avec la durée réelle de la dernière séance avec ce template (rien si jamais faite) ;
  - tuiles « Cette semaine » : tonnage hors échauffements et durée, écart en % avec la semaine précédente prise au même
    moment (du lundi au même jour et à la même heure), rien si l'une des deux n'a pas de tonnage ;
  - séance : puce de repos qui change le repos de l'exercice, puce de la dernière fois avec sa meilleure série de
    travail (jour si moins d'une semaine, sinon date), minuteur compact sur une ligne (le mode salle garde le grand).
- Pas de publication (31 octobre 2026, écart avec le cahier § 8 et la phase 11) : LiftApp reste une app personnelle,
  installée sur le S25+ sans passer par Google Play (ni compte développeur, ni test fermé). La phase 11 se réduit à
  la version release installée sur le téléphone ; son plan est à valider (voir `PROGRESS.md`).

## Méthode de travail (section 7)

- **En début de session : lire `PROGRESS.md`** (phase en cours ou prochaine étape à décider, décisions techniques,
  problèmes connus) avant de proposer quoi que ce soit.
- Une phase à la fois, dans l'ordre. Une phase est terminée quand ses tests passent et que le développeur l'a testée
  sur son téléphone (Samsung Galaxy S25+, Android 16).
- Avant chaque phase : plan court (fichiers créés ou modifiés, choix techniques) puis attente de validation.
- Petites étapes, un commit par étape, messages clairs.
- Tenir `PROGRESS.md` à jour : phase en cours, fait, décisions, problèmes connus.
- Signaler tout écart avec le cahier des charges au lieu de le faire en silence.
