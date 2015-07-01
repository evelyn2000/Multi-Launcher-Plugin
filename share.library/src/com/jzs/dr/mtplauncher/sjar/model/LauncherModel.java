package com.jzs.dr.mtplauncher.sjar.model;

import android.app.Application;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.jzs.common.content.QsIntent;
import com.jzs.common.launcher.IIconCache;
import com.jzs.common.launcher.ILauncherModel;
import com.jzs.common.launcher.ILauncherProvider;
import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.common.launcher.LauncherHelper;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.FolderInfo;
import com.jzs.common.launcher.model.ILauncherModelCallbacks;
import com.jzs.common.launcher.model.IPackageUpdatedCallbacks;
import com.jzs.common.launcher.model.IconCacheEntry;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.utils.InstallShortcutHelper;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.widget.FastBitmapDrawable;
import com.jzs.dr.mtplauncher.sjar.ctrl.DeferredHandler;
import com.jzs.dr.mtplauncher.sjar.ctrl.InstallWidgetReceiver;
import com.jzs.dr.mtplauncher.sjar.ctrl.InstallWidgetReceiver.WidgetMimeTypeHandlerData;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
public abstract class LauncherModel extends BroadcastReceiver implements ILauncherModel {
    private static final boolean DEBUG_LOADERS_REORDER = false;
    public static final boolean DEBUG_LOADERS = Util.DEBUG_LOADERS;//true;
    public static final String TAG = "QsLog.Launcher.Model";

    private static final int ITEMS_CHUNK = 6; // batch size for the workspace icons
    //private final boolean mAppsCanBeOnExternalStorage;
    protected int mBatchSize; // 0 is all apps at once
    protected int mAllAppsLoadDelay; // milliseconds between batches

    
    protected final Object mLock = new Object();
    protected DeferredHandler mHandler = new DeferredHandler();
    protected LoaderTask mLoaderTask;
    protected boolean mIsLoaderTaskRunning;

    // Specific runnable types that are run on the main thread deferred handler, this allows us to
    // clear all queued binding runnables when the Launcher activity is destroyed.
    protected static final int MAIN_THREAD_NORMAL_RUNNABLE = 0;
    protected static final int MAIN_THREAD_BINDING_RUNNABLE = 1;


    public static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    static {
        sWorkerThread.start();
    }
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    // We start off with everything not loaded.  After that, we assume that
    // our monitoring of the package manager provides all updates and we never
    // need to do a requery.  These are only ever touched from the loader thread.
    private boolean mWorkspaceLoaded;
    private boolean mAllAppsLoaded;

    // When we are loading pages synchronously, we can't just post the binding of items on the side
    // pages as this delays the rotation process.  Instead, we wait for a callback from the first
    // draw (in Workspace) to initiate the binding of the remaining side pages.  Any time we start
    // a normal load, we also clear this set of Runnables.
    protected static final ArrayList<Runnable> mDeferredBindRunnables = new ArrayList<Runnable>();

    protected WeakReference<ILauncherModelCallbacks> mCallbacks;
    protected IPackageUpdatedCallbacks mPackageUpdatedCallbacks;

    // < only access in worker thread >
    protected AllAppsList mBgAllAppsList;

    // The lock that must be acquired before referencing any static bg data structures.  Unlike
    // other locks, this one can generally be held long-term because we never expect any of these
    // static data structures to be referenced outside of the worker thread except on the first
    // load after configuration change.
    public static final Object sBgLock = new Object();

    // sBgItemsIdMap maps *all* the ItemInfos (shortcuts, folders, and widgets) created by
    // LauncherModel to their ids
    public static final HashMap<Long, ItemInfo> sBgItemsIdMap = new HashMap<Long, ItemInfo>();

    // sBgWorkspaceItems is passed to bindItems, which expects a list of all folders and shortcuts
    //       created by LauncherModel that are directly on the home screen (however, no widgets or
    //       shortcuts within folders).
    public static final ArrayList<ItemInfo> sBgWorkspaceItems = new ArrayList<ItemInfo>();

    // sBgAppWidgets is all LauncherAppWidgetInfo created by LauncherModel. Passed to bindAppWidget()
    public static final ArrayList<LauncherAppWidgetInfo> sBgAppWidgets =
        new ArrayList<LauncherAppWidgetInfo>();

    // sBgFolders is all FolderInfos created by LauncherModel. Passed to bindFolders()
    public static final HashMap<Long, FolderInfo> sBgFolders = new HashMap<Long, FolderInfo>();

    // sBgDbIconCache is the set of ItemInfos that need to have their icons updated in the database
    public static final HashMap<Object, dbIconTitleCache> sBgDbIconCache = new HashMap<Object, dbIconTitleCache>();
    private static class dbIconTitleCache{
        public byte[] icon;
        public String title;
    };
    // </ only access in worker thread >
    protected final LauncherHelper mLauncher;
    protected IIconCache mIconCache;
    //protected Bitmap mDefaultIcon;

//    protected static int mCellCountX;
//    protected static int mCellCountY;
//    protected static int mScreenCount;

    protected int mPreviousConfigMcc;
    /// M: record pervious mnc and orientation config.
    protected int mPreviousConfigMnc;
    protected int mPreviousOrientation;

    /// M: Flag to record whether we need to flush icon cache and label cache.
    protected boolean mForceFlushCache;
    private Context mContext;

    public LauncherModel(Context context, LauncherHelper launcher) {
    	mContext = context;
        //mAppsCanBeOnExternalStorage = !Environment.isExternalStorageEmulated();
        mLauncher = launcher;
        
        mIconCache = launcher.getIconCache();
        mBgAllAppsList = new AllAppsList(mIconCache);
        
        //ISharedPrefSettingsManager sharedManager = launcher.getSharedPrefSettingsManager();
        
//        mScreenCount = sharedManager.getWorkspaceScreenCount();
//        updateWorkspaceLayoutCells(sharedManager.getWorkspaceCountCellX(), 
//        		sharedManager.getWorkspaceCountCellY());

        /*mDefaultIcon = mIconCache.getDefaultIcon();.getIconUtilities().createIconBitmap(
                				mIconCache.getFullResDefaultActivityIcon());*/

        initialise();
    }
    
    protected void initialise(){
    	final Resources res = getContext().getResources();
        mAllAppsLoadDelay = 0;//res.getInteger(R.integer.config_allAppsBatchLoadDelay);
        mBatchSize = 0;//res.getInteger(R.integer.config_allAppsBatchSize);
        Configuration config = res.getConfiguration();
        mPreviousConfigMcc = config.mcc;
        /// M: Assign the initial value to the variable of the mPreviousSkin.
        mPreviousConfigMnc = config.mnc;
        //mPreviousSkin = config.skin;
        mPreviousOrientation = config.orientation;
        
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(QsIntent.ACTION_JZS_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        getApplication().registerReceiver(this, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        getApplication().registerReceiver(this, filter);
    }
    
    public void ReleaseInstance(){
    	getApplication().unregisterReceiver(this);
	}
    
    public void onTrimMemory(int level){
    	
    }
    
    public Context getContext(){
    	return mContext;
    }
    
//    public Launcher getLauncher(){
//    	return mLauncher;
//    }
    
    public Application getApplication(){
    	return (Application)mLauncher.getLauncherApplication();
    }
    
    public IIconCache getIconCache(){
    	return mIconCache;
    }
    
    public int getBatchSize(){
    	return mBatchSize;
    }
    
    public int getAllAppsLoadDelay(){
    	return mAllAppsLoadDelay;
    }
    
    public boolean getWorkspaceLoaded(){
    	return mWorkspaceLoaded;
    }

    public void setWorkspaceLoaded(boolean loaded){
    	mWorkspaceLoaded = loaded;
    }
    
    public boolean getAllAppsLoaded(){
    	return mAllAppsLoaded;
    }

    public void setAllAppsLoaded(boolean loaded){
    	mAllAppsLoaded = loaded;
    }
    
    public boolean getIsLoaderTaskRunning(){
    	return mIsLoaderTaskRunning;
    }

    public void setLoaderTaskRunning(boolean loaded){
    	mIsLoaderTaskRunning = loaded;
    }
    
    
    public DeferredHandler getHandler(){
    	return mHandler;
    }
    
    public Object getLockObject(){
    	return mLock;
    }
    
    
    /** Runs the specified runnable immediately if called from the main thread, otherwise it is
     * posted on the main thread handler. */
    public void runOnMainThread(Runnable r) {
        runOnMainThread(r, 0);
    }
    public void runOnMainThread(Runnable r, int type) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            // If we are on the worker thread, post onto the main handler
            mHandler.post(r);
        } else {
            r.run();
        }
    }

    /** Runs the specified runnable immediately if called from the worker thread, otherwise it is
     * posted on the worker thread handler. */
    public static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }

    public Bitmap getFallbackIcon() {
        //return Bitmap.createBitmap(mIconCache.getDefaultIcon());
        return mIconCache.getDefaultIcon();
    }

    public void unbindItemInfosAndClearQueuedBindRunnables() {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            throw new RuntimeException("Expected unbindLauncherItemInfos() to be called from the " +
                    "main thread");
        }

        // Clear any deferred bind runnables
        mDeferredBindRunnables.clear();
        // Remove any queued bind runnables
        mHandler.cancelAllRunnablesOfType(MAIN_THREAD_BINDING_RUNNABLE);
        // Unbind all the workspace items
        unbindWorkspaceItemsOnMainThread();
    }

    /** Unbinds all the sBgWorkspaceItems and sBgAppWidgets on the main thread */
    public void unbindWorkspaceItemsOnMainThread() {
        // Ensure that we don't use the same workspace items data structure on the main thread
        // by making a copy of workspace items first.
        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> tmpAppWidgets = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            tmpWorkspaceItems.addAll(sBgWorkspaceItems);
            tmpAppWidgets.addAll(sBgAppWidgets);
        }
        Runnable r = new Runnable() {
                @Override
                public void run() {
                   for (ItemInfo item : tmpWorkspaceItems) {
                       item.unbind();
                   }
                   for (ItemInfo item : tmpAppWidgets) {
                       item.unbind();
                   }
                }
            };
        runOnMainThread(r);
    }

    /**
     * Adds an item to the DB if it was not created previously, or move it to a new
     * <container, screen, cellX, cellY>
     */
    public void addOrMoveItemInDatabase(ItemInfo item, long container,
            int screen, int cellX, int cellY) {
        if (item.container == ItemInfo.NO_ID) {
            // From all apps
            addItemToDatabase(item, container, screen, cellX, cellY, false);
        } else {
            // From somewhere else
            moveItemInDatabase(item, container, screen, cellX, cellY);
        }
    }

    public void checkItemInfoLocked(
            final long itemId, final ItemInfo item, StackTraceElement[] stackTrace) {
        ItemInfo modelItem = sBgItemsIdMap.get(itemId);
        if (modelItem != null && item != modelItem) {
            // check all the data is consistent
            if (modelItem instanceof ShortcutInfo && item instanceof ShortcutInfo) {
                ShortcutInfo modelShortcut = (ShortcutInfo) modelItem;
                ShortcutInfo shortcut = (ShortcutInfo) item;
                if (modelShortcut.title.toString().equals(shortcut.title.toString()) &&
                        modelShortcut.intent.filterEquals(shortcut.intent) &&
                        modelShortcut.id == shortcut.id &&
                        modelShortcut.itemType == shortcut.itemType &&
                        modelShortcut.container == shortcut.container &&
                        modelShortcut.screen == shortcut.screen &&
                        modelShortcut.cellX == shortcut.cellX &&
                        modelShortcut.cellY == shortcut.cellY &&
                        modelShortcut.spanX == shortcut.spanX &&
                        modelShortcut.spanY == shortcut.spanY &&
                        ((modelShortcut.dropPos == null && shortcut.dropPos == null) ||
                        (modelShortcut.dropPos != null &&
                                shortcut.dropPos != null &&
                                modelShortcut.dropPos[0] == shortcut.dropPos[0] &&
                        modelShortcut.dropPos[1] == shortcut.dropPos[1]))) {
                    // For all intents and purposes, this is the same object
                    return;
                }
            }

            // the modelItem needs to match up perfectly with item if our model is
            // to be consistent with the database-- for now, just require
            // modelItem == item or the equality check above
            String msg = "item: " + ((item != null) ? item.toString() : "null") +
                    "modelItem: " +
                    ((modelItem != null) ? modelItem.toString() : "null") +
                    "Error: ItemInfo passed to checkItemInfo doesn't match original";
            RuntimeException e = new RuntimeException(msg);
            if (stackTrace != null) {
                e.setStackTrace(stackTrace);
            }
            throw e;
        }
    }

    public void checkItemInfo(final ItemInfo item) {
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final long itemId = item.id;
        Runnable r = new Runnable() {
            public void run() {
                synchronized (sBgLock) {
                    checkItemInfoLocked(itemId, item, stackTrace);
                }
            }
        };
        runOnWorkerThread(r);
    }

    protected void updateItemInDatabaseHelper(final ContentValues values,
            final ItemInfo item, final String callingFunction) {
        final long itemId = item.id;
        final Uri uri = LauncherSettings.Favorites.getContentUri(itemId, false);
        final ContentResolver cr = getContext().getContentResolver();
        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "updateItemInDatabaseHelper values = " + values + ", item = " + item);
        }

        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            public void run() {
                cr.update(uri, values, null, null);

                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    checkItemInfoLocked(itemId, item, stackTrace);

                    if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                            item.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                        // Item is in a folder, make sure this folder exists
                        if (!sBgFolders.containsKey(item.container)) {
                            // An items container is being set to a that of an item which is not in
                            // the list of Folders.
                            String msg = "item: " + item + " container being set to: " +
                                    item.container + ", not in the list of folders";
                            Log.e(TAG, msg);
                            //Launcher.dumpDebugLogsToConsole();
                        }
                    }

                    // Items are added/removed from the corresponding FolderInfo elsewhere, such
                    // as in Workspace.onDrop. Here, we just add/remove them from the list of items
                    // that are on the desktop, as appropriate
                    ItemInfo modelItem = sBgItemsIdMap.get(itemId);
                    if(modelItem != null){
                        if (modelItem.container == LauncherSettings.Favorites.CONTAINER_DESKTOP ||
                                modelItem.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                            switch (modelItem.itemType) {
                                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                                    if (!sBgWorkspaceItems.contains(modelItem)) {
                                        sBgWorkspaceItems.add(modelItem);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        } else {
                            sBgWorkspaceItems.remove(modelItem);
                        }
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Move an item in the DB to a new <container, screen, cellX, cellY>
     */
    public void moveItemInDatabase(final ItemInfo item, final long container,
            final int screen, final int cellX, final int cellY) {
        String transaction = "DbDebug    Modify item (" + item.title + ") in db, id: " + item.id +
                " (" + item.container + ", " + item.screen + ", " + item.cellX + ", " + item.cellY +
                ") --> " + "(" + container + ", " + screen + ", " + cellX + ", " + cellY + ")";
        //Launcher.sDumpLogs.add(transaction);
        //Log.d(TAG, transaction);

        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "moveItemInDatabase: item = " + item + ", container = " + container + ", screen = " + screen
                    + ", cellX = " + cellX + ", cellY = " + cellY);
        }

        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;

        // We store hotseat items in canonical form which is this orientation invariant position
        // in the hotseat
        if (screen < 0 &&
                container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            item.screen = mLauncher.getOrderInHotseat(cellX, cellY);
        } else {
            item.screen = screen;
        }

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites.CONTAINER, item.container);
        values.put(LauncherSettings.Favorites.CELLX, item.cellX);
        values.put(LauncherSettings.Favorites.CELLY, item.cellY);
        values.put(LauncherSettings.Favorites.SCREEN, item.screen);

        updateItemInDatabaseHelper(values, item, "moveItemInDatabase");
    }

    /**
     * Move and/or resize item in the DB to a new <container, screen, cellX, cellY, spanX, spanY>
     */
    public void modifyItemInDatabase(final ItemInfo item, final long container,
            final int screen, final int cellX, final int cellY, final int spanX, final int spanY) {
        String transaction = "DbDebug    Modify item (" + item.title + ") in db, id: " + item.id +
                " (" + item.container + ", " + item.screen + ", " + item.cellX + ", " + item.cellY +
                ") --> " + "(" + container + ", " + screen + ", " + cellX + ", " + cellY + ")";
        //Launcher.sDumpLogs.add(transaction);
        Log.d(TAG, transaction);

        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "modifyItemInDatabase: item = " + item + ", container = " + container + ", screen = " + screen
                    + ", cellX = " + cellX + ", cellY = " + cellY + ", spanX = " + spanX + ", spanY = " + spanY);
        }

        item.cellX = cellX;
        item.cellY = cellY;
        item.spanX = spanX;
        item.spanY = spanY;

        // We store hotseat items in canonical form which is this orientation invariant position
        // in the hotseat
        if (screen < 0 &&
                container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            item.screen = mLauncher.getOrderInHotseat(cellX, cellY);
        } else {
            item.screen = screen;
        }

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites.CONTAINER, item.container);
        values.put(LauncherSettings.Favorites.CELLX, item.cellX);
        values.put(LauncherSettings.Favorites.CELLY, item.cellY);
        values.put(LauncherSettings.Favorites.SPANX, item.spanX);
        values.put(LauncherSettings.Favorites.SPANY, item.spanY);
        values.put(LauncherSettings.Favorites.SCREEN, item.screen);

        updateItemInDatabaseHelper(values, item, "modifyItemInDatabase");
    }

    /**
     * Update an item to the database in a specified container.
     */
    public void updateItemInDatabase(final ItemInfo item) {
        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "updateItemInDatabase: item = " + item);
        }

        final ContentValues values = new ContentValues();
        item.onAddToDatabase(values);
        item.updateValuesWithCoordinates(values, item.cellX, item.cellY);
        updateItemInDatabaseHelper(values, item, "updateItemInDatabase");
    }

    /**
     * Returns true if the shortcuts already exists in the database.
     * we identify a shortcut by its title and intent.
     */
    public static boolean shortcutExists(Context context, String title, Intent intent) {
        /**
         * M: When installShortcut, Launcher add flags Intent.FLAG_ACTIVITY_NEW_TASK |
         * Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED shortcut intent cannot match with intent saved in launcher, so create
         * shortcut repeated.
         */
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
            new String[] { "title", "intent" }, "title=? and intent=?",
            new String[] { title, intent.toUri(0) }, null);
        boolean result = false;
        try {
            result = c.moveToFirst();
        } finally {
            c.close();
        }
        return result;
    }

    /**
     * Returns an ItemInfo array containing all the items in the LauncherModel.
     * The ItemInfo.id is not set through this function.
     */
    public static ArrayList<ItemInfo> getItemsInLocalCoordinates(Context context) {
        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, new String[] {
                LauncherSettings.Favorites.ITEM_TYPE, LauncherSettings.Favorites.CONTAINER,
                LauncherSettings.Favorites.SCREEN, LauncherSettings.Favorites.CELLX, LauncherSettings.Favorites.CELLY,
                LauncherSettings.Favorites.SPANX, LauncherSettings.Favorites.SPANY }, null, null, null);

        final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
        final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
        final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
        final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
        final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
        final int spanXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
        final int spanYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);

        try {
            while (c.moveToNext()) {
                ItemInfo item = new ItemInfo();
                item.cellX = c.getInt(cellXIndex);
                item.cellY = c.getInt(cellYIndex);
                item.spanX = c.getInt(spanXIndex);
                item.spanY = c.getInt(spanYIndex);
                item.container = c.getInt(containerIndex);
                item.itemType = c.getInt(itemTypeIndex);
                item.screen = c.getInt(screenIndex);

                items.add(item);
            }
        } catch (Exception e) {
            items.clear();
        } finally {
            c.close();
        }

        return items;
    }

    /**
     * Find a folder in the db, creating the FolderInfo if necessary, and adding it to folderList.
     */
    public FolderInfo getFolderById(Context context, HashMap<Long,FolderInfo> folderList, long id) {
        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, null,
                "_id=? and (itemType=? or itemType=?)",
                new String[] { String.valueOf(id),
                        String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_FOLDER)}, null);

        try {
            if (c.moveToFirst()) {
                final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
                final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
                final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
                final int iconPackageIndex = c.getColumnIndexOrThrow(
                        LauncherSettings.Favorites.ICON_PACKAGE);
                final int iconResourceIndex = c.getColumnIndexOrThrow(
                        LauncherSettings.Favorites.ICON_RESOURCE);

                FolderInfo folderInfo = null;
                switch (c.getInt(itemTypeIndex)) {
                    case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                        folderInfo = findOrMakeFolder(folderList, id, c.getString(iconPackageIndex)
                                , c.getString(iconResourceIndex));
                        break;
                }

                folderInfo.title = c.getString(titleIndex);
                folderInfo.id = id;
                folderInfo.container = c.getInt(containerIndex);
                folderInfo.screen = c.getInt(screenIndex);
                folderInfo.cellX = c.getInt(cellXIndex);
                folderInfo.cellY = c.getInt(cellYIndex);

                return folderInfo;
            }
        } finally {
            c.close();
        }

        return null;
    }

//    public static void addItemToDatabase(Launcher launcher, final ItemInfo item, final long container,
//            final int screen, final int cellX, final int cellY, final boolean notify) {
//    	
////    	ILauncherPluginEntry pluginEntry = (ILauncherPluginEntry)launcher.getLauncherPluginEntry();
////        if(pluginEntry == null)
////        	return;
//        
//        addItemToDatabase(launcher, launcher.getLauncherProvider(), 
//        		item, container, screen, cellX, cellY, notify);
//    }
    /**
     * Add an item to the database in a specified container. Sets the container, screen, cellX and
     * cellY fields of the item. Also assigns an ID to the item.
     */
    public void addItemToDatabase(final ItemInfo item, final long container,
            final int screen, final int cellX, final int cellY, final boolean notify) {
        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "addItemToDatabase item = " + item + ", container = " + container + ", screen = " + screen
                    + ", cellX " + cellX + ", cellY = " + cellY + ", notify = " + notify);
        }
        //Context context
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;
        // We store hotseat items in canonical form which is this orientation invariant position
        // in the hotseat
        if (screen < 0 &&
                container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            item.screen = mLauncher.getOrderInHotseat(cellX, cellY);
        } else {
            item.screen = screen;
        }
        ILauncherProvider provider = mLauncher.getLauncherProvider();
        
        final ContentValues values = new ContentValues();
        final ContentResolver cr = mLauncher.getContentResolver();
        item.onAddToDatabase(values);

        if(provider != null)
        	item.id = provider.generateNewId();
        
        values.put(LauncherSettings.Favorites._ID, item.id);
        item.updateValuesWithCoordinates(values, item.cellX, item.cellY);
        
        Runnable r = new Runnable() {
            public void run() {
                String transaction = "DbDebug    Add item (" + item.title + ") to db, id: "
                        + item.id + " (" + container + ", " + screen + ", " + cellX + ", "
                        + cellY + ")";
                //Launcher.sDumpLogs.add(transaction);
                Log.d(TAG, transaction);

                cr.insert(notify ? LauncherSettings.Favorites.CONTENT_URI :
                        LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values);

                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    checkItemInfoLocked(item.id, item, null);
                    sBgItemsIdMap.put(item.id, item);
                    if (Util.DEBUG_LOADERS) {
                        Util.Log.d(TAG, "addItemToDatabase sBgItemsIdMap.put = " + item.id + ", item = " + item);
                    }
                    switch (item.itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                            sBgFolders.put(item.id, (FolderInfo) item);
                            // Fall through
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP ||
                                    item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                                sBgWorkspaceItems.add(item);
                            } else {
                                if (!sBgFolders.containsKey(item.container)) {
                                    // Adding an item to a folder that doesn't exist.
                                    String msg = "adding item: " + item + " to a folder that " +
                                            " doesn't exist";
                                    Log.e(TAG, msg);
                                    //Launcher.dumpDebugLogsToConsole();
                                }
                            }
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                            sBgAppWidgets.add((LauncherAppWidgetInfo) item);
                            if (Util.DEBUG_LOADERS) {
                                Util.Log.d(TAG, "addItemToDatabase sAppWidgets.add = " + item);
                            }
                            break;
                    }
                }
            }
        };
        runOnWorkerThread(r);
        
        //return true;
    }

    /**
     * Creates a new unique child id, for a given cell span across all layouts.
     */
    public static int getCellLayoutChildId(
            long container, int screen, int localCellX, int localCellY, int spanX, int spanY) {
        return (((int) container & 0xFF) << 24)
                | (screen & 0xFF) << 16 | (localCellX & 0xFF) << 8 | (localCellY & 0xFF);
    }
    
    public int getMaxCellCountX() {
        return mLauncher.getResConfigManager().getInteger(ResConfigManager.CONFIG_MAX_WORKSPACE_CELL_COUNTX);
    }

    public int getMaxCellCountY() {
        return mLauncher.getResConfigManager().getInteger(ResConfigManager.CONFIG_MAX_WORKSPACE_CELL_COUNTY);
    }
    
    public int getMaxWorkspaceScreenCount() {
        return mLauncher.getResConfigManager().getInteger(ResConfigManager.CONFIG_WORKSPACE_MAX_SCREENCOUNT);
    }

    public int getCellCountX() {
        return mLauncher.getSharedPrefSettingsManager().getWorkspaceCountCellX();
    }

    public int getCellCountY() {
        return mLauncher.getSharedPrefSettingsManager().getWorkspaceCountCellY();
    }
    
    public int getWorkspaceScreenCount() {
        return mLauncher.getSharedPrefSettingsManager().getWorkspaceScreenCount();
    }

    /**
     * Updates the model orientation helper to take into account the current layout dimensions
     * when performing local/canonical coordinate transformations.
     */
//    public void updateWorkspaceLayoutCells(int cellCountX, int cellCountY) {
//        mCellCountX = cellCountX;
//        mCellCountY = cellCountY;
//    }
//    
//    public void updateWorkspaceLayoutCount(int count) {
//    	mScreenCount = count;
//    }

    /**
     * Removes the specified item from the database
     * @param context
     * @param item
     */
    public void deleteItemFromDatabase(/*Context context, */final ItemInfo item) {
        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "deleteItemFromDatabase item = " + item);
        }

        final ContentResolver cr = getContext().getContentResolver();
        final Uri uriToDelete = LauncherSettings.Favorites.getContentUri(item.id, false);
        Runnable r = new Runnable() {
            public void run() {
                String transaction = "DbDebug    Delete item (" + item.title + ") from db, id: "
                        + item.id + " (" + item.container + ", " + item.screen + ", " + item.cellX +
                        ", " + item.cellY + ")";
                //Launcher.sDumpLogs.add(transaction);
                Log.d(TAG, transaction);

                cr.delete(uriToDelete, null, null);

                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    switch (item.itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                            sBgFolders.remove(item.id);
                            for (ItemInfo info: sBgItemsIdMap.values()) {
                                if (info.container == item.id) {
                                    // We are deleting a folder which still contains items that
                                    // think they are contained by that folder.
                                    String msg = "deleting a folder (" + item + ") which still " +
                                            "contains items (" + info + ")";
                                    Log.e(TAG, msg);
                                    //Launcher.dumpDebugLogsToConsole();
                                }
                            }
                            sBgWorkspaceItems.remove(item);
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                            sBgWorkspaceItems.remove(item);
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                            sBgAppWidgets.remove((LauncherAppWidgetInfo) item);
                            break;
                    }
                    sBgItemsIdMap.remove(item.id);
                    sBgDbIconCache.remove(item);
                }
                if (Util.DEBUG_LOADERS) {
                    Util.Log.d(TAG, "deleteItemFromDatabase sAppWidgets.remove = " + item
                            + ", sItemsIdMap.remove = " + item.id);
                }
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Remove the contents of the specified folder from the database
     */
    public /*static */void deleteFolderContentsFromDatabase(/*Context context, */final FolderInfo info) {
        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "deleteFolderContentsFromDatabase info = " + info);
        }

        final ContentResolver cr = getContext().getContentResolver();

        Runnable r = new Runnable() {
            public void run() {
                cr.delete(LauncherSettings.Favorites.getContentUri(info.id, false), null, null);
                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    sBgItemsIdMap.remove(info.id);
                    sBgFolders.remove(info.id);
                    sBgDbIconCache.remove(info);
                    sBgWorkspaceItems.remove(info);
                    if (Util.DEBUG_LOADERS) {
                        Util.Log.d(TAG, "deleteFolderContentsFromDatabase sBgItemsIdMap.remove = " + info.id);
                    }
                }

                cr.delete(LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION,
                        LauncherSettings.Favorites.CONTAINER + "=" + info.id, null);
                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    for (ItemInfo childInfo : info.contents) {
                        sBgItemsIdMap.remove(childInfo.id);
                        sBgDbIconCache.remove(childInfo);
                        if (Util.DEBUG_LOADERS) {
                            Util.Log.d(TAG, "deleteFolderContentsFromDatabase sItemsIdMap.remove = " + childInfo.id);
                        }
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }
    
    public void setPackageUpdateCallback(IPackageUpdatedCallbacks callback){
        synchronized (mLock) {
            mPackageUpdatedCallbacks = callback;
        }
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void setCallbacks(ILauncherModelCallbacks callbacks) {
        synchronized (mLock) {
            mCallbacks = new WeakReference<ILauncherModelCallbacks>(callbacks);
        }
    }
    
    public ILauncherModelCallbacks getCallbacks(){
    	if(mCallbacks != null)
    		return mCallbacks.get();
    	return null;
    }

    /**
     * Call from the handler for ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED and
     * ACTION_PACKAGE_CHANGED.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG_LOADERS) {
            Log.d(TAG, "onReceive intent=" + intent);
        }
        //Log.i("QsLog", "LauncherModel::onReceive intent=" + intent);
        
        final String action = intent.getAction();

        if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                || Intent.ACTION_PACKAGE_ADDED.equals(action)
                || QsIntent.ACTION_JZS_PACKAGE_CHANGED.equals(action)) {
            final String packageName = intent.getData().getSchemeSpecificPart();
            final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            int op = PackageUpdatedTask.OP_NONE;

            if (packageName == null || packageName.length() == 0) {
                // they sent us a bad intent
                Log.e(TAG, "onReceive packageName is null, "+action);
                return;
            }

            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)|| QsIntent.ACTION_JZS_PACKAGE_CHANGED.equals(action)) {
                op = PackageUpdatedTask.OP_UPDATE;
            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                if (!replacing) {
                    op = PackageUpdatedTask.OP_REMOVE;
                }
                // else, we are replacing the package, so a PACKAGE_ADDED will be sent
                // later, we will update the package at this time
            } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                if (!replacing) {
                    op = PackageUpdatedTask.OP_ADD;
                } else {
                    op = PackageUpdatedTask.OP_UPDATE;
                }
            }
            
            if(DEBUG_LOADERS){
                Util.Log.i(TAG, "ACTION_PACKAGE_CHANGED, op:"+op
                        +", replacing="+replacing
                        +", packageName:"+packageName);
            }

            if (op != PackageUpdatedTask.OP_NONE) {
            	if(QsIntent.ACTION_JZS_PACKAGE_CHANGED.equals(action)
            	        || Intent.ACTION_PACKAGE_CHANGED.equals(action)){
            		String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_COMPONENT_NAME_LIST);
            		if(packages != null && packages.length > 1)
            			enqueuePackageUpdated(new PackageUpdatedTask(op, packages));
            		else
            			enqueuePackageUpdated(new PackageUpdatedTask(op, new String[] { packageName }));
            	} else {
            		enqueuePackageUpdated(new PackageUpdatedTask(op, new String[] { packageName }));
            	}
            }

        } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
            // First, schedule to add these apps back in.
            String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
            if(DEBUG_LOADERS){
                if(packages != null){
                    Util.Log.d(TAG, "ACTION_EXTERNAL_APPLICATIONS_AVAILABLE: size:"+packages.length);
                    for(String pkg : packages){
                        Util.Log.d(TAG, "jzs.pkg:"+pkg);
                    }
                } else {
                    Util.Log.d(TAG, "ACTION_EXTERNAL_APPLICATIONS_AVAILABLE: size 0");
                }
            }
            enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_ADD_OR_UPDATE, packages));
            // Then, rebind everything.
            startLoaderFromBackground();
        } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
            String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
            if(DEBUG_LOADERS){
                if(packages != null)
                    Util.Log.d(TAG, "ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE: size:"+packages.length);
                else
                    Util.Log.d(TAG, "ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE: size 0");
            }
            enqueuePackageUpdated(new PackageUpdatedTask(
                        PackageUpdatedTask.OP_UNAVAILABLE, packages));
        } else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            // If we have changed locale we need to clear out the labels in all apps/workspace.
            Util.Log.d(TAG, "LOCALE_CHANGED: config = " + context.getResources().getConfiguration());
            forceReload();
        } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
             // Check if configuration change was an mcc/mnc change which would affect app resources
             // and we would need to clear out the labels in all apps/workspace. Same handling as
             // above for ACTION_LOCALE_CHANGED
             Configuration currentConfig = context.getResources().getConfiguration();
             
//             /// M: Check if configuration change was skin change, we would need to clear FolderIcon.sStaticValuesDirty.
//            if (!mPreviousSkin.equals(currentConfig.skin)) {
//                FolderIcon.resetValuesDirty();
//                /// M: Update previousSkin.
//                mPreviousSkin = currentConfig.skin;
//            }
             
            if (mPreviousConfigMcc != currentConfig.mcc || mPreviousConfigMnc != currentConfig.mnc) {
                Util.Log.d(TAG, "Reload apps on config change. curr_mcc:" + currentConfig.mcc + ", prevmcc:"
                        + mPreviousConfigMcc + ",mPreviousConfigMnc = " + mPreviousConfigMnc + ",currentConfig.mnc = "
                        + currentConfig.mnc + ", currentConfig = " + currentConfig);
                forceReload();
            }
            // Update previousConfig
            mPreviousConfigMcc = currentConfig.mcc;
            mPreviousConfigMnc = currentConfig.mnc;

            /// M: set re-sync apps pages flag if orientation change.
            if (mPreviousOrientation != currentConfig.orientation) {
                if (mCallbacks != null) {
                	ILauncherModelCallbacks callbacks = mCallbacks.get();
                    if (callbacks != null) {
                        callbacks.notifyOrientationChanged();
                    }
                }
                mPreviousOrientation = currentConfig.orientation;
            }
        } else if (SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED.equals(action) ||
                   SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED.equals(action)) {
            if (mCallbacks != null) {
            	ILauncherModelCallbacks callbacks = mCallbacks.get();
                if (callbacks != null) {
                    callbacks.bindSearchablesChanged();
                }
            }
        }/* else if (ACTION_SWITCH_SCENE.equals(action)) {
            /// M: added for scene feature, receive swich_scene intent to switch scene.
            if (mCallbacks != null) {
                Callbacks callbacks = mCallbacks.get();
                if (callbacks != null) {
                    callbacks.switchScene();
                }
            }
            
            updateDatabaseAndSetWallpaper();
        }*/
    }
    
    public void reBindAllApplications(){
        if(isAllAppsLoaded()){
            enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_REBIND_ALL_APPS, null));
        } else {
            if (DEBUG_LOADERS) {
                Log.w(TAG, "app not loaded");
            }
        }
    }
    
    public void reBindWorkspace(){
        
    }

    /// M: Modified for scene feature, remove the private modifier.
    public void forceReload() {
        resetLoadedState(true, true);
        if (DEBUG_LOADERS) {
            Log.d(TAG, "forceReload: mLoaderTask =" + mLoaderTask + ", mAllAppsLoaded = "
                    + mAllAppsLoaded + ", mWorkspaceLoaded = " + mWorkspaceLoaded + ", this = " + this);
        }

        // Do this here because if the launcher activity is running it will be restarted.
        // If it's not running startLoaderFromBackground will merely tell it that it needs
        // to reload.
        startLoaderFromBackground();
    }

    public void resetLoadedState(boolean resetAllAppsLoaded, boolean resetWorkspaceLoaded) {
        synchronized (mLock) {
            if (Util.DEBUG_LOADERS) {
                Util.Log.d(TAG, "resetLoadedState: mLoaderTask =" + mLoaderTask
                        + ", this = " + this);
            }
            // Stop any existing loaders first, so they don't set mAllAppsLoaded or
            // mWorkspaceLoaded to true later
            stopLoaderLocked();
            if (resetAllAppsLoaded) mAllAppsLoaded = false;
            if (resetWorkspaceLoaded) mWorkspaceLoaded = false;
        }
    }

    /**
     * When the launcher is in the background, it's possible for it to miss paired
     * configuration changes.  So whenever we trigger the loader from the background
     * tell the launcher that it needs to re-run the loader when it comes back instead
     * of doing it now.
     */
    public void startLoaderFromBackground() {
        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "startLoaderFromBackground: mCallbacks = " + mCallbacks + ", this = " + this);
        }

        boolean runLoader = false;
        if (mCallbacks != null) {
        	ILauncherModelCallbacks callbacks = mCallbacks.get();
            if (Util.DEBUG_LOADERS) {
                Util.Log.d(TAG, "startLoaderFromBackground: callbacks = " + callbacks + ", this = " + this);
            }
            if (callbacks != null) {
                if (Util.DEBUG_LOADERS) {
                    Util.Log.d(TAG, "startLoaderFromBackground: callbacks.setLoadOnResume() = "
                            + callbacks.setLoadOnResume() + ", this = " + this);
                }
                // Only actually run the loader if they're not paused.
                if (!callbacks.setLoadOnResume()) {
                    runLoader = true;
                }
            }
        }
        if (runLoader) {
            startLoader(false, -1);
        }
    }

    // If there is already a loader task running, tell it to stop.
    // returns true if isLaunching() was true on the old task
    private boolean stopLoaderLocked() {
        boolean isLaunching = false;
        LoaderTask oldTask = mLoaderTask;
        if (oldTask != null) {
            if (oldTask.isLaunching()) {
                isLaunching = true;
            }
            oldTask.stopLocked();
        }
        if (DEBUG_LOADERS) {
            Util.Log.d(TAG, "stopLoaderLocked: mLoaderTask =" + mLoaderTask + ", isLaunching = "
                    + isLaunching + ", this = " + this);
        }
        return isLaunching;
    }
    
    public void startLoader(boolean isLaunching, int synchronousBindPage) {
        startLoader(isLaunching, synchronousBindPage, true);
    }
    
    public void startLoader(boolean isLaunching, int synchronousBindPage, boolean synchronousBindApps) {
        synchronized (mLock) {
            if (DEBUG_LOADERS) {
                Util.Log.d(TAG, "startLoader: isLaunching=" + isLaunching + ", mCallbacks = " + mCallbacks);
            }

            // Clear any deferred bind-runnables from the synchronized load process
            // We must do this before any loading/binding is scheduled below.
            mDeferredBindRunnables.clear();

            // Don't bother to start the thread if we know it's not going to do anything
            if (mCallbacks != null && mCallbacks.get() != null) {
                // If there is already one running, tell it to stop.
                // also, don't downgrade isLaunching if we're already running
                isLaunching = isLaunching || stopLoaderLocked();
                /// M: added for top package feature, load top packages from a xml file.
                //AllAppsList.loadTopPackage(mApp);
                mLoaderTask = new LoaderTask(getContext(), isLaunching, synchronousBindApps);
                if (Util.DEBUG_LOADERS) {
                    Util.Log.d(TAG, "startLoader: mAllAppsLoaded = " + mAllAppsLoaded
                            + ",mWorkspaceLoaded = " + mWorkspaceLoaded + ",synchronousBindPage = "
                            + synchronousBindPage + ",mIsLoaderTaskRunning = "
                            + mIsLoaderTaskRunning + ",mLoaderTask = " + mLoaderTask,
                            new Throwable("startLoader"));
                }
                
                /// M: we need also to check mIsLoaderTaskRunning first to avoid
                /// RuntimeException happens in runBindSynchronousPage,
                /// this would happen when Launcher recreated by orientation
                /// change (like run android.dpi.cts.ConfigurationScreenLayoutTest).
                if (synchronousBindPage > -1 && mAllAppsLoaded && mWorkspaceLoaded
                        && !mIsLoaderTaskRunning) {
                    mLoaderTask.runBindSynchronousPage(synchronousBindPage, synchronousBindApps);
                } else {
                    sWorkerThread.setPriority(Thread.NORM_PRIORITY);
                    sWorker.post(mLoaderTask);
                }
            }
        }
    }

    public void bindRemainingSynchronousPages() {
        // Post the remaining side pages to be loaded
        if (!mDeferredBindRunnables.isEmpty()) {
            for (final Runnable r : mDeferredBindRunnables) {
                mHandler.post(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
            mDeferredBindRunnables.clear();
        }
    }

    public void stopLoader() {
        synchronized (mLock) {
            if (mLoaderTask != null) {
                if (Util.DEBUG_LOADERS) {
                    Util.Log.d(TAG, "stopLoader: mLoaderTask = " + mLoaderTask
                            + ",mIsLoaderTaskRunning = " + mIsLoaderTaskRunning);
                }
                mLoaderTask.stopLocked();
            }
        }
    }

    public boolean isAllAppsLoaded() {
        return mAllAppsLoaded;
    }

    public boolean isLoadingWorkspace() {
        synchronized (mLock) {
            if (mLoaderTask != null) {
                return mLoaderTask.isLoadingWorkspace();
            }
        }
        return false;
    }

    /**
     * Runnable for the thread that loads the contents of the launcher:
     *   - workspace icons
     *   - widgets
     *   - all apps icons
     */
//    private class LoaderTask implements Runnable {
//        
//    }

    public void enqueuePackageUpdated(PackageUpdatedTask task) {
        sWorker.post(task);
    }

    private class PackageUpdatedTask implements Runnable {
        int mOp;
        String[] mPackages;

        public static final int OP_NONE = 0;
        public static final int OP_ADD = 1;
        public static final int OP_UPDATE = 2;
        public static final int OP_REMOVE = 3; // uninstlled
        public static final int OP_UNAVAILABLE = 4; // external media unmounted
        
        public static final int OP_ADD_OR_UPDATE = 5;
        
        public static final int OP_REBIND_ALL_APPS = 6;


        public PackageUpdatedTask(int op, String[] packages) {
            mOp = op;
            mPackages = packages;
        }

        public void run() {
            final Context context = getContext();

            final String[] packages = mPackages;
            final int N = packages != null ? packages.length : 0;
            
            switch (mOp) {
                case OP_ADD:
                case OP_ADD_OR_UPDATE:
                    for (int i=0; i<N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "PackageUpdatedTask:: mAllAppsList.addPackage " + packages[i]);
                        mBgAllAppsList.addPackage(context, packages[i], (OP_ADD_OR_UPDATE == mOp));
                    }
                    break;
                case OP_UPDATE:
                    for (int i=0; i<N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "PackageUpdatedTask::mAllAppsList.updatePackage " + packages[i]);
                        mBgAllAppsList.updatePackage(context, packages[i]);
                    }
                    break;
                case OP_REMOVE:
                case OP_UNAVAILABLE:
                    for (int i=0; i<N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "PackageUpdatedTask::mAllAppsList.removePackage " + packages[i]);
                        mBgAllAppsList.removePackage(packages[i]);
                    }
                    break;
                case OP_REBIND_ALL_APPS:{
                    final ILauncherModelCallbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                    if (callbacks == null) {
                        Log.w(TAG, "Nobody to tell about the new app.  Launcher is probably loading.");
                        return;
                    }
                    final ArrayList<ApplicationInfo> data = (ArrayList<ApplicationInfo>)mBgAllAppsList.data.clone();
                    mHandler.post(new Runnable() {
                        public void run() {
                            ILauncherModelCallbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                            if (callbacks == cb && cb != null) {
                                callbacks.bindAllApplications(data);
                            }
                        }
                    });
                }return;
            }

            ArrayList<ApplicationInfo> added = null;
            ArrayList<ApplicationInfo> modified = null;
            /// M: added for remove appWidget.
            ArrayList<String> appWidgetRemoved = null;
            if (DEBUG_LOADERS) Log.d(TAG, "PackageUpdatedTask:: 111==");
            if (mBgAllAppsList.added.size() > 0) {
                added = new ArrayList<ApplicationInfo>(mBgAllAppsList.added);
                mBgAllAppsList.added.clear();
            }
            if (mBgAllAppsList.modified.size() > 0) {
                modified = new ArrayList<ApplicationInfo>(mBgAllAppsList.modified);
                mBgAllAppsList.modified.clear();
            }
            if (DEBUG_LOADERS) {
                Util.Log.d(TAG, "PackageUpdatedTask:: 222==added:"+added);
                Util.Log.d(TAG, "PackageUpdatedTask:: 333==modified:"+modified);
            }
            // We may be removing packages that have no associated launcher application, so we
            // pass through the removed package names directly.
            // NOTE: We flush the icon cache aggressively in removePackage() above.
            final ArrayList<String> removedPackageNames = new ArrayList<String>();
            if (mBgAllAppsList.removed.size() > 0) {
                mBgAllAppsList.removed.clear();

                for (int i = 0; i < N; ++i) {
                    removedPackageNames.add(packages[i]);
                }
            }
            
            /// M: added for remove appWidget.
            if (mBgAllAppsList.appwidgetRemoved.size() > 0) {
                appWidgetRemoved = mBgAllAppsList.appwidgetRemoved;
                mBgAllAppsList.appwidgetRemoved = new ArrayList<String>();
            }

            final ILauncherModelCallbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
            if (callbacks == null) {
                Log.w(TAG, "Nobody to tell about the new app.  Launcher is probably loading.");
                return;
            }

            if (Util.DEBUG_LOADERS) {
                Util.Log.d(TAG, "PackageUpdatedTask: added = " + added 
                        + ",removedPackageNames = " + removedPackageNames
                        + ",appWidgetRemoved = " + appWidgetRemoved);
                Util.Log.d(TAG, "PackageUpdatedTask: modified = " + modified );
            }

            if (added != null) {
                final ArrayList<ApplicationInfo> addedFinal = added;
                mHandler.post(new Runnable() {
                    public void run() {
                        if(mPackageUpdatedCallbacks != null)
                            mPackageUpdatedCallbacks.bindAppsAdded(addedFinal);
                        
                    	ILauncherModelCallbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsAdded(addedFinal);
                        }
                    }
                });
            }
            if (modified != null) {
                final ArrayList<ApplicationInfo> modifiedFinal = modified;
                mHandler.post(new Runnable() {
                    public void run() {
                        
                        if(mPackageUpdatedCallbacks != null)
                            mPackageUpdatedCallbacks.bindAppsUpdated(modifiedFinal);
                        
                    	ILauncherModelCallbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsUpdated(modifiedFinal);
                        }
                    }
                });
            }
            if (!removedPackageNames.isEmpty()) {
                final boolean permanent = mOp != OP_UNAVAILABLE;
                mHandler.post(new Runnable() {
                    public void run() {
                        if(mPackageUpdatedCallbacks != null)
                            mPackageUpdatedCallbacks.bindAppsRemoved(removedPackageNames, permanent);
                        
                    	ILauncherModelCallbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsRemoved(removedPackageNames, permanent);
                        }
                    }
                });
            }           
            
            /// M: added for remove appWidget.
            if (appWidgetRemoved != null) {
                final boolean permanent = mOp != OP_UNAVAILABLE;
                final ArrayList<String> removedFinal = appWidgetRemoved;
                mHandler.post(new Runnable() {
                    public void run() {
                        
                    	ILauncherModelCallbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppWidgetRemoved(removedFinal, permanent);
                        }
                    }
                });
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    
                    if(mPackageUpdatedCallbacks != null)
                        mPackageUpdatedCallbacks.bindPackagesUpdated();
                    
                	ILauncherModelCallbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                    if (callbacks == cb && cb != null) {
                        callbacks.bindPackagesUpdated();
                    }
                }
            });
        }
    }

    /**
     * This is called from the code that adds shortcuts from the intent receiver.  This
     * doesn't have a Cursor, but
     */
    public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, Context context) {
        return getShortcutInfo(manager, intent, context, null, -1, -1, null);
    }

    /**
     * Make an ShortcutInfo object for a shortcut that is an application.
     *
     * If c is not null, then it will be used to fill in missing data like the title and icon.
     */
    public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, Context context,
            Cursor c, int iconIndex, int titleIndex, HashMap<Object, CharSequence> labelCache) {
        Bitmap icon = null;
        final ShortcutInfo info = new ShortcutInfo();

        ComponentName componentName = intent.getComponent();
        if (componentName == null) {
            return null;
        }

        try {
            PackageInfo pi = manager.getPackageInfo(componentName.getPackageName(), 0);
            if (!pi.applicationInfo.enabled) {
                // If we return null here, the corresponding item will be removed from the launcher
                // db and will not appear in the workspace.
                return null;
            }
        } catch (NameNotFoundException e) {
            Log.d(TAG, "getPackInfo failed for package " + componentName.getPackageName());
        }

        // TODO: See if the PackageManager knows about this case.  If it doesn't
        // then return null & delete this.

        // the resource -- This may implicitly give us back the fallback icon,
        // but don't worry about that.  All we're doing with usingFallbackIcon is
        // to avoid saving lots of copies of that in the database, and most apps
        // have icons anyway.

        // Attempt to use queryIntentActivities to get the ResolveInfo (with IntentFilter info) and
        // if that fails, or is ambiguious, fallback to the standard way of getting the resolve info
        // via resolveActivity().
        ResolveInfo resolveInfo = null;
        ComponentName oldComponent = intent.getComponent();
        Intent newIntent = new Intent(intent.getAction(), null);
        newIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        newIntent.setPackage(oldComponent.getPackageName());
        List<ResolveInfo> infos = manager.queryIntentActivities(newIntent, 0);
        for (ResolveInfo i : infos) {
            ComponentName cn = new ComponentName(i.activityInfo.packageName,
                    i.activityInfo.name);
            if (cn.equals(oldComponent)) {
                resolveInfo = i;
            }
        }
        if (resolveInfo == null) {
            resolveInfo = manager.resolveActivity(intent, 0);
        }
        IconCacheEntry entry = mIconCache.getTitleAndIcon(componentName, resolveInfo, labelCache);
        if(entry != null){
            icon = entry.icon;
            info.title = entry.title;
        }

        // the db
        if (icon == null) {
            if (c != null) {
                icon = getIconFromCursor(c, iconIndex, context);
            }
        }
        // the fallback icon
        if (icon == null) {
            icon = getFallbackIcon();
            info.usingFallbackIcon = true;
        }
        info.setIcon(icon);

        // from the resource
//        if (resolveInfo != null) {
//            ComponentName key = Util.getComponentNameFromResolveInfo(resolveInfo);
//            if (labelCache != null && labelCache.containsKey(key)) {
//                info.title = labelCache.get(key);
//            } else {
//                info.title = resolveInfo.activityInfo.loadLabel(manager);
//                if (labelCache != null) {
//                    labelCache.put(key, info.title);
//                }
//            }
//        }
        // from the db
        if (info.title == null) {
            if (c != null) {
                info.title =  c.getString(titleIndex);
            }
        }
        // fall back to the class name of the activity
        if (info.title == null) {
            info.title = componentName.getClassName();
        }
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        return info;
    }

    /**
     * Returns the set of workspace ShortcutInfos with the specified intent.
     */
    public ArrayList<ItemInfo> getWorkspaceShortcutItemInfosWithIntent(Intent intent) {
        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            for (ItemInfo info : sBgWorkspaceItems) {
                if (info instanceof ShortcutInfo) {
                    ShortcutInfo shortcut = (ShortcutInfo) info;
                    if (shortcut.intent.toUri(0).equals(intent.toUri(0))) {
                        items.add(shortcut);
                    }
                }
            }
        }
        return items;
    }

    /**
     * Make an ShortcutInfo object for a shortcut that isn't an application.
     */
    protected ShortcutInfo getShortcutInfo(Cursor c, Context context,
            int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, int iconIndex,
            int titleIndex, ComponentName componentName) {

        Bitmap icon = null;
        final ShortcutInfo info = new ShortcutInfo();
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;

        // TODO: If there's an explicit component and we can't install that, delete it.
        IconCacheEntry entry = mIconCache.getTitleAndIcon(componentName);
        if(entry != null){
            icon = entry.icon;
            info.title = entry.title;
        } 
//        android.util.Log.i("QsLog", "getShortcutInfo()==title:"+info.title
//                +"==dbtitle:"+c.getString(titleIndex)+"=componentName:"+componentName);
        if(TextUtils.isEmpty(info.title)){
            info.title = c.getString(titleIndex);
        }
        
        info.customIcon = false;
        int iconType = c.getInt(iconTypeIndex);
        
        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "getShortcutInfo 00 title="+info.title
                    +", componentName:"+componentName
                    +", iconType:"+iconType);
        }
        
        switch (iconType) {
        case LauncherSettings.Favorites.ICON_TYPE_RESOURCE:            
            if(icon == null || mIconCache.isDefaultIcon(icon)){
                String packageName = c.getString(iconPackageIndex);
                String resourceName = c.getString(iconResourceIndex);
                PackageManager packageManager = context.getPackageManager();
                // the resource
                try {
                    Resources resources = packageManager.getResourcesForApplication(packageName);
                    if (resources != null) {
                        final int id = resources.getIdentifier(resourceName, null, null);
                        if(componentName != null){
                            icon = mIconCache.getIcon(componentName, id);
                        } else {
                            icon = mIconCache.createIconBitmap(packageName, resources, id);
                        }
                        if(icon != null){
                            info.usingFallbackIcon = mIconCache.isDefaultIcon(icon);
                            if(!info.usingFallbackIcon && entry != null && componentName == null){
                                entry.icon = icon;
                            }
                        }
                        
                    }
                } catch (Exception e) {
                    // drop this.  we have other places to look for icons
                }
            }
            if (Util.DEBUG_LOADERS) {
                Util.Log.d(TAG, "getShortcutInfo 22 componentName="+componentName
                        +", icon:"+icon);
            }
            
            // the db
            if (icon == null) {
                icon = getIconFromCursor(c, iconIndex, context);
            } 
            
            if (Util.DEBUG_LOADERS) {
                Util.Log.d(TAG, "getShortcutInfo 33 componentName="+componentName
                        +", icon:"+icon);
            }
            // the fallback icon
            if (icon == null) {
                icon = getFallbackIcon();
                info.usingFallbackIcon = true;
            }
            break;
        case LauncherSettings.Favorites.ICON_TYPE_BITMAP:
            if(icon == null){
                icon = getIconFromCursor(c, iconIndex, context);
                if (icon == null) {
                    icon = getFallbackIcon();
                    info.customIcon = false;
                    info.usingFallbackIcon = true;
                } else {
                    info.customIcon = true;
                }
            }
            break;
        default:
            if(icon == null){
                icon = getFallbackIcon();
                info.usingFallbackIcon = true;
                info.customIcon = false;
            }
            break;
        }
        info.setIcon(icon);
        return info;
    }

    public Bitmap getIconFromCursor(Cursor c, int iconIndex, Context context) {

        if (Util.DEBUG_LOADERS) {
        	Util.Log.d(TAG, "getIconFromCursor app="
                    + c.getString(c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE)));
        }
        byte[] data = c.getBlob(iconIndex);
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
//            return mIconCache.getIconUtilities().createIconBitmap(
//                    BitmapFactory.decodeByteArray(data, 0, data.length));
        } catch (Exception e) {
            return null;
        }
    }
    
    public ShortcutInfo addShortcut(Intent data, long container, int screen,
            int cellX, int cellY, boolean notify) {
        final ShortcutInfo info = infoFromShortcutIntent(mLauncher, data, null);
        if (info == null) {
            return null;
        }
        
        addItemToDatabase(info, container, screen, cellX, cellY, notify);

        return info;
    }

    /**
     * Attempts to find an AppWidgetProviderInfo that matches the given component.
     */
    public AppWidgetProviderInfo findAppWidgetProviderInfoWithComponent(Context context,
            ComponentName component) {
        List<AppWidgetProviderInfo> widgets =
            AppWidgetManager.getInstance(context).getInstalledProviders();
        for (AppWidgetProviderInfo info : widgets) {
            if (info.provider.equals(component)) {
                return info;
            }
        }
        return null;
    }

    /**
     * Returns a list of all the widgets that can handle configuration with a particular mimeType.
     */
    public List<WidgetMimeTypeHandlerData> resolveWidgetsForMimeType(Context context, String mimeType) {
        final PackageManager packageManager = context.getPackageManager();
        final List<WidgetMimeTypeHandlerData> supportedConfigurationActivities =
            new ArrayList<WidgetMimeTypeHandlerData>();

        final Intent supportsIntent =
            new Intent(InstallWidgetReceiver.ACTION_SUPPORTS_CLIPDATA_MIMETYPE);
        supportsIntent.setType(mimeType);

        // Create a set of widget configuration components that we can test against
        final List<AppWidgetProviderInfo> widgets =
            AppWidgetManager.getInstance(context).getInstalledProviders();
        final HashMap<ComponentName, AppWidgetProviderInfo> configurationComponentToWidget =
            new HashMap<ComponentName, AppWidgetProviderInfo>();
        for (AppWidgetProviderInfo info : widgets) {
            configurationComponentToWidget.put(info.configure, info);
        }

        // Run through each of the intents that can handle this type of clip data, and cross
        // reference them with the components that are actual configuration components
        final List<ResolveInfo> activities = packageManager.queryIntentActivities(supportsIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : activities) {
            final ActivityInfo activityInfo = info.activityInfo;
            final ComponentName infoComponent = new ComponentName(activityInfo.packageName,
                    activityInfo.name);
            if (configurationComponentToWidget.containsKey(infoComponent)) {
                supportedConfigurationActivities.add(
                        new InstallWidgetReceiver.WidgetMimeTypeHandlerData(info,
                                configurationComponentToWidget.get(infoComponent)));
            }
        }
        return supportedConfigurationActivities;
    }

    public ShortcutInfo infoFromShortcutIntent(Context context, Intent data, Bitmap fallbackIcon) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = null;//data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

        if (intent == null) {
            // If the intent is null, we can't construct a valid ShortcutInfo, so we return null
        	Util.Log.e(TAG, "Can't construct ShorcutInfo with null intent");
            return null;
        }

        Bitmap icon = null;
        boolean customIcon = false;
        ShortcutIconResource iconResource = null;
        final ComponentName componentName = intent.getComponent();
        if(componentName != null){
            IconCacheEntry entry = mIconCache.getTitleAndIcon(componentName);
            if(entry != null){
                icon = entry.icon;
                name = entry.title;
            }
        }
        
        if(TextUtils.isEmpty(name))
            name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        
        if(icon == null){
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra != null && extra instanceof ShortcutIconResource) {
                try {
                    iconResource = (ShortcutIconResource) extra;
                    final PackageManager packageManager = context.getPackageManager();
                    Resources resources = packageManager.getResourcesForApplication(
                            iconResource.packageName);
                    final int id = resources.getIdentifier(iconResource.resourceName, null, null);
                    if(componentName != null){
                        icon = mIconCache.getIcon(componentName, id);
                    } else if (bitmap != null && bitmap instanceof Bitmap) {
                        icon = mIconCache.createIconBitmap(iconResource.packageName, (Bitmap)bitmap);
                        customIcon = true;
                    } else {
                        icon = mIconCache.createIconBitmap(iconResource.packageName, resources, id);
                    }
                } catch (Exception e) {
                    Util.Log.w(TAG, "Could not load shortcut icon: " + extra);
                }
            } else if (bitmap != null && bitmap instanceof Bitmap) {
                if(componentName != null){
                    icon = mIconCache.createIconBitmap(componentName.getPackageName(), (Bitmap)bitmap);
                } else {
                    icon = mIconCache.getIconUtilities().createIconBitmapWithMask((Bitmap)bitmap);
                }
                customIcon = true;
            }
        }

        final ShortcutInfo info = new ShortcutInfo();

        if (icon == null) {
            if (fallbackIcon != null) {
                icon = fallbackIcon;
            } else {
                icon = getFallbackIcon();
                info.usingFallbackIcon = true;
            }
        }
        info.setIcon(icon);

        info.title = name;
        info.intent = intent;
        info.customIcon = customIcon;
        info.iconResource = iconResource;

        return info;
    }

    public boolean queueIconToBeChecked(HashMap<Object, dbIconTitleCache> cache, ShortcutInfo info, Cursor c,
            int iconIndex, int titleIndex) {
        // If apps can't be on SD, don't even bother.
//        if (!mAppsCanBeOnExternalStorage) {
//            return false;
//        }
        
        // If this icon doesn't have a custom icon, check to see
        // what's stored in the DB, and if it doesn't match what
        // we're going to show, store what we are going to show back
        // into the DB.  We do this so when we're loading, if the
        // package manager can't find an icon (for example because
        // the app is on SD) then we can use that instead.
        if(info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT){
            dbIconTitleCache item = new dbIconTitleCache();
            if (!info.customIcon && !info.usingFallbackIcon) {                
                item.icon = c.getBlob(iconIndex);
            }
            
            item.title = c.getString(titleIndex);
            cache.put(info, item);
            return true;
        }

        return false;
    }
    public void updateSavedIcon(Context context, ShortcutInfo info, dbIconTitleCache data) {
        boolean needSave = false;
        try {
            if (data.icon != null) {
                Bitmap saved = BitmapFactory.decodeByteArray(data.icon, 0, data.icon.length);
                Bitmap loaded = info.getIcon(mIconCache);
                needSave = !saved.sameAs(loaded);
            } else {
                needSave = true;
            }
        } catch (Exception e) {
            needSave = true;
        }
        
        if(data.title == null || !data.title.equals(info.title))
            needSave = true;
        
        if (needSave) {
        	Util.Log.d(TAG, "going to save icon bitmap for info=" + info);
            // This is slower than is ideal, but this only happens once
            // or when the app is updated with a new icon.
            updateItemInDatabase(info);
        }
    }
    
    public Comparator<ApplicationInfo> getApplicationComparator(){
        final int sorttype = mLauncher.getSharedPrefSettingsManager().getInt(SharedPrefSettingsManager.KEY_ALLAPPS_SORT_TYPE, 
        		ResConfigManager.CONFIG_DEFAULT_APP_ICON_SORT_TYPE, 
        		SharedPrefSettingsManager.ALLAPPS_SORT_BY_DEFAULT);
        return getApplicationComparator(sorttype);
    }
    
    private Comparator<ApplicationInfo> getApplicationComparator(int sorttype){
        if(sorttype == SharedPrefSettingsManager.ALLAPPS_SORT_BY_LAUNCHE_TIMES){
            return new ShortcutLauncheTimesComparator();
        } else if(sorttype == SharedPrefSettingsManager.ALLAPPS_SORT_BY_TITLE){
            return new ShortcutTitleComparator();
        } else if(sorttype == SharedPrefSettingsManager.ALLAPPS_SORT_BY_INSTALL_DATE_DESC){
            return new ShortcutInstallDateComparator(false);
        } 
        
        return new ShortcutInstallDateComparator();
    }
    
    protected void sortApplicationItems(List<ResolveInfo> apps, 
            PackageManager packageManager, HashMap<Object, CharSequence> labelCache){
        
        Collections.sort(apps,
                new ShortcutNameComparator(packageManager, labelCache));
    }
    
    protected void sortApplicationItems(int sorttype, List<ApplicationInfo> data, PackageManager packageManager){
        Collections.sort(data, getApplicationComparator(sorttype));
    }
    
    public void sortApplicationItems(List<ApplicationInfo> data){
        Collections.sort(data, getApplicationComparator());
    }
    
    protected static class ShortcutTitleComparator implements Comparator<ApplicationInfo> {
        private Collator mCollator;
        public ShortcutTitleComparator() {
            mCollator = Collator.getInstance();
        }

        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            int result = mCollator.compare(a.title, b.title);
            if (result == 0) {
                result = a.componentName.compareTo(b.componentName);
            }
            return result;
        }
    };
    
    protected static class ShortcutInstallDateComparator implements Comparator<ApplicationInfo> {
        private Collator mCollator;
        private boolean mIsAsc;
        public ShortcutInstallDateComparator() {
            this(true);
        }
        
        public ShortcutInstallDateComparator(boolean asc) {
            mCollator = Collator.getInstance();
            mIsAsc = asc;
        }

        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            if(a.firstInstallTime > b.firstInstallTime)
                return (mIsAsc ? 1 : -1);
            
            if(a.firstInstallTime < b.firstInstallTime)
                return (mIsAsc ? -1 : 1);
            
            return mCollator.compare(a.title, b.title);
        }
    };
    
    protected static class ShortcutLauncheTimesComparator implements Comparator<ApplicationInfo> {
        private Collator mCollator;
        public ShortcutLauncheTimesComparator() {
            mCollator = Collator.getInstance();
        }

        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            int ret = a.launchedFreq - b.launchedFreq;
            if(ret == 0)
                return mCollator.compare(a.title, b.title);
            return ret;
        }
    };
    
    protected static class ShortcutNameComparator implements Comparator<ResolveInfo> {
        private Collator mCollator;
        private PackageManager mPackageManager;
        private HashMap<Object, CharSequence> mLabelCache;
        public ShortcutNameComparator(PackageManager pm) {
            mPackageManager = pm;
            mLabelCache = new HashMap<Object, CharSequence>();
            mCollator = Collator.getInstance();
        }
        public ShortcutNameComparator(PackageManager pm, HashMap<Object, CharSequence> labelCache) {
            mPackageManager = pm;
            mLabelCache = labelCache;
            mCollator = Collator.getInstance();
        }
        public final int compare(ResolveInfo a, ResolveInfo b) {
            CharSequence labelA, labelB;
            ComponentName keyA = Util.getComponentNameFromResolveInfo(a);
            ComponentName keyB = Util.getComponentNameFromResolveInfo(b);
            if (mLabelCache.containsKey(keyA)) {
                labelA = mLabelCache.get(keyA);
            } else {
                labelA = a.loadLabel(mPackageManager).toString();

                mLabelCache.put(keyA, labelA);
            }
            if (mLabelCache.containsKey(keyB)) {
                labelB = mLabelCache.get(keyB);
            } else {
                labelB = b.loadLabel(mPackageManager).toString();

                mLabelCache.put(keyB, labelB);
            }
            return mCollator.compare(labelA, labelB);
        }
    };

    /**
     * Return an existing FolderInfo object if we have encountered this ID previously,
     * or make a new one.
     */
    public static FolderInfo findOrMakeFolder(HashMap<Long, FolderInfo> folders, long id) {
    	return findOrMakeFolder(folders, id, null, null);
    }
    public static FolderInfo findOrMakeFolder(HashMap<Long, FolderInfo> folders, long id, String packageName, String className) {
        FolderInfo folderInfo = folders.get(id);
        if (folderInfo == null) {
//            android.util.Log.w("QsLog", "findOrMakeFolder()==packageName:"+packageName
//                    +"==className:"+className);
            // No placeholder -- create a new instance
            folderInfo = new FolderInfo(packageName, className);
            folders.put(id, folderInfo);
        }
        return folderInfo;
    }

    public static final Comparator<ApplicationInfo> getAppNameComparator() {
        final Collator collator = Collator.getInstance();
        return new Comparator<ApplicationInfo>() {
            public final int compare(ApplicationInfo a, ApplicationInfo b) {
                int result = collator.compare(a.title, b.title);
                if (result == 0) {
                    result = a.componentName.compareTo(b.componentName);
                }
                return result;
            }
        };
    }
    public static final Comparator<ApplicationInfo> APP_INSTALL_TIME_COMPARATOR
            = new Comparator<ApplicationInfo>() {
        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            if (a.firstInstallTime < b.firstInstallTime) return 1;
            if (a.firstInstallTime > b.firstInstallTime) return -1;
            return Collator.getInstance().compare(a.title, b.title);
        }
    };
    public static final Comparator<AppWidgetProviderInfo> getWidgetNameComparator() {
        final Collator collator = Collator.getInstance();
        return new Comparator<AppWidgetProviderInfo>() {
            public final int compare(AppWidgetProviderInfo a, AppWidgetProviderInfo b) {
                return collator.compare(a.label.toString(), b.label.toString());
            }
        };
    }
//    public static ComponentName getComponentNameFromResolveInfo(ResolveInfo info) {
//        if (info.activityInfo != null) {
//            return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
//        } else {
//            return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
//        }
//    }
    
    
    public static class WidgetAndShortcutNameComparator implements Comparator<Object> {
        private Collator mCollator;
        private PackageManager mPackageManager;
        private HashMap<Object, String> mLabelCache;
        public WidgetAndShortcutNameComparator(PackageManager pm) {
            mPackageManager = pm;
            mLabelCache = new HashMap<Object, String>();
            mCollator = Collator.getInstance();
        }
        public final int compare(Object a, Object b) {
            String labelA, labelB;
            if (mLabelCache.containsKey(a)) {
                labelA = mLabelCache.get(a);
            } else {
                labelA = (a instanceof AppWidgetProviderInfo) ?
                    ((AppWidgetProviderInfo) a).label :
                    ((ResolveInfo) a).loadLabel(mPackageManager).toString();
                mLabelCache.put(a, labelA);
            }
            if (mLabelCache.containsKey(b)) {
                labelB = mLabelCache.get(b);
            } else {
                labelB = (b instanceof AppWidgetProviderInfo) ?
                    ((AppWidgetProviderInfo) b).label :
                    ((ResolveInfo) b).loadLabel(mPackageManager).toString();
                mLabelCache.put(b, labelB);
            }
            return mCollator.compare(labelA, labelB);
        }
    };

    public void dumpState() {
    	Util.Log.d(TAG, "mCallbacks=" + mCallbacks);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.data", mBgAllAppsList.data);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.added", mBgAllAppsList.added);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.removed", mBgAllAppsList.removed);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.modified", mBgAllAppsList.modified);
        if (mLoaderTask != null) {
            mLoaderTask.dumpState();
        } else {
        	Util.Log.d(TAG, "mLoaderTask=null");
        }
    }
    
    /**
     * M: Set flush cache.
     */
    public synchronized void setFlushCache() {
    	Util.Log.d(TAG, "Set flush cache flag for locale changed.");
        mForceFlushCache = true;
    }
    
    /**
     * M: Flush icon cache and label cache if locale has been changed.
     * 
     * @param labelCache label cache.
     */
    public synchronized void flushCacheIfNeeded(HashMap<Object, CharSequence> labelCache) {
        if (Util.DEBUG_LOADERS) {
        	Util.Log.d(TAG, "flushCacheIfNeeded: sForceFlushCache = " + mForceFlushCache
                    + ", mLoaderTask = " + mLoaderTask + ", labelCache = " + labelCache);
        }        
        if (mForceFlushCache) {
            labelCache.clear();
            mIconCache.flush();
            mForceFlushCache = false;
        }   
    }

    public List<ApplicationInfo> getAllAppsList() {
        return (List<ApplicationInfo>)mBgAllAppsList.data.clone();
    }

    /** Filters the set of items who are directly or indirectly (via another container) on the
     * specified screen. */
    protected void filterCurrentWorkspaceItems(int currentScreen,
            ArrayList<ItemInfo> allWorkspaceItems,
            ArrayList<ItemInfo> currentScreenItems,
            ArrayList<ItemInfo> otherScreenItems) {
        // Purge any null ItemInfos
        Iterator<ItemInfo> iter = allWorkspaceItems.iterator();
        while (iter.hasNext()) {
            ItemInfo i = iter.next();
            if (i == null) {
                iter.remove();
            }
        }

        // If we aren't filtering on a screen, then the set of items to load is the full set of
        // items given.
        if (currentScreen < 0) {
            currentScreenItems.addAll(allWorkspaceItems);
        }

        // Order the set of items by their containers first, this allows use to walk through the
        // list sequentially, build up a list of containers that are in the specified screen,
        // as well as all items in those containers.
        Set<Long> itemsOnScreen = new HashSet<Long>();
        Collections.sort(allWorkspaceItems, new Comparator<ItemInfo>() {
            @Override
            public int compare(ItemInfo lhs, ItemInfo rhs) {
                return (int) (lhs.container - rhs.container);
            }
        });
        for (ItemInfo info : allWorkspaceItems) {
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                if (info.screen == currentScreen) {
                    currentScreenItems.add(info);
                    itemsOnScreen.add(info.id);
                } else {
                    otherScreenItems.add(info);
                }
            } else if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                currentScreenItems.add(info);
                itemsOnScreen.add(info.id);
            } else {
                if (itemsOnScreen.contains(info.container)) {
                    currentScreenItems.add(info);
                    itemsOnScreen.add(info.id);
                } else {
                    otherScreenItems.add(info);
                }
            }
        }
    }

    /** Filters the set of widgets which are on the specified screen. */
    protected void filterCurrentAppWidgets(int currentScreen,
            ArrayList<LauncherAppWidgetInfo> appWidgets,
            ArrayList<LauncherAppWidgetInfo> currentScreenWidgets,
            ArrayList<LauncherAppWidgetInfo> otherScreenWidgets) {
        // If we aren't filtering on a screen, then the set of items to load is the full set of
        // widgets given.
        if (currentScreen < 0) {
            currentScreenWidgets.addAll(appWidgets);
        }

        for (LauncherAppWidgetInfo widget : appWidgets) {
            if (widget == null) continue;
            if (widget.container == LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                    widget.screen == currentScreen) {
                currentScreenWidgets.add(widget);
            } else {
                otherScreenWidgets.add(widget);
            }
        }
    }

    /** Filters the set of folders which are on the specified screen. */
    protected void filterCurrentFolders(int currentScreen,
            HashMap<Long, ItemInfo> itemsIdMap,
            HashMap<Long, FolderInfo> folders,
            HashMap<Long, FolderInfo> currentScreenFolders,
            HashMap<Long, FolderInfo> otherScreenFolders) {
        // If we aren't filtering on a screen, then the set of items to load is the full set of
        // widgets given.
        if (currentScreen < 0) {
            currentScreenFolders.putAll(folders);
        }

        for (long id : folders.keySet()) {
            ItemInfo info = itemsIdMap.get(id);
            FolderInfo folder = folders.get(id);
            if (info == null || folder == null) continue;
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                    info.screen == currentScreen) {
                currentScreenFolders.put(id, folder);
            } else {
                otherScreenFolders.put(id, folder);
            }
        }
    }
    
//    protected void sortApplicationItems(List<ResolveInfo> apps, 
//    		PackageManager packageManager, HashMap<Object, CharSequence> labelCache){
//    	
//    	Collections.sort(apps,
//                new ShortcutNameComparator(packageManager, labelCache));
//    }
    
    protected ApplicationInfo createApplicationInfo(ResolveInfo app, 
    		PackageManager packageManager, HashMap<Object, CharSequence> labelCache){
    	ApplicationInfo info = new ApplicationInfo(packageManager, app,
                mIconCache, labelCache);
    	
//    	android.util.Log.i("QsLog", "icon:"+info.iconBitmap
//    			+", title:"+info.title);
    	//if(info.iconBitmap == null || TextUtils.isEmpty(info.title))
    	//	return null;
    	return info;
    }
    
    protected void bindWorkspaceItems(final ILauncherModelCallbacks oldCallbacks,
            final ArrayList<ItemInfo> workspaceItems,
            final ArrayList<LauncherAppWidgetInfo> appWidgets,
            final HashMap<Long, FolderInfo> folders,
            ArrayList<Runnable> deferredBindRunnables,
            final LoaderTask task) {

        final boolean postOnMainThread = (deferredBindRunnables != null);

        // Bind the workspace items
        int N = workspaceItems.size();
        for (int i = 0; i < N; i += ITEMS_CHUNK) {
            final int start = i;
            final int chunkSize = (i+ITEMS_CHUNK <= N) ? ITEMS_CHUNK : (N-i);
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                	ILauncherModelCallbacks callbacks = task.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindItems(workspaceItems, start, start+chunkSize);
                    }
                }
            };
            if (postOnMainThread) {
                deferredBindRunnables.add(r);
            } else {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
        }

        // Bind the folders
        if (!folders.isEmpty()) {
            final Runnable r = new Runnable() {
                public void run() {
                	ILauncherModelCallbacks callbacks = task.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindFolders(folders);
                    }
                }
            };
            if (postOnMainThread) {
                deferredBindRunnables.add(r);
            } else {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
        }

        // Bind the widgets, one at a time
        N = appWidgets.size();
        for (int i = 0; i < N; i++) {
            final LauncherAppWidgetInfo widget = appWidgets.get(i);
            final Runnable r = new Runnable() {
                public void run() {
                	ILauncherModelCallbacks callbacks = task.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindAppWidget(widget);
                    }
                }
            };
            if (postOnMainThread) {
                deferredBindRunnables.add(r);
            } else {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
        }
    }

    /** Sorts the set of items by hotseat, workspace (spatially from top to bottom, left to
     * right) */
    protected void sortWorkspaceItemsSpatially(ArrayList<ItemInfo> workspaceItems) {
        // XXX: review this
        Collections.sort(workspaceItems, new Comparator<ItemInfo>() {
            @Override
            public int compare(ItemInfo lhs, ItemInfo rhs) {
//                int cellCountX = LauncherModel.getCellCountX();
//                int cellCountY = LauncherModel.getCellCountY();
//                int screenOffset = cellCountX * cellCountY;
//                int containerOffset = screenOffset * (Launcher.SCREEN_COUNT + 1); // +1 hotseat
//                long lr = (lhs.container * containerOffset + lhs.screen * screenOffset +
//                        lhs.cellY * cellCountX + lhs.cellX);
//                long rr = (rhs.container * containerOffset + rhs.screen * screenOffset +
//                        rhs.cellY * cellCountX + rhs.cellX);
//                return (int) (lr - rr);
            	
            	return 0;
            }
        });
    }
    
    protected boolean isLoadWorkspaceFirst(ILauncherModelCallbacks cbk){
    	return cbk != null ? (!cbk.isAllAppsVisible()) : true;
    }
    
    protected void loadAllAppsByBatchInternal(final LoaderTask task, Context context, 
    		final AllAppsList bgAllAppsList, HashMap<Object, CharSequence> labelCache){
    	
    	final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> apps = null;

        int N = Integer.MAX_VALUE;
        
		final int sorttype = mLauncher.getSharedPrefSettingsManager().getInt(SharedPrefSettingsManager.KEY_ALLAPPS_SORT_TYPE, 
				ResConfigManager.CONFIG_DEFAULT_APP_ICON_SORT_TYPE, 
				SharedPrefSettingsManager.ALLAPPS_SORT_BY_DEFAULT);
		
		final List<ComponentName> ignorelist =  bgAllAppsList.getIgnoreAppList(context, mLauncher.getResConfigManager());
		
        int startIndex;
        int i = 0;
        int batchSize = -1;
        while (i < N && !task.isStoped()) {
            if (i == 0) {
            	bgAllAppsList.clear();
                final long qiaTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
                apps = packageManager.queryIntentActivities(mainIntent, 0);
                if (DEBUG_LOADERS) {
                    Log.d(TAG, "queryIntentActivities took "
                            + (SystemClock.uptimeMillis() - qiaTime) + "ms");
                }
                if (apps == null) {
                    return;
                }
                N = apps.size();
                if (DEBUG_LOADERS) {
                    Log.d(TAG, "queryIntentActivities got " + N + " apps, mBatchSize = "
                            + mBatchSize + ", this = " + this);
                }
                if (N == 0) {
                    // There are no apps?!?
                    return;
                }
                if (mBatchSize == 0) {
                    batchSize = N;
                } else {
                    batchSize = mBatchSize;
                }

		        /*
		         * M: If locale changed, we need to clear icon cache and label
		         * cache before we get the right label cache, this can make
		         * sure the next step to add application to list will cache
		         * the right label.
		         */
		        flushCacheIfNeeded(labelCache);
		        final long sortTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
		        if(sorttype == SharedPrefSettingsManager.ALLAPPS_SORT_BY_TITLE){
		            sortApplicationItems(apps, packageManager, labelCache);
		        }
		//        Collections.sort(apps,
		//                new LauncherModel.ShortcutNameComparator(packageManager, mLabelCache));
		        if (DEBUG_LOADERS) {
		            Log.w(TAG, "sort took " + (SystemClock.uptimeMillis() - sortTime) + "ms"
		                    + ", this = " + this);
		        }
			}
        
            final long t2 = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

            startIndex = i;
            for (int j=0; i<N && j<batchSize; j++) {
                // This builds the icon bitmaps.
            	final ApplicationInfo info = createApplicationInfo(apps.get(i), packageManager, 
            			labelCache);
            	if(info != null && !ignorelist.contains(info.componentName)){            		
            		
            		bgAllAppsList.add(info);
            		
            		if (DEBUG_LOADERS) {
            			Util.Log.i(TAG, info.toString());
            		}
            	}
                i++;
            }
			
//			if(sorttype != SharedPrefSettingsManager.ALLAPPS_SORT_BY_TITLE){
//	            sortApplicationItems(sorttype, bgAllAppsList.added, packageManager);
//	        }

//            if (!LauncherExtPlugin.getAllAppsListExt(mApp).isShowWifiSettings()) {
//                mBgAllAppsList.removeWifiSettings();
//            }
//
//            mBgAllAppsList.reorderApplist();

            final boolean first = i <= batchSize;
            final ILauncherModelCallbacks callbacks = mCallbacks.get();//tryGetCallbacks(oldCallbacks);
            final ArrayList<ApplicationInfo> added = bgAllAppsList.added;
            bgAllAppsList.added = new ArrayList<ApplicationInfo>();

            mHandler.post(new Runnable() {
                public void run() {
                    final long t = SystemClock.uptimeMillis();
                    if (callbacks != null) {
                        if (first) {
                            callbacks.bindAllApplications(added);
                        } else {
                            callbacks.bindAppsAdded(added);
                        }
                        if (DEBUG_LOADERS) {
                            Log.d(TAG, "bound " + added.size() + " apps in "
                                + (SystemClock.uptimeMillis() - t) + "ms");
                        }
                    } else {
                        Log.i(TAG, "not binding apps: no Launcher activity");
                    }
                }
            });

            if (DEBUG_LOADERS) {
                Log.d(TAG, "batch of " + (i-startIndex) + " icons processed in "
                        + (SystemClock.uptimeMillis()-t2) + "ms");
            }

            if (mAllAppsLoadDelay > 0 && i < N) {
                try {
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "sleeping for " + mAllAppsLoadDelay + "ms");
                    }
                    Thread.sleep(mAllAppsLoadDelay);
                } catch (InterruptedException exc) { }
            }
        }
        
//        if(!task.isStoped()){
//            sortApplicationItems(sorttype, bgAllAppsList.data, packageManager);
//        }

//        if (DEBUG_LOADERS) {
//            Log.d(TAG, "cached all " + N + " apps in "
//                    + (SystemClock.uptimeMillis() - t) + "ms"
//                    + (mAllAppsLoadDelay > 0 ? " (including delay)" : ""));
//        }
    }

    protected void loadWorkspaceInternal(final LoaderTask task, Context context, 
    		ArrayList<Long> itemsToRemove, HashMap<Object, CharSequence> labelCache){
    	
    	//final Context context = mContext;
        final ContentResolver contentResolver = context.getContentResolver();
        final PackageManager manager = context.getPackageManager();
        final AppWidgetManager widgets = AppWidgetManager.getInstance(context);
        final boolean isSafeMode = manager.isSafeMode();
        
    	/// M: modified for scene feature, query all items from db
        /// if the scene values of the item equals the current scene.
        final Cursor c = contentResolver.query(LauncherSettings.Favorites.CONTENT_URI,
        		null, null, null, null);

        // +1 for the hotseat (it can be larger than the workspace)
        // Load workspace in reverse order to ensure that latest items are loaded first (and
        // before any earlier duplicates)
        final int screenCount = getMaxWorkspaceScreenCount();
        final int cellCountX = getMaxCellCountX();
        final int cellCountY = getMaxCellCountY();
        final ItemInfo occupied[][][] =
                new ItemInfo[screenCount + 1][cellCountX + 1][cellCountY + 1];

        try {
            final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
            final int intentIndex = c.getColumnIndexOrThrow
                    (LauncherSettings.Favorites.INTENT);
            final int titleIndex = c.getColumnIndexOrThrow
                    (LauncherSettings.Favorites.TITLE);
            final int iconTypeIndex = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.ICON_TYPE);
            final int iconIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
            final int iconPackageIndex = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.ICON_PACKAGE);
            final int iconResourceIndex = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.ICON_RESOURCE);
            final int containerIndex = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.CONTAINER);
            final int itemTypeIndex = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.ITEM_TYPE);
            final int appWidgetIdIndex = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.APPWIDGET_ID);
            final int screenIndex = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.SCREEN);
            final int cellXIndex = c.getColumnIndexOrThrow
                    (LauncherSettings.Favorites.CELLX);
            final int cellYIndex = c.getColumnIndexOrThrow
                    (LauncherSettings.Favorites.CELLY);
            final int spanXIndex = c.getColumnIndexOrThrow
                    (LauncherSettings.Favorites.SPANX);
            final int spanYIndex = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.SPANY);
            
            //final int freqIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.LAUNCH_FREQ);
            //final int lastTimeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.LAST_LAUNCH_TIME);
            //final int uriIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.URI);
            //final int displayModeIndex = c.getColumnIndexOrThrow(
            //        LauncherSettings.Favorites.DISPLAY_MODE);

            ShortcutInfo info;
            String intentDescription;
            LauncherAppWidgetInfo appWidgetInfo;
            int container;
            long id;
            Intent intent;

            while (!task.isStoped() && c.moveToNext()) {
                try {
                    int itemType = c.getInt(itemTypeIndex);

                    switch (itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                        intentDescription = c.getString(intentIndex);
                        try {
                            intent = Intent.parseUri(intentDescription, 0);
                        } catch (URISyntaxException e) {
                            continue;
                        }

                        if (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                            info = getShortcutInfo(manager, intent, context, c, iconIndex,
                                    titleIndex, labelCache);
                        } else {
                            info = getShortcutInfo(c, context, iconTypeIndex,
                                    iconPackageIndex, iconResourceIndex, iconIndex,
                                    titleIndex, intent.getComponent());

                            // App shortcuts that used to be automatically added to Launcher
                            // didn't always have the correct intent flags set, so do that
                            // here
                            if (intent.getAction() != null &&
                                intent.getCategories() != null &&
                                intent.getAction().equals(Intent.ACTION_MAIN) &&
                                intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                                intent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            }
                        }

                        if (info != null) {
                            info.intent = intent;
                            info.id = c.getLong(idIndex);
                            container = c.getInt(containerIndex);
                            info.container = container;
                            info.screen = c.getInt(screenIndex);
                            info.cellX = c.getInt(cellXIndex);
                            info.cellY = c.getInt(cellYIndex);

                            // check & update map of what's occupied
                            if (!checkItemPlacement(occupied, info, screenCount)) {
                                break;
                            }

                            switch (container) {
                            case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                            case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                sBgWorkspaceItems.add(info);
                                break;
                            default:
                                // Item is in a user folder
                                FolderInfo folderInfo =
                                        findOrMakeFolder(sBgFolders, container);
                                folderInfo.add(info);
                                break;
                            }
                            sBgItemsIdMap.put(info.id, info);

                            // now that we've loaded everthing re-save it with the
                            // icon in case it disappears somehow.
                            queueIconToBeChecked(sBgDbIconCache, info, c, iconIndex, titleIndex);
                        } else {
                            // Failed to load the shortcut, probably because the
                            // activity manager couldn't resolve it (maybe the app
                            // was uninstalled), or the db row was somehow screwed up.
                            // Delete it.
                            id = c.getLong(idIndex);
                            Log.e(TAG, "Error loading shortcut " + id + ", removing it");
                            contentResolver.delete(LauncherSettings.Favorites.getContentUri(
                                        id, false), null, null);
                        }
                        break;

                    case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                        id = c.getLong(idIndex);
//                        android.util.Log.w("QsLog", "loadWorkspaceInternal()==packageName:"+c.getString(iconPackageIndex)
//                                +"==className:"+c.getString(iconResourceIndex));
                        FolderInfo folderInfo = findOrMakeFolder(sBgFolders, id, 
                                c.getString(iconPackageIndex),
                                c.getString(iconResourceIndex));

                        folderInfo.title = c.getString(titleIndex);
                        folderInfo.id = id;
                        container = c.getInt(containerIndex);
                        folderInfo.container = container;
                        folderInfo.screen = c.getInt(screenIndex);
                        folderInfo.cellX = c.getInt(cellXIndex);
                        folderInfo.cellY = c.getInt(cellYIndex);

                        // check & update map of what's occupied
                        if (!checkItemPlacement(occupied, folderInfo, screenCount)) {
                            break;
                        }
                        switch (container) {
                            case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                            case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                sBgWorkspaceItems.add(folderInfo);
                                break;
                        }
                        sBgItemsIdMap.put(folderInfo.id, folderInfo);
                        sBgFolders.put(folderInfo.id, folderInfo);
                        if (Util.ENABLE_DEBUG) {
                        	Util.Log.d(TAG, "loadWorkspace sBgItemsIdMap.put = " + folderInfo);
                        }
                        break;
                    case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                        // Read all Launcher-specific widget details
                        int appWidgetId = c.getInt(appWidgetIdIndex);
                        id = c.getLong(idIndex);

                        final AppWidgetProviderInfo provider =
                                widgets.getAppWidgetInfo(appWidgetId);

                        if (!isSafeMode && (provider == null || provider.provider == null ||
                                provider.provider.getPackageName() == null)) {
                            String log = "Deleting widget that isn't installed anymore: id="
                                + id + " appWidgetId=" + appWidgetId;
                            Log.e(TAG, log);
                            //Launcher.sDumpLogs.add(log);
                            itemsToRemove.add(id);
                        } else {
                            appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId,
                                    provider.provider);
                            appWidgetInfo.id = id;
                            appWidgetInfo.screen = c.getInt(screenIndex);
                            appWidgetInfo.cellX = c.getInt(cellXIndex);
                            appWidgetInfo.cellY = c.getInt(cellYIndex);
                            appWidgetInfo.spanX = c.getInt(spanXIndex);
                            appWidgetInfo.spanY = c.getInt(spanYIndex);
                            int[] minSpan = Launcher.getMinSpanForWidget(mLauncher, provider);
                            appWidgetInfo.minSpanX = minSpan[0];
                            appWidgetInfo.minSpanY = minSpan[1];
                            appWidgetInfo.minWidth = provider.minWidth;
                            appWidgetInfo.minHeight = provider.minHeight;

                            container = c.getInt(containerIndex);
                            if (container != LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                                container != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                                Log.e(TAG, "Widget found where container != " +
                                    "CONTAINER_DESKTOP nor CONTAINER_HOTSEAT - ignoring!");
                                continue;
                            }
                            appWidgetInfo.container = c.getInt(containerIndex);

                            // check & update map of what's occupied
                            if (!checkItemPlacement(occupied, appWidgetInfo, screenCount)) {
                                break;
                            }
                            sBgItemsIdMap.put(appWidgetInfo.id, appWidgetInfo);
                            sBgAppWidgets.add(appWidgetInfo);
                        }
                        break;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Desktop items loading interrupted:", e);
                }
            }
        } finally {
            c.close();
        }
    }
    
 // check & update map of what's occupied; used to discard overlapping/invalid items
    protected boolean checkItemPlacement(ItemInfo occupied[][][], ItemInfo item, int screenCount) {
        int containerIndex = item.screen;
        if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            // Return early if we detect that an item is under the hotseat button
            if (mCallbacks == null || mCallbacks.get().isAllAppsButtonRank(item.screen)) {
            	Util.Log.e(TAG, "Error loading shortcut into hotseat " + item
                        + " into AllAppsButtonRank position ");
                return false;
            }

            // We use the last index to refer to the hotseat and the screen as the rank, so
            // test and update the occupied state accordingly
            if (occupied[screenCount][item.screen][0] != null) {
                Util.Log.e(TAG, "Error loading shortcut into hotseat " + item
                    + " into position (" + item.screen + ":" + item.cellX + "," + item.cellY
                    + ") occupied by " + occupied[screenCount][item.screen][0]);
                return false;
            } else {
                occupied[screenCount][item.screen][0] = item;
                return true;
            }
        } else if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            // Skip further checking if it is not the hotseat or workspace container
            return true;
        }

        // Check if any workspace icons overlap with each other
        for (int x = item.cellX; x < (item.cellX + item.spanX); x++) {
            for (int y = item.cellY; y < (item.cellY + item.spanY); y++) {
                if (occupied[containerIndex][x][y] != null) {
                	Util.Log.e(TAG, "Error loading shortcut " + item
                        + " into cell (" + containerIndex + "-" + item.screen + ":"
                        + x + "," + y
                        + ") occupied by "
                        + occupied[containerIndex][x][y]);
                    return false;
                }
            }
        }
        for (int x = item.cellX; x < (item.cellX + item.spanX); x++) {
            for (int y = item.cellY; y < (item.cellY + item.spanY); y++) {
                occupied[containerIndex][x][y] = item;
            }
        }

        return true;
    }
/**
     * Runnable for the thread that loads the contents of the launcher:
     *   - workspace icons
     *   - widgets
     *   - all apps icons
     */
    public class LoaderTask implements Runnable {
        private Context mContext;
        private boolean mIsLaunching;
        private boolean mIsLoadingAndBindingWorkspace;
        private boolean mStopped;
        private boolean mLoadAndBindStepFinished;
        private boolean mIsLoadAndBindApps;

        private HashMap<Object, CharSequence> mLabelCache;
        public LoaderTask(Context context, boolean isLaunching) {
            this(context, isLaunching, true);
        }
        
        public LoaderTask(Context context, boolean isLaunching, boolean bindApps) {
            mContext = context;
            mIsLaunching = isLaunching;
            mIsLoadAndBindApps = bindApps;
            mLabelCache = new HashMap<Object, CharSequence>();
            if (DEBUG_LOADERS) {
                Util.Log.d(TAG, "LoaderTask construct: mLabelCache = " + mLabelCache +
                        ", mIsLaunching = " + mIsLaunching + ", this = " + this);
            }
        }
        
        public boolean isStoped(){
        	return mStopped;
        }

        public boolean isLaunching() {
            return mIsLaunching;
        }

        public boolean isLoadingWorkspace() {
            return mIsLoadingAndBindingWorkspace;
        }

        private void loadAndBindWorkspace() {
            mIsLoadingAndBindingWorkspace = true;

            // Load the workspace
            if (DEBUG_LOADERS) {
                Log.d(TAG, "loadAndBindWorkspace mWorkspaceLoaded=" + mWorkspaceLoaded);
            }

            if (!mWorkspaceLoaded) {
                loadWorkspace();
                synchronized (LoaderTask.this) {
                    if (mStopped) {
                        Util.Log.d(TAG, "loadAndBindWorkspace returned by stop flag.");
                        return;
                    }
                    mWorkspaceLoaded = true;
                }
            }

            // Bind the workspace
            bindWorkspace(-1);
        }

        private void waitForIdle() {
            // Wait until the either we're stopped or the other threads are done.
            // This way we don't start loading all apps until the workspace has settled
            // down.
            synchronized (LoaderTask.this) {
                final long workspaceWaitTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
                if (DEBUG_LOADERS) {
                    Log.d(TAG, "waitForIdle start, workspaceWaitTime : " + workspaceWaitTime + "ms, Thread priority :"
                            + Thread.currentThread().getPriority() + ", this = " + this);
                }

                mHandler.postIdle(new Runnable() {
                        public void run() {
                            synchronized (LoaderTask.this) {
                                mLoadAndBindStepFinished = true;
                                if (DEBUG_LOADERS) {
                                    Log.d(TAG, "done with previous binding step");
                                }
                                LoaderTask.this.notify();
                            }
                        }
                    });

                while (!mStopped && !mLoadAndBindStepFinished) {
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                        // Ignore
                    }
                }
                if (DEBUG_LOADERS) {
                    Log.d(TAG, "waited " + (SystemClock.uptimeMillis() - workspaceWaitTime)
                            + "ms for previous step to finish binding, mStopped = " + mStopped
                            + ",mLoadAndBindStepFinished = " + mLoadAndBindStepFinished);
                }
            }
        }
        
        void runBindSynchronousPage(int synchronousBindPage) {
            runBindSynchronousPage(synchronousBindPage, true);
        }

        void runBindSynchronousPage(int synchronousBindPage, boolean bindApps) {
            if (Util.ENABLE_DEBUG) {
                Util.Log.d(TAG, "runBindSynchronousPage: mAllAppsLoaded = " + mAllAppsLoaded
                        + ",mWorkspaceLoaded = " + mWorkspaceLoaded + ",synchronousBindPage = "
                        + synchronousBindPage + ",mIsLoaderTaskRunning = " + mIsLoaderTaskRunning
                        + ",mStopped = " + mStopped + ",this = " + this);
            }

            if (synchronousBindPage < 0) {
                // Ensure that we have a valid page index to load synchronously
                throw new RuntimeException("Should not call runBindSynchronousPage() without " +
                        "valid page index");
            }
            if (!mAllAppsLoaded || !mWorkspaceLoaded) {
                // Ensure that we don't try and bind a specified page when the pages have not been
                // loaded already (we should load everything asynchronously in that case)
                throw new RuntimeException("Expecting AllApps and Workspace to be loaded");
            }
            synchronized (mLock) {
                if (mIsLoaderTaskRunning) {
                    // Ensure that we are never running the background loading at this point since
                    // we also touch the background collections
                    throw new RuntimeException("Error! Background loading is already running");
                }
            }

            // XXX: Throw an exception if we are already loading (since we touch the worker thread
            //      data structures, we can't allow any other thread to touch that data, but because
            //      this call is synchronous, we can get away with not locking).

            // The LauncherModel is static in the LauncherApplication and mHandler may have queued
            // operations from the previous activity.  We need to ensure that all queued operations
            // are executed before any synchronous binding work is done.
            mHandler.flush();

            // Divide the set of loaded items into those that we are binding synchronously, and
            // everything else that is to be bound normally (asynchronously).
            bindWorkspace(synchronousBindPage);
            // XXX: For now, continue posting the binding of AllApps as there are other issues that
            //      arise from that.
            if(bindApps)
                onlyBindAllApps();
        }

        public void run() {
            synchronized (mLock) {
                if (DEBUG_LOADERS) {
                    Util.Log.d(TAG, "Set load task running flag >>>>, mIsLaunching = " +
                            mIsLaunching + ",this = " + this);
                }
                mIsLoaderTaskRunning = true;
            }
            // Optimize for end-user experience: if the Launcher is up and // running with the
            // All Apps interface in the foreground, load All Apps first. Otherwise, load the
            // workspace first (default).
            final ILauncherModelCallbacks cbk = mCallbacks.get();
            final boolean loadWorkspaceFirst = (!mIsLoadAndBindApps || isLoadWorkspaceFirst(cbk));// != null ? (!cbk.isAllAppsVisible()) : true;

            keep_running: {
                // Elevate priority when Home launches for the first time to avoid
                // starving at boot time. Staring at a blank home is not cool.
                synchronized (mLock) {
                    if (DEBUG_LOADERS) Log.d(TAG, "Setting thread priority to " +
                            (mIsLaunching ? "DEFAULT" : "BACKGROUND"));
                    android.os.Process.setThreadPriority(mIsLaunching
                            ? Process.THREAD_PRIORITY_DEFAULT : Process.THREAD_PRIORITY_BACKGROUND);
                }
                if (loadWorkspaceFirst) {
                    if (DEBUG_LOADERS) Log.d(TAG, "step 1: loading workspace this = " + this);
                    loadAndBindWorkspace();
                } else {
                    if (DEBUG_LOADERS) Log.d(TAG, "step 1: special: loading all apps this = " + this);
                    loadAndBindAllApps();
                }

                if (mStopped) {
                    Util.Log.i(TAG, "LoadTask break in the middle, this = " + this);
                    break keep_running;
                }

                // Whew! Hard work done.  Slow us down, and wait until the UI thread has
                // settled down.
                synchronized (mLock) {
                    if (mIsLaunching) {
                        if (DEBUG_LOADERS) Log.d(TAG, "Setting thread priority to BACKGROUND");
                        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    }
                }
                waitForIdle();

                // second step
                if (loadWorkspaceFirst) {
                    if (DEBUG_LOADERS) Log.d(TAG, "step 2: loading all apps this = " + this);
                    if(mIsLoadAndBindApps)
                        loadAndBindAllApps();
                } else {
                    if (DEBUG_LOADERS) Log.d(TAG, "step 2: special: loading workspace this = " + this);
                    loadAndBindWorkspace();
                }

                // Restore the default thread priority after we are done loading items
                synchronized (mLock) {
                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                }
            }


            // Update the saved icons if necessary
            if (DEBUG_LOADERS) Log.d(TAG, "Comparing loaded icons to database icons");
            synchronized (sBgLock) {
                for (Object key : sBgDbIconCache.keySet()) {
                    updateSavedIcon(mContext, (ShortcutInfo) key, sBgDbIconCache.get(key));
                }
                sBgDbIconCache.clear();
            }

            // Clear out this reference, otherwise we end up holding it until all of the
            // callback runnables are done.
            mContext = null;

            synchronized (mLock) {
                // If we are still the last one to be scheduled, remove ourselves.
                if (mLoaderTask == this) {
                    mLoaderTask = null;
                }
                if (DEBUG_LOADERS) {
                    Util.Log.d(TAG, "Reset load task running flag <<<<, this = " + this);
                }
                mIsLoaderTaskRunning = false;
            }
        }

        public void stopLocked() {
            synchronized (LoaderTask.this) {
                mStopped = true;
                this.notify();
            }
            if (DEBUG_LOADERS) {
                Util.Log.d(TAG, "stopLocked completed, this = " + LoaderTask.this 
                        + ", mLoaderTask = " + mLoaderTask + ",mIsLoaderTaskRunning = "
                        + mIsLoaderTaskRunning);
            }
        }

        /**
         * Gets the callbacks object.  If we've been stopped, or if the launcher object
         * has somehow been garbage collected, return null instead.  Pass in the Callbacks
         * object that was around when the deferred message was scheduled, and if there's
         * a new Callbacks object around then also return null.  This will save us from
         * calling onto it with data that will be ignored.
         */
        public ILauncherModelCallbacks tryGetCallbacks(ILauncherModelCallbacks oldCallbacks) {
            synchronized (mLock) {
                if (mStopped) {
                    Util.Log.i(TAG, "tryGetCallbacks returned null by stop flag.");
                    return null;
                }

                if (mCallbacks == null) {
                    return null;
                }

                final ILauncherModelCallbacks callbacks = mCallbacks.get();
                if (callbacks != oldCallbacks) {
                    return null;
                }
                if (callbacks == null) {
                    Log.w(TAG, "no mCallbacks");
                    return null;
                }

                return callbacks;
            }
        }

        private void loadWorkspace() {
            final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

            final Context context = mContext;
            final ContentResolver contentResolver = context.getContentResolver();
            final PackageManager manager = context.getPackageManager();
            final AppWidgetManager widgets = AppWidgetManager.getInstance(context);
            final boolean isSafeMode = manager.isSafeMode();

            // Make sure the default workspace is loaded, if needed
            mLauncher.getLauncherProvider().loadDefaultFavoritesIfNecessary(0);

            synchronized (sBgLock) {
                sBgWorkspaceItems.clear();
                sBgAppWidgets.clear();
                sBgFolders.clear();
                sBgItemsIdMap.clear();
                sBgDbIconCache.clear();

                final ArrayList<Long> itemsToRemove = new ArrayList<Long>();
                
                loadWorkspaceInternal(this, mContext, itemsToRemove, mLabelCache);
                
                if (itemsToRemove.size() > 0) {
                    ContentProviderClient client = contentResolver.acquireContentProviderClient(
                                    LauncherSettings.Favorites.CONTENT_URI);
                    // Remove dead items
                    for (long id : itemsToRemove) {
                        if (DEBUG_LOADERS) {
                            Log.d(TAG, "Removed id = " + id);
                        }
                        // Don't notify content observers
                        try {
                            client.delete(LauncherSettings.Favorites.getContentUri(id, false),
                                    null, null);
                        } catch (RemoteException e) {
                            Log.w(TAG, "Could not remove id = " + id);
                        }
                    }
                    
                    client.release();
                }

//                if (DEBUG_LOADERS) {
//                    Log.d(TAG, "loaded workspace in " + (SystemClock.uptimeMillis()-t) + "ms");
//                    Log.d(TAG, "workspace layout: ");
//                    for (int y = 0; y < mCellCountY; y++) {
//                        String line = "";
//                        for (int s = 0; s < Launcher.SCREEN_COUNT; s++) {
//                            if (s > 0) {
//                                line += " | ";
//                            }
//                            for (int x = 0; x < mCellCountX; x++) {
//                                line += ((occupied[s][x][y] != null) ? "#" : ".");
//                            }
//                        }
//                        Log.d(TAG, "[ " + line + " ]");
//                    }
//                }
            }
        }

        /**
         * Binds all loaded data to actual views on the main thread.
         */
        private void bindWorkspace(int synchronizeBindPage) {
            final long t = SystemClock.uptimeMillis();
            Runnable r;

            // Don't use these two variables in any of the callback runnables.
            // Otherwise we hold a reference to them.
            final ILauncherModelCallbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Util.Log.w(TAG, "LoaderTask running with no launcher");
                return;
            }

            final boolean isLoadingSynchronously = (synchronizeBindPage > -1);
            final int currentScreen = isLoadingSynchronously ? synchronizeBindPage :
                oldCallbacks.getCurrentWorkspaceScreen();

            // Load all the items that are on the current page first (and in the process, unbind
            // all the existing workspace items before we call startBinding() below.
            unbindWorkspaceItemsOnMainThread();
            ArrayList<ItemInfo> workspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> appWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            HashMap<Long, FolderInfo> folders = new HashMap<Long, FolderInfo>();
            HashMap<Long, ItemInfo> itemsIdMap = new HashMap<Long, ItemInfo>();
            synchronized (sBgLock) {
                workspaceItems.addAll(sBgWorkspaceItems);
                appWidgets.addAll(sBgAppWidgets);
                folders.putAll(sBgFolders);
                itemsIdMap.putAll(sBgItemsIdMap);
            }

            ArrayList<ItemInfo> currentWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<ItemInfo> otherWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> currentAppWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            ArrayList<LauncherAppWidgetInfo> otherAppWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            HashMap<Long, FolderInfo> currentFolders = new HashMap<Long, FolderInfo>();
            HashMap<Long, FolderInfo> otherFolders = new HashMap<Long, FolderInfo>();

            // Separate the items that are on the current screen, and all the other remaining items
            filterCurrentWorkspaceItems(currentScreen, workspaceItems, currentWorkspaceItems,
                    otherWorkspaceItems);
            filterCurrentAppWidgets(currentScreen, appWidgets, currentAppWidgets,
                    otherAppWidgets);
            filterCurrentFolders(currentScreen, itemsIdMap, folders, currentFolders,
                    otherFolders);
            sortWorkspaceItemsSpatially(currentWorkspaceItems);
            sortWorkspaceItemsSpatially(otherWorkspaceItems);

            // Tell the workspace that we're about to start binding items
            r = new Runnable() {
                public void run() {
                	ILauncherModelCallbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.startBinding();
                    }
                }
            };
            runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);

            // Load items on the current page
            bindWorkspaceItems(oldCallbacks, currentWorkspaceItems, currentAppWidgets,
                    currentFolders, null, this);
            if (isLoadingSynchronously) {
                r = new Runnable() {
                    public void run() {
                    	ILauncherModelCallbacks callbacks = tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.onPageBoundSynchronously(currentScreen);
                        }
                    }
                };
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }

            // Load all the remaining pages (if we are loading synchronously, we want to defer this
            // work until after the first render)
            mDeferredBindRunnables.clear();
            bindWorkspaceItems(oldCallbacks, otherWorkspaceItems, otherAppWidgets, otherFolders,
                    (isLoadingSynchronously ? mDeferredBindRunnables : null), this);

            // Tell the workspace that we're done binding items
            r = new Runnable() {
                public void run() {
                	ILauncherModelCallbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.finishBindingItems();
                    }

                    /// M: Binding workspace is done, reset the installing shortcut flag too
                    InstallShortcutHelper.setInstallingShortcut(false);

                    // If we're profiling, ensure this is the last thing in the queue.
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "bound workspace in "
                            + (SystemClock.uptimeMillis() - t) + "ms");
                    }

                    mIsLoadingAndBindingWorkspace = false;
                }
            };
            if (isLoadingSynchronously) {
                mDeferredBindRunnables.add(r);
            } else {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
        }

        private void loadAndBindAllApps() {
            if (Util.DEBUG_LOADERS) {
                Util.Log.d(TAG, "loadAndBindAllApps: mAllAppsLoaded =" + mAllAppsLoaded
                        + ", mStopped = " + mStopped + ", this = " + this);
            }
            if (!mAllAppsLoaded) {
                loadAllAppsByBatch();
                synchronized (LoaderTask.this) {
                    if (mStopped) {
                        Util.Log.d(TAG, "loadAndBindAllApps returned by stop flag.");
                        return;
                    }
                    mAllAppsLoaded = true;
                }
            } else {
                onlyBindAllApps();
            }
        }

        private void onlyBindAllApps() {
            final ILauncherModelCallbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher (onlyBindAllApps)");
                return;
            }
            
            if (DEBUG_LOADERS) {
                Util.Log.d(TAG, "onlyBindAllApps: oldCallbacks =" + oldCallbacks + ", this = " + this);
            }

            // shallow copy
            @SuppressWarnings("unchecked")
            final ArrayList<ApplicationInfo> list
                    = (ArrayList<ApplicationInfo>) mBgAllAppsList.data.clone();
            Runnable r = new Runnable() {
                public void run() {
                    final long t = SystemClock.uptimeMillis();
                    final ILauncherModelCallbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindAllApplications(list);
                    }
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "bound all " + list.size() + " apps from cache in "
                                + (SystemClock.uptimeMillis() - t) + "ms");
                    }
                }
            };
            boolean isRunningOnMainThread = !(sWorkerThread.getThreadId() == Process.myTid());
            if (oldCallbacks.isAllAppsVisible() && isRunningOnMainThread) {
                r.run();
            } else {
                mHandler.post(r);
            }
        }

        private void loadAllAppsByBatch() {
            final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

            // Don't use these two variables in any of the callback runnables.
            // Otherwise we hold a reference to them.
            final ILauncherModelCallbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher (loadAllAppsByBatch)");
                return;
            }
            
            loadAllAppsByBatchInternal(this, mContext, mBgAllAppsList, mLabelCache);
        }

        public void dumpState() {
            synchronized (sBgLock) {
                Log.d(TAG, "mLoaderTask.mContext=" + mContext);
                Log.d(TAG, "mLoaderTask.mIsLaunching=" + mIsLaunching);
                Log.d(TAG, "mLoaderTask.mStopped=" + mStopped);
                Log.d(TAG, "mLoaderTask.mLoadAndBindStepFinished=" + mLoadAndBindStepFinished);
                Log.d(TAG, "mItems size=" + sBgWorkspaceItems.size());
            }
        }
    }

} 
