# Meross Local Pairer 1.0.2

Correctif du scan d'appairage.

- Un scan Wi-Fi refusé ou limité par Android n'affiche plus « Scan failed wifi
  networks » : les résultats précédents sont conservés et un nouveau scan est
  lancé automatiquement après 3 secondes.
- Cette relance complète les scans automatiques à 5 s et 12 s déjà ajoutés en
  1.0.1, utile pour les appareils dont l'AP démarre en retard.
