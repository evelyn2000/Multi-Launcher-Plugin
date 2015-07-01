package com.jzs.dr.mtplauncher.sjar;

import java.lang.ref.WeakReference;

import com.jzs.common.launcher.IIconCache;
import com.jzs.common.launcher.ILauncherApplication;
import com.jzs.common.launcher.ILauncherModel;
import com.jzs.common.launcher.ILauncherProvider;
import com.jzs.common.launcher.LauncherHelper;
import com.jzs.common.os.JzsSystemProperties;
import com.jzs.common.plugin.IPlugin;
import com.jzs.common.plugin.IPluginManager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

public abstract class LauncherApplication extends Application implements ILauncherApplication {
	protected final static String TAG = "LauncherApplication";
	private WeakReference<LauncherHelper> mLauncherHelper;
	
	@Override
    public void onCreate() {
        super.onCreate();

        if(mLauncherHelper == null || mLauncherHelper.get() == null){
        	initLauncherPluginEntry();
        }
	}
	
	@Override
    public void onTerminate() {
        super.onTerminate();

        if(mLauncherHelper != null){
        	mLauncherHelper.get().ReleaseInstance();
        	mLauncherHelper.clear();
        }
	}
	
	@Override
	public void onLowMemory() {
		// TODO Auto-generated method stub
		super.onLowMemory();
		if(getLauncherHelper() != null){
			getLauncherHelper().onTrimMemory(0);
        }
	}

	@Override
	public void onTrimMemory(int level) {
		// TODO Auto-generated method stub
		super.onTrimMemory(level);
		if(getLauncherHelper() != null){
			getLauncherHelper().onTrimMemory(level);
        }
	}

    public LauncherHelper setLauncherActivity(Activity launcherActivity){
    	//android.util.Log.e("QsLog", "LauncherApplication::setLauncherActivity()===");
    	
    	LauncherHelper entry = mLauncherHelper.get();
    	entry.setLauncherActivity(launcherActivity);
    	//entry.getLauncherModel().setCallbacks(launcher);
    	return entry;
    }

	protected final void onLauncherProviderChanged() {
		//android.util.Log.e("QsLog", "LauncherApplication::onLauncherProviderChanged()===");		
        if(getLauncherHelper() == null){
        	initLauncherPluginEntry();
        }
    }
	
	public ILauncherModel getModel(){
		return getLauncherHelper().getModel();
	}
	
	public IIconCache getIconCache(){
		return getLauncherHelper().getIconCache();
	}
	
	public LauncherHelper getLauncherHelper(){
		if(mLauncherHelper == null)
			return null;
		
    	return mLauncherHelper.get();
    }
    
    private LauncherHelper initLauncherPluginEntry(){
    	LauncherHelper pluginEntry = null;// = createLauncherPluginEntryDefault();
    	if(JzsSystemProperties.checkPermission()){
	    	IPluginManager pluginMgr = (IPluginManager)getSystemService(IPluginManager.PLUGIN_MANAGER_SERVICE);
			if(pluginMgr != null){
				IPlugin plugin = pluginMgr.getDefaultPlugin("com.jzs.dr.mtplauncher.JzsSupportLauncher", IPluginManager.PLUGIN_TYPE_STYLE);
				if(plugin != null){
					//android.util.Log.e("QsLog", "initLauncherPluginEntry()==get plugin success==");
					final Context targetContext = plugin.getTargetContext(this);
					if(targetContext != null){					
						pluginEntry = plugin.createInstanceWithTargetParameter(this, targetContext, this);
						if(pluginEntry == null){
							android.util.Log.e("QsLog", "initLauncherPluginEntry()==create plugin fail=="+plugin.toString());
						}
					} else {
						//android.util.Log.e("QsLog", "initLauncherPluginEntry()==get plugin context fail==");
					}
				} else {
					android.util.Log.e("QsLog", "initLauncherPluginEntry()==get plugin fail==");
					//pluginMgr.dumpPlugins();
				}
			} else {
				android.util.Log.e("QsLog", "initLauncherPluginEntry()==get plugin manager fail==");
			}
    	}
		
		if(pluginEntry == null)
			pluginEntry = createLauncherPluginEntryDefault();
		
		pluginEntry.initialise(this);

		mLauncherHelper = new WeakReference<LauncherHelper>(pluginEntry);
    	return pluginEntry;
    }
    
    
    public abstract ILauncherProvider getLauncherProvider();
    protected abstract LauncherHelper createLauncherPluginEntryDefault();
    public abstract void showAppIconAndTitleCustomActivity();
    //public abstract IIconUtilities createIconUtilities(ISharedPrefSettingsManager pref, IResConfigManager res);
//    public abstract IconCache createIconCache();
//    public abstract LauncherModel createLauncherModel();
//    public abstract ISharedPrefSettingsManager getSharedPrefSettingsManager();
//    public abstract IResConfigManager getResConfigManager(ISharedPrefSettingsManager pref);
//    public abstract IInstallShortcutReceiver getInstallShortcutReceiver();
}
