# Changelog

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
