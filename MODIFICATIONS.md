# Modifications

- Fork date: 2026-11-14
- Upstream: https://github.com/albertogeniola/Custom-Meross-Pairer
- Upstream version: 0.0.11
- Upstream license: GPL-2.0
- Fork license: GPL-2.0 (see `LICENSE`)
- Fork version: 0.1.3 (`versionCode` 25)
- Application id: `org.localpairer` (distinct from upstream)
- Display name: **Local Pairer** (unofficial)

## Behavior fixes

| File | Change |
|---|---|
| `app/.../ConfigureMqttFragment.java` | Fixed custom UserId/Key override: use `getText().toString()` and validate a numeric UserId. |
| `merosslib/.../MessageSetConfigKey.java` | Send `port`/`secondPort` as JSON numbers, add `redirect: 0`, allow distinct secondary endpoint. |
| `merosslib/.../MerossDeviceAp.java` | Validate response method/namespace, raise on `ERROR`, expose `setIp`/`setCloudKey`. |
| `app/.../FetchDeviceInfoFragment.java` | Replace the fixed 10 s wait with a 1 s retry loop (15 s max) for `System.All`. |
| `app/.../ExecutePairingFragment.java` | Validate `Config.Key` before `Config.Wifi`; show protocol error details; wait 1 s between commands. |
| `app/.../MerossUtils.java` | Broaden AP SSID detection, including `Meross_RS_chWF_*` MRS100 variants. |
| `app/.../AbstractWifiFragment.java` | Bind HTTP traffic to the target WiFi on Android 6-9 (pre-Q path). |
| `merosslib/src/test/.../ProtocolTest.java` | **New**: tests for numeric ports/redirect and SETACK/ERROR validation. |
| `MainActivity`, `DeviceListFragment`, `AboutFragment`, `SupportThisApp` | Device-first navigation; drawer replaced by contextual pair/refresh/info actions; About and support pages updated. |

## Security and packaging

| File | Change |
|---|---|
| `AndroidPreferencesManager.java` | Store Meross API credentials with SecurePreferences; migrate legacy plaintext entries. |
| `res/xml/network_security_config.xml` | Cleartext HTTP limited to the AP endpoint `10.10.10.1` and local `.local`/`.lan` API hostnames. |
| `app/build.gradle` | Release builds are unsigned instead of debug-signed; version 0.1.3. |
| `build.gradle` | Removed dead `jcenter()` and the Paho snapshot repository (both returned errors on CI); use Maven Central + JitPack. |
| `.github/workflows/build.yaml` | Install Android SDK 35 explicitly before the Gradle build. |
| Android package/namespace | Renamed to `org.localpairer`; app label changed to Local Pairer. |

The Meross provisioning protocol was not changed: `Appliance.Config.Key`,
`Appliance.Config.WifiList`, `Appliance.Config.Wifi` and
`Appliance.System.All` payloads remain compatible with the official app.

All source files remain under GPL-2.0. The upstream LICENSE is preserved.
