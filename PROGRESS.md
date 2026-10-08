# Avancement

## Phase en cours : 3 — Templates

Plan validé le 7 octobre 2026 : reps cibles en une seule case vide par défaut, réorganisation par glisser-déposer
(bibliothèque Reorderable), 2 minutes de repos par défaut.

- [x] Design system « ON AIR » validé (`design-system/`) et appliqué au thème Compose.
- [x] Couche données (`TemplateDao`, `TemplateRepository`) et logique pure testée (brouillon, dossiers, ordre).
- [x] Onglet Séances : templates hors dossier puis dossiers repliables ; créer, renommer, dupliquer, déplacer,
  supprimer ; glisser-déposer dans une même section.
- [x] Éditeur de template et catalogue en mode sélection multiple.
- [x] Séries détaillées (type et reps cibles par série), base en version 2 avec la première migration, testée.
- [x] Tests instrumentés : 11/11 sur l'émulateur API 37 (dont 5 nouveaux pour les templates). Parcours complet
  vérifié à la main sur l'émulateur, thèmes clair et sombre.
- [ ] Test sur le S25+ par le développeur.

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

## Questions ouvertes
- Les séries dégressives comptent-elles pour les records et les statistiques ? À trancher avant la phase 4.
- Exercices au poids du corps (tractions, dips) : faut-il un champ « lest » optionnel en séance ? Phase 4.

## Problèmes connus
- `kotlinx-coroutines-test` doit rester à la même version que `kotlinx-coroutines-core` utilisé par l'app
  (1.10.2, imposé par AndroidX), sinon les tests instrumentés échouent avec un `NoSuchMethodError`.
- Avertissement lint `OldTargetApi` (targetSdk 36 alors que l'API 37 existe) : volontaire, c'est le niveau exigé
  par Google Play. À revoir avant la publication.
