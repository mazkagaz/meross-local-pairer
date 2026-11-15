# Local Pairer — Meross provisioning for local brokers (unofficial)

Fork repository: https://github.com/mazkagaz/meross-local-pairer

Local Pairer is a fork of
[Custom Pairer for Meross Devices](https://github.com/albertogeniola/Custom-Meross-Pairer)
by Alberto Geniola. It configures Meross devices to use a local MQTT broker
instead of the Meross cloud. It is **not affiliated with Meross or the upstream
author**.

The app talks to the device in its provisioning AP (`http://10.10.10.1/config`)
and sends the standard Meross namespaces:

- `Appliance.System.All` / `Appliance.Config.WifiList` to inspect the device;
- `Appliance.Config.Key` to set the MQTT broker host/port and credentials;
- `Appliance.Config.Wifi` to join the target Wi-Fi network.

It works with the
[HA Meross local broker add-on](https://github.com/albertogeniola/ha-meross-local-brocker)
and any compatible MQTT broker.

## Fixes in this fork

- Custom UserId/Key override now reads the actual text fields and validates the
  numeric UserId (upstream sent the `EditText` object instead of its text).
- `Config.Key` sends `port`/`secondPort` as numbers and includes `redirect: 0`,
  matching the official Meross app.
- AP responses are validated: wrong method/namespace and `ERROR` replies are
  rejected instead of being treated as success.
- Device info is retried every second for up to 15 s instead of waiting a fixed
  10 s.
- `Config.Key` is checked before `Config.Wifi` is sent, with protocol error
  details shown in the UI.
- AP SSID detection accepts more firmware variants, including
  `Meross_RS_chWF_*`.
- Meross API credentials are stored with SecurePreferences; cleartext HTTP is
  restricted to the AP endpoint; release builds are no longer debug-signed.
- HTTP is bound to the target Wi-Fi on Android 6-9 (pre-Q connection path).

## Build

Requirements: JDK 17 and Android SDK 35.

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Release builds are intentionally unsigned:

```bash
./gradlew assembleReleaseUnsigned
```

Sign the resulting APK/AAB with your own key before distributing it.

Unit tests cover the `Config.Key` wire format and AP response validation:

```bash
./gradlew test
```

## Usage

1. Put the Meross device in provisioning/AP mode (usually a long press on the
   button until the LED blinks).
2. Open Local Pairer, grant Wi-Fi/location permissions, and scan for the
   `Meross_*` access point.
3. Select your home Wi-Fi and enter its password.
4. Enter the local broker host and port.
5. Optionally override UserId/Key with the dedicated credentials created for
   the local broker. Using dedicated credentials avoids reusing your Meross
   cloud key.
6. Start pairing. For the HA add-on, enable MQTT discovery if you want the
   broker to pre-fill its address.

The device reboots onto your Wi-Fi and connects to the broker over TLS.

## Known limitations

- Legacy `/do_cmd.htm` provisioning and BLE provisioning are not implemented.
- The AP address is currently fixed to `10.10.10.1`.
- WPA3/SAE networks are not supported.
- Pairing was not hardware-tested in this fork; the code builds and the protocol
  unit tests pass, but a real MRS100/broker round-trip is still required.

## License and attribution

GPL-2.0. The original project and LICENSE are preserved; see NOTICE and
MODIFICATIONS.md. The upstream README is kept as `UPSTREAM_README.md`.

Meross is a trademark of its respective owner.
