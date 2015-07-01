package com.jzs.dr.mtplauncher.defaults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.app.SearchManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.PorterDuff;

import com.jzs.common.content.QsIntent;
import com.jzs.common.launcher.IGlobalStaticFunc;
import com.jzs.common.launcher.ILauncherApplication;
import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.common.launcher.model.AppWidgetInfo;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.FolderInfo;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.defaults.widget.AppsCustomizePagedViewDefault;
import com.jzs.dr.mtplauncher.defaults.widget.AppsCustomizeTabHost;
import com.jzs.dr.mtplauncher.defaults.widget.WorkspaceDefault;
import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.LauncherApplication;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragController;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget.DragObject;
import com.jzs.dr.mtplauncher.sjar.ctrl.HolographicImageView;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherAnimUtils;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherTransitionable;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherViewPropertyAnimator;
import com.jzs.dr.mtplauncher.sjar.model.LauncherAppWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherModel;
import com.jzs.dr.mtplauncher.sjar.model.LauncherSettings;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.widget.AppsCustomizePagedView;
import com.jzs.dr.mtplauncher.sjar.widget.BubbleTextView;
import com.jzs.dr.mtplauncher.sjar.widget.CellLayout;
import com.jzs.dr.mtplauncher.sjar.widget.DragLayer;
import com.jzs.dr.mtplauncher.sjar.widget.DragView;
import com.jzs.dr.mtplauncher.sjar.widget.Folder;
import com.jzs.dr.mtplauncher.sjar.widget.FolderIcon;
import com.jzs.dr.mtplauncher.sjar.widget.HolographicLinearLayout;
import com.jzs.dr.mtplauncher.sjar.widget.IAppsCustomizePanel;
import com.jzs.dr.mtplauncher.sjar.widget.MtpShortcutView;
import com.jzs.dr.mtplauncher.sjar.widget.SearchDropTargetBar;
import com.jzs.dr.mtplauncher.sjar.widget.Workspace;
import com.jzs.dr.mtplauncher.sjar.widget.SmoothPagedView;
//import com.jzs.dr.mtplauncher.widget.Workspace;
//import com.jzs.dr.mtplauncher.sjar.exthelpers.LauncherHelper;
import com.android.common.Search;
import com.jzs.dr.mtplauncher.sjar.widget.PageViewIndicator;
import com.jzs.dr.mtplauncher.defaults.widget.TopFloatBar;

import com.jzs.dr.mtplauncher.CustomCropImageActivity;
import com.jzs.dr.mtplauncher.R;
import com.qs.utils.ConfigOption;

public class LauncherDefault extends Launcher {
    private final static String TAG = "JzsLauncherDefault";
	private static final int MENU_GROUP_WALLPAPER = 1;
//    private static final int MENU_WALLPAPER_SETTINGS = Menu.FIRST + 1;
//    private static final int MENU_MANAGE_APPS = MENU_WALLPAPER_SETTINGS + 1;
//    private static final int MENU_SYSTEM_SETTINGS = MENU_MANAGE_APPS + 1;
//    private static final int MENU_HELP = MENU_SYSTEM_SETTINGS + 1;
//    private static final int MENU_CUSTOM_ICON = MENU_HELP + 1;
    
    static final boolean DEBUG_WIDGETS = true;

    
    private static final String TOOLBAR_ICON_METADATA_NAME = "com.android.launcher.toolbar_icon";
    private static final String TOOLBAR_SEARCH_ICON_METADATA_NAME =
            "com.android.launcher.toolbar_search_icon";
    private static final String TOOLBAR_VOICE_SEARCH_ICON_METADATA_NAME =
            "com.android.launcher.toolbar_voice_search_icon";
    private static final String MTK_APP_TODO_PACKAGE_NAME = "com.mediatek.todos";
    
    private AnimatorSet mStateAnimation;
    private AnimatorSet mDividerAnimator;
    
 // We set the state in both onCreate and then onNewIntent in some cases, which causes both
    // scroll issues (because the workspace may not have been measured yet) and extra work.
    // Instead, just save the state that we need to restore Launcher to, and commit it in onResume.
    //private State mOnResumeState = State.NONE;
    
    private BubbleTextView mWaitingForResume;
    
    
 // How long to wait before the new-shortcut animation automatically pans the workspace
    private static int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 10;
    
	private View mQsbDivider;
    private View mDockDivider;
    private View mLauncherView;
    
    //private ItemInfo mPendingAddInfo = new ItemInfo();
    //private AppWidgetProviderInfo mPendingAddWidgetInfo;

    

    
    
    private View mAllAppsButton;
    
    
    private AppsCustomizeTabHost mAppsCustomizeTabHost;
    private AppsCustomizePagedView mAppsCustomizeContent;
    //private AppsCustomizePagedView mAppsCustomizeContentWidget;
    private boolean mAutoAdvanceRunning = false;
    
    private Drawable mWorkspaceBackgroundDrawable;
    private Drawable mBlackBackgroundDrawable;
    
    //private Workspace mWorkspace;

    /// M: Used to force reload when loading workspace
    
    /// M: Save current CellLayout bounds before workspace.changeState(CellLayout will be scaled).
    private Rect mCurrentBounds = new Rect();

  /// M: flag to indicate whether the orientation has changed.
    private boolean mOrientationChanged;
  /// M: flag to indicate whether the pages in app customized pane were recreated.
    private boolean mPagesWereRecreated;
    
    //private SpannableStringBuilder mDefaultKeySsb = null;
    private int mNewShortcutAnimatePage = -1;
    private ArrayList<View> mNewShortcutAnimateViews = new ArrayList<View>();
    
    
    
    
 // External icons saved in case of resource changes, orientation, etc.
    private static Drawable.ConstantState[] sGlobalSearchIcon = new Drawable.ConstantState[2];
    private static Drawable.ConstantState[] sVoiceSearchIcon = new Drawable.ConstantState[2];
    private static Drawable.ConstantState[] sAppMarketIcon = new Drawable.ConstantState[2];
    
    
    private boolean mUnreadLoadCompleted = false;
    private boolean mBindingWorkspaceFinished = false;
    private boolean mBindingAppsFinished = false;
    
    
    private static ArrayList<PendingAddArguments> sPendingAddList
    					= new ArrayList<PendingAddArguments>();

	
    public LauncherDefault(Context localContext){
		this(localContext, null);
	}
    
    public LauncherDefault(Context localContext, ILauncherApplication mainApp){
    	super(localContext, mainApp);
    	
    }

	protected IResConfigManager createResConfigManager(ISharedPrefSettingsManager pref, IResConfigManager base){
		base.attachBaseContext(this);
		return base;
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.launcher);

		setupViews();

		registerContentObservers();

		//mSavedState = savedInstanceState;
		restoreState(mSavedState);

		// Update customization drawer _after_ restoring the states
		if (mAppsCustomizeContent != null) {
			mAppsCustomizeContent.onPackagesUpdated();
		}

		initApplicationsLoader();

        if (!getModel().isAllAppsLoaded()) {
            ViewGroup appsCustomizeContentParent = (ViewGroup) mAppsCustomizeContent.getParent();
            mInflater.inflate(R.layout.apps_customize_progressbar, appsCustomizeContentParent);
        }

        initDefaultKeySsb();

        //registerCloseSystemDialogsIntentReceiver();

        //updateGlobalIcons();

        // On large interfaces, we want the screen to auto-rotate based on the current orientation
        unlockScreenOrientation(true);
        
        // M: Register orientation listener.
        //registerOrientationListener();
	}
	
	public IAppsCustomizePanel getAppsCustomizePanel(){
		return mAppsCustomizeTabHost;
	}
	
//	public View getAllAppsRootView(){
//		return mAppsCustomizeTabHost;
//	}

    @Override
    public boolean onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_BIND_APPWIDGET) {
            int appWidgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_CANCELED) {
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);
            } else if (resultCode == RESULT_OK) {
                addAppWidgetImpl(appWidgetId, mPendingAddInfo, null, mPendingAddWidgetInfo);
            }
            return true;
        }
        boolean delayExitSpringLoadedMode = false;
        boolean isWidgetDrop = (requestCode == REQUEST_PICK_APPWIDGET ||
                requestCode == REQUEST_CREATE_APPWIDGET);
        mWaitingForResult = false;

        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onActivityResult: requestCode = " + requestCode 
                    + ", resultCode = " + resultCode + ", data = " + data 
                    + ", mPendingAddInfo = " + mPendingAddInfo);
        }

        // We have special handling for widgets
        if (isWidgetDrop) {
            int appWidgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (appWidgetId < 0) {
            	Util.Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not returned from the \\" +
                        "widget configuration activity.");
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);
            } else {
                completeTwoStageWidgetDrop(resultCode, appWidgetId);
            }
            return true;
        }

        // The pattern used here is that a user PICKs a specific application,
        // which, depending on the target, might need to CREATE the actual target.

        // For example, the user would PICK_SHORTCUT for "Music playlist", and we
        // launch over to the Music app to actually CREATE_SHORTCUT.
        if (resultCode == RESULT_OK && mPendingAddInfo.container != ItemInfo.NO_ID) {
            final PendingAddArguments args = new PendingAddArguments();
            args.requestCode = requestCode;
            args.intent = data;
            args.container = mPendingAddInfo.container;
            args.screen = mPendingAddInfo.screen;
            args.cellX = mPendingAddInfo.cellX;
            args.cellY = mPendingAddInfo.cellY;
            if (isWorkspaceLocked()) {
                sPendingAddList.add(args);
            } else {
                delayExitSpringLoadedMode = completeAdd(args);
            }
        }
        mDragLayer.clearAnimatedView();
        // Exit spring loaded mode if necessary after cancelling the configuration of a widget
        exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), delayExitSpringLoadedMode,
                null);
        
        return true;
    }
    
    @Override 
    public void onStart() {
        super.onStart();
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "(Launcher)onStart: this = " + this);
        }
        // Launch performance debug log.
        //getWindow().getDecorView().getViewTreeObserver().addOnPostDrawListener(mPostDrawListener);

        if (isAllAppsVisible() && mAppsCustomizeTabHost != null && mAppsCustomizeTabHost.getVisibility() == View.VISIBLE
                && mAppsCustomizeTabHost.getContentVisibility() == View.GONE) {
            mAppsCustomizeTabHost.setContentVisibility(View.VISIBLE);
        }
    }

    @Override 
    public void onStop() {
        super.onStop();
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "(Launcher)onStop: this = " + this);
        }

        // Launch performance debug log.
        //getWindow().getDecorView().getViewTreeObserver().removeOnPostDrawListener(mPostDrawListener);
    }
    
	@Override
    public void onResume() {
		if (mOrientationChanged && mPagesWereRecreated) {
            Util.Log.d(TAG, "(Launcher)onResume: mOrientationChanged && mPagesWereRecreated");
            mAppsCustomizeContent.invalidateAppPages(mAppsCustomizeContent.getCurrentPage(), true);
            //mAppsCustomizeContentWidget.invalidateAppPages(mAppsCustomizeContent.getCurrentPage(), true);
        }
        resetReSyncFlags();
        
        super.onResume();
               
        // Background was set to gradient in onPause(), restore to black if in all apps.
        setWorkspaceBackground(mState == State.WORKSPACE);


        // Reset the pressed state of icons that were locked in the press state while activities
        // were launching
        if (mWaitingForResume != null) {
            // Resets the previous workspace icon press state
            mWaitingForResume.setStayPressed(false);
        }
        if (mAppsCustomizeContent != null) {
            // Resets the previous all apps icon press state
            mAppsCustomizeContent.resetDrawableState();
        }
//        if (mAppsCustomizeContentWidget != null) {
//        	mAppsCustomizeContentWidget.resetDrawableState();
//        }
        // It is possible that widgets can receive updates while launcher is not in the foreground.
        // Consequently, the widgets will be inflated in the orientation of the foreground activity
        // (framework issue). On resuming, we ensure that any widgets are inflated for the current
        // orientation.
        mWorkspace.reinflateWidgetsIfNecessary();

        // Again, as with the above scenario, it's possible that one or more of the global icons
        // were updated in the wrong orientation.
        //updateGlobalIcons();

        /// M: Enable orientation listener when we resume Launcher.
        enableOrientationListener();
        if((mState & State.APPS_CUSTOMIZE) ==  State.APPS_CUSTOMIZE){
        	 if(mWorkspaceContainer != null){
        		 mWorkspaceContainer.setVisibility(View.INVISIBLE);
        	 }
        }

        int appContentType = mAppsCustomizeContent.getCurrentContentType();
        if(mAppsCustomizeTabHost != null) {
            String tag = mAppsCustomizeTabHost.getCurrentTabTag();
            int appTabHostType = mAppsCustomizeTabHost.getContentTypeForTabTag(tag);
            
            if(appContentType == AppsCustomizePagedView.CONTENTTYPE_WIDGETS &&
            	appTabHostType == AppsCustomizePagedView.CONTENTTYPE_APPS_ALL){
            	mAppsCustomizeContent.setContentType(AppsCustomizePagedView.CONTENTTYPE_APPS_ALL);
            }
        }
	}
	
	@Override
    public void onPause() {
		
		// NOTE: We want all transitions from launcher to act as if the wallpaper were enabled
        // to be consistent.  So re-enable the flag here, and we will re-disable it as necessary
        // when Launcher resumes and we are still in AllApps.
        updateWallpaperVisibility(true);

        super.onPause();
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "(Launcher)onPause: this = " + this);
        }

//        /// M: Call the appropriate callback for the IMTKWidget on the current page when we pause Launcher.
//        mWorkspace.onPauseWhenShown(mWorkspace.getCurrentPage());
//        //resetReSyncFlags();
//
//        mPaused = true;
//        mDragController.cancelDrag();
//        mDragController.resetLastGestureUpTime();

        /// M: Disable the orientation listener when we pause Launcher.
        disableOrientationListener();
	}
	
	@Override
    public boolean onBackPressed() {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "Back key pressed, mState = " + mState + ", mOnResumeState = " + mOnResumeState);
        }
        boolean ret = super.onBackPressed();
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "Back key pressed, mState = " + mState + ", mOnResumeState = " + mOnResumeState
                    +"==ret:"+ret);
        }
    	if (!ret) {
    		if(mWorkspace != null){
	            mWorkspace.exitWidgetResizeMode();
	            // Back button is a no-op here, but give at least some feedback for the button press
	            mWorkspace.showOutlinesTemporarily();
    		}
        }
    	//android.util.Log.d("QsLog", "onBackPressed()====ret:"+ret);
        /// M: Cancel long press widget to add message.
        cancelLongPressWidgetToAddMessage();
        
        return ret;
    }
	
	public void onClick(View v) {
        // Make sure that rogue clicks don't get through while allapps is launching, or after the
        // view has detached (it's possible for this to happen if the view is removed mid touch).
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "Click on view " + v);
        }

        if (v.getWindowToken() == null) {
        	Util.Log.d(TAG, "Click on a view with no window token, directly return.");
            return;
        }

        if (!mWorkspace.isFinishedSwitchingState()) {
        	Util.Log.d(TAG, "The workspace is in switching state when clicking on view, directly return.");
            return;
        }

        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            // Open shortcut
            final Intent intent = ((ShortcutInfo) tag).intent;
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            intent.setSourceBounds(new Rect(pos[0], pos[1],
                    pos[0] + v.getWidth(), pos[1] + v.getHeight()));

            boolean success = startActivitySafely(v, intent, tag);

            if (success && v instanceof BubbleTextView) {
                mWaitingForResume = (BubbleTextView) v;
                mWaitingForResume.setStayPressed(true);
            }
        } else if (tag instanceof FolderInfo) {
            if (v instanceof FolderIcon) {
                FolderIcon fi = (FolderIcon) v;
                handleFolderClick(fi);
            }
        } else if (v == mAllAppsButton) {
            if (isAllAppsVisible()) {
                showWorkspace(true);
            } else {
                onClickAllAppsButton(v);
            }
        }
    }
	
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
        // this is an intercepted event being forwarded from mWorkspace;
        // clicking anywhere on the workspace causes the customization drawer to slide down
        showWorkspace(true);
        return false;
    }
	
	@Override
    public Object onRetainNonConfigurationInstance() {
		super.onRetainNonConfigurationInstance();
		if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onRetainNonConfigurationInstance: mSavedState = "
                    + mSavedState );
        }
        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.surrender();
        }

        return Boolean.TRUE;
    }
	
	@Override
    public boolean onNewIntent(Intent intent) {
	    if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onNewIntent: intent = " + intent
                    +"==mOnResumeState = " + Integer.toHexString(mOnResumeState)
                    +"==mState:"+Integer.toHexString(mState));
        }
	    
        if(super.onNewIntent(intent)){
            return true;
        }
        //android.util.Log.e("QsLog", "Defalut::onNewIntent()==");
        // Close the menu
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            // also will cancel mWaitingForResult.
            closeSystemDialogs();

            final boolean alreadyOnHome =
                    ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                        != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

            Runnable processIntent = new Runnable() {
                public void run() {
                    if (mWorkspace == null) {
                        // Can be cases where mWorkspace is null, this prevents a NPE
                        return;
                    }
                    Folder openFolder = mWorkspace.getOpenFolder();
                    // In all these cases, only animate if we're already on home
                    mWorkspace.exitWidgetResizeMode();
                    
                    boolean showpreview = false;
                    if (Util.ENABLE_DEBUG) {
                    	Util.Log.d(TAG, "onNewIntent: alreadyOnHome = " + alreadyOnHome
                    			+"==mState:"+mState
                    			+"==isTouchActive:"+mWorkspace.isTouchActive());
                    }
                    if (alreadyOnHome && mState == State.WORKSPACE && !mWorkspace.isTouchActive() &&
                            openFolder == null) {
                        /// M: Call the appropriate callback for the IMTKWidget on the current page
                        /// when press "Home" key move to default screen.
                        mWorkspace.moveOutAppWidget(mWorkspace.getCurrentPage());
                        showpreview = ((mOnResumeState != State.WORKSPACE_PREVIEW && 
                        					mWorkspace.getCurrentPage() == mWorkspace.getDefaultScreenIndex()) ? true : false);
                        mWorkspace.moveToDefaultScreen(true);
                    } else if(isWorkspacePreviewVisible()){
                    	hideWorkspacePreiews(true);
                    } else if(isCustomSettingsScreenVisible()){
                    	hideCustomSettingsScreen(true);
                    }
                    if (Util.ENABLE_DEBUG) {
                    	Util.Log.d(TAG, "onNewIntent: alreadyOnHome = " + alreadyOnHome
                    			+"==mState:"+mState
                    			+"==showpreview:"+showpreview);
                    }
                    closeFolder();
                    exitSpringLoadedDragMode();

                    // If we are already on home, then just animate back to the workspace,
                    // otherwise, just wait until onResume to set the state back to Workspace
                    if (alreadyOnHome) {
                        showWorkspace(true);
                    } else {
                        mOnResumeState = State.WORKSPACE;
                    }

                    final View v = getActivity().getWindow().peekDecorView();
                    if (v != null && v.getWindowToken() != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }

                    // Reset AllApps to its initial state
                    if (!alreadyOnHome && mAppsCustomizeTabHost != null) {
                        mAppsCustomizeTabHost.reset();
                    }
                    
                    if(showpreview){
                    	showWorkspacePreviews(true);
                    }
                }
            };

            if (alreadyOnHome && !mWorkspace.hasWindowFocus()) {
                // Delay processing of the intent to allow the status bar animation to finish
                // first in order to avoid janky animations.
                mWorkspace.postDelayed(processIntent, 350);
            } else {
                // Process the intent immediately.
                processIntent.run();
            }

        }
        
        return true;
    }
	
	private void handleFolderClick(FolderIcon folderIcon) {
        final FolderInfo info = folderIcon.getFolderInfo();
        Folder openFolder = mWorkspace.getFolderForTag(info);

        // If the folder info reports that the associated folder is open, then verify that
        // it is actually opened. There have been a few instances where this gets out of sync.
        if (info.opened && openFolder == null) {
            Util.Log.d(TAG, "Folder info marked as open, but associated folder is not open. Screen: "
                    + info.screen + " (" + info.cellX + ", " + info.cellY + ")");
            info.opened = false;
        }

        if (!info.opened && !folderIcon.getFolder().isDestroyed()) {
            // Close any open folder
            closeFolder();
            // Open the requested folder
            openFolder(folderIcon);
        } else {
            // Find the open folder...
            int folderScreen;
            if (openFolder != null) {
                folderScreen = mWorkspace.getPageForView(openFolder);
                // .. and close it
                closeFolder(openFolder);
                if (folderScreen != mWorkspace.getCurrentPage()) {
                    // Close any folder open on the current screen
                    closeFolder();
                    // Pull the folder onto this screen
                    openFolder(folderIcon);
                }
            }
        }
    }
	

    @Override
    public void onSaveInstanceState(Bundle outState) {
        
        super.onSaveInstanceState(outState);

        // Save the current AppsCustomize tab
        if (mAppsCustomizeTabHost != null) {
            String currentTabTag = mAppsCustomizeTabHost.getCurrentTabTag();
            if (currentTabTag != null) {
                outState.putString("apps_customize_currentTab", currentTabTag);
            }
            int currentIndex = mAppsCustomizeContent.getSaveInstanceStateIndex();
            outState.putInt("apps_customize_currentIndex", currentIndex);
            
            //currentIndex = mAppsCustomizeContentWidget.getSaveInstanceStateIndex();
            //outState.putInt("apps_customize_currentIndex_widgets", currentIndex);
        }
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, " onSaveInstanceState: outState = " + outState);
        }
    }
	
	private void setupViews(){
		mLauncherView = findViewById(R.id.launcher);
		DragLayer dragLayer = (DragLayer) findViewById(R.id.drag_layer);
		
		super.setupViews(dragLayer);
		final WorkspaceDefault workspace = (WorkspaceDefault) getWorkspace();//dragLayer.findViewWithTag("workspace");
		
        
//        mQsbDivider = findViewById(R.id.qsb_divider);
//        mDockDivider = findViewById(R.id.dock_divider);

        mLauncherView.setSystemUiVisibility(
        		View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        //mWorkspaceBackgroundDrawable = getResources().getDrawable(R.drawable.workspace_bg);
        //mBlackBackgroundDrawable = new ColorDrawable(Color.BLACK);
        mBlackBackgroundDrawable = new ColorDrawable(0x20000000);
        
        
        final DragController dragController = getDragController();
        // Setup the workspace
        workspace.setHapticFeedbackEnabled(false);
        workspace.setOnLongClickListener(this);
        workspace.setup(dragController);
        dragController.addDragListener(workspace);
        
     // Get the search/delete bar
        //mSearchDropTargetBar = (SearchDropTargetBar) dragLayer.findViewWithTag("qsb_bar");
        //mSearchDropTargetBar = (SearchDropTargetBar) dragLayer.findViewById(R.id.qsb_bar);

        // Setup AppsCustomize
        mAppsCustomizeTabHost = (AppsCustomizeTabHost) dragLayer.findViewById(R.id.apps_customize_pane);
        mAppsCustomizeContent = (AppsCustomizePagedView)
                mAppsCustomizeTabHost.findViewById(R.id.apps_customize_pane_content);
//        mAppsCustomizeContentWidget = (AppsCustomizePagedView)
//        		mAppsCustomizeTabHost.findViewById(R.id.apps_customize_pane_content_widgets);
        mAppsCustomizeContent.setup(this, dragController);
        //mAppsCustomizeContentWidget.setup(this, dragController);
        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        dragController.setDragScoller(workspace);
        dragController.setScrollView(dragLayer);
        dragController.setMoveTarget(workspace);
        dragController.addDropTarget(workspace);
        if (mSearchDropTargetBar != null) {
            mSearchDropTargetBar.setup(this, dragController);
        }
        
        PageViewIndicator indicator = (PageViewIndicator)getPageViewIndicator();// (dragLayer.findViewById(R.id.workspace_paged_view_indicator));
        if(indicator != null){
        	indicator.initial(workspace.getCurrentPage(), workspace.getPageCount());
        	workspace.setPageSwitchListener(indicator);
        }
        
//        TopFloatBar topFloatBar = (TopFloatBar)mLauncherView.findViewWithTag("topFloatBar");
//        if(topFloatBar != null)
//        	workspace.setTopFloatBar(topFloatBar);
	}
	
	public void onAttachedToWindow() {
    	super.onAttachedToWindow();
    	
    	// Listen for broadcasts related to user-presence
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        //if(isSupportGesture())
         //   filter.addAction(QsIntent.ACTION_JZS_KEYGUARD_UNLOCKED);
        registerReceiver(mReceiver, filter);
	}
    
    public void onDetachedFromWindow() {
    	if (mAttached) {
            unregisterReceiver(mReceiver);
            mAttached = false;
        }
    	super.onDetachedFromWindow();
	}
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	
    }
    
    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        // The following code used to be in onResume, but it turns out onResume is called when
        // you're in All Apps and click home to go to the workspace. onWindowVisibilityChanged
        // is a more appropriate event to handle
        if (mVisible) {
            mAppsCustomizeTabHost.onWindowVisible();
            if (!mWorkspaceLoading) {
                final ViewTreeObserver observer = mWorkspace.getViewTreeObserver();
                // We want to let Launcher draw itself at least once before we force it to build
                // layers on all the workspace pages, so that transitioning to Launcher from other
                // apps is nice and speedy. Usually the first call to preDraw doesn't correspond to
                // a true draw so we wait until the second preDraw call to be safe
                observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    public boolean onPreDraw() {
                        // We delay the layer building a bit in order to give
                        // other message processing a time to run.  In particular
                        // this avoids a delay in hiding the IME if it was
                        // currently shown, because doing that may involve
                        // some communication back with the app.
                    	getWorkspace().postDelayed(mBuildLayersRunnable, 500);

                        observer.removeOnPreDrawListener(this);
                        return true;
                    }
                });
            }
            // When Launcher comes back to foreground, a different Activity might be responsible for
            // the app market intent, so refresh the icon
            updateAppMarketIcon();
            clearTypedText();
        }
    }
    
    private void setWorkspaceBackground(boolean workspace) {
//        mLauncherView.setBackgroundDrawable(workspace ?
//                mWorkspaceBackgroundDrawable : mBlackBackgroundDrawable);
    }

	protected void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }
        
        super.restoreState(savedState);

     // Restore the AppsCustomize tab
        if (mAppsCustomizeTabHost != null) {
            String curTab = savedState.getString("apps_customize_currentTab");
            if (curTab != null) {
                // We set this directly so that there is no delay before the tab is set

            	mAppsCustomizeContent.setContentType(
                        mAppsCustomizeTabHost.getContentTypeForTabTag(curTab));
            	mAppsCustomizeContent.setVisibility(View.VISIBLE);
 
                mAppsCustomizeTabHost.setCurrentTabByTag(curTab);

               	mAppsCustomizeContent.loadAssociatedPages(
                        mAppsCustomizeContent.getCurrentPage());

                
                if (mState == State.APPS_CUSTOMIZE && mAppsCustomizeTabHost.getVisibility() != View.VISIBLE){
                	mAppsCustomizeTabHost.setVisibility(View.VISIBLE);
                }
            }

            int currentIndex = savedState.getInt("apps_customize_currentIndex");
            mAppsCustomizeContent.restorePageForIndex(currentIndex);
        }
    }

    /**
     * M: Pop up message allows to you add only one IMTKWidget for the given AppWidgetInfo.
     *
     * @param info The information of the IMTKWidget.
     */
    public void showOnlyOneWidgetMessage(PendingAddWidgetInfo info) {
//        try {
//            PackageManager pm = getPackageManager();
//            String label = pm.getApplicationLabel(pm.getApplicationInfo(info.componentName.getPackageName(), 0)).toString();
//            Toast.makeText(this, getString(R.string.one_video_widget, label), Toast.LENGTH_SHORT).show();
//        } catch (PackageManager.NameNotFoundException e) {
//            Util.Log.e(TAG, "Got NameNotFounceException when showOnlyOneWidgetMessage.", e);
//        }
        // Exit spring loaded mode if necessary after adding the widget.
        exitSpringLoadedDragModeDelayed(false, false, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isWorkspaceLocked()) {
            return false;
        }

        super.onCreateOptionsMenu(menu);

        Intent manageApps = new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS);
        manageApps.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        Intent settings = new Intent(android.provider.Settings.ACTION_SETTINGS);
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//        String helpUrl = getString(R.string.help_url);
//        Intent help = new Intent(Intent.ACTION_VIEW, Uri.parse(helpUrl));
//        help.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
//                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

	 if(!com.qs.utils.ConfigOption.QS_SUPPORT_LOCKSCREEN_WALLPAPER || com.qs.utils.ConfigOption.QS_PRJ_NAME.startsWith("A698PIERRE"))
	 {
        menu.add(MENU_GROUP_WALLPAPER, MENU_WALLPAPER_SETTINGS, 0, R.string.menu_wallpaper)
            .setIcon(android.R.drawable.ic_menu_gallery)
            .setAlphabeticShortcut('W');
	 }	
        menu.add(0, MENU_MANAGE_APPS, 0, R.string.menu_manage_apps)
            .setIcon(android.R.drawable.ic_menu_manage)
            .setIntent(manageApps)
            .setAlphabeticShortcut('M');
        menu.add(0, MENU_SYSTEM_SETTINGS, 0, R.string.menu_settings)
            .setIcon(android.R.drawable.ic_menu_preferences)
            .setIntent(settings)
            .setAlphabeticShortcut('P');
//        if (!helpUrl.isEmpty()) {
//            menu.add(0, MENU_HELP, 0, R.string.menu_help)
//                .setIcon(android.R.drawable.ic_menu_help)
//                .setIntent(help)
//                .setAlphabeticShortcut('H');
//        } 
        
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mAppsCustomizeTabHost != null && mAppsCustomizeTabHost.isTransitioning()) {
            return false;
        }
        boolean allAppsVisible = (mAppsCustomizeTabHost.getVisibility() == View.VISIBLE);
        menu.setGroupVisible(MENU_GROUP_WALLPAPER, !allAppsVisible);

        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
////        case MENU_CUSTOM_ICON:
////        	showCropImageActivity();
////        	return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    
    
    public void onClickSearchButton(View v) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onClickSearchButton v = " + v);
        }

        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        onSearchRequested();
    }

    /**
     * Event handler for the voice button
     *
     * @param v The view that was clicked.
     */
    public void onClickVoiceButton(View v) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onClickVoiceButton v = " + v);
        }

        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        try {
            final SearchManager searchManager =
                    (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            ComponentName activityName = searchManager.getGlobalSearchActivity();
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (activityName != null) {
                intent.setPackage(activityName.getPackageName());
            }
            startActivity(null, intent, "onClickVoiceButton");
            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast);
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivitySafely(null, intent, "onClickVoiceButton");
        }
    }
    private Intent mAppMarketIntent = null;
    public void onClickAppMarketButton(View v) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onClickAppMarketButton v = " + v + ", mAppMarketIntent = " + mAppMarketIntent);
        }

        if (mAppMarketIntent != null) {
            startActivitySafely(v, mAppMarketIntent, "app market");
        } else {
        	Util.Log.e(TAG, "Invalid app market intent.");
        }
    }

    public boolean onLongClick(View v) {
        if(!super.onLongClick(v)){
            return false;
        }

        /// M: modidfied for Unread feature, to find CellLayout through while loop.
        while (!(v instanceof CellLayout)) {
            v = (View) v.getParent();
        }

        resetAddInfo();
        CellLayout.CellInfo longClickCellInfo = (CellLayout.CellInfo) v.getTag();
        // This happens when long clicking an item with the dpad/trackball
        if (longClickCellInfo == null) {
            return true;
        }

        // The hotseat touch handling does not go through Workspace, and we always allow long press
        // on hotseat items.
        final View itemUnderLongClick = longClickCellInfo.cell;
        boolean allowLongPress = isHotseatLayout(v) || mWorkspace.allowLongPress();
        
        if (allowLongPress && !mDragController.isDragging()) {
            if (itemUnderLongClick == null) {
                // User long pressed on empty space
                mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                startWallpaper();
            } else {
                if (!(itemUnderLongClick instanceof Folder)) {
                    /// M: Call the appropriate callback for the IMTKWidget on the current page
                    /// when long click and begin to drag IMTKWidget.
                    mWorkspace.startDragAppWidget(mWorkspace.getCurrentPage());
                    // User long pressed on an item
                    
                    enterSpringLoadedDragMode();
                    
                    mWorkspace.startDrag(longClickCellInfo);
                }
            }
        }
        return true;
    }
    
    private void setPivotsForZoom(View view, float scaleFactor) {
        view.setPivotX(view.getWidth() / 2.0f);
        view.setPivotY(view.getHeight() / 2.0f);
    }

    void disableWallpaperIfInAllApps() {
        // Only disable it if we are in all apps
        if (false && isAllAppsVisible()) {
            if (mAppsCustomizeTabHost != null &&
                    !mAppsCustomizeTabHost.isTransitioning()) {
                updateWallpaperVisibility(false);
            }
        }
    }
    
    public void updateWallpaperVisibility(boolean visible) {
        if(false){
        	super.updateWallpaperVisibility(true);//set bg half transparent
        	//super.updateWallpaperVisibility(visible);
            setWorkspaceBackground(visible);
        }
    }
    
    private void dispatchOnLauncherTransitionPrepare(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionPrepare(this, animated, toWorkspace);
        }
    }

    private void dispatchOnLauncherTransitionStart(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStart(this, animated, toWorkspace);
        }

        // Update the workspace transition step as well
        dispatchOnLauncherTransitionStep(v, 0f);
    }

    private void dispatchOnLauncherTransitionStep(View v, float t) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStep(this, t);
        }
    }

    private void dispatchOnLauncherTransitionEnd(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionEnd(this, animated, toWorkspace);
        }

        // Update the workspace transition step as well
        dispatchOnLauncherTransitionStep(v, 1f);
    }
    
    protected void showAppsCustomizeHelper(final boolean animated, final boolean springLoaded) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "showAppsCustomizeHelper animated = " + animated + ", springLoaded = " + springLoaded);
        }

        if (mStateAnimation != null) {
            mStateAnimation.cancel();
            mStateAnimation = null;
        }
        final Resources res = getResources();

        final int duration = res.getInteger(R.integer.config_appsCustomizeZoomInTime);
        final int fadeDuration = res.getInteger(R.integer.config_appsCustomizeFadeInTime);
        final float scale = (float) res.getInteger(R.integer.config_appsCustomizeZoomScaleFactor);
        final View fromView = mWorkspace;
        final AppsCustomizeTabHost toView = mAppsCustomizeTabHost;
        final int startDelay =
                res.getInteger(R.integer.config_workspaceAppsCustomizeAnimationStagger);

        setPivotsForZoom(toView, scale);

        // Shrink workspaces away if going to AppsCustomize from workspace
        Animator workspaceAnim =
                mWorkspace.getChangeStateAnimation(Workspace.State.SMALL, animated);

        if (animated) {
            toView.setScaleX(scale);
            toView.setScaleY(scale);
            final LauncherViewPropertyAnimator scaleAnim = new LauncherViewPropertyAnimator(toView);
            scaleAnim.
                scaleX(1f).scaleY(1f).
                setDuration(duration).
                setInterpolator(new Workspace.ZoomOutInterpolator());

            toView.setVisibility(View.VISIBLE);
            toView.setAlpha(0f);
            final ObjectAnimator alphaAnim = ObjectAnimator
                .ofFloat(toView, "alpha", 0f, 1f)
                .setDuration(fadeDuration);
            alphaAnim.setInterpolator(new DecelerateInterpolator(1.5f));
            alphaAnim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (animation == null) {
                        throw new RuntimeException("animation is null");
                    }
                    float t = (Float) animation.getAnimatedValue();
                    dispatchOnLauncherTransitionStep(fromView, t);
                    dispatchOnLauncherTransitionStep(toView, t);
                }
            });

            // toView should appear right at the end of the workspace shrink
            // animation
            mStateAnimation = LauncherAnimUtils.createAnimatorSet();
            mStateAnimation.play(scaleAnim).after(startDelay);
            mStateAnimation.play(alphaAnim).after(startDelay);

            mStateAnimation.addListener(new AnimatorListenerAdapter() {
                boolean animationCancelled = false;

                @Override
                public void onAnimationStart(Animator animation) {
                    updateWallpaperVisibility(true);
                    // Prepare the position
                    toView.setTranslationX(0.0f);
                    toView.setTranslationY(0.0f);
                    toView.setVisibility(View.VISIBLE);
                    toView.bringToFront();
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    dispatchOnLauncherTransitionEnd(fromView, animated, false);
                    dispatchOnLauncherTransitionEnd(toView, animated, false);

//                    if (mWorkspace != null && !springLoaded && !isScreenLarge()) {
//                        // Hide the workspace scrollbar
//                        mWorkspace.hideScrollingIndicator(true);
//                        hideDockDivider();
//                    }
                    if (!animationCancelled) {
                        updateWallpaperVisibility(false);
                    }

                    // Hide the search bar
                    if (mSearchDropTargetBar != null) {
                        mSearchDropTargetBar.hideSearchBar(false);
                    }
                    
                    if(mWorkspaceContainer != null) mWorkspaceContainer.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    animationCancelled = true;
                }
            });

            if (workspaceAnim != null) {
                mStateAnimation.play(workspaceAnim);
            }

            boolean delayAnim = false;
            final ViewTreeObserver observer;

            dispatchOnLauncherTransitionPrepare(fromView, animated, false);
            dispatchOnLauncherTransitionPrepare(toView, animated, false);

            // If any of the objects being animated haven't been measured/laid out
            // yet, delay the animation until we get a layout pass
            if ((((LauncherTransitionable) toView).getContent().getMeasuredWidth() == 0) ||
                    (mWorkspace.getMeasuredWidth() == 0) ||
                    (toView.getMeasuredWidth() == 0)) {
                observer = mWorkspace.getViewTreeObserver();
                delayAnim = true;
            } else {
                observer = null;
            }

            final AnimatorSet stateAnimation = mStateAnimation;
            final Runnable startAnimRunnable = new Runnable() {
                public void run() {
                    // Check that mStateAnimation hasn't changed while
                    // we waited for a layout/draw pass
                    if (mStateAnimation != stateAnimation)
                        return;
                    setPivotsForZoom(toView, scale);
                    dispatchOnLauncherTransitionStart(fromView, animated, false);
                    dispatchOnLauncherTransitionStart(toView, animated, false);
                    toView.post(new Runnable() {
                        public void run() {
                            // Check that mStateAnimation hasn't changed while
                            // we waited for a layout/draw pass
                            if (mStateAnimation != stateAnimation)
                                return;
                            mStateAnimation.start();
                        }
                    });
                }
            };
            if (delayAnim) {
                final OnGlobalLayoutListener delayedStart = new OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        toView.post(startAnimRunnable);
                        observer.removeOnGlobalLayoutListener(this);
                    }
                };
                observer.addOnGlobalLayoutListener(delayedStart);
            } else {
                startAnimRunnable.run();
            }
        } else {
            toView.setTranslationX(0.0f);
            toView.setTranslationY(0.0f);
            toView.setScaleX(1.0f);
            toView.setScaleY(1.0f);
            toView.setVisibility(View.VISIBLE);
            toView.bringToFront();

            if (!springLoaded && !isScreenLarge()) {
                // Hide the workspace scrollbar
                //mWorkspace.hideScrollingIndicator(true);
                hideDockDivider();

                // Hide the search bar
                if (mSearchDropTargetBar != null) {
                    mSearchDropTargetBar.hideSearchBar(false);
                }
            }
            dispatchOnLauncherTransitionPrepare(fromView, animated, false);
            dispatchOnLauncherTransitionStart(fromView, animated, false);
            dispatchOnLauncherTransitionEnd(fromView, animated, false);
            dispatchOnLauncherTransitionPrepare(toView, animated, false);
            dispatchOnLauncherTransitionStart(toView, animated, false);
            dispatchOnLauncherTransitionEnd(toView, animated, false);
            updateWallpaperVisibility(false);
            if(mWorkspaceContainer != null) mWorkspaceContainer.setVisibility(View.INVISIBLE);
        }
    }
private final static boolean DISABLE_APP_VIEW_ANIMATION = false;
    /**
     * Zoom the camera back into the workspace, hiding 'fromView'.
     * This is the opposite of showAppsCustomizeHelper.
     * @param animated If true, the transition will be animated.
     */
    protected void hideAppsCustomizeHelper(int toState, final boolean animated,
            final boolean springLoaded, final Runnable onCompleteRunnable) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.e(TAG, "hideAppsCustomzieHelper(0) toState = " + toState + ", animated = " + animated
                    + ", springLoaded = " + springLoaded);
        }

        if (mStateAnimation != null) {
            mStateAnimation.cancel();
            mStateAnimation = null;
        }
        Resources res = getResources();
        
        if (Util.ENABLE_DEBUG) {
            Util.Log.e(TAG, "hideAppsCustomzieHelper(1) toState = " + toState + ", animated = " + animated
                    + ", springLoaded = " + springLoaded);
        }
        
        if(mWorkspaceContainer != null) mWorkspaceContainer.setVisibility(View.VISIBLE);

        final int duration = res.getInteger(R.integer.config_appsCustomizeZoomOutTime);
        final int fadeOutDuration =
                res.getInteger(R.integer.config_appsCustomizeFadeOutTime);
        final float scaleFactor = (float)
                res.getInteger(R.integer.config_appsCustomizeZoomScaleFactor);
        final View fromView = mAppsCustomizeTabHost;
        final View toView = mWorkspace;
        Animator workspaceAnim = null;

        if (toState == State.WORKSPACE) {
            int stagger = res.getInteger(R.integer.config_appsCustomizeWorkspaceAnimationStagger);
            workspaceAnim = mWorkspace.getChangeStateAnimation(
                    Workspace.State.NORMAL, animated, stagger);
        } else if (toState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            workspaceAnim = mWorkspace.getChangeStateAnimation(
                    Workspace.State.SPRING_LOADED, animated);
        }
        if (Util.ENABLE_DEBUG) {
            Util.Log.e(TAG, "hideAppsCustomzieHelper(2) toState = " + toState + ", animated = " + animated
                    + ", springLoaded = " + springLoaded);
        }
        if(!DISABLE_APP_VIEW_ANIMATION){
            setPivotsForZoom(fromView, scaleFactor);
            updateWallpaperVisibility(true);
        }
        //showHotseat(animated);
        if (animated) {
            final LauncherViewPropertyAnimator scaleAnim;
            final ObjectAnimator alphaAnim;
            if(!DISABLE_APP_VIEW_ANIMATION){
                scaleAnim = 
                        new LauncherViewPropertyAnimator(fromView);
                scaleAnim.
                    scaleX(scaleFactor).scaleY(scaleFactor).
                    setDuration(duration).
                    setInterpolator(new Workspace.ZoomInInterpolator());
            
            
                alphaAnim = ObjectAnimator
                    .ofFloat(fromView, "alpha", 1f, 0f)
                    .setDuration(fadeOutDuration);
                alphaAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                alphaAnim.addUpdateListener(new AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float t = 1f - (Float) animation.getAnimatedValue();
                        dispatchOnLauncherTransitionStep(fromView, t);
                        dispatchOnLauncherTransitionStep(toView, t);
                    }
                });
            } else {
                scaleAnim = null;
                alphaAnim = null;
            }

            mStateAnimation = LauncherAnimUtils.createAnimatorSet();

            dispatchOnLauncherTransitionPrepare(fromView, animated, true);
            dispatchOnLauncherTransitionPrepare(toView, animated, true);
            mAppsCustomizeContent.pauseScrolling();
            //mAppsCustomizeContentWidget.pauseScrolling();
            if (Util.ENABLE_DEBUG) {
                Util.Log.e(TAG, "hideAppsCustomzieHelper(3) mStateAnimation = " + mStateAnimation);
            }
            mStateAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if(!DISABLE_APP_VIEW_ANIMATION){
                        updateWallpaperVisibility(true);
                        fromView.setVisibility(View.GONE);
                    }
                    dispatchOnLauncherTransitionEnd(fromView, animated, true);
                    dispatchOnLauncherTransitionEnd(toView, animated, true);
//                    if (mWorkspace != null) {
//                        mWorkspace.hideScrollingIndicator(false);
//                    }
                    if (onCompleteRunnable != null) {
                        onCompleteRunnable.run();
                    }
                    mAppsCustomizeContent.updateCurrentPageScroll();
                    mAppsCustomizeContent.resumeScrolling();
                }
            });
            
            if(!DISABLE_APP_VIEW_ANIMATION){
                mStateAnimation.playTogether(scaleAnim, alphaAnim);
            }
            
            if (workspaceAnim != null) {
                if(DISABLE_APP_VIEW_ANIMATION){
                    fromView.setVisibility(View.GONE);
                }
                mStateAnimation.play(workspaceAnim);
            } 
            
            dispatchOnLauncherTransitionStart(fromView, animated, true);
            dispatchOnLauncherTransitionStart(toView, animated, true);
            final Animator stateAnimation = mStateAnimation;
            mWorkspace.post(new Runnable() {
                public void run() {
                    if (stateAnimation != mStateAnimation)
                        return;
                    mStateAnimation.start();
                }
            });
            
            if(DISABLE_APP_VIEW_ANIMATION && workspaceAnim == null){
                fromView.setVisibility(View.GONE);
                
                dispatchOnLauncherTransitionEnd(fromView, animated, true);
                dispatchOnLauncherTransitionEnd(toView, animated, true);
                
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
            }
            
        } else {
            fromView.setVisibility(View.GONE);
            dispatchOnLauncherTransitionPrepare(fromView, animated, true);
            dispatchOnLauncherTransitionStart(fromView, animated, true);
            dispatchOnLauncherTransitionEnd(fromView, animated, true);
            dispatchOnLauncherTransitionPrepare(toView, animated, true);
            dispatchOnLauncherTransitionStart(toView, animated, true);
            dispatchOnLauncherTransitionEnd(toView, animated, true);
            //mWorkspace.hideScrollingIndicator(false);
        }
        
        if (Util.ENABLE_DEBUG) {
            Util.Log.e(TAG, "hideAppsCustomzieHelper(end) toState = " + toState + ", animated = " + animated
                    + ", springLoaded = " + springLoaded);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onTrimMemory: level = " + level);
        }

        if (mAppsCustomizeTabHost != null && level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            mAppsCustomizeTabHost.onTrimMemory();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus) {
            // When another window occludes launcher (like the notification shade, or recents),
            // ensure that we enable the wallpaper flag so that transitions are done correctly.
            updateWallpaperVisibility(true);
        } else {
            // When launcher has focus again, disable the wallpaper if we are in AllApps
            mWorkspace.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disableWallpaperIfInAllApps();
                }
            }, 500);
        }
    }

    public void showWorkspace(boolean animated, Runnable onCompleteRunnable) {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "showWorkspace: animated = " + animated + ", mState = " + mState);
        }
        
    	super.showWorkspace(animated, onCompleteRunnable);
        
        
        if (mState != State.WORKSPACE) {
            boolean wasInSpringLoadedMode = (mState == State.APPS_CUSTOMIZE_SPRING_LOADED);
            //mWorkspace.setVisibility(View.VISIBLE);
            if (Util.ENABLE_DEBUG) {
                Util.Log.d(TAG, "showWorkspace: animated = " + animated + ", wasInSpringLoadedMode = " + wasInSpringLoadedMode);
            }
            hideAppsCustomizeHelper(State.WORKSPACE, animated, false, onCompleteRunnable);

            // Show the search bar (only animate if we were showing the drop target bar in spring
            // loaded mode)
            if (mSearchDropTargetBar != null) {
                mSearchDropTargetBar.showSearchBar(wasInSpringLoadedMode);
            }

            // We only need to animate in the dock divider if we're going from spring loaded mode
            //showDockDivider(animated && wasInSpringLoadedMode);

            // Set focus to the AppsCustomize button
            if (mAllAppsButton != null) {
                mAllAppsButton.requestFocus();
            }
        }

        //mWorkspace.flashScrollingIndicator(animated);

        // Change the state *after* we've called all the transition code
        mState = State.WORKSPACE;

        // Resume the auto-advance of widgets
        mUserPresent = true;
        updateRunning();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView()
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    public void showAllApps(boolean animated) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "showAllApps: animated = " + animated + ", mState = " + mState
                    + ", mCurrentBounds = " + mCurrentBounds);
        }
        if (mState != State.WORKSPACE) return;
        
        if(mWorkspaceContainer != null) mWorkspaceContainer.setVisibility(View.INVISIBLE);
        
        /// M: Recorder current bounds of current cellLayout.
        if (mWorkspace != null) {
            mDragLayer.getDescendantRectRelativeToSelf(mWorkspace.getCurrentDropLayout(), mCurrentBounds);
        }

        /// M: Call the appropriate callback for the IMTKWidget on the current page when enter all apps list.
        mWorkspace.startCovered(mWorkspace.getCurrentPage());
        showAppsCustomizeHelper(animated, false);
        if(mAppsCustomizeTabHost != null) mAppsCustomizeTabHost.requestFocus();

        // Change the state *after* we've called all the transition code
        mState = State.APPS_CUSTOMIZE;

        // Pause the auto-advance of widgets until we are out of AllApps
        mUserPresent = false;
        updateRunning();
        closeFolder();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView()
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    public void enterSpringLoadedDragMode() {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "enterSpringLoadedDragMode mState = " + mState + ", mOnResumeState = " + mOnResumeState);
        }
        
    	if (isAllAppsVisible()) {
            hideAppsCustomizeHelper(State.APPS_CUSTOMIZE_SPRING_LOADED, true, true, null);
            hideDockDivider();
            mState = State.APPS_CUSTOMIZE_SPRING_LOADED;
        } else {
            super.enterSpringLoadedDragMode();
        }
    }

    public void hideDockDivider() {
        if (mQsbDivider != null && mDockDivider != null) {
            mQsbDivider.setVisibility(View.INVISIBLE);
            mDockDivider.setVisibility(View.INVISIBLE);
        }
    }

    public void showDockDivider(boolean animated) {
//        if (mQsbDivider != null && mDockDivider != null) {
//            mQsbDivider.setVisibility(View.VISIBLE);
//            mDockDivider.setVisibility(View.VISIBLE);
//            if (mDividerAnimator != null) {
//                mDividerAnimator.cancel();
//                mQsbDivider.setAlpha(1f);
//                mDockDivider.setAlpha(1f);
//                mDividerAnimator = null;
//            }
//            if (animated) {
//                mDividerAnimator = LauncherAnimUtils.createAnimatorSet();
//                mDividerAnimator.playTogether(LauncherAnimUtils.ofFloat(mQsbDivider, "alpha", 1f),
//                        LauncherAnimUtils.ofFloat(mDockDivider, "alpha", 1f));
//                int duration = 0;
//                if (mSearchDropTargetBar != null) {
//                    duration = mSearchDropTargetBar.getTransitionInDuration();
//                }
//                mDividerAnimator.setDuration(duration);
//                mDividerAnimator.start();
//            }
//        }
    }

    public void lockAllApps() {
        // TODO
    }

    public void unlockAllApps() {
        // TODO
    }

    

    /**
     * Add an item from all apps or customize onto the given workspace screen.
     * If layout is null, add to the current screen.
     */
    public void addExternalItemToScreen(ItemInfo itemInfo, final CellLayout layout) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "addExternalItemToScreen itemInfo = " + itemInfo + ", layout = " + layout);
        }

        if (!mWorkspace.addExternalItemToScreen(itemInfo, layout)) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
        }
    }
    
    /** Maps the current orientation to an index for referencing orientation correct global icons */
    private int getCurrentOrientationIndexForGlobalIcons() {
        // default - 0, landscape - 1
        switch (getResources().getConfiguration().orientation) {
        case Configuration.ORIENTATION_LANDSCAPE:
            return 1;
        default:
            return 0;
        }
    }

    private Drawable getExternalPackageToolbarIcon(ComponentName activityName, String resourceName) {
        try {
            PackageManager packageManager = getPackageManager();
            // Look for the toolbar icon specified in the activity meta-data
            Bundle metaData = packageManager.getActivityInfo(
                    activityName, PackageManager.GET_META_DATA).metaData;
            if (metaData != null) {
                int iconResId = metaData.getInt(resourceName);
                if (iconResId != 0) {
                    Resources res = packageManager.getResourcesForActivity(activityName);
                    return res.getDrawable(iconResId);
                }
            }
        } catch (NameNotFoundException e) {
            // This can happen if the activity defines an invalid drawable
            Util.Log.w(TAG, "Failed to load toolbar icon; " + activityName.flattenToShortString() +
                    " not found", e);
        } catch (Resources.NotFoundException nfe) {
            // This can happen if the activity defines an invalid drawable
            Util.Log.w(TAG, "Failed to load toolbar icon from " + activityName.flattenToShortString(),
                    nfe);
        }
        return null;
    }

    // if successful in getting icon, return it; otherwise, set button to use default drawable
    private Drawable.ConstantState updateTextButtonWithIconFromExternalActivity(
            int buttonId, ComponentName activityName, int fallbackDrawableId,
            String toolbarResourceName) {
        Drawable toolbarIcon = getExternalPackageToolbarIcon(activityName, toolbarResourceName);
        Resources r = getResources();
        if(buttonId == R.id.market_button){
        	toolbarIcon = r.getDrawable(R.drawable.ic_launcher_market_holo);
        }
        int w = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_width);
        int h = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_height);

        TextView button = (TextView) findViewById(buttonId);
        // If we were unable to find the icon via the meta-data, use a generic one
        if (toolbarIcon == null) {
            toolbarIcon = r.getDrawable(fallbackDrawableId);
            toolbarIcon.setBounds(0, 0, w, h);
            if (button != null) {
                button.setCompoundDrawables(toolbarIcon, null, null, null);
            }
            return null;
        } else {
            toolbarIcon.setBounds(0, 0, w, h);
            if (button != null) {
                button.setCompoundDrawables(toolbarIcon, null, null, null);
            }
            return toolbarIcon.getConstantState();
        }
    }

    // if successful in getting icon, return it; otherwise, set button to use default drawable
    private Drawable.ConstantState updateButtonWithIconFromExternalActivity(
            int buttonId, ComponentName activityName, int fallbackDrawableId,
            String toolbarResourceName) {
        ImageView button = (ImageView) findViewById(buttonId);
        Drawable toolbarIcon = getExternalPackageToolbarIcon(activityName, toolbarResourceName);

        if (button != null) {
            // If we were unable to find the icon via the meta-data, use a
            // generic one
            if (toolbarIcon == null) {
                button.setImageResource(fallbackDrawableId);
            } else {
                button.setImageDrawable(toolbarIcon);
            }
        }

        return toolbarIcon != null ? toolbarIcon.getConstantState() : null;

    }

    private void updateTextButtonWithDrawable(int buttonId, Drawable d) {
        TextView button = (TextView) findViewById(buttonId);
        button.setCompoundDrawables(d, null, null, null);
    }

    private void updateButtonWithDrawable(int buttonId, Drawable.ConstantState d) {
        ImageView button = (ImageView) findViewById(buttonId);
        button.setImageDrawable(d.newDrawable(getResources()));
    }

    private void invalidatePressedFocusedStates(View container, View button) {
        if (container instanceof HolographicLinearLayout) {
            HolographicLinearLayout layout = (HolographicLinearLayout) container;
            layout.invalidatePressedFocusedStates();
        } else if (button instanceof HolographicImageView) {
            HolographicImageView view = (HolographicImageView) button;
            view.invalidatePressedFocusedStates();
        }
    }
    
    private void updateGlobalIcons() {
        boolean searchVisible = false;
        boolean voiceVisible = false;
        // If we have a saved version of these external icons, we load them up immediately
        int coi = getCurrentOrientationIndexForGlobalIcons();
        if (sGlobalSearchIcon[coi] == null || sVoiceSearchIcon[coi] == null ||
                sAppMarketIcon[coi] == null) {
            updateAppMarketIcon();
            searchVisible = updateGlobalSearchIcon();
            voiceVisible = updateVoiceSearchIcon(searchVisible);
        }
        if (sGlobalSearchIcon[coi] != null) {
             updateGlobalSearchIcon(sGlobalSearchIcon[coi]);
             searchVisible = true;
        }
        if (sVoiceSearchIcon[coi] != null) {
            updateVoiceSearchIcon(sVoiceSearchIcon[coi]);
            voiceVisible = true;
        }
        if (sAppMarketIcon[coi] != null) {
            updateAppMarketIcon(sAppMarketIcon[coi]);
        }
        if (mSearchDropTargetBar != null) {
            mSearchDropTargetBar.onSearchPackagesChanged(searchVisible, voiceVisible);
        }
    }

    private boolean updateGlobalSearchIcon() {
        final View searchButtonContainer = findViewById(R.id.search_button_container);
        final ImageView searchButton = (ImageView) findViewById(R.id.search_button);
        final View voiceButtonContainer = findViewById(R.id.voice_button_container);
        final View voiceButton = findViewById(R.id.voice_button);
        final View voiceButtonProxy = findViewById(R.id.voice_button_proxy);

        final SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName activityName = searchManager.getGlobalSearchActivity();
        if (activityName != null) {
            /// M: only show search engine name on non-OP01 projects.
            
                searchButton.setImageResource(R.drawable.ic_home_search_normal_holo);


            if (searchButtonContainer != null) searchButtonContainer.setVisibility(View.VISIBLE);
            searchButton.setVisibility(View.VISIBLE);
            invalidatePressedFocusedStates(searchButtonContainer, searchButton);
            return true;
        } else {
            // We disable both search and voice search when there is no global search provider
            if (searchButtonContainer != null) searchButtonContainer.setVisibility(View.GONE);
            if (voiceButtonContainer != null) voiceButtonContainer.setVisibility(View.GONE);
            searchButton.setVisibility(View.GONE);
            voiceButton.setVisibility(View.GONE);
            if (voiceButtonProxy != null) {
                voiceButtonProxy.setVisibility(View.GONE);
            }
            return false;
        }
    }

    private void updateGlobalSearchIcon(Drawable.ConstantState d) {
        final View searchButtonContainer = findViewById(R.id.search_button_container);
        final View searchButton = (ImageView) findViewById(R.id.search_button);
        updateButtonWithDrawable(R.id.search_button, d);
        invalidatePressedFocusedStates(searchButtonContainer, searchButton);
    }

    private boolean updateVoiceSearchIcon(boolean searchVisible) {
        final View voiceButtonContainer = findViewById(R.id.voice_button_container);
        final View voiceButton = findViewById(R.id.voice_button);
        final View voiceButtonProxy = findViewById(R.id.voice_button_proxy);

        // We only show/update the voice search icon if the search icon is enabled as well
        final SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName globalSearchActivity = searchManager.getGlobalSearchActivity();

        ComponentName activityName = null;
        if (globalSearchActivity != null) {
            // Check if the global search activity handles voice search
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            intent.setPackage(globalSearchActivity.getPackageName());
            activityName = intent.resolveActivity(getPackageManager());
        }

        if (activityName == null) {
            // Fallback: check if an activity other than the global search activity
            // resolves this
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            activityName = intent.resolveActivity(getPackageManager());
        }
        if (searchVisible && activityName != null) {
            int coi = getCurrentOrientationIndexForGlobalIcons();
            sVoiceSearchIcon[coi] = updateButtonWithIconFromExternalActivity(
                    R.id.voice_button, activityName, R.drawable.ic_home_voice_search_holo,
                    TOOLBAR_VOICE_SEARCH_ICON_METADATA_NAME);
            if (sVoiceSearchIcon[coi] == null) {
                sVoiceSearchIcon[coi] = updateButtonWithIconFromExternalActivity(
                        R.id.voice_button, activityName, R.drawable.ic_home_voice_search_holo,
                        TOOLBAR_ICON_METADATA_NAME);
            }
            if (voiceButtonContainer != null) voiceButtonContainer.setVisibility(View.VISIBLE);
            voiceButton.setVisibility(View.VISIBLE);
            if (voiceButtonProxy != null) {
                voiceButtonProxy.setVisibility(View.VISIBLE);
            }
            invalidatePressedFocusedStates(voiceButtonContainer, voiceButton);
            return true;
        } else {
            if (voiceButtonContainer != null) voiceButtonContainer.setVisibility(View.GONE);
            voiceButton.setVisibility(View.GONE);
            if (voiceButtonProxy != null) {
                voiceButtonProxy.setVisibility(View.GONE);
            }
            return false;
        }
    }

    private void updateVoiceSearchIcon(Drawable.ConstantState d) {
        final View voiceButtonContainer = findViewById(R.id.voice_button_container);
        final View voiceButton = findViewById(R.id.voice_button);
        updateButtonWithDrawable(R.id.voice_button, d);
        invalidatePressedFocusedStates(voiceButtonContainer, voiceButton);
    }

    /**
     * Sets the app market icon.
     */
    private void updateAppMarketIcon() {
        final View marketButton = findViewById(R.id.market_button);
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MARKET);
        // Find the app market activity by resolving an intent.
        // (If multiple app markets are installed, it will return the ResolverActivity.)
        ComponentName activityName = intent.resolveActivity(getPackageManager());
        if (activityName != null) {
            int coi = getCurrentOrientationIndexForGlobalIcons();
            mAppMarketIntent = intent;
            sAppMarketIcon[coi] = updateTextButtonWithIconFromExternalActivity(
                    R.id.market_button, activityName, R.drawable.ic_launcher_market_holo,
                    TOOLBAR_ICON_METADATA_NAME);
            marketButton.setVisibility(View.VISIBLE);
        } else {
            // We should hide and disable the view so that we don't try and restore the visibility
            // of it when we swap between drag & normal states from IconDropTarget subclasses.
            marketButton.setVisibility(View.GONE);
            marketButton.setEnabled(false);
        }
    }

    private void updateAppMarketIcon(Drawable.ConstantState d) {
        // Ensure that the new drawable we are creating has the approprate toolbar icon bounds
        Resources r = getResources();
        Drawable marketIconDrawable = d.newDrawable(r);
        int w = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_width);
        int h = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_height);
        marketIconDrawable.setBounds(0, 0, w, h);

        updateTextButtonWithDrawable(R.id.market_button, marketIconDrawable);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event, boolean result) {
        //final boolean result = super.dispatchPopulateAccessibilityEvent(event);
        final List<CharSequence> text = event.getText();
        text.clear();
        // Populate event with a fake title based on the current state.
        if (mState == State.APPS_CUSTOMIZE) {
            text.add(getString(R.string.all_apps_button_label));
        } else {
            text.add(getString(R.string.all_apps_home_button_label));
        }
        return result;
    }
    
    public void startBinding() {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "startBinding: this = " + this);
        }

        /// M: Cancel Drag when reload to avoid dragview lost parent and JE @{
        if (mDragController != null) {
            mDragController.cancelDrag();
        }
        /// M: }@

        final Workspace workspace = mWorkspace;

        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();
        mWorkspace.clearDropTargets();
        int count = workspace.getChildCount();
        for (int i = 0; i < count; i++) {
            // Use removeAllViewsInLayout() to avoid an extra requestLayout() and invalidate().
            final CellLayout layoutParent = (CellLayout) workspace.getChildAt(i);
            layoutParent.removeAllViewsInLayout();
            layoutParent.requestChildLayout();  
        }
        workspace.invalidate();
        mWidgetsToAdvance.clear();
        if (mHotseat != null) {
            mHotseat.resetLayout();
        }
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "startBinding: mIsLoadingWorkspace = " + mIsLoadingWorkspace);
        }
        mIsLoadingWorkspace = false;
    }

    /**
     * Bind the items start-end from the list.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindItems(List<ItemInfo> shortcuts, int start, int end) {
        setLoadOnResume();

        // Get the list of added shortcuts and intersect them with the set of shortcuts here
        Set<String> newApps = new HashSet<String>();
        newApps = getSharedPreferences().getStringSet(IGlobalStaticFunc.NEW_APPS_LIST_KEY, newApps);

        Workspace workspace = mWorkspace;
        for (int i = start; i < end; i++) {
            final ItemInfo item = shortcuts.get(i);
            if (Util.ENABLE_DEBUG) {
            	Util.Log.d(TAG, "bindItems: start = " + start + ", end = " + end 
                        + "item = " + item + ", this = " + this);
            }

            // Short circuit if we are loading dock items for a configuration which has no dock
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                    mHotseat == null) {
                continue;
            }

            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    ShortcutInfo info = (ShortcutInfo) item;
                    String uri = info.intent.toUri(0).toString();
                    View shortcut = createShortcut(info);
                    workspace.addInScreen(shortcut, item.container, item.screen, item.cellX,
                            item.cellY, 1, 1, false);
                    boolean animateIconUp = false;
                    synchronized (newApps) {
                        if (newApps.contains(uri)) {
                            animateIconUp = newApps.remove(uri);
                        }
                    }
                    if (animateIconUp) {
                        // Prepare the view to be animated up
                        shortcut.setAlpha(0f);
                        shortcut.setScaleX(0f);
                        shortcut.setScaleY(0f);
                        mNewShortcutAnimatePage = item.screen;
                        if (!mNewShortcutAnimateViews.contains(shortcut)) {
                            mNewShortcutAnimateViews.add(shortcut);
                        }
                    }
                    break;
                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                    FolderIcon newFolder = FolderIcon.fromXml(this,
                            (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()),
                            (FolderInfo) item);
                    workspace.addInScreen(newFolder, item.container, item.screen, item.cellX,
                            item.cellY, 1, 1, false);
                    break;
            }
        }

        workspace.requestLayout();
    }

   
    /**
     * Callback saying that there aren't any more items to bind.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void finishBindingItems() {
        super.finishBindingItems();


        // If we received the result of any pending adds while the loader was running (e.g. the
        // widget configuration forced an orientation change), process them now.
        for (int i = 0; i < sPendingAddList.size(); i++) {
            completeAdd(sPendingAddList.get(i));
        }
        sPendingAddList.clear();

        // Update the market app icon as necessary (the other icons will be managed in response to
        // package changes in bindSearchablesChanged()
        updateAppMarketIcon();

        // Animate up any icons as necessary
        if (mVisible || mWorkspaceLoading) {
            Runnable newAppsRunnable = new Runnable() {
                @Override
                public void run() {
                    runNewAppsAnimation(false);
                }
            };

            boolean willSnapPage = mNewShortcutAnimatePage > -1 &&
                    mNewShortcutAnimatePage != mWorkspace.getCurrentPage();
            if (canRunNewAppsAnimation()) {
                // If the user has not interacted recently, then either snap to the new page to show
                // the new-apps animation or just run them if they are to appear on the current page
                if (willSnapPage) {
                    mWorkspace.snapToPage(mNewShortcutAnimatePage, newAppsRunnable);
                } else {
                    runNewAppsAnimation(false);
                }
            } else {
                // If the user has interacted recently, then just add the items in place if they
                // are on another page (or just normally if they are added to the current page)
                runNewAppsAnimation(willSnapPage);
            }
        }

        mWorkspaceLoading = false;

        /// M: If unread information load completed, we need to bind it to workspace.        
        if (mUnreadLoadCompleted) {
            bindWorkspaceUnreadInfo();
        }
        mBindingWorkspaceFinished = true;
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Runs a new animation that scales up icons that were added while Launcher was in the
     * background.
     *
     * @param immediate whether to run the animation or show the results immediately
     */
    private void runNewAppsAnimation(boolean immediate) {
        AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        Collection<Animator> bounceAnims = new ArrayList<Animator>();

        // Order these new views spatially so that they animate in order
        Collections.sort(mNewShortcutAnimateViews, new Comparator<View>() {
            @Override
            public int compare(View a, View b) {
                CellLayout.LayoutParams alp = (CellLayout.LayoutParams) a.getLayoutParams();
                CellLayout.LayoutParams blp = (CellLayout.LayoutParams) b.getLayoutParams();
                int cellCountX = getSharedPrefSettingsManager().getWorkspaceCountCellX();
                return (alp.cellY * cellCountX + alp.cellX) - (blp.cellY * cellCountX + blp.cellX);
            }
        });

        // Animate each of the views in place (or show them immediately if requested)
        if (immediate) {
            for (View v : mNewShortcutAnimateViews) {
                v.setAlpha(1f);
                v.setScaleX(1f);
                v.setScaleY(1f);
            }
        } else {
            for (int i = 0; i < mNewShortcutAnimateViews.size(); ++i) {
                View v = mNewShortcutAnimateViews.get(i);
                ValueAnimator bounceAnim = LauncherAnimUtils.ofPropertyValuesHolder(v,
                        PropertyValuesHolder.ofFloat("alpha", 1f),
                        PropertyValuesHolder.ofFloat("scaleX", 1f),
                        PropertyValuesHolder.ofFloat("scaleY", 1f));
                bounceAnim.setDuration(IGlobalStaticFunc.NEW_SHORTCUT_BOUNCE_DURATION);
                bounceAnim.setStartDelay(i * IGlobalStaticFunc.NEW_SHORTCUT_STAGGER_DELAY);
                bounceAnim.setInterpolator(new SmoothPagedView.OvershootInterpolator());
                bounceAnims.add(bounceAnim);
            }
            anim.playTogether(bounceAnims);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mWorkspace != null) {
                        mWorkspace.postDelayed(mBuildLayersRunnable, 500);
                    }
                }
            });
            anim.start();
        }

        // Clean up
        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();
        new Thread("clearNewAppsThread") {
            public void run() {
            	getSharedPreferences().edit()
                            .putInt(IGlobalStaticFunc.NEW_APPS_PAGE_KEY, -1)
                            .putStringSet(IGlobalStaticFunc.NEW_APPS_LIST_KEY, null)
                            .commit();
            }
        }.start();
    }

    @Override
    public void bindSearchablesChanged() {
        boolean searchVisible = updateGlobalSearchIcon();
        boolean voiceVisible = updateVoiceSearchIcon(searchVisible);
        if (mSearchDropTargetBar != null) {
            mSearchDropTargetBar.onSearchPackagesChanged(searchVisible, voiceVisible);
        }
    }

    /**
     * Add the icons for all apps.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAllApplications(final List<ApplicationInfo> apps) {
//        if (Util.ENABLE_DEBUG) {
//        	Util.Log.d(TAG, "bindAllApplications: apps = " + apps);
//        }
        
    	Runnable setAllAppsRunnable = new Runnable() {
            public void run() {
                if (mAppsCustomizeContent != null) {
                	if(ConfigOption.QS_PRJ_NAME.startsWith("A802C_AOC")){
                		for(ApplicationInfo info : apps){
	        				if(MTK_APP_TODO_PACKAGE_NAME.equals(info.getPackageName())){
	        					apps.remove(info);
	        					break;
	        				}
        				}
                	}
                    mAppsCustomizeContent.setApps(apps);
                }
//                if(mAppsCustomizeContentWidget != null){
//                	mAppsCustomizeContentWidget.setApps(apps);
//                }
            }
        };

        /// M: If unread information load completed, we need to update information in app list.
        if (mUnreadLoadCompleted) {
            AppsCustomizePagedViewDefault.updateUnreadNumInAppInfo(apps);
        }
        // Remove the progress bar entirely; we could also make it GONE
        // but better to remove it since we know it's not going to be used
        View progressBar = mAppsCustomizeTabHost.
            findViewById(R.id.apps_customize_progress_bar);
        if (progressBar != null) {
            ((ViewGroup)progressBar.getParent()).removeView(progressBar);

            // We just post the call to setApps so the user sees the progress bar
            // disappear-- otherwise, it just looks like the progress bar froze
            // which doesn't look great
            mAppsCustomizeTabHost.post(setAllAppsRunnable);
        } else {
            // If we did not initialize the spinner in onCreate, then we can directly set the
            // list of applications without waiting for any progress bars views to be hidden.
            setAllAppsRunnable.run();
        }
        mBindingAppsFinished = true;
    }

    /**
     * A package was installed.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsAdded(List<ApplicationInfo> apps) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "bindAppsAdded: apps = " + apps);
        }
        super.bindAppsAdded(apps);

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.addApps(apps);
        }
//        if(mAppsCustomizeContentWidget != null){
//        	mAppsCustomizeContentWidget.addApps(apps);
//        }
    }

    /**
     * A package was updated.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsUpdated(List<ApplicationInfo> apps) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "bindAppsUpdated: apps = " + apps);
        }
        //android.util.Log.d("QsLog", "bindAppsUpdated(0)=="+apps.size());
        
        super.bindAppsUpdated(apps);
        
        //android.util.Log.d("QsLog", "bindAppsUpdated(1)==");
        
        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.updateApps(apps);
        }
//        if(mAppsCustomizeContentWidget != null){
//        	mAppsCustomizeContentWidget.updateApps(apps);
//        }
        //android.util.Log.d("QsLog", "bindAppsUpdated(2)==");
    }

    /**
     * A package was uninstalled.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsRemoved(List<String> packageNames, boolean permanent) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "bindAppsRemoved: packageNames = " + packageNames + ", permanent = " + permanent);
        }     
   
        if (permanent) {
            mWorkspace.removeItems(packageNames);
        }

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.removeApps(packageNames);
        }
//        if(mAppsCustomizeContentWidget != null){
//        	mAppsCustomizeContentWidget.removeApps(packageNames);
//        }

        // Notify the drag controller
        mDragController.onAppsRemoved(packageNames, this);
    }

    /**
     * A number of packages were updated.
     */
    public void bindPackagesUpdated() {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "bindPackagesUpdated.");
        }
        //android.util.Log.d("QsLog", "bindPackagesUpdated()==");
        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.onPackagesUpdated();
        }
//        if(mAppsCustomizeContentWidget != null){
//        	mAppsCustomizeContentWidget.onPackagesUpdated();
//        }
    }
    
    public Rect getCurrentBounds() {
        return mCurrentBounds;
    }
    
    public void bindComponentUnreadChanged(final ComponentName component, final int unreadNum) {
        if (Util.DEBUG_UNREAD) {
        	Util.Log.d(TAG, "bindComponentUnreadChanged: component = " + component
                    + ", unreadNum = " + unreadNum + ", this = " + this);
        }
        // Post to message queue to avoid possible ANR.
        mHandler.post(new Runnable() {
            public void run() {
                final long start = System.currentTimeMillis();
                if (Util.DEBUG_PERFORMANCE) {
                	Util.Log.d(TAG, "bindComponentUnreadChanged begin: component = " + component
                            + ", unreadNum = " + unreadNum + ", start = " + start);
                }
                if (mWorkspace != null) {
                    mWorkspace.updateComponentUnreadChanged(component, unreadNum);
                }

                if (mAppsCustomizeContent != null) {
                    mAppsCustomizeContent.updateAppsUnreadChanged(component, unreadNum);
                }
//                if(mAppsCustomizeContentWidget != null){
//                	mAppsCustomizeContentWidget.updateAppsUnreadChanged(component, unreadNum);
//                }
                if (Util.DEBUG_PERFORMANCE) {
                	Util.Log.d(TAG, "bindComponentUnreadChanged end: current time = "
                            + System.currentTimeMillis() + ", time used = "
                            + (System.currentTimeMillis() - start));
                }
            }
        });
    }
    
   /**
     * M: Bind shortcuts unread number if binding process has finished.
     */
    public void bindUnreadInfoIfNeeded() {
        if (Util.DEBUG_UNREAD) {
        	Util.Log.d(TAG, "bindUnreadInfoIfNeeded: mBindingWorkspaceFinished = "
                    + mBindingWorkspaceFinished + ", thread = " + Thread.currentThread());
        }
        if (mBindingWorkspaceFinished) {
            bindWorkspaceUnreadInfo();
        }
        
        if (mBindingAppsFinished) {
            bindAppsUnreadInfo();
        }
        mUnreadLoadCompleted = true;
    }
    
    /**
     * M: Bind unread number to shortcuts with data in MTKUnreadLoader.
     */
    private void bindWorkspaceUnreadInfo() {
        mHandler.post(new Runnable() {
            public void run() {
                final long start = System.currentTimeMillis();
                if (Util.DEBUG_PERFORMANCE) {
                	Util.Log.d(TAG, "bindWorkspaceUnreadInfo begin: start = " + start);
                }
                if (mWorkspace != null) {
                    mWorkspace.updateShortcutsAndFoldersUnread();
                }
                if (Util.DEBUG_PERFORMANCE) {
                	Util.Log.d(TAG, "bindWorkspaceUnreadInfo end: current time = "
                            + System.currentTimeMillis() + ",time used = "
                            + (System.currentTimeMillis() - start));
                }
            }
        });
    }
    
    /**
     * M: Bind unread number to shortcuts with data in MTKUnreadLoader.
     */
    private void bindAppsUnreadInfo() {
        mHandler.post(new Runnable() {
            public void run() {
                final long start = System.currentTimeMillis();
                if (Util.DEBUG_PERFORMANCE) {
                	Util.Log.d(TAG, "bindAppsUnreadInfo begin: start = " + start);
                }
                if (mAppsCustomizeContent != null) {
                    mAppsCustomizeContent.updateAppsUnread();
                }
//                if(mAppsCustomizeContentWidget != null){
//                	mAppsCustomizeContentWidget.updateAppsUnread();
//                }
                if (Util.DEBUG_PERFORMANCE) {
                	Util.Log.d(TAG, "bindAppsUnreadInfo end: current time = "
                            + System.currentTimeMillis() + ",time used = "
                            + (System.currentTimeMillis() - start));
                }
            }
        });
    }
    
    private Toast mLongPressWidgetToAddToast;
    /**
     * M: Show long press widget to add message, avoid duplication of message.
     */
    public void showLongPressWidgetToAddMessage() {
        if (mLongPressWidgetToAddToast == null) {
            mLongPressWidgetToAddToast = Toast.makeText(getApplicationContext(), R.string.long_press_widget_to_add,
                    Toast.LENGTH_SHORT);
        } else {
            mLongPressWidgetToAddToast.setText(R.string.long_press_widget_to_add);
            mLongPressWidgetToAddToast.setDuration(Toast.LENGTH_SHORT);
        }
        mLongPressWidgetToAddToast.show();
    }

    /**
     * M: Cancel long press widget to add message when press back key.
     */
    private void cancelLongPressWidgetToAddMessage() {
        if (mLongPressWidgetToAddToast != null) {
            mLongPressWidgetToAddToast.cancel();
        }
    }
    
    /**
     * M: A widget was uninstalled/disabled.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppWidgetRemoved(List<String> appWidget, boolean permanent) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "bindAppWidgetRemoved: appWidget = " + appWidget + ", permanent = " + permanent);
        }        
        if (permanent) {
            mWorkspace.removeItems(appWidget);
        }
    }

    /**
     * M: set orientation changed flag, this would make the apps customized pane
     * recreate views in certain condition.
     */
    public void notifyOrientationChanged() {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "notifyOrientationChanged: mOrientationChanged = "
                    + mOrientationChanged + ", mPaused = " + mPaused);
        }
        mOrientationChanged = true;
    }

    /**
     * M: tell Launcher that the pages in app customized pane were recreated.
     */
    public void notifyPagesWereRecreated() {
        mPagesWereRecreated = true;
    }

    /**
     * M: reset re-sync apps pages flags.
     */
    private void resetReSyncFlags() {
        mOrientationChanged = false;
        mPagesWereRecreated = false;
    }
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                Util.Log.d(TAG, "ACTION_SCREEN_OFF: mPendingAddInfo = " + mPendingAddInfo
                        + ", mAppsCustomizeTabHost = " + mAppsCustomizeTabHost + ", this = " + this);
                mUserPresent = false;
                mDragLayer.clearAllResizeFrames();
                updateRunning();

                // Reset AllApps to its initial state only if we are not in the middle of
                // processing a multi-step drop
                if (mAppsCustomizeTabHost != null && mPendingAddInfo.container == ItemInfo.NO_ID) {
                    mAppsCustomizeTabHost.reset();
                    showWorkspace(false);
                }
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mUserPresent = true;
                updateRunning();
//                android.util.Log.i("Jzs.QsLog", "ACTION_USER_PRESENT()===="+isKeyguardLocked()
//                        +"==isResumed:"+isResumed());
                if(isResumed())
                    enableGesture(true, false);
            } /*else if(QsIntent.ACTION_JZS_KEYGUARD_UNLOCKED.equals(action)) {
                android.util.Log.i("QsLog", "ACTION_KEYGUARD_UNLOCKED()===="+isKeyguardLocked()
                    +"==isResumed:"+isResumed());
                if(isResumed())
                    enableGesture(true, false);
            }*/
        }
    };
    
    protected boolean isSupportAnimateEffect(){
        return true;
    }
    
}
