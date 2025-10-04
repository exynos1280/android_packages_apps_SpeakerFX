/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.audiofx.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.lineageos.audiofx.Constants;
import org.lineageos.audiofx.R;
import org.lineageos.audiofx.fragment.AudioFxFragment;
import org.lineageos.audiofx.service.AudioFxService;
import org.lineageos.audiofx.service.DevicePreferenceManager;

public class ActivityMusic extends Activity {

    private static final String TAG = ActivityMusic.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final String TAG_AUDIOFX = "audiofx";
    public static final String EXTRA_CALLING_PACKAGE = "audiofx::extra_calling_package";

    private MaterialSwitch mCurrentDeviceToggle;
    MasterConfigControl mConfig;
    String mCallingPackage;

    private boolean mWaitingForService = true;
    private SharedPreferences.OnSharedPreferenceChangeListener mServiceReadyObserver;

    private final CompoundButton.OnCheckedChangeListener mGlobalEnableToggleListener
            = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView,
                final boolean isChecked) {
            mConfig.setCurrentDeviceEnabled(isChecked);
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        if (DEBUG) {
            Log.i(TAG, "onCreate() called with "
                    + "savedInstanceState = [" + savedInstanceState + "]");
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_fragment),
                (view, insets) -> {
            Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    view.getPaddingLeft(),
                    systemInsets.top,
                    view.getPaddingRight(),
                    systemInsets.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });

        mCallingPackage = getIntent().getStringExtra(EXTRA_CALLING_PACKAGE);
        Log.i(TAG, "calling package: " + mCallingPackage);

        mConfig = MasterConfigControl.getInstance(this);

        final SharedPreferences globalPrefs = Constants.getGlobalPrefs(this);

        mWaitingForService = !defaultsSetup();
        if (mWaitingForService) {
            Log.w(TAG, "waiting for service.");
            mServiceReadyObserver = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                        String key) {
                    if (key != null && key.equals(Constants.SAVED_DEFAULTS) && defaultsSetup()) {
                        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
                        mConfig.onResetDefaults();
                        init(savedInstanceState);

                        mWaitingForService = false;
                        mServiceReadyObserver = null;
                    }
                }
            };
            globalPrefs.registerOnSharedPreferenceChangeListener(mServiceReadyObserver);
            startService(new Intent(ActivityMusic.this, AudioFxService.class));
            // TODO add loading fragment if service initialization takes too long
        } else {
            init(savedInstanceState);
        }
    }

    private boolean defaultsSetup() {
        final int targetVersion = DevicePreferenceManager.CURRENT_PREFS_INT_VERSION;
        final SharedPreferences prefs = Constants.getGlobalPrefs(this);
        final int currentVersion = prefs.getInt(Constants.AUDIOFX_GLOBAL_PREFS_VERSION_INT, 0);
        final boolean defaultsSaved = prefs.getBoolean(Constants.SAVED_DEFAULTS, false);
        return defaultsSaved && currentVersion >= targetVersion;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // should null it out if one was there, compat redirector with package will go through
        // onCreate
        mCallingPackage = intent.getStringExtra(EXTRA_CALLING_PACKAGE);
    }

    @Override
    protected void onDestroy() {
        if (mServiceReadyObserver != null) {
            Constants.getGlobalPrefs(this)
                    .unregisterOnSharedPreferenceChangeListener(mServiceReadyObserver);
            mServiceReadyObserver = null;
        }
        super.onDestroy();
    }

    private void init(Bundle savedInstanceState) {
        mConfig = MasterConfigControl.getInstance(this);

        // Bind to service so we can access DevicePreferenceManager
        mConfig.bindService();

        ActionBar ab = getActionBar();
        ab.setTitle(R.string.app_name_lineage);
        ab.setDisplayShowTitleEnabled(true);

        // Set device preset info as subtitle (will be updated when UI is ready)
        getActionBar().setSubtitle("Defaults: loading...");

        final View extraView = LayoutInflater.from(this)
                .inflate(R.layout.action_bar_custom_components, null);
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL);
        ab.setCustomView(extraView, lp);
        ab.setDisplayShowCustomEnabled(true);

        mCurrentDeviceToggle = ab.getCustomView().findViewById(R.id.global_toggle);
        mCurrentDeviceToggle.setOnCheckedChangeListener(mGlobalEnableToggleListener);

        Button resetButton = ab.getCustomView().findViewById(R.id.reset_settings_button);
        resetButton.setOnClickListener(v -> {
            Log.i(TAG, "Reset triggered");
            mConfig.resetAllSettings();
        });

        if (savedInstanceState == null && findViewById(R.id.main_fragment) != null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.main_fragment, new AudioFxFragment(), TAG_AUDIOFX)
                    .commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) {
            Log.i(TAG, "onResume() called");
        }
        // Check for device changes when app resumes (e.g., after switching outputs)
        if (mConfig != null) {
            mConfig.checkForDeviceChange();
        }

        // Update subtitle when user opens the app UI
        updateDevicePresetSubtitle();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) {
            Log.i(TAG, "onPause() called");
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (DEBUG) {
            Log.i(TAG, "onConfigurationChanged() called with "
                    + "newConfig = [" + newConfig + "]");
        }
        if (newConfig.orientation != getResources().getConfiguration().orientation) {
            mCurrentDeviceToggle = null;
        }
    }

    public void setGlobalToggleChecked(boolean checked) {
        if (mCurrentDeviceToggle != null) {
            mCurrentDeviceToggle.setOnCheckedChangeListener(null);
            mCurrentDeviceToggle.setChecked(checked);
            mCurrentDeviceToggle.setOnCheckedChangeListener(mGlobalEnableToggleListener);
        }
    }

    public void setGlobalToggleEnabled(boolean enabled) {
        if (mCurrentDeviceToggle != null) {
            mCurrentDeviceToggle.setEnabled(enabled);
        }
    }

    public CompoundButton getGlobalSwitch() {
        return mCurrentDeviceToggle;
    }

    private void updateDevicePresetSubtitle() {
        try {
            if (mConfig == null) {
                getActionBar().setSubtitle("Defaults: config unavailable");
                return;
            }

            DevicePreferenceManager devicePrefs = mConfig.getDevicePreferenceManager();
            if (devicePrefs != null) {
                String presetInfo = devicePrefs.getDevicePresetDisplayString();
                getActionBar().setSubtitle(presetInfo);
            } else {
                // Service not bound yet, try again after a short delay
                getActionBar().setSubtitle("Defaults: loading...");
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    DevicePreferenceManager retryPrefs = mConfig.getDevicePreferenceManager();
                    if (retryPrefs != null) {
                        String presetInfo = retryPrefs.getDevicePresetDisplayString();
                        getActionBar().setSubtitle(presetInfo);
                    } else {
                        getActionBar().setSubtitle("Defaults: service not ready");
                    }
                }, 500);
            }
        } catch (Exception e) {
            getActionBar().setSubtitle("Defaults: error");
        }
    }
}
