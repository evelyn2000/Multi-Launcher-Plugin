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

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import java.util.HashMap;

import com.jzs.common.launcher.IIconCache;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.IconCacheEntry;
import com.jzs.common.manager.IAppsManager;
import com.jzs.common.manager.IIconUtilities;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public class IconCache implements IIconCache{
    @SuppressWarnings("unused")
    protected static final String TAG = "IconCache";

    protected static final int INITIAL_ICON_CACHE_CAPACITY = 50;

//    public static class CacheEntry {
//        public Bitmap icon;
//        public String title;
//    }

    protected final Bitmap mDefaultIcon;
    protected final Context mContext;
    protected final PackageManager mPackageManager;
    protected final HashMap<ComponentName, IconCacheEntry> mCache =
            new HashMap<ComponentName, IconCacheEntry>(INITIAL_ICON_CACHE_CAPACITY);
    protected int mIconDpi;
    protected final IIconUtilities mIconUtilities;
    protected final IAppsManager mAppsManager;

    public IconCache(Context context, IIconUtilities iconUtilities) {
    	mIconUtilities = iconUtilities;
    	mContext = context;//launcherEntry.getContext();
    	
    	if(context == null || iconUtilities == null){
			throw new RuntimeException("Jzs.IconCache context and iconUtilities cat't be null..");
		}
    	mAppsManager = (IAppsManager)mContext.getSystemService(IAppsManager.MANAGER_SERVICE);
    	
//        ActivityManager activityManager =
//                (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

        mPackageManager = mContext.getPackageManager();
        //mIconDpi = activityManager.getLauncherLargeIconDensity();

        // need to set mIconDpi before getting default icon
        mDefaultIcon = makeDefaultIcon();
    }
    
    public IIconUtilities getIconUtilities(){
    	return mIconUtilities;
    }
    
    protected int getIconDpi(){
    	return mIconDpi;
    }
    
    protected Context getContext(){
    	return mContext;
    }
    
    protected PackageManager getPackageManager(){
    	return mPackageManager;
    }
    
    public final Bitmap getDefaultIcon(){
        return mDefaultIcon;
    }

    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(),
                android.R.drawable.sym_def_app_icon);
    }

    public Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d = null;
        if(resources != null){
            try {
                d = resources.getDrawable(iconId);//getDrawableForDensity(iconId, mIconDpi);
            } catch (Resources.NotFoundException e) {
                d = null;
            }
        }

        return d;//(d != null) ? d : getFullResDefaultActivityIcon();
    }
    
    public CharSequence getTitleResString(Resources resources, int labelRes) {
        if(resources != null && labelRes != 0){
            try {
                return resources.getText(labelRes);//getDrawableForDensity(iconId, mIconDpi);
            } catch (Resources.NotFoundException e) {
            }
        }

        return null;
    }
    
    public CharSequence getTitleResString(String packageName, int labelRes) {
        if(labelRes != 0)
            return mPackageManager.getText(packageName, labelRes, null);
        return null;
    }

    public Drawable getFullResIcon(String packageName, int iconId) {
        return mPackageManager.getDrawable(packageName, iconId, null);
//        Resources resources = null;
//        try {
//            resources = mPackageManager.getResourcesForApplication(packageName);            
//        } catch (PackageManager.NameNotFoundException e) {
//            resources = null;
//        }
//        if (resources != null) {
//            if (iconId != 0) {
//                return getFullResIcon(resources, iconId);
//            }
//        }
//        if(dr == null)
//            dr = getFullResDefaultActivityIcon();
//        return dr;
    }

    public Drawable getFullResIcon(ComponentInfo info) {
        final Drawable dr = info.loadIcon(mPackageManager); 
//        if(dr != null){
//            android.util.Log.i("QsLog", "createIconBitmap getFullResIcon()===w:"+dr.getIntrinsicWidth()
//                    +"==h:"+dr.getIntrinsicHeight()
//                    +"=componentName:"+info.name);
//        }
        return (dr != null ? dr : getFullResDefaultActivityIcon());
    }
    
    public Drawable getFullResIcon(ResolveInfo info) {
        return info != null ? getFullResIcon(info.activityInfo) : getFullResDefaultActivityIcon();
    }

    protected Bitmap makeDefaultIcon() {
        Drawable d = getFullResDefaultActivityIcon();
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
                Math.max(d.getIntrinsicHeight(), 1),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return b;
    }
    
    public void dumpCacheEntry(){
        synchronized (mCache) {
            for (IconCacheEntry e : mCache.values()) {
                if(e.icon != null){
                    Util.Log.i(TAG, "icon : ["+e.icon.getWidth()+","+e.icon.getHeight()
                            +", density="+e.icon.getDensity()
                            +", ByteCount="+e.icon.getByteCount()
                            +", config="+e.icon.getConfig()
                            +"], title="+e.title);
                } else {
                    Util.Log.i(TAG, "icon : [null], title="+e.title);
                }
            }
        }
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    public void remove(ComponentName componentName) {
        synchronized (mCache) {
            mCache.remove(componentName);
        }
    }

    /**
     * Empty out the cache.
     */
    public void flush() {
        synchronized (mCache) {
        	for (ComponentName cn : mCache.keySet()) {
        		IconCacheEntry e = mCache.get(cn);
                e.icon = null;
                e.title = null;
                e = null;
            }
            mCache.clear();
        }

        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "Flush icon cache here.");
        }
    }
    
    public void add(ComponentName componentName, IconCacheEntry entry){
    	synchronized (mCache) {
    		mCache.put(componentName, entry);
        }
    }

    /**
     * Fill in "application" with the icon and label for "info."
     */
    public IconCacheEntry getTitleAndIcon(ComponentName componentName) {
        synchronized (mCache) {
            if (componentName == null) {
                return null;
            }
            
            return cacheLockedInternal(componentName, 0, 0);
        }
    }
    public IconCacheEntry getTitleAndIcon(ComponentName componentName, ComponentInfo info) {
        synchronized (mCache) {
            return cacheLockedInternal(componentName, info, null);
        }
    }
    
    public IconCacheEntry getTitleAndIcon(ComponentName componentName, ResolveInfo info,
            HashMap<Object, CharSequence> labelCache) {
    	synchronized (mCache) {
    		return cacheLocked(componentName, info, labelCache);
        }
    }
    
    public void getTitleAndIcon(ApplicationInfo application, ResolveInfo info,
            HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
        	IconCacheEntry entry = cacheLocked(application.componentName, info, labelCache);

            application.title = entry.title;
            application.iconBitmap = entry.icon;
        }
    }
    
    public Bitmap getIcon(ComponentName component) {
        synchronized (mCache) {
            if (component == null) {
                return null;
            }
            
            IconCacheEntry entry = cacheLockedInternal(component, 0, 0);
            return entry.icon;
        }
    }
    
    public Bitmap getIcon(ComponentName component, int iconRes){
    	synchronized (mCache) {
            
    	    if (component == null) {
                return null;
            }
    	    IconCacheEntry entry = cacheLockedInternal(component, iconRes, 0);
            return entry.icon;
        }
    }

    public Bitmap getIcon(Intent intent) {
        synchronized (mCache) {
            final ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            ComponentName component = intent.getComponent();
            if(Util.DEBUG_LOADERS){
                Util.Log.w(TAG, "IconCache::getIcon(0) =="+intent);
            }
            if (resolveInfo == null || component == null) {
                android.util.Log.w("QsLog", "IconCache::getIcon fail=="+intent);
                if(Util.DEBUG_LOADERS){
                    Util.Log.w(TAG, "IconCache::getIcon fail=="+intent);
                }
//                if (componentName != null && info != null) {
//                    if (info.applicationInfo != null
//                            && (info.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0
//                            && isPackageUnavailable(componentName.getPackageName())) {
//
//                        return createIconBitmap(true, Resources.getSystem().getDrawable(
//                                com.android.internal.R.drawable.sym_app_on_sd_unavailable_icon));
//                    }
//                }
                return mDefaultIcon;
            }

            IconCacheEntry entry = cacheLocked(component, resolveInfo, null);
            if(entry.icon == null)
                entry.icon = mDefaultIcon;
            return entry.icon;
        }
    }

    public Bitmap getIcon(ComponentName component, ResolveInfo resolveInfo,
            HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
            if (component == null) {
                return null;
            }

            IconCacheEntry entry = cacheLocked(component, resolveInfo, labelCache);
            return entry.icon;
        }
    }
    
    public String getTitle(ComponentName component, int labelRes){
        synchronized (mCache) {
            
            if (component == null) {
                return null;
            }
            IconCacheEntry entry = cacheLockedInternal(component, 0, labelRes);
            return entry.title;
        }
    }
    
    public String getTitle(ComponentName component, ComponentInfo info){
        synchronized (mCache) {
            if (component == null) {
                return null;
            }

            IconCacheEntry entry = cacheLockedInternal(component, info, null);
            return entry.title;
        }
    }
    
    public String getTitle(ComponentName component, ResolveInfo resolveInfo,
            HashMap<Object, CharSequence> labelCache){
        synchronized (mCache) {
            if (component == null) {
                return null;
            }

            IconCacheEntry entry = cacheLocked(component, resolveInfo, labelCache);
            return entry.title;
        }
    }

    public boolean isDefaultIcon(Bitmap icon) {
        return mDefaultIcon == icon;
    }
    
    protected IconCacheEntry cacheLocked(ComponentName componentName, ResolveInfo info,
            HashMap<Object, CharSequence> labelCache) {
        if(info != null)
            return cacheLockedInternal(componentName, (info.activityInfo == null ? info.serviceInfo : info.activityInfo), labelCache);
        
        return cacheLockedInternal(componentName, null, labelCache);
    }
    
    protected IconCacheEntry cacheLockedInternal(ComponentName componentName, int iconRes, int labelRes) {
        IconCacheEntry entry = mCache.get(componentName);
        if (entry == null) {
            entry = new IconCacheEntry();
            mCache.put(componentName, entry);
        }
        
        if(TextUtils.isEmpty(entry.title)){
            entry.title = createLabelString(componentName, labelRes);
        }

        if(entry.icon == null/* || isDefaultIcon(entry.icon)*/){
            entry.icon = createIconBitmap(componentName, iconRes);
        }
        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "cacheLockedInternal: componentName = " + componentName
                    + ", iconRes = " + Integer.toHexString(iconRes) + ", labelRes = "
                    +  Integer.toHexString(labelRes)
                    + ", title = " + entry.title
                    + ", isDefaultIcon = " + isDefaultIcon(entry.icon)
                    );
        }
        return entry;
    }

    protected IconCacheEntry cacheLockedInternal(ComponentName componentName, ComponentInfo info,
            HashMap<Object, CharSequence> labelCache) {
        if (Util.DEBUG_LOADERS) {
        	Util.Log.d(TAG, "cacheLockedInternal: componentName = " + componentName
                    + ", info = " + info + ", HashMap<Object, CharSequence>:size = "
                    +  ((labelCache == null) ? "null" : labelCache.size()));
        }

        IconCacheEntry entry = mCache.get(componentName);
        if (entry == null/* && info != null*/) {
            entry = new IconCacheEntry();

            mCache.put(componentName, entry);

//            final ComponentName key = info != null ? (new ComponentName(info.packageName, info.name)) : null;
//            if (labelCache != null && key != null && labelCache.containsKey(key)) {
//                entry.title = labelCache.get(key).toString();
//                if (Util.DEBUG_LOADERS) {
//                	Util.Log.d(TAG, "CacheLocked get title from cache: title = " + entry.title);
//                }
//            } else {
//                entry.title = createLabelString(componentName, info);//info.loadLabel(mPackageManager).toString();
//                if (Util.DEBUG_LOADERS) {
//                	Util.Log.d(TAG, "CacheLocked get title from pms: title = " + entry.title);
//                }                
//                if (labelCache != null && key != null) {
//                    labelCache.put(key, entry.title);
//                }
//            }
//            if (entry.title == null && info != null) {
//                entry.title = info.name;
//                if (Util.DEBUG_LOADERS) {
//                	Util.Log.d(TAG, "CacheLocked get title from activity information: entry.title = " + entry.title);
//                }
//            }
//
//            entry.icon = createIconBitmap(componentName, info);
        } 
        
        if(TextUtils.isEmpty(entry.title)){
            entry.title = createLabelString(componentName, info);
            if (entry.title == null && info != null) {
                entry.title = info.name;
            }
        }
        
        if(entry.icon == null/* || isDefaultIcon(entry.icon)*/){
            entry.icon = createIconBitmap(componentName, info);
        }
        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "cacheLockedInternal: componentName = " + componentName
                    + ", title = " + entry.title
                    + ", isDefaultIcon = " + isDefaultIcon(entry.icon)
                    + ", info = " + info
                    );
        }
        return entry;
    }
    
    protected Bitmap createIconBitmap(String packageName, String className, int iconResId) {
//        if (Util.DEBUG_LOADERS) {
//            Util.Log.d(TAG, "cacheLocked: createIconBitmap = " + packageName
//                    + ", className = " + className + ", iconResId = "+Integer.toHexString(iconResId));
//        }
        
        if (mAppsManager != null && !TextUtils.isEmpty(packageName) && (iconResId != 0 || !TextUtils.isEmpty(className))){

            Drawable icon = mAppsManager.getIconDrawable(packageName, className, iconResId);
            if (icon != null){
                return mIconUtilities.createIconBitmap(icon);
            }
            
            if(iconResId != 0){
                boolean systemApps = isSystemApp(packageName);
                Bitmap bmp = createIconBitmap(systemApps, getFullResIcon(packageName, iconResId));
                if(!systemApps && bmp != null)
                    mAppsManager.setDefaultIcon(packageName, className, iconResId, bmp);
                return bmp;
            }
        } else if(iconResId != 0){
            return createIconBitmap(packageName, getFullResIcon(packageName, iconResId));
        }
        return null;
    }
    
    public Bitmap createIconBitmap(String packageName, Resources resources, int iconResId) {
//        if (Util.DEBUG_LOADERS) {
//            Util.Log.d(TAG, "cacheLocked: createIconBitmap = " + packageName
//                    + ", iconResId = "+Integer.toHexString(iconResId));
//        }
        if(resources != null && iconResId != 0)
            return createIconBitmap(packageName, getFullResIcon(resources, iconResId));
        return null;
    }
    
    public Bitmap createIconBitmap(ComponentName componentName, int iconResId) {
//        if (Util.DEBUG_LOADERS) {
//            Util.Log.d(TAG, "cacheLocked: createIconBitmap = " + componentName
//                    + ", iconResId = "+Integer.toHexString(iconResId));
//        }
        if (componentName != null){
            return createIconBitmap(componentName.getPackageName(), componentName.getClassName(), 
                    iconResId);
        }
        return null;
    }

    public Bitmap createIconBitmap(ComponentName componentName, ComponentInfo info){
//        if (Util.DEBUG_LOADERS) {
//            Util.Log.d(TAG, "cacheLocked: createIconBitmap = " + componentName
//                    + ", info = " + info);
//        }
        
        if (mAppsManager != null && componentName != null) {
            
            Drawable icon = info != null ? mAppsManager.getIconDrawable(info) : mAppsManager.getIconDrawable(componentName);
            //Bitmap icon = info != null ? mAppsManager.getIconDrawable(info) : mAppsManager.getIconDrawable(componentName);
            if (icon != null){
                return mIconUtilities.createIconBitmap(icon);
            }
            
            if(info != null){
                Drawable dr = info.loadIcon(mPackageManager);
                if(dr != null){
                    boolean systemApps = isSystemApp(componentName.getPackageName());
                    Bitmap bmp = createIconBitmap(systemApps, dr);
                    if(!systemApps && bmp != null)
                        mAppsManager.setDefaultIcon(info, bmp);
                    return bmp;
                }
            }
            
        } else if(info != null){
            Drawable dr = info.loadIcon(mPackageManager);
            if(dr != null)
                return createIconBitmap(componentName == null ? null : componentName.getPackageName(), dr);
        }
//        android.util.Log.w("QsLog", "IconCache::createIconBitmap fail==cmp:"+componentName+", cmpinfo:"+info);
//        if(componentName != null && info != null){
//            if(info.applicationInfo != null 
//                && (info.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0
//                && isPackageUnavailable(componentName.getPackageName()) ){
//                
//                return createIconBitmap(true, Resources.getSystem().getDrawable(
//                        com.android.internal.R.drawable.sym_app_on_sd_unavailable_icon));
//            }
//        }
    	return mDefaultIcon;
    }
    
    public Bitmap createIconBitmap(String packageName, Bitmap orgIcon) {     
//        if (Util.DEBUG_LOADERS) {
//            Util.Log.d(TAG, "cacheLocked: createIconBitmap = packageName :" + packageName);
//        }
        return createIconBitmap(isSystemApp(packageName), orgIcon);
    }
    
    public Bitmap createIconBitmap(String packageName, Drawable orgIcon) {
//        if (Util.DEBUG_LOADERS) {
//            Util.Log.d(TAG, "cacheLocked: createIconBitmap = packageName :" + packageName);
//        }
        return createIconBitmap(isSystemApp(packageName), orgIcon);
    }
    
    protected Bitmap createIconBitmap(boolean isSystemApp, Bitmap orgIcon) {     
//        if (Util.DEBUG_LOADERS) {
//            Util.Log.d(TAG, "cacheLocked: createIconBitmap = isSystemApp :" + isSystemApp);
//        }
        if(orgIcon != null)
            return mIconUtilities.createIconBitmap(orgIcon);
        return null;
    }
    
    protected Bitmap createIconBitmap(boolean isSystemApp, Drawable orgIcon) {
//        if (Util.DEBUG_LOADERS) {
//            Util.Log.d(TAG, "cacheLocked: createIconBitmap = isSystemApp :" + isSystemApp);
//        }
        if(orgIcon != null)
            return mIconUtilities.createIconBitmap(orgIcon);
        return null;
    }
    
    public String createLabelString(ComponentName componentName, int labelRes){
//        if (Util.DEBUG_LOADERS) {
//            Util.Log.d(TAG, "cacheLocked: createLabelString = " + componentName
//                    + ", labelRes = " + labelRes);
//        }
        if(componentName != null){
            if (mAppsManager != null) {
                String str = mAppsManager.getTitle(componentName.getPackageName(), 
                        componentName.getClassName(), labelRes);
                if (!TextUtils.isEmpty(str)){
                    return str;
                }
            }
            
            if(labelRes != 0){
                CharSequence text = getTitleResString(componentName.getPackageName(), labelRes);
                return (text != null ? text.toString() : null);
            }
        }
        
        return null;
    }
    
    public String createLabelString(ComponentName componentName, ComponentInfo info){
//        android.util.Log.d("QsLog", "createLabelString===packageName:"+componentName
//                +"==labelRes:"+(info != null ? Integer.toHexString(info.labelRes) : "null")
//                +"==name:"+(info != null ? info.name : "null"));
//        if (Util.DEBUG_LOADERS) {
//            Util.Log.d(TAG, "cacheLocked: createLabelString = " + componentName
//                    + ", info = " + info);
//        }
        if (mAppsManager != null) {

            String str = info != null ? mAppsManager.getTitle(info) : mAppsManager.getTitle(componentName);
            if (!TextUtils.isEmpty(str)){
                return str;
            }
        }
    	return info != null ? info.loadLabel(mPackageManager).toString() : null;
    }
    
    protected boolean isSystemApp(String packageName){
        if(packageName != null){
            try {
                return isSystemApp(mPackageManager.getApplicationInfo(packageName, 0).flags);
            } catch (NameNotFoundException e) {
                //android.util.Log.d("ApplicationInfo", "PackageManager.getApplicationInfo failed for " + packageName);
                return false;
            }
        }
        
        return true;
    }
    
    protected boolean isSystemApp(int appFlags){
        if (((appFlags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) 
                || ((appFlags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)) {
            return true;
        } 
        return false;
    }
    
    public HashMap<ComponentName,Bitmap> getAllIcons() {
        synchronized (mCache) {
            HashMap<ComponentName,Bitmap> set = new HashMap<ComponentName,Bitmap>();
            for (ComponentName cn : mCache.keySet()) {
                final IconCacheEntry e = mCache.get(cn);
                set.put(cn, e.icon);
            }
            return set;
        }
    }
    
    protected boolean isPackageUnavailable(String packageName) {
        try {
            return mPackageManager.getPackageInfo(packageName, 0) == null;
        } catch (NameNotFoundException ex) {
            return true;
        }
    }
}
