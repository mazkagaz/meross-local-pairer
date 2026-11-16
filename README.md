# Meross Local Pairer — appairage Meross vers un broker local (non officiel)

Application Android qui configure un appareil Meross pour qu'il se connecte à
un broker MQTT local au lieu du cloud Meross. Elle est utilisée une seule fois,
lors de l'appairage ; ensuite Home Assistant pilote l'appareil localement.

**Version stable 1.1.0.** Elle gère l'appairage Wi-Fi des appareils au
firmware 7 et l'appairage **Bluetooth LE** des appareils au firmware 8 (MRS100
8.x), dont l'AP Wi-Fi n'est pas ouvert sans confirmation BLE. Une recette
F-Droid est préparée (`fdroid/org.localpairer.yml`) ; l'application
n'apparaîtra dans F-Droid qu'après acceptation de la merge request dans
`fdroiddata`.

## Les trois briques du montage

| Brique | Rôle | Dépôt |
|---|---|---|
| **Meross Local Pairer** | cette application Android | [mazkagaz/meross-local-pairer](https://github.com/mazkagaz/meross-local-pairer) |
| **Meross Local Broker** | add-on Home Assistant (MQTT + API locale) | [mazkagaz/meross-local-broker](https://github.com/mazkagaz/meross-local-broker) |
| **Meross Local Cloud** | intégration Home Assistant (HACS) | [mazkagaz/meross-local-cloud](https://github.com/mazkagaz/meross-local-cloud) |

```
MRS100 ──MQTT/TLS──> add-on Meross Local Broker ──API/MQTT──> intégration HA
                            ^
                            │ appairage initial
                 Meross Local Pairer (Android)
```

## Télécharger l'application

1. Ouvrir la page
   [Releases](https://github.com/mazkagaz/meross-local-pairer/releases/latest).
2. Télécharger le fichier `.apk` de la dernière release (APK signé).
3. Sur Android, autoriser l'installation depuis cette source si demandé.
4. Installer l'APK.

L'application n'est pas publiée sur le Play Store. La CI est configurée pour
signer les APK de release avec une clé stable du projet : les mises à jour
s'installeront ensuite par-dessus la version précédente. Une désinstallation
unique est nécessaire au passage d'une ancienne build signée avec une autre
clé (et plus tard au passage à F-Droid, qui utilise sa propre clé). Voir
`fdroid/README.md` pour la soumission F-Droid.

## Avant l'appairage

- L'add-on **Meross Local Broker** est installé et démarré.
- Un **compte local** existe dans l'add-on (email + mot de passe, Meross Link
  désactivé).
- Le téléphone et le MRS100 sont à portée Wi-Fi (et Bluetooth pour le
  firmware 8).
- Le Wi-Fi domestique est en **2,4 GHz WPA2** (le MRS100 ne gère pas le 5 GHz
  ni WPA3).
- L'appareil est en mode appairage : maintenir le bouton jusqu'au clignotement,
  selon la notice du modèle. Un MRS100 en firmware 8 se met alors en
  **publicité Bluetooth** (`MRBL_…`), pas en AP Wi-Fi.

## Appairage pas à pas

1. Ouvrir Meross Local Pairer et accorder les permissions Wi-Fi et localisation.
2. Se connecter au broker local :
   - **recommandé** : écran de connexion HTTP, saisir
     `http://homeassistant.local:2003`, l'email local et le mot de passe local ;
   - **ou** : menu **Manual setup**, saisir `UserId` (souvent `1`) et la clé
     `mqttKey` du compte local.

   Avec la connexion HTTP, l'application récupère automatiquement les
   identifiants et peut vérifier l'appairage. Si `homeassistant.local` ne
   répond pas, utiliser l'adresse IP de Home Assistant ; si l'adresse IP est
   refusée par Android, utiliser **Manual setup**.
3. Revenir à l'écran d'appairage, puis choisir la méthode selon le firmware :

   **Firmware 7 — AP Wi-Fi**
   1. Scanner et sélectionner l'AP `Meross_RS_XXXX` (ou
      `Meross_RS_chWF_*`).
   2. Sélectionner le Wi-Fi domestique et saisir son mot de passe.
   3. Saisir le broker : hôte = IP de Home Assistant (ou
      `homeassistant.local`), port = **2001**.
   4. Lancer l'appairage. L'appareil redémarre, rejoint le Wi-Fi et se connecte
      au broker.

   **Firmware 8 — Bluetooth LE**
   1. Toucher **Firmware 8: pair via Bluetooth**.
   2. Sélectionner l'appareil `MRBL_…`, puis **Connect**.
   3. L'application lit l'identité (UUID/MAC/type) et la liste Wi-Fi de
      l'appareil.
   4. Sélectionner le Wi-Fi domestique, saisir le mot de passe, puis
      **Configure device**. Le broker local déjà configuré dans l'application
      est utilisé (port 2001).
   5. L'application vérifie que l'appareil devient **ONLINE** ; elle n'affiche
      pas de succès si ce n'est pas le cas.

4. Vérifier dans l'add-on que l'appareil est **ONLINE**, puis ajouter
   l'[intégration Home Assistant](https://github.com/mazkagaz/meross-local-cloud#installation-avec-hacs).

Après l'appairage, les entités `cover`, `number`, boutons de calibration et
capteur de statut sont créés dans Home Assistant.

## Dépannage

| Problème | Vérifier |
|---|---|
| L'AP `Meross_*` n'apparaît pas | mode appairage activé ; en firmware 8, c'est normal : utiliser l'appairage Bluetooth |
| Aucun appareil `MRBL_…` en Bluetooth | Bluetooth activé, permissions Bluetooth/localisation, appareil en mode appairage, à portée |
| Connexion HTTP impossible | utiliser `homeassistant.local` ou **Manual setup** |
| Échec des identifiants | compte local de l'add-on, `UserId`/`mqttKey` corrects |
| L'appareil ne rejoint pas le Wi-Fi | WPA2 2,4 GHz, mot de passe, portée |
| L'appareil n'est pas ONLINE | port 2001 joignable, broker démarré, logs de l'add-on |
| Appareil déjà lié au cloud Meross | réinitialiser l'appareil avant l'appairage |
| WPA3 uniquement | utiliser un SSID WPA2 ou un réseau 2,4 GHz dédié |

## Pour les développeurs

```bash
./gradlew test
./gradlew assembleDebug
./gradlew assembleReleaseUnsigned
```

Le build nécessite JDK 17 et Android SDK 35. Les tests couvrent le format
`Appliance.Config.Key` et la validation des réponses AP (SETACK/ERROR) ;
l'appairage BLE se teste sur un appareil réel.

La CI signe les APK avec une clé stable fournie par les secrets GitHub
(`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`). Ne jamais committer le keystore
ni ses mots de passe.

## Origine et licence

Fork non officiel de
[Custom Pairer for Meross Devices](https://github.com/albertogeniola/Custom-Meross-Pairer)
par Alberto Geniola, sous **GPL-2.0**. Le README upstream est conservé dans
`UPSTREAM_README.md` ; voir aussi `NOTICE` et `MODIFICATIONS.md`.

Ce fork n'est ni affilié, ni approuvé, ni sponsorisé par Meross ou l'auteur
upstream. Meross est une marque de son propriétaire respectif.
