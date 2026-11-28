# Android lint triage

## Fixed

- 3 erreurs de lint corrigées ; `lintDebug` est dans la CI.
- `DefaultLocale`, `ObsoleteSdkInt`, `RtlHardcoded`, `RtlSymmetry`,
  `TypographyEllipsis`, `UseCompatTextViewDrawableXml`, `ContentDescription`,
  `Deprecated`, `FragmentTagUsage`, `MonochromeLauncherIcon`,
  `LockedOrientationActivity`, `DiscouragedApi`, `CustomSplashScreen` :
  corrigés ou neutralisés localement avec justification.
- `TrustAllX509TrustManager` / `CustomX509TrustManager` : annotations locales
  sur `TlsProbe` et `AndroidTlsPolicy`, qui implémentent volontairement le
  probe TLS et le pinning TOFU.

## Remaining budget: 10 warnings

| Category | Count | Decision |
|---|---:|---|
| Overdraw | 4 | fonds de layout à revoir, validation visuelle nécessaire |
| NotifyDataSetChanged | 2 | liste remplacée en bloc ; passer à DiffUtil |
| AndroidGradlePluginVersion | 1 | Gradle 9.9.3 disponible, à traiter via Dependabot |
| StaticFieldLeak | 1 | cycle de vie du callback HTTP à revoir |
| VectorPath | 1 | simplification du tracé de l’icône satellite |
| MergeRootFrame | 1 | hiérarchie de layout à simplifier |

Ces avertissements ne sont pas désactivés globalement. Le budget CI les rend
visibles et empêche toute nouvelle régression au-delà de 10.
