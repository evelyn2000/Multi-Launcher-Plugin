package com.jzs.dr.mtplauncher.sjar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.jzs.common.content.QsIntent;
import com.jzs.common.launcher.ILauncherApplication;
import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.common.launcher.LauncherHelper;
import com.jzs.common.launcher.model.AppWidgetInfo;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.FolderInfo;
import com.jzs.common.launcher.model.ILauncherModelCallbacks;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.common.manager.IGestureManager;
import com.jzs.common.manager.IIconUtilities;
import com.jzs.common.os.Build;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragController;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget.DragObject;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherAnimUtils;
import com.jzs.dr.mtplauncher.sjar.model.IconUtilitiesLocal;
import com.jzs.dr.mtplauncher.sjar.model.LauncherAppWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherModel;
import com.jzs.dr.mtplauncher.sjar.model.LauncherSettings;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.model.ResConfigManager;
import com.jzs.dr.mtplauncher.sjar.model.SharedPrefSettingsManager;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.widget.AppWidgetResizeFrame;
import com.jzs.dr.mtplauncher.sjar.widget.BubbleTextView;
import com.jzs.dr.mtplauncher.sjar.widget.CellLayout;
import com.jzs.dr.mtplauncher.sjar.widget.DeleteDropTarget;
import com.jzs.dr.mtplauncher.sjar.widget.DragLayer;
import com.jzs.dr.mtplauncher.sjar.widget.Folder;
import com.jzs.dr.mtplauncher.sjar.widget.FolderIcon;
import com.jzs.dr.mtplauncher.sjar.widget.Hotseat;
import com.jzs.dr.mtplauncher.sjar.widget.IAppsCustomizePanel;
import com.jzs.dr.mtplauncher.sjar.widget.LauncherAppWidgetHostView;
import com.jzs.dr.mtplauncher.sjar.widget.ShortcutAndWidgetContainer;
import com.jzs.dr.mtplauncher.sjar.widget.Workspace;
import com.jzs.dr.mtplauncher.sjar.widget.LauncherAppWidgetHost;
import com.jzs.dr.mtplauncher.sjar.widget.MtpShortcutView;
import com.jzs.dr.mtplauncher.sjar.widget.SearchDropTargetBar;
import com.jzs.dr.mtplauncher.sjar.widget.DragView;
import com.jzs.dr.mtplauncher.sjar.widget.WorkspaceContainer;
import com.jzs.dr.mtplauncher.sjar.widget.WorkspaceCustomSettings;
import com.jzs.dr.mtplauncher.sjar.widget.WorkspacePreviews;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.widget.Advanceable;
import android.graphics.PorterDuff;

import com.android.common.Search;

public abstract class Launcher extends LauncherHelper 
				implements View.OnClickListener, 
					View.OnLongClickListener, View.OnTouchListener{
	
    protected static final int MENU_GROUP_CUSTOM_SETTINGS = 5;
    protected static final int MENU_GROUP_CUSTOM_ICON_SETTINGS = 6;
    protected static final int MENU_GROUP_FOLDER = 7;
    protected static final int MENU_GROUP_APP_ICONS_SORT = 8;
    protected static final int MENU_GROUP_ANIMATE_EFFECT = 9;
    
	protected static final int MENU_CUSTOM_SETTINGS = Menu.FIRST + 1;
	protected static final int MENU_CUSTOM_ICON_SETTINGS = MENU_CUSTOM_SETTINGS + 1;
	protected static final int MENU_CREATE_FOLDER = MENU_CUSTOM_ICON_SETTINGS + 1;
	protected static final int MENU_HELP_EXPORT_APPS_INFO = MENU_CREATE_FOLDER + 1;
	protected static final int MENU_LOCAL_SETTINGS = MENU_HELP_EXPORT_APPS_INFO + 1;
	protected static final int MENU_WALLPAPER_SETTINGS = MENU_LOCAL_SETTINGS + 1;
	protected static final int MENU_MANAGE_APPS = MENU_WALLPAPER_SETTINGS + 1;
	protected static final int MENU_SYSTEM_SETTINGS = MENU_MANAGE_APPS + 1;
	protected static final int MENU_HELP = MENU_SYSTEM_SETTINGS + 1;
	protected static final int MENU_APP_ICONS_SORT = MENU_HELP + 1;
	protected static final int MENU_ANIMATE_EFFECT = MENU_APP_ICONS_SORT + 1;
	
	protected static final int MENU_CHILD_GROUP_ID = 20;
	protected static final int MENU_CHILD_ID = MENU_HELP + 50;
	
	protected static final int DIALOG_CHOICE_WORKSPACE_GRID_SIZE = 100;
	
	// Type: int
	protected static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    protected static final String RUNTIME_STATE = "launcher.state";
    // Type: int
    protected static final String RUNTIME_STATE_PENDING_ADD_CONTAINER = "launcher.add_container";
    // Type: int
    protected static final String RUNTIME_STATE_PENDING_ADD_SCREEN = "launcher.add_screen";
    // Type: int
    protected static final String RUNTIME_STATE_PENDING_ADD_CELL_X = "launcher.add_cell_x";
    // Type: int
    protected static final String RUNTIME_STATE_PENDING_ADD_CELL_Y = "launcher.add_cell_y";
    // Type: boolean
    protected static final String RUNTIME_STATE_PENDING_FOLDER_RENAME = "launcher.rename_folder";
    // Type: long
    protected static final String RUNTIME_STATE_PENDING_FOLDER_RENAME_ID = "launcher.rename_folder_id";
    // Type: int
    protected static final String RUNTIME_STATE_PENDING_ADD_SPAN_X = "launcher.add_span_x";
    // Type: int
    protected static final String RUNTIME_STATE_PENDING_ADD_SPAN_Y = "launcher.add_span_y";
    // Type: parcelable
    protected static final String RUNTIME_STATE_PENDING_ADD_WIDGET_INFO = "launcher.add_widget_info";
    
    protected static final int EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT = 300;
    protected static final int EXIT_SPRINGLOADED_MODE_LONG_TIMEOUT = 600;
	
	protected final static String TAG = "JzsLauncher";
	public static final int MAX_UNREAD_COUNT = 99;
	
	/** Standard activity result: operation canceled. */
    public static final int RESULT_CANCELED    = Activity.RESULT_CANCELED;
    /** Standard activity result: operation succeeded. */
    public static final int RESULT_OK           = Activity.RESULT_OK;
    /** Start of user-defined activity results. */
    public static final int RESULT_FIRST_USER   = Activity.RESULT_FIRST_USER;
    
	public static final int REQUEST_CREATE_SHORTCUT = 1;
	public static final int REQUEST_CREATE_APPWIDGET = 5;
	public static final int REQUEST_PICK_APPLICATION = 6;
	public static final int REQUEST_PICK_SHORTCUT = 7;
	public static final int REQUEST_PICK_APPWIDGET = 9;
	public static final int REQUEST_PICK_WALLPAPER = 10;

	public static final int REQUEST_BIND_APPWIDGET = 11;

	public static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";
	
	//public static final String PREFERENCES = "launcher.preferences";
	
	// The Intent extra that defines whether to ignore the launch animation
	public static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION =
            "com.android.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";
	
	
	public static final int APPWIDGET_HOST_ID = 1024;
	
	
							
	protected int mState = State.WORKSPACE;
	protected int mOnResumeState = State.NONE;
	
	protected static final Object sLock = new Object();
	protected static int sScreen = -1;// = DEFAULT_SCREEN;
	
	//private ILauncherPluginEntry mLauncherPluginEntry;
	//protected ResConfigManager mResConfigManager;
	//protected LauncherModel mLauncherModel;
	//protected IconCache mIconCache;
	protected DragController mDragController;
	
	protected boolean mUserPresent = true;
	protected boolean mVisible = false;
	protected boolean mAttached = false;
    
	protected Bundle mSavedState;
	// Keep track of whether the user has left launcher
	protected static boolean sPausedFromUserAction = false;
	
	//protected LauncherHelper mLauncherHelper;
	//protected SharedPreferences mSharedPrefs;
	
	protected LayoutInflater mInflater;
	
	protected SearchDropTargetBar mSearchDropTargetBar;
	protected Hotseat mHotseat;
	protected DragLayer mDragLayer;
	protected Workspace mWorkspace;
	protected AppWidgetManager mAppWidgetManager;
	protected LauncherAppWidgetHost mAppWidgetHost;
	protected boolean mWorkspaceLoading = true;

	protected boolean mPaused = true;
	protected boolean mRestoring;
	protected boolean mWaitingForResult;
	protected boolean mOnResumeNeedsLoad;
	
	//private Activity mLauncherActivity;
	private View mRootView;
	protected View mWorkspaceContainer;
	protected WorkspacePreviews mWorkspacePreviewView;
	protected WorkspaceCustomSettings mCustomSettingsView;
	protected View mPageViewIndicator;
	private boolean mIsEnableRotation;
	
	protected FolderInfo mFolderInfo;
	protected ImageView mFolderIconImageView;
	protected Bitmap mFolderIconBitmap;
	protected Canvas mFolderIconCanvas;
	protected Rect mRectForFolderAnimation = new Rect();
	
	protected SpannableStringBuilder mDefaultKeySsb = null;
	protected static HashMap<Long, FolderInfo> sFolders = new HashMap<Long, FolderInfo>();
	
	//protected AlertDialog mAlertDialog;
	
	private Workspace.AnimateEffectType mAnimateEffectType = Workspace.AnimateEffectType.Standard;
    
	protected ItemInfo mPendingAddInfo = new ItemInfo();
	protected AppWidgetProviderInfo mPendingAddWidgetInfo;
	protected final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList<Integer>();
	protected boolean mIsLoadingWorkspace;
	protected int[] mTmpAddItemCellCoordinates = new int[2];
	
	protected Runnable mBuildLayersRunnable = new Runnable() {
        public void run() {
            if (mWorkspace != null) {
            	mWorkspace.buildPageHardwareLayers();
            }
        }
    };
	
    private static float sScreenDensity;
	private static boolean sIsScreenLarge;
	private static int sLongPressTimeout = 300;
	public static float getScreenDensity() {
        return sScreenDensity;
    }
	
	public static boolean isScreenLarge() {
        return sIsScreenLarge;
    }

    public static boolean isScreenLandscape(Context context) {
        return context.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
    }
    
    public static int getLongPressTimeout(){
    	return sLongPressTimeout;
    }
    
    public Launcher(Context localContext, ILauncherApplication mainApp){
    	super(localContext, mainApp);
    	
    	final int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
        sIsScreenLarge = screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
            screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
        sScreenDensity = getResources().getDisplayMetrics().density;
    }
	
	public Launcher(Context localContext){
		this(localContext, null);
	}
	
	protected IResConfigManager createResConfigManager(ISharedPrefSettingsManager pref, IResConfigManager base){
		return new ResConfigManager(this, pref, base);
	}
	
//	@Override
//	protected IIconUtilities createIconUtilities(){
//	    IIconUtilities iconUtil = super.createIconUtilities();
//	    if(iconUtil != null)
//	        return iconUtil;
//	    
//        return new IconUtilitiesLocal(getLocalContext(), getSharedPrefSettingsManager());
//    }
	
    public void onCreate(Bundle savedInstanceState) {    	
    	mInflater = LayoutInflater.from(this);
    	
    	getModel().setCallbacks(this);
		
		//mLauncherHelper = mLauncherPluginEntry.getLauncherHelper();
		//mIconCache  = mLauncherPluginEntry.getIconCache();
		mDragController = new DragController(this);
		
		mAppWidgetManager = AppWidgetManager.getInstance(getActivity());
        mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();
        
        mPaused = false;

        mIsEnableRotation = getResConfigManager().getBoolean(IResConfigManager.CONFIG_ALLOW_SCREEN_ROTATION);
        mSavedState = savedInstanceState;
        checkForLocaleChange();
        
        if(isSupportAnimateEffect()){
            String str = getSharedPrefSettingsManager().getString(SharedPrefSettingsManager.KEY_ANIMATE_EFFECT_TYPE, 
                    ResConfigManager.CONFIG_DEFAULT_ANIMATE_EFFECT_TYPE, 
                    Workspace.AnimateEffectType.Standard.toString());
            if(str != null && str.length() > 0){
                mAnimateEffectType = Workspace.AnimateEffectType.valueOf(str);
            }
        }
        
        
        checkSupportGesture();
	}
    
    protected void initDefaultKeySsb(){
    	// For handling default keys
        mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(mDefaultKeySsb, 0);
    }
    
    protected void initApplicationsLoader(){
    	if (!mRestoring) {
            /// M: Reset load state if locale changed before.
            if (sLocaleChanged) {
                getModel().resetLoadedState(true, true);
                sLocaleChanged = false;
            }
            mIsLoadingWorkspace = true;
            if (sPausedFromUserAction) {
                // If the user leaves launcher, then we should just load items asynchronously when
                // they return.
            	getModel().startLoader(true, -1);
            } else {
                // We only load the page synchronously if the user rotates (or triggers a
                // configuration change) while launcher is in the foreground
            	getModel().startLoader(true, mWorkspace.getCurrentPage());
            }
        }
    }
    
    protected final void setContentView(int layoutid){
    	//android.util.Log.i("QsLog", "Launcher::setContentView(0)===000=");
    	mRootView = mInflater.inflate(layoutid, null);
    	getActivity().setContentView(mRootView);
    	
    	if(getSharedPrefSettingsManager().getBoolean(ISharedPrefSettingsManager.KEY_IS_FULLSCREEN, 
				ResConfigManager.CONFIG_IS_FULLSCREEN, false)){
    		setFullscreen(true);
    	}
    }

	public DragController getDragController(){
		return mDragController;
	}

	public final View findViewById(int id){
		return mRootView.findViewById(id);
	}

	protected void setupViews(DragLayer dragLayer){
		
		final DragController dragController = mDragController;
		mDragLayer = dragLayer;
		mWorkspace = (Workspace) dragLayer.findViewWithTag("workspace");
//		if(!isSupportAnimateEffect()){
//		    setAnimateEffectType(Workspace.AnimateEffectType.Standard);
//		}
		mWorkspaceContainer = dragLayer.findViewWithTag("workspace_container");
		mWorkspacePreviewView = (WorkspacePreviews)dragLayer.findViewWithTag("workspace_preview_group");
		mCustomSettingsView = (WorkspaceCustomSettings)dragLayer.findViewWithTag("workspace_custom_settings");
		//android.util.Log.i("QsLog", "Launcher::setupViews()====count:"+mWorkspace.getChildCount());
		// Setup the drag layer
        mDragLayer.setup(this, dragController);
        
        final ISharedPrefSettingsManager shareManager = getSharedPrefSettingsManager();

        mPageViewIndicator = mWorkspaceContainer != null ? mWorkspaceContainer.findViewWithTag("paged_view_indicator") : null;
        if(mPageViewIndicator != null){
//        	if(!shareManager.getWorkspaceShowHotseatBar()){
//        		mPageViewIndicator.setLayoutParams(params);
        		
        }
        // Setup the hotseat
        mHotseat = (Hotseat) dragLayer.findViewWithTag("hotseat");
        if (mHotseat != null) {
            mHotseat.setup(this);
            if(!shareManager.getWorkspaceShowHotseatBar()){
            	hideHotseat(false);
            }
        }
        
        mSearchDropTargetBar = (SearchDropTargetBar) dragLayer.findViewWithTag("qsb_bar");
        
//        android.util.Log.w("QsLog", "setupViews()==mHotseat:"+mHotseat
//        		+", mHotseat1:"+dragLayer.findViewById(com.jzs.dr.mtplauncher.sjar.R.id.hotseat)
//        		+", mDragLayer:"+mDragLayer
//        		+", mWorkspace:"+mWorkspace
//        		+", mSearchDropTargetBar:"+mSearchDropTargetBar);
        
	}
	
	public final Workspace getWorkspace(){
		return mWorkspace;
	}
	
	public final Hotseat getHotseat(){
		return mHotseat;
	}
	public final View getPageViewIndicator(){
		return mPageViewIndicator;
	}
	public final View getWorkspaceContainer(){
		return mWorkspaceContainer;
	}
	public final SearchDropTargetBar getSearchBar(){
		return mSearchDropTargetBar;
	}
	public final DragLayer getDragLayer(){
		return mDragLayer;
	}
	
	public boolean isFolderClingVisible() {
        return false;
    }
	public void openFolder(FolderIcon folderIcon){
		Folder folder = folderIcon.getFolder();
        FolderInfo info = folder.mInfo;

        info.opened = true;

        // Just verify that the folder hasn't already been added to the DragLayer.
        // There was a one-off crash where the folder had a parent already.
        if (folder.getParent() == null) {
            mDragLayer.addView(folder);
            mDragController.addDropTarget((DropTarget) folder);
        } else {
            Util.Log.w(TAG, "Opening folder (" + folder + ") which already has a parent (" +
                    folder.getParent() + ").");
        }
        folder.animateOpen();
        growAndFadeOutFolderIcon(folderIcon);
	}
	
	public void closeFolder(){
		Folder folder = mWorkspace.getOpenFolder();
        if (folder != null) {
            if (folder.isEditingName()) {
                folder.dismissEditingName();
            }
            closeFolder(folder);
            
            // Dismiss the folder cling
            //dismissFolderCling(null);
        }
	}
	
	protected void closeFolder(Folder folder){
		folder.getInfo().opened = false;

        ViewGroup parent = (ViewGroup) folder.getParent().getParent();
        if (parent != null) {
            FolderIcon fi = (FolderIcon) mWorkspace.getViewForTag(folder.mInfo);
            shrinkAndFadeInFolderIcon(fi);
        }
        folder.animateClosed();
	}
	
	public FolderIcon addFolder(CellLayout layout, long container, final int screen, int cellX,
            int cellY){
		
		final IResConfigManager resManager = getResConfigManager();
		final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = resManager.getText(IResConfigManager.STR_FOLDER_DEFAULT_NAME);//getText(R.string.folder_name);

        // Update the model
        getModel().addItemToDatabase(folderInfo, container, screen, cellX, cellY,
                false);
        sFolders.put(folderInfo.id, folderInfo);

        // Create the view
        FolderIcon newFolder = FolderIcon.fromXml(this, layout, folderInfo);
        mWorkspace.addInScreen(newFolder, container, screen, cellX, cellY, 1, 1,
                isWorkspaceLocked());
        
        return newFolder;
	}
	
	public void removeFolder(FolderInfo folder) {
        sFolders.remove(folder.id);
	}
	
	protected void copyFolderIconToImage(FolderIcon fi) {
        final int width = fi.getMeasuredWidth();
        final int height = fi.getMeasuredHeight();

        // Lazy load ImageView, Bitmap and Canvas
        if (mFolderIconImageView == null) {
            mFolderIconImageView = new ImageView(this);
        }
        if (mFolderIconBitmap == null || mFolderIconBitmap.getWidth() != width ||
                mFolderIconBitmap.getHeight() != height) {
            mFolderIconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mFolderIconCanvas = new Canvas(mFolderIconBitmap);
        }

        DragLayer.LayoutParams lp;
        if (mFolderIconImageView.getLayoutParams() instanceof DragLayer.LayoutParams) {
            lp = (DragLayer.LayoutParams) mFolderIconImageView.getLayoutParams();
        } else {
            lp = new DragLayer.LayoutParams(width, height);
        }

        // The layout from which the folder is being opened may be scaled, adjust the starting
        // view size by this scale factor.
        float scale = mDragLayer.getDescendantRectRelativeToSelf(fi, mRectForFolderAnimation);
        lp.customPosition = true;
        lp.x = mRectForFolderAnimation.left;
        lp.y = mRectForFolderAnimation.top;
        lp.width = (int) (scale * width);
        lp.height = (int) (scale * height);

        mFolderIconCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        fi.draw(mFolderIconCanvas);
        mFolderIconImageView.setImageBitmap(mFolderIconBitmap);
        if (fi.getFolder() != null) {
            mFolderIconImageView.setPivotX(fi.getFolder().getPivotXForIconAnimation());
            mFolderIconImageView.setPivotY(fi.getFolder().getPivotYForIconAnimation());
        }
        // Just in case this image view is still in the drag layer from a previous animation,
        // we remove it and re-add it.
        if (mDragLayer.indexOfChild(mFolderIconImageView) != -1) {
            mDragLayer.removeView(mFolderIconImageView);
        }
        mDragLayer.addView(mFolderIconImageView, lp);
        if (fi.getFolder() != null) {
            fi.getFolder().bringToFront();
        }
    }

	protected void growAndFadeOutFolderIcon(FolderIcon fi) {
        if (fi == null) {
            return;
        }
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.5f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.5f);

        FolderInfo info = (FolderInfo) fi.getTag();
        if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            CellLayout cl = (CellLayout) fi.getParent().getParent();
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) fi.getLayoutParams();
            cl.setFolderLeaveBehindCell(lp.cellX, lp.cellY);
        }

        // Push an ImageView copy of the FolderIcon into the DragLayer and hide the original
        copyFolderIconToImage(fi);
        if (info.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            fi.setVisibility(View.INVISIBLE);
        }

        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(mFolderIconImageView, alpha,
                scaleX, scaleY);
        //oa.setDuration(getResources().getInteger(R.integer.config_folderAnimDuration));
        oa.setDuration(getResConfigManager().getInteger(IResConfigManager.CONFIG_FOLDER_ANIM_DURATION));
        oa.start();
    }
    
	protected void shrinkAndFadeInFolderIcon(final FolderIcon fi) {
        if (fi == null) return;
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f);

        final CellLayout cl = (CellLayout) fi.getParent().getParent();

        // We remove and re-draw the FolderIcon in-case it has changed
        mDragLayer.removeView(mFolderIconImageView);
        copyFolderIconToImage(fi);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(mFolderIconImageView, alpha,
                scaleX, scaleY);
        //oa.setDuration(getResources().getInteger(R.integer.config_folderAnimDuration));
        oa.setDuration(getResConfigManager().getInteger(IResConfigManager.CONFIG_FOLDER_ANIM_DURATION));
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (cl != null) {
                    cl.clearFolderLeaveBehind();
                    // Remove the ImageView copy of the FolderIcon and make the original visible.
                    mDragLayer.removeView(mFolderIconImageView);
                }
                if(fi != null){
                    fi.setVisibility(View.VISIBLE);
                }
            }
        });
        oa.start();
    }
	
	public LauncherAppWidgetHost getAppWidgetHost(){
		return mAppWidgetHost;
	}
	
	
	public void invalidateOptionsMenu(){
		getActivity().invalidateOptionsMenu();
	}
	
	public final void overridePendingTransition(int enterAnim, int exitAnim){
		getActivity().overridePendingTransition(enterAnim, exitAnim);
	}
	
	public final void setResult(int resultCode) {
		getActivity().setResult(resultCode);
    }
	
	public final void setResult(int resultCode, Intent data) {
		getActivity().setResult(resultCode, data);
	}
	
    public void onResume(){
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onResume(0): mOnResumeState = " + Integer.toHexString(mOnResumeState)
                    +"==mState:"+Integer.toHexString(mState));
        }
    	/// M: Call the appropriate callback for the IMTKWidget on the current page when we resume Launcher.
        mWorkspace.onResumeWhenShown(mWorkspace.getCurrentPage());
        
        // Restore the previous launcher state
        if ((mOnResumeState&State.WORKSPACE) > 0) {
            showWorkspace(false);
        } else if ((mOnResumeState&State.APPS_CUSTOMIZE) > 0) {
            showAllApps(false);
        }
        mOnResumeState = State.NONE;
        
        getInstallShortcutReceiver().flushInstallQueue();
    	mPaused = false;
    	sPausedFromUserAction = false;
    	if (mRestoring || mOnResumeNeedsLoad) {
            mWorkspaceLoading = true;
            mIsLoadingWorkspace = true;
            getModel().startLoader(true, -1);
            mRestoring = false;
            mOnResumeNeedsLoad = false;
        }
    	if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onResume(1): mOnResumeState = " + Integer.toHexString(mOnResumeState)
                    +"==mState:"+Integer.toHexString(mState));
        }
    	enableGesture(true, true);
	}
	
	public void onPause() {
	    enableGesture(false, false);
	    if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onPause(0): mOnResumeState = " + Integer.toHexString(mOnResumeState)
                    +"==mState:"+Integer.toHexString(mState));
        }
		if(isWorkspacePreviewVisible())
        	mOnResumeState = State.WORKSPACE_PREVIEW;
        else if(isCustomSettingsScreenVisible())
        	mOnResumeState = State.CUSTOM_SETTINGS;
		
        hideWorkspacePreiews(false);
        hideCustomSettingsScreen(false);

		 /// M: Call the appropriate callback for the IMTKWidget on the current page when we pause Launcher.
        mWorkspace.onPauseWhenShown(mWorkspace.getCurrentPage());
        //resetReSyncFlags();

        mPaused = true;
        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();
        
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onPause(1): mOnResumeState = " + Integer.toHexString(mOnResumeState)
                    +"==mState:"+Integer.toHexString(mState));
        }
	}
	
	public void onStart() {
		
	}
	
	public void onStop() {
		//android.util.Log.i("QsLog", "onStop()====");
	}
	
	public void onUserLeaveHint(){
		sPausedFromUserAction = true;
	}
	
	public void onDestroy(){
		//android.util.Log.i("QsLog", "onDestroy()====");
		// Remove all pending runnables
        mHandler.removeMessages(ADVANCE_MSG);
        mHandler.removeMessages(0);
        if(mWorkspace != null)  mWorkspace.removeCallbacks(mBuildLayersRunnable);
        
        if(mExportAppTask != null)
            mExportAppTask.cancel(true);
        
//        if(mSelectGridSizeDialog != null){
//            mSelectGridSizeDialog.dismiss();
//            mSelectGridSizeDialog = null;
//        }

        // Stop callbacks from LauncherModel
        LauncherApplication app = ((LauncherApplication) getApplication());
        getModel().stopLoader();
        getModel().setCallbacks(null);//.setLauncher(null);
        
		if(mAppWidgetHost != null){
			try {
	            mAppWidgetHost.stopListening();
	        } catch (NullPointerException ex) {
	            Util.Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
	        }
	        mAppWidgetHost = null;
		}
		mWidgetsToAdvance.clear();
		TextKeyListener.getInstance().release();
		
		getModel().unbindItemInfosAndClearQueuedBindRunnables();

		unRegisterContentObservers();

		mDragLayer.clearAllResizeFrames();
		if(mWorkspace != null){
			mWorkspace.removeAllViews();
	        mWorkspace = null;
		}
        mDragController = null;

        LauncherAnimUtils.onDestroyActivity();
        
        /// M: Disable orientation listener when launcher is destroyed.
        disableOrientationListener();
	}
	
	public final boolean isResumed() {
        return (getActivity() != null && getActivity().isResumed());
    }
	
    public void onAttachedToWindow() {
    	mAttached = true;
        mVisible = true;
	}
    
    public void onDetachedFromWindow() {
    	mVisible = false;
    	mAttached = false;
//        if (mAttached) {
//            //unregisterReceiver(mReceiver);
//            mAttached = false;
//        }
        updateRunning();
	}
	
    public void onWindowVisibilityChanged(int visibility) {
        mVisible = visibility == View.VISIBLE;
        updateRunning();
//        // The following code used to be in onResume, but it turns out onResume is called when
//        // you're in All Apps and click home to go to the workspace. onWindowVisibilityChanged
//        // is a more appropriate event to handle
//        if (mVisible) {
//            mAppsCustomizeTabHost.onWindowVisible();
//            if (!mWorkspaceLoading) {
//                final ViewTreeObserver observer = mWorkspace.getViewTreeObserver();
//                // We want to let Launcher draw itself at least once before we force it to build
//                // layers on all the workspace pages, so that transitioning to Launcher from other
//                // apps is nice and speedy. Usually the first call to preDraw doesn't correspond to
//                // a true draw so we wait until the second preDraw call to be safe
//                observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//                    public boolean onPreDraw() {
//                        // We delay the layer building a bit in order to give
//                        // other message processing a time to run.  In particular
//                        // this avoids a delay in hiding the IME if it was
//                        // currently shown, because doing that may involve
//                        // some communication back with the app.
//                        mWorkspace.postDelayed(mBuildLayersRunnable, 500);
//
//                        observer.removeOnPreDrawListener(this);
//                        return true;
//                    }
//                });
//            }
//            // When Launcher comes back to foreground, a different Activity might be responsible for
//            // the app market intent, so refresh the icon
//            updateAppMarketIcon();
//            clearTypedText();
//        }
    }
    
	public boolean onNewIntent(Intent intent){
	    
	    try {
            dismissDialog(MENU_ANIMATE_EFFECT);
            removeDialog(MENU_ANIMATE_EFFECT);
        } catch(Exception ex){
        }
	    
	    if(isAllAppsVisible()){
    		try {
    		    dismissDialog(MENU_APP_ICONS_SORT);
    		    removeDialog(MENU_APP_ICONS_SORT);
    		    return true;
    		} catch(Exception ex){
    		    //android.util.Log.e("QsLog", "onNewIntent()==", ex);
    		}
	    }
	    
	    return false;
	}
	
	protected static int intToState(int stateOrdinal) {
//        int state = State.WORKSPACE;
//        final State[] stateValues = State.values();
//        for (int i = 0; i < stateValues.length; i++) {
//            if (stateValues[i].ordinal() == stateOrdinal) {
//                state = stateValues[i];
//                break;
//            }
//        }
        return stateOrdinal;
    }
	
	protected void restoreState(Bundle savedState) {
		if (savedState == null) {
            return;
        }

        int state = savedState.getInt(RUNTIME_STATE, State.WORKSPACE);
        if (state == State.APPS_CUSTOMIZE 
        		|| state == State.APPS_CUSTOMIZE_WIDGET 
        		|| state == State.WORKSPACE_PREVIEW
        		|| state == State.CUSTOM_SETTINGS) {
        	
//        	android.util.Log.i("QsLog", "==sLocaleChanged:"+sLocaleChanged
//        			+"==state:"+Integer.toHexString(state));
            mOnResumeState = sLocaleChanged ? State.WORKSPACE : state;
        }

        int currentScreen = savedState.getInt(RUNTIME_STATE_CURRENT_SCREEN, -1);
        if (currentScreen > -1) {
            mWorkspace.setCurrentPage(currentScreen);
        }

        final long pendingAddContainer = savedState.getLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, -1);
        final int pendingAddScreen = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SCREEN, -1);

        if (pendingAddContainer != ItemInfo.NO_ID && pendingAddScreen > -1) {
            mPendingAddInfo.container = pendingAddContainer;
            mPendingAddInfo.screen = pendingAddScreen;
            mPendingAddInfo.cellX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X);
            mPendingAddInfo.cellY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y);
            mPendingAddInfo.spanX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X);
            mPendingAddInfo.spanY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y);
            mPendingAddWidgetInfo = savedState.getParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO);
            mWaitingForResult = true;
            mRestoring = true;
        }
        
        boolean renameFolder = savedState.getBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, false);
        if (renameFolder) {
            long id = savedState.getLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID);
            mFolderInfo = getModel().getFolderById(this, sFolders, id);
            mRestoring = true;
        }
	}
	
	public void onRestoreInstanceState(Bundle state){
		if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onRestoreInstanceState: state = " + state
                    );
        }
        
        for (int page: mSynchronouslyBoundPages) {
            mWorkspace.restoreInstanceStateForChild(page);
        }
	}
	
	public void onSaveInstanceState(Bundle outState){
		outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mWorkspace.getNextPage());
		outState.putInt(RUNTIME_STATE, mState);
        // We close any open folder since it will not be re-opened, and we need to make sure
        // this state is reflected.
        closeFolder();

        if (mPendingAddInfo.container != ItemInfo.NO_ID && mPendingAddInfo.screen > -1 &&
                mWaitingForResult) {
            outState.putLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, mPendingAddInfo.container);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SCREEN, mPendingAddInfo.screen);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X, mPendingAddInfo.cellX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y, mPendingAddInfo.cellY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X, mPendingAddInfo.spanX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y, mPendingAddInfo.spanY);
            outState.putParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO, mPendingAddWidgetInfo);
        }
        
        if (mFolderInfo != null && mWaitingForResult) {
            outState.putBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, true);
            outState.putLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID, mFolderInfo.id);
        }
	}
	
	public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		
		return false;
	}
	
	public boolean onBackPressed() {
//		android.util.Log.i("QsLog", "onBackPressed()==app:"+isAllAppsVisible()
//				+"==prev:"+isWorkspacePreviewVisible()
//				+"==sett:"+isCustomSettingsScreenVisible());
//	    if(mSelectGridSizeDialog != null){
//            mSelectGridSizeDialog.cancel();
//            mSelectGridSizeDialog = null;
//            return true;
//        }
	    
//	    if(mAlertDialog != null && mAlertDialog.isShowing()){
//	        mAlertDialog.cancel();
//	        mAlertDialog = null;
//	        return true;
//	    }
	    
		if (isAllAppsVisible()) {
    		showWorkspace(true);
    		return true;
        } else if(isWorkspacePreviewVisible()){
        	hideWorkspacePreiews(false);
        	return true;
        } else if(isCustomSettingsScreenVisible()){
            if(mCustomSettingsView != null && mCustomSettingsView.onBackPressed())
                return true;
            hideCustomSettingsScreen(false);
        	return true;
        } else if (mWorkspace != null && mWorkspace.getOpenFolder() != null) {
            Folder openFolder = mWorkspace.getOpenFolder();
            if (openFolder.isEditingName()) {
                openFolder.dismissEditingName();
            } else {
                closeFolder();
            }
            return true;
        }
		return false;
	}
	
	public void onClick(View v){
		
	}
	
	public boolean onLongClick(View v) {
		if (!isDraggingEnabled()) {
            Util.Log.d(TAG, "onLongClick: isDraggingEnabled() = " + isDraggingEnabled());
            return false;
        }

        if (isWorkspaceLocked()) {
        	Util.Log.d(TAG, "onLongClick: isWorkspaceLocked() mWorkspaceLoading " + mWorkspaceLoading
                    + ", mWaitingForResult = " + mWaitingForResult);
            return false;
        }
        
        if (mState != State.WORKSPACE) {
            Util.Log.d(TAG, "onLongClick: mState != State.WORKSPACE: = " + mState);
            return false;
        }
        
		return true;
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		return false;
	}
	
	protected boolean isSupportAnimateEffect(){
	    return true;
	}

    public void setAnimateEffectType(Workspace.AnimateEffectType animate){
        mAnimateEffectType = animate;
    }
    
    public Workspace.AnimateEffectType getAnimateEffectType(){
        return mAnimateEffectType;
    }
	
	public boolean onCreateOptionsMenu(Menu menu){
		if (isWorkspaceLocked()) {
            return false;
        }
		
		final IResConfigManager res = getResConfigManager();//.getString(type)
		if("eng".equals(android.os.Build.TYPE)){
            menu.add(0, MENU_HELP_EXPORT_APPS_INFO, 0, "export apps and workspace");
        }

		menu.add(MENU_GROUP_CUSTOM_SETTINGS, MENU_CUSTOM_SETTINGS, 0, res.getText(IResConfigManager.STR_MENU_CUSTOM_SETTINGS))
        .setAlphabeticShortcut('C');

        menu.add(MENU_GROUP_CUSTOM_ICON_SETTINGS, MENU_CUSTOM_ICON_SETTINGS, 0, res.getText(IResConfigManager.STR_MENU_CUSTOM_ICON_SETTINGS))
        .setAlphabeticShortcut('I');
        
        if(isSupportAnimateEffect()){
            menu.add(MENU_GROUP_ANIMATE_EFFECT, MENU_ANIMATE_EFFECT, 0, res.getText(IResConfigManager.STR_MENU_ANIMATE_EFFECT))
            .setAlphabeticShortcut('A');
        }
        
        if(Folder.SUPPORT_EMPTY_FOLDER){
            menu.add(MENU_GROUP_FOLDER, MENU_CREATE_FOLDER, 0, res.getText(IResConfigManager.STR_MENU_CREATE_FOLDER))
            .setAlphabeticShortcut('F');
        }
        
        menu.add(MENU_GROUP_APP_ICONS_SORT, MENU_APP_ICONS_SORT, 0, res.getText(IResConfigManager.STR_MENU_SORT_APPS_ICON))
        .setAlphabeticShortcut('S');

		return true;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu){
	    if(isAllAppsVisible()){
	        menu.setGroupVisible(MENU_GROUP_CUSTOM_ICON_SETTINGS, true);
	        menu.setGroupVisible(MENU_GROUP_CUSTOM_SETTINGS, false);
	        if(Folder.SUPPORT_EMPTY_FOLDER){
	            menu.setGroupVisible(MENU_GROUP_FOLDER, false);
	        }
	        menu.setGroupVisible(MENU_GROUP_APP_ICONS_SORT, true);
	    } else {
	        menu.setGroupVisible(MENU_GROUP_APP_ICONS_SORT, false);
	        menu.setGroupVisible(MENU_GROUP_CUSTOM_ICON_SETTINGS, false);
	        if(mState == State.WORKSPACE) {
	            menu.setGroupVisible(MENU_GROUP_CUSTOM_SETTINGS, true);
	            if(Folder.SUPPORT_EMPTY_FOLDER){
	                menu.setGroupVisible(MENU_GROUP_FOLDER, true);
	            }
	        } else {
	            menu.setGroupVisible(MENU_GROUP_CUSTOM_SETTINGS, false);
	            if(Folder.SUPPORT_EMPTY_FOLDER){
	                menu.setGroupVisible(MENU_GROUP_FOLDER, false);
	            }
	        }
	    }
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()) {
        case MENU_WALLPAPER_SETTINGS:
            startWallpaper();
            return true;
        case MENU_CUSTOM_SETTINGS:
        	showCustomSettingsScreen(true);
        	return true;
        case MENU_CUSTOM_ICON_SETTINGS:
            showAppIconAndTitleCustomActivity();
            return true;
        case MENU_APP_ICONS_SORT:
            showDialog(MENU_APP_ICONS_SORT);
            return true;
        case MENU_ANIMATE_EFFECT:
            showDialog(MENU_ANIMATE_EFFECT);
            return true;
        case MENU_HELP_EXPORT_APPS_INFO:
            jzsExportAppsAndWorkspace();
            return true;
        case MENU_CREATE_FOLDER:
        	qsAddFolderByMenu();
        	return true;
        }
		return false;
	}
	
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
            }
        }
		return false;
	}
	
	public void onTrimMemory(int level) {
		
	}
	
	public void onWindowFocusChanged(boolean hasFocus){
		
	}
	
	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event, boolean result){
		return result;
	}
	
	public Object onRetainNonConfigurationInstance(){
        // Flag the loader to stop early before switching
        getModel().stopLoader();
		return null;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    }

	public boolean onContextItemSelected(MenuItem item) {
		return false;
	}

	public void onContextMenuClosed(Menu menu){
		
	}
	
	public final void showDialog(int id) {
        getActivity().showDialog(id);
    }

	public Dialog onCreateDialog(int id) {
//	    if(id == DIALOG_CHOICE_WORKSPACE_GRID_SIZE){
//	        return mSelectGridSizeDialog;
//	    }
	    if(MENU_APP_ICONS_SORT == id){
	        final IResConfigManager resources = getResConfigManager();
	        int selectitem = getSharedPrefSettingsManager().getInt(SharedPrefSettingsManager.KEY_ALLAPPS_SORT_TYPE, 
	        		ResConfigManager.CONFIG_DEFAULT_APP_ICON_SORT_TYPE, 
	        		SharedPrefSettingsManager.ALLAPPS_SORT_BY_DEFAULT);
	        return new AlertDialog.Builder(getActivity())
	        .setTitle(resources.getString(IResConfigManager.STR_MENU_SORT_APPS_ICON))
            .setSingleChoiceItems(resources.getTextArray(IResConfigManager.ARRAY_APP_ICONS_SORT_ENTRIES), 
                    selectitem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        
                    int selectitem = getSharedPrefSettingsManager().getInt(SharedPrefSettingsManager.KEY_ALLAPPS_SORT_TYPE, 
                            SharedPrefSettingsManager.ALLAPPS_SORT_BY_DEFAULT);
                    if(selectitem != whichButton){
                        getSharedPrefSettingsManager().setInt(SharedPrefSettingsManager.KEY_ALLAPPS_SORT_TYPE, 
                                whichButton);
                        
                        getModel().reBindAllApplications();
                    }
                    dialog.dismiss();
                }
            })
//            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int whichButton) {
//
//                    /* User clicked Yes so do some stuff */
//                }
//            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    dialog.dismiss();
                }
            })
           .create();
	    } else if(MENU_ANIMATE_EFFECT == id){
	        final IResConfigManager resources = getResConfigManager();
	        final String[] animateArray = resources.getStringArray(ResConfigManager.ARRAY_ANIMATE_EFFECT_VALUES);
	        final String defaultAnimateEffect = resources.getString(ResConfigManager.CONFIG_DEFAULT_ANIMATE_EFFECT_TYPE, 
	                Workspace.AnimateEffectType.Standard.toString());
            String strselectitem = getSharedPrefSettingsManager().getString(SharedPrefSettingsManager.KEY_ANIMATE_EFFECT_TYPE, 
                    defaultAnimateEffect);
            int selectitem = 0;
            if(strselectitem != null && animateArray != null){
                for(String str : animateArray){
                    if(str.equals(strselectitem)){
                        break;
                    }
                    selectitem++;
                }
                if(selectitem >= animateArray.length)
                    selectitem = 0;
            }
            
            return new AlertDialog.Builder(getActivity())
            .setTitle(resources.getString(IResConfigManager.STR_MENU_ANIMATE_EFFECT))
            .setSingleChoiceItems(resources.getTextArray(IResConfigManager.ARRAY_ANIMATE_EFFECT_ENTRIES), 
                    selectitem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        
                    String strselectitem = getSharedPrefSettingsManager().getString(SharedPrefSettingsManager.KEY_ANIMATE_EFFECT_TYPE, 
                            defaultAnimateEffect);
                    int selectitem = 0;
                    if(strselectitem != null && animateArray != null){
                        for(String str : animateArray){
                            if(str.equals(strselectitem)){
                                break;
                            }
                            selectitem++;
                        }
                        if(selectitem >= animateArray.length)
                            selectitem = 0;
                    }
                    
                    if(selectitem != whichButton && whichButton < animateArray.length){
                        getSharedPrefSettingsManager().setString(SharedPrefSettingsManager.KEY_ANIMATE_EFFECT_TYPE, 
                                animateArray[whichButton]);
                        
                        mAnimateEffectType = Workspace.AnimateEffectType.valueOf(animateArray[whichButton]);
//                        if(mWorkspace != null){
//                            mWorkspace.setAnimateEffectType(Workspace.AnimateEffectType.valueOf(animateArray[whichButton]));
//                        }
                    }
                    dialog.dismiss();
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    dialog.dismiss();
                }
            })
           .create();
	    }
        return null;
    }
	
	public void onPrepareDialog(int id, Dialog dialog) {
	    
	}

	public void onConfigurationChanged(Configuration newConfig){
		
	}

	public void onAttachFragment(Fragment fragment) {
    }
	
	public void showWorkspace(boolean animated) {
		showWorkspace(animated, null);
	}
	
	public void showWorkspace(boolean animated, Runnable onCompleteRunnable) {
	    if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "showWorkspace: animated = " + animated + ", mState = " + mState
                    );
        }
	    
	    if(mState == State.WORKSPACE_SPRING_LOADED){
	        Animator workspaceAnim = mWorkspace.getChangeStateAnimation(
                    Workspace.State.NORMAL, animated, 0);
	        if (workspaceAnim != null) {
	            workspaceAnim.start();
            }
	        mState = State.WORKSPACE;
        } else if (mState != State.WORKSPACE) {
			if(mWorkspacePreviewView != null) mWorkspacePreviewView.setVisibility(View.GONE);
			if(mCustomSettingsView != null) mCustomSettingsView.setVisibility(View.GONE);
			mWorkspace.setVisibility(View.VISIBLE);
		}
	}
	
	public void showAllApps(boolean animated){
		if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "showAllApps: animated = " + animated + ", mState = " + mState
                    );
        }
        if (mState != State.WORKSPACE) return;
	}
	
	protected void setAllAppsRootViewVisibility(int visibility){
		((View)getAppsCustomizePanel()).setVisibility(visibility);
		//mAppsCustomizeTabHost.setVisibility(View.GONE);
	}
	
	public void enterSpringLoadedDragMode() {
		if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "enterSpringLoadedDragMode mState = " + mState + ", mOnResumeState = " + mOnResumeState);
        }
        
    	if (isAllAppsVisible() && mState != State.APPS_CUSTOMIZE_SPRING_LOADED) {
            hideAppsCustomizeHelper(State.APPS_CUSTOMIZE_SPRING_LOADED, true, true, null);
            //hideDockDivider();
            mState = State.APPS_CUSTOMIZE_SPRING_LOADED;
        } else if(mState == State.WORKSPACE){
            Animator workspaceAnim = mWorkspace.getChangeStateAnimation(
                    Workspace.State.SPRING_LOADED, true);
            if (workspaceAnim != null) {
                workspaceAnim.start();
            }
            mState = State.WORKSPACE_SPRING_LOADED;
        }
	}
	
	public void exitSpringLoadedDragModeDelayed(final boolean successfulDrop, boolean extendedDelay,
            final Runnable onCompleteRunnable) {
		
		if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "exitSpringLoadedDragModeDelayed successfulDrop = " + successfulDrop + ", extendedDelay = "
                    + extendedDelay + ", mState = " + mState);
        }

        if (mState != State.APPS_CUSTOMIZE_SPRING_LOADED && mState != State.WORKSPACE_SPRING_LOADED) {
            return;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (successfulDrop) {
                    // Before we show workspace, hide all apps again because
                    // exitSpringLoadedDragMode made it visible. This is a bit hacky; we should
                    // clean up our state transition functions
                	setAllAppsRootViewVisibility(View.GONE);
                    //mAppsCustomizeTabHost.setVisibility(View.GONE);
                    showWorkspace(true, onCompleteRunnable);
                } else {
                    exitSpringLoadedDragMode();
                }
            }
        }, (extendedDelay ?
                EXIT_SPRINGLOADED_MODE_LONG_TIMEOUT :
                EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT));
	}
	
	public void exitSpringLoadedDragMode() {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "exitSpringLoadedDragMode mState = " + mState);
        }

        if (mState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            final boolean animated = true;
            final boolean springLoaded = true;
            showAppsCustomizeHelper(animated, springLoaded);
            mState = State.APPS_CUSTOMIZE;
        } else if(mState == State.WORKSPACE_SPRING_LOADED){
            showWorkspace(true);
            mState = State.WORKSPACE;
        }
        // Otherwise, we are not in spring loaded mode, so don't do anything.
    }
	
	protected void showAppsCustomizeHelper(final boolean animated, final boolean springLoaded) {
		
	}
	
	protected void hideAppsCustomizeHelper(int toState, final boolean animated,
            final boolean springLoaded, final Runnable onCompleteRunnable) {
		
	}
	
	public void setFullscreen(boolean fullscreen){
		Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (fullscreen) {
            winParams.flags |=  bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
	}
	
	public boolean getShortcutLableVisiable(){
	    return getSharedPrefSettingsManager().getBoolean(SharedPrefSettingsManager.KEY_WORKSPACE_SHOW_LABEL, 
	            ResConfigManager.CONFIG_SHOW_WORKSPACE_LABEL, true);
	}
	
	public void setShortcutLableVisiable(final boolean visiable, final Runnable onCompleteRunnable){
	    mHandler.post(new Runnable() {
            @Override
            public void run() {
                Hotseat hotseat = getHotseat(); 
                if(hotseat != null){
                    hotseat.setShortcutLabelVisiable(visiable);
                }
                
                Workspace workspace = getWorkspace();
                if(workspace != null)
                    workspace.setShortcutLabelVisiable(visiable);
                
                if(mAttached && onCompleteRunnable != null)
                    onCompleteRunnable.run();
            }
        });
	}
	
//	protected void changeWorkspaceGridSize(ISharedPrefSettingsManager sharePrefManager, int x, int y){
//	    if(x > 0 && y > 0 && (x != sharePrefManager.getWorkspaceCountCellX() || 
//	        y != sharePrefManager.getWorkspaceCountCellY())){
//	        
////	        android.util.Log.i("QsLog", "changeWorkspaceGridSize====="
////	                +"==x:"+sharePrefManager.getWorkspaceCountCellX()
////	                +"==y:"+sharePrefManager.getWorkspaceCountCellY()
////	                +"==x:"+x
////                    +"==y:"+y);
//	        
//	        final Workspace workspace = getWorkspace();
//            if(workspace != null){
//                int count = workspace.getPageCount();
//                for(int i=0; i<count; i++){
//                    final CellLayout page = (CellLayout)workspace.getPageAt(i);
//                    if(page != null){
//                        page.setGridSize(x, y);
//                    }
//                }
//            }
//            
//            sharePrefManager.setWorkspaceCountCellX(x);
//            sharePrefManager.setWorkspaceCountCellY(y);
//	    }
//	}
	
	private void updateWidgetsSizeIfNecessary(CellLayout page, int x, int y){
	    ShortcutAndWidgetContainer swc = page.getShortcutsAndWidgets();
        final int itemCount = swc.getChildCount();
        for (int j = 0; j < itemCount; j++) {
            View v = swc.getChildAt(j);
            if (v.getTag() instanceof LauncherAppWidgetInfo) {
                LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
                LauncherAppWidgetHostView lahv = (LauncherAppWidgetHostView) info.hostView;
                if (lahv != null) {
                    info.updateWidgetSize(this, x, y, true);
                }
            }
        }
	}
	
	public void changeWorkspaceGridSize(final ISharedPrefSettingsManager sharePrefManager, 
	        final int x, final int y, final Runnable onCompleteRunnable){
	    final Workspace workspace = getWorkspace();
        if(workspace != null){
            
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int currentPage = Math.max(0, workspace.getCurrentPage());
                    int count = workspace.getPageCount();
//                    
//                    android.util.Log.d("QsLog", "Launcher::changeWorkspaceGridSize====="
//                            +"==currentPage:"+currentPage
//                            +"==count:"+count
//                            +"==x:"+x+"==y:"+y);
//                    
                    final int oldX = sharePrefManager.getWorkspaceCountCellX();
                    final int oldY = sharePrefManager.getWorkspaceCountCellY();
                    
                    workspace.cleanupReorder(true);
                    sharePrefManager.setWorkspaceCountCellX(x);
                    sharePrefManager.setWorkspaceCountCellY(y);
                    AppWidgetResizeFrame.resetCellLayoutMetricsCache();

                    for(int i=currentPage; i<count && mAttached; i++){
                        final CellLayout page = (CellLayout)workspace.getPageAt(i);
                        if(page != null){
                            page.setGridSize(x, y);
                            //updateWidgetsSizeIfNecessary(page, x, y);
                        }
                    }
                    
                    for(int i=0; i<currentPage && mAttached; i++){
                        final CellLayout page = (CellLayout)workspace.getPageAt(i);
                        if(page != null){
                            page.setGridSize(x, y);
                            //updateWidgetsSizeIfNecessary(page, x, y);
                        }
                    }

                    if(mAttached){
                        IAppsCustomizePanel appPanel = getAppsCustomizePanel();
                        if(appPanel != null)
                            appPanel.clearAllWidgetPages();
                        
                        workspace.requestLayout();
                        
                        if(!getModel().isLoadingWorkspace() && 
                                    (x > oldX || y > oldY)){
                            getModel().startLoader(false, -1, false);
                        }

                        if(onCompleteRunnable != null)
                            onCompleteRunnable.run();
                    }
                }
            });
        }
	}
	
//	private Dialog mSelectGridSizeDialog;
//	public void changeWorkspaceGridSize(final Runnable onCompleteRunnable){
//	    
//	    if(mSelectGridSizeDialog != null)
//	        mSelectGridSizeDialog.dismiss();
//	    
//	    final IResConfigManager resources = getResConfigManager();
//        final ISharedPrefSettingsManager sharePrefManager = getSharedPrefSettingsManager();
//        final int currentX = sharePrefManager.getWorkspaceCountCellX();
//        final int currentY = sharePrefManager.getWorkspaceCountCellY();
//        final String [] gridSizeValues = resources.getStringArray(IResConfigManager.ARRAY_WORKSPACE_GRID_SIZE_VALUES);
//        final int currentChecked = WorkspaceCustomSettings.getSelectWorkspaceGridSize(gridSizeValues, 
//                currentX, currentY);
//        
////        android.util.Log.i("QsLog", "changeWorkspaceGridSize=====currentChecked:"+currentChecked
////                +"==x:"+currentX
////                +"==y:"+currentY);
//        
//        mSelectGridSizeDialog = new AlertDialog.Builder(getActivity())
//            .setTitle(resources.getString(IResConfigManager.STR_CUSTOM_SETTINGS_LABEL))
//            .setSingleChoiceItems(resources.getTextArray(IResConfigManager.ARRAY_WORKSPACE_GRID_SIZE_ENTRIES), 
//                    currentChecked, 
//                    new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int whichButton) {
//    
//                    /* User clicked on a radio button do some stuff */
////                    android.util.Log.i("QsLog", "=====currentChecked:"+currentChecked
////                            +"==whichButton:"+whichButton);
//                    if(mAttached && whichButton < gridSizeValues.length){
//                        String[] xy = gridSizeValues[whichButton].split("x");
//                        if(xy.length == 2){
//                            try {
//                                changeWorkspaceGridSize(sharePrefManager, Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
//                            } catch (NumberFormatException ex){
//                                
//                            }
//                        }
//                    }
//                }
//            })
//            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int whichButton) {
//    
////                    android.util.Log.i("QsLog", "=ok====currentChecked:"+currentChecked
////                            +"==whichButton:"+whichButton);
//                    
//                    /* User clicked Yes so do some stuff */
//                    //mSelectGridSizeDialog.dismiss();
//                    //mSelectGridSizeDialog = null;
////                    if(mAttached && onCompleteRunnable != null){
////                        mHandler.post(onCompleteRunnable);
////                    }
//                }
//            })
//            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int whichButton) {
////                    android.util.Log.i("QsLog", "=cancel====currentChecked:"+currentChecked
////                            +"==whichButton:"+whichButton);
//                    /* User clicked No so do some stuff */
//                    //mSelectGridSizeDialog.dismiss();
//                    
//                    //mSelectGridSizeDialog = null;
//                    if(mAttached){
//                        changeWorkspaceGridSize(sharePrefManager, currentX, currentY);
////                        if(onCompleteRunnable != null)
////                            mHandler.post(onCompleteRunnable);
//                    }
//                }
//            })
//            .setOnDismissListener(new DialogInterface.OnDismissListener() {
//                
//                @Override
//                public void onDismiss(DialogInterface dialog) {
//                    // TODO Auto-generated method stub
//                    //android.util.Log.i("QsLog", "onDismiss===");
//                    mSelectGridSizeDialog = null;
//                    if(mAttached){
//                        //changeWorkspaceGridSize(sharePrefManager, currentX, currentY);
//                        if(onCompleteRunnable != null)
//                            mHandler.post(onCompleteRunnable);
//                    }
//                }
//            })
//           .show();
//	}
	
	public boolean isHotseatVisiable(){
		return (mHotseat != null && (mHotseat.getVisibility() == View.VISIBLE/* || mHotseat.getAlpha() > 0f*/));
	}
	/**
     * Shows the hotseat area.
     */
    public void showHotseat(boolean animated) {
//        if (!isScreenLarge()) {
//            if (animated) {
//                if (mHotseat.getAlpha() != 1f) {
//                    int duration = 0;
//                    if (mSearchDropTargetBar != null) {
//                        duration = mSearchDropTargetBar.getTransitionInDuration();
//                    }
//                    mHotseat.animate().alpha(1f).setDuration(duration);
//                }
//            } else {
//                mHotseat.setAlpha(1f);
//            }
//        }
    	mHotseat.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the hotseat area.
     */
    public void hideHotseat(boolean animated) {
//        if (!isScreenLarge()) {
//            if (animated) {
//                if (mHotseat.getAlpha() != 0f) {
//                    int duration = 0;
//                    if (mSearchDropTargetBar != null) {
//                        duration = mSearchDropTargetBar.getTransitionOutDuration();
//                    }
//                    mHotseat.animate().alpha(0f).setDuration(duration);
//                }
//            } else {
//                mHotseat.setAlpha(0f);
//            }
//        }
        mHotseat.setVisibility(View.GONE);
    }
    
    public final void dismissDialog(int id) {
        getActivity().dismissDialog(id);
    }
    
    public final void removeDialog(int id) {
        getActivity().removeDialog(id);
    }

	public boolean closeSystemDialogs(){
		// Whatever we were doing is hereby canceled.
//	    if(mSelectGridSizeDialog != null){
//	        mSelectGridSizeDialog.cancel();
//	        mSelectGridSizeDialog = null;
//	    }
//	    if(isAllAppsVisible())
//	        removeDialog(MENU_APP_ICONS_SORT);
//	    if(mAlertDialog != null){
//	        mAlertDialog.cancel();
//	        mAlertDialog = null;
//	    }
        mWaitingForResult = false;
        return false;
	}
	
	public boolean startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch){
		
		if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "startSearch.");
        }
        showWorkspace(true);

        if (initialQuery == null) {
            // Use any text typed in the launcher as the initial query
            initialQuery = getTypedText();
        }
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString(Search.SOURCE, "launcher-search");
        }
        Rect sourceBounds = new Rect();
        if (getSearchBar() != null) {
            sourceBounds = getSearchBar().getSearchBarBounds();
        }

        startGlobalSearch(initialQuery, selectInitialQuery,
            appSearchData, sourceBounds);
        
		return true;
	}
	
	/**
     * Starts the global search activity. This code is a copied from SearchManager
     */
    public void startGlobalSearch(String initialQuery,
            boolean selectInitialQuery, Bundle appSearchData, Rect sourceBounds) {
        final SearchManager searchManager =
            (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName globalSearchActivity = searchManager.getGlobalSearchActivity();
        if (globalSearchActivity == null) {
            Util.Log.w(TAG, "No global search activity found.");
            return;
        }
        Intent intent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(globalSearchActivity);
        // Make sure that we have a Bundle to put source in
        if (appSearchData == null) {
            appSearchData = new Bundle();
        } else {
            appSearchData = new Bundle(appSearchData);
        }
        // Set source to package name of app that starts global search, if not set already.
        if (!appSearchData.containsKey("source")) {
            appSearchData.putString("source", getPackageName());
        }
        intent.putExtra(SearchManager.APP_DATA, appSearchData);
        if (!TextUtils.isEmpty(initialQuery)) {
            intent.putExtra(SearchManager.QUERY, initialQuery);
        }
        if (selectInitialQuery) {
            intent.putExtra(SearchManager.EXTRA_SELECT_QUERY, selectInitialQuery);
        }
        intent.setSourceBounds(sourceBounds);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Util.Log.e(TAG, "Global search activity not found: " + globalSearchActivity);
        }
    }
	
	public boolean onSearchRequested() {
		return getActivity().onSearchRequested();
	}
	
	protected boolean acceptFilter() {
        final InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        return !inputManager.isFullscreenMode();
    }
	
	protected String getTypedText() {
        return mDefaultKeySsb.toString();
    }

	protected void clearTypedText() {
        mDefaultKeySsb.clear();
        mDefaultKeySsb.clearSpans();
        Selection.setSelection(mDefaultKeySsb, 0);
    }
	
	public boolean onKeyDown(int keyCode, KeyEvent event, boolean superResult){
		final int uniChar = event.getUnicodeChar();
        final boolean handled = superResult;
        final boolean isKeyNotWhitespace = uniChar > 0 && !Character.isWhitespace(uniChar);
        if (Util.DEBUG_KEY) {
        	Util.Log.d(TAG, " onKeyDown: KeyCode = " + keyCode + ", KeyEvent = " + event
                    + ", uniChar = " + uniChar + ", handled = " + handled + ", isKeyNotWhitespace = "
                    + isKeyNotWhitespace);
        }
        //android.util.Log.i("QsLog", "onKeyDown()==handled:"+handled);
        
        if (!handled && acceptFilter() && isKeyNotWhitespace) {
            boolean gotKey = TextKeyListener.getInstance().onKeyDown((View)getWorkspace(), mDefaultKeySsb,
                    keyCode, event);
            if (gotKey && mDefaultKeySsb != null && mDefaultKeySsb.length() > 0) {
                // something usable has been typed - start a search
                // the typed text will be retrieved and cleared by
                // showSearchDialog()
                // If there are multiple keystrokes before the search dialog takes focus,
                // onSearchRequested() will be called for every keystroke,
                // but it is idempotent, so it's fine.
                return onSearchRequested();
            }
        }

        // Eat the long press event so the keyboard doesn't come up.
        if (keyCode == KeyEvent.KEYCODE_MENU && event.isLongPress()) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            /// M: invalidate the option menu before menu pop ups. Since the
            // menu item count differs between workspace and app list, if we
            // press menu key in workspace, and then do it in app list,the
            // menu window will animate because window size changed. We add this
            // step to force re-create menu decor view, this would lower the
            // time duration of option menu pop ups. Also we could do it only
            // when the menu pop switch between workspace and app list.
            invalidateOptionsMenu();
        }

        return handled;
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event, boolean superResult) {
		//final boolean handled = getLauncherActivity().super.onKeyDown(keyCode, event);
		
		return superResult;
	}
	
	public boolean isHotseatLayout(View layout) {
        return getHotseat() != null && layout != null &&
                (layout instanceof CellLayout) && (layout == getHotseat().getLayout());
    }
	public boolean isWorkspaceLocked() {
        return mWorkspaceLoading || mWaitingForResult;
    }
	public static int getScreen() {
        synchronized (sLock) {
            return sScreen;
        }
    }

	public static void setScreen(int screen) {
        synchronized (sLock) {
            sScreen = screen;
        }
    }
	
	public void onClickAllAppsButton(View v) {
        showAllApps(true);
    }
	
	public void onTouchDownAllAppsButton(View v) {
        // Provide the same haptic feedback that the system offers for virtual keys.
        v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
    }
	
	public boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return !getModel().isLoadingWorkspace();
    }
	
	public CellLayout getCellLayout(long container, int screen) {
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            if (getHotseat() != null) {
                return getHotseat().getLayout();
            } else {
                return null;
            }
        } else {
            return (CellLayout) getWorkspace().getChildAt(screen);
        }
    }
	
	public int getOrderInHotseat(final int cellX, final int cellY){
		if (getHotseat() != null) {
            return getHotseat().getOrderInHotseat(cellX, cellY);
        }
		return cellX;
	}
	
	public void snapToWorkspaceScreen(int screen){
		snapToWorkspaceScreen(screen, true);
	}
	
	public boolean isCustomSettingsScreenVisible(){
    	return (mState == State.CUSTOM_SETTINGS || mOnResumeState == State.CUSTOM_SETTINGS 
    			/*|| (mCustomSettingsView != null && mCustomSettingsView.getVisibility() == View.VISIBLE)*/);
    }
	
	public void hideCustomSettingsScreen(boolean isAnimation){
		//android.util.Log.w("QsLog", "hideCustomSettingsScreen=======mState:"+mState+"======");
		if (mState != State.CUSTOM_SETTINGS) return;
		
		if(mWorkspaceContainer != null) mWorkspaceContainer.setVisibility(View.VISIBLE);
    	
    	if(mCustomSettingsView != null) mCustomSettingsView.hide();

		mState = State.WORKSPACE;
		if(mWorkspaceContainer != null) mWorkspaceContainer.bringToFront();
	}
	
	public void showCustomSettingsScreen(boolean isAnimation){
		//android.util.Log.w("QsLog", "showCustomSettingsScreen=======mState:"+mState+"======");
		if (mState == State.CUSTOM_SETTINGS || mState != State.WORKSPACE || mWorkspace == null || mCustomSettingsView == null) return;
		
		mCustomSettingsView.show(mWorkspace);
		if(mWorkspaceContainer != null) mWorkspaceContainer.setVisibility(View.INVISIBLE);
		mState = State.CUSTOM_SETTINGS;
	}
	
	public boolean isWorkspacePreviewVisible(){
    	return (mState == State.WORKSPACE_PREVIEW || mOnResumeState == State.WORKSPACE_PREVIEW 
    			/*|| (mWorkspacePreviewView != null && mWorkspacePreviewView.getVisibility() == View.VISIBLE)*/);
    }
	
	public void snapToWorkspaceScreen(int screen, boolean showWorkspace){
    	
    	//android.util.Log.w("QsLog", "snapToPage======="+screen+"======");
    	
    	if (mWorkspace == null) return;
    	
    	mWorkspace.snapToPage(screen);
    	
    	if(showWorkspace){
    		hideWorkspacePreiews(false);
    		hideCustomSettingsScreen(false);
    	}
    }
    
    public void hideWorkspacePreiews(boolean isAnimation){
    	
    	if (mState != State.WORKSPACE_PREVIEW) return;
		
    	if(mWorkspaceContainer != null) mWorkspaceContainer.setVisibility(View.VISIBLE);
    	
    	if(mWorkspacePreviewView != null) mWorkspacePreviewView.hide();

		mState = State.WORKSPACE;

		if(mWorkspaceContainer != null) mWorkspaceContainer.bringToFront();
    }
    
    public void showWorkspacePreviews(boolean isAnimation) {
    	
    	//Util.Log.i(TAG, "showWorkspacePreviews()===mState:"+mState); 
    	
    	if (mState == State.WORKSPACE_PREVIEW || mState != State.WORKSPACE || mWorkspace == null || mWorkspacePreviewView == null) return;

    	mWorkspacePreviewView.show(mWorkspace);
    	
    	if(mWorkspaceContainer != null) mWorkspaceContainer.setVisibility(View.INVISIBLE);
    	
    	mState = State.WORKSPACE_PREVIEW;
    	
    	//android.util.Log.w("QsLog", "showPreviews()===mState:"+mState); 
    }
	
	public View createShortcut(ShortcutInfo info) {
        return createShortcut(IResConfigManager.LAYOUT_APPLICATION_SHORTCUT, info);
    }
	
	public View createShortcut(int layoutResId, ShortcutInfo info) {
        return createShortcut(layoutResId,
                (ViewGroup) getWorkspace().getChildAt(getWorkspace().getCurrentPage()), 
                info);
    }
	
	public View createShortcut(ViewGroup parent, ShortcutInfo info){
		View view = getResConfigManager().inflaterView(IResConfigManager.LAYOUT_APPLICATION_SHORTCUT, parent);//mLauncherExtHelper.createShortcut(parent);
		return applyShortcutInfo(view, info);
	}

    public View createShortcut(int layoutResId, ViewGroup parent, ShortcutInfo info) {
        View view = getResConfigManager().inflaterView(layoutResId, parent);//mLauncherExtHelper.createShortcut(parent);
        return applyShortcutInfo(view, info);
    	//MtpShortcutView shortcut = (MtpShortcutView)mInflater.inflate(layoutResId, parent, false);
        //return applyShortcutInfo(mInflater.inflate(layoutResId, parent, false), info);
    }
    
    public View applyShortcutInfo(View view, ShortcutInfo info) {
        /// M: modified for unread feature, the icon is playced in a RelativeLayout,
        /// we should set click/longClick listener for the icon not the RelativeLayout.
    	if(view != null){
	    	if(view instanceof MtpShortcutView){
	    		MtpShortcutView shortcut = (MtpShortcutView)view;	    		
	    		shortcut.setViewLabelVisible(getShortcutLableVisiable());
	    		shortcut.applyFromShortcutInfo(info, getIconCache());
		        //shortcut.setTitle(info.title);
	    		shortcut.mFavorite.setOnClickListener(this);
	    		shortcut.mFavorite.setOnLongClickListener(this);
	    	} else if(view instanceof BubbleTextView){
	    		BubbleTextView shortcut = (BubbleTextView)view;
	    		shortcut.setViewLabelVisible(getShortcutLableVisiable());
	    		shortcut.applyFromShortcutInfo(info, getIconCache());
	    		shortcut.setOnClickListener(this);
	    		shortcut.setOnLongClickListener(this);
	    	}
    	}
        return view;
    }
	
    public void startActivityForResult(Intent intent, int requestCode) {
        if (requestCode >= 0) {
            mWaitingForResult = true;
        }
        this.getActivity().startActivityForResult(intent, requestCode);
    }
    
    public void startActivity(Intent intent) {
    	getActivity().startActivity(intent);
    }
    
	public boolean startActivity(View v, Intent intent, Object tag) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "startActivity v = " + v + ", intent = " + intent + ", tag = " + tag);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            // Only launch using the new animation if the shortcut has not opted out (this is a
            // private contract between launcher and may be ignored in the future).
            boolean useLaunchAnimation = (v != null) &&
                    !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
            if (useLaunchAnimation) {
                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                        v.getMeasuredWidth(), v.getMeasuredHeight());

                getActivity().startActivity(intent, opts.toBundle());
            } else {
            	getActivity().startActivity(intent);
            }
            return true;
        } catch (SecurityException e) {
            Toast.makeText(getActivity(), getResConfigManager().getText(IResConfigManager.STR_ACTIVITY_NOT_FOUND), 
            		Toast.LENGTH_SHORT).show();
            Util.Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity. "
                    + "tag=" + tag + " intent=" + intent, e);
        }
        return false;
    }
	
	public boolean startActivitySafely(View v, Intent intent, Object tag) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "startActivitySafely v = " + v + ", intent = " + intent + ", tag = " + tag);
        }

        boolean success = false;
        try {
            success = startActivity(v, intent, tag);
        } catch (ActivityNotFoundException e) {
            //Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        	Util.Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
        }
        return success;
    }

    public void startActivityForResultSafely(Intent intent, int requestCode) {
    	if (Util.ENABLE_DEBUG) {
    		Util.Log.d(TAG, "startActivityForResultSafely: intent = " + intent
                    + ", requestCode = " + requestCode);
        }

        try {
            getActivity().startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), getResConfigManager().getText(IResConfigManager.STR_ACTIVITY_NOT_FOUND), 
            		Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(getActivity(), getResConfigManager().getText(IResConfigManager.STR_ACTIVITY_NOT_FOUND), 
            		Toast.LENGTH_SHORT).show();
            Util.Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity.", e);
        }
    }
    
    public void startApplicationDetailsActivity(ComponentName componentName) {
        if (Util.ENABLE_DEBUG) {
    		Util.Log.d(TAG, "startApplicationDetailsActivity: componentName = " + componentName);
        }

        String packageName = componentName.getPackageName();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivitySafely(null, intent, "startApplicationDetailsActivity");
    }

    public void startApplicationUninstallActivity(ApplicationInfo appInfo) {
        if (Util.ENABLE_DEBUG) {
    		Util.Log.d(TAG, "startApplicationUninstallActivity: appInfo = " + appInfo);
        }

        //if ((appInfo.flags & ApplicationInfo.DOWNLOADED_FLAG) == 0) {
        if(!appInfo.isDownloadApp()){
            // System applications cannot be installed. For now, show a toast explaining that.
            // We may give them the option of disabling apps this way.
            //int messageId = R.string.uninstall_system_app_text;
            //Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
        	Toast.makeText(getActivity(), 
        			getResConfigManager().getString(IResConfigManager.STR_UNINSTALL_SYSTEM_APP), 
        			Toast.LENGTH_SHORT).show();
        } else {
            String packageName = appInfo.getPackageName();
            String className = appInfo.getClassName();
            startApplicationUninstallActivity(packageName, className);
        }
    }
    
    public void startApplicationUninstallActivity(String packageName, String className) {
        Intent intent = new Intent(
                Intent.ACTION_DELETE, Uri.fromParts("package", packageName, className));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        getActivity().startActivity(intent);
    }
    
    public void startWallpaper() {
        showWorkspace(true);
        final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        Intent chooser = Intent.createChooser(pickWallpaper,
                getResConfigManager().getText(IResConfigManager.STR_CHOOSE_WALLPAPER));
        // NOTE: Adds a configure option to the chooser if the wallpaper supports it
        //       Removed in Eclair MR1
//        WallpaperManager wm = (WallpaperManager)
//                getSystemService(Context.WALLPAPER_SERVICE);
//        WallpaperInfo wi = wm.getWallpaperInfo();
//        if (wi != null && wi.getSettingsActivity() != null) {
//            LabeledIntent li = new LabeledIntent(getPackageName(),
//                    R.string.configure_wallpaper, 0);
//            li.setClassName(wi.getPackageName(), wi.getSettingsActivity());
//            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { li });
//        }
        startActivityForResult(chooser, REQUEST_PICK_WALLPAPER);
    }
    
    public int[] getSpanForWidget(ComponentName component, int minWidth,
            int minHeight) {
        Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(this, component, null);
        // We want to account for the extra amount of padding that we are adding to the widget
        // to ensure that it gets the full amount of space that it has requested
        int requiredWidth = minWidth + padding.left + padding.right;
        int requiredHeight = minHeight + padding.top + padding.bottom;
        if(getWorkspace() != null && getWorkspace().getNormalCellLayout() != null)
        	return getWorkspace().getNormalCellLayout().rectToCell(requiredWidth, requiredHeight, null);
        else
        	return CellLayout.rectToCell(this, requiredWidth, requiredHeight, null);
    }
    
    public static int[] getSpanForWidget(LauncherHelper launcher, ComponentName component, int minWidth,
            int minHeight) {
        Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(launcher, component, null);
        // We want to account for the extra amount of padding that we are adding to the widget
        // to ensure that it gets the full amount of space that it has requested
        int requiredWidth = minWidth + padding.left + padding.right;
        int requiredHeight = minHeight + padding.top + padding.bottom;
        return CellLayout.rectToCell(launcher, requiredWidth, requiredHeight, null);
    }

    public static int[] getSpanForWidget(LauncherHelper launcher, AppWidgetProviderInfo info) {
        return getSpanForWidget(launcher, info.provider, info.minWidth, info.minHeight);
    }

    public static int[] getMinSpanForWidget(LauncherHelper launcher, AppWidgetProviderInfo info) {
        return getSpanForWidget(launcher, info.provider, info.minResizeWidth, info.minResizeHeight);
    }

    public static int[] getSpanForWidget(LauncherHelper launcher, PendingAddWidgetInfo info) {
        return getSpanForWidget(launcher, info.componentName, info.minWidth, info.minHeight);
    }

    public static int[] getMinSpanForWidget(LauncherHelper launcher, PendingAddWidgetInfo info) {
        return getSpanForWidget(launcher, info.componentName, info.minResizeWidth,
                info.minResizeHeight);
    }
    
    public void showOutOfSpaceMessage(boolean isHotseatLayout) {
        String str = (isHotseatLayout ? getResConfigManager().getString(IResConfigManager.STR_HOTSEAT_OUT_OF_SPACE)
        				: getResConfigManager().getString(IResConfigManager.STR_OUT_OF_SPACE));
        //R.string.hotseat_out_of_space : R.string.out_of_space);
        Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
    }
    
    public void updateWallpaperVisibility(boolean visible){
        if(true){
        	int wpflags = visible ? WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER : 0;
            int curflags = getWindow().getAttributes().flags
                    & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
            if (wpflags != curflags) {
                getWindow().setFlags(wpflags, WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            }
        }
    }
    /**
     * Add an application shortcut to the workspace.
     *
     * @param data The intent describing the application.
     * @param cellInfo The position on screen where to create the shortcut.
     */
    public void completeAddApplication(Intent data, long container, int screen, int cellX, int cellY) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "completeAddApplication: Intent = " + data
                    + ", container = " + container + ", screen = " + screen + ", cellX = " + cellX
                    + ", cellY = " + cellY);
        }    

        final int[] cellXY = mTmpAddItemCellCoordinates;
        final CellLayout layout = getCellLayout(container, screen);

        // First we check if we already know the exact location where we want to add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
        } else if (!layout.findCellForSpan(cellXY, 1, 1)) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        final ShortcutInfo info = getModel().getShortcutInfo(getPackageManager(), data, this);

        if (info != null) {
            info.setActivity(data.getComponent(), Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            info.container = ItemInfo.NO_ID;
            mWorkspace.addApplicationShortcut(info, layout, container, screen, cellXY[0], cellXY[1],
                    isWorkspaceLocked(), cellX, cellY);
        } else {
            Util.Log.e(TAG, "Couldn't find ActivityInfo for selected application: " + data);
        }
    }

    /**
     * Add a shortcut to the workspace.
     *
     * @param data The intent describing the shortcut.
     * @param cellInfo The position on screen where to create the shortcut.
     */
    protected void completeAddShortcut(Intent data, long container, int screen, int cellX,
            int cellY) {
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        CellLayout layout = getCellLayout(container, screen);

        boolean foundCellSpan = false;

        ShortcutInfo info = getModel().infoFromShortcutIntent(this, data, null);
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "completeAddShortcut: info = " + info + ", data = " + data
                    + ", container = " + container + ", screen = " + screen + ", cellX = "
                    + cellX + ", cellY = " + cellY);
        }

        if (info == null) {
            return;
        }
        final View view = createShortcut(info);

        // First we check if we already know the exact location where we want to add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
            foundCellSpan = true;

            // If appropriate, either create a folder or add to an existing folder
            if (mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0,
                    true, null,null)) {
                return;
            }
            DragObject dragObject = new DragObject();
            dragObject.dragInfo = info;
            if (mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0, dragObject,
                    true)) {
                return;
            }
        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(touchXY[0], touchXY[1], 1, 1, cellXY);
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, 1, 1);
        }

        if (!foundCellSpan) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        getModel().addItemToDatabase(info, container, screen, cellXY[0], cellXY[1], false);

        if (mIsLoadingWorkspace) {
        	getModel().forceReload();
        }
        
        if (!mRestoring) {
            mWorkspace.addInScreen(view, container, screen, cellXY[0], cellXY[1], 1, 1,
                    isWorkspaceLocked());
        }
    }
    
    protected void completeAddAppWidget(final int appWidgetId, long container, int screen,
            AppWidgetHostView hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null) {
            appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        }
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "completeAddAppWidget: appWidgetId = " + appWidgetId
                    + ", container = " + container + ", screen = " + screen);
        }

        // Calculate the grid spans needed to fit this widget
        CellLayout layout = getCellLayout(container, screen);
        
        /// M: If screen is -1, layout will be null, replaced with currentDropLayout.
        if (layout == null) {
            layout = mWorkspace.getCurrentDropLayout();
        }

        int[] minSpanXY = getMinSpanForWidget(this, appWidgetInfo);
        int[] spanXY = getSpanForWidget(this, appWidgetInfo);

        // Try finding open space on Launcher screen
        // We have saved the position to which the widget was dragged-- this really only matters
        // if we are placing widgets on a "spring-loaded" screen
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        int[] finalSpan = new int[2];
        boolean foundCellSpan = false;
        if (mPendingAddInfo.cellX >= 0 && mPendingAddInfo.cellY >= 0) {
            cellXY[0] = mPendingAddInfo.cellX;
            cellXY[1] = mPendingAddInfo.cellY;
            spanXY[0] = mPendingAddInfo.spanX;
            spanXY[1] = mPendingAddInfo.spanY;
            foundCellSpan = true;
        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(
                    touchXY[0], touchXY[1], minSpanXY[0], minSpanXY[1], spanXY[0],
                    spanXY[1], cellXY, finalSpan);
            spanXY[0] = finalSpan[0];
            spanXY[1] = finalSpan[1];
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, minSpanXY[0], minSpanXY[1]);
        }

        if (!foundCellSpan) {
            if (appWidgetId != -1) {
                // Deleting an app widget ID is a void call but writes to disk before returning
                // to the caller...
                new Thread("deleteAppWidgetId") {
                    public void run() {
                        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                    }
                }.start();
            }
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        // Build Launcher-specific widget info and save to database
        LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(appWidgetId,
                appWidgetInfo.provider);
        launcherInfo.spanX = spanXY[0];
        launcherInfo.spanY = spanXY[1];
        launcherInfo.minSpanX = mPendingAddInfo.minSpanX;
        launcherInfo.minSpanY = mPendingAddInfo.minSpanY;
        launcherInfo.minWidth = appWidgetInfo.minWidth;
        launcherInfo.minHeight = appWidgetInfo.minHeight;

        getModel().addItemToDatabase(launcherInfo,
                container, screen, cellXY[0], cellXY[1], false);

        if (mIsLoadingWorkspace) {
        	Util.Log.d(TAG, "Just Loading Workspace, force reload");
        	getModel().forceReload();
        }

        if (!mRestoring) {
            if (hostView == null) {
                // Perform actual inflation because we're live
                launcherInfo.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
                launcherInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            } else {
                // The AppWidgetHostView has already been inflated and instantiated
                launcherInfo.hostView = hostView;
            }

            launcherInfo.hostView.setTag(launcherInfo);
            launcherInfo.hostView.setVisibility(View.VISIBLE);
            launcherInfo.notifyWidgetSizeChanged(this);

            mWorkspace.addInScreen(launcherInfo.hostView, container, screen, cellXY[0], cellXY[1],
                    launcherInfo.spanX, launcherInfo.spanY, isWorkspaceLocked());

            addWidgetToAutoAdvanceIfNeeded(launcherInfo.hostView, appWidgetInfo);
        }
        resetAddInfo();
    }
    
    protected void completeTwoStageWidgetDrop(final int resultCode, final int appWidgetId) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "completeTwoStageWidgetDrop resultCode = " + resultCode + ", appWidgetId = " + appWidgetId
                    + ", mPendingAddInfo.screen = " + mPendingAddInfo.screen);
        }

        CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(mPendingAddInfo.screen);
        Runnable onCompleteRunnable = null;
        int animationType = 0;

        AppWidgetHostView boundWidget = null;
        if (resultCode == RESULT_OK) {
            animationType = Workspace.COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION;
            final AppWidgetHostView layout = mAppWidgetHost.createView(this, appWidgetId,
                    mPendingAddWidgetInfo);
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    completeAddAppWidget(appWidgetId, mPendingAddInfo.container,
                            mPendingAddInfo.screen, layout, null);
                    exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), false,
                            null);
                }
            };
        } else if (resultCode == RESULT_CANCELED) {
            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), false,
                            null);
                }
            };
        }
        if (mDragLayer.getAnimatedView() != null) {
            mWorkspace.animateWidgetDrop(mPendingAddInfo, cellLayout,
                    (DragView) mDragLayer.getAnimatedView(), onCompleteRunnable,
                    animationType, boundWidget, true);
        } else {
            // The animated view may be null in the case of a rotation during widget configuration
            onCompleteRunnable.run();
        }
    }
    
    /**
     * Returns whether we should delay spring loaded mode -- for shortcuts and widgets that have
     * a configuration step, this allows the proper animations to run after other transitions.
     */
    protected boolean completeAdd(PendingAddArguments args) {
        boolean result = false;
        switch (args.requestCode) {
            case REQUEST_PICK_APPLICATION:
                completeAddApplication(args.intent, args.container, args.screen, args.cellX,
                        args.cellY);
                break;
            case REQUEST_PICK_SHORTCUT:
                processShortcut(args.intent);
                break;
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(args.intent, args.container, args.screen, args.cellX,
                        args.cellY);
                result = true;
                break;
            case REQUEST_CREATE_APPWIDGET:
                int appWidgetId = args.intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                completeAddAppWidget(appWidgetId, args.container, args.screen, null, null);
                result = true;
                break;
            case REQUEST_PICK_WALLPAPER:
                // We just wanted the activity result here so we can clear mWaitingForResult
                break;
        }
        // Before adding this resetAddInfo(), after a shortcut was added to a workspace screen,
        // if you turned the screen off and then back while in All Apps, Launcher would not
        // return to the workspace. Clearing mAddInfo.container here fixes this issue
        resetAddInfo();
        return result;
    }
    
    protected void resetAddInfo() {
        mPendingAddInfo.container = ItemInfo.NO_ID;
        mPendingAddInfo.screen = -1;
        mPendingAddInfo.cellX = mPendingAddInfo.cellY = -1;
        mPendingAddInfo.spanX = mPendingAddInfo.spanY = -1;
        mPendingAddInfo.minSpanX = mPendingAddInfo.minSpanY = -1;
        mPendingAddInfo.dropPos = null;
    }
    
    public void addAppWidgetImpl(final int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget,
            AppWidgetProviderInfo appWidgetInfo) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "addAppWidgetImpl: appWidgetId = " + appWidgetId
                    + ", info = " + info + ", boundWidget = " + boundWidget 
                    + ", appWidgetInfo = " + appWidgetInfo);
        }

        if (appWidgetInfo.configure != null) {
            mPendingAddWidgetInfo = appWidgetInfo;

            // Launch over to configure widget, if needed
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResultSafely(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            // Otherwise just add it
            completeAddAppWidget(appWidgetId, info.container, info.screen, boundWidget,
                    appWidgetInfo);
            // Exit spring loaded mode if necessary after adding the widget
            exitSpringLoadedDragModeDelayed(true, false, null);
        }
    }

    /**
     * Process a shortcut drop.
     *
     * @param componentName The name of the component
     * @param screen The screen where it should be added
     * @param cell The cell it should be added to, optional
     * @param position The location on the screen where it was dropped, optional
     */
    public void processShortcutFromDrop(ComponentName componentName, long container, int screen,
            int[] cell, int[] loc) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "processShortcutFromDrop componentName = " + componentName + ", container = " + container
                    + ", screen = " + screen);
        }

        resetAddInfo();
        mPendingAddInfo.container = container;
        mPendingAddInfo.screen = screen;
        mPendingAddInfo.dropPos = loc;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }

        Intent createShortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        createShortcutIntent.setComponent(componentName);
        processShortcut(createShortcutIntent);
    }

    /**
     * Process a widget drop.
     *
     * @param info The PendingAppWidgetInfo of the widget being added.
     * @param screen The screen where it should be added
     * @param cell The cell it should be added to, optional
     * @param position The location on the screen where it was dropped, optional
     */
    public void addAppWidgetFromDrop(PendingAddWidgetInfo info, long container, int screen,
            int[] cell, int[] span, int[] loc) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "addAppWidgetFromDrop: info = " + info + ", container = " + container + ", screen = "
                    + screen);
        }

        resetAddInfo();
        mPendingAddInfo.container = info.container = container;
        mPendingAddInfo.screen = info.screen = screen;
        mPendingAddInfo.dropPos = loc;
        mPendingAddInfo.minSpanX = info.minSpanX;
        mPendingAddInfo.minSpanY = info.minSpanY;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }
        if (span != null) {
            mPendingAddInfo.spanX = span[0];
            mPendingAddInfo.spanY = span[1];
        }

        AppWidgetHostView hostView = info.boundWidget;
        int appWidgetId;
        if (hostView != null) {
            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetImpl(appWidgetId, info, hostView, info.info);
        } else {
            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            appWidgetId = getAppWidgetHost().allocateAppWidgetId();
            Bundle options = info.bindOptions;

            boolean success = false;
            if (options != null) {
                success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId,
                        info.componentName, options);
            } else {
                success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId,
                        info.componentName);
            }
            if (success) {
                addAppWidgetImpl(appWidgetId, info, null, info.info);
            } else {
                mPendingAddWidgetInfo = info.info;
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.componentName);
                // TODO: we need to make sure that this accounts for the options bundle.
                // intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
            }
        }
    }

    public void processShortcut(Intent intent) {
        // Handle case where user selected "Applications"
    	final IResConfigManager res = getResConfigManager();
        String applicationName = res.getString(IResConfigManager.STR_GROUP_APPLICATIONS);
        											//R.string.group_applications);
        String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "processShortcut: applicationName = " + applicationName
                    + ", shortcutName = " + shortcutName + ", intent = " + intent);
        }

        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
            pickIntent.putExtra(Intent.EXTRA_TITLE, res.getText(IResConfigManager.STR_TITLE_SELECT_APPLICATION));//R.string.title_select_application));
            startActivityForResultSafely(pickIntent, REQUEST_PICK_APPLICATION);
        } else {
            startActivityForResultSafely(intent, REQUEST_CREATE_SHORTCUT);
        }
    }

    public void processWallpaper(Intent intent) {
        startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
    }
    
    public boolean isRotationEnabled() {
        return mIsEnableRotation;
    }
    
    private int mapConfigurationOriActivityInfoOri(int configOri) {
        final Display d = getActivity().getWindowManager().getDefaultDisplay();
        int naturalOri = Configuration.ORIENTATION_LANDSCAPE;
        switch (d.getRotation()) {
        case Surface.ROTATION_0:
        case Surface.ROTATION_180:
            // We are currently in the same basic orientation as the natural orientation
            naturalOri = configOri;
            break;
        case Surface.ROTATION_90:
        case Surface.ROTATION_270:
            // We are currently in the other basic orientation to the natural orientation
            naturalOri = (configOri == Configuration.ORIENTATION_LANDSCAPE) ?
                    Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
            break;
        }

        int[] oriMap = {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        };
        // Since the map starts at portrait, we need to offset if this device's natural orientation
        // is landscape.
        int indexOffset = 0;
        if (naturalOri == Configuration.ORIENTATION_LANDSCAPE) {
            indexOffset = 1;
        }
        return oriMap[(d.getRotation() + indexOffset) % 4];
    }
    
    public void lockScreenOrientation() {
        if (isRotationEnabled()) {
            getActivity().setRequestedOrientation(mapConfigurationOriActivityInfoOri(getResources()
                    .getConfiguration().orientation));
        }
    }
    
    public void unlockScreenOrientation(boolean immediate) {
        if (isRotationEnabled()) {
            if (immediate) {
            	getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            } else {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                    	getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                }, 500);
            }
        }
    }
    
  /// M: Add for launch specified applications in landscape. @{
    private static final int ORIENTATION_0 = 0;
    private static final int ORIENTATION_90 = 90;
    private static final int ORIENTATION_180 = 180;
    private static final int ORIENTATION_270 = 270;
    private OrientationEventListener mOrientationListener;
    private int mLastOrientation = ORIENTATION_0;
    /**
     * M: Register OrientationListerner when onCreate.
     */
    protected void registerOrientationListener() {
//        mOrientationListener = new OrientationEventListener(Launcher.this) {
//            @Override
//            public void onOrientationChanged(int orientation) {
//                orientation = roundOrientation(orientation);
//                if (orientation != mLastOrientation) {
//                    if (mLastOrientation == Launcher.ORIENTATION_0 || mLastOrientation == Launcher.ORIENTATION_180) {
//                        if (orientation == Launcher.ORIENTATION_270 || orientation == Launcher.ORIENTATION_90) {
//                            boolean isRotateEnabled = Settings.System.getInt(getContentResolver(),
//                                    Settings.System.ACCELEROMETER_ROTATION, 1) != 0;
//                            if (isRotateEnabled) {
//                                String cmpName = Settings.System.getString(getContentResolver(),
//                                        Settings.System.LANDSCAPE_LAUNCHER);
//                                if (cmpName != null && !cmpName.equals("none")) {
//                                    fireAppRotated(cmpName);
//                                }
//                            }
//                        }
//                    }
//                    mLastOrientation = orientation;
//                }
//            }
//        };
//        final String cmpName = Settings.System.getString(getContentResolver(), Settings.System.LANDSCAPE_LAUNCHER);
//        if (cmpName != null && !cmpName.equals("none")) {
//            mOrientationListener.enable();
//        }
    }
    
    /**
     * M: Calculate orientation.
     *
     * @param orientation
     * @return
     */
    protected int roundOrientation(int orientation) {
        return ((orientation + 45) / 90 * 90) % 360;
    }
    
    /**
     * M: Launch the specified app with name of "cmpName" and intent action is intent.ACTION_ROTATED_MAIN.
     *
     * @param cmpName
     */
    private void fireAppRotated(String cmpName) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "fireAppRotated: cmpName = " + cmpName);
        }

//        String name[] = cmpName.split("/");
//        Intent intent = new Intent(Intent.ACTION_ROTATED_MAIN);
//        intent.setComponent(new ComponentName(name[0], name[1]));
//        startActivitySafely(null, intent, null);
    }
    
    /**
     * M: Enable OrientationListener when onResume.
     */
    protected void enableOrientationListener() {
        if(mOrientationListener == null) return;            
        final String cmpName = Settings.System.getString(getContentResolver(), Settings.System.LANDSCAPE_LAUNCHER);
        if (cmpName != null && !cmpName.equals("none")) {
            if (mOrientationListener.canDetectOrientation()) {
                mOrientationListener.enable();
                mLastOrientation = Launcher.ORIENTATION_270;
            } else {
                //Toast.makeText(this.getActivity(), R.string.orientation, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * M: Disable OrientationListener when onPause/onDestory.
     */
    protected void disableOrientationListener() {
        if(mOrientationListener == null) return;
        final String cmpName = Settings.System.getString(getContentResolver(), Settings.System.LANDSCAPE_LAUNCHER);
        if (cmpName != null && !cmpName.equals("none")) {
            mLastOrientation = Launcher.ORIENTATION_0;
            mOrientationListener.disable();
        }
    }
    
    // Related to the auto-advancing of widgets
    protected final int ADVANCE_MSG = 1;
    protected final int mAdvanceInterval = 20000;
    protected final int mAdvanceStagger = 250;
    protected long mAutoAdvanceSentTime;
    protected long mAutoAdvanceTimeLeft = -1;
    protected boolean mAutoAdvanceRunning = false;
    protected HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance =
	        new HashMap<View, AppWidgetProviderInfo>();
	
	public void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (Util.ENABLE_DEBUG) {
			Util.Log.d(TAG, "addWidgetToAutoAdvanceIfNeeded hostView = " + hostView + ", appWidgetInfo = "
                    + appWidgetInfo);
        }

        if (appWidgetInfo == null || appWidgetInfo.autoAdvanceViewId == -1) {
            return;
        }
        View v = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
        if (v instanceof Advanceable) {
            mWidgetsToAdvance.put(hostView, appWidgetInfo);
            ((Advanceable) v).fyiWillBeAdvancedByHostKThx();
            updateRunning();
        }
    }
	
	protected void removeWidgetToAutoAdvance(View hostView) {
        if (Util.ENABLE_DEBUG) {
			Util.Log.d(TAG, "removeWidgetToAutoAdvance hostView = " + hostView);
        }

        if (mWidgetsToAdvance.containsKey(hostView)) {
            mWidgetsToAdvance.remove(hostView);
            updateRunning();
        }
    }

	public void removeAppWidget(LauncherAppWidgetInfo launcherInfo){
		if (Util.ENABLE_DEBUG) {
			Util.Log.d(TAG, "removeAppWidget launcherInfo = " + launcherInfo);
        }

        removeWidgetToAutoAdvance(launcherInfo.hostView);
        launcherInfo.hostView = null;
	}

	protected void sendAdvanceMessage(long delay) {
        mHandler.removeMessages(ADVANCE_MSG);
        Message msg = mHandler.obtainMessage(ADVANCE_MSG);
        mHandler.sendMessageDelayed(msg, delay);
        mAutoAdvanceSentTime = System.currentTimeMillis();
    }

	protected void updateRunning() {
        boolean autoAdvanceRunning = mVisible && mUserPresent && !mWidgetsToAdvance.isEmpty();
        if (autoAdvanceRunning != mAutoAdvanceRunning) {
            mAutoAdvanceRunning = autoAdvanceRunning;
            if (autoAdvanceRunning) {
                long delay = mAutoAdvanceTimeLeft == -1 ? mAdvanceInterval : mAutoAdvanceTimeLeft;
                sendAdvanceMessage(delay);
            } else {
                if (!mWidgetsToAdvance.isEmpty()) {
                    mAutoAdvanceTimeLeft = Math.max(0, mAdvanceInterval -
                            (System.currentTimeMillis() - mAutoAdvanceSentTime));
                }
                mHandler.removeMessages(ADVANCE_MSG);
                mHandler.removeMessages(0); // Remove messages sent using postDelayed()
            }
        }
    }
    
    protected final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ADVANCE_MSG) {
                int i = 0;
                for (View key: mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(mWidgetsToAdvance.get(key).autoAdvanceViewId);
                    final int delay = mAdvanceStagger * i;
                    if (v instanceof Advanceable) {
                       postDelayed(new Runnable() {
                           public void run() {
                               ((Advanceable) v).advance();
                           }
                       }, delay);
                    }
                    i++;
                }
                sendAdvanceMessage(mAdvanceInterval);
            }
        }
    };
    
    
    
    protected ContentObserver mWidgetObserver;// = new AppWidgetResetObserver();
    protected void registerContentObservers() {
        ContentResolver resolver = getContentResolver();
        if(mWidgetObserver == null){
        	mWidgetObserver = new AppWidgetResetObserver();
        } else {
        	getContentResolver().unregisterContentObserver(mWidgetObserver);
        }
        resolver.registerContentObserver(LauncherSettings.CONTENT_APPWIDGET_RESET_URI,
                true, mWidgetObserver);
    }
    protected void unRegisterContentObservers() {
    	if(mWidgetObserver != null){
			getContentResolver().unregisterContentObserver(mWidgetObserver);
			mWidgetObserver = null;
		}
    }
    protected void onAppWidgetReset() {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onAppWidgetReset.");
        }

        if (mAppWidgetHost != null) {
            mAppWidgetHost.startListening();
        }
    }
    /**
     * Receives notifications whenever the appwidgets are reset.
     */
    private class AppWidgetResetObserver extends ContentObserver {
        public AppWidgetResetObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            onAppWidgetReset();
        }
    }
    
	public boolean setLoadOnResume(){
		if (mPaused) {
            Util.Log.i(TAG, "setLoadOnResume: this = " + this);
            mOnResumeNeedsLoad = true;
            return true;
        } else {
            return false;
        }
	}
    public int getCurrentWorkspaceScreen(){
    	if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        }
           
    	return getSharedPrefSettingsManager().getWorkspaceDefaultScreen();
        
    }
    public void startBinding(){
    	/// M: Cancel Drag when reload to avoid dragview lost parent and JE @{
        if (mDragController != null) {
            mDragController.cancelDrag();
        }
        /// M: }@
    }
    
    public void bindItems(List<ItemInfo> shortcuts, int start, int end){
    	
    }
    
    public void bindFolders(HashMap<Long,FolderInfo> folders){
    	setLoadOnResume();
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "bindFolders: this = " + this);
        }
        sFolders.clear();
        sFolders.putAll(folders);
    }
    
    public void finishBindingItems(){
    	setLoadOnResume();
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "finishBindingItems: mSavedState = " + mSavedState  + ", this = " + this);
        }

        if (mSavedState != null) {
            if (!mWorkspace.hasFocus()) {
                mWorkspace.getChildAt(mWorkspace.getCurrentPage()).requestFocus();
            }
            mSavedState = null;
        }
        
        mWorkspace.restoreInstanceStateForRemainingPages();
    }
    
    public void bindAppWidget(AppWidgetInfo binditem){
    	setLoadOnResume();

        LauncherAppWidgetInfo item = (LauncherAppWidgetInfo)binditem;
        final long start = Util.DEBUG_WIDGETS ? SystemClock.uptimeMillis() : 0;
        if (Util.DEBUG_WIDGETS) {
            Util.Log.i(TAG, "bindAppWidget: " + item+"==hostView:"+item.hostView);
        }
        if(item.hostView != null)
            return;
        
        final Workspace workspace = mWorkspace;

        final int appWidgetId = item.appWidgetId;
        final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (Util.DEBUG_WIDGETS) {
        	Util.Log.d(TAG, "bindAppWidget: id=" + item.appWidgetId + " belongs to component "
                    + (appWidgetInfo == null ? "" : appWidgetInfo.provider));
        }
        
        /// M: If appWidgetInfo is null, appWidgetHostView will be error view,
        /// don't add in homescreen.
        if (appWidgetInfo == null) {
            return;
        }

        item.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);

        item.hostView.setTag(item);
        item.onBindAppWidget(this);
//        android.util.Log.i("QsLog", "bindAppWidget=="
//                +"==appWidgetInfo.minWidth:"+appWidgetInfo.minWidth
//                +"==appWidgetInfo.minHeight:"+appWidgetInfo.minHeight
//                +"==item.minWidth:"+item.minWidth
//                +"==item.minHeight:"+item.minHeight);
        
        item.minWidth = appWidgetInfo.minWidth;
        item.minHeight = appWidgetInfo.minHeight;

        /// M: Call the appropriate callback for the IMTKWidget will be bound to workspace.
        mWorkspace.setAppWidgetIdAndScreen(item.hostView, mWorkspace.getCurrentPage(), appWidgetId);

        workspace.addInScreen(item.hostView, item.container, item.screen, item.cellX,
                item.cellY, item.spanX, item.spanY, false);
        addWidgetToAutoAdvanceIfNeeded(item.hostView, appWidgetInfo);

        workspace.requestLayout();

        if (Util.DEBUG_WIDGETS) {
        	Util.Log.d(TAG, "bound widget id=" + item.appWidgetId + " in " + (SystemClock.uptimeMillis() - start) + "ms");
        }
    }
    
    public void bindAllApplications(List<ApplicationInfo> apps){
    	
    }
    
    public void bindAppsAdded(List<ApplicationInfo> apps){
    	setLoadOnResume();
    	
    	if (mWorkspace != null) {
            mWorkspace.updateShortcuts(apps);
        }
    }
    
    public void bindAppsUpdated(List<ApplicationInfo> apps){
        setLoadOnResume();
        if (mWorkspace != null) {
            mWorkspace.updateShortcuts(apps);
        }
    }
    
    public void bindAppsRemoved(List<String> packageNames, boolean permanent){
        if (permanent) {
            mWorkspace.removeItems(packageNames);
        }
        
        mDragController.onAppsRemoved(packageNames, this);
    }
    
    public void bindPackagesUpdated(){
    	
    }
    
    public boolean isAllAppsVisible(){
    	return ((mState&State.APPS_CUSTOMIZE) > 0) || ((mOnResumeState&State.APPS_CUSTOMIZE)>0);
    }
    
    public boolean isAllAppsButtonRank(int rank){
    	if(mHotseat != null)
    		return mHotseat.isAllAppsButtonRank(rank);
    	
    	if(rank >= 0 && rank == getSharedPrefSettingsManager().getAllAppsButtonRank())
    		return true;
    	return false;
    }
    
    public void bindSearchablesChanged(){
    	
    }
    
    public void onPageBoundSynchronously(int page){
    	mSynchronouslyBoundPages.add(page);
    }
    /// M: added the new callback fun for the scene feature.
//    public void switchScene(){
//    	
//    }
    /// M: added the new callback fun for remove appWidget.
    public void bindAppWidgetRemoved(List<String> appWidget, boolean permanent){
    	
    }

    /// M: set flag to re-sync apps pages if orientation changed.
    public void notifyOrientationChanged(){
    	
    }
    
    //public abstract boolean isFolderClingVisible();
	
	
	public Rect getCurrentBounds(){
		return null;
	}
	
	public void notifyPagesWereRecreated() {
		
	}
	
	public void showAppIconAndTitleCustomActivity(){
		getLauncherApplication().showAppIconAndTitleCustomActivity();
	}
	
	private static AsyncTask<Void, Integer, Void> mExportAppTask = null;
	private void jzsExportAppsAndWorkspace(){
	    if(mExportAppTask != null)
	        return;
	    
	    mExportAppTask = new AsyncTask<Void, Integer, Void>(){

            @Override
            protected Void doInBackground(Void... arg0) {
                getIconCache().dumpCacheEntry();
                JzTestSaveAllWidgets();
                return null;
            }
            
            @Override
            protected void onPreExecute() {

            }

            @Override
            protected void onProgressUpdate(Integer... values) {

            }
            
            @Override
            protected void onPostExecute(Void result) {
                mExportAppTask = null;
            }
        };
        mExportAppTask.execute();
    }
	
	private String JzGetItemInfoString(ItemInfo info, boolean bIsShowSpan) {
        String strWrite = "launcher:screen=\"" + info.screen + "\" \r\n"
                + "launcher:x=\"" + info.cellX + "\" \r\n" + "launcher:y=\""
                + info.cellY + "\" \r\n";

        if (bIsShowSpan) {
            strWrite += "launcher:spanX=\"" + info.spanX + "\" \r\n"
                    + "launcher:spanY=\"" + info.spanY + "\" \r\n";
        }
        return strWrite;
    }

    private String JzGetChildViewWriteString(View child) {
        String strWrite = "";
        Object tag = child.getTag();

        //if (child instanceof LauncherAppWidgetHostView) {
        if(tag instanceof LauncherAppWidgetInfo){

            LauncherAppWidgetInfo launcherInfo = (LauncherAppWidgetInfo) tag;
            // strWrite =
            // "customwidget==screen:"+launcherInfo.screen+",x:"+launcherInfo.cellX+",y:"+launcherInfo.cellY+
            // ",spanX:"+launcherInfo.spanX+",spanY:"+launcherInfo.spanY+",appWidgetId:"+launcherInfo.appWidgetId;
            // strWrite = "customwidget==" + JzGetItemInfoString(launcherInfo) +
            // ",appWidgetId:"+launcherInfo.appWidgetId;

            AppWidgetProviderInfo info = ((LauncherAppWidgetHostView) child)
                    .getAppWidgetInfo();
            if (info != null) {
                strWrite = "<appwidget \r\n " + "launcher:packageName=\""
                        + info.provider.getPackageName() + "\" \r\n"
                        + "launcher:className=\""
                        + info.provider.getClassName() + "\" \r\n";
                // strWrite += ",compantname:"+info.provider.toString();
            } else {
                // strWrite += ",compantname is unknow";
                strWrite = "<!--  compantname is unknow \r\n"
                        + JzGetItemInfoString(launcherInfo, true) + " -->";
                return strWrite;
            }

            strWrite += JzGetItemInfoString(launcherInfo, true) + "/>\r\n";
        } else if (child instanceof Folder) {
            Folder f = (Folder) child;

            FolderInfo folderInfo = (FolderInfo) tag;// f.getInfo();
            strWrite = "<!--  Folder=="
                    + JzGetItemInfoString(folderInfo, false) + ", title:"
                    + folderInfo.title + " -->";
        } else if (tag instanceof ApplicationInfo) {
            ApplicationInfo info = (ApplicationInfo) tag;
            Intent intent = info.intent;
            ComponentName name = intent.getComponent();

            //if (name == null) {
            if(info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT){
                strWrite = "<shortcut \r\n" + "launcher:title=\"" + info.title
                        + "\" \r\n" + "launcher:uri=\"" + intent.toUri(0)
                        + "\" \r\n";

                // strWrite = "shortcut==" + JzGetItemInfoString(info, false) +
                // ", title:"+info.title + ", uri:"+intent.toUri(0);
            } else {
                strWrite = "<favorite \r\n" + "launcher:packageName=\""
                        + name.getPackageName() + "\" \r\n"
                        + "launcher:className=\"" + name.getClassName()
                        + "\" \r\n";
                // strWrite = "favorite==" + JzGetItemInfoString(info, false) +
                // ", title:"+info.title + ", component:"+name;
            }

            strWrite += JzGetItemInfoString(info, false) + "/>\r\n";
        } else if(tag instanceof ShortcutInfo){
            
            ShortcutInfo info = (ShortcutInfo) tag;
            Intent intent = info.intent;
            ComponentName name = intent.getComponent();
            
            if(info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION){
                strWrite = "<favorite \r\n" + "launcher:packageName=\""
                    + name.getPackageName() + "\" \r\n"
                    + "launcher:className=\"" + name.getClassName()
                    + "\" \r\n";
            }else{
                strWrite = "<shortcut \r\n" + "launcher:title=\"" + info.title
                    + "\" \r\n" + "launcher:uri=\"" + intent.toUri(0)
                    + "\" \r\n";
            }
            strWrite += JzGetItemInfoString(info, false) + "/>\r\n";
            
            //Log.d("QsHtcLauncher",
            //      "Launcher::JzGetChildViewWriteString()==ShortcutInfo item type===");
        }else {
        
            android.util.Log.d("QsLog",
                    "Launcher::JzGetChildViewWriteString()==unknow item type===");
        }

        return strWrite;
    }

    private void JzTestSaveAllWidgets() {
        //Log.d("QsHtcLauncher", "Launcher::JzTestSaveAllWidgets()==start===");
        // jz for get user launcher configuare

        try {

            //long now = System.currentTimeMillis();
            //Time curTime = new Time();
            //curTime.set(now);

            final String status = Environment.getExternalStorageState();
            if(!status.equals(Environment.MEDIA_MOUNTED) || status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)){
                android.util.Log.d("QsLog", "JzTestSaveAllWidgets()===no sdcard====");
                return;
            }
            String strWrite = "";//new StringBuilder();
            
            List<ApplicationInfo> list = getModel().getAllAppsList();
            if(list != null && list.size() > 0){
                
                File outputAppsFile = new File(Environment.getExternalStorageDirectory(), "qs_apps_info.xml");
                if(outputAppsFile.exists())
                    outputAppsFile.delete();
                
                FileOutputStream qsAppsfout = new FileOutputStream(outputAppsFile);
                if(qsAppsfout != null){
                    
                    for(ApplicationInfo info : list){
                        strWrite = info.toString() + "\r\n";
                        qsAppsfout.write(strWrite.getBytes());
                    }
                    
                    qsAppsfout.close();
                }
            }

            String strFileNameString = "qs_default_workspace.xml";
            File outputfile = new File(Environment.getExternalStorageDirectory(), strFileNameString);
            if(outputfile.exists())
            {
                outputfile.delete();
            }
            
            FileOutputStream qsfout = new FileOutputStream(outputfile);
            //this.openFileOutput(strFileNameString, Context.MODE_WORLD_WRITEABLE); // new
                                                    // FileOutputStream(qsfile);
            
            android.util.Log.d("QsLog", "JzsLauncher::JzTestSaveAllWidgets()=======");
            if (qsfout != null) {
                // strWrite =
                // "============start======"+curTime.toString()+"==================\r\n";
                strWrite = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
                        +"<favorites xmlns:launcher=\"http://schemas.android.com/apk/res/com.android.qshome\">\r\n";
                qsfout.write(strWrite.getBytes());
            
                if(mWorkspace != null){
                    

                    int nCellLayoutCount = mWorkspace.getPageCount();
                    for (int i = 0; i < nCellLayoutCount; i++) {
                        ShortcutAndWidgetContainer  screen = ((CellLayout)mWorkspace.getPageAt(i)).getShortcutsAndWidgets();
                        int count = screen.getChildCount();
    
                        for (int j = 0; j < count; j++) {
                            View child = screen.getChildAt(j);
                            if (child == null) {
                                android.util.Log.d("JzsLauncher",
                                        "JzsLauncher::JzTestSaveAllWidgets()==child:"
                                                + j + " is null===");
                                continue;
                            }
                            strWrite = JzGetChildViewWriteString(child);
    
                            if (strWrite != "") {
                                strWrite += "\r\n";
                                qsfout.write(strWrite.getBytes());
                            }
                        }
                    }  
                }
                
                if(getHotseat() != null){
                    ShortcutAndWidgetContainer  screen = getHotseat().getLayout().getShortcutsAndWidgets();
                    int count = screen.getChildCount();
                    
                    for (int j = 0; j < count; j++) {
                        View child = screen.getChildAt(j);
                        if (child == null) {
                            android.util.Log.d("JzsLauncher",
                                    "JzsLauncher::JzTestSaveAllWidgets()==child:"
                                            + j + " is null===");
                            continue;
                        }
                        strWrite = JzGetChildViewWriteString(child);

                        if (strWrite != "") {
                            strWrite += "\r\n";
                            qsfout.write(strWrite.getBytes());
                        }
                    }
                }
                
                strWrite = "</favorites>\r\n";
                qsfout.write(strWrite.getBytes());
                
                qsfout.close();

            } else {
                android.util.Log.d("QsLog",
                        "Launcher::JzTestSaveAllWidgets()==qsfout is null===");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block

            android.util.Log.d("QsLog",
                    "Launcher::JzTestSaveAllWidgets()==open file error:"
                            + e.getMessage() + "==");
            //e.printStackTrace();
        }
        // //////////////////////////////////////////////////////////////////////////////
    }
	
	protected static final String PREFERENCES = "launcher.preferences";
	protected static LocaleConfiguration sLocaleConfiguration = null;
	protected static boolean sLocaleChanged = false;
	protected void checkForLocaleChange() {
        if (sLocaleConfiguration == null) {
            new AsyncTask<Void, Void, LocaleConfiguration>() {
                @Override
                protected LocaleConfiguration doInBackground(Void... unused) {
                    LocaleConfiguration localeConfiguration = new LocaleConfiguration();
                    readConfiguration(Launcher.this, localeConfiguration);
                    return localeConfiguration;
                }

                @Override
                protected void onPostExecute(LocaleConfiguration result) {
                    sLocaleConfiguration = result;
                    checkForLocaleChange();  // recursive, but now with a locale configuration
                }
            }.execute();
            return;
        }

        final Configuration configuration = getResources().getConfiguration();

        final String previousLocale = sLocaleConfiguration.locale;
        final String locale = configuration.locale.toString();

        final int previousMcc = sLocaleConfiguration.mcc;
        final int mcc = configuration.mcc;

        final int previousMnc = sLocaleConfiguration.mnc;
        final int mnc = configuration.mnc;

        boolean localeChanged = !locale.equals(previousLocale) || mcc != previousMcc || mnc != previousMnc;

        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "checkForLocaleChange: previousLocale = " + previousLocale
                    + ", locale = " + locale + ", previousMcc = " + previousMcc + ", mcc = " + mcc
                    + ", previousMnc = " + previousMnc + ", mnc = " + mnc + ", localeChanged = "
                    + localeChanged + ", this = " + this);
        }

        if (localeChanged) {
            sLocaleConfiguration.locale = locale;
            sLocaleConfiguration.mcc = mcc;
            sLocaleConfiguration.mnc = mnc;

            /// M: When locale changed, reset collator and flush caches.
            sLocaleChanged = localeChanged;            
            getResConfigManager().onTrimMemory(0);
            getSharedPrefSettingsManager().onTrimMemory(0);
            getModel().setFlushCache();
            getIconCache().flush();

            final LocaleConfiguration localeConfiguration = sLocaleConfiguration;
            new Thread("WriteLocaleConfiguration") {
                @Override
                public void run() {
                    writeConfiguration(Launcher.this, localeConfiguration);
                }
            }.start();
        }
    }

	protected static class LocaleConfiguration {
        public String locale;
        public int mcc = -1;
        public int mnc = -1;
    }

	protected static void readConfiguration(Context context, LocaleConfiguration configuration) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(context.openFileInput(PREFERENCES));
            configuration.locale = in.readUTF();
            configuration.mcc = in.readInt();
            configuration.mnc = in.readInt();
        } catch (FileNotFoundException e) {
        	Util.Log.d(TAG, "FileNotFoundException when read configuration.");
        } catch (IOException e) {
        	Util.Log.d(TAG, "IOException when read configuration.");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                	Util.Log.d(TAG, "IOException when close file.");
                }
            }
        }
    }

	protected static void writeConfiguration(Context context, LocaleConfiguration configuration) {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(context.openFileOutput(PREFERENCES, Context.MODE_PRIVATE));
            out.writeUTF(configuration.locale);
            out.writeInt(configuration.mcc);
            out.writeInt(configuration.mnc);
            out.flush();
        } catch (FileNotFoundException e) {
        	Util.Log.d(TAG, "FileNotFoundException when write configuration.");
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            context.getFileStreamPath(PREFERENCES).delete();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                	Util.Log.d(TAG, "IOException when close file.");
                }
            }
        }
    }
	
	private static int mIsSupportGesture = -1;
	private IGestureManager mGestureMgr;// = (IGestureManager)mContext.getSystemService(IGestureManager.MANAGER_SERVICE);
	
	private boolean checkSupportGesture(){
        if(mIsSupportGesture < 0){
        	mIsSupportGesture = 0;
            if(!getResConfigManager().getBoolean(ResConfigManager.CONFIG_SUPPORT_GESTURE, false)){
                return false;
            }
            
            if(Build.getFrameworkVersion() > 1){
	            mGestureMgr = (IGestureManager)getSystemService(IGestureManager.MANAGER_SERVICE);
	            if(mGestureMgr != null && mGestureMgr.getSupportProximityGesture() > IGestureManager.PS_GESTURE_TYPE_NONE){
	            	mIsSupportGesture = 1;
	            }
            } else {
	            final android.content.pm.PackageManager packageManager = super.getPackageManager();
	            try {
	                packageManager.getPackageInfo("com.jzs.gesture.settings", 0);
	                mIsSupportGesture = 1;
	            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
	                mIsSupportGesture = 0;
	            }
            }
        }
        return mIsSupportGesture > 0;
    }

	protected void enableGesture(boolean enable, boolean checkKeyguard){
//		if(Util.ENABLE_DEBUG){
//		    Util.Log.i(TAG, "enableGesture()==enable:"+enable
//		            +"==checkKeyguard:"+checkKeyguard
//		            +"==isKeyguardLocked:"+isKeyguardLocked()
//		            +"==mIsSupportGesture:"+mIsSupportGesture);
//		}
//	    android.util.Log.i("Jzs.QsLog", "enableGesture()==enable:"+enable
//                +"==checkKeyguard:"+checkKeyguard
//                +"==isKeyguardLocked:"+isKeyguardLocked()
//                +"==mIsSupportGesture:"+mIsSupportGesture);
        if(!isSupportGesture() || (checkKeyguard && isKeyguardLocked())){
        	if(Util.ENABLE_DEBUG){
    		    Util.Log.i(TAG, "enableGesture()==enable:"+enable
    		            +"==checkKeyguard:"+checkKeyguard
    		            +"==isKeyguardLocked:"+isKeyguardLocked()
    		            +"==mIsSupportGesture:"+mIsSupportGesture);
    		}
            return;
        }
        
        if(Build.getFrameworkVersion() > 1){
	        if(mGestureMgr != null){
	        	mGestureMgr.setProximityGesturePowerStatus(enable);
	        } 
	        return;
        }
        final Intent intent = new Intent(QsIntent.ACTION_JZS_GESTURE_SETTINGS);
        intent.putExtra(QsIntent.EXTRA_JZS_GESTURE_ENABLE, enable ? 1 : 0);
        sendBroadcast(intent);
    }
	
	protected boolean isSupportGesture(){
	    return mIsSupportGesture > 0;
	}

    private android.app.KeyguardManager mKeyguardManager;
    protected boolean isKeyguardLocked() {
        if (mKeyguardManager == null) {
            mKeyguardManager = (android.app.KeyguardManager) getSystemService(android.content.Context.KEYGUARD_SERVICE);
        }
        // isKeyguardSecure excludes the slide lock case.
        boolean locked = (mKeyguardManager != null) && (mKeyguardManager.isKeyguardLocked() 
        							|| mKeyguardManager.inKeyguardRestrictedInputMode());

        return locked;
    }
	public abstract IAppsCustomizePanel getAppsCustomizePanel();
	//public abstract View getAllAppsRootView();
	
	public static class PendingAddArguments {
		public int requestCode;
		public Intent intent;
		public long container;
		public int screen;
		public int cellX;
		public int cellY;
    }

	void qsAddFolderByMenu(){
		//mWorkspace.getCurrentDropLayout();
		int screen = mWorkspace.getCurrentPage();
		CellLayout layout = (CellLayout)mWorkspace.getPageAt(screen);
		int cellXY[] = new int[2];
		if(!qsGetEmptyCellPosition(layout, cellXY)){
			showOutOfSpaceMessage(isHotseatLayout(layout));
			return;
		}
	    long container = isHotseatLayout(layout) 
			? LauncherSettings.Favorites.CONTAINER_HOTSEAT 
			: LauncherSettings.Favorites.CONTAINER_DESKTOP;//
		FolderIcon folder = addFolder(layout, container, screen, cellXY[0], cellXY[1]);
		AnimatorSet animeSet = new AnimatorSet();
		animeSet.setDuration(300);
		ValueAnimator animator1 = ObjectAnimator.ofFloat(folder, "alpha", 0.0f, 1.0f);
		ValueAnimator animator2 = ObjectAnimator.ofFloat(folder, "scaleX", 0.0f, 1.0f);
		ValueAnimator animator3 = ObjectAnimator.ofFloat(folder, "scaleY", 0.0f, 1.0f);
		animeSet.playTogether(animator1, animator2, animator3);
		animeSet.start();
	}
	
	private boolean qsGetEmptyCellPosition(CellLayout layout, int cellXY[]){
	    if(layout == null) return false;
	    
		int countX = layout.getCountX();
		int countY = layout.getCountY();
		for(int i=0; i< countY; i++){
			for(int j=0; j<countX; j++){
				if(layout.isOccupied(j, i))
					continue;
				else{
					cellXY[0] = j;
					cellXY[1] = i;
					return true;
				}
			}
		}
		return false;
	}
}
