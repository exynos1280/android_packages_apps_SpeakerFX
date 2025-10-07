## SpeakerFX (AudioFX fork)

This is simply a modification of LineageOS' [AudioFX](https://github.com/LineageOS/android_packages_apps_AudioFX) that only works with the loudspeaker output. It replaces AudioFX.

It applies some sane defaults to compensante for having no vendor speaker equalization in custom ROMs.

As of commit [f88d084](https://github.com/exynos1280/android_packages_apps_SpeakerFX/commit/f88d084b493f5eccfbca76792d7389bcbb6ec126) we also support different defaults per device.
Here are the supported devices and how it works:

#### Platforms with default presets:
- Exynos 1280 (s5e8825)

#### Devices with default presets:
- Redmi Note 8 (ginkgo)

Presets are loaded in this priority:

```device > platform > fallback preset```

That is to say, the ```device``` preset will try to be loaded first, if it doesn't exist, the ```platform``` preset will try to be loaded, and if that doesn't match either, the ```fallback``` preset will be loaded.
The fallback preset is currently a copy of the s5e8825 one.

**Wanna add a preset for you device? Submit a PR!**

### Changes and notes
* Output selection removed.
* Custom defaults for different devices.
* Disabled when another output is being used.
* `LoudnessEnhancer` effect is enabled and hardcoded to **+24db**.
* The app is not visible in the launcher. It's integrated the Settings app over at `Sound and vibration` -> `SpeakerFX`.
* Presets and custom EQ from AudioFX are still present.

### Default configuration
* Bass: **0%**
* Virtualizer: **0%**
* Preset: **Small speaker (custom, see above)**

### Screenshot

<img src="https://img001.prntscr.com/file/img001/0AXdpzw_TmWFKBg1Yfo9EA.png" alt="App screenshot" width="400"/>

### How to use

Clone to `packages/apps/SpeakerFX` in your ROM or add to your device tree.

```
git clone https://github.com/exynos1280/android_packages_apps_SpeakerFX packages/apps/SpeakerFX
```

Add to your `common.mk` / `device.mk`:

```
# SpeakerFX
PRODUCT_PACKAGES += SpeakerFX
```

This will override AudioFX if enabled.

### Credits
**All credits go to LineageOS devs for AudioFX!**
