# Meross Local Pairer 1.1.0

Appairage Bluetooth des appareils au firmware 8.

- Nouveau bouton **Firmware 8: pair via Bluetooth** sur l'écran de scan.
- Scan BLE du service Meross A00A, lecture de l'identité (UUID/MAC/type) et
  de la liste Wi-Fi de l'appareil.
- Envoi des identifiants du broker local et du Wi-Fi sélectionné via le
  protocole BLE Meross, puis vérification que l'appareil devient ONLINE.
- Les appareils firmware 7 continuent d'utiliser l'appairage Wi-Fi classique.
