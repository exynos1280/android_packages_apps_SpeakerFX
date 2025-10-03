/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.audiofx.service;

import static android.media.AudioDeviceInfo.convertDeviceTypeToInternalDevice;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioOutputChangeListener extends AudioDeviceCallback {

    private static final String TAG = "AudioFx-" + AudioOutputChangeListener.class.getSimpleName();

    private boolean mInitial = true;

    private final Context mContext;
    private final AudioManager mAudioManager;
    private final Handler mHandler;
    private int mLastDevice = -1;
    private boolean mIsMonitoring = false;

    private final ArrayList<AudioOutputChangedCallback> mCallbacks = new ArrayList<>();
    private final Runnable mPeriodicCheck = new Runnable() {
        @Override
        public void run() {
            if (mIsMonitoring) {
                checkForDeviceChange();
                mHandler.postDelayed(this, 2000); // Check every 2 seconds
            }
        }
    };

    public interface AudioOutputChangedCallback {
        void onAudioOutputChanged(boolean firstChange, AudioDeviceInfo outputDevice);
    }

    public AudioOutputChangeListener(Context context, Handler handler) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mHandler = handler;
    }

    public void addCallback(AudioOutputChangedCallback... callbacks) {
        synchronized (mCallbacks) {
            boolean initial = mCallbacks.size() == 0;
            mCallbacks.addAll(Arrays.asList(callbacks));
            if (initial) {
                mAudioManager.registerAudioDeviceCallback(this, mHandler);
                startPeriodicMonitoring();
            }
        }
    }

    public void removeCallback(AudioOutputChangedCallback... callbacks) {
        synchronized (mCallbacks) {
            mCallbacks.removeAll(Arrays.asList(callbacks));
            if (mCallbacks.size() == 0) {
                mAudioManager.unregisterAudioDeviceCallback(this);
                stopPeriodicMonitoring();
            }
        }
    }

    private void startPeriodicMonitoring() {
        if (!mIsMonitoring) {
            mIsMonitoring = true;
            Log.d(TAG, "Starting periodic device monitoring");
            mHandler.post(mPeriodicCheck);
        }
    }

    private void stopPeriodicMonitoring() {
        if (mIsMonitoring) {
            mIsMonitoring = false;
            Log.d(TAG, "Stopping periodic device monitoring");
            mHandler.removeCallbacks(mPeriodicCheck);
        }
    }

    private void callback() {
        synchronized (mCallbacks) {
            final AudioDeviceInfo device = getCurrentDevice();

            if (device == null) {
                Log.w(TAG, "Unable to determine audio device!");
                return;
            }

            if (mInitial || device.getId() != mLastDevice) {
                Log.d(TAG, "onAudioOutputChanged id: " + device.getId() +
                        " type: " + device.getType() +
                        " name: " + device.getProductName() +
                        " address: " + device.getAddress() +
                        " [" + device + "]");
                mLastDevice = device.getId();
                mHandler.post(() -> {
                    synchronized (mCallbacks) {
                        for (AudioOutputChangedCallback callback : mCallbacks) {
                            callback.onAudioOutputChanged(mInitial, device);
                        }
                    }
                });

                if (mInitial) {
                    mInitial = false;
                }
            }
        }
    }

    @Override
    public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
        Log.d(TAG, "Audio devices added: " + addedDevices.length);
        callback();
    }

    @Override
    public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
        Log.d(TAG, "Audio devices removed: " + removedDevices.length);
        callback();
    }

    /**
     * Force a device change check - useful when manual output switching occurs
     */
    public void checkForDeviceChange() {
        callback();
    }

    public List<AudioDeviceInfo> getConnectedOutputs() {
        final List<AudioDeviceInfo> outputs = new ArrayList<>();
        final int forMusic = mAudioManager.getDevicesForStream(AudioManager.STREAM_MUSIC);
        for (AudioDeviceInfo ai : mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if ((convertDeviceTypeToInternalDevice(ai.getType()) & forMusic) > 0) {
                outputs.add(ai);
            }
        }
        return outputs;
    }

    public AudioDeviceInfo getCurrentDevice() {
        // Get the current music stream routing to determine which device is active
        final int musicDevices = mAudioManager.getDevicesForStream(AudioManager.STREAM_MUSIC);
        final List<AudioDeviceInfo> connectedDevices = getConnectedOutputs();

        // Find the device that matches the current music stream routing
        for (AudioDeviceInfo device : connectedDevices) {
            final int deviceType = convertDeviceTypeToInternalDevice(device.getType());

            if ((deviceType & musicDevices) != 0) {
                return device;
            }
        }

        // If no device matches the routing mask, try to find the most likely candidate
        // Priority: Speaker > Wired Headphones > Bluetooth > Others
        AudioDeviceInfo speaker = null;
        AudioDeviceInfo wiredHeadphones = null;
        AudioDeviceInfo bluetooth = null;

        for (AudioDeviceInfo device : connectedDevices) {
            switch (device.getType()) {
                case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                    speaker = device;
                    break;
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                    wiredHeadphones = device;
                    break;
                case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                    bluetooth = device;
                    break;
            }
        }

        // Return in priority order
        if (speaker != null) {
            return speaker;
        } else if (wiredHeadphones != null) {
            return wiredHeadphones;
        } else if (bluetooth != null) {
            return bluetooth;
        } else if (connectedDevices.size() > 0) {
            return connectedDevices.get(0);
        }

        return null;
    }
}
