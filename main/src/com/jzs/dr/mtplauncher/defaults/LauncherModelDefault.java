package com.jzs.dr.mtplauncher.defaults;

import com.jzs.common.launcher.LauncherHelper;
import com.jzs.dr.mtplauncher.sjar.model.IconCache;
import com.jzs.dr.mtplauncher.sjar.model.LauncherModel;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;

import com.jzs.dr.mtplauncher.R;

public class LauncherModelDefault extends LauncherModel {
	private final static String TAG = "LauncherModel";
	
	//private Context mContext;
	private static LauncherModel sLauncherModel;
    
    public static LauncherModel getInstance(Context context, LauncherHelper launcher){
    	if (sLauncherModel == null) {
    		sLauncherModel = new LauncherModelDefault(context, launcher);
        }
        return sLauncherModel;
    }
    
	public LauncherModelDefault(Context context, LauncherHelper launcher){
		super(context, launcher);
	}
	
	protected void initialise(){
		super.initialise();
		
		final Resources res =  getContext().getResources();
        mAllAppsLoadDelay = res.getInteger(R.integer.config_allAppsBatchLoadDelay);
        mBatchSize = res.getInteger(R.integer.config_allAppsBatchSize);
		// Register intent receivers
//        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
//        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
//        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
//        filter.addDataScheme("package");
//        mContext.registerReceiver(this, filter);
//        filter = new IntentFilter();
//        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
//        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
//        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
//        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
//        mContext.registerReceiver(this, filter);
//        filter = new IntentFilter();
//        filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
//        mContext.registerReceiver(this, filter);
//        filter = new IntentFilter();
//        filter.addAction(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED);
//        mContext.registerReceiver(this, filter);
	}
	
	public void ReleaseInstance(){
		super.ReleaseInstance();
//		mContext.unregisterReceiver(this);
		sLauncherModel = null;
	}
	
//	@Override
//    public void onReceive(Context context, Intent intent) {
//		if(Util.DEBUG_LOADERS)
//			Util.Log.d(TAG, "onReceive intent=" + intent);
//
//        final String action = intent.getAction();
//	}
}
