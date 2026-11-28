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

## Remaining budget: 169 warnings

| Category | Count | Decision |
|---|---:|---|
| AndroidGradlePluginVersion | 1 | version AGP à aligner |
| HardcodedText | 73 | dette de traduction ; à externaliser dans les ressources |
| UnusedResources | 48 | nettoyage de ressources legacy à faire avec validation visuelle |
| SetTextI18n | 28 | formatage/ressources à reprendre |
| VectorRaster | 7 | icônes à redimensionner ou à valider |
| Overdraw | 4 | fonds de layout à revoir |
| IconLocation | 3 | PNG à placer dans un dossier de densité |
| NotifyDataSetChanged | 2 | optimisation RecyclerView |
| StaticFieldLeak | 1 | cycle de vie du callback HTTP à revoir |
| VectorPath | 1 | simplification du tracé |
| MergeRootFrame | 1 | hiérarchie de layout à simplifier |

Ces avertissements ne sont pas désactivés globalement. Le budget CI les rend
visibles et empêche toute nouvelle régression au-delà de 169.
