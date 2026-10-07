# Design system LiftApp « ON AIR »

Ambiance enseigne de studio : noir profond, rouge signal lumineux, typo condensée (Barlow Condensed + Barlow).
Structure Material 3, thème sombre par défaut et thème clair. Ces fichiers servent de référence visuelle ; l'app
Android en reprend les valeurs dans `core/designsystem/theme/`.

## Contenu

| Fichier        | Rôle                                                                                       |
|----------------|--------------------------------------------------------------------------------------------|
| `styles.css`   | Source de vérité : tokens (variables CSS) puis classes des composants.                       |
| `index.html`   | Guide de style vivant : fondations, 30 composants Material 3, 5 extensions muscu, Dev Mode.   |
| `concept.html` | Deux écrans d'exemple (accueil, séance en cours) construits uniquement avec les composants.  |
| `icons.html`   | Sprite SVG des icônes, copié en haut de `index.html` et `concept.html`.                      |

Ouvrir `index.html` dans un navigateur (double-clic, pas de serveur nécessaire). Le thème choisi en haut à droite est
mémorisé et partagé entre les trois pages.

## Règles

- Toute couleur, taille, rayon ou durée passe par un token. Jamais de valeur brute dans un composant.
- Chaque fond garde son `on-*`. Les paires sont vérifiées par calcul (texte ≥ 4,5:1) : toute nouvelle paire aussi.
- `primary-container` (rouge signal) sert aux fonds pleins avec texte blanc ; `primary` sert au texte, aux icônes et
  aux contours.
- La lueur néon (`--glow-signal`, `--glow-amber`) est réservée aux moments forts : séance en direct, minuteur, record.
- Icônes : uniquement le sprite, jamais d'emoji.

## Ajouter un token

1. Le déclarer dans `:root` (thème sombre) **et** dans `:root[data-theme="light"]` s'il dépend du thème.
2. S'il s'agit d'une couleur de fond, déclarer aussi son `on-*` et vérifier le contraste.
3. L'ajouter dans `TOKEN_GROUPS` (script de `index.html`) pour qu'il apparaisse dans les prompts.
4. Le reporter dans `Color.kt`, `Type.kt` ou `LiftTokens` côté Android.

## Ajouter un composant

1. Écrire ses classes dans `styles.css`, dans la catégorie qui convient, en n'utilisant que des tokens.
2. Ajouter une `<section id="cle">` dans `index.html` avec toutes ses variantes et tous ses états, chaque exemple
   portant `data-insp="cle"`.
3. Ajouter l'entrée `cle` dans l'objet `SPECS` (nom, fichier Kotlin cible, description, propriétés, tokens, mesures).
4. Ajouter le lien dans la barre latérale. Le bouton « Copier en prompt » apparaît tout seul.

## Changer de thème

Seules les valeurs des tokens changent : les composants, le guide et la maquette suivent automatiquement.
