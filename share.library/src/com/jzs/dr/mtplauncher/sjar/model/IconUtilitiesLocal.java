package com.jzs.dr.mtplauncher.sjar.model;

import android.content.Context;

import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.internal.manager.IconUtilities;

public class IconUtilitiesLocal extends IconUtilities {
    
    public IconUtilitiesLocal(Context context, ISharedPrefSettingsManager settingsManager){
        super(context, (settingsManager != null ? settingsManager.getAppIconSize() : 0));
        //this(context, 0, 0);
    }
}
