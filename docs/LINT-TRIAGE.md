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

## Remaining budget: 114 warnings

| Category | Count | Decision |
|---|---:|---|
| HardcodedText | 73 | dette de traduction ; à externaliser dans les ressources |
| SetTextI18n | 28 | formatage/ressources à reprendre |
| Overdraw | 4 | fonds de layout à revoir |
| VectorRaster | 2 | icônes à redimensionner ou à valider |
| NotifyDataSetChanged | 2 | optimisation RecyclerView |
| AndroidGradlePluginVersion | 1 | version AGP à aligner |
| StaticFieldLeak | 1 | cycle de vie du callback HTTP à revoir |
| VectorPath | 1 | simplification du tracé |
| MergeRootFrame | 1 | hiérarchie de layout à simplifier |
| IconLocation | 1 | PNG à placer dans un dossier de densité |

Ces avertissements ne sont pas désactivés globalement. Le budget CI les rend
visibles et empêche toute nouvelle régression au-delà de 114.
