# LiftApp

App Android de suivi de musculation, gratuite et hors-ligne. Elle s'inspire de Strong et Hevy et mise sur une
progression visible et motivante : records, courbes, streak hebdomadaire, carte des muscles travaillés.

> Projet en cours de développement : phases 1 à 6 sur 11 terminées (catalogue, templates, séance en cours, minuteur
> et notification, historique, records et courbes), test fermé sur Google Play en préparation. Le détail est dans
> [PROGRESS.md](PROGRESS.md).

## Stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room · DataStore · Hilt · KSP · Vico · GitHub Actions

Architecture en trois couches (UI, domaine, données), recommandée par Android. Le détail est dans [CLAUDE.md](CLAUDE.md).

## Lancer le projet

1. Ouvrir le dossier dans Android Studio (version 2026.2 ou plus récente).
2. Brancher un téléphone Android 8.0 ou plus avec le débogage USB activé.
3. Cliquer sur **Run**.

En ligne de commande :

```bash
./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug
```

## Version release pour Google Play

1. Une seule fois, créer la clé d'envoi hors du dépôt (le JDK d'Android Studio fournit `keytool`) :

   ```bash
   keytool -genkeypair -v -keystore C:/Users/paulf/keys/liftapp-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```

   Garder une copie de la clé et de ses mots de passe en lieu sûr (gestionnaire de mots de passe).
2. Copier `keystore.properties.example` en `keystore.properties` (ignoré par Git) et le remplir.
3. Augmenter `versionCode` dans `app/build.gradle.kts` (chaque envoi sur Google Play en demande un nouveau).
4. Construire le bundle, à envoyer dans la Play Console :

   ```bash
   ./gradlew bundleRelease
   ```

   Il se trouve dans `app/build/outputs/bundle/release/app-release.aab`.

Sans `keystore.properties`, la release est signée avec la clé de debug : elle s'installe pour être testée, mais
Google Play la refuse.

## Documentation

- [Cahier des charges](docs/cahier-des-charges.md)
- [Avancement](PROGRESS.md)
- [Politique de confidentialité](docs/politique-de-confidentialite.md) (brouillon, publiée à part)
