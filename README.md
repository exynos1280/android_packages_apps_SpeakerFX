## SpeakerFX (AudioFX fork)

This is simply a modification of LineageOS' [AudioFX](https://github.com/LineageOS/android_packages_apps_AudioFX) that only works with the loudspeaker output. It replaces AudioFX.

It applies some sane defaults to compensante for having no vendor speaker equalization in custom ROMs.

### Changes and notes
* Output selection removed.
* Disabled when another output is being used.
* `LoudnessEnhancer` effect is enabled and hardcoded to **+8db**.
* The app is not visible in the launcher. It's integrated the Settings app over at `Sound and vibration` -> `SpeakerFX`.
* Presets and custom EQ from AudioFX are still present.

### Default configuration
* Bass: **50%**
* Virtualizer: **0%**
* Preset: **Small speaker**

### Screenshot

<img src="https://img001.prntscr.com/file/img001/R_zKhvxoRoWKt5cnzoBYcw.png" alt="App screenshot" width="400"/>

### How to use

Clone to `packages/apps/SpeakerFX` in your ROM or add to your device tree.

```
git clone https://github.com/exynos1280/android_packages_apps_SpeakerFX -b lineage-22.2 packages/apps/SpeakerFX
```

Add to your `common.mk` / `device.mk`:

```
# SpeakerFX
PRODUCT_PACKAGES += SpeakerFX
```

This will override AudioFX if enabled.

### Credits
**All credits go to LineageOS devs for AudioFX!**
