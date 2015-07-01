package com.jzs.dr.mtplauncher.sjar.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.ISharedPrefSettingsManager;

import android.content.Context;
import android.content.SharedPreferences;

//import com.jzs.dr.mtplauncher.R;


public class SharedPrefSettingsManager implements ISharedPrefSettingsManager {
	
	private static final String sSharedPreferencesKey = "com.jzs.dr.mtplauncher.prefs";
	
	public static final String KEY_ALLAPPS_SORT_TYPE = "Key_AllApps_Sorttype";
	public static final int ALLAPPS_SORT_BY_INSTALL_DATE = 0;
	public static final int ALLAPPS_SORT_BY_INSTALL_DATE_DESC = 1;
	public static final int ALLAPPS_SORT_BY_TITLE = 2;
	public static final int ALLAPPS_SORT_BY_LAUNCHE_TIMES = 3;
	public static final int ALLAPPS_SORT_BY_MANUAL = 3;
	public static final int ALLAPPS_SORT_BY_DEFAULT = ALLAPPS_SORT_BY_INSTALL_DATE;
	
	public static final String KEY_ANIMATE_EFFECT_TYPE = "Key_Animate_Effect";
	
	
	private SharedPreferences mPreference = null;
	private Context mContext;
	private IResConfigManager mResConfigManager;
	
	private final static Map<String, String> sCacheInfoMap = new HashMap<String, String>();
	
	private static SharedPrefSettingsManager sSettingsManager;
	public static ISharedPrefSettingsManager getInstance(Context context){
    	if (sSettingsManager == null) {
    		sSettingsManager = new SharedPrefSettingsManager(context);
        }
        return sSettingsManager;
    }
	
	private SharedPrefSettingsManager(Context context){
		mContext = context;
		mPreference = context.getSharedPreferences(sSharedPreferencesKey,
                Context.MODE_WORLD_WRITEABLE|Context.MODE_WORLD_READABLE);
	}
	
	public final void setResConfigManager(IResConfigManager res){
		mResConfigManager = res;
	}
	
	public final void onTrimMemory(int level){
		sCacheInfoMap.clear();
	}
	
	public static String getSharedPreferencesKey() {
        return sSharedPreferencesKey;
    }
	
	public final SharedPreferences getSharedPreferences(){
		return mPreference;
	}
	
	public int getInt(String key, int defResKey, int defValue){
		return getInt(key, mResConfigManager.getInteger(defResKey, defValue));
	}
	
	public int getInt(String key, int defValue){
		int ret = defValue;
		synchronized (sCacheInfoMap) {
			if(sCacheInfoMap.containsKey(key)){
				return Integer.valueOf(sCacheInfoMap.get(key));
			} 
			
			ret = mPreference.getInt(key, defValue);
			sCacheInfoMap.put(key, String.valueOf(ret));
		}
		return ret;
	}
	
	public void setInt(String key, int value){
		synchronized (sCacheInfoMap) {
			sCacheInfoMap.put(key, String.valueOf(value));
		}
		mPreference.edit().putInt(key, value).commit();
	}
	
	public float getFloat(String key, float defValue){
		float ret = defValue;
		synchronized (sCacheInfoMap) {
			if(sCacheInfoMap.containsKey(key)){
				return Float.valueOf(sCacheInfoMap.get(key));
			} 
			
			ret = mPreference.getFloat(key, defValue);
			sCacheInfoMap.put(key, String.valueOf(ret));
		}
		return ret;
	}
	
	public void setFloat(String key, float value){
		synchronized (sCacheInfoMap) {
			sCacheInfoMap.put(key, String.valueOf(value));
		}
		mPreference.edit().putFloat(key, value).commit();
	}
	
	public Boolean getBoolean(String key, int defResKey, Boolean defValue){
		return getBoolean(key, mResConfigManager.getBoolean(defResKey, defValue));
	}
	
	public Boolean getBoolean(String key, Boolean defValue){
		Boolean ret = defValue;
		synchronized (sCacheInfoMap) {
			if(sCacheInfoMap.containsKey(key)){
				return Boolean.valueOf(sCacheInfoMap.get(key));
			} 
			
			ret = mPreference.getBoolean(key, defValue);
			sCacheInfoMap.put(key, String.valueOf(ret));
		}
		return ret;
		//return mPreference.getBoolean(key, defValue);
	}
	
	public void setBoolean(String key, Boolean value){
		synchronized (sCacheInfoMap) {
			sCacheInfoMap.put(key, String.valueOf(value));
		}
		mPreference.edit().putBoolean(key, value).commit();
	}
	
	public String getString(String key, int defResKey, String defValue){
		return getString(key, mResConfigManager.getString(defResKey, defValue));
	}
	
	public String getString(String key, String defValue){
		String ret = defValue;
		synchronized (sCacheInfoMap) {
			if(sCacheInfoMap.containsKey(key)){
				return sCacheInfoMap.get(key);
			} 
	
			ret = mPreference.getString(key, defValue);
			sCacheInfoMap.put(key, ret);
		}
		return ret;
		//return mPreference.getString(key, defValue);
	}
	
	public void setString(String key, String value){
		synchronized (sCacheInfoMap) {
			sCacheInfoMap.put(key, String.valueOf(value));
		}
		mPreference.edit().putString(key, value).commit();
	}
	
	public int getWorkspaceCountCellX(){
		int ret = getWorkspaceCountCellX(-1);
		if(ret < 0)
			ret = mResConfigManager.getInteger(ResConfigManager.CONFIG_WORKSPACE_CELL_COUNTX, 4);
		return ret;
	}
	
	public int getWorkspaceCountCellX(int defValue){
		return getInt(sCountCellXKey, defValue);
	}
	
	public void setWorkspaceCountCellX(int count){
		setInt(sCountCellXKey, count);
	}
	
	public int getWorkspaceCountCellY(){
		int ret = getWorkspaceCountCellY(-1);
		if(ret < 0)
			ret = mResConfigManager.getInteger(ResConfigManager.CONFIG_WORKSPACE_CELL_COUNTY, 4);
		return ret;
	}
	
	public int getWorkspaceCountCellY(int defValue){
		return getInt(sCountCellYKey, defValue);
	}
	
	public void setWorkspaceCountCellY(int count){
		setInt(sCountCellYKey, count);
	}
	
	public int getWorkspaceScreenCount(){
		int ret = getWorkspaceScreenCount(-1);
		if(ret < 0)
			ret = mResConfigManager.getInteger(ResConfigManager.CONFIG_WORKSPACE_SCREEN_COUNT, 5);
		return ret;
	}
	
	public int getWorkspaceScreenCount(int defValue){
		return getInt(sCountScreenKey, defValue);
	}
	
	public void setWorkspaceScreenCount(int count){
		setInt(sCountScreenKey, count);
	}
	
	public int getWorkspaceScreenMaxCount(){
		int ret = getWorkspaceScreenMaxCount(-1);
		if(ret < 0)
			ret = mResConfigManager.getInteger(ResConfigManager.CONFIG_WORKSPACE_MAX_SCREENCOUNT, 7);
		return ret;
	}
	public int getWorkspaceScreenMaxCount(int defValue){
		return getInt(sMaxCountScreenKey, defValue);
	}
	public void setWorkspaceScreenMaxCount(int count){
		setInt(sMaxCountScreenKey, count);
	}
	
	public int getWorkspaceScreenMinCount(){
		int ret = getWorkspaceScreenMinCount(-1);
		if(ret < 0)
			ret = mResConfigManager.getInteger(ResConfigManager.CONFIG_WORKSPACE_MIN_SCREENCOUNT, 3);
		return ret;
	}
	public int getWorkspaceScreenMinCount(int defValue){
		return getInt(sMinCountScreenKey, defValue);
	}
	public void setWorkspaceScreenMinCount(int count){
		setInt(sMinCountScreenKey, count);
	}
	
	public int getWorkspaceDefaultScreen(){
		int ret = getWorkspaceDefaultScreen(-1);
		if(ret < 0)
			ret = mResConfigManager.getInteger(ResConfigManager.CONFIG_WORKSPACE_DEFAULT_SCREEN, -1);
		if(ret < 0)
			ret = getWorkspaceScreenCount()/2;
		return ret;
	}
	
	public int getWorkspaceDefaultScreen(int defValue){
		return getInt(sDefaultScreenKey, defValue);
	}
	
	public void setWorkspaceDefaultScreen(int value){
		setInt(sDefaultScreenKey, value);
	}
	
	public int getAppIconSize(){
		int ret = getAppIconSize(-1);
		if(ret < 0)
			ret = mResConfigManager.getDimensionPixelSize(ResConfigManager.DIM_APP_ICON_SIZE, 48);
		return ret;
	}
	
	public int getAppIconSize(int defValue){
		return getInt(sAppIconSizeKey, defValue);
	}
	
	public void setAppIconSize(int size){
		setInt(sAppIconSizeKey, size);
	}
	
	public int getAllAppsButtonRank(){
		int ret = getAllAppsButtonRank(-2);
		if(ret < ALL_APPS_BTN_RANK_INDEX_NONE){
			ret = mResConfigManager.getInteger(ResConfigManager.CONFIG_HOTSET_ALLAPP_INDEX, 
					ALL_APPS_BTN_RANK_INDEX_NONE);
		}
		return ret;
	}
	public int getAllAppsButtonRank(int defValue){
		return getInt(sAppBtnIndexKey, defValue);
	}
	public void setAllAppsButtonRank(int value){
		setInt(sAppBtnIndexKey, value);
	}
	
	public boolean getEnableStaticWallpaper(){
		return getEnableStaticWallpaper(mResConfigManager.getBoolean(ResConfigManager.CONFIG_ENABLE_STATIC_WALLPAPER));
	}
	public boolean getEnableStaticWallpaper(boolean defValue){
		return getBoolean(sStaticWallpaperKey, defValue);
	}
	public void setEnableStaticWallpaper(boolean enable){
    	setBoolean(sStaticWallpaperKey, enable);
    }
	
	public boolean getWorkspaceSupportCycleSliding(){
		return getWorkspaceSupportCycleSliding(mResConfigManager.getBoolean(ResConfigManager.CONFIG_WORKSPACE_SUPPORT_CYCLYSLIDING));
	}
	public boolean getWorkspaceSupportCycleSliding(boolean defValue){
		return getBoolean(sWorkspaceCycleSlidingKey, defValue);
	}
	public void setWorkspaceSupportCycleSliding(boolean enable){
    	setBoolean(sWorkspaceCycleSlidingKey, enable);
    }
	
	public boolean getAppsSupportCycleSliding(){
		return getAppsSupportCycleSliding(mResConfigManager.getBoolean(ResConfigManager.CONFIG_APPS_SUPPORT_CYCLYSLIDING));
	}
	public boolean getAppsSupportCycleSliding(boolean defValue){
		return getBoolean(sAppsCycleSlidingKey, defValue);
	}
	public void setAppsSupportCycleSliding(boolean enable){
    	setBoolean(sAppsCycleSlidingKey, enable);
    }
	
	public boolean getSlideAppAndWidgetTogether(){
		return getSlideAppAndWidgetTogether(mResConfigManager.getBoolean(ResConfigManager.CONFIG_APPS_WIDGET_SLIDE_TOGETHER));
	}
	public boolean getSlideAppAndWidgetTogether(boolean defValue){
		return getBoolean(sAppsWidgetSlideTogetherKey, defValue);
	}
	public void setSlideAppAndWidgetTogether(boolean enable){
    	setBoolean(sAppsWidgetSlideTogetherKey, enable);
    }
	
	public boolean getWorkspaceEnableScreenIndicatorBar(){
		return getWorkspaceEnableScreenIndicatorBar(mResConfigManager.getBoolean(ResConfigManager.CONFIG_SHOW_BAR_SCREENINDICATOR));
	}
	public boolean getWorkspaceEnableScreenIndicatorBar(boolean defValue){
		return getBoolean(sShowScreenIndicatorBarKey, defValue);
	}
	public void setWorkspaceEnableScreenIndicatorBar(boolean enable){
		setBoolean(sShowScreenIndicatorBarKey, enable);
	}
	
	public boolean getWorkspaceShowHotseatBar(){
		return getWorkspaceShowHotseatBar(mResConfigManager.getBoolean(ResConfigManager.CONFIG_SHOW_BAR_HOTSEAT));
	}
	public boolean getWorkspaceShowHotseatBar(boolean defValue){
		return getBoolean(sShowHotseatBarKey, defValue);
	}
	public void setWorkspaceShowHotseatBar(boolean enable){
		setBoolean(sShowHotseatBarKey, enable);
	}
}
