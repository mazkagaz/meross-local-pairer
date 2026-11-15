# Changelog

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
