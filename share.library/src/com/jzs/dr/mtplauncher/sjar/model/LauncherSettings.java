/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jzs.dr.mtplauncher.sjar.model;

import com.jzs.common.launcher.model.LauncherSettingsCommon;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Settings related utilities.
 */
public class LauncherSettings extends LauncherSettingsCommon {
	
//	private static final String TAG = "LauncherProvider";
//    private static final boolean LOGD = false;

	public static final String DATABASE_NAME = "launcher.db";

    public static final int DATABASE_VERSION = 2;

    
    
    public static final String TABLE_SETTINGS = "settings";
    
//    public static final String DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED =
//            "DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED";
//    public static final String DEFAULT_WORKSPACE_RESOURCE_ID =
//            "DEFAULT_WORKSPACE_RESOURCE_ID";
//    
    public static final Uri CONTENT_APPWIDGET_RESET_URI =
            Uri.parse("content://" + AUTHORITY + "/appWidgetReset");
    
    public static final class Settings extends android.provider.Settings.NameValueTable/* implements BaseColumns*/ {
    	
    	public static final String NAME = "name";
        public static final String VALUE = "value";
    	
    	public static final Uri CONTENT_URI = Uri.parse("content://" +
				LauncherSettings.AUTHORITY + "/" + LauncherSettings.TABLE_SETTINGS +
                "?" + LauncherSettings.PARAMETER_NOTIFY + "=true");

        /**
         * The content:// style URL for this table. When this Uri is used, no notification is
         * sent if the content changes.
         */
		public static final Uri CONTENT_URI_NO_NOTIFICATION = Uri.parse("content://" +
				LauncherSettings.AUTHORITY + "/" + LauncherSettings.TABLE_SETTINGS +
                "?" + LauncherSettings.PARAMETER_NOTIFY + "=false");

        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         * @param notify True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
		public static Uri getContentUri(long id, boolean notify) {
            return Uri.parse("content://" + LauncherSettings.AUTHORITY +
                    "/" + LauncherSettings.TABLE_SETTINGS + "/" + id + "?" +
                    LauncherSettings.PARAMETER_NOTIFY + "=" + notify);
        }
		
//		public static int getInt(ContentResolver cr, String name, int def) {
//            return getIntForUser(cr, name, def, UserHandle.myUserId());
//        }

    }
	
	
}
