/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.audiofx.service;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import org.lineageos.audiofx.service.AudioFxService;

import java.util.HashMap;
import java.util.Map;

public class DeviceDefaultsManager {

    private static final String TAG = AudioFxService.TAG;
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public enum PresetSource {
        DEVICE("device"),
        PLATFORM("platform"),
        FALLBACK("fallback");

        private final String displayName;

        PresetSource(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static class DevicePreset {
        public final String name;
        public final String presetValues;
        public final PresetSource source;

        public DevicePreset(String name, String presetValues, PresetSource source) {
            this.name = name;
            this.presetValues = presetValues;
            this.source = source;
        }

        public String getDisplayString() {
            return String.format("Defaults: %s (%s)", name, source.getDisplayName());
        }
    }

    private static final Map<String, String> DEVICE_PRESETS = new HashMap<>();
    private static final Map<String, String> PLATFORM_PRESETS = new HashMap<>();

    private static boolean sDeviceDetectionLogged = false;

    // Initialize device-specific presets
    static {
        // Device presets (higher priority)
        DEVICE_PRESETS.put("ginkgo", "-300;200;-700;-600;200");

        // Platform presets (lower priority)
        PLATFORM_PRESETS.put("s5e8825", "-50;530;-1000;-550;-10");
    }

    private final Context mContext;
    private String mDeviceName;
    private String mPlatformName;
    private DevicePreset mCurrentPreset;

    public DeviceDefaultsManager(Context context) {
        mContext = context;
        detectDeviceAndPlatform();
        selectPreset();
    }

    /**
     * Check if device/platform detection has changed and re-select preset if needed
     * This should be called when the app starts to handle cases where new presets were added
     */
    public boolean checkAndUpdatePreset() {
        String oldDevice = mDeviceName;
        String oldPlatform = mPlatformName;
        DeviceDefaultsManager.DevicePreset oldPreset = mCurrentPreset;

        detectDeviceAndPlatform();
        selectPreset();

        // Check if anything changed
        boolean changed = !oldDevice.equals(mDeviceName) || 
                         !oldPlatform.equals(mPlatformName) ||
                         !oldPreset.name.equals(mCurrentPreset.name) ||
                         oldPreset.source != mCurrentPreset.source;

        if (changed) {
            Log.i(TAG, "Device/platform detection changed:");
            Log.i(TAG, "  Device: '" + oldDevice + "' -> '" + mDeviceName + "'");
            Log.i(TAG, "  Platform: '" + oldPlatform + "' -> '" + mPlatformName + "'");
            Log.i(TAG, "  Preset: '" + oldPreset.getDisplayString() + "' -> '" + mCurrentPreset.getDisplayString() + "'");
        }

        return changed;
    }

    private void detectDeviceAndPlatform() {
        mDeviceName = SystemProperties.get("ro.product.device", "");
        mPlatformName = SystemProperties.get("ro.product.board", "");

        // Only log device detection once per boot
        if (!sDeviceDetectionLogged) {
            Log.i(TAG, "Device detection: device='" + mDeviceName + "', platform='" + mPlatformName + "'");
            sDeviceDetectionLogged = true;
        }
    }

    private void selectPreset() {
        // Priority: device > platform > fallback
        if (!mDeviceName.isEmpty() && DEVICE_PRESETS.containsKey(mDeviceName)) {
            mCurrentPreset = new DevicePreset(
                mDeviceName, 
                DEVICE_PRESETS.get(mDeviceName), 
                PresetSource.DEVICE
            );
        } else if (!mPlatformName.isEmpty() && PLATFORM_PRESETS.containsKey(mPlatformName)) {
            mCurrentPreset = new DevicePreset(
                mPlatformName, 
                PLATFORM_PRESETS.get(mPlatformName), 
                PresetSource.PLATFORM
            );
        } else {
            // Fallback to Exynos 1280 defaults
            mCurrentPreset = new DevicePreset(
                "s5e8825", 
                "-50;530;-1000;-550;-10", 
                PresetSource.FALLBACK
            );
        }
    }

    public DevicePreset getCurrentPreset() {
        return mCurrentPreset;
    }

    public String getPresetValues() {
        return mCurrentPreset.presetValues;
    }

    public String getPresetDisplayString() {
        return mCurrentPreset.getDisplayString();
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public String getPlatformName() {
        return mPlatformName;
    }

    public boolean isDeviceSupported() {
        return !mDeviceName.isEmpty() && DEVICE_PRESETS.containsKey(mDeviceName);
    }

    public boolean isPlatformSupported() {
        return !mPlatformName.isEmpty() && PLATFORM_PRESETS.containsKey(mPlatformName);
    }
}
