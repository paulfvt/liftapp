# Avancement

## Phase en cours : 2 — Modèle de données et catalogue

### Fait
- Base Room v1 avec toutes les tables du modèle (schéma exporté dans `app/schemas/`).
- Catalogue : 753 exercices de free-exercise-db, traduits en français, 1 500 images WebP (~17 Mo).
  Import au premier lancement, réimport sans perte de note ni de masquage quand `CATALOG_VERSION` augmente.
- Écrans : liste avec recherche sans accents et filtres (muscle ou groupe, matériel), fiche avec images animées,
  note permanente et masquage, création et modification d'exercices personnalisés.
- Accès au catalogue depuis l'onglet Séances.
- Tests JVM (domaine, conversions) et tests instrumentés (base, import complet).

### Reste à faire pour clore la phase 2
- [ ] Lancer les tests instrumentés sur le téléphone (`./gradlew connectedDebugAndroidTest`).
- [ ] Relire les noms traduits : `tools/catalog/noms-a-relire.csv`.
- [ ] Test sur le téléphone : chercher « squat », filtrer « Jambes », ajouter une note et la retrouver.

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

## Questions ouvertes
- Les séries dégressives comptent-elles pour les records et les statistiques ? À trancher avant la phase 4.
- Exercices au poids du corps (tractions, dips) : faut-il un champ « lest » optionnel en séance ? Phase 4.

## Problèmes connus
- Avertissement lint `OldTargetApi` (targetSdk 36 alors que l'API 37 existe) : volontaire, c'est le niveau exigé
  par Google Play. À revoir avant la publication.
