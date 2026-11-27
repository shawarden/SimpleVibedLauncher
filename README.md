# SimpleVibedLauncher (Android TV / Google TV)

A very small home-screen replacement. No libraries, no ads. The only network use is the optional channel buttons
(see Button mapper), which talk to the TV itself.

**Home:** the time and day sit in the far left corner (display only). On the right, left to right: Apps, Inputs,
launcher Settings (house), Wi-Fi, and the TV's own Settings (cog). Favourite apps fill the rest: four per row, as wide
as the screen allows, with short rows and the whole block centred (it scrolls if there are more than fit).
**Apps:** every installed app. Hold OK on one for: Add/Remove Favourites, Force Stop, App Info, Uninstall.
**Favourites:** hold OK on one for: Remove from Favourites, Force Stop, App Info.
**Inputs:** (the HDMI connector icon) lists the TV's HDMI (and other external) inputs and marks the ones with a device connected. OK switches to it.
**Wi-Fi:** first OK shows the network details (name, signal, band, speed, IP, gateway, DNS). OK again, on the
"Network settings" button that starts with focus, opens the TV's network settings. Back closes it.
**House (launcher settings):** OK or Left/Right changes a value; it is saved straight away.
* Clock format: same as the TV, 12 hour or 24 hour (applies to the home screen and the screensaver).
* Screensaver starts after: never, 1, 2, 5, 10 or 30 minutes.
* Screensaver clock moves: with gravity (speeds up falling, slows rising) or a clean bounce (constant speed, straight lines).
* Gravity strength / speed: low (the original), medium or high. In gravity mode it sets how strong the gravity is; with the
  clean bounce it sets how fast the clock moves. Faster movement means more edge bounces, so the colour changes more often.
* Gravity mode background: "Colour steps on bounce" (the original: the colour changes a step each time the clock hits
  an edge) or "Breathes with the clock". Breathing: the background is black (000000) when the clock is on the bottom
  edge and brightens as it rises, reaching its peak colour at the top of the arc, then dims again as it falls. Each
  of R, G and B has its own peak, picked at random between 0 and the screensaver brightness limit, and a new set of
  peaks is picked at every landing (the screen is black then, so you don't see the change). Only used with gravity.
* Screensaver clock shows: time and date, or time only.
* Screensaver brightness (32/64/96/128) and background brightness (16/32/48/64).
* Advanced settings: a slider for each adjustable number (see below).
* Button mapper: opens the screen described below.
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

## Button mapper

Launcher settings > Button mapper. It lists the buttons you have set up. **+ Add a button** asks you to press a button
on the remote and then what it should do. OK on a listed button gives: change what it does, add/change/clear an alias
(shown instead of the button's own name), test it now, remove it. Back, Home, the arrows and OK can't be remapped.

A button can be set to:
* **Media controls:** play/pause, play, pause, stop, next, previous, rewind, fast forward (sent to whatever is playing).
* **Volume:** up, down, mute.
* **Channels and TV functions:** channel up/down, input select, HDMI 1-4, audio, subtitles, info, options, help, the four
  colour buttons, standby. These are sent to the TV itself (see IP control below).
* **Android TV functions:** Home, Back, Recent apps, Notifications, Quick settings, Power menu, Screenshot, voice
  assistant, and the D-pad (the D-pad ones only on Android 13 or newer).
* **System shortcuts:** TV settings, network, Bluetooth, display, sound, apps, accessibility, keyboard, date and time,
  language, storage, developer options.
* **This launcher:** home screen, apps drawer, inputs list, network info, launcher settings, button mapper, start the
  screensaver now.
* **Open an app**, **open an app shortcut** (shortcuts that apps have registered; most TV apps have none, and Android
  only lets the default home app list or start them), or **Nothing** (the button is simply blocked).

**The service.** Android only lets an *accessibility service* see remote buttons while another app is in front, so
the mapper needs the "SimpleVibedLauncher buttons" service switched on, once. The mapper's first row shows whether it is on;
OK on it opens the TV's Accessibility settings. (It does not read the screen.) To do it from a computer instead:

    adb shell settings get secure enabled_accessibility_services      # see what is on already
    adb shell settings put secure enabled_accessibility_services <existing>:local.simplelauncher/local.simplelauncher.KeyService
    adb shell settings put secure accessibility_enabled 1

(put the existing value in place of `<existing>`, or leave out `<existing>:` if the first command printed null.) If
you already use another button-remapping app, don't give both apps the same button.

**IP control (for the channel/TV buttons).** An app can't press Channel up on the TV by itself, so these buttons send
Sony's own network remote code to the TV. On the TV, switch on IP control (in Settings under Network, Home network;
the wording differs between models), set Authentication to "Normal and Pre-Shared Key", set a Pre-Shared Key, then enter
the same key in the Button mapper's last row. Everything else works without this.

## Advanced settings (sliders)

Launcher settings > Advanced settings. Left / Right moves a slider (hold the key to go faster); OK puts that one slider
back to its original value; the last row resets all of them. Values that have been changed are shown in yellow. Times
are in seconds. "Lowest / highest" pairs always stay in order (moving one past the other drags the other along).
Changes are saved at once. The first-screen settings (idle time, brightness limits) are not repeated here.

* **Home background:** size of each colour change (1-16 on the 0-255 scale), fastest and slowest colour change (seconds).
* **Screensaver:** colour fade after each bounce; gravity at Low, and the Medium and High multipliers; bounce energy
  (lowest, highest; it sets the speed at the top of each arc); sideways speed (slowest, fastest); clean-bounce
  up-down speed (slowest, fastest) and its Medium and High multipliers.
* **Going into the screensaver:** background colour change time, clock growing time, sideways drift of the falling
  icons (least, most) and how long they take to all let go.
* **Coming back:** background colour change time, time by which everything is back in place, clock shrinking time.
* **Layout:** favourites per row (2-8) and apps per row in the drawer (3-10).

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
