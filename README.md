# FuelGo Driver (Android)

Native Android app (Kotlin + Jetpack Compose) for FuelGo tanker drivers. Same architecture,
theme and networking as `fuelgo_customer`, wired to the `FuelGo.Api` .NET backend. It mirrors the
web portal's driver screens (`Frontend/src/app/features/driver`).

## What it does

| Area | Web portal | This app |
|---|---|---|
| Login | Driver role login | Email/mobile + password; rejects non-driver accounts |
| Job queue | `/driver` | **Jobs** tab: status cards, quick actions, auto-refresh every 15 s, pull-to-refresh |
| Job detail | `/driver/job` | **Active** tab / job details: stages, OpenStreetMap map with destination pin + area search, navigate in Maps, flag an issue |
| Status flow | Assigned, EnRoute, Arrived | Start ride, Mark arrived (backend-enforced transitions) |
| Proof of delivery | `/driver/proof` | 4-digit customer OTP, litres dispensed (+mismatch warning), cash / wallet / credit payment gate, confirm delivery |
| Failed delivery | n/a | "Delivery failed" from the Arrived stage |
| Shift & duty | Commander driver management | **Shift** tab: start/end shift, availability switch, recent shifts, incident log + report |
| Notifications | Bell | Top-bar bell with unread badge (polled every 30 s) |
| Theme | Light/dark toggle | Light/dark toggle (saved) |
| Profile | n/a | Licence, rating, deliveries, assigned tanker, change password, server settings, log out |

Delivery flow: **Assigned -> (Start ride) -> EnRoute -> (Mark arrived) -> Arrived -> (OTP + litres) -> Delivered.**
`Delivered` is only reachable through `POST orders/{id}/deliver`, which verifies the customer's OTP.

## Backend endpoints used

`Auth/login`, `Auth/change-password`, `orders/driver/my`, `orders/{id}/status`, `orders/{id}/deliver`,
`drivers/me`, `drivers/{id}` (availability), `drivers/{id}/shifts[/start|/{shiftId}/end]`,
`drivers/{id}/incidents`, `notifications[/{id}/read|/read-all]`.

## Server address

The API address is not compiled in. On first launch tap the menu on the login screen ->
**API Configuration** (or Profile -> **Server settings** once signed in), e.g. `http://192.168.1.42:5086/api`.

## Build the signed APK on GitHub Actions

`.github/workflows/build-release.yml` builds a signed **release** APK on every push (and on `v*` tags
publishes a GitHub Release). Add these four repository secrets first
(**Settings -> Secrets and variables -> Actions**):

- `RELEASE_KEYSTORE_BASE64` - base64 of your `.keystore` file
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

You can reuse the customer app's keystore values or create a new keystore for the driver app
(`keytool -genkeypair -v -keystore fuelgo-driver.keystore -alias fuelgo-driver -keyalg RSA -keysize 2048 -validity 10000`,
then `base64 -w0 fuelgo-driver.keystore`). **Never commit the keystore or its passwords**; `.gitignore` excludes them.

Download the APK from the run's **Artifacts** (`fuelgo-driver-release-apk`). A manual unsigned debug build is
available as **Build debug APK (manual)**.

## Local build

Open the folder in Android Studio (Koala+ / JDK 17). The repo ships no Gradle wrapper jar; accept
**Create Gradle Wrapper** when prompted, or run `gradle wrapper --gradle-version 8.7`.
