# Meross Local Pairer 1.0.6

Correctif d'identité et de nettoyage de l'appairage.

- La connexion Wi-Fi est vérifiée contre le SSID et le BSSID choisis : l'app ne
  peut plus confondre l'AP d'un appareil précédemment laissé en mode appairage
  avec celui de l'appareil visé.
- L'écran de confirmation affiche l'AP utilisé, et l'écran final affiche l'UUID
  réellement appairé.
- Un avertissement apparaît lorsque plusieurs appareils Meross sont visibles.
- L'état de l'appairage est réinitialisé entre deux tentatives, et la boucle de
  vérification est annulée à la fermeture de l'écran.
