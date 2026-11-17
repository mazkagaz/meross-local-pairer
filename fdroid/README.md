# Publication F-Droid

L'application est éligible à F-Droid : licence GPL-2.0, aucun service
propriétaire, aucun traqueur, dépendances libres. JitPack est accepté par
F-Droid pour les bibliothèques libres ; la dépendance `checkview` (sans licence
explicite) a été retirée pour éviter un rejet.

## Soumission

1. Forker `https://gitlab.com/fdroid/fdroiddata`.
2. Copier `fdroid/org.localpairer.yml` vers `metadata/org.localpairer.yml`.
3. Vérifier localement (facultatif) :
   `fdroid lint org.localpairer && fdroid build org.localpairer`.
4. Ouvrir une merge request « New app: Meross Local Pairer ».
5. Répondre aux éventuelles demandes des relecteurs.

Le fichier pointe sur le tag `v2.0.4` et le `versionCode` 67, avec l'historique
des versions précédentes. Les prochaines versions seront détectées par
`AutoUpdateMode: Version` / `UpdateCheckMode: Tags`.

## Signature

F-Droid signe les APK avec sa propre clé. Un APK installé depuis GitHub (clé
stable du projet) doit être désinstallé avant d'installer la version F-Droid,
sinon Android refuse la mise à jour pour cause de signature différente.

## Vérifications

- `minSdkVersion` 23, `targetSdkVersion` 36, Gradle 8.14.7, JDK 17.
- Réseau : HTTP en clair limité à l'AP d'appairage `10.10.10.1` et aux hôtes
  locaux `.local`/`.lan` ; MQTT en TLS.
- Aucun identifiant du cloud Meross, aucune télémétrie.
- Fastlane : descriptions et changelog dans `fastlane/metadata/android/`.
