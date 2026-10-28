# Fiche Play Store et formulaires de la Play Console

Brouillon préparé le 21 octobre 2026. Les règles de Google Play changent : revérifier chaque point dans la Play
Console au moment de la saisie.

## Fiche

- **Nom de l'application** (30 caractères max) : LiftApp : carnet de muscu
- **Catégorie** : Santé et remise en forme
- **Description courte** (80 caractères max) :
  Carnet de muscu hors ligne : séances, minuteur de repos, records et courbes.
- **Description complète** (4 000 caractères max) :

  > LiftApp est un carnet de musculation simple, gratuit et sans compte. Tout reste sur ton téléphone.
  >
  > PRÉPARE TES SÉANCES
  > • Crée tes templates (push, pull, jambes…) et range-les dans des dossiers.
  > • Plus de 750 exercices avec images et instructions en français, ou tes propres exercices.
  > • Séries d'échauffement, de travail et dégressives, reps cibles et temps de repos par exercice.
  >
  > PENDANT LA SÉANCE
  > • Ta performance de la dernière fois s'affiche à côté de chaque série, et les valeurs sont pré-remplies.
  > • Le minuteur de repos se lance quand tu valides une série et reste visible écran verrouillé.
  > • Mode salle : gros boutons, une série à la fois, écran toujours allumé.
  > • La séance est enregistrée en continu : rien n'est perdu si l'app se ferme.
  >
  > SUIS TES PROGRÈS
  > • Historique de toutes tes séances, modifiables après coup.
  > • Records par exercice : 1RM estimé, charge max, meilleure série, reps max par charge.
  > • Courbes de progression sur 1 mois, 3 mois, 1 an ou depuis le début.
  >
  > RESTE MOTIVÉ
  > • Objectif de séances par semaine et streak en semaines : les jours de repos ne sont jamais pénalisés.
  > • Célébration de tes records pendant la séance, résumé de fin de séance comparé à la dernière fois.
  > • Carte du corps des muscles travaillés sur 7 jours.
  > • Objectifs à paliers bronze, argent et or, badges et souvenirs d'il y a quelques mois.
  > • Récap du mois à partager en image, et projection prudente de ton 1RM.
  >
  > SUIS TON CORPS
  > • Poids, tours et masse grasse, avec la moyenne sur 7 jours pour lisser les variations du poids.
  > • Photos de progression avec la silhouette de la dernière photo pour garder le même cadrage.
  > • Comparaison avant/après avec un curseur.
  > • Photos rangées à part de ta galerie, verrouillables par un code, ton visage ou ton empreinte.
  >
  > TES DONNÉES T'APPARTIENNENT
  > • Pas de compte, pas de publicité, pas de suivi.
  > • L'app n'a pas accès à internet : tes données ne quittent pas ton téléphone.
  >
  > LiftApp est en développement actif : tableau de bord et widget arrivent bientôt.

- **Icône** : 512 × 512 px, PNG 32 bits : [play-store/icon-512.png](play-store/icon-512.png) (piste « Néon »).
- **Image de présentation** : 1 024 × 500 px, PNG 24 bits :
  [play-store/feature-graphic.png](play-store/feature-graphic.png), source dans `feature-graphic.html`.
- **Captures d'écran** (2 à 8, téléphone) : six captures de 1080 × 2160 dans
  [play-store/screenshots/](play-store/screenshots/), dans l'ordre d'affichage : séance en cours, progression,
  templates, mode salle, historique, fiche d'exercice. Le grand côté ne doit pas dépasser le double du petit : les
  captures brutes (1080 × 2400) sont donc réduites et posées sous une légende.

### Refaire les captures (après un changement d'interface)

1. Sur l'émulateur, récupérer la base (app arrêtée) avec `run-as` (voir « Problèmes connus » dans `PROGRESS.md`),
   puis lancer `python tools/play-store/seed_demo_data.py lift.db` sur la copie et la remettre en place.
2. Dans l'app, supprimer la séance « À supprimer » de l'historique : les records sont recalculés.
3. Mode démo de la barre d'état (`adb shell am broadcast -a com.android.systemui.demo -e command enter`, puis
   `clock -e hhmm 1830`), et autorisations notifications et alarmes exactes accordées.
4. Capturer avec `adb exec-out screencap -p` : `1-templates`, `2-workout` (séance Push démarrée, échauffements et
   première série validés), `3-gym-mode`, `4-progress` (développé couché), `5-history`, `6-exercise`.
5. `python tools/play-store/compose_screenshots.py <captures> docs/play-store/screenshots app/src/main/res/font`.

## Contenu de l'application (rubrique « Règles et programmes »)

- **Règles de confidentialité** : URL de la page publiée (voir `politique-de-confidentialite.md`).
- **Accès à l'application** : toutes les fonctionnalités sont accessibles sans identifiants.
- **Annonces** : non, l'application ne contient pas d'annonces.
- **Classification du contenu** (questionnaire IARC) : catégorie « Toutes les autres applications » ; non à toutes
  les questions (violence, sexualité, langage, substances, jeux d'argent, interactions entre utilisateurs, partage de
  la position, achats numériques). Résultat attendu : PEGI 3.
- **Public cible** : 18 ans et plus, le plus simple. Inclure les moins de 13 ans soumettrait l'app au programme
  « Familles », inutile ici.
- **Sécurité des données** :
  - L'application collecte-t-elle ou partage-t-elle des données utilisateur ? **Non** (aucune donnée ne quitte
    l'appareil, aucune permission internet).
  - La sauvegarde Android (Google) relève du système, pas de l'app : elle n'est pas déclarée comme collecte.
- **Applications gouvernementales, fonctionnalités financières** : non.
- **Applications de santé** : si la Play Console demande la déclaration, cocher le suivi d'activité physique,
  sans connexion à Santé Connect.
- **Service de premier plan** : aucun (le minuteur n'en utilise pas), rien à déclarer.
- **Caméra et biométrie** (phase 7) : `CAMERA` pour les photos prises dans l'app, `USE_BIOMETRIC` et
  `USE_FINGERPRINT` (ajoutées par androidx.biometric) pour le déverrouillage des photos. Toujours aucune permission
  internet : la réponse « aucune donnée collectée » reste valable.
- **Alarmes exactes** : l'app utilise `SCHEDULE_EXACT_ALARM` (accordée par l'utilisateur), pas `USE_EXACT_ALARM`.
  Vérifier qu'aucune déclaration n'est demandée.

## Test fermé

- Créer une piste « Test fermé », y ajouter une liste de testeurs (adresses Gmail), envoyer `app-release.aab`.
- Au moins 12 testeurs inscrits pendant 14 jours d'affilée avant de pouvoir demander l'accès à la production.
- Les testeurs s'inscrivent par le lien d'adhésion de la piste, puis installent l'app depuis le Play Store.
