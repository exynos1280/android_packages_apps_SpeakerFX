/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.audiofx.backends;

import android.media.AudioDeviceInfo;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.BassBoost;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Virtualizer;
import android.util.Log;

import org.lineageos.audiofx.Constants;

/**
 * EffectSet which comprises standard Android effects
 */
class AndroidEffects extends EffectSetWithAndroidEq {

    /**
     * Session-specific bassboost
     */
    private BassBoost mBassBoost;

    /**
     * Session-specific virtualizer
     */
    private Virtualizer mVirtualizer;

    /**
     * Session-specific reverb
     */
    private PresetReverb mPresetReverb;

    /**
     * Session-specific loudness enhancer
     */
    private LoudnessEnhancer mLoudnessEnhancer;

    public AndroidEffects(int sessionId, AudioDeviceInfo deviceInfo) {
        super(sessionId, deviceInfo);
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        mBassBoost = new BassBoost(100, mSessionId);
        mVirtualizer = new Virtualizer(100, mSessionId);
        mPresetReverb = new PresetReverb(100, mSessionId);
        // create loudness enhancer for session
        try {
            mLoudnessEnhancer = new LoudnessEnhancer(mSessionId);
        } catch (Throwable t) {
            mLoudnessEnhancer = null;
        }
    }

    @Override
    public void release() {
        super.release();

        try {
            if (mBassBoost != null) {
                mBassBoost.release();
            }
        } catch (Exception e) {
            // ignored;
        }
        try {
            if (mVirtualizer != null) {
                mVirtualizer.release();
            }
        } catch (Exception e) {
            // ignored
        }
        try {
            if (mPresetReverb != null) {
                mPresetReverb.release();
            }
        } catch (Exception e) {
            // ignored
        }
        try {
            if (mLoudnessEnhancer != null) {
                mLoudnessEnhancer.release();
            }
        } catch (Exception e) {
            // ignored
        }
        mBassBoost = null;
        mVirtualizer = null;
        mPresetReverb = null;
        mLoudnessEnhancer = null;
    }

    @Override
    public synchronized void setDevice(AudioDeviceInfo deviceInfo) {
        super.setDevice(deviceInfo);
        // apply speaker-specific gain compensation
        if (mLoudnessEnhancer != null && deviceInfo != null) {
            final boolean isSpeaker = deviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;
            try {
                if (isSpeaker) {
                    // High boost: +24 dB
                    mLoudnessEnhancer.setTargetGain(2400);
                    mLoudnessEnhancer.setEnabled(true);
                } else {
                    mLoudnessEnhancer.setEnabled(false);
                    mLoudnessEnhancer.setTargetGain(0);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update LoudnessEnhancer for device change", e);
            }
        }
    }

    @Override
    public void setGlobalEnabled(boolean globalEnabled) {
        super.setGlobalEnabled(globalEnabled);

        if (!globalEnabled) {
            // disable everything. it will get explictly enabled
            // individually when necessary.
            try {
                if (mVirtualizer != null) {
                    mVirtualizer.setEnabled(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to disable virtualizer!", e);
            }
            try {
                if (mBassBoost != null) {
                    mBassBoost.setEnabled(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to disable bass boost!", e);
            }
            try {
                if (mPresetReverb != null) {
                    mPresetReverb.setEnabled(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to disable reverb!", e);
            }
            try {
                if (mLoudnessEnhancer != null) {
                    mLoudnessEnhancer.setEnabled(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to disable loudness enhancer!", e);
            }
        }
    }

    @Override
    public boolean hasVirtualizer() {
        return mVirtualizer != null;
    }

    @Override
    public boolean hasReverb() {
        return mPresetReverb != null;
    }

    @Override
    public boolean hasBassBoost() {
        return mBassBoost != null;
    }

    @Override
    public void enableBassBoost(boolean enable) {
        try {
            if (mBassBoost != null) {
                mBassBoost.setEnabled(enable);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to " + (enable ? "enable" : "disable") + " bass boost!", e);
        }
    }

    @Override
    public void setBassBoostStrength(short strength) {
        setParameterSafe(mBassBoost, BassBoost.PARAM_STRENGTH, strength);
    }

    @Override
    public void enableVirtualizer(boolean enable) {
        try {
            if (mVirtualizer != null) {
                mVirtualizer.setEnabled(enable);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to " + (enable ? "enable" : "disable") + " virtualizer!", e);
        }
    }

    @Override
    public void setVirtualizerStrength(short strength) {
        setParameterSafe(mVirtualizer, Virtualizer.PARAM_STRENGTH, strength);
    }

    @Override
    public void enableReverb(boolean enable) {
        try {
            if (mPresetReverb != null) {
                mPresetReverb.setEnabled(enable);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to " + (enable ? "enable" : "disable") + " preset reverb!", e);
        }

    }

    @Override
    public void setReverbPreset(short preset) {
        setParameterSafe(mPresetReverb, PresetReverb.PARAM_PRESET, preset);
    }

    @Override
    public int getBrand() {
        return Constants.EFFECT_TYPE_ANDROID;
    }

    private void setParameterSafe(AudioEffect e, int p, short v) {
        if (e == null) {
            return;
        }
        try {
            e.setParameter(p, v);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to set param " + p + " for effect " + e.getDescriptor().name, ex);
        }
    }

    @Override
    public void setOutputGainMillibels(int gainMb) {
        if (mLoudnessEnhancer == null) {
            return;
        }
        try {
            mLoudnessEnhancer.setTargetGain(gainMb);
            mLoudnessEnhancer.setEnabled(gainMb != 0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set loudness target gain", e);
        }
    }
}
