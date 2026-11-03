# Politique de confidentialité de LiftApp

Dernière mise à jour : 3 novembre 2026

LiftApp est une application Android gratuite de suivi de musculation, développée par
[paulfvt](https://github.com/paulfvt) et distribuée sur [GitHub](https://github.com/paulfvt/liftapp/releases).
Elle fonctionne entièrement sur ton téléphone, sans compte et sans serveur.

## Données que l'application enregistre

L'application enregistre uniquement ce que tu y saisis :

- tes templates de séance, tes séances et leurs séries (exercices, charges, répétitions, durées, distances) ;
- tes notes sur les exercices et les exercices que tu crées ;
- ton profil, entièrement facultatif : prénom, taille, date de naissance, sexe ;
- tes mesures : poids, tours (bras, poitrine, taille, hanches, cuisse, mollet), taux de masse grasse ;
- tes photos de progression, prises dans l'application ou importées depuis ta galerie ;
- tes réglages (thème, unités, repos par défaut, son et vibration du minuteur, mode salle, masquage du poids et
  des photos, verrouillage des photos).

Ces données sont stockées dans l'espace privé de l'application sur ton téléphone. Les autres applications n'y ont
pas accès, et tes photos de progression n'apparaissent pas dans la galerie.

## Aucune collecte, aucun partage

- L'application ne demande pas la permission d'accéder à internet : elle ne peut rien envoyer.
- Aucune donnée n'est collectée par le développeur ni transmise à un tiers.
- Pas de compte, pas de publicité, pas d'outil de mesure d'audience ni de suivi.
- Le récap mensuel peut être partagé en image, seulement si tu appuies sur « Partager » : c'est toi qui choisis
  l'application qui la reçoit, dans le menu de partage d'Android. Rien d'autre n'est envoyé.
- Si tu poses le widget sur ton écran d'accueil, il y affiche ta semaine, ta prochaine séance, ton dernier record et
  ta séance en cours : toute personne qui voit ton écran d'accueil peut les lire. Ces informations restent sur le
  téléphone.

## Photos et verrouillage

- Une photo importée depuis la galerie est copiée dans l'application : seule l'image que tu choisis est lue, grâce
  au sélecteur de photos d'Android. L'original reste dans ta galerie.
- Tu peux verrouiller tes photos avec un code propre à l'application. Ce code n'est jamais enregistré tel quel :
  l'application n'en garde qu'une empreinte chiffrée, qui ne permet pas de le retrouver.
- Si tu déverrouilles avec ton visage, ton empreinte ou le verrouillage de ton téléphone, c'est Android qui fait la
  vérification : l'application reçoit seulement la réponse « c'est bien toi », jamais tes données biométriques.

## Export et sauvegarde

Depuis les réglages, tu peux exporter ton historique et tes mesures en fichiers CSV (lisibles dans Excel), et
sauvegarder toutes tes données dans un fichier ZIP pour les restaurer plus tard, par exemple sur un nouveau téléphone.

- C'est toi qui choisis où chaque fichier est enregistré, avec le sélecteur de fichiers d'Android (mémoire du
  téléphone, carte SD, service de stockage en ligne installé sur le téléphone). L'application n'a besoin d'aucune
  autorisation pour cela et n'envoie rien elle-même.
- Pour une restauration, l'application lit seulement le fichier que tu choisis. Elle vérifie qu'il s'agit bien d'une
  sauvegarde LiftApp, puis remplace toutes les données actuelles, après ta confirmation.
- **Le fichier de sauvegarde n'est pas chiffré.** Si tu y inclus tes photos (option cochée par défaut, que tu peux
  décocher), toute personne qui ouvre le fichier peut les voir, même si elles sont verrouillées dans l'application.
  Garde-le en lieu sûr. S'il est enregistré dans un service en ligne, ce service le conserve selon ses propres règles.

## Sauvegarde Android

Si tu as activé la sauvegarde de ton téléphone (paramètres Android, compte Google), Android peut copier les données
de l'application dans ton espace de sauvegarde Google, comme pour tes autres applications. Cette sauvegarde est
gérée par Android et Google, selon les règles de ton compte ; le développeur n'y a pas accès. Elle permet de retrouver
tes séances et tes mesures sur un nouveau téléphone.

**Tes photos de progression sont exclues de cette sauvegarde en ligne.** Elles suivent seulement lors d'un transfert
direct d'un téléphone à un autre (câble ou connexion sans fil entre les deux appareils).

## Permissions utilisées

- **Notifications** : afficher la séance en cours et le compte à rebours du repos, même écran verrouillé.
- **Alarmes exactes** : déclencher l'alerte de fin de repos à l'heure, même quand l'application est en arrière-plan.
- **Vibreur** : faire vibrer le téléphone à la fin du repos, si tu l'as activé.
- **Caméra** : prendre tes photos de progression dans l'application. Elle est demandée à ta première photo, jamais
  avant, et n'est utilisée que sur l'écran de prise de photo.
- **Biométrie** : déverrouiller tes photos avec ton visage ou ton empreinte, si tu choisis cette option.

Tu peux retirer ces autorisations à tout moment dans les paramètres Android ; l'application continue de fonctionner,
avec un minuteur moins précis écran verrouillé et sans prise de photo (l'import depuis la galerie reste possible).

## Supprimer tes données

Désinstaller l'application supprime toutes ses données du téléphone, photos comprises. Tu peux aussi les effacer
depuis les paramètres Android (Applications, LiftApp, Stockage, Effacer les données), ou supprimer une mesure ou une
photo depuis l'application. La sauvegarde Android éventuelle se gère depuis ton compte Google. Les fichiers
d'export et de sauvegarde que tu as enregistrés ne sont pas supprimés avec l'application : à toi de les effacer.

## Enfants

L'application ne s'adresse pas spécifiquement aux enfants et ne collecte aucune donnée, y compris les leurs.

## Modifications

Si l'application évolue, cette page sera mise à jour avant la sortie de la nouvelle version, avec une nouvelle date
en haut de page.

## Contact

Pour toute question, ouvre une [issue sur GitHub](https://github.com/paulfvt/liftapp/issues).
