# Maintenance — Meross Local Pairer

Dernière revue : **2026-10-27**

## Politique de support

- **Android** : `compileSdk`/`targetSdk` 37, `minSdk` 24.
- **JDK CI** : 17 ; source/toolchain Java 10.
- **Gradle / AGP** : versions épinglées dans le wrapper et `build.gradle`.
- **F-Droid** : recette différée après le 2027-02-15 ; ne pas la considérer
  comme maintenue avant cette date.
- **Play Store** : décision séparée ; aucun build Play automatique.

## Rythme

| Fréquence | Action |
|---|---|
| Chaque PR | build, lint Android, tests unitaires, traductions ; budget lint de 10 warnings maximum |
| Hebdomadaire | rebuild planifié pour détecter les changements d’environnement |
| Mensuel | revue des PR Dependabot Gradle et GitHub Actions |
| Trimestriel | revue SDK Android, AGP, Gradle, JDK |
| Annuel | revue sécurité et politique de signature |

## Automatisations

- `Dependabot` : dépendances Gradle et GitHub Actions.
- Build planifié hebdomadaire.
- Workflow `CI` : contrôle des traductions et budget lint.
- `release.yaml` : signature et publication APK.

## Checklist de release

- [ ] `versionCode` et `versionName` sont cohérents.
- [ ] Le tag et la version de l’application correspondent.
- [ ] `CHANGELOG.md` contient l’entrée de version.
- [ ] Les traductions sont synchronisées.
- [ ] Le build release est signé avec les secrets de release.
- [ ] L’APK est testé sur au moins une version Android récente.
- [ ] La recette F-Droid n’est mise à jour qu’au moment de la soumission.

## Règles de conception

- Les tests de régression standard et dégradés sont ajoutés avec chaque
  fonctionnalité.
- Les erreurs de permissions, TLS, BLE et stockage doivent être testées.
- Un changement de SDK ou d’AGP est une PR dédiée, jamais mélangée à une
  évolution fonctionnelle.
- La politique de signature reste stable : Play et F-Droid ont des clés
  distinctes.
