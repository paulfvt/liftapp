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
  > TES DONNÉES T'APPARTIENNENT
  > • Pas de compte, pas de publicité, pas de suivi.
  > • L'app n'a pas accès à internet : tes données ne quittent pas ton téléphone.
  >
  > LiftApp est en développement actif : objectif hebdomadaire, carte des muscles travaillés, mesures et photos
  > de progression arrivent bientôt.

- **Icône** : 512 × 512 px, PNG 32 bits : [play-store/icon-512.png](play-store/icon-512.png) (piste « Néon »).
- **Image de présentation** : 1 024 × 500 px, JPEG ou PNG 24 bits.
- **Captures d'écran** : au moins 2 (8 max) pour téléphone. Le grand côté ne doit pas dépasser le double du petit :
  les captures brutes du S25+ ou de l'émulateur (1080 × 2400) sont trop allongées, elles seront recadrées en
  1080 × 2160. Proposition : liste des templates, séance en cours avec le minuteur, mode salle, courbe de progression,
  détail d'une séance.

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
- **Alarmes exactes** : l'app utilise `SCHEDULE_EXACT_ALARM` (accordée par l'utilisateur), pas `USE_EXACT_ALARM`.
  Vérifier qu'aucune déclaration n'est demandée.

## Test fermé

- Créer une piste « Test fermé », y ajouter une liste de testeurs (adresses Gmail), envoyer `app-release.aab`.
- Au moins 12 testeurs inscrits pendant 14 jours d'affilée avant de pouvoir demander l'accès à la production.
- Les testeurs s'inscrivent par le lien d'adhésion de la piste, puis installent l'app depuis le Play Store.
