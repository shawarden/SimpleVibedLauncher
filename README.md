# Simple Launcher (Android TV / Google TV)

A very small home-screen replacement. No libraries, no ads, no network access.

**Home:** the time and day sit in the far left corner (display only). On the right, left to right: Apps, Inputs,
launcher Settings (house), Wi-Fi, and the TV's own Settings (cog). Favourite apps fill the rest: four per row, as wide
as the screen allows, with short rows and the whole block centred (it scrolls if there are more than fit).
**Apps:** every installed app. Hold OK on one for: Add/Remove Favourites, Force Stop, App Info, Uninstall.
**Favourites:** hold OK on one for: Remove from Favourites, Force Stop, App Info.
**Inputs:** lists the TV's HDMI (and other external) inputs and marks the ones with a device connected. OK switches to it.
**Wi-Fi:** first OK shows the network details (name, signal, band, speed, IP, gateway, DNS). OK again, on the
"Network settings" button that starts with focus, opens the TV's network settings. Back closes it.
**House (launcher settings):** OK or Left/Right changes a value; it is saved straight away.
* Clock format: same as the TV, 12 hour or 24 hour (applies to the home screen and the screensaver).
* Screensaver starts after: never, 1, 2, 5, 10 or 30 minutes.
* Screensaver clock moves: with gravity (speeds up falling, slows rising) or a clean bounce (constant speed, straight lines).
* Gravity strength / speed: low (the original), medium or high. In gravity mode it sets how strong the gravity is; with the
  clean bounce it sets how fast the clock moves. Faster movement means more edge bounces, so the colour changes more often.
* Screensaver clock shows: time and date, or time only.
* Screensaver brightness (32/64/96/128) and background brightness (16/32/48/64).
**Background:** dim colour drift. Each of R, G, B stays within the brightness limit (32 by default) and moves up or down by
exactly 8, slowly and at random.
**Screensaver:** starts after the idle time (2 minutes by default). Same idea as the background but with a limit of 64 by
default. The clock moves around the screen and never settles; the colour steps by 8 on every bounce off an edge.
**Going into the screensaver (from the home screen):** every icon and favourite is released and falls under the Gravity
strength setting (low/medium/high), each with its own small random sideways drift and a short random delay. Pieces that
reach the bottom keep going and leave the screen. While they fall the background morphs to the screensaver colours and
the clock grows to its screensaver size and is released too, but it bounces off the edges. Once everything else has
gone, the clock carries on bouncing as normal (colour steps on each bounce).
**Leaving it:** any key wakes it (the key is not passed on). Everything springs back up to its place with a small
overshoot, the clock shrinks and springs home, and the background morphs back to the home colour (about 2 seconds).
The system daydream (if your TV lets you choose one) uses the plain screensaver with no fall.

## Build

Needs Android Studio (Koala or newer) or JDK 17 plus the Android SDK 34.

* Android Studio: File > Open > this folder > Build > Build APK(s).
* Command line: needs JDK 17 and the Android SDK (set `ANDROID_HOME`, or put `sdk.dir=/path/to/sdk` in a `local.properties` file here), then `./gradlew assembleDebug`. The Gradle wrapper is included and downloads Gradle 8.7 itself.

The APK ends up at `app/build/outputs/apk/debug/app-debug.apk`.

## Install on the TV

1. TV: Settings > System > About, press OK on "Android TV OS build" 7 times, then Settings > System > Developer options > turn on USB debugging / Network debugging.
2. Computer: `adb connect <tv-ip>:5555` and accept the prompt on the TV.
3. `adb install -r app-debug.apk`

## Make it the Home screen

Google TV does not offer a "default launcher" choice, so set it over ADB.
First note what the TV uses now, so you can switch back:

    adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME

Then switch, and check the Home button:

    adb shell cmd package set-home-activity --user 0 local.simplelauncher/.HomeActivity
    adb shell input keyevent KEYCODE_HOME

To go back: run the same `set-home-activity` command with the component you noted above, or uninstall this app.

If the Home button on the remote still opens the stock launcher, the firmware is intercepting it; this app does not
include a key-catching accessibility service. Don't disable the stock launcher until Home works.

## Notes

* **Force Stop:** Android does not let normal apps force-stop other apps. "Force Stop" in the menus kills the app's
  background processes (the most a normal app may do). For a true Force stop, use "App Info" and press Force stop there.
* **Screen saver:** the TV's own screen saver / ambient mode will still start after its own timeout (Settings > System >
  Ambient mode, or Screen saver). Set that to a longer time or Never if you want this one to run. The app also registers
  as a system screen saver ("Dim colour clock") for TVs that let you pick one.
* **Settings:** the cog opens the TV's normal Settings.
* **Inputs:** switching uses the standard Android TV request for an HDMI input. Some TVs route it through their own
  TV app; if one doesn't react, tell me which TV software it is and the switching can be adapted.
* **Wi-Fi name:** Android only shows the network name to apps with the Location permission, so the app asks once the
  first time you press the Wi-Fi icon. If you say no (or the TV's Location setting is off) everything else still shows.
* **Tuning:** every number (colour limits, step, speeds, gravity, idle time, columns) is in `Config.java`.
