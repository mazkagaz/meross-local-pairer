# Modifications

- Fork date: 2026-11-14
- Upstream: https://github.com/albertogeniola/Custom-Meross-Pairer
- Upstream version: 0.0.11
- Upstream license: GPL-2.0
- Fork license: GPL-2.0 (see `LICENSE`)
- Fork version: 1.1.1 (`versionCode` 45)
- Application id: `org.localpairer` (distinct from upstream)
- Display name: **Meross Local Pairer** (unofficial)

## Branding

- Banner: `branding/meross-local-pairer-banner.jpeg`, displayed at the top of every screen
  and as the splash art.
- Launcher logo: `branding/meross-local-pairer-logo.jpeg`, used to generate the
  `mipmap-*` launcher icons.

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
| `MainActivity`, `DeviceListFragment`, `AboutFragment`, `SupportThisApp` | Device-first navigation; drawer replaced by contextual pair/refresh/settings/info actions; About and support pages updated. |
| `fragment_account.xml`, `AccountFragment` | Official Meross cloud login removed; the app is local-only. |
| `SplashActivity`, `ConfigureMqttFragment` | Optional "never ask again" disclaimer; a read-only "default" broker derived from the API host, editable via a Modify checkbox. |

## Security and packaging

| File | Change |
|---|---|
| `AndroidPreferencesManager.java` | Store Meross API credentials with SecurePreferences; migrate legacy plaintext entries. |
| `res/xml/network_security_config.xml` | Cleartext HTTP limited to the AP endpoint `10.10.10.1` and local `.local`/`.lan` API hostnames. |
| `app/build.gradle` | APKs signed with a stable project key when the CI signing variables are present; version 1.1.1. |
| `app/.../ble/MerossBleClient.java`, `MerossBleProtocol.java`, `BlePairActivity.java` | **New**: Bluetooth LE provisioning for firmware 8 devices (service A00A, B002 write, B003 notify, JSON framing, AES Wi-Fi password); automatic BLE discovery in the scan screen. |
| `AllowedWifiNetworks.java`, `NetworkPolicy.java` | **New**: broker traffic restricted to user-validated Wi-Fi networks; device APs allowed during pairing. |
| `build.gradle` | Removed dead `jcenter()` and the Paho snapshot repository (both returned errors on CI); use Maven Central + JitPack. |
| `.github/workflows/build.yaml` | Install Android SDK 35 explicitly before the Gradle build. |
| Android package/namespace | Renamed to `org.localpairer`; app label changed to Meross Local Pairer. |

The Wi-Fi provisioning protocol was not changed: `Appliance.Config.Key`,
`Appliance.Config.WifiList`, `Appliance.Config.Wifi` and
`Appliance.System.All` payloads remain compatible with the official app.
Firmware 8 adds a Bluetooth LE transport for the same protocol, using
`Appliance.Config.Key` + `Appliance.Config.WifiX` frames over GATT A00A.

All source files remain under GPL-2.0. The upstream LICENSE is preserved.
