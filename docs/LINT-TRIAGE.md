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

## Budget: 0 warnings

Les derniers avertissements ont été corrigés ou neutralisés localement :

- Gradle passé en **9.9.3** ;
- `Overdraw` neutralisé sur les racines qui doivent peindre leur fond ou leur ripple ;
- `NotifyDataSetChanged` justifié pour les remplacements complets de listes ;
- `StaticFieldLeak` justifié pour les tâches HTTP courtes ;
- `VectorPath` conservé pour l’illustration satellite ;
- `MergeRootFrame` conservé pour le layout hôte d’activité.

Le budget CI est désormais **0 warning** : toute nouvelle alerte fait échouer le build.

Ces avertissements ne sont pas désactivés globalement. Le budget CI les rend
visibles et empêche toute nouvelle régression au-delà de 0.
