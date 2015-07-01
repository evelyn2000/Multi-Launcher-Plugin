package com.jzs.dr.mtplauncher.exts;

import android.content.Context;
import android.content.res.XmlResourceParser;

import com.jzs.dr.mtplauncher.sjar.exthelpers.ILauncherProviderExt;

public class LauncherProviderExt implements ILauncherProviderExt {

	public LauncherProviderExt(Context context){
		
	}
	
	public void ReleaseInstance(){
		
	}
	
	public int getAllAppsButtonRank(){
		return ALL_APPS_BTN_RANK_INDEX_DEFAULT;
	}
	
	public XmlResourceParser getDefaultWorkspaceResource(){
		return null;
	}
	
	public String CreateDbTableExtString(){
		return "";
	}
	
	public void onTrimMemory(int level) {
	}
}
