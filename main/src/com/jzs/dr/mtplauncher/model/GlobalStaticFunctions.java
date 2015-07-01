package com.jzs.dr.mtplauncher.model;

import com.jzs.common.launcher.IGlobalStaticFunc;
import com.jzs.dr.mtplauncher.LauncherApplicationMain;
import com.jzs.dr.mtplauncher.ctrl.InstallShortcutReceiver;
import com.jzs.dr.mtplauncher.ctrl.UninstallShortcutReceiver;

public class GlobalStaticFunctions implements IGlobalStaticFunc {
	private final LauncherApplicationMain mApp;
	
	private static IGlobalStaticFunc sInstance;
	public static IGlobalStaticFunc getInstance(LauncherApplicationMain app){
    	if (sInstance == null) {
    		sInstance = new GlobalStaticFunctions(app);
        }
        return sInstance;
    }
	
	private GlobalStaticFunctions(LauncherApplicationMain app){
		mApp = app;
	}
	
	public void enableInstallQueue(){
		InstallShortcutReceiver.enableInstallQueue();
	}
	
	public void disableAndFlushInstallQueue() {
		InstallShortcutReceiver.disableAndFlushInstallQueue(mApp);
	}
	
	public void flushInstallQueue(){
		InstallShortcutReceiver.flushInstallQueue(mApp);
	}
	
	public void ReleaseInstance(){
		
	}
	
	public void onTrimMemory(int level) {
		
	}
	
	public void enableUninstallQueue(){
		UninstallShortcutReceiver.enableUninstallQueue();
	}
	
	public void disableAndFlushUninstallQueue(){
		UninstallShortcutReceiver.disableAndFlushUninstallQueue(mApp);
	}
	
}
