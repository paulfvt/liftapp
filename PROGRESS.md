# Avancement

## Phase 12 — Nutrition (V2) : terminée le 15 novembre 2026, version 1.1.0 publiée

Étapes 1 à 5 faites le 3 novembre 2026, étapes 6 et 7 le 11 novembre (voir « Retours du 11 novembre » plus bas).
Version release avec les corrections du 12 novembre installée sur le S25+ le 14 novembre 2026, testée par le
développeur le 15 novembre : bonne.

Captures du README refaites le 15 novembre (9 au lieu de 6, dont journal, étiquette et Tendance ; plus d'ancienne
barre du bas ni de badges) avec `tools/play-store/seed_demo_data.py`, mis à jour pour la base v6 : séances, repas et
pesées datés à partir du jour des captures, procédure en tête du script. Défaut vu au passage, corrigé après la
1.1.0 : la Tendance écrivait « 2700 » au lieu de « 2 700 ».

GitHub : version 1.1.0 (`versionCode` 2) publiée le 15 novembre 2026, release `v1.1.0` avec `LiftApp.apk`,
nutrition comprise. Prochaine version : augmenter `versionCode` et `versionName`, seulement sur demande du
développeur.

Fait, un commit par étape :
- Navigation : onglet Nutrition en deuxième, Séances au milieu ; l'historique est listé sous les templates (même
  page), « Tout voir » de l'accueil y descend.
- Base d'aliments : `tools/nutrition/build_foods.py` produit `assets/nutrition/foods.json` (3 092 aliments de la
  table Ciqual 2020, 93 écartés faute de protéines, glucides ou lipides). Base en version 5, migration 4 → 5 testée,
  import au lancement (`FoodImporter`).
- Calculs (`nutrition/domain`, 29 tests) : besoins, macros, objectif historisé, recherche, dépense réelle, aliments
  personnels.
- Écrans : journal du jour (anneau des calories, barres des macros, 4 repas, « Copier hier », « Enregistrer ce
  repas »), Ajouter un aliment (recherche, Récents, Favoris, Mes repas, Mes aliments), Quantité (pavé des séances,
  ±10 g, « 100 g » et « Dernière fois », favori), création et modification d'un aliment, Mes besoins.
- Vérifié sur l'émulateur, depuis la version 1.0.0 et ses données : passage en version 5 sans perte, recherche
  (« poulet filet »), ajout de 180 g, objectif de prise de masse (3 030 kcal, 153 / 434 / 76 g comme les maquettes),
  repas enregistré ajouté au dîner, aliment personnel créé puis ajouté.
- 290 tests JVM et 51 tests instrumentés verts. Lint sans nouvel avertissement. Aucune nouvelle dépendance.

Plan validé le 3 novembre 2026 (règles dans `CLAUDE.md`, maquettes sur le canevas « Nutrition LiftApp ») :
1. Navigation : barre Accueil · Nutrition · Séances · Progrès · Profil, historique sous les templates.
2. Base d'aliments : script `tools/nutrition/` (table Ciqual vers un fichier embarqué, importé au lancement comme le
   catalogue), base en version 5 avec de nouvelles tables seulement, migration 4 → 5 testée.
3. Calculs purs testés : besoins, objectif, macros, totaux du jour, recherche, dépense réelle.
4. Écrans Journal du jour, Ajouter un aliment, Quantité (pavé des séances), favoris, récents, aliments créés,
   « Mes repas », « Copier hier ». Test sur le téléphone.
5. Mes besoins : objectif calculé, modifiable, historisé.
6. Tendance : moyenne des calories, poids, dépense réelle estimée.
7. Carte « Nutrition du jour » sur l'accueil, export CSV des repas, crédit Ciqual, README, politique de
   confidentialité. Test sur le téléphone.

Pas de nouvelle dépendance prévue.

Ajout validé le 5 novembre 2026, fait le même jour (à tester sur le téléphone) : photo de l'étiquette (règles dans
`CLAUDE.md`). Vérifié sur l'émulateur avec deux tableaux d'étiquettes dessinés pour l'occasion (deux colonnes 100 g
et portion, kJ et kcal sur deux rangées, « < 0,5 g ») : 4 valeurs sur 4 à chaque fois ; photo de l'appareil photo
effacée après lecture ; envoi des statistiques de ML Kit en échec silencieux, sans plantage. 300 tests JVM verts.
1. Lecture du tableau (calcul pur, testé) : lignes de texte et leur position → kcal, protéines, glucides, lipides
   pour 100 g.
2. ML Kit text-recognition embarqué (nouvelle dépendance validée), permission internet retirée du manifeste, release
   limitée aux processeurs ARM des téléphones.
3. « Créer un aliment » : « Prendre l'étiquette en photo » et « Choisir une image », champs remplis à vérifier.
4. Crédits dans « À propos », README, politique de confidentialité ; test sur l'émulateur puis le téléphone.

## Retours du 12 novembre 2026 : corrigés et validés sur le téléphone le 15 novembre

Retours du développeur après le test sur le S25+ (scan, Tendance et accueil) ; un commit par point :
- Bug : « Utiliser ces valeurs » laissait les cases de l'aliment vides. Le scan déposait ses valeurs dans le
  SavedStateHandle de l'entrée de navigation, mais l'écran les attendait dans celui de son ViewModel, un autre objet.
  Elles sont relevées dans la navigation, comme le choix d'exercices. Même défaut corrigé pour « Choisir une image
  plutôt ». Les essais du 11 novembre n'avaient pas couvert ce parcours (la fausse caméra de l'émulateur ne lit rien).
- Virgules perdues sur certaines images (« 5,8 » lu « 58 ») : les deux lectures votent ensemble, les calories
  tranchent, sinon la version à virgule gagne.
- Accueil : titres rouges pour la prochaine séance et la nutrition, titre de la séance sorti de la carte, carte
  cliquable vers l'aperçu du template.

Vérifié sur l'émulateur : « Choisir une image plutôt » depuis le scan remplit les 4 cases (même mécanisme que
« Utiliser ces valeurs ») ; titres rouges en thème clair ; carte de la séance qui ouvre l'aperçu, « Démarrer » de
l'aperçu qui lance la séance, puis « SÉANCE EN COURS » avec « Reprendre ». Commande de la CI verte.

## Retours du 11 novembre 2026 et fin de la phase 12 : testés sur le téléphone le 12 novembre

Retours du développeur (séances validées, « rien à redire ») et plan validé le même jour après les maquettes 5 à 8 du
canevas « Nutrition LiftApp ». Un commit par point :
- Badges et clubs 100/140/180 kg retirés (écart avec le cahier § 4.7, noté dans `CLAUDE.md`). Streak, paliers,
  records et récap restent. Base en version 6 : la migration 5 → 6 supprime la table `badge`, testée.
- Lecture d'étiquette plus fiable : photo redressée selon l'inclinaison des lignes (cause probable des ratés : une
  rangée large d'une photo penchée tombait sur la rangée voisine), liste des ingrédients ignorée, sous-rangées
  coupées, « Énergie (kJ/kcal) 1520/362 », confusions de lecture corrigées. Carte « Les calories ne collent pas »
  avec la correction quand un seul chiffre explique l'écart.
- Scan en direct : caméra de l'app (CameraX, déjà validé) avec un cadre, vote sur les dernières images, lampe. Plus
  de photo par l'appareil photo d'Android ni de dossier `label/` dans le FileProvider. Aucune nouvelle dépendance.
- Étape 6, Tendance (bouton du journal) : semaine de calories et protéines, poids des 4 semaines, dépense réelle et
  objectif proposé.
- Étape 7 : carte « Nutrition du jour » sur l'accueil, export CSV des repas, README et politique de confidentialité.
- Recettes maison : idée proposée, mise de côté par le développeur.

Vérifié sur l'émulateur (version debug) : 51 tests instrumentés verts, dont la migration 5 → 6 ; Progrès sans
badges ; Tendance ; carte de l'accueil avec et sans objectif ; scan en direct ouvert après la permission, en plein
écran ; étiquette de la galerie lue (4 valeurs) ; carte de contrôle avec « Mettre 8,4 g » ; export des repas lu tel
quel. 317 tests JVM verts, lint sans nouvel avertissement.

Testés sur le téléphone le 12 novembre (retours plus haut) : scan en direct sur de vrais paquets, Tendance avec les
vraies données, carte de l'accueil. Pas confirmés par le développeur : la carte de contrôle des calories et l'export
des repas dans Excel.

## Retours du test sur le téléphone (9 novembre 2026) : corrigés, à tester sur le téléphone

Retours du développeur, causes trouvées dans les journaux du S25+ ; un commit par point :
- Scan de l'étiquette qui fermait l'app et widget bloqué en chargement : même cause, la version release seulement.
  R8 en mode complet retirait le constructeur de classes que les bibliothèques créent par leur nom (les composants
  de ML Kit, les « InputMerger » de WorkManager 2.7.1 amené par Glance) ; WorkManager ne lançait plus aucune tâche,
  donc plus de session Glance. Deux règles dans `app/proguard-rules.pro`. Reproduit puis vérifié sur l'émulateur avec
  une release R8 : étiquette lue (4 valeurs sur 4), widget affiché.
- Vibration de fin de repos « absente » : Android l'avait bien jouée, mais l'annulait en entier quand le développeur
  éteignait l'écran avec le bouton latéral pendant le décompte (« cancelled_by_screen_off », 5 repos sur 18 le
  9 novembre, dans l'app comme depuis l'écran d'accueil). Chaque temps fort lance désormais sa propre vibration.
- Petits dings du décompte : à 3, 2 et 1 s, un ding bref (`res/raw/rest_tick.wav`, produit par
  `tools/sounds/build_rest_tick.py`, même timbre que le ding ding) avec l'impulsion ; à la fin, le ding ding et la
  vibration longue. Sons préchargés avec `SoundPool` ; l'alarme sonne 5 s avant la fin pour les charger (plus d'1 s
  sur l'émulateur). Vérifié sur l'émulateur, app quittée et bouton latéral pressé entre deux dings : quatre
  vibrations jouées en entier à 1 s d'écart, quatre sons, musique baissée puis rendue.
- Accueil : flamme et « STREAK » empilés avec « SEMAINES » à droite du chiffre ; plus de « Encore x séances » ni de
  « Modifier l'objectif » (l'objectif se change dans les Réglages).

## Phase 11 — Version publique sur GitHub : terminée le 3 novembre 2026

Demande du développeur : un beau README et le dépôt public pour que d'autres puissent télécharger l'app. Tranché le
3 novembre 2026 : licence MIT, captures sur l'émulateur avec des données fictives, e-mail des commits laissé visible,
passage du S25+ à la version publique avec sauvegarde. Écart avec « Pas de publication » noté dans `CLAUDE.md`
(GitHub seulement, toujours pas de Google Play).

Fait, un commit par étape :
- Clé de la version publique créée hors du dépôt (`C:/Users/paulf/keys/liftapp-release.jks`, mots de passe dans
  `keystore.properties`, ignoré par Git), version 1.0.0. À sauvegarder ailleurs : sans elle, plus de mise à jour.
- Release R8 vérifiée sur l'émulateur en suivant le chemin du développeur : sauvegarde depuis la version de test,
  désinstallation, installation de la 1.0.0, restauration (séance en cours comprise), permissions, alarme app quittée
  et écran verrouillé (service autorisé, vibration, son en entier), pavé, courbes, images du catalogue.
- README (aperçu, fonctionnalités, installation de l'APK, vie privée, développeurs, crédits), licence MIT,
  six captures dans `docs/screenshots/`, politique de confidentialité complétée.
- Dépôt rendu public, description et sujets ajoutés, release `v1.0.0` avec `LiftApp.apk` (25,6 Mo), lien de
  téléchargement du README vérifié.

- S25+ passé à la 1.0.0 le 3 novembre 2026 : sauvegarde ZIP comparée table par table avec les données de l'app
  (identique, base intègre), copie sur le PC (`Documents/LiftApp-sauvegardes`), désinstallation de la version de
  test, installation, restauration validée par le développeur. Désormais, installer sur le téléphone avec
  `installRelease`, jamais `installDebug` (autre signature : Android refuserait la mise à jour).

## Retours du test sur le téléphone (2 novembre 2026) : corrigés et validés sur le téléphone

Deuxième série de retours du développeur, avec une capture de Strong en modèle pour la saisie. Questions tranchées
le 2 novembre 2026 (voir `CLAUDE.md`) ; un commit par point :
- Fin de repos : sur le S25+, le son ne sortait jamais et la vibration s'arrêtait app quittée. L'alarme lance
  désormais un service de premier plan de quelques secondes (`RestAlertService`, type `shortService`, permis depuis
  une alarme exacte) : l'app vibre et joue elle-même le ding ding en usage « alarme », chargé pendant le décompte,
  en baissant la musique. Écart avec la décision du 21 octobre (pas de service), validé. Vérifié sur l'émulateur,
  app quittée et écran verrouillé, en vibreur : service autorisé (« ALARM_MANAGER_WHILE_IDLE »), vibration jouée,
  son lu en entier (44 100 échantillons). L'alarme part 0,5 s plus tôt pour ne pas perdre la première impulsion.
  Confirmé sur le S25+ le 2 novembre 2026 (journaux : trois impulsions, son pile à la fin, app quittée validée par
  le développeur). Puis, à sa demande, le son suit le volume multimédia au lieu de celui du réveil.
- Mode salle : PASSER tient en entier (−15 s et +30 s se partagent le reste) ; les gros − et + agissent enfin sur la
  case ouverte (elle gardait le texte tapé et ignorait les appuis).
- « Précédent » et pré-remplissage : seulement la dernière séance de ce template, rien s'il n'y a jamais été fait
  (un exercice dans deux templates a deux historiques). Séance sans template : tous templates confondus.
- Pavé numérique façon Strong à la place du clavier d'Android (séance, mode salle, séance passée) : chiffres, virgule
  ou deux-points, effacer, fermer, − et + aux pas du mode salle, RPE, SUIVANT. La valeur est sélectionnée à
  l'ouverture. « Suivant » passe à la case d'après et valide la série quittée en séance ; la liste suit, le minuteur
  reste visible au-dessus. Logique de saisie testée (`KeypadInputTest`). Pas de touche de signe : sur le lest, −
  descend sous zéro (assistance), pour garder des touches d'au moins 48 dp.
- Contours fins : trait blanc à 22 % autour des cartes (`LiftCard`), tuiles, cases de saisie et vignettes ; gris en
  thème clair. Les contours ambre (dernier record, récap) restent.
- 261 tests JVM et 44 tests instrumentés verts, lint sans nouvel avertissement. Vérifié sur l'émulateur : pavé
  (frappe, Suivant qui valide et change d'exercice, mode salle), contours, alarme app ouverte et app quittée.

## Retours du test sur le téléphone (1er novembre 2026) : corrigés

Retours du développeur après la retouche visuelle, traités un commit par point :
- Bloc noir au-dessus du clavier pendant la saisie d'une série : la liste ajoutait toute la hauteur du clavier en
  plus de la barre du bas déjà décomptée. `consumeWindowInsets` sur la séance, la modification d'une séance passée et
  l'édition d'un template. Vérifié sur l'émulateur.
- Prochaine séance : « Pull » était reproposé après une séance Pull, car ses templates ne sont pas dans un dossier
  (règle du 6 octobre). Les templates hors dossier tournent désormais dans l'ordre de la liste (décision notée dans
  `CLAUDE.md`, testée).
- Fin de repos : vibration dès 3 s avant la fin (une impulsion par seconde, puis pendant le son) et « ding ding »
  d'1 s synthétisé pour l'app (`res/raw/rest_end_chime.wav`), joué par une notification à l'usage « alarme » : il
  s'entend en vibreur et en silencieux. Depuis Android 15, il faut la catégorie « alarme » sur la notification, sinon
  Android repasse le son en usage « notification » (coupé en vibreur). Vérifié sur l'émulateur en vibreur, app en
  arrière-plan (journal du lecteur de sons et de la vibration).
- Image d'un exercice agrandie d'un appui (séance, template, aperçu, séance passée).
- Widget : grande taille (250 × 200 dp et plus, 4 × 2 sur la plupart des téléphones). En séance : durée, série et
  dernière fois, série à faire en grand, progression avec barre, repos et « Série validée ». Hors séance : jours de
  la semaine et tuiles tonnage et durée. Posé et vérifié sur l'émulateur dans les deux états.
- 255 tests JVM et 44 tests instrumentés verts, lint sans nouvel avertissement.

## Retouche visuelle ON AIR : code terminé le 1er novembre 2026

Demande du développeur : rapprocher l'Accueil et la séance en cours de la maquette `design-system/concept.html`.
Tranché le 1er novembre 2026 : style des boutons appliqué partout, tuiles « Cette semaine » ajoutées, durée de la
prochaine séance = durée réelle de la dernière séance avec ce template (rien si jamais faite).

Fait en une traite à la demande du développeur, un commit par étape :
- Boutons communs (`core/designsystem/component/Buttons.kt`) : 88 boutons des écrans et les boutons flottants
  convertis ; le clavier du code et le sélecteur de type de série gardent leur forme.
- Accueil : en-tête « DIMANCHE 1 NOVEMBRE / SEMAINE 44 » et enseigne « HORS ANTENNE » ou « EN DIRECT », halo rouge
  en haut (`--wash`), semaine sans carte avec la rangée des jours, carte de la prochaine séance (dossier, « 5 exercices
  · 17 séries · ~ 49 min », puces), tuiles « Cette semaine », dernier record en lueur ambre.
- Séance en cours : progression « 3 / 17 séries · 660 kg », muscles et matériel, puces « Repos 2 min » (change le
  repos de l'exercice) et « 14 oct. : 82,5 × 8 », prochaine série entourée de rouge, minuteur compact sur une ligne,
  « TERMINER LA SÉANCE » à contour. Le mode salle garde son grand minuteur.
- Vérifié sur l'émulateur : thèmes sombre et clair, accueil, « Tout voir » vers l'Historique, séance (validation,
  changement de repos, mode salle). Libellés lus en casse normale par le lecteur d'écran.
- 253 tests JVM (nouveaux : jours entraînés, numéro de semaine, dernière durée, tuiles de la semaine avec le
  changement d'heure, meilleure série) et 44 tests instrumentés verts. Lint sans nouvel avertissement. Ni nouvelle
  dépendance, ni changement de schéma.

Plan validé le 1er novembre 2026 :
1. Boutons du guide de style partout : composants communs (`core/designsystem/component/Buttons.kt` : plein, tonal,
   contour, texte) avec coins de 8 dp (12 dp pour les grands) et libellés en majuscules, puis remplacement des
   boutons Material par défaut dans les écrans (environ 170 appels, sans changer leur rôle).
2. Accueil, en-tête et semaine : date du jour et « SEMAINE 44 » (numéro ISO, semaine du lundi au dimanche) à la place
   de « ACCUEIL », badge « HORS ANTENNE » ou « EN DIRECT » (séance en cours, un appui l'ouvre) ; anneau avec lueur ;
   rangée L M M J V S D (jour avec une séance terminée en rouge, aujourd'hui entouré). « Encore n séances » et
   « Modifier l'objectif » gardés.
3. Accueil, prochaine séance et record : dossier du template, nom en majuscules, « 5 exercices · 18 séries · ~1 h 05 »
   (durée de la dernière fois), puces des premiers exercices puis « +n », bouton « DÉMARRER » ; dernier record entouré
   de la lueur ambre. Souvenir gardé. Calculs dans `home/domain`, testés.
4. Tuiles « Cette semaine » : tonnage (séries validées hors échauffements) et durée totale avec le nombre de séances ;
   écart en % avec la semaine précédente prise au même moment (du lundi jusqu'au même jour et à la même heure), rien
   si elle était vide ; « Tout voir » ouvre l'Historique. Calculs testés (semaine à cheval sur deux mois, semaine vide,
   échauffements exclus).
5. Séance en cours, haut : barre de progression « 7 / 16 séries · 4 120 kg » sous le titre ; sous chaque exercice,
   muscles · matériel, puces « Repos 2 min » (un appui change le repos) et dernière fois « Lundi : 80 × 8 » (jour si
   moins d'une semaine, sinon date ; meilleure série de travail). Photo de l'exercice gardée.
6. Séance en cours, tableau et minuteur : en-têtes en petites capitales, prochaine série à valider entourée de rouge,
   « + AJOUTER UNE SÉRIE » ; minuteur de repos compact sur une ligne (REPOS 01:47, −15 S, +30 S, passer), l'avertissement
   d'alerte en retard en petit dessous ; « TERMINER LA SÉANCE » en bouton à contour.
7. Vérifications : thèmes clair et sombre, TalkBack et zones de 48 dp, mode salle, captures avant et après sur
   l'émulateur, `PROGRESS.md`.

Ni nouvelle dépendance, ni changement de schéma. Fichiers principaux : `home/ui/HomeScreen.kt`, `WeekCard.kt`,
`HomeViewModel.kt`, `home/domain/Dashboard.kt`, `home/data/DashboardRepository.kt`, `workout/ui/WorkoutScreen.kt`,
`WorkoutSetRow.kt`, `RestTimer.kt`, `core/designsystem/component/`, `strings.xml`.

## Phase 11 réduite à un usage personnel (plan du 31 octobre 2026, remplacé le 3 novembre par la version publique)

Décidé le 31 octobre 2026 : pas de publication sur Google Play (le compte développeur est payant), l'app reste pour
le développeur. Écart avec le cahier des charges (§ 8 et phase 11), noté dans `CLAUDE.md`. À lancer après la
retouche visuelle.

Plan proposé (un commit par étape) :
1. Version release (optimisée par R8) vérifiée sur l'émulateur : catalogue et images, séance, minuteur et
   notification, widget, photos et verrou, export, sauvegarde et restauration. R8 renomme et retire du code, ce qui
   peut casser ce qui passe par réflexion.
2. Installation sur le S25+ par-dessus la version de développement. Sans `keystore.properties`, la release est
   signée avec la clé de debug de ce PC : même signature, donc mise à jour sans perte de données. Sauvegarde ZIP
   avant, par précaution.
3. README : « Installer sur son téléphone » à la place de « Version release pour Google Play », avancement à jour.
   Rappel : sur un autre PC (autre clé de debug), il faut désinstaller avant d'installer, donc sauvegarder d'abord.
4. Documents qui ne servaient qu'à la publication (politique de confidentialité, fiche et captures Play Store,
   `tools/play-store/`) : à garder sans les tenir à jour, ou à supprimer. À trancher par le développeur.

## Phase 10 — Réglages et données : terminée le 1er novembre 2026

Testée et validée par le développeur sur le S25+ le 1er novembre 2026 (sauvegarde, restauration, export).
Installation sur le téléphone : il a fallu désactiver le Blocage automatique de Samsung (Paramètres, Sécurité et
confidentialité) pour activer le débogage USB.

Fait en une traite à la demande du développeur, un commit par étape :
- Écran Réglages (Profil, « Réglages ») : thème système, clair ou sombre ; objectif hebdomadaire ; repos par défaut
  des exercices ajoutés ; son et vibration de fin de repos (aussi dans le menu de la séance).
- Unités kg/lb et cm/in, choisies sur l'écran de bienvenue et dans les réglages. Valeurs calculées arrondies à
  0,5 kg ou 1,25 lb, saisie au dixième de livre, mode salle à ±5 lb. Partout : séance, historique, records, courbes,
  objectifs, projection, récap, mesures, taille, notification, widget. La base reste en métrique.
- Masquage (section Confidentialité) : le poids de corps et la masse grasse disparaissent des mesures du Profil et
  de la saisie ; la section Photos du Profil est cachée. Rien n'est effacé.
- Export CSV de l'historique (une ligne par série validée) et des mesures (une ligne par saisie), dans un fichier
  choisi avec le sélecteur d'Android, au format Excel français.
- Sauvegarde ZIP (base, préférences, photos au choix avec avertissement) et restauration qui remplace tout après
  confirmation, avec redémarrage de l'app.
- Politique de confidentialité (export et sauvegarde, fichier non chiffré) et fiche Play Store à jour.
- Vérifié sur l'émulateur : réglages, thème, unités en lb et in (Profil, Accueil, Historique, détail d'une séance),
  exports relus sur le PC (BOM, en-têtes, séparateurs), sauvegarde relue sur le PC (base intègre, 23 séances,
  4 photos, préférences sans les clés propres au téléphone), restauration complète (réglage modifié après la
  sauvegarde revenu, historique et photos intacts), faux ZIP refusé.
- 243 tests JVM et 44 tests instrumentés verts (dont la sauvegarde : avec et sans photos, ZIP étranger, version
  plus récente). Lint sans nouvel avertissement. Aucune nouvelle dépendance, pas de changement de schéma.
- Émulateur : données de démonstration intactes (suite instrumentée lancée sans désinstaller), unités remises en kg
  et cm ; une sauvegarde `liftapp-sauvegarde-2026-10-31.zip` est dans Téléchargements.

Choix tranchés par le développeur le 31 octobre 2026 (détail dans `CLAUDE.md`) : masquer le poids cache aussi la
masse grasse (changé ensuite dans le code) ; exports et sauvegarde complets même si le poids ou les photos sont
masqués ; une sauvegarde sans photos efface les photos actuelles à la restauration ; restauration seulement dans
les Réglages.

Plan validé le 28 octobre 2026 :
1. Écran Réglages (depuis le Profil) : thème (système, clair, sombre), objectif hebdomadaire, temps de repos par
   défaut, son et vibration (gardés aussi dans le menu de la séance), masquage du poids de corps et des photos.
2. Unités kg/lb et cm/in : conversions et arrondis testés (0,5 kg, 1,25 lb), affichage et saisie partout (séance,
   historique, objectifs, mesures, taille, notification, widget), mode salle à ±5 lb, choix sur l'écran de bienvenue.
   La base reste en métrique.
3. Masquage : poids de corps retiré de toute l'app, section photos du Profil cachée.
4. Export CSV de l'historique (une ligne par série) et des mesures, enregistré par le sélecteur de fichiers.
5. Sauvegarde complète en ZIP (base, réglages, profil, photos au choix) et restauration qui remplace tout : fichier
   vérifié avant, version plus récente refusée, migrations Room pour une version plus ancienne, redémarrage de l'app.
6. Documentation, tests, émulateur, `PROGRESS.md`.

Pas de nouvelle dépendance prévue (ZIP et CSV avec la bibliothèque standard, sélecteur de fichiers d'Android).
Décidé le 28 octobre 2026 (détail dans `CLAUDE.md`) : CSV au format Excel français, photos dans la sauvegarde au
choix (cochées par défaut, avertissement), restauration par remplacement.

## Phase 9 — Tableau de bord et widget : terminée le 28 octobre 2026

Testée et validée par le développeur sur le S25+ le 28 octobre 2026.

Fait en une traite à la demande du développeur, un commit par étape :
- Prochaine séance et dernier record (calculs testés), `DashboardRepository` commun à l'accueil et au widget.
- Écran Accueil, premier onglet, ouvert au lancement : semaine (anneau, streak, objectif modifiable), prochaine
  séance avec « Démarrer » (« Reprendre » si une séance est en cours, « Séance vide » sans template), dernier record,
  souvenir. Progrès garde carte du corps, objectifs, badges, récap et exercices.
- Widget Glance, petite et moyenne taille. Hors séance : anneau, streak, prochaine séance, dernier record,
  « Démarrer » (l'app s'ouvre sur la séance créée). En séance : exercice, série, compte à rebours du repos ou durée de
  la séance (Chronometer d'Android), « Série validée ». Aperçu dans le sélecteur de widgets.
- Mises à jour du widget : à chaque changement de ses données tant que l'app tourne, après une action de la
  notification ou du widget, à la fin d'un repos, et toutes les 3 h par Android.
- Politique de confidentialité (widget visible sur l'écran d'accueil), fiche Play Store et crédits (Glance) à jour.
- Vérifié sur l'émulateur : accueil et « Démarrer », widget posé depuis le sélecteur, « Démarrer » du widget,
  passage en séance, « Série validée » depuis le widget, compte à rebours puis durée de la séance à la fin du repos,
  petite taille, retour hors séance après abandon.
- 221 tests JVM et 40 tests instrumentés verts (dont `startOrResume`). Lint sans nouvel avertissement.
- Émulateur : données de démonstration restaurées après les tests instrumentés ; le widget est à reposer à la main
  (la désinstallation l'a retiré).

Plan validé le 28 octobre 2026 :
1. Prochaine séance, calcul testé : template suivant le dernier utilisé dans son dossier, sinon le dernier utilisé ;
   template supprimé, dossier d'un seul template, aucun historique (premier template de la liste), aucun template
   (« Séance vide »).
2. Dernier record, calcul testé : le dernier record battu tous exercices confondus, avec sa date.
3. Écran Accueil (package `home`) : semaine (anneau et streak), prochaine séance avec « Démarrer », dernier record,
   souvenir. Onglet Accueil rétabli en premier, l'app s'ouvre dessus.
4. Widget hors séance (Glance) : anneau dessiné en image, streak, prochaine séance avec « Démarrer », dernier record ;
   petite et moyenne taille.
5. Widget en séance : exercice, série, poids et reps, compte à rebours du repos par le `Chronometer` d'Android
   (`AndroidRemoteViews`, sans rafraîchir le widget chaque seconde), « Série validée » par le récepteur de la
   notification.
6. Mises à jour du widget : changements de la séance et fin de repos (`WorkoutSessionSync`), fin ou modification
   d'une séance, mise à jour périodique de secours pour le changement de semaine.
7. Aperçu dans le sélecteur de widgets, politique de confidentialité, fiche Play Store, tests, émulateur,
   `PROGRESS.md`.

Décidé le 28 octobre 2026 (détail dans `CLAUDE.md`) : répartition Accueil et Progrès selon le cahier, « Démarrer »
du widget lance la séance, Glance 1.2.0 (`glance-appwidget` et `glance-material3`) validé.

## Phase 8 — Motivation : terminée le 28 octobre 2026

Testée et validée par le développeur sur le S25+ le 28 octobre 2026.

Plan validé le 28 octobre 2026, fait en une traite à la demande du développeur (un commit par étape).
Règles métier tranchées le même jour : voir `CLAUDE.md`. Pas de nouvelle dépendance ni de changement de schéma :
`goal`, `weekly_goal` et `badge` existent depuis la v1 (seuls un DAO et des requêtes ont été ajoutés).

Fait :
- Objectif hebdomadaire historisé (choix sur l'écran de bienvenue, puis depuis la carte de la semaine, valable dès la
  semaine en cours) et streak en semaines ; anneau `WeeklyRing` du guide de style.
- Records battus calculés en rejouant l'historique (`recordBreaks`, `liveRecords`) : célébration pendant la séance
  (bandeau ambre avec rebond, vibration, annonce TalkBack), jamais à la première séance sur un exercice.
- Résumé de fin de séance : records battus avec l'ancienne valeur, volume comparé à la dernière séance du même
  template, paliers et badges débloqués.
- Carte du corps (7 jours glissants, cardio exclu) avec les tracés MuscleMap, silhouette de femme si le profil
  l'indique, liste des muscles en texte. Crédit et licence MIT dans « À propos ».
- Objectifs à paliers sur le 1RM estimé (bronze et argent proposés, modifiables), badges (séances, semaines
  d'affilée, clubs), souvenir (12, 6, 3 ou 1 mois), récap mensuel partageable en image, projection en fourchette.
- Tout s'affiche en haut de l'onglet Progrès en attendant l'accueil (phase 9) ; objectif et projection aussi sur la
  progression d'un exercice.
- Politique de confidentialité (partage volontaire du récap) et fiche Play Store mises à jour.
- Vérifié sur l'émulateur avec les données de démonstration : anneau, souvenir, carte du corps, badges rattrapés à
  leur date d'origine, célébration d'un record, résumé, objectif, récap et menu de partage, projection, thème clair.
- 213 tests JVM et 39 tests instrumentés verts (dont `MotivationDataTest`). Lint sans nouvel avertissement.
- Corrigé avant commit : `LocalDate.EPOCH` (Android 14 et plus seulement) aurait planté sur Android 8 à 13, repéré
  par lint ; suppression d'un objectif désormais confirmée (ses paliers atteints seraient perdus).

Le test fermé reste en pause (compte Google Play plus tard) : un build issu de la phase 7 peut lui servir.

## Abandonné : préparation du test fermé (une partie de la phase 11)

Abandonné le 31 octobre 2026 : pas de publication. Le travail fait reste utile (release signée, icône, écran de
bienvenue, « À propos » et licences).

Le 21 octobre 2026, le développeur a choisi de passer à la phase 7 et de créer le compte Google Play plus tard.
Envoyer sur le Play Store un build issu d'une phase terminée.

Décidé le 21 octobre 2026 : le test fermé passe avant la phase 7, car Google exige 12 testeurs inscrits pendant
14 jours d'affilée avant la production. Les phases 7 à 9 avancent pendant le test. Nom et identifiant gardés :
« LiftApp », `com.paulfvt.liftapp` (l'identifiant ne pourra plus changer après le premier envoi).

Plan validé le 21 octobre 2026 :
1. Version de release signée : clé d'envoi créée par le développeur, mots de passe dans `keystore.properties`
   (hors Git), Play App Signing. Release testée sur l'émulateur (R8).
2. Pendant le test, l'app s'ouvre sur Séances et l'onglet Accueil est masqué (retour en phase 9).
3. Onglet Profil réduit pour l'instant à « À propos » : version et licences (free-exercise-db, Barlow, bibliothèques
   Apache 2.0), écrit à la main sans nouvelle dépendance.
4. Premier lancement : un seul écran de bienvenue (ce que fait l'app, données gardées sur le téléphone,
   « Commencer »). Unités (phase 10) et objectif hebdomadaire (phase 8) s'y ajouteront.
5. Icône ON AIR : maquette dans `design-system/` à valider, puis icône adaptative en vecteur et version 512 px.
6. Politique de confidentialité en français, hébergée sur un petit dépôt public à part (le dépôt de l'app est privé).
7. Fiche Play Store : descriptions, captures, bannière, réponses préparées pour « Sécurité des données » et la
   classification du contenu.

À faire par le développeur : compte développeur Google Play (25 $, vérification d'identité), 12 testeurs avec une
adresse Gmail, clé d'envoi et `keystore.properties` (commande dans le README).

Fait le 21 octobre 2026 :
- Étape 1 : signature configurée. Release minifiée par R8 testée sur l'émulateur (catalogue et images, template,
  séance, minuteur, fin de séance, historique, courbes) sans plantage.
- Étapes 2 à 4 : app ouverte sur Séances (4 onglets), Profil → « À propos » → texte des licences, écran de bienvenue.
- Étape 5 : icône « Néon » (piste B de `design-system/icon.html`, choisie par le développeur) en icône adaptative
  avec couche monochrome, vérifiée sur l'émulateur ; version Play Store dans `docs/play-store/icon-512.png`.
- Étape 6 : brouillon `docs/politique-de-confidentialite.md`, nom et adresse de contact à compléter.
- Étape 7 : `docs/fiche-play-store.md` (textes, réponses aux formulaires), image de présentation et six captures
  légendées dans `docs/play-store/`, faites sur l'émulateur avec des données de démonstration
  (`tools/play-store/`).
- Corrigé : l'aperçu d'un template cachait « Démarrer » sous le bord de l'écran à partir de 5 exercices. La feuille
  s'ouvre maintenant en entier et seule la liste défile, boutons fixés en bas (vérifié avec 4, 5 et 12 exercices).
- Reste : mise en ligne de la politique de confidentialité, test sur le S25+.

## Phase 7 — Profil, mesures et photos : terminée le 22 octobre 2026

- Profil fixe (prénom, taille, date de naissance, sexe, tous facultatifs) dans le DataStore, écran de modification.
- Mesures : poids, tours (bras, poitrine, taille, hanches, cuisse, mollet) et masse grasse avec sa méthode. Chaque
  saisie crée une entrée, suppression possible. Une courbe par mesure ; pour le poids, les pesées en points et la
  moyenne mobile sur 7 jours en ligne. Dernière valeur et écart dans l'onglet Profil.
- Photos : copie réduite et redressée dans `files/photos/`, timeline par mois filtrable par pose, import par le
  sélecteur de photos d'Android (date lue dans la photo), écran d'une photo avec suppression.
- Prise de photo avec CameraX : aperçu 3:4, silhouette de la dernière photo de la même pose, caméra avant ou arrière.
- Comparaison avant/après avec curseur, choix des deux photos, écart en jours.
- Verrouillage facultatif : PIN, visage ou empreinte, code oublié par le verrouillage du téléphone.
- Politique de confidentialité et fiche Play Store mises à jour (profil, mesures, photos, caméra, biométrie).
- 151 tests JVM et 35 tests instrumentés verts. Lint sans nouvel avertissement. Vérifié sur l'émulateur en debug et
  en release (R8) : profil, mesures, import, caméra avant et arrière, comparaison, verrou (mauvais code, attente,
  reverrouillage en quittant l'app, galerie ouverte sans reverrouiller).

Testé et validé par le développeur sur le S25+ le 22 octobre 2026 : caméra réelle et silhouette, visage ou
empreinte, « Code oublié ? » avec le verrouillage du téléphone, reverrouillage au retour, rotation sans reverrouillage.

Dépendances validées le 21 octobre 2026 : CameraX 1.6.2 (`camera-core`, `camera-camera2`, `camera-lifecycle`,
`camera-compose`), androidx.biometric 1.1.0, et androidx.exifinterface 1.4.2, déjà apportée par CameraX et déclarée
car utilisée directement. Pas de changement de schéma : `body_measurement` et `progress_photo` existent depuis la v1.
Reportés en phase 10 (Réglages, cahier § 4.11) : unités lb et in, masquage du poids et de la section photos.

## Phase 6 — Historique, records et courbes : terminée le 21 octobre 2026
- Onglet Historique : séances par mois, détail, modification (en mémoire jusqu'à « Enregistrer ») et suppression.
- Records recalculés en entier pour les exercices touchés : fin de séance, modification, suppression.
- Onglet Progrès et écran Progression d'un exercice : records datés, courbes (Vico) sur 1 mois, 3 mois, 1 an ou tout,
  reps max par charge. Accessible aussi depuis le détail d'une séance et la fiche d'un exercice.
- 118 tests JVM et 31 tests instrumentés verts. Vérifié sur l'émulateur, validé par le développeur le 21 octobre 2026.

## Phase 5 — Minuteur et notification : terminée le 21 octobre 2026
- Sans service de premier plan : notification avec compte à rebours dessiné par le système et alarme exacte pour
  l'alerte de fin de repos (voir « Décisions techniques »).
- Base v4 (début et fin du repos dans la séance), migration testée.
- Valider une série lance le repos ; −15 s, +30 s, Passer ; actions « Série validée » et « +30 s » dans la
  notification, Live Update sur Android 16.
- Permissions demandées à la première séance ; son et vibration de fin de repos désactivables.
- Mode salle : écran allumé, une série à la fois, gros boutons.
- 97 tests JVM et 25 tests instrumentés verts. Vérifié sur l'émulateur (alarme à l'heure écran verrouillé, son joué
  par le système), validé par le développeur le 21 octobre 2026.

## Phase 4 — Séance en cours : terminée le 21 octobre 2026
- Base v3 (repos et reps cibles dans la séance), migration testée.
- Logique pure testée : performance précédente, pré-remplissage, saisie des valeurs, résumé.
- `WorkoutDao`, `WorkoutRepository`, test de reprise après redémarrage.
- Démarrage (aperçu du template, séance vide), écran de séance, bandeau EN DIRECT, reprise automatique
  au lancement.
- Fin de séance : résumé, mise à jour du template, abandon. Thèmes clair et sombre.
- 79 tests JVM et 20 tests instrumentés, testé sur le S25+.
- Choix kg/lb reporté en phase 10.

## Phase 3 — Templates : terminée le 11 octobre 2026
- Design system « ON AIR » validé (`design-system/`) et appliqué au thème Compose.
- Onglet Séances : templates hors dossier puis dossiers repliables ; créer, renommer, dupliquer, déplacer,
  supprimer ; glisser-déposer dans une même section.
- Éditeur de template : séries détaillées (type et reps cibles par série), repos par exercice, catalogue en mode
  sélection multiple. Base en version 2 avec la première migration, testée.
- 53 tests JVM et 12 tests instrumentés verts, testé sur le S25+.
- Supersets : hors V1 (décision du 6 octobre 2026), donc absents de cette phase.

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
- **Écriture continue de la séance** : chaque saisie part dans une file d'écritures qui tourne dans le scope de
  l'application (`WorkoutViewModel`). Une seule coroutine la vide, donc l'ordre de frappe est respecté, et une saisie
  faite juste avant de quitter l'écran n'est pas perdue.
- **Champs de saisie de la séance** : le texte tapé n'est jamais remplacé pendant que le champ a le focus (la base
  renvoie la valeur avec un temps de retard) ; il est resynchronisé quand le champ perd le focus.
- **Séance en plein écran** : pas de barre du bas sur la séance ni sur le catalogue ouvert depuis elle. Ailleurs,
  un bandeau EN DIRECT ramène à la séance. Au lancement, une séance encore en cours est rouverte directement.
- **Séance abandonnée** : gardée avec le statut `DISCARDED` (modèle du cahier), ignorée par « Précédent ».
- **Couleur « success »** du guide de style ajoutée au thème Compose (`ExtendedColors`), Material 3 n'en ayant pas.
- **Minuteur sans service de premier plan** (validé le 21 octobre 2026) : aucun type de service ne convient
  proprement (`health` exige une permission de capteur ou « Activité physique » inutile ici, `specialUse` est
  examiné par Google, `shortService` est limité à 3 min). L'heure de fin du repos est enregistrée dans la séance ;
  la notification affiche un compte à rebours dessiné par Android à partir de cette heure, donc juste même quand
  l'app ne tourne pas. Une alarme exacte (`SCHEDULE_EXACT_ALARM`, accordée par l'utilisateur) déclenche l'alerte de
  fin de repos ; sans elle, alarme inexacte qui peut arriver en retard écran verrouillé.
- **Règles du repos** : valider une série lance le repos de son exercice (2 min si non réglé, rien si 0 s) ; pas de
  repos après la dernière série de la séance ; décocher une série ne touche pas au repos ; +30 s sur un repos terminé
  relance 30 s. L'alerte n'est jouée que si l'alarme arrive moins d'une minute après la fin du repos.
- **Alerte de fin de repos** : la vibration est jouée par l'app (usage « alarme », rien en silencieux), car un canal
  de notification garde la vibration choisie à sa création. Le son passe par une notification brève « Repos terminé »
  sur un canal à part, publiée seulement si le son est activé : un son joué directement par l'app est coupé par
  Android quand elle est en arrière-plan (constaté sur l'émulateur écran verrouillé). Vibreur, silencieux et « Ne pas
  déranger » sont alors gérés par Android.
- **Son et vibration réglés depuis le menu de la séance** (écart temporaire avec le cahier § 4.5 / § 4.10) : ils
  rejoindront l'écran Réglages en phase 10. Stockés dans DataStore (`WorkoutSettingsRepository`).
- **Après un redémarrage du téléphone**, notification et alarme reviennent à la réouverture de l'app (pas de
  récepteur de démarrage, qui demanderait une permission de plus). La séance et l'heure de fin du repos sont, elles,
  gardées en base.
- **Notification** : canal sans son ni vibration, visible écran verrouillé ; hors repos, elle affiche la durée de la
  séance. « Série validée » n'est proposée que si la série suivante a ses valeurs. Un appui ouvre la séance.
- **Mode salle** : interrupteur mémorisé dans le menu de la séance. Écran maintenu allumé, seule la série à faire est
  affichée, boutons − et + de 72 dp (2,5 kg, 1 rep, 15 s, 100 m en km ou 5 m), valeurs aussi modifiables au clavier.
  Les pas sont appliqués à la valeur en base pour que des appuis rapides s'additionnent.
- **Records** (validé le 21 octobre 2026) : les séries dégressives comptent, seuls les échauffements sont exclus.
  Une série à 1 rep a un 1RM égal au poids soulevé ; Epley au-delà, jusqu'à 12 reps. Les records sont toujours
  recalculés en entier pour un exercice (fin de séance, modification, suppression), jamais modifiés au coup par coup.
- **Records des exercices sans charge** (écart avec le cahier § 4.6, validé le 21 octobre 2026) : poids du corps =
  reps max et lest max, durée = durée max, distance = distance max, chacun avec sa courbe.
- **Graphiques avec Vico** (`compose-m3`, Apache 2.0, validé le 21 octobre 2026) : axes, zoom et valeur au toucher
  sans les recoder ; servira aussi aux mesures (phase 7) et à la projection (phase 8).
- **Signature de la release** : `keystore.properties` absent (CI, autre PC) → release signée avec la clé de debug,
  installable pour tester R8 mais refusée par Google Play, donc impossible à publier par erreur.
- **Package `onboarding`** ajouté à la liste du cahier des charges (écran de bienvenue, enrichi en phases 8 et 10).
  La préférence `welcome_done` est lue avant d'afficher quoi que ce soit : tant qu'elle ne l'est pas (quelques
  millisecondes), seul le fond de la fenêtre est visible.
- **Licences** : textes officiels Apache 2.0 et OFL 1.1 copiés depuis les notices d'Android Studio, copyright Barlow
  lu dans les fichiers de police. Rangés dans `res/raw` avec un paragraphe par ligne pour se replier à l'écran.
  Le catalogue est dans le domaine public (Unlicense) : crédité, sans texte de licence à joindre.
- **`BuildConfig` activé** pour afficher la version dans « À propos ».
- **Icône en vecteurs** : la lueur du néon est obtenue en repassant le même tracé quatre fois (halo large et
  transparent, puis cœur clair), car les vecteurs Android n'ont pas de flou. Version Play Store rendue depuis le même
  SVG par Chrome sans fenêtre (`--headless --screenshot`), sans nouvel outil à installer.
- **Aucune permission internet** dans l'app (vérifié dans le manifeste final) : argument de la politique de
  confidentialité et du formulaire « Sécurité des données » (aucune donnée collectée).

- **Courbes** : `ProgressChart` prend une valeur par jour (`core/domain/DailyValue`) et une tendance facultative.
  Séances : meilleure valeur du jour ; mesures : moyenne du jour. La moyenne mobile est calculée sur toute la série
  puis découpée à la période, pour que les premiers points tiennent compte des jours d'avant.
- **Mesures** : affichées au dixième ; une mesure d'un jour passé est enregistrée à midi (heure exacte inconnue).
- **Photos** : copie JPEG de 2048 px au plus, redressée selon l'EXIF puis enregistrée sans orientation. La prise de vue
  CameraX écrit d'abord dans le cache, puis passe par la même copie. Silhouette à 35 % d'opacité, retournée en miroir
  pour la caméra avant (son aperçu est un miroir, la photo enregistrée ne l'est pas).
- **Verrou des photos** : PIN de 4 à 8 chiffres, empreinte PBKDF2-SHA256 (120 000 tours, sel de 16 octets) dans le
  DataStore. Après 5 erreurs, attente de 30 s qui double à chaque erreur (15 min au plus), compteur gardé après un
  redémarrage. Reverrouillage dans `MainActivity.onStop` (sauf rotation), sauf juste après avoir ouvert le sélecteur
  de photos ou les réglages d'Android depuis l'app (`PhotoLockSession.keepUnlockedWhileAway`). Visage ou empreinte en
  classe « faible » (`BIOMETRIC_WEAK`), la plupart des visages en font partie. `PhotoLockGate` entoure chaque écran
  de photos dans la navigation : l'écran et son ViewModel ne sont créés qu'une fois déverrouillé.
- **Clés du SavedStateHandle** : jamais le nom d'un argument de la route (bug rencontré : la clé « pose » lisait
  l'argument vide de `PhotoCaptureRoute`).

- **Motivation dans le package `progress`** (pas de nouveau package) : calculs purs dans `progress/domain`
  (`WeeklyGoals`, `RecordBreaks`, `BodyMap`, `Goals`, `Badges`, `Flashback`, `MonthlyRecap`, `Projection`), un DAO
  `MotivationDao` pour `weekly_goal`, `goal` et `badge`. Textes dans `values/strings_motivation.xml`.
- **Objectif hebdomadaire** : le choix de la bienvenue est enregistré au 1er janvier 1970 (il vaut pour toutes les
  semaines) ; un changement ajoute une ligne au lundi de la semaine en cours, qui remplace un changement fait plus tôt
  dans la même semaine (index unique). Sans ligne (installations d'avant la phase 8), 3 séances par défaut.
- **Records battus** : le cache `personal_record` ne garde que le meilleur actuel. `recordBreaks` rejoue l'historique
  séance par séance (résumé, récap) ; `liveRecords` compare la séance en cours au cache et aux séries déjà validées.
  Les couples « série, mesure » déjà célébrés sont gardés (`rememberSaveable`) : pas de nouvelle célébration après une
  rotation ; une série validée depuis la notification est célébrée au retour dans l'app.
- **Paliers et badges** : `AchievementRepository.refresh()` après une fin de séance (dans sa transaction, résultat
  montré dans le résumé), une séance modifiée ou supprimée, et au lancement (rattrapage silencieux de l'historique,
  badges datés au moment où ils ont été mérités). Il n'ajoute jamais que ce qui manque.
- **Clubs** : liste des clés free-exercise-db dans `CLUBS` (`Badges.kt`) ; un exercice personnalisé ne compte pas.
- **Carte du corps** : `app/src/main/assets/body-map/body_map.json` (230 Ko) produit par
  `tools/body-map/build_body_map.py` (commit MuscleMap figé, option `--source` pour une copie locale). Tracés convertis
  une fois en `Path` Compose par le ViewModel, hors du fil principal ; face et dos dans un cadre commun.
- **Image du récap** : la carte est dessinée dans un `GraphicsLayer` puis convertie en image (ni capture d'écran ni
  permission), PNG dans `cache/share/`, partagé via le `FileProvider` `${applicationId}.fileprovider`.
- **Projection** : régression de y = a + b × ln(1 + jours / 30) sur les 6 derniers mois, horizon 90 jours affiché en
  période (début, mi, fin de mois). Fourchette d'environ 80 % (±1,28 écart-type des résidus, au moins ±1,25 kg),
  borne basse jamais sous le meilleur déjà atteint ; pas de projection au-delà de ±10 % d'incertitude.
- **Ressources texte** : une apostrophe doit être écrite `\'`. Un script Python ou un heredoc du shell peut perdre la
  barre oblique : aapt refuse alors le fichier (« Invalid unicode escape »).
- **Accueil et widget partagent `DashboardRepository`** (`home/data`) : prochaine séance (`nextTemplate`), dernier
  record (`latestRecord`), streak et souvenir sont calculés d'une seule façon. Les templates récemment utilisés
  viennent d'une requête groupée sur `workout.templateId`.
- **Démarrage commun** : `WorkoutRepository.startOrResume` rouvre la séance en cours s'il y en a une, sinon démarre
  celle demandée ; faux si le template a été supprimé entre-temps (le widget peut afficher un état ancien).
- **« Démarrer » du widget** : ouvre `MainActivity` avec l'action `START_WORKOUT` (et l'identifiant du template).
  Lancer un écran depuis un récepteur en arrière-plan est bloqué par Android ; l'ouverture directe de l'app par un
  appui sur le widget, elle, est toujours permise. Traité dans `onCreate` (pas après une rotation) et `onNewIntent`.
- **Widget (package `widget`)** : `LiftWidget` (Glance) lit ses données par un point d'entrée Hilt (`WidgetEntryPoint`)
  car Android l'instancie lui-même. Pendant une séance, le tableau de bord n'est pas calculé (`flatMapLatest`) : chaque
  saisie relancerait le calcul pour rien. Couleurs : `ColorProviders` de glance-material3 avec les schémas du thème ;
  l'anneau (image) et le chronomètre (vue Android) utilisent des couleurs `widget_*` de `res/values(-night)`.
- **Chronomètre du widget** : `AndroidRemoteViews` avec un `Chronometer` (layouts `widget_*_chronometer.xml`), base
  exprimée en `elapsedRealtime`. Il décompte seul ; l'app redessine le widget à la fin du repos
  (`WorkoutSessionSync`) pour passer à la durée de la séance.
- **Package `settings`** (phase 10) : `data` (réglages DataStore, export CSV, sauvegarde), `domain` (CSV,
  manifeste et contrôles de la sauvegarde, testés), `ui` (écran Réglages, section Données).
- **Unités** : `core/domain/Units.kt` (conversions et arrondis, testés), `core/ui/UnitFormats.kt` (textes). Les
  unités choisies sont fournies aux écrans par un `CompositionLocal` (`LocalUnits`) depuis `MainActivity`.
- **CSV** : point-virgule, virgule décimale, dates jj/mm/aaaa, fin de ligne CRLF, BOM UTF-8 (sans lui, Excel abîme
  les accents). Un nom saisi qui commence par `=`, `+`, `-` ou `@` reçoit une apostrophe devant, pour qu'un tableur
  ne l'exécute pas comme une formule.
- **Format de sauvegarde** : ZIP avec `manifest.properties` (format, version du schéma Room, date, photos),
  `database/lift.db`, `preferences/lift_preferences.preferences_pb`, `photos/…`. La base est copiée pendant une
  transaction Room (fichier et journal WAL cohérents), puis le journal est reporté dans la copie. Les préférences
  propres au téléphone (`workout_permissions_asked`, erreurs du code des photos) ne sont pas sauvegardées.
- **Boutons du guide de style** : les boutons Material 3 sont en pilule et le thème ne permet pas de changer leur
  forme ; `SignalButton`, `LiftOutlinedButton`, `LiftTonalButton` et `LiftTextButton` prennent le libellé en texte,
  le mettent en capitales espacées et le font lire dans sa casse d'origine (description pour le lecteur d'écran).
- **Accueil** : `Dashboard` porte aussi la date du jour, les jours entraînés, les tuiles de la semaine
  (`home/domain/WeekStats.kt`), le dossier et la dernière durée de la prochaine séance ; le halo `--wash` est une
  couleur `wash` d'`ExtendedColors`.
- **Restauration en deux temps** : le ZIP est extrait dans `no_backup/restore/` (entrées connues seulement, taille
  bornée à 4 Go) et la base y est ouverte par Room, qui applique les migrations et vérifie le schéma ; rien n'est
  remplacé si elle échoue. Après confirmation, l'app redémarre (`Intent.makeRestartActivityTask` puis arrêt du
  processus) et `PendingRestore` remplace base, préférences et photos dans `LiftApplication.onCreate()`, avant que
  Hilt n'ouvre quoi que ce soit.
- **Bouton rond du widget** : fait main (Box et Image teintée) ; le `CircleIconButton` de Glance affichait une coche
  minuscule.

- **Table Ciqual** (phase 12) : version 2020, la plus récente trouvable automatiquement le 3 novembre 2026. Le site
  ciqual.anses.fr était en maintenance et la version plus récente de recherche.data.gouv refuse les robots : le
  script prend la copie déposée par l'Anses sur Zenodo (même fichier, empreinte SHA-256 vérifiée). Pour une autre
  version : `python tools/nutrition/build_foods.py --source fichier.zip`, puis augmenter `FOODS_VERSION`.
- **Lecture de la table** : le fichier des compositions n'est pas du XML valide (« < 0,5 » brut), d'où une lecture par
  motifs. Protéines « N x facteur de Jones » (celles des étiquettes) ; « traces » = 0, « < x » = x / 2 ; énergie
  manquante calculée avec les coefficients du règlement UE 1169/2011 (4, 4, 9, fibres 2, alcool 7).
- **Journal** : `food_entry.date` est un jour du calendrier (LocalDate), pas un Instant : un repas appartient à une
  journée. Chaque ligne copie les valeurs pour 100 g de l'aliment, les jours passés ne changent donc jamais.
- **Recherche d'aliments** en mémoire, comme les exercices (3 100 noms) : mots dans le désordre, accents, « œ » et
  pluriel ignorés. Le texte du champ est un état à part (`queryText`) : relié au calcul des résultats, hors du fil
  principal, il perdait des lettres tapées vite (constaté sur l'émulateur).
- **Pavé numérique** : `NumericKeypad` (core/designsystem) est partagé par la séance et la quantité d'un aliment.
- **Couleurs des macros** : protéines en `primary`, lipides en `secondary`, glucides en bleu (`carbs` d'`ExtendedColors`,
  `--macro-carbs` dans `design-system/styles.css`) ; le libellé est toujours écrit à côté.
- **Lecture des étiquettes** : ML Kit rend des lignes et leur boîte ; `readNutritionLabel` associe chaque libellé
  aux chiffres de sa rangée (tableaux lus en colonnes) et prend la première colonne, celle pour 100 g en Europe.
  L'appareil photo d'Android (`TakePicture`) écrit dans `cache/label/` (FileProvider), effacé après lecture ; la
  permission caméra est exigée avant, l'app la déclarant pour les photos de progression.
- **ML Kit sans internet** : la bibliothèque ajoute la permission internet et programme l'envoi de statistiques
  d'usage (datatransport) ; la permission est retirée du manifeste, l'envoi échoue sans bruit et se reprogramme.
  Permissions finales identiques à la 1.0.0.
- **Mes besoins** : sans pesée, pas d'objectif (protéines et lipides sont par kg) ; sans sexe, date de naissance ou
  taille, les calories se saisissent à la main. Les réglages sont gardés dans la ligne de `nutrition_goal`.
- **R8 en mode complet** (par défaut depuis AGP 8) : `-keep class X` ne garde plus le constructeur sans argument. Une
  bibliothèque qui crée des classes par leur nom (manifeste, base de WorkManager) casse alors en release seulement,
  sans erreur à la compilation. Toute fonction qui repose sur une bibliothèque se vérifie sur une release R8 avant
  d'installer sur le téléphone (voir Problèmes connus pour l'émulateur).
- **Alerte de fin de repos par temps forts** (9 novembre 2026) : `restAlertBeats` donne les temps forts encore à venir
  (3, 2, 1 s, puis la fin) ; `RestEndAlert` relit la base avant chacun (repos passé ou prolongé : tout s'arrête) et
  lance une vibration et un son par temps fort. Une seule vibration pour tout le décompte était annulée en entier à
  l'extinction de l'écran.

## Questions ouvertes
- Aucune pour l'instant (choix des phases 7 et 10 tranchés le 31 octobre 2026).

## Problèmes connus
- Émulateur saturé (11 novembre 2026) : affichage et clavier à près de 100 % du processeur, l'app ne répond plus
  pendant une saisie (ANR dans le clavier, pas dans le code de l'app). Le relancer avec
  `emulator -avd Medium_Phone_API_37.0 -no-snapshot-load -gpu host`.
- Les pluriels suivent la langue du téléphone : l'émulateur, en anglais, écrit « 0 jours notés », le S25+ en français
  « 0 jour noté ». Ce n'est pas un bug de l'app.
- Le scan en direct ne se teste pas sur l'émulateur (sa fausse caméra ne montre aucune étiquette) : le parcours
  « Utiliser ces valeurs » se vérifie sur le téléphone ; « Choisir une image plutôt » passe par le même mécanisme.
- Release limitée à arm64-v8a et armeabi-v7a (modèle ML Kit d'environ 4 Mo par processeur) : lint signale
  `ChromeOsAbiSupport`, l'APK ne s'installe pas sur un Chromebook Intel. Ajouter x86_64 si besoin (+4 Mo).
  Pour essayer une release sur l'émulateur, ajouter x86_64 aux `abiFilters` le temps du test, sans le committer
  (sinon le scan de l'étiquette plante faute de bibliothèque ML Kit pour ce processeur).
- `connectedDebugAndroidTest` sur l'émulateur API 37 se termine sans erreur mais rapporte 0 test (2 novembre 2026).
  Les lancer à la main : `installDebug installDebugAndroidTest`, puis
  `adb shell am instrument -w com.paulfvt.liftapp.test/androidx.test.runner.AndroidJUnitRunner` (44 tests OK).
- Widget : une rangée ou une colonne Glance n'accepte que 10 éléments (limite des RemoteViews) ; au-delà, la fin est
  coupée sans erreur. Remplacer les `Spacer` par des marges quand une rangée est longue.
- Émulateur : après un arrêt brutal, il peut redémarrer depuis une sauvegarde rapide cassée (« Can't find service:
  input ») ; le relancer avec `-no-snapshot-load`.
- Widget : juste après une mise à jour de l'app, il affiche l'écran de chargement de Glance jusqu'à ce que l'app
  tourne à nouveau (une minute environ sur l'émulateur). Changer de thème clair ou sombre ne redessine l'anneau qu'à
  la mise à jour suivante (c'est une image).
- Widget : l'émulateur ne permet pas de le poser autrement qu'à la main (appui long sur l'écran d'accueil, Widgets,
  recherche « LiftApp », glisser l'aperçu). `connectedDebugAndroidTest` le retire en désinstallant l'app.
- Anneau et streak sont recalculés quand les données changent : si l'app reste ouverte au passage du dimanche au
  lundi, la nouvelle semaine n'apparaît qu'à la prochaine séance ou à la réouverture de l'onglet.
- Lint signale des versions plus récentes (Gradle 9.8.2, DataStore 1.3.0, Kotlin 2.4.21, kotlinx.serialization
  1.12.0, kotlinx-coroutines-test 1.11.0). Mises à jour à décider à part ; coroutines-test doit rester alignée sur
  1.10.2 (voir plus bas).
- `connectedDebugAndroidTest` désinstalle l'app de l'émulateur, données comprises. Pour garder les données :
  `./gradlew installDebug installDebugAndroidTest` puis
  `adb shell am instrument -w com.paulfvt.liftapp.test/androidx.test.runner.AndroidJUnitRunner` (toute la suite, sans
  désinstaller). Sinon, sauvegarder avant depuis l'app (Réglages, « Sauvegarder toutes les données ») et restaurer
  après.
- `kotlinx-coroutines-test` doit rester à la même version que `kotlinx-coroutines-core` utilisé par l'app
  (1.10.2, imposé par AndroidX), sinon les tests instrumentés échouent avec un `NoSuchMethodError`.
- Avertissement lint `OldTargetApi` (targetSdk 36 alors que l'API 37 existe) : volontaire. Sans publication, rien
  n'oblige à monter ; à faire quand une nouvelle version d'Android le justifie, en retestant l'app.
- Tests instrumentés : Gradle peut afficher `BUILD SUCCESSFUL` alors que l'APK de test ne s'est pas installé
  (émulateur pas encore prêt). Toujours vérifier le nombre de tests passés dans
  `app/build/outputs/androidTest-results/connected/debug/*.xml` (31 au 21 octobre 2026).
- Le filtre `-Pandroid.testInstrumentationRunnerArguments.class=…` n'a lancé aucun test : lancer toute la suite.
- Émulateur `Medium_Phone_API_37.0` sans accès root ni `sqlite3` : pour préparer des données, récupérer la base avec
  `adb exec-out run-as com.paulfvt.liftapp cat databases/lift.db`, la modifier sur le PC, puis la remettre avec
  `run-as … cp` depuis `/data/local/tmp` (app arrêtée, fichiers `-wal` et `-shm` supprimés).
