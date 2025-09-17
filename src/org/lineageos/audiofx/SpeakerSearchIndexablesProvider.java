/*
 * SPDX-FileCopyrightText: 2025 The SpeakerFX Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.audiofx;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesProvider;
import android.provider.SearchIndexablesContract;

public class SpeakerSearchIndexablesProvider extends SearchIndexablesProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryXmlResources(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS);
        cursor.addRow(new Object[] {
            1, // rank
            R.xml.speakerfx_settings, // xmlResId
            null, // className
            0, // iconResId
            "com.android.settings.action.EXTRA_SETTINGS", // intentAction
            "org.lineageos.audiofx", // intentTargetPackage
            "org.lineageos.audiofx.activity.ActivityMusic" // intentTargetClass
        });
        return cursor;
    }

    @Override
    public Cursor queryRawData(String[] projection) {
        return new MatrixCursor(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS);
    }

    @Override
    public Cursor queryNonIndexableKeys(String[] projection) {
        return new MatrixCursor(SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS);
    }
}
