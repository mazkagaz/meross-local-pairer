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
5. Utiliser le template `Default.md` de fdroiddata (abstract, captures,
   checklists) et répondre aux demandes des relecteurs.

La première MR (`!58475`) a été fermée en novembre 2026 : template non suivi
et dépôt maintenu depuis moins de trois mois. Ne pas la rouvrir ; soumettre
une nouvelle MR après la période de trois mois, avec la recette réduite à la
dernière version.

Le fichier ne contient que la dernière version (2.0.9, `versionCode` 72) et
son hash de commit complet, comme exigé par le template F-Droid. Les prochaines versions seront détectées par
`AutoUpdateMode: Version` / `UpdateCheckMode: Tags`.

## Signature

F-Droid signe les APK avec sa propre clé. Un APK installé depuis GitHub (clé
stable du projet) doit être désinstallé avant d'installer la version F-Droid,
sinon Android refuse la mise à jour pour cause de signature différente.

## Vérifications

- `minSdkVersion` 23, `targetSdkVersion` 37, Gradle 9.9.1, JDK 17.
- Réseau : HTTP en clair limité à l'AP d'appairage `10.10.10.1` et aux hôtes
  locaux `.local`/`.lan` ; MQTT en TLS.
- Aucun identifiant du cloud Meross, aucune télémétrie.
- Construire avec `gradleprops: [fdroidBuild=true]` : le lien et la
  vérification de mise à jour GitHub sont alors absents. L’app détecte aussi
  une installation via F-Droid (`org.fdroid.fdroid` / `org.fdroid.basic`).
- Fastlane : descriptions et changelog dans `fastlane/metadata/android/`.
