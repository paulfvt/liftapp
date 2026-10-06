# LiftApp — règles du projet

App Android gratuite de suivi de musculation, hors-ligne d'abord, sans backend en V1.
Référence fonctionnelle : [docs/cahier-des-charges.md](docs/cahier-des-charges.md). Avancement : [PROGRESS.md](PROGRESS.md).

Le développeur débute en Android : explique chaque choix technique non évident en une ou deux phrases.
En cas d'ambiguïté ou de point non couvert, pose la question au lieu d'inventer une règle métier.

## Stack (section 3 du cahier des charges)

- Kotlin, Jetpack Compose, Material 3, Navigation Compose (routes typées avec kotlinx.serialization).
- Room (SQLite) pour les données métier, DataStore pour les réglages et le profil fixe, coroutines et Flow.
- Hilt pour l'injection de dépendances (KSP, jamais kapt).
- Glance pour le widget, CameraX + Coil pour les photos, androidx.biometric pour le verrouillage.
- AGP 9 avec Kotlin intégré : ne pas appliquer `org.jetbrains.kotlin.android`. Versions dans `gradle/libs.versions.toml`.
- minSdk 26. targetSdk = niveau exigé par Google Play à la publication (36 en octobre 2026).
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

- Publication en test fermé après la phase 6 ; motivation, photos et widget ajoutés pendant le test.
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
- Prochaine séance sur l'accueil : template suivant le dernier utilisé dans son dossier, sinon le dernier utilisé.
- Semaine du lundi au dimanche, fuseau du téléphone. La semaine en cours ne casse jamais la streak.
  Une séance compte pour sa date de début.
- Pas de célébration de record lors de la première séance sur un exercice.
- Clubs 100/140/180 kg : charge réellement soulevée sur au moins une rep. Badges et paliers acquis définitivement.
- Photos : PIN propre à l'app, avec option visage ou empreinte. Exclues de la sauvegarde cloud Android.
- Carte du corps : tracés SVG de Jsplice/MuscleMap (licence MIT, à citer dans « À propos »).

## Méthode de travail (section 7)

- Une phase à la fois, dans l'ordre. Une phase est terminée quand ses tests passent et que le développeur l'a testée
  sur son téléphone (Samsung Galaxy S25+, Android 16).
- Avant chaque phase : plan court (fichiers créés ou modifiés, choix techniques) puis attente de validation.
- Petites étapes, un commit par étape, messages clairs.
- Tenir `PROGRESS.md` à jour : phase en cours, fait, décisions, problèmes connus.
- Signaler tout écart avec le cahier des charges au lieu de le faire en silence.
