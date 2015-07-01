package com.jzs.dr.mtplauncher.sjar.model;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.jzs.common.launcher.IIconCache;
import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.IconCacheEntry;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;
import com.jzs.dr.mtplauncher.sjar.R;

/**
 * Stores the list of all applications for the all apps view.
 */
public class AllAppsList {
    private final static String TAG = "AllAppsList";
    public static final int DEFAULT_APPLICATIONS_NUMBER = 42;
    
    /** The list off all apps. */
    public ArrayList<ApplicationInfo> data =
            new ArrayList<ApplicationInfo>(DEFAULT_APPLICATIONS_NUMBER);
    /** The list of apps that have been added since the last notify() call. */
    public ArrayList<ApplicationInfo> added =
            new ArrayList<ApplicationInfo>(DEFAULT_APPLICATIONS_NUMBER);
    /** The list of apps that have been removed since the last notify() call. */
    public ArrayList<ApplicationInfo> removed = new ArrayList<ApplicationInfo>();
    /** The list of apps that have been modified since the last notify() call. */
    public ArrayList<ApplicationInfo> modified = new ArrayList<ApplicationInfo>();

    /**
     * M: The list of appWidget that have been removed since the last notify()
     * call.
     */
    public ArrayList<String> appwidgetRemoved = new ArrayList<String>();
    private IIconCache mIconCache;

    /**
     * Boring constructor.
     */
    public AllAppsList(IIconCache iconCache) {
        mIconCache = iconCache;
    }

    /**
     * Add the supplied ApplicationInfo objects to the list, and enqueue it into the
     * list to broadcast when notify() is called.
     *
     * If the app is already in the list, doesn't add it.
     */
    public void add(ApplicationInfo info) {
        add(info, false);
    }
    public void add(ApplicationInfo info, boolean existthenupdate) {
        if (findActivity(data, info.componentName)) {
            if (Util.DEBUG_LOADERS) {
                Util.Log.e(TAG, "addPackage existing info:"+info);
            }
            
            if(existthenupdate)
                modified.add(info);
            
            return;
        }
        data.add(info);
        added.add(info);
    }
    
    public void clear() {
        data.clear();
        // TODO: do we clear these too?
        added.clear();
        removed.clear();
        modified.clear();
        /// M: clear appWidgetRemoved.
        appwidgetRemoved.clear();
    }

    public int size() {
        return data.size();
    }

    public ApplicationInfo get(int index) {
        return data.get(index);
    }

    public void addPackage(Context context, String packageName) {
        addPackage(context, packageName, false);
    }
    /**
     * Add the icons for the supplied apk called packageName.
     */
    public void addPackage(Context context, String packageName, boolean existthenupdate) {
        final List<ResolveInfo> matches = findActivitiesForPackage(context, packageName);

        if (matches.size() > 0) {
            for (ResolveInfo info : matches) {
                add(new ApplicationInfo(context.getPackageManager(), info, mIconCache, null), existthenupdate);
            }
        } else if (Util.DEBUG_LOADERS) {
            Util.Log.w(TAG, "addPackage can't find activity=packageName:"+packageName);
        }
    }

    /**
     * Remove the apps for the given apk identified by packageName.
     */
    public void removePackage(String packageName) {
        final List<ApplicationInfo> data = this.data;
        for (int i = data.size() - 1; i >= 0; i--) {
            ApplicationInfo info = data.get(i);
            final ComponentName component = info.intent.getComponent();
            if (packageName.equals(component.getPackageName())) {
                removed.add(info);
                data.remove(i);
            }
        }
        // This is more aggressive than it needs to be.
        mIconCache.flush();
    }
    
    public boolean removeSpecificApp(final String packageName, final String className) {
        ApplicationInfo appInfo = null;
        for (ApplicationInfo ai : added) {
            if (ai.componentName.getPackageName().equalsIgnoreCase(packageName)
                    && ai.componentName.getClassName().equalsIgnoreCase(className)) {
                appInfo = ai;
                break;
            }
        }

        if (appInfo != null) {
            data.remove(appInfo);
            added.remove(appInfo);
            return true;
        }
        return false;
    }

    /**
     * Add and remove icons for this package which has been updated.
     */
    public void updatePackage(Context context, String packageName) {
        final List<ResolveInfo> matches = findActivitiesForPackage(context, packageName);
        if (matches.size() > 0) {
            // Find disabled/removed activities and remove them from data and add them
            // to the removed list.
            for (int i = data.size() - 1; i >= 0; i--) {
                final ApplicationInfo applicationInfo = data.get(i);
                final ComponentName component = applicationInfo.intent.getComponent();
                if (packageName.equals(component.getPackageName())) {
                    if (!findActivity(matches, component)) {
                        removed.add(applicationInfo);
                        mIconCache.remove(component);
                        data.remove(i);
                    }
                }
            }

            // Find enabled activities and add them to the adapter
            // Also updates existing activities with new labels/icons
            int count = matches.size();
            for (int i = 0; i < count; i++) {
                final ResolveInfo info = matches.get(i);
                ApplicationInfo applicationInfo = findApplicationInfoLocked(
                        info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name);
                if (applicationInfo == null) {
                    add(new ApplicationInfo(context.getPackageManager(), info, mIconCache, null));
                } else {
                    mIconCache.remove(applicationInfo.componentName);
                    mIconCache.getTitleAndIcon(applicationInfo, info, null);
//                    IconCacheEntry iconEntry = mIconCache.getTitleAndIcon(applicationInfo.componentName, info, null);
//                    if(iconEntry != null){
//                    	applicationInfo.title = iconEntry.title;
//                    	applicationInfo.iconBitmap = iconEntry.icon;
//                    }
                    
                    modified.add(applicationInfo);
                }
            }
        } else {
            // Remove all data for this package.
            for (int i = data.size() - 1; i >= 0; i--) {
                final ApplicationInfo applicationInfo = data.get(i);
                final ComponentName component = applicationInfo.intent.getComponent();
                if (packageName.equals(component.getPackageName())) {
                    removed.add(applicationInfo);
                    mIconCache.remove(component);
                    data.remove(i);
                }
                /// M: only appWidget, if removed ,place in appWidgetRemoved.
                if (removed.size() == 0) {
                    appwidgetRemoved.add(packageName);
                }
            }
        }
    }

    /**
     * Query the package manager for MAIN/LAUNCHER activities in the supplied package.
     */
    private static List<ResolveInfo> findActivitiesForPackage(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);

        final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        return apps != null ? apps : new ArrayList<ResolveInfo>();
    }

    /**
     * Returns whether <em>apps</em> contains <em>component</em>.
     */
    private static boolean findActivity(List<ResolveInfo> apps, ComponentName component) {
        final String className = component.getClassName();
        for (ResolveInfo info : apps) {
            final ActivityInfo activityInfo = info.activityInfo;
            if (activityInfo.name.equals(className)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether <em>apps</em> contains <em>component</em>.
     */
    private static boolean findActivity(ArrayList<ApplicationInfo> apps, ComponentName component) {
        final int N = apps.size();
        for (int i=0; i<N; i++) {
            final ApplicationInfo info = apps.get(i);
            if (info.componentName.equals(component)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find an ApplicationInfo object for the given packageName and className.
     */
    private ApplicationInfo findApplicationInfoLocked(String packageName, String className) {
        for (ApplicationInfo info: data) {
            final ComponentName component = info.intent.getComponent();
            if (packageName.equals(component.getPackageName())
                    && className.equals(component.getClassName())) {
                return info;
            }
        }
        return null;
    }

    public List<ComponentName> getIgnoreAppList(Context context, IResConfigManager res){    	
    	List<ComponentName> list = new ArrayList<ComponentName>();
    	if(res == null) return list;
    	try {
            XmlResourceParser parser = res.getXml(ResConfigManager.IGNORE_APP_LIST_XML);//R.xml.default_toppackage);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            com.android.internal.util.XmlUtils.beginDocument(parser, "ignoreapps");

            final int depth = parser.getDepth();

            int type = -1;
            while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                    && type != XmlPullParser.END_DOCUMENT) {

                if (type != XmlPullParser.START_TAG) {
                    continue;
                }

                TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IgnorePackage);
                
                String pkg = a.getString(R.styleable.IgnorePackage_iPackageName);
                String clz = a.getString(R.styleable.IgnorePackage_iClassName);
                android.util.Log.i("QsLog", "ignore, pkg:"+pkg+", clz:"+clz);
                if(!TextUtils.isEmpty(pkg) && !TextUtils.isEmpty(clz))
                	list.add(new ComponentName(pkg, clz));
                
                a.recycle();
            }
        } catch (XmlPullParserException e) {
            //LauncherLog.w(TAG, "Got XmlPullParserException while parsing toppackage.", e);
        } catch (IOException e) {
            //LauncherLog.w(TAG, "Got IOException while parsing toppackage.", e);
        }
    	
    	return list;
    }
}
