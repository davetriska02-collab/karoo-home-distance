# karoo-home-distance

A Karoo cycling computer extension that shows how far you are from home during a ride. Built for Witley, Surrey (GU8 5SX).

## What it does

Two data fields you can add to any ride profile on your Hammerhead Karoo:

**Home Distance** — A numeric field showing distance to home in your chosen units (km or miles). Karoo handles the formatting automatically, so it looks and behaves like any other distance field.

**Home Direction** — A graphical field showing a directional arrow pointing towards home, with distance text below it. The arrow colour indicates roughly how far you are:
- Green — within 5 km (nearly there)
- Amber — 5 to 20 km (on the way)
- Red — more than 20 km (long way to go)

Home is hardcoded to lat 51.153, lon -0.656 (Witley, Surrey GU8 5SX). To change it, edit `HOME_LAT` and `HOME_LON` in `GpsUtils.kt` and rebuild.

## Prerequisites

- Android Studio Hedgehog or later
- JDK 17+
- A GitHub Personal Access Token with `read:packages` scope (to download the `karoo-ext` SDK from GitHub Packages)

## Build instructions

### 1. Set up GitHub Packages credentials

The `karoo-ext` SDK is hosted on GitHub Packages. You need a GitHub PAT to download it.

Option A — add to `gradle.properties` in the project root:
```
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PAT
```

Option B — set environment variables before building:
```sh
export USERNAME=YOUR_GITHUB_USERNAME
export TOKEN=YOUR_GITHUB_PAT
```

### 2. Build the APK

```sh
./gradlew assembleRelease
```

The signed APK will be at `app/build/outputs/apk/release/app-release-unsigned.apk`.

For a debug build (easier for sideloading during development):
```sh
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Installing on the Karoo

### Via ADB (simplest for development)

Connect your Karoo via USB and enable ADB in Developer Options, then:

```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Via Hammerhead Companion app sideload

1. Build a signed release APK
2. Transfer it to your phone
3. Open the Hammerhead Companion app
4. Go to Extensions, tap the menu, and choose "Install from file"
5. Select the APK

## Adding the data fields to a ride profile

1. Open the Karoo companion app on your phone
2. Go to Ride Profiles and select a profile
3. Edit a data page and add a new data field
4. Search for "Home" — you will see "Home Distance" and "Home Direction"
5. Add either or both to your layout and save

## Project structure

```
app/src/main/kotlin/uk/co/triska/karoohome/
    MainActivity.kt                     Entry-point activity (settings screen)
    GpsUtils.kt                         Haversine distance + bearing maths
    extension/
        HomeDistanceExtension.kt        KarooExtension service
        HomeDistanceDataType.kt         Numeric distance field
        HomeBearingDataType.kt          Graphical arrow + distance field
        Extensions.kt                   streamDataFlow + throttle helpers
    screens/
        MainScreen.kt                   Compose settings UI
    theme/
        Theme.kt                        Dark colour scheme
```

## Notes

- GPS updates are throttled to every 5 seconds to avoid battery drain.
- The extension only reads GPS data already being streamed by the Karoo system — it does not request location independently.
- Accuracy is filtered to < 500 m before any calculation is made.
