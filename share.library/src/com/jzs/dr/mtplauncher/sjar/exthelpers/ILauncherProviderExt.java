package com.jzs.dr.mtplauncher.sjar.exthelpers;

import com.jzs.common.launcher.IExtHelperBase;

import android.content.res.XmlResourceParser;

public interface ILauncherProviderExt extends IExtHelperBase{

	public final static int ALL_APPS_BTN_RANK_INDEX_DEFAULT = -1;
	public final static int ALL_APPS_BTN_RANK_INDEX_NONE = -2;
	int getAllAppsButtonRank();
	XmlResourceParser getDefaultWorkspaceResource();
	
	String CreateDbTableExtString();
	
}
