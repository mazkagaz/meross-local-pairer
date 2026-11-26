## 2.1.1

- Écran d'information d'appairage : suppression du texte « volet », remplacé
  par une formulation générique « expected device ».
- Les libellés d'appairage sont désormais des ressources traduisibles.
- L'icône par défaut n'est plus un volet mais l'icône générique du pairer.
- Aucun changement du protocole d'appairage ; support MSL610 non inclus.

## 2.1.0

- Page **About** : lien **Check for updates** vers la dernière release GitHub.
- Vérification silencieuse des mises à jour à l’ouverture (une fois par
  version, échec réseau ignoré).
- Ces deux fonctions sont désactivées pour les builds F-Droid
  (`-PfdroidBuild=true`) et les installations faites par F-Droid.
- Logo et bannière unifiés avec le broker et l’intégration.

## 2.0.9

- Après un appairage réussi, le bouton de confirmation et le retour depuis
  l’appairage Bluetooth ferment l’écran d’appairage et reviennent à la liste
  des appareils.
- Découverte Wi-Fi par rafale de scans à 0/5/15/30 s : un premier scan vide
  n’arrête plus la recherche, le statut reste visible et la rafale s’annule dès
  qu’un AP Meross est trouvé.
- Le dernier réseau Wi-Fi utilisé pour un appareil est sélectionné par défaut
  lors de l’appairage suivant, avec son mot de passe s’il est enregistré.
- Corrections de textes visibles et de la documentation.

## 2.0.8

- Découverte mDNS en **HTTPS uniquement** (`https_port`, défaut 2004) ;
  suppression du repli HTTP.
- Le port **2003** est refusé dans les champs de ports (connexion et compte)
  avec le message « Port 2003 is reserved. ».

## 2.0.7

- Lorsqu'un certificat broker différent est détecté, une boîte de dialogue
  affiche le sujet, l'émetteur, la date de validité et l'empreinte SHA-256 du
  nouveau certificat, avec les boutons **Accept** et **Deny**. Accepter
  remplace le pin et relance l'opération ; refuser conserve l'ancien
  certificat.

# Changelog

## 2.0.6

- Les espaces en début/fin de mot de passe sont conservés lors de la connexion.
- La vérification BLE est annulable et ne met plus à jour l'interface après la
  fermeture de l'écran.
- Modernisation : détection Wi-Fi via `NetworkCapabilities`, gestion du retour
  par `OnBackPressedDispatcher`, barres système via `WindowCompat`.
- Nettoyage : `PairActivity` n'est plus exportée, suppression du trust manager
  et du client HTTP inutilisés.

## 2.0.5

- Cible Android 17 (API 37) : AGP 9.5.1, Gradle 9.9.1, compileSdk/targetSdk 37.
- Prise en charge de la permission Android 17 **Accès au réseau local**
  (`ACCESS_LOCAL_NETWORK`) pour la découverte mDNS, le broker et l'appairage.
- L'APK publié s'appelle désormais `Meross-Local-Pairer-<version>.apk` au lieu
  du nom générique `app-release.apk`.

## 2.0.4

- Correction de l'affichage des réseaux de confiance : après avoir accepté le
  réseau courant, la liste se met immédiatement à jour au lieu d'afficher
  encore « aucun réseau validé ».
- Mise à jour de la toolchain Android : AGP 8.13.2, Gradle 8.14.7,
  compileSdk/targetSdk 36.

## 2.0.3

- Remplacement de la bibliothèque de stockage sécurisé abandonnée par
  **AndroidX EncryptedSharedPreferences** (Keystore AES). Les anciens
  identifiants et mots de passe Wi-Fi sont migrés automatiquement ; en cas
  d'échec, l'application échoue en mode fermé, sans fallback en clair.
- Suppression des dépendances inutilisées (Paho, WorkManager, Commons IO,
  legacy support, cardview) et des permissions associées. L'APK release
  descend à environ 3,6 Mo.
- Mise à jour AndroidX, Gson, Lombok et du client OkHttp ; Navigation reste en
  2.9.8, dernière version compatible avec Android 6.
- HTTP en clair refusé pour une adresse IP : HTTPS est requis pour les IP, le
  HTTP restant disponible pour les noms `.local`/`.lan`.
- Sauvegardes et transferts de données Android désactivés explicitement.

## 2.0.2

- Sécurité de distribution : les APK GitHub sont désormais construits en
  **release** (non débuggables) et signés avec la clé stable du projet, au lieu
  d'un APK debug signé.
- Les journaux HTTP ne contiennent plus le token, les en-têtes ni les corps de
  requête/réponse. Les diagnostics d'appairage ne sont écrits dans logcat que
  lorsque l'utilisateur active explicitement l'option.
- `minSdk` passe à **23 (Android 6.0)** : suppression du multi-dex legacy et des
  API incompatibles avec les anciennes versions, préparation du stockage
  chiffré moderne.
- La CI exécute les tests unitaires et construit l'APK release.

## 2.0.1

- Textes et documentation alignés sur la prise en charge réelle : appairage
  par AP Wi-Fi et Bluetooth LE, sans références génériques aux firmwares 7/8.
- L'écran d'appairage Bluetooth et la page About ne mentionnent plus un numéro
  de firmware. Le MRS100 reste la cible principalement testée ; les tests des
  autres modèles Meross sont bienvenus.

## 2.0.0

Version majeure, local-only et orientée grand public.

- Suppression du champ **Multifactor Authentication Code** : le broker local ne
  gère pas le MFA, le champ n'avait aucun effet. La connexion se limite à
  l'adresse du broker, au compte local et au mot de passe.
- Appairage par AP Wi-Fi et Bluetooth LE, avec détection automatique des
  appareils `MRBL_…`, nom d'appareil obligatoire et unique, et réseau Wi-Fi IoT
  ajouté automatiquement aux réseaux de confiance. Le MRS100 est pris en
  charge ; les autres modèles Meross restent à tester et à signaler.
- API locale en HTTPS (port 2004), MQTT/TLS (port 2001), certificat épinglé
  dans l'application ; découverte mDNS automatique avec adresses et ports
  publiés par le broker.
- Configuration simplifiée : adresse de Home Assistant + interrupteur HTTP/HTTPS,
  ports dérivés, mode **Advanced settings** pour les overrides et secrets.
- Gestion des appareils : oubli avec révocation, claim au réappairage (même
  UUID), renommage par appui sur la ligne, icônes adaptées au type.
- Interface : page About réécrite avec crédits, principes, confidentialité et
  bannière café ; flèches retour standard ; icônes d'appairage cohérentes.
- Signature stable des APK et notes de release générées depuis ce changelog.

## 1.4.11

- La réutilisation du Wi-Fi en Bluetooth ne dépend plus du BSSID exact : le
  réseau est retrouvé par SSID parmi les réseaux de confiance, et le mot de
  passe enregistré pour un autre point d'accès du même SSID est réutilisé.
- Correction de l'appairage AP : le mode **Skip** enregistrait le texte du
  champ au lieu du mot de passe. Les mots de passe sont désormais stockés par
  SSID et par BSSID. La case d'enregistrement est cochée par défaut.

## 1.4.10

- L'appairage Bluetooth réutilise les identifiants Wi-Fi comme l'appairage AP :
  le réseau courant (ou un réseau mémorisé) est présélectionné et le mot de
  passe est rempli automatiquement s'il a déjà été enregistré.
- Ajout des cases **Show password** et **Remember this Wi-Fi password** sur
  l'écran Bluetooth ; le mot de passe est mémorisé après configuration.

## 1.4.9

- Suppression de l'avertissement technique « Meross plugs are only capable… »
  affiché à tous les utilisateurs. Un broker personnalisé est désormais
  vérifié en TLS avant l'appairage : si le port n'accepte pas MQTT over TLS,
  un message clair invite à utiliser le port TLS (2001 pour l'add-on) et
  l'appairage ne démarre pas.
- L'écran d'informations de l'appareil pendant l'appairage utilise une icône
  cohérente et adaptée au type (volet ou prise), sur un fond circulaire discret.

## 1.4.8

- La découverte mDNS est **automatique** à l'ouverture de l'écran de connexion ;
  le bouton **Discovery** est supprimé. Le champ n'est prérempli que s'il est
  vide, et la saisie manuelle reste utilisable pendant la recherche. Un texte
  d'état indique la recherche, le résultat ou l'absence de broker.
- Nouvelle iconographie sur l'écran d'appairage : icônes Material cohérentes
  teintées (Wi-Fi, appairage, appui) et bouton de rescan blanc, à la place des
  anciennes illustrations.

## 1.4.7

- La découverte mDNS utilise désormais les enregistrements TXT publiés par le
  broker `alpha75` : adresses LAN de Home Assistant, port HTTP et port HTTPS.
  L'application teste la joignabilité de chaque adresse, préfère HTTPS quand il
  est annoncé, puis préremplit l'hôte et les ports. Aucune adresse n'est
  préremplie si aucune ne répond.
- Repli sur l'adresse IP résolue (et non plus le nom SRV) pour les brokers
  antérieurs ; le type de service mDNS n'a plus de point final.

## 1.4.6

- Corrige la flèche retour de la barre d'outils : elle est maintenant affichée
  et fonctionnelle sur About et Settings, comme sur les écrans d'appairage
  (`onSupportNavigateUp` manquant dans `MainActivity`).

## 1.4.5

- Page **About** réécrite pour téléphone : sections courtes, crédits du fork,
  principes, vie privée et mentions légales, puis support et code source.
- La page **Supporting this app** est supprimée ; son contenu est intégré à la
  page About.
- Nouvelle bannière café discrète en bas d'About (« Offer me a coffee »), avec
  icône locale neutre et lien PayPal. Le dépôt upstream n'est plus lié : seul
  le nom de l'auteur et du projet d'origine est cité.
- Tous les boutons **Back to devices** sont supprimés (About, Settings, écran
  d'appairage) au profit de la flèche retour standard de la barre d'outils.

## 1.4.4

- Le **Device name** est obligatoire pendant l'appairage (FW7 et FW8) ; il est
  masqué si des identifiants manuels sans API sont utilisés.
- Les noms d'appareils sont uniques (comparaison insensible à la casse) côté
  broker `alpha74`. Un doublon est refusé avec un message explicite.
- Nouveau renommage depuis la liste des appareils : toucher la ligne ouvre un
  dialogue de renommage, sans passer par le broker.
- Si l'enregistrement du nom échoue après l'appairage (doublon, broker plus
  ancien), un message l'indique et le renommage reste possible depuis la liste.

## 1.4.3

- Champ **Device name (optional)** pendant l'appairage (écran du broker en
  firmware 7, écran Bluetooth en firmware 8). Le nom est enregistré dans le
  broker via `POST /v1/Device/name` (broker `alpha73` ou plus récent) et
  apparaît dans la liste des appareils et dans Home Assistant.
- Si le broker est plus ancien, le nom est ignoré sans faire échouer
  l'appairage.

## 1.4.2

- Le réseau Wi-Fi choisi pour les appareils IoT est ajouté automatiquement aux
  réseaux de confiance, même s'il est différent de celui du téléphone. Les
  parcours firmware 7 (AP) et firmware 8 (Bluetooth) ne demandent donc plus
  d'autorisation séparée pour ce réseau pendant l'appairage.
- Un refus antérieur pour ce même SSID est annulé lors de l'appairage.

## 1.4.1

- Icône adaptée au type d'appareil dans la liste : les volets roulants
  (`mrs100`, `mss560`, `mss570`, `mrs200`) utilisent une icône de volet ; les
  autres appareils gardent l'icône de prise.

## 1.4.0

- Configuration simplifiée : saisir uniquement l'adresse de Home Assistant
  (nom `homeassistant.local` ou IP) et choisir HTTP ou HTTPS. Les ports par
  défaut (2003 en HTTP, 2004 en HTTPS) sont construits automatiquement.
- Nouveau mode **Advanced settings** (mémorisé) : ports API clair/chiffré
  modifiables, `userId`, token, `mqttKey`, Manual setup, gestion des réseaux
  Wi-Fi de confiance, diagnostics d'appairage et réinitialisation du
  certificat épinglé.
- Par défaut, le token, la clé MQTT et les diagnostics ne sont plus affichés.
- Au démarrage, l'application propose de faire confiance au Wi-Fi courant ;
  un refus est mémorisé et la gestion complète reste dans Advanced settings.
- La découverte mDNS ne remplit plus que l'adresse ; le choix HTTP/HTTPS est
  conservé.
- HTTPS est proposé par défaut pour une nouvelle configuration.

## 1.3.0

- Prise en charge du broker HTTPS (port **2004** du broker `alpha71`). Une URL
  `https://…` est acceptée à la connexion HTTP, y compris par adresse IP.
- Épinglage du certificat du broker (TOFU) : l'empreinte SHA-256 de la feuille
  est mémorisée au premier contact, puis vérifiée à chaque connexion. Si le
  certificat change, l'application affiche une erreur explicite.
- Settings → diagnostics affiche l'empreinte épinglée et permet de la
  réinitialiser après avoir régénéré les certificats de l'add-on.
- Le port 2003 reste utilisable en HTTP pour les installations existantes.

## 1.2.2

- Après un nouvel appairage (Wi-Fi firmware 7 ou Bluetooth firmware 8),
  l'application appelle `POST /v1/Device/claim` pour lever la révocation
  laissée par un oubli précédent. Le broker `alpha70` refuse désormais la
  connexion d'un appareil oublié, même s'il a conservé ses identifiants MQTT.
- L'UUID de l'appareil est conservé : les entités Home Assistant et les
  automatisations existantes restent valides après oubli puis réappairage.
- Si le broker est antérieur à `alpha70`, l'appel de claim est ignoré sans
  faire échouer l'appairage.

## 1.2.1

- La section **Allowed Wi-Fi networks** est affichée en premier dans Settings,
  avant le compte, et reste ouverte à la première configuration.
- La connexion au broker vérifie déjà le réseau autorisé avant l'appel API ;
  ce contrôle intervient donc avant le test des identifiants du compte.


## 1.2.0

- **Settings** remplace la page « Account & Login », avec des sections
  repliables : compte, Wi-Fi autorisé, diagnostics. La première configuration
  ouvre le compte et le Wi-Fi ; le reste est replié.
- « Manage allowed Wi-Fi networks » liste les réseaux visibles et permet de
  les autoriser sans y être connecté.
- Bouton **Back to devices** général, hors de la section compte.
- Suppression de l'ancien logo Home Assistant de la page de configuration.
- Suppression d'un appareil depuis la liste, via l'API authentifiée du broker
  (`POST /v1/Device/delete`, broker `alpha69`).


## 1.1.2

- « Allow current Wi-Fi » demande désormais les permissions Wi-Fi/Localisation
  et vérifie que les services de localisation sont actifs avant de lire le
  SSID. Message d'erreur explicite si le réseau ne peut pas être lu.


## 1.1.1

- Détection **Bluetooth automatique** sur l'écran de scan : les appareils
  `MRBL_…` apparaissent dans la liste, sans bouton de mode manuel. Un appui
  lance directement la connexion et le parcours Wi-Fi.
- Ajout d'une liste de **réseaux Wi-Fi autorisés** : le broker n'est contacté
  que sur un réseau validé, avec une demande d'autorisation au premier usage.
  Les AP d'appairage `Meross_*` restent autorisés pendant l'appairage.


## 1.1.0

- Ajout de l'appairage **Bluetooth LE** pour les appareils au firmware 8
  (MRS100 FW 8.x), dont l'AP Wi-Fi n'est pas disponible sans confirmation BLE.
- Le bouton « Firmware 8: pair via Bluetooth » scanne le service Meross A00A,
  lit l'identité de l'appareil, récupère sa liste Wi-Fi, envoie les
  identifiants du broker local puis les paramètres Wi-Fi, et vérifie que
  l'appareil devient ONLINE dans le broker.


## 1.0.11

- L'écran de scan affiche un statut persistant : heure du dernier scan, nombre
  d'appareils Meross trouvés et recherche automatique toutes les 30 s. Le
  spinner peut s'arrêter après quelques secondes, mais la recherche continue.


## 1.0.10

- Les scans automatiques ne consomment plus le quota Android : ils sont
  silencieux et espacés de 30 s, et seul le bouton manuel affiche les messages
  de limite ou de scan en cours.


## 1.0.9

- Le journal de diagnostic enregistre désormais **tous** les réseaux Wi-Fi
  vus, avant le filtre Meross, pour vérifier si un AP au nom inattendu est
  présent.
- Scans automatiques espacés de 30 s tant qu'aucun appareil n'est trouvé, au
  lieu de consommer le quota Android dès les premières secondes.
- Le message transitoire « Failed to locate MQTT endpoint via mDNS » n'est plus
  affiché : l'échec mDNS est journalisé et le broker par défaut est utilisé.


## 1.0.8

- Ajout d'un mode diagnostic dans les paramètres : journal d'appairage
  activable, copiable dans le presse-papiers et effaçable.
- Une seule session d'appairage peut agir à la fois ; une tentative laissée
  en arrière-plan est invalidée dès qu'une nouvelle commence.
- Le bouton de rescan explique désormais ce qu'il fait et pourquoi il peut
  être temporisé ; les scans automatiques ne consomment plus le quota quand
  un appareil est déjà visible.


## 1.0.7

- Ignore les valeurs `unknown` renvoyées par certains Android pour le SSID ou
  le BSSID, afin que la vérification d'identité ne rejette pas une connexion
  pourtant correcte.


## 1.0.6

- Vérifie que le réseau Wi-Fi effectivement connecté correspond bien au SSID et
  au BSSID demandés ; une connexion résiduelle à l'AP d'un appareil précédent
  ne peut plus être prise pour l'appareil en cours d'appairage.
- Annule la requête réseau précédente avant chaque nouvelle connexion et
  arrête la boucle de vérification à la destruction de l'écran.
- Réinitialise l'AP, l'UUID et la configuration broker entre deux tentatives.
- Affiche l'identité réellement appairée (SSID/BSSID puis UUID) et avertit
  lorsque plusieurs appareils Meross sont en mode appairage.


## 1.0.5

- Les scans Wi-Fi différés (5 s/12 s) et les relances automatiques sont
  annulés dès que l'écran de scan n'est plus visible. Ils ne peuvent plus se
  déclencher pendant la connexion à l'AP de l'appareil, ce qui pouvait casser
  l'appairage.


## 1.0.4

- L'écran de configuration du broker est désormais défilable : le port, les
  cases et les champs avancés ne sont plus masqués par le bouton d'appairage
  sur les petits écrans ou quand le clavier est ouvert.


## 1.0.3

- Relance jusqu'à trois fois la demande de scan Wi-Fi adressée à l'appareil
  quand le firmware la refuse (message « scan failed wifi network »), au lieu
  d'abandonner immédiatement. Message d'erreur final plus explicite.


## 1.0.2

- A refused/throttled Wi-Fi scan no longer reports "scan failed": the previous
  results are kept and the scan is retried automatically after 3 seconds.


## 1.0.1

- Relance automatiquement le scan Wi-Fi 5 s et 12 s après le premier scan,
  pour les appareils qui démarrent leur AP avec un peu de retard.


## 1.0.0 — première version stable

- Appairage des appareils Meross vers un broker MQTT local, sans cloud.
- Broker « default » prérempli depuis l'hôte de l'API (port 2001), en lecture
  seule sauf case **Modify**.
- Option **Never ask again** sur le disclaimer du splash.
- Icône launcher, variantes sombres/2x et bannière générées depuis les visuels
  transparents.
- Suppression de la dépendance `checkview` (licence absente) pour préparer
  l'inclusion F-Droid.
- Recette F-Droid et métadonnées fastlane préparées.

## 0.1.9

- « Never ask again » et broker par défaut.

## 0.1.8

- Icônes launcher transparentes.

## 0.1.7

- Bannière sur tous les écrans, logo launcher, page About corrigée.
