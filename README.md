# LiftApp

App Android de suivi de musculation, gratuite et hors-ligne. Elle s'inspire de Strong et Hevy et mise sur une
progression visible et motivante : records, courbes, streak hebdomadaire, carte des muscles travaillés.

> Nom de travail. Projet en cours de développement (phase 1 sur 11).

## Stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room · Hilt · KSP · GitHub Actions

Architecture en trois couches (UI, domaine, données), recommandée par Android. Le détail est dans [CLAUDE.md](CLAUDE.md).

## Lancer le projet

1. Ouvrir le dossier dans Android Studio (version 2026.2 ou plus récente).
2. Brancher un téléphone Android 8.0 ou plus avec le débogage USB activé.
3. Cliquer sur **Run**.

En ligne de commande :

```bash
./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug
```

## Documentation

- [Cahier des charges](docs/cahier-des-charges.md)
- [Avancement](PROGRESS.md)
