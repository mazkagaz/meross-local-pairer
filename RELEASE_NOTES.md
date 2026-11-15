# Meross Local Pairer 1.0.3

Robustesse de l'appairage des appareils au firmware récent.

- Quand l'appareil refuse la demande de scan Wi-Fi (« scan failed wifi
  network »), l'app le relance jusqu'à trois fois, à deux secondes
  d'intervalle, au lieu d'abandonner au premier refus.
- Après trois refus, le message d'erreur indique de redémarrer l'appareil et
  de réessayer.
- Correction d'une fuite du thread de travail à la fermeture de l'écran
  d'appairage.
