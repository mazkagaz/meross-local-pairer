# Meross Local Pairer 1.0.0 — première version stable

Meross Local Pairer configure un appareil Meross pour qu'il rejoigne votre
broker MQTT local au lieu du cloud Meross. C'est le compagnon d'appairage de
l'add-on **Meross Local Broker** pour Home Assistant.

Cette version 1.0.0 marque la première release stable : appairage, calibration
et pilotage local sont validés sur un MRS100 réel.

## Points forts

- **100 % local** : aucun compte ni identifiant du cloud Meross.
- Connexion au broker local par login HTTP, ou configuration manuelle
  `UserId`/`key`.
- Broker **default** prérempli depuis l'adresse de l'API (port MQTT 2001),
  modifiable via la case **Modify**.
- Splash avec option **Never ask again**.
- Icônes et bannière aux couleurs du projet, avec transparence réelle.
- Aucun traqueur, aucune publicité, aucune analyse d'usage.

## Téléchargement

L'APK debug signé est joint à cette release. Une recette F-Droid est préparée
dans `fdroid/` ; la publication sur F-Droid dépend de l'acceptation de la merge
request dans `fdroiddata`.

## Notes

- Nécessite un broker local (add-on Meross Local Broker ou broker MQTT
  compatible) ; l'application ne pilote pas les appareils elle-même.
- Projet non officiel, sans affiliation avec Meross.
- Licence GPL-2.0.
