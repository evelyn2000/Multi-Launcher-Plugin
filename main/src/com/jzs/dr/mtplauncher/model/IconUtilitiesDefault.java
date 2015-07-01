package com.jzs.dr.mtplauncher.model;

import android.content.Context;

import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.common.manager.IAppsManager;
import com.jzs.common.manager.IIconUtilities;

public final class IconUtilitiesDefault{

	protected static IIconUtilities sIconUtilities;
    public static IIconUtilities getInstance(Context context, ISharedPrefSettingsManager settingsManager, IResConfigManager res){
    	if (sIconUtilities == null) {
    	    IAppsManager appManager = (IAppsManager)context.getSystemService(IAppsManager.MANAGER_SERVICE);
    	    if(appManager != null){
    	        sIconUtilities = appManager.getIconUtilities();
    	    }
        }
        return sIconUtilities;
    }
    
    public void ReleaseInstance(){
    	sIconUtilities = null;
    }
}
