package com.jzs.dr.mtplauncher;

import java.lang.ref.WeakReference;

import com.jzs.common.launcher.IGlobalStaticFunc;
import com.jzs.common.launcher.IIconCache;
import com.jzs.common.launcher.ILauncherModel;
import com.jzs.common.launcher.ILauncherProvider;
import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.common.launcher.LauncherHelper;
import com.jzs.common.manager.IIconUtilities;
import com.jzs.dr.mtplauncher.defaults.LauncherDefault;
import com.jzs.dr.mtplauncher.defaults.LauncherModelDefault;
import com.jzs.dr.mtplauncher.model.GlobalStaticFunctions;
import com.jzs.dr.mtplauncher.model.IconCacheDefault;
import com.jzs.dr.mtplauncher.model.IconUtilitiesDefault;
import com.jzs.dr.mtplauncher.model.LauncherProviderBase;
import com.jzs.dr.mtplauncher.model.ResConfigManagerBase;
import com.jzs.dr.mtplauncher.sjar.LauncherApplication;
import com.jzs.dr.mtplauncher.sjar.model.LauncherSettings;
import com.jzs.dr.mtplauncher.sjar.model.SharedPrefSettingsManager;
import com.jzs.dr.mtplauncher.sjar.utils.InstallShortcutHelper;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;

public class LauncherApplicationMain extends LauncherApplication/* implements ILauncherApplication*/{
	private final static String TAG = "LauncherApplicationImpl";
	
	private WeakReference<LauncherProviderBase> mLauncherProvider;

	@Override
    public void onCreate() {
		//Util.Log.w(TAG, "LauncherApplicationMain::onCreate(0)====");
        super.onCreate();
   
        //Util.Log.w(TAG, "LauncherApplicationMain::onCreate(2)====");
        
        // Register for changes to the favorites
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true,
                mFavoritesObserver);
	}
	
	@Override
    public void onTerminate() {
        super.onTerminate();
        
        Util.Log.w(TAG, "LauncherApplicationMain::onTerminate()====");
        
        ContentResolver resolver = getContentResolver();
        resolver.unregisterContentObserver(mFavoritesObserver);
    }
	
	public void setLauncherProvider(LauncherProviderBase provider) {
        mLauncherProvider = new WeakReference<LauncherProviderBase>(provider);
        //Util.Log.w(TAG, "LauncherApplicationMain::setLauncherProvider()====");
        onLauncherProviderChanged();
    }
	
	public ILauncherProvider getLauncherProvider() {
        return mLauncherProvider.get();
    }

	/**
     * Receives notifications whenever the user favorites have changed.
     */
    private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (Util.ENABLE_DEBUG) {
            	Util.Log.d(TAG, "mFavoritesObserver onChange: selfChange = " + selfChange);
            }
            
//            /// M: Ignore the loading database process when is installing shortcut, 
//            ///    trigger it manually later due to the process may have more than one installation.
//            ///    Just decrease the installed shortcut for successful one @{ 
            if (InstallShortcutHelper.isInstallingShortcut()) {
                if (Util.ENABLE_DEBUG) {
                	Util.Log.d(TAG, "mFavoritesObserver onChange: is installing shortcut, so decrease the install count and return");
                }
                InstallShortcutHelper.decreaseInstallingCount(getLauncherHelper(), true);
                return;
            }
//            /// M: }@
//            
//            // If the database has ever changed, then we really need to force a reload of the
//            // workspace on the next load
            if(getLauncherHelper() != null){
            	getLauncherHelper().triggerLoadingDatabaseManually();
            }
        }
    };

	public IIconUtilities createIconUtilities(ISharedPrefSettingsManager pref, IResConfigManager res){
		return IconUtilitiesDefault.getInstance(this, pref, res);
	}
	
	public IIconCache createIconCache(){
		return new IconCacheDefault(this, getLauncherHelper().getIconUtilities());
	}
	
	public ILauncherModel createLauncherModel(){
		return LauncherModelDefault.getInstance(this, getLauncherHelper());
	}
    
    protected LauncherHelper createLauncherPluginEntryDefault(){
    	return new LauncherDefault(getBaseContext(), this);
    }
    
    public ISharedPrefSettingsManager getSharedPrefSettingsManager(){
    	return SharedPrefSettingsManager.getInstance(this);
    }
    
    public IResConfigManager getDefaultResConfigManager(ISharedPrefSettingsManager pref){
    	return ResConfigManagerBase.getInstance(this, pref);
    }
    
    public IGlobalStaticFunc getGlobalStaticFunctions(){
    	return GlobalStaticFunctions.getInstance(this);
    }
    
    public void showAppIconAndTitleCustomActivity(){
    	Intent intent = new Intent();
    	Context activity = getLauncherHelper().getActivity(); 
    	intent.setClass(activity, CustomCropImageActivity.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS|Intent.FLAG_ACTIVITY_NEW_TASK);
    	activity.startActivity(intent);
    }
	
}
