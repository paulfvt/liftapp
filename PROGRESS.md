# Avancement

## Phase en cours : 1 — Mise en place

### Fait
- Projet Android : AGP 9.4.1, Gradle 9.8.0, Kotlin 2.4.20, compileSdk 37, targetSdk 36, minSdk 26.
- Hilt (application et activité), Navigation Compose avec routes typées, barre du bas à 5 onglets
  (Accueil, Séances, Historique, Progression, Profil), écrans temporaires.
- Thème Material 3 clair et sombre (palette orange provisoire, sans couleurs dynamiques).
- Room configuré (plugin, KSP, dossier `app/schemas/`). La base elle-même arrive en phase 2 avec les entités.
- ktlint, Android Lint, CI GitHub Actions.
- Règles de sauvegarde Android : dossier `photos/` exclu de la sauvegarde cloud.
- `CLAUDE.md`, copie du cahier des charges dans `docs/`.

### Reste à faire pour clore la phase 1
- [x] Terminer l'assistant d'Android Studio (installation du SDK) et ouvrir le projet.
- [x] Générer le wrapper Gradle (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`).
- [x] Premier build local : `ktlintCheck lintDebug testDebugUnitTest assembleDebug` passe.
- [ ] Créer le dépôt GitHub privé, pousser et vérifier que la CI est verte.
- [ ] Lancer l'app sur le S25+.

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

## Questions ouvertes (à trancher avant la phase 2)
- Types de série : échauffement + travail seulement, ou aussi dégressive (réponses 9 et 10 contradictoires) ?
  Le type « échec » est-il conservé ? Proposition : échauffement + travail, et une case « à l'échec ».
- Performance précédente quand l'exercice n'a jamais été fait dans ce template : ne rien pré-remplir, ou reprendre
  la dernière fois tous templates confondus avec la mention « autre séance » ?

## Problèmes connus
- Avertissement lint `OldTargetApi` (targetSdk 36 alors que l'API 37 existe) : volontaire, c'est le niveau exigé
  par Google Play. À revoir avant la publication.
