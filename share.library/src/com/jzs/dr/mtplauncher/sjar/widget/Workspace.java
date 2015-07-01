package com.jzs.dr.mtplauncher.sjar.widget;


import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.graphics.Matrix;

import com.jzs.common.launcher.IGlobalStaticFunc;
import com.jzs.common.launcher.IIconCache;
import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.dr.mtplauncher.sjar.ctrl.Alarm;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragController;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragScroller;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget;
import com.jzs.dr.mtplauncher.sjar.ctrl.HolographicOutlineHelper;
import com.jzs.dr.mtplauncher.sjar.ctrl.IconKeyEventListener;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherTransitionable;
import com.jzs.dr.mtplauncher.sjar.ctrl.OnAlarmListener;
import com.jzs.dr.mtplauncher.sjar.ctrl.SpringLoadedDragController;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.FolderInfo;
import com.jzs.dr.mtplauncher.sjar.model.IconCache;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherAppWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherModel;
import com.jzs.dr.mtplauncher.sjar.model.LauncherSettings;
//import com.jzs.dr.mtplauncher.sjar.model.LauncherSettings.Favorites;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddWidgetInfo;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.widget.FolderIcon.FolderRingAnimator;


import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.LauncherApplication;
import com.jzs.dr.mtplauncher.sjar.R;

public abstract class Workspace extends SmoothPagedView
					implements DropTarget, DragSource, DragScroller, View.OnTouchListener,
					DragController.DragListener, LauncherTransitionable, ViewGroup.OnHierarchyChangeListener {
    
    private final static String TAG = "Workspace";
	// Relating to the animation of items being dropped externally
    public static final int ANIMATE_INTO_POSITION_AND_DISAPPEAR = 0;
    public static final int ANIMATE_INTO_POSITION_AND_REMAIN = 1;
    public static final int ANIMATE_INTO_POSITION_AND_RESIZE = 2;
    public static final int COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION = 3;
    public static final int CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION = 4;
    
    protected static final int BACKGROUND_FADE_OUT_DURATION = 350;
    protected static final int ADJACENT_SCREEN_DROP_DURATION = 300;
    protected static final int FLING_THRESHOLD_VELOCITY = 500;
    
    
	protected final WallpaperManager mWallpaperManager;
	public enum WallpaperVerticalOffset { TOP, MIDDLE, BOTTOM };
    protected int mWallpaperWidth;
    protected int mWallpaperHeight;
    protected WallpaperOffsetInterpolator mWallpaperOffset;
    protected boolean mUpdateWallpaperOffsetImmediately = false;
    protected Point mDisplaySize = new Point();
    protected int mWallpaperTravelWidth;
    protected static final float WALLPAPER_SCREENS_SPAN = 2f;
    protected IBinder mWindowToken;
    protected boolean mIsStaticWallpaper;
    
    protected SpringLoadedDragController mSpringLoadedDragController;
    protected float mSpringLoadedShrinkFactor;
    
    public enum State { NORMAL, SPRING_LOADED, SMALL, EDIT };
    protected State mState = State.NORMAL;
    protected boolean mIsSwitchingState = false;
    
    protected boolean mIsDragOccuring = false;
    protected boolean mChildrenLayersEnabled = true;
    
    /**
     * CellInfo for the cell that is currently being dragged
     */
    protected CellLayout.CellInfo mDragInfo;
    /**
     * Target drop area calculated during last acceptDrop call.
     */
    protected int[] mTargetCell = new int[2];
    protected int mDragOverX = -1;
    protected int mDragOverY = -1;
    /**
     * The CellLayout that is currently being dragged over
     */
    protected CellLayout mDragTargetLayout = null;
    /**
     * The CellLayout that we will show as glowing
     */
    protected CellLayout mDragOverlappingLayout = null;
    protected CellLayout mDropToLayout = null;
    
    
    protected static final int FOLDER_CREATION_TIMEOUT = 0;
    protected static final int REORDER_TIMEOUT = 250;
    protected FolderRingAnimator mDragFolderRingAnimator = null;
    protected boolean mAnimatingViewIntoPlace = false;
	protected boolean mCreateUserFolderOnDrop = false;
	protected boolean mAddToExistingFolderOnDrop = false;
	protected Alarm mFolderCreationAlarm;// = new Alarm();
	protected Alarm mReorderAlarm;// = new Alarm();
	protected FolderIcon mDragOverFolderIcon = null;
    
    protected float[] mDragViewVisualCenter = new float[2];
    /** Is the user is dragging an item near the edge of a page? */
    protected boolean mInScrollArea = false;
    
    protected Drawable mBackground;
    protected boolean mDrawBackground = false;
    protected float mBackgroundAlpha = 0;
    protected float mOverScrollMaxBackgroundAlpha = 0.0f;
    
    protected Runnable mDelayedSnapToPageRunnable;
    protected Runnable mDelayedResizeRunnable;

    protected float mWallpaperScrollRatio = 1.0f;
    
	public static final int DEFAULT_CELL_COUNT_X = 4;
	public static final int DEFAULT_CELL_COUNT_Y = 4;
	
	protected Launcher mLauncher;
    protected IIconCache mIconCache;
    protected DragController mDragController;
    protected DropTarget.DragEnforcer mDragEnforcer;
    
    protected boolean mWorkspaceFadeInAdjacentScreens;
    
	private int mDefaultPage;
	
	protected int mHotseatUnreadMarginRight = 0;
	protected int mWorkspaceUnreadMarginRight = 0;
	
	protected float mTransitionProgress;
	
	protected float mXDown;
	protected float mYDown;
	protected final static float START_DAMPING_TOUCH_SLOP_ANGLE = (float) Math.PI / 6;
	protected final static float MAX_SWIPE_ANGLE = (float) Math.PI / 3;
	protected final static float TOUCH_SLOP_DAMPING_FACTOR = 4;
	public static final int DRAG_BITMAP_PADDING = 2;
	
	protected int[] mTempCell = new int[2];
	protected int[] mTempEstimate = new int[2];
	protected HolographicOutlineHelper mOutlineHelper;// = new HolographicOutlineHelper();
	protected Bitmap mDragOutline = null;
	protected final Rect mTempRect = new Rect();
	protected final int[] mTempXY = new int[2];
	protected Matrix mTempInverseMatrix = new Matrix();
	protected float[] mTempDragCoordinates = new float[2];
	protected float[] mTempCellLayoutCenterCoordinates = new float[2];
	protected float[] mTempDragBottomRightCoordinates = new float[2];
    
	protected float mMaxDistanceForFolderCreation;
	
	// Related to dragging, folder creation and reordering
	public static final int DRAG_MODE_NONE = 0;
	public static final int DRAG_MODE_CREATE_FOLDER = 1;
	public static final int DRAG_MODE_ADD_TO_FOLDER = 2;
	public static final int DRAG_MODE_REORDER = 3;
	protected int mDragMode = DRAG_MODE_NONE;
	protected int mLastReorderX = -1;
	protected int mLastReorderY = -1;
	protected SparseArray<Parcelable> mSavedStates;
	protected final ArrayList<Integer> mRestoredPages = new ArrayList<Integer>();
	protected Runnable mBindPages;
	protected float mChildrenOutlineAlpha = 0;
	
	protected int mOutlineColor;
	
	private final int mMaxScreenCount;
    private final int mMinScreenCount;
    private final int mDefaultScreenCount;
    private final int mChildPageLayoutResource;
	
	public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     * @param defStyle Unused.
     */
    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContentIsRefreshable = false;
        mLauncher = (Launcher) context;
        
        // With workspace, data is available straight from the get-go
        setDataIsReady();
        
        mWallpaperManager = WallpaperManager.getInstance(context);
        
        final ISharedPrefSettingsManager sharePref = mLauncher.getSharedPrefSettingsManager();
        
        int cellCountX = DEFAULT_CELL_COUNT_X;
        int cellCountY = DEFAULT_CELL_COUNT_Y;

        TypedArray a = context.obtainStyledAttributes(attrs,
                						R.styleable.Workspace, defStyle, 0);
        
        cellCountX = a.getInt(R.styleable.Workspace_cellCountX, cellCountX);
        cellCountY = a.getInt(R.styleable.Workspace_cellCountY, cellCountY);
        mDefaultPage = sharePref.getWorkspaceDefaultScreen(a.getInt(R.styleable.Workspace_defaultScreen, -1));
        mOutlineColor = a.getColor(R.styleable.Workspace_outlineColor, getResources().getColor(android.R.color.holo_blue_light));
        
        mChildPageLayoutResource = a.getResourceId(R.styleable.Workspace_childPageLayout, 0);
        
        mMaxScreenCount = a.getInt(R.styleable.Workspace_maxScreenCount, sharePref.getWorkspaceScreenMaxCount());
        int defMinCount = sharePref.getWorkspaceScreenMinCount();
        mMinScreenCount = Math.max(defMinCount, a.getInt(R.styleable.Workspace_minScreenCount, defMinCount));
        
        
    	final int nChildCount = sharePref.getWorkspaceScreenCount();//.getInt(Launcher.QS_WORKSPACE_SCREEN_COUNT, 0);
		if(nChildCount < mMinScreenCount || nChildCount > mMaxScreenCount)
			mDefaultScreenCount = a.getInt(R.styleable.Workspace_defaultScreenCount, 5);
		else
			mDefaultScreenCount = nChildCount;
		
        a.recycle();
        
        setSupportCycleSlidingScreen(sharePref.getWorkspaceSupportCycleSliding());
        setEnableStaticWallpaper(sharePref.getEnableStaticWallpaper(), false);
        setScrollingIndicatorEnabled(sharePref.getWorkspaceEnableScreenIndicatorBar());
        
        updateLayoutCustomSettingsChanged(false, null);
        
        //mLauncher.getModel().updateWorkspaceLayoutCells(cellCountX, cellCountY);
        setHapticFeedbackEnabled(false);
                       
     // Disable multitouch across the workspace/all apps/customize tray
        setMotionEventSplittingEnabled(true);
        
        initWorkspace();
    }
    
    protected void initWorkspace() {

    	Context context = getContext();
    	
    	mFolderCreationAlarm = new Alarm();
    	mReorderAlarm = new Alarm();
    	
    	mDragEnforcer = new DropTarget.DragEnforcer(context);
    	mOutlineHelper = new HolographicOutlineHelper();
        
        mCurrentPage = getDefaultScreenIndex();
        Launcher.setScreen(mCurrentPage);
//        LauncherApplication app = (LauncherApplication)context.getApplicationContext();
        mIconCache = mLauncher.getIconCache();
        setWillNotDraw(false);
        setChildrenDrawnWithCacheEnabled(true);
        IResConfigManager res = mLauncher.getResConfigManager();
        
        mWorkspaceFadeInAdjacentScreens = res.getBoolean(IResConfigManager.CONFIG_WORKSPACE_FADEADJACENTSCREENS);
        //R.bool.config_workspaceFadeAdjacentScreens);
        
        mSpringLoadedShrinkFactor =
                res.getInteger(IResConfigManager.CONFIG_WORKSPACE_LOADSHRINKPERCENT) / 100.0f;
        		//R.integer.config_workspaceSpringLoadShrinkPercentage
        
      /// M: added for unread feature, initialize the value of the mUnreadMarginRight.
        mHotseatUnreadMarginRight = (int)res.getDimension(IResConfigManager.DIM_HOTSEAT_UNREAD_MARGIN_RIGHT);//R.dimen.hotseat_unread_margin_right);
        mWorkspaceUnreadMarginRight = (int)res.getDimension(IResConfigManager.DIM_WORKSPACE_UNREAD_MARGIN_RIGHT);//R.dimen.workspace_unread_margin_right);
        

        mWallpaperOffset = new WallpaperOffsetInterpolator();
        Display display = mLauncher.getWindowManager().getDefaultDisplay();
        display.getSize(mDisplaySize);
        mWallpaperTravelWidth = (int) (mDisplaySize.x *
                wallpaperTravelToScreenWidthRatio(mDisplaySize.x, mDisplaySize.y));

        mMaxDistanceForFolderCreation = (0.7f * res.getDimensionPixelSize(IResConfigManager.DIM_APP_ICON_SIZE));//R.dimen.app_icon_size));
        mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * mDensity);
        
        mBindPages = new Runnable() {
            @Override
            public void run() {
                mLauncher.getModel().bindRemainingSynchronousPages();
            }
        };
        
        //android.util.Log.i("QsLog", "Workspace::Workspace()====count:"+super.getChildCount());
    }
    
    @Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
//		android.util.Log.i("QsLog", "Workspace::onFinishInflate()====count:"+super.getChildCount()
//				+"==defcount:"+getDefaultScreenCount()
//				+"==mChildPageLayoutResource:"+Integer.toHexString(mChildPageLayoutResource));
		
		if(mChildPageLayoutResource == 0 && getChildCount() == 0){
			throw new RuntimeException("Jzs.Workspace attr childPageLayout cat't be empty..");
    	}

		if(getDefaultScreenCount() != getChildCount()){
			final LayoutInflater inflater = LayoutInflater.from(getContext());
			for(int i=getChildCount(); i<getDefaultScreenCount(); i++){
	        	addScreen(inflater);
	        }
		}
	}

	public int getMaxScreenCount(){
    	return mMaxScreenCount;
    }
    
    public int getMinScreenCount(){
    	return mMinScreenCount;
    }
    
    public int getDefaultScreenCount(){
    	return mDefaultScreenCount;
    }

    public int getDefaultScreenIndex(){
    	if(mDefaultPage < 0){
    		if(getPageCount() == 0){
    			return (int)getDefaultScreenCount()/2;
    		}
    		return (int)getPageCount()/2;
    	}
    	
    	return Math.max(0, Math.min(mDefaultPage, getPageCount() - 1));
    }
    
    public boolean setDefaultScreenIndex(int def){
    	if(def != mDefaultPage){
    		mDefaultPage = def;
    		final ISharedPrefSettingsManager sharePref = mLauncher.getSharedPrefSettingsManager();
    		sharePref.setWorkspaceDefaultScreen(def);
    		return true;
    	}
    	
    	return false;
    }

    public CellLayout getNormalCellLayout(){
    	return (CellLayout)getPageAt(0);
    }
    
    public boolean isSupportEditPageScreen(){
    	return mChildPageLayoutResource > 0;
    }
    
    public void updateLayoutCustomSettingsChanged(boolean requestLayout, final Runnable onCompleteRunnable){
    	final ISharedPrefSettingsManager sharePref = mLauncher.getSharedPrefSettingsManager();
    	IResConfigManager res = mLauncher.getResConfigManager();
        if(res.isLandscape()){
        	int paddingRight = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_RIGHT_PADDING_LAND);//getPaddingRight();
	        if(!isScrollingIndicatorEnabled()){
	        	paddingRight -= res.getDimensionPixelSize(IResConfigManager.DIM_BUTTON_BAR_SCREENINDICATOR_HEIGHT);
	        }
	        if(!sharePref.getWorkspaceShowHotseatBar()){
	        	paddingRight -= res.getDimensionPixelSize(IResConfigManager.DIM_BUTTON_BAR_HOTSEAT_HEIGHT);
	        }
	        if(paddingRight != getPaddingRight()){
	        	super.setPadding(super.getPaddingLeft(), super.getPaddingTop(), paddingRight, getPaddingBottom());
	        }
        } else {
	        int paddingBottom = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_BOTTOM_PADDING_PORT);//getPaddingBottom();
	        if(!isScrollingIndicatorEnabled()){
	        	paddingBottom -= res.getDimensionPixelSize(IResConfigManager.DIM_BUTTON_BAR_SCREENINDICATOR_HEIGHT);
	        }
	        if(!sharePref.getWorkspaceShowHotseatBar()){
	        	paddingBottom -= res.getDimensionPixelSize(IResConfigManager.DIM_BUTTON_BAR_HOTSEAT_HEIGHT);
	        }
	        if(paddingBottom != getPaddingBottom()){
	        	super.setPadding(super.getPaddingLeft(), super.getPaddingTop(), super.getPaddingRight(), paddingBottom);
	        }
        }
        
        if(requestLayout)
        	super.requestLayout();
        
        if(onCompleteRunnable != null)
        	onCompleteRunnable.run();
    }
    
    public View addScreen(LayoutInflater inflater){
    	return addScreen(inflater, -1, false);
    }
    
    public View addScreen(boolean changeDefautlSize){
    	return addScreen(LayoutInflater.from(mLauncher), -1, changeDefautlSize);
    }
    
    public View addScreen(LayoutInflater inflater, boolean changeDefautlSize){
    	return addScreen(inflater, -1, changeDefautlSize);
    }
    
    public View addScreen(LayoutInflater inflater, int index, boolean changeDefautlSize){
    	
    	if(getChildCount() >= getMaxScreenCount())
    		return null;

    	View child = inflater.inflate(mChildPageLayoutResource, this, false);

    	if(index < 0)
    		super.addView(child);
    	else
    		super.addView(child, index);

    	if(changeDefautlSize){
    		post(new Runnable() {
                public void run() {
		    		ISharedPrefSettingsManager sharePref = mLauncher.getSharedPrefSettingsManager();
		    		sharePref.setWorkspaceScreenCount(getChildCount());
                }
    		});
    	}

    	return child;
    }
    
    public boolean removeScreen(int index){
    	return removeScreen(index, false);
    }
    
    public boolean removeScreen(final int index, final boolean changeDefautlSize){

    	int curCount = getPageCount();
    	if(index >= 0 && index < curCount && curCount > getMinScreenCount()){
    		//return removeScreen(super.getChildAt(index));
    		final View view = getChildAt(index);
    		
    		super.removeView(view);
    		
    		post(new Runnable() {
                public void run() {
                	
                	removeChildWidgets((CellLayout)view);
                	int count = getPageCount();
                	
                	if(changeDefautlSize){
                		ISharedPrefSettingsManager sharePref = mLauncher.getSharedPrefSettingsManager();
                		sharePref.setWorkspaceScreenCount(count);
                	}

                	for(int i=index; i<count; i++){
                		updateChildWidgetsScreenIndex((CellLayout)getChildAt(i), i);
                	}
                }
    		});
    		
    		
    		return true;
    	}
    	return false;
    }
    
    public boolean removeScreen(View view){
    	if(getPageCount() > getMinScreenCount()){
    		removeChildWidgets((CellLayout)view);
    		super.removeViewInLayout(view);
    		return true;
    	}
    	return false;
    }
    
    protected void removeChildWidgets(CellLayout view){
    	ShortcutAndWidgetContainer cell = view.getShortcutsAndWidgets();
    	if(cell != null){
    		int nCount = cell.getChildCount();
    		for(int i=0; i<nCount; i++){
    			View widget = cell.getChildAt(i);
    			removeChildWidgets((ItemInfo) widget.getTag());
    		}
    	}
    }
    
    protected void removeChildWidgets(ItemInfo item){
    	if(item == null)
    		return;
    	
    	if (item instanceof ShortcutInfo) {
    		mLauncher.getModel().deleteItemFromDatabase(item);
            
        } else if (item instanceof FolderInfo) {
        	
            // Remove the folder from the workspace and delete the contents from launcher model
            FolderInfo folderInfo = (FolderInfo) item;
            mLauncher.removeFolder(folderInfo);
            mLauncher.getModel().deleteFolderContentsFromDatabase(folderInfo);
            
        } else if (item instanceof LauncherAppWidgetInfo) {
            // Remove the widget from the workspace
            mLauncher.removeAppWidget((LauncherAppWidgetInfo) item);
            mLauncher.getModel().deleteItemFromDatabase(item);

            final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) item;
            final LauncherAppWidgetHost appWidgetHost = mLauncher.getAppWidgetHost();
            if (appWidgetHost != null) {
                // Deleting an app widget ID is a void call but writes to disk before returning
                // to the caller...
                new Thread("deleteAppWidgetId") {
                    public void run() {
                        appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
                    }
                }.start();
            }
        }
    }
    
    protected void updateChildWidgetsScreenIndex(CellLayout view, int newScreenIndex){
//    	if(view.isAttrsLocked())
//    		return;
//    	
    	ShortcutAndWidgetContainer cell = view.getShortcutsAndWidgets();
    	if(cell != null){
    		int nCount = cell.getChildCount();
    		for(int i=0; i<nCount; i++){
    			View widget = cell.getChildAt(i);
    			final ItemInfo item = (ItemInfo) widget.getTag();
    			//android.util.Log.i("QsLog", "==newScreenIndex:"+newScreenIndex+"==screen:"+item.screen+"==item:"+item.toString());
    			mLauncher.getModel().moveItemInDatabase(item, item.container, newScreenIndex, item.cellX, item.cellY);
    		}
    	}
    }
    
 // estimate the size of a widget with spans hSpan, vSpan. return MAX_VALUE for each
    // dimension if unsuccessful
    public int[] estimateItemSize(int hSpan, int vSpan,
            ItemInfo itemInfo, boolean springLoaded) {
        int[] size = new int[2];
        if (getPageCount() > 0) {
            CellLayout cl = (CellLayout) getPageAt(0);
            Rect r = estimateItemPosition(cl, itemInfo, 0, 0, hSpan, vSpan);
            size[0] = r.width();
            size[1] = r.height();
            if (springLoaded) {
                size[0] *= mSpringLoadedShrinkFactor;
                size[1] *= mSpringLoadedShrinkFactor;
            }
            return size;
        } else {
            size[0] = Integer.MAX_VALUE;
            size[1] = Integer.MAX_VALUE;
            return size;
        }
    }
    public Rect estimateItemPosition(CellLayout cl, ItemInfo pendingInfo,
            int hCell, int vCell, int hSpan, int vSpan) {
        Rect r = new Rect();
        cl.cellToRect(hCell, vCell, hSpan, vSpan, r);
        return r;
    }

    public void onDragStart(DragSource source, Object info, int dragAction) {
        if (Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "onDragStart: source = " + source + ", info = " + info + ", dragAction = " + dragAction);
        }

        mIsDragOccuring = true;
        updateChildrenLayersEnabled(false);
        mLauncher.lockScreenOrientation();
        setChildrenBackgroundAlphaMultipliers(1f);
        // Prevent any Un/InstallShortcutReceivers from updating the db while we are dragging
        mLauncher.getInstallShortcutReceiver().enableInstallQueue();
        mLauncher.getInstallShortcutReceiver().enableUninstallQueue();
    }

    public void onDragEnd() {
        if (Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "onDragEnd: mIsDragOccuring = " + mIsDragOccuring);
        }

        mIsDragOccuring = false;
        updateChildrenLayersEnabled(false);
        mLauncher.unlockScreenOrientation(false);

        // Re-enable any Un/InstallShortcutReceiver and now process any queued items
        mLauncher.getInstallShortcutReceiver().disableAndFlushInstallQueue();
        //InstallShortcutReceiver.disableAndFlushInstallQueue(getContext());
        mLauncher.getInstallShortcutReceiver().disableAndFlushUninstallQueue();
    }
    
    
    @Override
    protected int getScrollMode() {
        return SmoothPagedView.X_LARGE_MODE;
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
    	super.onChildViewAdded(parent, child);
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        CellLayout cl = ((CellLayout) child);
        cl.setOnInterceptTouchListener(this);
        cl.setClickable(true);
        if(mLongClickListener != null){
            cl.setOnLongClickListener(mLongClickListener);
        }
//        cl.setContentDescription(getContext().getString(
//                R.string.workspace_description_format, getChildCount()));
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
    	super.onChildViewRemoved(parent, child);
    }

    protected boolean shouldDrawChild(View child) {
        final CellLayout cl = (CellLayout) child;
        return super.shouldDrawChild(child) &&
            (cl.getShortcutsAndWidgets().getAlpha() > 0 ||
             cl.getBackgroundAlpha() > 0);
    }

    /**
     * @return The open folder on the current screen, or null if there is none
     */
    public Folder getOpenFolder() {
        DragLayer dragLayer = mLauncher.getDragLayer();
        int count = dragLayer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = dragLayer.getChildAt(i);
            if (child instanceof Folder) {
                Folder folder = (Folder) child;
                if (folder.getInfo().opened)
                    return folder;
            }
        }
        return null;
    }

    public boolean isTouchActive() {
        return mTouchState != TOUCH_STATE_REST;
    }

    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child The child to add in one of the workspace's screens.
     * @param screen The screen in which to add the child.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     */
    public void addInScreen(View child, long container, int screen, int x, int y, int spanX, int spanY) {
        addInScreen(child, container, screen, x, y, spanX, spanY, false);
    }

    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child The child to add in one of the workspace's screens.
     * @param screen The screen in which to add the child.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     * @param insert When true, the child is inserted at the beginning of the children list.
     */
    public void addInScreen(View child, long container, int screen, int x, int y, int spanX, int spanY,
            boolean insert) {
        if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            if (screen < 0 || screen >= getPageCount()) {
                Util.Log.e(TAG, "The screen must be >= 0 and < " + getChildCount()
                    + " (was " + screen + "); skipping child");
                return;
            }
        }

        final CellLayout layout;
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            layout = mLauncher.getHotseat().getLayout();
            child.setOnKeyListener(null);

            // Hide folder title in the hotseat
            /// M: added for unread feature, set different margin right for the folder and the shortcut.
            if (child instanceof FolderIcon) {
                //if(((FolderIcon) child).isSupportPreviewChild())
                ((FolderIcon) child).setTextVisible(false);
                ((FolderIcon) child).setFolderUnreadMarginRight(mHotseatUnreadMarginRight);
            } else if (child instanceof MtpShortcutView) {
                ((MtpShortcutView) child).setShortcutUnreadMarginRight(mHotseatUnreadMarginRight);
            }

            if (screen < 0) {
                screen = mLauncher.getHotseat().getOrderInHotseat(x, y);
            } else {
                // Note: We do this to ensure that the hotseat is always laid out in the orientation
                // of the hotseat in order regardless of which orientation they were added
                x = mLauncher.getHotseat().getCellXFromOrder(screen);
                y = mLauncher.getHotseat().getCellYFromOrder(screen);
            }
        } else {
            // Show folder title if not in the hotseat
            if (child instanceof FolderIcon) {
                ((FolderIcon) child).setTextVisible(true);
            }

            layout = (CellLayout) getPageAt(screen);
            child.setOnKeyListener(new IconKeyEventListener());
            
            if (child instanceof FolderIcon) {
                ((FolderIcon) child).setFolderUnreadMarginRight(mWorkspaceUnreadMarginRight);
            } else if (child instanceof MtpShortcutView) {
                ((MtpShortcutView) child).setShortcutUnreadMarginRight(mWorkspaceUnreadMarginRight);
            }
        }

        LayoutParams genericLp = child.getLayoutParams();
        CellLayout.LayoutParams lp;
        if (genericLp == null || !(genericLp instanceof CellLayout.LayoutParams)) {
            lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
        } else {
            lp = (CellLayout.LayoutParams) genericLp;
            lp.cellX = x;
            lp.cellY = y;
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
        }

        if (spanX < 0 && spanY < 0) {
            lp.isLockedToGrid = false;
        }

        // Get the canonical child id to uniquely represent this view in this screen
        int childId = -1;
        /**
         * M: Fix IllegalArgumentException when onRestoreInstanceState called in
         * finishBindingItems. There is saved state in appwidget host
         * view(android.appwidget.AppWidgetHostView$ParcelableSparseArray), but
         * shortcut has no saved state, use default. 
         * 
         * Launcher use the child position to generate view id, for example, the id of 
         * the first child in the center cell of workspace will be 0x9c020000, 
         * whatever it is a shortcut or widget. 
         * 
         * When user add a widget in the center workspace, then remove the package of 
         * this widget in settings, install a shortcut to the center workspace,
         * the shortcut will has the same id as the previous moved widget.
         * 
         * Make different id for widget and shortcut to distinguish this.
         */
        if (child instanceof AppWidgetHostView
                && container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            childId = LauncherModel.getCellLayoutChildId(container + 1, screen, x, y, spanX, spanY);
        } else {
            childId = LauncherModel.getCellLayoutChildId(container, screen, x, y, spanX, spanY);
        }
        
        boolean markCellsAsOccupied = !(child instanceof Folder);
        if (!layout.addViewToCellLayout(child, insert ? 0 : -1, childId, lp, markCellsAsOccupied)) {
            // TODO: This branch occurs when the workspace is adding views
            // outside of the defined grid
            // maybe we should be deleting these items from the LauncherModel?
            Util.Log.w(TAG, "Failed to add to item at (" + lp.cellX + "," + lp.cellY + ") to CellLayout");
        }

        if (!(child instanceof Folder)) {
            child.setHapticFeedbackEnabled(false);
            child.setOnLongClickListener(mLongClickListener);
        }
        if (child instanceof DropTarget) {
            mDragController.addDropTarget((DropTarget) child);
        }
    }

    /**
     * Check if the point (x, y) hits a given page.
     */
    protected boolean hitsPage(int index, float x, float y) {
        final View page = getPageAt(index);
        if (page != null) {
            float[] localXY = { x, y };
            mapPointFromSelfToChild(page, localXY);
            return (localXY[0] >= 0 && localXY[0] < page.getWidth()
                    && localXY[1] >= 0 && localXY[1] < page.getHeight());
        }
        return false;
    }

    @Override
    protected boolean hitsPreviousPage(float x, float y) {
        // mNextPage is set to INVALID_PAGE whenever we are stationary.
        // Calculating "next page" this way ensures that you scroll to whatever page you tap on
        final int current = (mNextPage == INVALID_PAGE) ? mCurrentPage : mNextPage;

        // Only allow tap to next page on large devices, where there's significant margin outside
        // the active workspace
        return Launcher.isScreenLarge() && hitsPage(current - 1, x, y);
    }

    @Override
    protected boolean hitsNextPage(float x, float y) {
        // mNextPage is set to INVALID_PAGE whenever we are stationary.
        // Calculating "next page" this way ensures that you scroll to whatever page you tap on
        final int current = (mNextPage == INVALID_PAGE) ? mCurrentPage : mNextPage;

        // Only allow tap to next page on large devices, where there's significant margin outside
        // the active workspace
        return Launcher.isScreenLarge() && hitsPage(current + 1, x, y);
    }

    /**
     * Called directly from a CellLayout (not by the framework), after we've been added as a
     * listener via setOnInterceptTouchEventListener(). This allows us to tell the CellLayout
     * that it should intercept touch events, which is not something that is normally supported.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (Util.DEBUG_MOTION) {
            Util.Log.d(TAG, "onTouch: v = " + v + ", event = " + event + ", isFinishedSwitchingState() = "
                    + isFinishedSwitchingState() + ", mState = " + mState + ", mScrollX = " + mScrollX);
        }
        return (isSmall() || !isFinishedSwitchingState());
    }

    public boolean isSwitchingState() {
        return mIsSwitchingState;
    }

    /** This differs from isSwitchingState in that we take into account how far the transition
     *  has completed. */
    public boolean isFinishedSwitchingState() {
        return !mIsSwitchingState || (mTransitionProgress > 0.5f);
    }

    protected void onWindowVisibilityChanged(int visibility) {
        mLauncher.onWindowVisibilityChanged(visibility);
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (isSmall() || !isFinishedSwitchingState()) {
            // when the home screens are shrunken, shouldn't allow side-scrolling
            return false;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (Util.DEBUG_MOTION) {
            Util.Log.d(TAG, "onInterceptTouchEvent: ev = " + ev + ", mScrollX = " + mScrollX
                    +", mTouchState = " + mTouchState);
        }
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            mXDown = ev.getX();
            mYDown = ev.getY();
            break;
        case MotionEvent.ACTION_POINTER_UP:
        case MotionEvent.ACTION_UP:
            if (mTouchState == TOUCH_STATE_REST) {
                final CellLayout currentPage = (CellLayout) getPageAt(mCurrentPage);
                if (!currentPage.lastDownOnOccupiedCell()) {
                    onWallpaperTap(ev);
                }
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

//    public void reinflateWidgetsIfNecessary() {
//        reinflateWidgetsIfNecessary(false);
//    }
    
    public void reinflateWidgetsIfNecessary() {
        final int clCount = getPageCount();
        for (int i = 0; i < clCount; i++) {
            CellLayout cl = (CellLayout) getPageAt(i);
            ShortcutAndWidgetContainer swc = cl.getShortcutsAndWidgets();
            final int itemCount = swc.getChildCount();
            for (int j = 0; j < itemCount; j++) {
                View v = swc.getChildAt(j);

                if (v.getTag() instanceof LauncherAppWidgetInfo) {
                    LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
                    LauncherAppWidgetHostView lahv = (LauncherAppWidgetHostView) info.hostView;
                    if (lahv != null && lahv.orientationChangedSincedInflation()) {
                        mLauncher.removeAppWidget(info);
                        // Remove the current widget which is inflated with the wrong orientation
                        cl.removeView(lahv);
                        mLauncher.bindAppWidget(info);
                    }
                }
            }
        }
    }

    @Override
    protected void determineScrollingStart(MotionEvent ev) {
        if (isSmall()) return;
        if (!isFinishedSwitchingState()) return;

        float deltaX = Math.abs(ev.getX() - mXDown);
        float deltaY = Math.abs(ev.getY() - mYDown);

        if (Float.compare(deltaX, 0f) == 0) return;

        float slope = deltaY / deltaX;
        float theta = (float) Math.atan(slope);

        if (deltaX > mTouchSlop || deltaY > mTouchSlop) {
            cancelCurrentPageLongPress();
        }

        if (theta > MAX_SWIPE_ANGLE) {
            // Above MAX_SWIPE_ANGLE, we don't want to ever start scrolling the workspace
            return;
        } else if (theta > START_DAMPING_TOUCH_SLOP_ANGLE) {
            // Above START_DAMPING_TOUCH_SLOP_ANGLE and below MAX_SWIPE_ANGLE, we want to
            // increase the touch slop to make it harder to begin scrolling the workspace. This
            // results in vertically scrolling widgets to more easily. The higher the angle, the
            // more we increase touch slop.
            theta -= START_DAMPING_TOUCH_SLOP_ANGLE;
            float extraRatio = (float)
                    Math.sqrt((theta / (MAX_SWIPE_ANGLE - START_DAMPING_TOUCH_SLOP_ANGLE)));
            super.determineScrollingStart(ev, 1 + TOUCH_SLOP_DAMPING_FACTOR * extraRatio);
        } else {
            // Below START_DAMPING_TOUCH_SLOP_ANGLE, we don't do anything special
            super.determineScrollingStart(ev);
        }
    }

    @Override
    protected void onPageBeginMoving() {
        super.onPageBeginMoving();
        if(Util.DEBUG_ANIM){
	        Util.Log.e(TAG, "onPageBeginMoving() "
	                +", page:"+getCurrentPage()
	                +", next:"+getNextPage()
	                +", EffectType:"+getAnimateEffectType()
	                +", drag:"+mDragMode
	                +", mState:"+mState
	                +", TouchState:"+mTouchState
	                +", mAnimateState:"+mAnimateState
	                +", HardwareAcc:"+isHardwareAccelerated());
        }
        
        if(false){
            
            int size = getPageCount();
            for(int i=0; i<size; i++){
                final View view = getPageAt(i);
                Util.Log.e("QsLog", "onPageBeginMoving()"
                        +", i:"+i
                        +", PivotX:"+view.getPivotX()
                        +", PivotY:"+view.getPivotY()
                        +", TransX:"+view.getTranslationX()
                        +", TransY:"+view.getTranslationY()
                        +", Rot:"+view.getRotation()
                        +", RotX:"+view.getRotationX()
                        +", RotY:"+view.getRotationY()
                        +", ScaleX:"+view.getScaleX()
                        +", ScaleY:"+view.getScaleY()
                        +", mwidth:"+view.getMeasuredWidth()
                        +", mheight:"+view.getMeasuredHeight()
                        +", width:"+view.getWidth()
                        +", height:"+view.getHeight());
            }
        }
        
        

        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else {
            if (mNextPage != INVALID_PAGE) {
                // we're snapping to a particular screen
                enableChildrenCache(mCurrentPage, mNextPage);
            } else {
                // this is when user is actively dragging a particular screen, they might
                // swipe it either left or right (but we won't advance by more than one screen)
                enableChildrenCache(mCurrentPage - 1, mCurrentPage + 1);
            }
        }

        // Only show page outlines as we pan if we are on large screen
        if (Launcher.isScreenLarge()) {
            showOutlines();
            //mIsStaticWallpaper = mWallpaperManager.getWallpaperInfo() == null;
        }

        // If we are not fading in adjacent screens, we still need to restore the alpha in case the
        // user scrolls while we are transitioning (should not affect dispatchDraw optimizations)
        if (!mWorkspaceFadeInAdjacentScreens) {
            for (int i = 0; i < getPageCount(); ++i) {
                ((CellLayout) getPageAt(i)).setShortcutAndWidgetAlpha(1f);
            }
        }

        // Show the scroll indicator as you pan the page
        //showScrollingIndicator(false);
    }

    protected void onPageEndMoving() {
        super.onPageEndMoving();
        if(Util.DEBUG_ANIM){
			Util.Log.e(TAG, "onPageEndMoving()"
                +", page:"+getCurrentPage()
                +", EffectType:"+getAnimateEffectType()
                +", drag:"+mDragMode
                +", mState:"+mState
                +", TouchState:"+mTouchState
                +", mAnimateState:"+mAnimateState);
        }
        if(mState == State.NORMAL 
                && mAnimateState != AnimateState.IDLE){
            int size = getPageCount();
            for(int i=0; i<size; i++){
				if(false){
	                final View view = getPageAt(i);
	                android.util.Log.i("QsLog", "onPageEndMoving()"
	                        +", i:"+i
	                        +", PivotX:"+view.getPivotX()
	                        +", PivotY:"+view.getPivotY()
	                        +", TransX:"+view.getTranslationX()
	                        +", TransY:"+view.getTranslationY()
	                        +", Rot:"+view.getRotation()
	                        +", RotX:"+view.getRotationX()
	                        +", RotY:"+view.getRotationY()
	                        +", ScaleX:"+view.getScaleX()
	                        +", ScaleY:"+view.getScaleY());
	                animateReset(view);
				} else {
					animateReset(getPageAt(i));
				}
            }            
        }

        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else {
            clearChildrenCache();
        }

        if (mDragController.isDragging()) {
            if (isSmall()) {
                // If we are in springloaded mode, then force an event to check if the current touch
                // is under a new page (to scroll to)
                mDragController.forceMoveEvent();
            }
        } else {
            // If we are not mid-dragging, hide the page outlines if we are on a large screen
            if (Launcher.isScreenLarge()) {
                hideOutlines();
            }

            // Hide the scroll indicator as you pan the page
//            if (!mDragController.isDragging()) {
//                hideScrollingIndicator(false);
//            }
        }
        mOverScrollMaxBackgroundAlpha = 0.0f;

        if (mDelayedResizeRunnable != null) {
            mDelayedResizeRunnable.run();
            mDelayedResizeRunnable = null;
        }

        if (mDelayedSnapToPageRunnable != null) {
            mDelayedSnapToPageRunnable.run();
            mDelayedSnapToPageRunnable = null;
        }
    }

    @Override
    protected void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
        Launcher.setScreen(mCurrentPage);
    };
    
 // As a ratio of screen height, the total distance we want the parallax effect to span
    // horizontally
    protected float wallpaperTravelToScreenWidthRatio(int width, int height) {
        float aspectRatio = width / (float) height;

        // At an aspect ratio of 16/10, the wallpaper parallax effect should span 1.5 * screen width
        // At an aspect ratio of 10/16, the wallpaper parallax effect should span 1.2 * screen width
        // We will use these two data points to extrapolate how much the wallpaper parallax effect
        // to span (ie travel) at any aspect ratio:

        final float ASPECT_RATIO_LANDSCAPE = 16/10f;
        final float ASPECT_RATIO_PORTRAIT = 10/16f;
        final float WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE = 1.5f;
        final float WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT = 1.2f;

        // To find out the desired width at different aspect ratios, we use the following two
        // formulas, where the coefficient on x is the aspect ratio (width/height):
        //   (16/10)x + y = 1.5
        //   (10/16)x + y = 1.2
        // We solve for x and y and end up with a final formula:
        final float x =
            (WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE - WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT) /
            (ASPECT_RATIO_LANDSCAPE - ASPECT_RATIO_PORTRAIT);
        final float y = WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT - x * ASPECT_RATIO_PORTRAIT;
        return x * aspectRatio + y;
    }
    
    // The range of scroll values for Workspace
    protected int getScrollRange() {
        return getChildOffset(getPageCount() - 1) - getChildOffset(0);
    }

    protected void setWallpaperDimension() {
        Point minDims = new Point();
        Point maxDims = new Point();
        mLauncher.getWindowManager().getDefaultDisplay().getCurrentSizeRange(minDims, maxDims);

        final int maxDim = Math.max(maxDims.x, maxDims.y);
        final int minDim = Math.min(minDims.x, minDims.y);

        // We need to ensure that there is enough extra space in the wallpaper for the intended
        // parallax effects
        if (Launcher.isScreenLarge()) {
            mWallpaperWidth = (int) (maxDim * wallpaperTravelToScreenWidthRatio(maxDim, minDim));
            mWallpaperHeight = maxDim;
        } else {
            mWallpaperWidth = Math.min((int) (minDim * WALLPAPER_SCREENS_SPAN), maxDim);
            mWallpaperHeight = maxDim;
        }
        new Thread("setWallpaperDimension") {
            public void run() {
                mWallpaperManager.suggestDesiredDimensions(mWallpaperWidth, mWallpaperHeight);
            }
        }.start();
    }

    protected float wallpaperOffsetForCurrentScroll() {
        // Set wallpaper offset steps (1 / (number of screens - 1))
        mWallpaperManager.setWallpaperOffsetSteps(1.0f / (getPageCount() - 1), 1.0f);

        // For the purposes of computing the scrollRange and overScrollOffset, we assume
        // that mLayoutScale is 1. This means that when we're in spring-loaded mode,
        // there's no discrepancy between the wallpaper offset for a given page.
        float layoutScale = mLayoutScale;
        mLayoutScale = 1f;
        int scrollRange = getScrollRange();
        int scrollX = getScrollX();
    	/// M: modify to cycle sliding screen.
		if (isSupportCycleSlidingScreen()) {
			if (scrollX > mMaxScrollX) {
				int offset = scrollX - mMaxScrollX;
				scrollX = (int) ((getPageCount() - 1) * getWidth() * (1 - ((float) offset)	/ getWidth()));
			} else if (scrollX < 0) {
				scrollX = (getPageCount() - 1) * (-scrollX);
			}
		}
        // Again, we adjust the wallpaper offset to be consistent between values of mLayoutScale
        float adjustedScrollX = Math.max(0, Math.min(getScrollX(), mMaxScrollX));
        adjustedScrollX *= mWallpaperScrollRatio;
        mLayoutScale = layoutScale;

        float scrollProgress =
            adjustedScrollX / (float) scrollRange;

        if (mIsStaticWallpaper || Launcher.isScreenLarge()) {
            // The wallpaper travel width is how far, from left to right, the wallpaper will move
            // at this orientation. On tablets in portrait mode we don't move all the way to the
            // edges of the wallpaper, or otherwise the parallax effect would be too strong.
            int wallpaperTravelWidth = Math.min(mWallpaperTravelWidth, mWallpaperWidth);

            float offsetInDips = wallpaperTravelWidth * scrollProgress +
                (mWallpaperWidth - wallpaperTravelWidth) / 2; // center it
            float offset = offsetInDips / (float) mWallpaperWidth;
            return offset;
        } else {
            return scrollProgress;
        }
    }

    public void syncWallpaperOffsetWithScroll() {
        final boolean enableWallpaperEffects = isHardwareAccelerated();
        if (enableWallpaperEffects) {
            mWallpaperOffset.setFinalX(wallpaperOffsetForCurrentScroll());
        }
    }

    public void updateWallpaperOffsetImmediately() {
        mUpdateWallpaperOffsetImmediately = true;
    }

    public void updateWallpaperOffsets() {
        boolean updateNow = false;
        boolean keepUpdating = true;
        if (mUpdateWallpaperOffsetImmediately) {
            updateNow = true;
            keepUpdating = false;
            mWallpaperOffset.jumpToFinal();
            mUpdateWallpaperOffsetImmediately = false;
        } else {
            updateNow = keepUpdating = !mIsStaticWallpaper && mWallpaperOffset.computeScrollOffset();
        }
        if (!mIsStaticWallpaper && updateNow) {
            if (mWindowToken != null) {
                mWallpaperManager.setWallpaperOffsets(mWindowToken,
                        mWallpaperOffset.getCurrX(), mWallpaperOffset.getCurrY());
            }
        }
        if (keepUpdating) {
            invalidate();
        }
    }

    @Override
    public void updateCurrentPageScroll() {
        super.updateCurrentPageScroll();
        computeWallpaperScrollRatio(mCurrentPage);
    }

    @Override
    public void snapToPage(int whichPage) {
        super.snapToPage(whichPage);
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "snapToPage: whichPage = " + whichPage + ", mScrollX = " + getScrollX());
        }
        computeWallpaperScrollRatio(whichPage);
    }

    @Override
    public void snapToPage(int whichPage, int duration) {
        super.snapToPage(whichPage, duration);
        computeWallpaperScrollRatio(whichPage);
    }

    public void snapToPage(int whichPage, Runnable r) {
        if (mDelayedSnapToPageRunnable != null) {
            mDelayedSnapToPageRunnable.run();
        }
        mDelayedSnapToPageRunnable = r;
        snapToPage(whichPage, SLOW_PAGE_SNAP_ANIMATION_DURATION);
    }

    protected void computeWallpaperScrollRatio(int page) {
        // Here, we determine what the desired scroll would be with and without a layout scale,
        // and compute a ratio between the two. This allows us to adjust the wallpaper offset
        // as though there is no layout scale.
        float layoutScale = mLayoutScale;
        int scaled = getChildOffset(page) - getRelativeChildOffset(page);
        mLayoutScale = 1.0f;
        float unscaled = getChildOffset(page) - getRelativeChildOffset(page);
        mLayoutScale = layoutScale;
        if (scaled > 0) {
            mWallpaperScrollRatio = (1.0f * unscaled) / scaled;
        } else {
            mWallpaperScrollRatio = 1f;
        }
    }
    
    @Override
    public void computeScroll() {
        super.computeScroll();
        syncWallpaperOffsetWithScroll();
    }
    
    public void showOutlines(){
    	
    }
    
    public void hideOutlines() {
    	
    }
    
    public void showOutlinesTemporarily() {
        if (!mIsPageMoving && !isTouchActive()) {
            snapToPage(mCurrentPage);
        }
    }

    public void setChildrenOutlineAlpha(float alpha) {
        mChildrenOutlineAlpha = alpha;
        for (int i = 0; i < getPageCount(); i++) {
            CellLayout cl = (CellLayout) getPageAt(i);
            cl.setBackgroundAlpha(alpha);
        }
    }

    public float getChildrenOutlineAlpha() {
        return mChildrenOutlineAlpha;
    }

    public void disableBackground() {
        mDrawBackground = false;
    }
    public void enableBackground() {
        mDrawBackground = true;
    }
    
    public void setBackgroundAlpha(float alpha) {
        if (alpha != mBackgroundAlpha) {
            mBackgroundAlpha = alpha;
            invalidate();
        }
    }

    public float getBackgroundAlpha() {
        return mBackgroundAlpha;
    }

    public float backgroundAlphaInterpolator(float r) {
        float pivotA = 0.1f;
        float pivotB = 0.4f;
        if (r < pivotA) {
            return 0;
        } else if (r > pivotB) {
            return 1.0f;
        } else {
            return (r - pivotA)/(pivotB - pivotA);
        }
    }

    public float overScrollBackgroundAlphaInterpolator(float r) {
        float threshold = 0.08f;

        if (r > mOverScrollMaxBackgroundAlpha) {
            mOverScrollMaxBackgroundAlpha = r;
        } else if (r < mOverScrollMaxBackgroundAlpha) {
            r = mOverScrollMaxBackgroundAlpha;
        }

        return Math.min(r / threshold, 1.0f);
    }

    protected void updatePageAlphaValues(int screenCenter) {
        boolean isInOverscroll = mOverScrollX < 0 || mOverScrollX > mMaxScrollX;
        
        if (Util.DEBUG_ANIM) {
            Util.Log.i(TAG, "updatePageAlphaValues(end) = isInOverscroll:"+isInOverscroll
                    +"==SwitchingState:"+mIsSwitchingState
                    +"==mState:"+mState
                    +"==FadeIn:"+mWorkspaceFadeInAdjacentScreens);
        }
        
        if (mWorkspaceFadeInAdjacentScreens &&
                mState == State.NORMAL &&
                !mIsSwitchingState &&
                !isInOverscroll) {
            for (int i = 0; i < getPageCount(); i++) {
                CellLayout child = (CellLayout) getPageAt(i);
                if (child != null) {
                    float scrollProgress = getScrollProgress(screenCenter, child, i);
                    float alpha = 1 - Math.abs(scrollProgress);
                    child.getShortcutsAndWidgets().setAlpha(alpha);
                    if (!mIsDragOccuring) {
                        child.setBackgroundAlphaMultiplier(
                                backgroundAlphaInterpolator(Math.abs(scrollProgress)));
                    } else {
                        child.setBackgroundAlphaMultiplier(1f);
                    }
                }
            }
        }
    }

    protected void setChildrenBackgroundAlphaMultipliers(float a) {
        for (int i = 0; i < getPageCount(); i++) {
            CellLayout child = (CellLayout) getPageAt(i);
            child.setBackgroundAlphaMultiplier(a);
        }
    }

    @Override
    protected void screenScrolled(int screenCenter) {
        super.screenScrolled(screenCenter);
        if (Util.DEBUG_ANIM) {
            Util.Log.i(TAG, "screenScrolled() = screenCenter:"+screenCenter
                    +"==mChildrenLayersEnabled:"+mChildrenLayersEnabled);
        }
        //updatePageAlphaValues(screenCenter);
        //enableHwLayersOnVisiblePages();

//        if (mOverScrollX < 0 || mOverScrollX > mMaxScrollX) {
//            int index = mOverScrollX < 0 ? 0 : getPageCount() - 1;
//            CellLayout cl = (CellLayout) getPageAt(index);
//            float scrollProgress = getScrollProgress(screenCenter, cl, index);
//            cl.setOverScrollAmount(Math.abs(scrollProgress), index == 0);
//            float rotation = - WORKSPACE_OVERSCROLL_ROTATION * scrollProgress;
//            cl.setRotationY(rotation);
//            setFadeForOverScroll(Math.abs(scrollProgress));
//            if (!mOverscrollTransformsSet) {
//                mOverscrollTransformsSet = true;
//                cl.setCameraDistance(mDensity * mCameraDistance);
//                cl.setPivotX(cl.getMeasuredWidth() * (index == 0 ? 0.75f : 0.25f));
//                cl.setPivotY(cl.getMeasuredHeight() * 0.5f);
//                cl.setOverscrollTransformsDirty(true);
//            }
//        } else {
//            if (mOverscrollFade != 0) {
//                setFadeForOverScroll(0);
//            }
//            if (mOverscrollTransformsSet) {
//                mOverscrollTransformsSet = false;
//                ((CellLayout) getChildAt(0)).resetOverscrollTransforms();
//                ((CellLayout) getChildAt(getChildCount() - 1)).resetOverscrollTransforms();
//            }
//        }
    }

    @Override
    protected void overScroll(float amount) {
        acceleratedOverScroll(amount);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWindowToken = getWindowToken();
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onAttachedToWindow: mWindowToken = " + mWindowToken);
        }
        computeScroll();
        mDragController.setWindowToken(mWindowToken);
    }

    protected void onDetachedFromWindow() {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onDetachedFromWindow: mWindowToken = " + mWindowToken);
        }
        mWindowToken = null;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mFirstLayout && mCurrentPage >= 0 && mCurrentPage < getChildCount()) {
            mUpdateWallpaperOffsetImmediately = true;
        }
        super.onLayout(changed, left, top, right, bottom);

        if (Util.DEBUG_LAYOUT) {
            Util.Log.d(TAG, "onLayout: changed = " + changed + ", left = " + left
                    + ", top = " + top + ", right = " + right + ", bottom = " + bottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (Util.DEBUG_DRAW) {
            Util.Log.v(TAG, "onDraw(start) = "+this);
        }
        updateWallpaperOffsets();

        // Draw the background gradient if necessary
        if (mBackground != null && mBackgroundAlpha > 0.0f && mDrawBackground) {
            int alpha = (int) (mBackgroundAlpha * 255);
            mBackground.setAlpha(alpha);
            mBackground.setBounds(getScrollX(), 0, getScrollX() + getMeasuredWidth(),
                    getMeasuredHeight());
            mBackground.draw(canvas);
        }

        super.onDraw(canvas);

        // Call back to LauncherModel to finish binding after the first draw
        post(mBindPages);
        if (Util.DEBUG_DRAW) {
            Util.Log.v(TAG, "onDraw(end) = "+this);
        }
    }

    public boolean isDrawingBackgroundGradient() {
        return (mBackground != null && mBackgroundAlpha > 0.0f && mDrawBackground);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (!mLauncher.isAllAppsVisible()) {
            final Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                return openFolder.requestFocus(direction, previouslyFocusedRect);
            } else {
                return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
            }
        }
        return false;
    }

    @Override
    public int getDescendantFocusability() {
        if (isSmall()) {
            return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
        }
        return super.getDescendantFocusability();
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (!mLauncher.isAllAppsVisible()) {
            final Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                openFolder.addFocusables(views, direction);
            } else {
                super.addFocusables(views, direction, focusableMode);
            }
        }
    }
    
    public boolean isSmall() {
        return mState == State.SMALL || mState == State.SPRING_LOADED;
    }

    public void enableChildrenCache(int fromPage, int toPage) {
        if (fromPage > toPage) {
            final int temp = fromPage;
            fromPage = toPage;
            toPage = temp;
        }

        final int screenCount = getPageCount();
        if(isSupportCycleSlidingScreen()){
        	if(fromPage < 0 || toPage >= screenCount){
        		fromPage = 0;
                toPage = screenCount - 1;
        	}
        }
        fromPage = Math.max(fromPage, 0);
        toPage = Math.min(toPage, screenCount - 1);

        for (int i = fromPage; i <= toPage; i++) {
            final CellLayout layout = (CellLayout) getPageAt(i);
            layout.setChildrenDrawnWithCacheEnabled(true);
            layout.setChildrenDrawingCacheEnabled(true);
        }
    }

    public void clearChildrenCache() {
        final int screenCount = getPageCount();
        for (int i = 0; i < screenCount; i++) {
            final CellLayout layout = (CellLayout) getPageAt(i);
            layout.setChildrenDrawnWithCacheEnabled(false);
            // In software mode, we don't want the items to continue to be drawn into bitmaps
            if (!isHardwareAccelerated()) {
                layout.setChildrenDrawingCacheEnabled(false);
            }
        }
    }
    
    protected boolean isEnableChildrenLayers(boolean force){
    	boolean small = mState == State.SMALL || mIsSwitchingState;
        return force || small || mAnimatingViewIntoPlace || isPageMoving();
    }

    protected void updateChildrenLayersEnabled(boolean force) {
        boolean enableChildrenLayers = isEnableChildrenLayers(force);// || small || mAnimatingViewIntoPlace || isPageMoving();
        if (Util.DEBUG_ANIM) {
            Util.Log.d(TAG, "updateChildrenLayersEnabled(start) = new:"+enableChildrenLayers
                    +"==old:"+mChildrenLayersEnabled
                    +"==force:"+force);
        }
        if (enableChildrenLayers != mChildrenLayersEnabled) {
            mChildrenLayersEnabled = enableChildrenLayers;
            if (mChildrenLayersEnabled) {
                enableHwLayersOnVisiblePages();
            } else {
                for (int i = 0; i < getPageCount(); i++) {
                    final CellLayout cl = (CellLayout) getPageAt(i);
                    cl.disableHardwareLayers();
                }
            }
        }
    }

    protected void enableHwLayersOnVisiblePages() {
		if (mChildrenLayersEnabled) {
			final int screenCount = getPageCount();
			getVisiblePages(mTempVisiblePagesRange);
			int leftScreen = mTempVisiblePagesRange[0]-1;
			int rightScreen = mTempVisiblePagesRange[1]+1;
			
			if (Util.DEBUG_ANIM) {
	            Util.Log.w(TAG, "enableHwLayersOnVisiblePages(1) = leftScreen:"+leftScreen
	                    +"==rightScreen:"+rightScreen);
	        }
			
			if(true){				
				if (leftScreen > rightScreen) {
		            int temp = leftScreen;
		            leftScreen = rightScreen;
		            rightScreen = temp;
		        }

		        if(isSupportCycleSlidingScreen()){
		        	if(leftScreen < 0 || rightScreen >= screenCount){
		        		leftScreen = 0;
		        		rightScreen = screenCount - 1;
		        	}
		        }
		        leftScreen = Math.max(leftScreen, 0);
		        rightScreen = Math.min(rightScreen, screenCount - 1);
		        
		        if (Util.DEBUG_ANIM) {
		            Util.Log.w(TAG, "enableHwLayersOnVisiblePages(2) = leftScreen:"+leftScreen
		                    +"==rightScreen:"+rightScreen);
		        }

		        for (int i = 0; i < screenCount; i++) {
		            final CellLayout layout = (CellLayout) getPageAt(i);
		            if(i < leftScreen || i > rightScreen)
		            	layout.disableHardwareLayers();
		            else
		            	layout.enableHardwareLayers();
		        }
		        
		        return;
			}
			if (leftScreen == rightScreen) {
				// make sure we're caching at least two pages always
				if (rightScreen < screenCount - 1) {
					rightScreen++;
				} else if (leftScreen > 0) {
					leftScreen--;
				}
			}
			if (Util.DEBUG_ANIM) {
	            Util.Log.w(TAG, "enableHwLayersOnVisiblePages(2) = leftScreen:"+leftScreen
	                    +"==rightScreen:"+rightScreen);
	        }
			
//			for (int i = 0; i < screenCount; i++) {
//				final CellLayout layout = (CellLayout) getPageAt(i);
//				if (!(leftScreen <= i && i <= rightScreen && shouldDrawChild(layout))) {
//					layout.disableHardwareLayers();
//				}
//			}
			for (int i = 0; i < screenCount; i++) {
				final CellLayout layout = (CellLayout) getPageAt(i);
				if (leftScreen <= i && i <= rightScreen) {
					layout.enableHardwareLayers();
				} else {
					layout.disableHardwareLayers();
				}
			}
		}
	}

    public void buildPageHardwareLayers() {
        // force layers to be enabled just for the call to buildLayer
        if (Util.DEBUG_ANIM) {
            Util.Log.i(TAG, "buildPageHardwareLayers(start) = mChildrenLayersEnabled:"+mChildrenLayersEnabled
                    +","+getWindowToken());
        }
        
        updateChildrenLayersEnabled(true);
        if (getWindowToken() != null) {
            final int childCount = getPageCount();
            for (int i = 0; i < childCount; i++) {
                CellLayout cl = (CellLayout) getPageAt(i);
                cl.buildHardwareLayer();
            }
        }
        updateChildrenLayersEnabled(false);
        
        if (Util.DEBUG_ANIM) {
            Util.Log.i(TAG, "buildPageHardwareLayers(end) = mChildrenLayersEnabled:"+mChildrenLayersEnabled
                    +","+this);
        }
    }

    protected void onWallpaperTap(MotionEvent ev) {
        final int[] position = mTempCell;
        getLocationOnScreen(position);

        int pointerIndex = ev.getActionIndex();
        position[0] += (int) ev.getX(pointerIndex);
        position[1] += (int) ev.getY(pointerIndex);

        mWallpaperManager.sendWallpaperCommand(getWindowToken(),
                ev.getAction() == MotionEvent.ACTION_UP
                        ? WallpaperManager.COMMAND_TAP : WallpaperManager.COMMAND_SECONDARY_TAP,
                position[0], position[1], 0, null);
    }
    
    /*
    *
    * We call these methods (onDragStartedWithItemSpans/onDragStartedWithSize) whenever we
    * start a drag in Launcher, regardless of whether the drag has ever entered the Workspace
    *
    * These methods mark the appropriate pages as accepting drops (which alters their visual
    * appearance).
    *
    */
    public void onDragStartedWithItem(View v) {
        if (Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "onDragStartedWithItem: v = " + v);
        }
        final Canvas canvas = new Canvas();

        // The outline is used to visualize where the item will land if dropped
        mDragOutline = createDragOutline(v, canvas, DRAG_BITMAP_PADDING);
    }

    public void onDragStartedWithItem(PendingAddItemInfo info, Bitmap b, boolean clipAlpha) {
        final Canvas canvas = new Canvas();

        int[] size = estimateItemSize(info.spanX, info.spanY, info, false);

        // The outline is used to visualize where the item will land if dropped
        mDragOutline = createDragOutline(b, canvas, DRAG_BITMAP_PADDING, size[0],
                size[1], clipAlpha);
    }
    
    public void exitWidgetResizeMode() {
        DragLayer dragLayer = mLauncher.getDragLayer();
        dragLayer.clearAllResizeFrames();
    }
    
    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        if (Util.DEBUG_ANIM) {
            Util.Log.d(TAG, "onLauncherTransitionPrepare: animated = " + animated+"==toWorkspace:"+toWorkspace);
        }
        mIsSwitchingState = true;
        cancelScrollingIndicatorAnimations();
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
        mTransitionProgress = t;
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        if (Util.DEBUG_ANIM) {
            Util.Log.d(TAG, "onLauncherTransitionEnd: animated = " + animated+"==toWorkspace:"+toWorkspace);
        }
        mIsSwitchingState = false;
        mWallpaperOffset.setOverrideHorizontalCatchupConstant(false);
        updateChildrenLayersEnabled(false);
        // The code in getChangeStateAnimation to determine initialAlpha and finalAlpha will ensure
        // ensure that only the current page is visible during (and subsequently, after) the
        // transition animation.  If fade adjacent pages is disabled, then re-enable the page
        // visibility after the transition animation.
        if (!mWorkspaceFadeInAdjacentScreens) {
            for (int i = 0; i < getPageCount(); i++) {
                final CellLayout cl = (CellLayout) getPageAt(i);
                cl.setShortcutAndWidgetAlpha(1f);
            }
        }
    }

    @Override
    public View getContent() {
        return this;
    }
    
    /**
     * Draw the View v into the given Canvas.
     *
     * @param v the view to draw
     * @param destCanvas the canvas to draw on
     * @param padding the horizontal and vertical padding to use when drawing
     */
    protected void drawDragView(View v, Canvas destCanvas, int padding, boolean pruneToDrawable) {
        final Rect clipRect = mTempRect;
        v.getDrawingRect(clipRect);

        boolean textVisible = false;

        destCanvas.save();
        if (v instanceof TextView && pruneToDrawable) {
            Drawable d = ((TextView) v).getCompoundDrawables()[1];
            clipRect.set(0, 0, d.getIntrinsicWidth() + padding, d.getIntrinsicHeight() + padding);
            destCanvas.translate(padding / 2, padding / 2);
            d.draw(destCanvas);
        } else {
            if (v instanceof FolderIcon) {
                // For FolderIcons the text can bleed into the icon area, and so we need to
                // hide the text completely (which can't be achieved by clipping).
                if (((FolderIcon) v).isContainerPreviewImageView() && ((FolderIcon) v).getTextVisible()) {
                    ((FolderIcon) v).setTextVisible(false);
                    textVisible = true;
                }
            } else if (v instanceof BubbleTextView) {
                final BubbleTextView tv = (BubbleTextView) v;
                clipRect.bottom = tv.getExtendedPaddingTop() - (int) BubbleTextView.PADDING_V +
                        tv.getLayout().getLineTop(0);
            } else if (v instanceof TextView) {
                final TextView tv = (TextView) v;
                clipRect.bottom = tv.getExtendedPaddingTop() - tv.getCompoundDrawablePadding() +
                        tv.getLayout().getLineTop(0);
            }
            destCanvas.translate(-v.getScrollX() + padding / 2, -v.getScrollY() + padding / 2);
            destCanvas.clipRect(clipRect, Op.REPLACE);
            v.draw(destCanvas);

            // Restore text visibility of FolderIcon if necessary
            if (textVisible) {
                ((FolderIcon) v).setTextVisible(true);
            }
        }
        destCanvas.restore();
    }

    /**
     * Returns a new bitmap to show when the given View is being dragged around.
     * Responsibility for the bitmap is transferred to the caller.
     */
    public Bitmap createDragBitmap(View v, Canvas canvas, int padding) {
        Bitmap b;

        if (v instanceof TextView) {
            Drawable d = ((TextView) v).getCompoundDrawables()[1];
            b = Bitmap.createBitmap(d.getIntrinsicWidth() + padding,
                    d.getIntrinsicHeight() + padding, Bitmap.Config.ARGB_8888);
        } else {
            b = Bitmap.createBitmap(
                    v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);
        }

        canvas.setBitmap(b);
        drawDragView(v, canvas, padding, true);
        canvas.setBitmap(null);

        return b;
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    protected Bitmap createDragOutline(View v, Canvas canvas, int padding) {
        /// M: added for theme feature, get different outline color for different themes.
        final int outlineColor = mOutlineColor;//getResources().getColor(android.R.color.holo_blue_light);

        final Bitmap b = Bitmap.createBitmap(
                v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);

        canvas.setBitmap(b);
        drawDragView(v, canvas, padding, true);
        mOutlineHelper.applyMediumExpensiveOutlineWithBlur(b, canvas, outlineColor, outlineColor);
        canvas.setBitmap(null);
        return b;
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    protected Bitmap createDragOutline(Bitmap orig, Canvas canvas, int padding, int w, int h,
            boolean clipAlpha) {
        /// M: added for theme feature, get different outline color for different themes.
        final int outlineColor = mOutlineColor;//getResources().getColor(android.R.color.holo_blue_light);
        
        final Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(b);

        Rect src = new Rect(0, 0, orig.getWidth(), orig.getHeight());
        float scaleFactor = Math.min((w - padding) / (float) orig.getWidth(),
                (h - padding) / (float) orig.getHeight());
        int scaledWidth = (int) (scaleFactor * orig.getWidth());
        int scaledHeight = (int) (scaleFactor * orig.getHeight());
        Rect dst = new Rect(0, 0, scaledWidth, scaledHeight);

        // center the image
        dst.offset((w - scaledWidth) / 2, (h - scaledHeight) / 2);

        canvas.drawBitmap(orig, src, dst, null);
        mOutlineHelper.applyMediumExpensiveOutlineWithBlur(b, canvas, outlineColor, outlineColor,
                clipAlpha);
        canvas.setBitmap(null);

        return b;
    }

    public void startDrag(CellLayout.CellInfo cellInfo) {
        View child = cellInfo.cell;
        if (Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "startDrag cellInfo = " + cellInfo + ",child = " + child);
        }

        /// M: Abnormal case, if user long press on all apps button and then
        /// long press on other shortcuts in hotseat, the dragInfo will be
        /// null, exception will happen, so need return directly.
        if (child != null && child.getTag() == null) {
            Util.Log.d(TAG, "Abnormal start drag: cellInfo = " + cellInfo + ",child = " + child);
            return;
        }
        
        // Make sure the drag was started by a long press as opposed to a long click.
        if (!child.isInTouchMode()) {
            if (Util.ENABLE_DEBUG) {
                Util.Log.i(TAG, "The child " + child + " is not in touch mode.");
            }
            return;
        }

        mDragInfo = cellInfo;
        child.setVisibility(INVISIBLE);
        CellLayout layout = (CellLayout) child.getParent().getParent();
        layout.prepareChildForDrag(child);

        child.clearFocus();
        child.setPressed(false);

        final Canvas canvas = new Canvas();

        // The outline is used to visualize where the item will land if dropped
        mDragOutline = createDragOutline(child, canvas, DRAG_BITMAP_PADDING);
        beginDragShared(child, this);
    }

    public void beginDragShared(View child, DragSource source) {
    	IResConfigManager r = mLauncher.getResConfigManager();//getResources();

        // The drag bitmap follows the touch point around on the screen
        final Bitmap b = createDragBitmap(child, new Canvas(), DRAG_BITMAP_PADDING);

        final int bmpWidth = b.getWidth();
        final int bmpHeight = b.getHeight();

        float scale = mLauncher.getDragLayer().getLocationInDragLayer(child, mTempXY);
        int dragLayerX =
                Math.round(mTempXY[0] - (bmpWidth - scale * child.getWidth()) / 2);
        int dragLayerY =
                Math.round(mTempXY[1] - (bmpHeight - scale * bmpHeight) / 2
                        - DRAG_BITMAP_PADDING / 2);
       if (Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "beginDragShared: child = " + child + ", source = " + source
                    + ", dragLayerX = " + dragLayerX + ", dragLayerY = " + dragLayerY);
        }

        Point dragVisualizeOffset = null;
        Rect dragRect = null;
        if (child instanceof BubbleTextView || child instanceof PagedViewIcon) {
            int iconSize = r.getDimensionPixelSize(IResConfigManager.DIM_APP_ICON_SIZE);//R.dimen.app_icon_size);
            int iconPaddingTop = r.getDimensionPixelSize(IResConfigManager.DIM_APP_ICON_PADDING_TOP);//R.dimen.app_icon_padding_top);
            int top = child.getPaddingTop();
            int left = (bmpWidth - iconSize) / 2;
            int right = left + iconSize;
            int bottom = top + iconSize;
            dragLayerY += top;
            // Note: The drag region is used to calculate drag layer offsets, but the
            // dragVisualizeOffset in addition to the dragRect (the size) to position the outline.
            dragVisualizeOffset = new Point(-DRAG_BITMAP_PADDING / 2,
                    iconPaddingTop - DRAG_BITMAP_PADDING / 2);
            dragRect = new Rect(left, top, right, bottom);
        } else if (child instanceof FolderIcon) {
            int previewSize = r.getDimensionPixelSize(IResConfigManager.DIM_FOLDER_ICON_PREVIEW_SIZE);//R.dimen.folder_preview_size);
            dragRect = new Rect(0, 0, child.getWidth(), previewSize);
        }

        // Clear the pressed state if necessary
        if (child instanceof BubbleTextView) {
            BubbleTextView icon = (BubbleTextView) child;
            icon.clearPressedOrFocusedBackground();
        }

        mDragController.startDrag(b, dragLayerX, dragLayerY, source, child.getTag(),
                DragController.DRAG_ACTION_MOVE, dragVisualizeOffset, dragRect, scale);
        b.recycle();

        
    }

    public void addApplicationShortcut(ShortcutInfo info, CellLayout target, long container, int screen,
            int cellX, int cellY, boolean insertAtFirst, int intersectX, int intersectY) {
        View view = mLauncher.createShortcut(target, (ShortcutInfo) info);

        final int[] cellXY = new int[2];
        target.findCellForSpanThatIntersects(cellXY, 1, 1, intersectX, intersectY);
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "addApplicationShortcut: info = " + info + ", view = "
                    + view + ", container = " + container + ", screen = " + screen
                    + ", cellXY[0] = " + cellXY[0] + ", cellXY[1] = " + cellXY[1]
                    + ", insertAtFirst = " + insertAtFirst);
        }

        addInScreen(view, container, screen, cellXY[0], cellXY[1], 1, 1, insertAtFirst);
        mLauncher.getModel().addOrMoveItemInDatabase(info, container, screen, cellXY[0],
                cellXY[1]);
    }

    public boolean transitionStateShouldAllowDrop() {
        return ((!isSwitchingState() || mTransitionProgress > 0.5f) && mState != State.SMALL);
    }

    /**
     * {@inheritDoc}
     */
    public boolean acceptDrop(DragObject d) {
        // If it's an external drop (e.g. from All Apps), check if it should be accepted
        CellLayout dropTargetLayout = mDropToLayout;
        if (d.dragSource != this) {
            // Don't accept the drop if we're not over a screen at time of drop
            if (dropTargetLayout == null) {
                return false;
            }
            if (!transitionStateShouldAllowDrop()) return false;

            mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset,
                    d.dragView, mDragViewVisualCenter);

            // We want the point to be mapped to the dragTarget.
            if (mLauncher.isHotseatLayout(dropTargetLayout)) {
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(dropTargetLayout, mDragViewVisualCenter, null);
            }

            int spanX = 1;
            int spanY = 1;
            if (mDragInfo != null) {
                final CellLayout.CellInfo dragCellInfo = mDragInfo;
                spanX = dragCellInfo.spanX;
                spanY = dragCellInfo.spanY;
            } else {
                final ItemInfo dragInfo = (ItemInfo) d.dragInfo;
                spanX = dragInfo.spanX;
                spanY = dragInfo.spanY;
            }

            int minSpanX = spanX;
            int minSpanY = spanY;
            if (d.dragInfo instanceof PendingAddWidgetInfo) {
                minSpanX = ((PendingAddWidgetInfo) d.dragInfo).minSpanX;
                minSpanY = ((PendingAddWidgetInfo) d.dragInfo).minSpanY;
            }

            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, dropTargetLayout,
                    mTargetCell);
            float distance = dropTargetLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                    mDragViewVisualCenter[1], mTargetCell);
            if (willCreateUserFolder((ItemInfo) d.dragInfo, dropTargetLayout,
                    mTargetCell, distance, true)) {
                return true;
            }
            if (willAddToExistingUserFolder((ItemInfo) d.dragInfo, dropTargetLayout,
                    mTargetCell, distance)) {
                return true;
            }

            int[] resultSpan = new int[2];
            mTargetCell = dropTargetLayout.createArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY,
                    null, mTargetCell, resultSpan, CellLayout.MODE_ACCEPT_DROP);
            boolean foundCell = mTargetCell[0] >= 0 && mTargetCell[1] >= 0;

            // Don't accept the drop if there's no room for the item
            if (!foundCell) {
                // Don't show the message if we are dropping on the AllApps button and the hotseat
                // is full
                boolean isHotseat = mLauncher.isHotseatLayout(dropTargetLayout);
                if (mTargetCell != null && isHotseat) {
                    Hotseat hotseat = mLauncher.getHotseat();
                    if (hotseat.isAllAppsButtonRank(
                            hotseat.getOrderInHotseat(mTargetCell[0], mTargetCell[1]))) {
                        return false;
                    }
                }

                mLauncher.showOutOfSpaceMessage(isHotseat);
                return false;
            }
            
            /// M: Don't accept the drop if there exists one IMTKWidget which providerName equals the providerName of the
            // dragInfo.
//            if (d.dragInfo instanceof PendingAddWidgetInfo) {
//                PendingAddWidgetInfo info = (PendingAddWidgetInfo) d.dragInfo;
//                if (searchIMTKWidget(this, info.componentName.getClassName()) != null) {
//                    mLauncher.showOnlyOneWidgetMessage(info);
//                    return false;
//                }
//            }
        }
        return true;
    }

    public boolean willCreateUserFolder(ItemInfo info, CellLayout target, int[] targetCell, float
            distance, boolean considerTimeout) {
        if (distance > mMaxDistanceForFolderCreation) return false;
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);

        if (dropOverView != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.tmpCellY)) {
                return false;
            }
        }

        boolean hasntMoved = false;
        if (mDragInfo != null) {
            hasntMoved = dropOverView == mDragInfo.cell;
        }

        if (dropOverView == null || hasntMoved || (considerTimeout && !mCreateUserFolderOnDrop)) {
            return false;
        }

        boolean aboveShortcut = (dropOverView.getTag() instanceof ShortcutInfo);
        boolean willBecomeShortcut =
                (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT);

        return (aboveShortcut && willBecomeShortcut);
    }

    public boolean willAddToExistingUserFolder(Object dragInfo, CellLayout target, int[] targetCell,
            float distance) {
//        android.util.Log.i("QsLog", "willAddToExistingUserFolder()==distance:"+distance
//                +"===cur:"+mMaxDistanceForFolderCreation);
        if (distance > mMaxDistanceForFolderCreation) return false;
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);

        if (dropOverView != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.tmpCellY)) {
                return false;
            }
        }

        if (dropOverView instanceof FolderIcon) {
            FolderIcon fi = (FolderIcon) dropOverView;
            if (fi.acceptDrop(dragInfo)) {
                return true;
            }
        }
        return false;
    }

    public boolean createUserFolderIfNecessary(View newView, long container, CellLayout target,
            int[] targetCell, float distance, boolean external, DragView dragView,
            Runnable postAnimationRunnable) {
        if (distance > mMaxDistanceForFolderCreation) return false;
        View v = target.getChildAt(targetCell[0], targetCell[1]);
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "createUserFolderIfNecessary: newView = " + newView
                    + ", mDragInfo = " + mDragInfo + ", container = " + container + ", target = "
                    + target + ", targetCell[0] = " + targetCell[0] + ", targetCell[1] = "
                    + targetCell[1] + ", external = " + external + ", dragView = " + dragView
                    + ", v = " + v + ", mCreateUserFolderOnDrop = " + mCreateUserFolderOnDrop);
        }

    	boolean hasntMoved = false;
        if (mDragInfo != null) {
            CellLayout cellParent = getParentCellLayoutForView(mDragInfo.cell);
            hasntMoved = (mDragInfo.cellX == targetCell[0] &&
                    mDragInfo.cellY == targetCell[1]) && (cellParent == target);
        }

        if (v == null || hasntMoved || !mCreateUserFolderOnDrop) {
            if (Util.ENABLE_DEBUG) {
                Util.Log.d(TAG, "Do not create user folder: hasntMoved = " + hasntMoved + ", mCreateUserFolderOnDrop = "
                        + mCreateUserFolderOnDrop + ", v = " + v);
            }
            return false;
        }
        mCreateUserFolderOnDrop = false;
        final int screen = (targetCell == null) ? mDragInfo.screen : indexOfChild(target);

        boolean aboveShortcut = (v.getTag() instanceof ShortcutInfo);
        boolean willBecomeShortcut = (newView.getTag() instanceof ShortcutInfo);
        
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "createUserFolderIfNecessary: aboveShortcut = "
                    + aboveShortcut + ", willBecomeShortcut = " + willBecomeShortcut);
        }

        if (aboveShortcut && willBecomeShortcut) {
            ShortcutInfo sourceInfo = (ShortcutInfo) newView.getTag();
            ShortcutInfo destInfo = (ShortcutInfo) v.getTag();
            // if the drag started here, we need to remove it from the workspace
            if (!external) {
                getParentCellLayoutForView(mDragInfo.cell).removeView(mDragInfo.cell);
            }

            Rect folderLocation = new Rect();
            float scale = mLauncher.getDragLayer().getDescendantRectRelativeToSelf(v, folderLocation);
            target.removeView(v);

            FolderIcon fi =
                mLauncher.addFolder(target, container, screen, targetCell[0], targetCell[1]);
            destInfo.cellX = -1;
            destInfo.cellY = -1;
            sourceInfo.cellX = -1;
            sourceInfo.cellY = -1;

            // If the dragView is null, we can't animate
            boolean animate = dragView != null;
            if (animate) {
                fi.performCreateAnimation(destInfo, v, sourceInfo, dragView, folderLocation, scale,
                        postAnimationRunnable);
            } else {
                fi.addItem(destInfo);
                fi.addItem(sourceInfo);
            }
            return true;
        }
        return false;
    }

    public boolean addToExistingFolderIfNecessary(View newView, CellLayout target, int[] targetCell,
            float distance, DragObject d, boolean external) {
        if (distance > mMaxDistanceForFolderCreation) {
            return false;
        }

        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "createUserFolderIfNecessary: newView = " + newView + ", target = " + target
                    + ", targetCell[0] = " + targetCell[0] + ", targetCell[1] = " + targetCell[1] + ", external = "
                    + external + ", d = " + d + ", dropOverView = " + dropOverView);
        }
        if (!mAddToExistingFolderOnDrop) {
            return false;
        }
        mAddToExistingFolderOnDrop = false;

        if (dropOverView instanceof FolderIcon) {
            FolderIcon fi = (FolderIcon) dropOverView;
            if (fi.acceptDrop(d.dragInfo)) {
                fi.onDrop(d);

                // if the drag started here, we need to remove it from the workspace
                if (!external) {
                    getParentCellLayoutForView(mDragInfo.cell).removeView(mDragInfo.cell);
                }
                if (Util.ENABLE_DEBUG) {
                    Util.Log.d(TAG, "addToExistingFolderIfNecessary: fi = " + fi
                            + ", d = " + d);
                }
                return true;
            }
        }
        return false;
    }

    public void onDrop(final DragObject d) {
        mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset, d.dragView,
                mDragViewVisualCenter);

        CellLayout dropTargetLayout = mDropToLayout;

        // We want the point to be mapped to the dragTarget.
        if (dropTargetLayout != null) {
            if (mLauncher.isHotseatLayout(dropTargetLayout)) {
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(dropTargetLayout, mDragViewVisualCenter, null);
            }
        }
        if (Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "onDrop 1: drag view = " + d.dragView + ", dragInfo = " + d.dragInfo
                    + ", dragSource  = " + d.dragSource + ", dropTargetLayout = " + dropTargetLayout
                    + ", mDragInfo = " + mDragInfo + ", mInScrollArea = " + mInScrollArea
                    + ", this = " + this);
        }

        int snapScreen = -1;
        boolean resizeOnDrop = false;
        if (d.dragSource != this) {
            final int[] touchXY = new int[] { (int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1] };
            onDropExternal(touchXY, d.dragInfo, dropTargetLayout, false, d);
        } else if (mDragInfo != null) {
            final View cell = mDragInfo.cell;

            Runnable resizeRunnable = null;
            if (dropTargetLayout != null) {
                // Move internally
                boolean hasMovedLayouts = (getParentCellLayoutForView(cell) != dropTargetLayout);
                boolean hasMovedIntoHotseat = mLauncher.isHotseatLayout(dropTargetLayout);
                long container = hasMovedIntoHotseat ?
                        LauncherSettings.Favorites.CONTAINER_HOTSEAT :
                        LauncherSettings.Favorites.CONTAINER_DESKTOP;
                int screen = (mTargetCell[0] < 0) ?
                        mDragInfo.screen : indexOfChild(dropTargetLayout);
                int spanX = mDragInfo != null ? mDragInfo.spanX : 1;
                int spanY = mDragInfo != null ? mDragInfo.spanY : 1;
                // First we find the cell nearest to point at which the item is
                // dropped, without any consideration to whether there is an item there.

                mTargetCell = findNearestArea((int) mDragViewVisualCenter[0], (int)
                        mDragViewVisualCenter[1], spanX, spanY, dropTargetLayout, mTargetCell);
                float distance = dropTargetLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                        mDragViewVisualCenter[1], mTargetCell);
                if (Util.DEBUG_DRAG) {
                    Util.Log.d(TAG, "onDrop 2: cell = " + cell + ",screen = " + screen
                            + ", mInScrollArea = " + mInScrollArea + ", mTargetCell = " + mTargetCell
                            + ", this = " + this);
                }  

                // If the item being dropped is a shortcut and the nearest drop
                // cell also contains a shortcut, then create a folder with the two shortcuts.
                if (!mInScrollArea && createUserFolderIfNecessary(cell, container,
                        dropTargetLayout, mTargetCell, distance, false, d.dragView, null)) {
                    return;
                }

                if (addToExistingFolderIfNecessary(cell, dropTargetLayout, mTargetCell,
                        distance, d, false)) {
                    return;
                }

                // Aside from the special case where we're dropping a shortcut onto a shortcut,
                // we need to find the nearest cell location that is vacant
                ItemInfo item = (ItemInfo) d.dragInfo;
                int minSpanX = item.spanX;
                int minSpanY = item.spanY;
                if (item.minSpanX > 0 && item.minSpanY > 0) {
                    minSpanX = item.minSpanX;
                    minSpanY = item.minSpanY;
                }

                int[] resultSpan = new int[2];
                mTargetCell = dropTargetLayout.createArea((int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY, cell,
                        mTargetCell, resultSpan, CellLayout.MODE_ON_DROP);

                boolean foundCell = mTargetCell[0] >= 0 && mTargetCell[1] >= 0;

                // if the widget resizes on drop
                if (foundCell && (cell instanceof AppWidgetHostView) &&
                        (resultSpan[0] != item.spanX || resultSpan[1] != item.spanY)) {
                    resizeOnDrop = true;
                    item.spanX = resultSpan[0];
                    item.spanY = resultSpan[1];
                    AppWidgetHostView awhv = (AppWidgetHostView) cell;
                    AppWidgetResizeFrame.updateWidgetSizeRanges(awhv, mLauncher, resultSpan[0],
                            resultSpan[1]);
                }

                if (mCurrentPage != screen && !hasMovedIntoHotseat) {
                    snapScreen = screen;
                    snapToPage(screen);
                }

                if (foundCell) {
                    final ItemInfo info = (ItemInfo) cell.getTag();
                    if (hasMovedLayouts) {
                        // Reparent the view
                        getParentCellLayoutForView(cell).removeView(cell);
                        addInScreen(cell, container, screen, mTargetCell[0], mTargetCell[1],
                                info.spanX, info.spanY);
                    }

                    // update the item's position after drop
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
                    lp.cellX = lp.tmpCellX = mTargetCell[0];
                    lp.cellY = lp.tmpCellY = mTargetCell[1];
                    lp.cellHSpan = item.spanX;
                    lp.cellVSpan = item.spanY;
                    lp.isLockedToGrid = true;

                    /// M: Make different id for widget and shortcut.
                    int childId = -1;
                    if (cell instanceof AppWidgetHostView
                            && container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                        childId = LauncherModel.getCellLayoutChildId(container + 1,
                                mDragInfo.screen, mTargetCell[0], mTargetCell[1], mDragInfo.spanX,
                                mDragInfo.spanY);
                    } else {
                        childId = LauncherModel.getCellLayoutChildId(container, mDragInfo.screen,
                                mTargetCell[0], mTargetCell[1], mDragInfo.spanX, mDragInfo.spanY);
                    }                    
                    cell.setId(childId);
                  
                    if (container != LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                            cell instanceof LauncherAppWidgetHostView) {
                        final CellLayout cellLayout = dropTargetLayout;
                        // We post this call so that the widget has a chance to be placed
                        // in its final location

                        final LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) cell;
                        AppWidgetProviderInfo pinfo = hostView.getAppWidgetInfo();
                        if (pinfo != null/* &&
                                pinfo.resizeMode != AppWidgetProviderInfo.RESIZE_NONE*/) {
                            final Runnable addResizeFrame = new Runnable() {
                                public void run() {
                                    DragLayer dragLayer = mLauncher.getDragLayer();
                                    dragLayer.addResizeFrame(info, hostView, cellLayout);
                                }
                            };
                            resizeRunnable = (new Runnable() {
                                public void run() {
                                    if (!isPageMoving()) {
                                        addResizeFrame.run();
                                    } else {
                                        mDelayedResizeRunnable = addResizeFrame;
                                    }
                                }
                            });
                        }
                    }

                    mLauncher.getModel().moveItemInDatabase(info, container, screen, lp.cellX,
                            lp.cellY);
                } else {
                    // If we can't find a drop location, we return the item to its original position
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
                    mTargetCell[0] = lp.cellX;
                    mTargetCell[1] = lp.cellY;
                    CellLayout layout = (CellLayout) cell.getParent().getParent();
                    layout.markCellsAsOccupiedForView(cell);
                }
            }

            final CellLayout parent = (CellLayout) cell.getParent().getParent();
            final Runnable finalResizeRunnable = resizeRunnable;
            // Prepare it to be animated into its new position
            // This must be called after the view has been re-parented
            final Runnable onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    mAnimatingViewIntoPlace = false;
                    updateChildrenLayersEnabled(false);
                    if (finalResizeRunnable != null) {
                        finalResizeRunnable.run();
                    }
                }
            };
            mAnimatingViewIntoPlace = true;
            if (d.dragView.hasDrawn()) {
                final ItemInfo info = (ItemInfo) cell.getTag();
                if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET) {
                    int animationType = resizeOnDrop ? ANIMATE_INTO_POSITION_AND_RESIZE :
                            ANIMATE_INTO_POSITION_AND_DISAPPEAR;
                    animateWidgetDrop(info, parent, d.dragView,
                            onCompleteRunnable, animationType, cell, false);
                } else {
                    int duration = snapScreen < 0 ? -1 : ADJACENT_SCREEN_DROP_DURATION;
                    mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, cell, duration,
                            onCompleteRunnable, this);
                }
            } else {
                d.deferDragViewCleanupPostAnimation = false;
                cell.setVisibility(VISIBLE);
            }
            parent.onDropChild(cell);
            
            /// M: Call the appropriate callback when don't drop the IMTKWidget. 
            if (mTargetCell[0] == -1 && mTargetCell[1] == -1) {
            	stopDragAppWidget(mDragInfo.screen);
            }
        }
    }
    
    public Animator getChangeStateAnimation(final State state, boolean animated) {
        return getChangeStateAnimation(state, animated, 0);
    }
    
    public Animator getChangeStateAnimation(final State state, boolean animated, int delay) {
    	return null;
    }

    public void setFinalScrollForPageChange(int screen) {
//        if (screen >= 0) {
//            mSavedScrollX = getScrollX();
//            CellLayout cl = (CellLayout) getChildAt(screen);
//            mSavedTranslationX = cl.getTranslationX();
//            mSavedRotationY = cl.getRotationY();
//            final int newX = getChildOffset(screen) - getRelativeChildOffset(screen);
//            setScrollX(newX);
//            cl.setTranslationX(0f);
//            cl.setRotationY(0f);
//        }
    }

    public void resetFinalScrollForPageChange(int screen) {
//        if (screen >= 0) {
//            CellLayout cl = (CellLayout) getChildAt(screen);
//            setScrollX(mSavedScrollX);
//            cl.setTranslationX(mSavedTranslationX);
//            cl.setRotationY(mSavedRotationY);
//        }
    }

    public void getViewLocationRelativeToSelf(View v, int[] location) {
        getLocationInWindow(location);
        int x = location[0];
        int y = location[1];

        v.getLocationInWindow(location);
        int vX = location[0];
        int vY = location[1];

        location[0] = vX - x;
        location[1] = vY - y;
    }

    public void onDragEnter(DragObject d) {
        if (Util.DEBUG_DRAG) {
            Util.Log.i(TAG, "onDragEnter: d = " + d + ", mDragTargetLayout = "
                    + mDragTargetLayout
                    + ", mIsSwitchingState = " + mIsSwitchingState
                    +", mState:"+mState
                    +", isSmall:"+isSmall());
        }

        mDragEnforcer.onDragEnter();
        mCreateUserFolderOnDrop = false;
        mAddToExistingFolderOnDrop = false;

        mDropToLayout = null;
        CellLayout layout = getCurrentDropLayout();
        setCurrentDropLayout(layout);
        setCurrentDragOverlappingLayout(layout);

        // Because we don't have space in the Phone UI (the CellLayouts run to the edge) we
        // don't need to show the outlines
        if (Launcher.isScreenLarge()) {
            showOutlines();
        }
    }
    
    public void onDragExit(DragObject d) {
        if (Util.DEBUG_DRAG) {
            Util.Log.i(TAG, "onDragExit: d = " + d
                    + ", mIsSwitchingState = " + mIsSwitchingState
                    +", mState:"+mState
                    +", isSmall:"+isSmall());
        }

        mDragEnforcer.onDragExit();

        // Here we store the final page that will be dropped to, if the workspace in fact
        // receives the drop
        if (mInScrollArea) {
            if (isPageMoving()) {
                // If the user drops while the page is scrolling, we should use that page as the
                // destination instead of the page that is being hovered over.
                mDropToLayout = (CellLayout) getPageAt(getNextPage());
            } else {
            mDropToLayout = mDragOverlappingLayout;
            }
        } else {
            mDropToLayout = mDragTargetLayout;
        }

        if (mDragMode == DRAG_MODE_CREATE_FOLDER) {
            mCreateUserFolderOnDrop = true;
        } else if (mDragMode == DRAG_MODE_ADD_TO_FOLDER) {
            mAddToExistingFolderOnDrop = true;
        }

        // Reset the scroll area and previous drag target
        onResetScrollArea();
        if (Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "doDragExit: drag source = " + (d != null ? d.dragSource : null)
                    + ", drag info = " + (d != null ? d.dragInfo : null) + ", mDragTargetLayout = "
                    + mDragTargetLayout + ", mIsPageMoving = " + mIsPageMoving);
        }
        setCurrentDropLayout(null);
        setCurrentDragOverlappingLayout(null);

        mSpringLoadedDragController.cancel();

        if (!mIsPageMoving) {
            hideOutlines();
        }
    }

    public void setCurrentDropLayout(CellLayout layout) {
        if (mDragTargetLayout != null) {
            mDragTargetLayout.revertTempState();
            mDragTargetLayout.onDragExit();
        }
        mDragTargetLayout = layout;
        if (mDragTargetLayout != null) {
            mDragTargetLayout.onDragEnter();
        }
        cleanupReorder(true);
        cleanupFolderCreation();
        setCurrentDropOverCell(-1, -1);
    }

    public void setCurrentDragOverlappingLayout(CellLayout layout) {
        if (mDragOverlappingLayout != null) {
            mDragOverlappingLayout.setIsDragOverlapping(false);
        }
        mDragOverlappingLayout = layout;
        if (mDragOverlappingLayout != null) {
            mDragOverlappingLayout.setIsDragOverlapping(true);
        }
        invalidate();
    }

    public void setCurrentDropOverCell(int x, int y) {
        if (x != mDragOverX || y != mDragOverY) {
            mDragOverX = x;
            mDragOverY = y;
            setDragMode(DRAG_MODE_NONE);
        }
    }

    public void setDragMode(int dragMode) {
        if (dragMode != mDragMode) {
            if (dragMode == DRAG_MODE_NONE) {
                cleanupAddToFolder();
                // We don't want to cancel the re-order alarm every time the target cell changes
                // as this feels to slow / unresponsive.
                cleanupReorder(false);
                cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_ADD_TO_FOLDER) {
                cleanupReorder(true);
                cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_CREATE_FOLDER) {
                cleanupAddToFolder();
                cleanupReorder(true);
            } else if (dragMode == DRAG_MODE_REORDER) {
                cleanupAddToFolder();
                cleanupFolderCreation();
            }
            mDragMode = dragMode;
        }
    }

    public void cleanupFolderCreation() {
        //android.util.Log.i("QsLog", "cleanupFolderCreation(0)===");
        if (mDragFolderRingAnimator != null) {
            mDragFolderRingAnimator.animateToNaturalState();
        }
        mFolderCreationAlarm.cancelAlarm();
    }

    protected void cleanupAddToFolder() {
        if (mDragOverFolderIcon != null) {
            mDragOverFolderIcon.onDragExit(null);
            mDragOverFolderIcon = null;
        }
    }

    public void cleanupReorder(boolean cancelAlarm) {
        // Any pending reorders are canceled
        if (cancelAlarm) {
            mReorderAlarm.cancelAlarm();
        }
        mLastReorderX = -1;
        mLastReorderY = -1;
    }

    public DropTarget getDropTargetDelegate(DragObject d) {
        return null;
    }

    /*
    *
    * Convert the 2D coordinate xy from the parent View's coordinate space to this CellLayout's
    * coordinate space. The argument xy is modified with the return result.
    *
    */
    public void mapPointFromSelfToChild(View v, float[] xy) {
       mapPointFromSelfToChild(v, xy, null);
   }

   /*
    *
    * Convert the 2D coordinate xy from the parent View's coordinate space to this CellLayout's
    * coordinate space. The argument xy is modified with the return result.
    *
    * if cachedInverseMatrix is not null, this method will just use that matrix instead of
    * computing it itself; we use this to avoid redundant matrix inversions in
    * findMatchingPageForDragOver
    *
    */
    public void mapPointFromSelfToChild(View v, float[] xy, Matrix cachedInverseMatrix) {
       if (cachedInverseMatrix == null) {
           v.getMatrix().invert(mTempInverseMatrix);
           cachedInverseMatrix = mTempInverseMatrix;
       }
       int scrollX = getScrollX();
       if (mNextPage != INVALID_PAGE) {
           scrollX = mScroller.getFinalX();
       }
       xy[0] = xy[0] + scrollX - v.getLeft();
       xy[1] = xy[1] + getScrollY() - v.getTop();
       cachedInverseMatrix.mapPoints(xy);
   }

    public void mapPointFromSelfToHotseatLayout(Hotseat hotseat, float[] xy) {
       hotseat.getLayout().getMatrix().invert(mTempInverseMatrix);
       xy[0] = xy[0] - hotseat.getLeft() - hotseat.getLayout().getLeft();
       xy[1] = xy[1] - hotseat.getTop() - hotseat.getLayout().getTop();
       mTempInverseMatrix.mapPoints(xy);
   }

   /*
    *
    * Convert the 2D coordinate xy from this CellLayout's coordinate space to
    * the parent View's coordinate space. The argument xy is modified with the return result.
    *
    */
    public void mapPointFromChildToSelf(View v, float[] xy) {
       v.getMatrix().mapPoints(xy);
       int scrollX = getScrollX();
       if (mNextPage != INVALID_PAGE) {
           scrollX = mScroller.getFinalX();
       }
       xy[0] -= (scrollX - v.getLeft());
       xy[1] -= (getScrollY() - v.getTop());
   }

    public static float squaredDistance(float[] point1, float[] point2) {
        float distanceX = point1[0] - point2[0];
        float distanceY = point2[1] - point2[1];
        return distanceX * distanceX + distanceY * distanceY;
   }

    /*
     *
     * Returns true if the passed CellLayout cl overlaps with dragView
     *
     */
    public boolean overlaps(CellLayout cl, DragView dragView,
            int dragViewX, int dragViewY, Matrix cachedInverseMatrix) {
        // Transform the coordinates of the item being dragged to the CellLayout's coordinates
        final float[] draggedItemTopLeft = mTempDragCoordinates;
        draggedItemTopLeft[0] = dragViewX;
        draggedItemTopLeft[1] = dragViewY;
        final float[] draggedItemBottomRight = mTempDragBottomRightCoordinates;
        draggedItemBottomRight[0] = draggedItemTopLeft[0] + dragView.getDragRegionWidth();
        draggedItemBottomRight[1] = draggedItemTopLeft[1] + dragView.getDragRegionHeight();

        // Transform the dragged item's top left coordinates
        // to the CellLayout's local coordinates
        mapPointFromSelfToChild(cl, draggedItemTopLeft, cachedInverseMatrix);
        float overlapRegionLeft = Math.max(0f, draggedItemTopLeft[0]);
        float overlapRegionTop = Math.max(0f, draggedItemTopLeft[1]);

        if (overlapRegionLeft <= cl.getWidth() && overlapRegionTop >= 0) {
            // Transform the dragged item's bottom right coordinates
            // to the CellLayout's local coordinates
            mapPointFromSelfToChild(cl, draggedItemBottomRight, cachedInverseMatrix);
            float overlapRegionRight = Math.min(cl.getWidth(), draggedItemBottomRight[0]);
            float overlapRegionBottom = Math.min(cl.getHeight(), draggedItemBottomRight[1]);

            if (overlapRegionRight >= 0 && overlapRegionBottom <= cl.getHeight()) {
                float overlap = (overlapRegionRight - overlapRegionLeft) *
                         (overlapRegionBottom - overlapRegionTop);
                if (overlap > 0) {
                    return true;
                }
             }
        }
        return false;
    }

    /*
     *
     * This method returns the CellLayout that is currently being dragged to. In order to drag
     * to a CellLayout, either the touch point must be directly over the CellLayout, or as a second
     * strategy, we see if the dragView is overlapping any CellLayout and choose the closest one
     *
     * Return null if no CellLayout is currently being dragged over
     *
     */
    protected CellLayout findMatchingPageForDragOver(
            DragView dragView, float originX, float originY, boolean exact) {
        // We loop through all the screens (ie CellLayouts) and see which ones overlap
        // with the item being dragged and then choose the one that's closest to the touch point
        final int screenCount = getPageCount();
        CellLayout bestMatchingScreen = null;
        float smallestDistSoFar = Float.MAX_VALUE;

        for (int i = 0; i < screenCount; i++) {
            CellLayout cl = (CellLayout) getPageAt(i);

            final float[] touchXy = {originX, originY};
            // Transform the touch coordinates to the CellLayout's local coordinates
            // If the touch point is within the bounds of the cell layout, we can return immediately
            cl.getMatrix().invert(mTempInverseMatrix);
            mapPointFromSelfToChild(cl, touchXy, mTempInverseMatrix);

            if (false) {
                if(i == 0 || i == (screenCount - 1)){
                    Util.Log.w(TAG, "findMatchingPageForDragOver: i = " + i
                            +", xy:["+touchXy[0]+","+touchXy[1]+"]"
                            +", org:["+originX+","+originY+"]"
                            +", width:"+cl.getWidth()
                            +", scalewidth:"+getScaledMeasuredWidth(cl)
                            +", height:"+cl.getHeight()
                            +", scrollX:"+getScrollX()
                            +", left:"+cl.getLeft()
                            +", top:"+cl.getTop()
                            +", x:"+cl.getX());
                } else {
                    Util.Log.i(TAG, "findMatchingPageForDragOver: i = " + i
                            +", xy:["+touchXy[0]+","+touchXy[1]+"]"
                            +", org:["+originX+","+originY+"]"
                            +", width:"+cl.getWidth()
                            +", scalewidth:"+getScaledMeasuredWidth(cl)
                            +", height:"+cl.getHeight()
                            +", scrollX:"+getScrollX()
                            +", left:"+cl.getLeft()
                            +", top:"+cl.getTop()
                            +", x:"+cl.getX());
                }
            }
            
            if (touchXy[0] >= 0 && touchXy[0] <= cl.getWidth() &&
                    touchXy[1] >= 0 && touchXy[1] <= cl.getHeight()) {
                return cl;
            } else if(!exact && isSupportCycleSlidingScreen() && (mState == State.SPRING_LOADED)){
                if(i == 0 && touchXy[0] < 0){
                    return (CellLayout) getPageAt((screenCount - 1));
                } else if((i == (screenCount - 1)) && (touchXy[0] > cl.getWidth())){
                    return (CellLayout) getPageAt(0);
                }
            }

            if (!exact) {
                // Get the center of the cell layout in screen coordinates
                final float[] cellLayoutCenter = mTempCellLayoutCenterCoordinates;
                cellLayoutCenter[0] = cl.getWidth() / 2;
                cellLayoutCenter[1] = cl.getHeight() / 2;
                mapPointFromChildToSelf(cl, cellLayoutCenter);

                touchXy[0] = originX;
                touchXy[1] = originY;

                // Calculate the distance between the center of the CellLayout
                // and the touch point
                float dist = squaredDistance(touchXy, cellLayoutCenter);

                if (false) {
                    Util.Log.i(TAG, "findMatchingPageForDragOver(2): i = " + i 
                            +", dist:" + dist
                            +", smallest:"+smallestDistSoFar
                            +", center:["+cellLayoutCenter[0]+","+cellLayoutCenter[1]+"]"
                            +", width:"+cl.getWidth()
                            +", height:"+cl.getHeight()
                            +", scrollX:"+getScrollX()
                            +", left:"+cl.getLeft()
                            +", top:"+cl.getTop()
                            +", x:"+cl.getX());
                }
                
                if (dist < smallestDistSoFar) {
                    smallestDistSoFar = dist;
                    bestMatchingScreen = cl;
                }
                
                if (false && bestMatchingScreen != null && isSupportCycleSlidingScreen()) {
					int page = indexOfChild(bestMatchingScreen);
					if (page == screenCount - 1) {
						bestMatchingScreen = (CellLayout) getPageAt(0);
					} else if (page == 0) {
						bestMatchingScreen = (CellLayout) getPageAt(screenCount - 1);
					}
                }
            }
        }
        return bestMatchingScreen;
    }

    // This is used to compute the visual center of the dragView. This point is then
    // used to visualize drop locations and determine where to drop an item. The idea is that
    // the visual center represents the user's interpretation of where the item is, and hence
    // is the appropriate point to use when determining drop location.
    protected float[] getDragViewVisualCenter(int x, int y, int xOffset, int yOffset,
            DragView dragView, float[] recycle) {
        float res[];
        if (recycle == null) {
            res = new float[2];
        } else {
            res = recycle;
        }
        IResConfigManager resManager = mLauncher.getResConfigManager();
        // First off, the drag view has been shifted in a way that is not represented in the
        // x and y values or the x/yOffsets. Here we account for that shift.
        x += resManager.getDimensionPixelSize(IResConfigManager.DIM_DRAG_VIEW_OFFSETX);//getResources().getDimensionPixelSize(R.dimen.dragViewOffsetX);
        y += resManager.getDimensionPixelSize(IResConfigManager.DIM_DRAG_VIEW_OFFSETY);//getResources().getDimensionPixelSize(R.dimen.dragViewOffsetY);

        // These represent the visual top and left of drag view if a dragRect was provided.
        // If a dragRect was not provided, then they correspond to the actual view left and
        // top, as the dragRect is in that case taken to be the entire dragView.
        // R.dimen.dragViewOffsetY.
        int left = x - xOffset;
        int top = y - yOffset;

        // In order to find the visual center, we shift by half the dragRect
        res[0] = left + dragView.getDragRegion().width() / 2;
        res[1] = top + dragView.getDragRegion().height() / 2;

        return res;
    }

    protected boolean isDragWidget(DragObject d) {
        return (d.dragInfo instanceof LauncherAppWidgetInfo ||
                d.dragInfo instanceof PendingAddWidgetInfo);
    }
    protected boolean isExternalDragWidget(DragObject d) {
        return d.dragSource != this && isDragWidget(d);
    }

    public void onDragOver(DragObject d) {
        if (false && Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "onDragOver: d = " + d + 
                    ", dragInfo = " + d.dragInfo + ", mInScrollArea = " + mInScrollArea
                    + ", mIsSwitchingState = " + mIsSwitchingState
                    +", mState:"+mState
                    +", isSmall:"+isSmall());
        }

        // Skip drag over events while we are dragging over side pages
        if (mInScrollArea || mIsSwitchingState || mState == State.SMALL) return;

        Rect r = new Rect();
        CellLayout layout = null;
        ItemInfo item = (ItemInfo) d.dragInfo;

        // Ensure that we have proper spans for the item that we are dropping
        if (item.spanX < 0 || item.spanY < 0) throw new RuntimeException("Improper spans found");
        mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset,
            d.dragView, mDragViewVisualCenter);

        final View child = (mDragInfo == null) ? null : mDragInfo.cell;
        // Identify whether we have dragged over a side page
        if (isSmall()) {
            if (mLauncher.getHotseat() != null && !isExternalDragWidget(d)) {
                mLauncher.getHotseat().getHitRect(r);
                if (r.contains(d.x, d.y)) {
                    layout = mLauncher.getHotseat().getLayout();
                }
            }
            if (layout == null) {
                layout = findMatchingPageForDragOver(d.dragView, d.x, d.y, false);
            }
            if (layout != mDragTargetLayout) {
                if (Util.DEBUG_DRAG) {
                    Util.Log.w(TAG, "onDragOver: layout = " + layout 
                            + ", mDragTargetLayout = " + mDragTargetLayout 
                            + ", mInScrollArea = " + mInScrollArea
                            + ", mIsSwitchingState = " + mIsSwitchingState
                            +", mState:"+mState
                            +", isSmall:"+isSmall());
                }
                
                setCurrentDropLayout(layout);
                setCurrentDragOverlappingLayout(layout);

                boolean isInSpringLoadedMode = (mState == State.SPRING_LOADED);

                if (isInSpringLoadedMode) {
                    if (mLauncher.isHotseatLayout(layout)) {
                        mSpringLoadedDragController.cancel();
                    } else {
                        mSpringLoadedDragController.setAlarm(mDragTargetLayout);
                    }
                }
            }
        } else {
            // Test to see if we are over the hotseat otherwise just use the current page
            if (mLauncher.getHotseat() != null && !isDragWidget(d)) {
                mLauncher.getHotseat().getHitRect(r);
                if (r.contains(d.x, d.y)) {
                    layout = mLauncher.getHotseat().getLayout();
                }
            }
            if (layout == null) {
                layout = getCurrentDropLayout();
            }
            if (layout != mDragTargetLayout) {
                setCurrentDropLayout(layout);
                setCurrentDragOverlappingLayout(layout);
            }
        }

        // Handle the drag over
        if (mDragTargetLayout != null) {
            // We want the point to be mapped to the dragTarget.
            if (mLauncher.isHotseatLayout(mDragTargetLayout)) {
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(mDragTargetLayout, mDragViewVisualCenter, null);
            }

            ItemInfo info = (ItemInfo) d.dragInfo;

            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], item.spanX, item.spanY,
                    mDragTargetLayout, mTargetCell);

            setCurrentDropOverCell(mTargetCell[0], mTargetCell[1]);

            float targetCellDistance = mDragTargetLayout.getDistanceFromCell(
                    mDragViewVisualCenter[0], mDragViewVisualCenter[1], mTargetCell);

            final View dragOverView = mDragTargetLayout.getChildAt(mTargetCell[0],
                    mTargetCell[1]);

            manageFolderFeedback(info, mDragTargetLayout, mTargetCell,
                    targetCellDistance, dragOverView);

            int minSpanX = item.spanX;
            int minSpanY = item.spanY;
            if (item.minSpanX > 0 && item.minSpanY > 0) {
                minSpanX = item.minSpanX;
                minSpanY = item.minSpanY;
            }

            boolean nearestDropOccupied = mDragTargetLayout.isNearestDropLocationOccupied((int)
                    mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], item.spanX,
                    item.spanY, child, mTargetCell);

            if (!nearestDropOccupied) {
                mDragTargetLayout.visualizeDropLocation(child, mDragOutline,
                        (int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1],
                        mTargetCell[0], mTargetCell[1], item.spanX, item.spanY, false,
                        d.dragView.getDragVisualizeOffset(), d.dragView.getDragRegion());
            } else if ((mDragMode == DRAG_MODE_NONE || mDragMode == DRAG_MODE_REORDER)
                    && !mReorderAlarm.alarmPending() && (mLastReorderX != mTargetCell[0] ||
                    mLastReorderY != mTargetCell[1])) {

                // Otherwise, if we aren't adding to or creating a folder and there's no pending
                // reorder, then we schedule a reorder
                ReorderAlarmListener listener = new ReorderAlarmListener(mDragViewVisualCenter,
                        minSpanX, minSpanY, item.spanX, item.spanY, d.dragView, child);
                mReorderAlarm.setOnAlarmListener(listener);
                mReorderAlarm.setAlarm(REORDER_TIMEOUT);
            }

            if (mDragMode == DRAG_MODE_CREATE_FOLDER || mDragMode == DRAG_MODE_ADD_TO_FOLDER ||
                    !nearestDropOccupied) {
                if (mDragTargetLayout != null) {
                    mDragTargetLayout.revertTempState();
                }
            }
        }
    }

    protected void manageFolderFeedback(ItemInfo info, CellLayout targetLayout,
            int[] targetCell, float distance, View dragOverView) {
        boolean userFolderPending = willCreateUserFolder(info, targetLayout, targetCell, distance,
                false);

        if (mDragMode == DRAG_MODE_NONE && userFolderPending &&
                !mFolderCreationAlarm.alarmPending()) {
            mFolderCreationAlarm.setOnAlarmListener(new
                    FolderCreationAlarmListener(targetLayout, targetCell[0], targetCell[1]));
            mFolderCreationAlarm.setAlarm(FOLDER_CREATION_TIMEOUT);
            return;
        }

        boolean willAddToFolder =
                willAddToExistingUserFolder(info, targetLayout, targetCell, distance);

        if (willAddToFolder && mDragMode == DRAG_MODE_NONE) {
            mDragOverFolderIcon = ((FolderIcon) dragOverView);
            mDragOverFolderIcon.onDragEnter(info);
            if (targetLayout != null) {
                targetLayout.clearDragOutlines();
            }
            setDragMode(DRAG_MODE_ADD_TO_FOLDER);
            return;
        }

        if (mDragMode == DRAG_MODE_ADD_TO_FOLDER && !willAddToFolder) {
            setDragMode(DRAG_MODE_NONE);
        }
        if (mDragMode == DRAG_MODE_CREATE_FOLDER && !userFolderPending) {
            setDragMode(DRAG_MODE_NONE);
        }

        return;
    }
    
    @Override
    public void getHitRect(Rect outRect) {
        // We want the workspace to have the whole area of the display (it will find the correct
        // cell layout to drop to in the existing drag/drop logic.
        outRect.set(0, 0, mDisplaySize.x, mDisplaySize.y);
    }

    /**
     * Add the item specified by dragInfo to the given layout.
     * @return true if successful
     */
    public boolean addExternalItemToScreen(ItemInfo dragInfo, CellLayout layout) {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "addExternalItemToScreen: dragInfo = " + dragInfo
                    + ", layout = " + layout);
        }
        if (layout.findCellForSpan(mTempEstimate, dragInfo.spanX, dragInfo.spanY)) {
            onDropExternal(dragInfo.dropPos, (ItemInfo) dragInfo, (CellLayout) layout, false);
            return true;
        }
        mLauncher.showOutOfSpaceMessage(mLauncher.isHotseatLayout(layout));
        return false;
    }

    protected void onDropExternal(int[] touchXY, Object dragInfo,
            CellLayout cellLayout, boolean insertAtFirst) {
        onDropExternal(touchXY, dragInfo, cellLayout, insertAtFirst, null);
    }

    /**
     * Drop an item that didn't originate on one of the workspace screens.
     * It may have come from Launcher (e.g. from all apps or customize), or it may have
     * come from another app altogether.
     *
     * NOTE: This can also be called when we are outside of a drag event, when we want
     * to add an item to one of the workspace screens.
     */
    protected void onDropExternal(final int[] touchXY, final Object dragInfo,
            final CellLayout cellLayout, boolean insertAtFirst, DragObject d) {
        final Runnable exitSpringLoadedRunnable = new Runnable() {
            @Override
            public void run() {
                mLauncher.exitSpringLoadedDragModeDelayed(true, false, null);
            }
        };

        ItemInfo info = (ItemInfo) dragInfo;
        int spanX = info.spanX;
        int spanY = info.spanY;
        if (mDragInfo != null) {
            spanX = mDragInfo.spanX;
            spanY = mDragInfo.spanY;
        }

        final long container = mLauncher.isHotseatLayout(cellLayout) ?
                LauncherSettings.Favorites.CONTAINER_HOTSEAT :
                    LauncherSettings.Favorites.CONTAINER_DESKTOP;
        final int screen = indexOfChild(cellLayout);
        if (!mLauncher.isHotseatLayout(cellLayout) && screen != mCurrentPage
                && mState != State.SPRING_LOADED) {
            snapToPage(screen);
        }

        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onDropExternal: touchXY[0] = "
                    + ((touchXY != null) ? touchXY[0] : -1) + ", touchXY[1] = "
                    + ((touchXY != null) ? touchXY[1] : -1) + ", dragInfo = " + dragInfo
                    + ",info = " + info + ", cellLayout = " + cellLayout + ", insertAtFirst = "
                    + insertAtFirst + ", dragInfo = " + d.dragInfo + ", screen = " + screen
                    + ", container = " + container);
        }

        if (info instanceof PendingAddItemInfo) {
            final PendingAddItemInfo pendingInfo = (PendingAddItemInfo) dragInfo;

            boolean findNearestVacantCell = true;
            if (pendingInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                mTargetCell = findNearestArea((int) touchXY[0], (int) touchXY[1], spanX, spanY,
                        cellLayout, mTargetCell);
                float distance = cellLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                        mDragViewVisualCenter[1], mTargetCell);
                if (willCreateUserFolder((ItemInfo) d.dragInfo, cellLayout, mTargetCell,
                        distance, true) || willAddToExistingUserFolder((ItemInfo) d.dragInfo,
                                cellLayout, mTargetCell, distance)) {
                    findNearestVacantCell = false;
                }
            }

            final ItemInfo item = (ItemInfo) d.dragInfo;
            boolean updateWidgetSize = false;
            if (findNearestVacantCell) {
                int minSpanX = item.spanX;
                int minSpanY = item.spanY;
                if (item.minSpanX > 0 && item.minSpanY > 0) {
                    minSpanX = item.minSpanX;
                    minSpanY = item.minSpanY;
                }
                int[] resultSpan = new int[2];
                mTargetCell = cellLayout.createArea((int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1], minSpanX, minSpanY, info.spanX, info.spanY,
                        null, mTargetCell, resultSpan, CellLayout.MODE_ON_DROP_EXTERNAL);

                if (resultSpan[0] != item.spanX || resultSpan[1] != item.spanY) {
                    updateWidgetSize = true;
                }
                item.spanX = resultSpan[0];
                item.spanY = resultSpan[1];
            }

            Runnable onAnimationCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    // When dragging and dropping from customization tray, we deal with creating
                    // widgets/shortcuts/folders in a slightly different way
                    switch (pendingInfo.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                        int span[] = new int[2];
                        span[0] = item.spanX;
                        span[1] = item.spanY;
                        mLauncher.addAppWidgetFromDrop((PendingAddWidgetInfo) pendingInfo,
                                container, screen, mTargetCell, span, null);
                        break;
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                        mLauncher.processShortcutFromDrop(pendingInfo.componentName,
                                container, screen, mTargetCell, null);
                        break;
                    default:
                        throw new IllegalStateException("Unknown item type: " +
                                pendingInfo.itemType);
                    }
                }
            };
            View finalView = pendingInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                    ? ((PendingAddWidgetInfo) pendingInfo).boundWidget : null;

            if (finalView instanceof AppWidgetHostView && updateWidgetSize) {
                AppWidgetHostView awhv = (AppWidgetHostView) finalView;
                AppWidgetResizeFrame.updateWidgetSizeRanges(awhv, mLauncher, item.spanX,
                        item.spanY);
            }

            int animationStyle = ANIMATE_INTO_POSITION_AND_DISAPPEAR;
            if (pendingInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET &&
                    ((PendingAddWidgetInfo) pendingInfo).info.configure != null) {
                animationStyle = ANIMATE_INTO_POSITION_AND_REMAIN;
            }
            animateWidgetDrop(info, cellLayout, d.dragView, onAnimationCompleteRunnable,
                    animationStyle, finalView, true);
        } else {
            // This is for other drag/drop cases, like dragging from All Apps
            View view = null;

            switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                if (info.container == NO_ID && info instanceof ApplicationInfo) {
                    // Came from all apps -- make a copy
                    info = new ShortcutInfo((ApplicationInfo) info);
                }
                view = mLauncher.createShortcut(cellLayout, (ShortcutInfo) info);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                view = FolderIcon.fromXml(mLauncher, cellLayout,
                        (FolderInfo) info);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
            }

            // First we find the cell nearest to point at which the item is
            // dropped, without any consideration to whether there is an item there.
            if (touchXY != null) {
                mTargetCell = findNearestArea((int) touchXY[0], (int) touchXY[1], spanX, spanY,
                        cellLayout, mTargetCell);
                float distance = cellLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                        mDragViewVisualCenter[1], mTargetCell);
                d.postAnimationRunnable = exitSpringLoadedRunnable;
                if (createUserFolderIfNecessary(view, container, cellLayout, mTargetCell, distance,
                        true, d.dragView, d.postAnimationRunnable)) {
                    return;
                }
                if (addToExistingFolderIfNecessary(view, cellLayout, mTargetCell, distance, d,
                        true)) {
                    return;
                }
            }

            if (touchXY != null) {
                // when dragging and dropping, just find the closest free spot
                mTargetCell = cellLayout.createArea((int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1], 1, 1, 1, 1,
                        null, mTargetCell, null, CellLayout.MODE_ON_DROP_EXTERNAL);
            } else {
                cellLayout.findCellForSpan(mTargetCell, 1, 1);
            }
            addInScreen(view, container, screen, mTargetCell[0], mTargetCell[1], info.spanX,
                    info.spanY, insertAtFirst);
            cellLayout.onDropChild(view);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
            cellLayout.getShortcutsAndWidgets().measureChild(view);


            mLauncher.getModel().addOrMoveItemInDatabase(info, container, screen,
                    lp.cellX, lp.cellY);

            if (d.dragView != null) {
                // We wrap the animation call in the temporary set and reset of the current
                // cellLayout to its final transform -- this means we animate the drag view to
                // the correct final location.
                setFinalTransitionTransform(cellLayout);
                mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, view,
                        exitSpringLoadedRunnable);
                resetTransitionTransform(cellLayout);
            }
        }
    }

    public Bitmap createWidgetBitmap(ItemInfo widgetInfo, View layout) {
        int[] unScaledSize = estimateItemSize(widgetInfo.spanX,
                widgetInfo.spanY, widgetInfo, false);
        
        if(unScaledSize[0] > 0 && unScaledSize[1] > 0){
            int visibility = layout.getVisibility();
            layout.setVisibility(VISIBLE);
    
            int width = MeasureSpec.makeMeasureSpec(unScaledSize[0], MeasureSpec.EXACTLY);
            int height = MeasureSpec.makeMeasureSpec(unScaledSize[1], MeasureSpec.EXACTLY);
            Bitmap b = Bitmap.createBitmap(unScaledSize[0], unScaledSize[1],
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
    
            layout.measure(width, height);
            layout.layout(0, 0, unScaledSize[0], unScaledSize[1]);
            layout.draw(c);
            c.setBitmap(null);
            layout.setVisibility(visibility);
            return b;
        }
        return null;
    }

    protected void getFinalPositionForDropAnimation(int[] loc, float[] scaleXY,
            DragView dragView, CellLayout layout, ItemInfo info, int[] targetCell,
            boolean external, boolean scale) {
        // Now we animate the dragView, (ie. the widget or shortcut preview) into its final
        // location and size on the home screen.
        int spanX = info.spanX;
        int spanY = info.spanY;

        Rect r = estimateItemPosition(layout, info, targetCell[0], targetCell[1], spanX, spanY);
        loc[0] = r.left;
        loc[1] = r.top;

        setFinalTransitionTransform(layout);
        float cellLayoutScale =
                mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(layout, loc);
        resetTransitionTransform(layout);

        float dragViewScaleX;
        float dragViewScaleY;
        if (scale) {
            dragViewScaleX = (1.0f * r.width()) / dragView.getMeasuredWidth();
            dragViewScaleY = (1.0f * r.height()) / dragView.getMeasuredHeight();
        } else {
            dragViewScaleX = 1f;
            dragViewScaleY = 1f;
        }

        // The animation will scale the dragView about its center, so we need to center about
        // the final location.
        loc[0] -= (dragView.getMeasuredWidth() - cellLayoutScale * r.width()) / 2;
        loc[1] -= (dragView.getMeasuredHeight() - cellLayoutScale * r.height()) / 2;

        scaleXY[0] = dragViewScaleX * cellLayoutScale;
        scaleXY[1] = dragViewScaleY * cellLayoutScale;
    }

    public void animateWidgetDrop(ItemInfo info, CellLayout cellLayout, DragView dragView,
            final Runnable onCompleteRunnable, int animationType, final View finalView,
            boolean external) {
        Rect from = new Rect();
        mLauncher.getDragLayer().getViewRectRelativeToSelf(dragView, from);

        int[] finalPos = new int[2];
        float scaleXY[] = new float[2];
        boolean scalePreview = !(info instanceof PendingAddShortcutInfo);
        getFinalPositionForDropAnimation(finalPos, scaleXY, dragView, cellLayout, info, mTargetCell,
                external, scalePreview);

        IResConfigManager res = mLauncher.getResConfigManager();//.getResources();
        int duration = res.getInteger(IResConfigManager.CONFIG_DROP_ANIM_MAX_DURATION) - 200;
        //R.integer.config_dropAnimMaxDuration

        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "animateWidgetDrop: info = " + info + ", animationType = " + animationType + ", finalPos = ("
                    + finalPos[0] + ", " + finalPos[1] + "), scaleXY = (" + scaleXY[0] + ", " + scaleXY[1]
                    + "), scalePreview = " + scalePreview + ",external = " + external);
        }

        // In the case where we've prebound the widget, we remove it from the DragLayer
        if (finalView instanceof AppWidgetHostView && external) {
            Util.Log.d(TAG, "6557954 Animate widget drop, final view is appWidgetHostView");
            mLauncher.getDragLayer().removeView(finalView);
        }
        if ((animationType == ANIMATE_INTO_POSITION_AND_RESIZE || external) && finalView != null) {
            Bitmap crossFadeBitmap = createWidgetBitmap(info, finalView);
            dragView.setCrossFadeBitmap(crossFadeBitmap);
            dragView.crossFade((int) (duration * 0.8f));
        } else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET && external) {
            scaleXY[0] = scaleXY[1] = Math.min(scaleXY[0],  scaleXY[1]);
        }

        DragLayer dragLayer = mLauncher.getDragLayer();
        if (animationType == CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION) {
            mLauncher.getDragLayer().animateViewIntoPosition(dragView, finalPos, 0f, 0.1f, 0.1f,
                    DragLayer.ANIMATION_END_DISAPPEAR, onCompleteRunnable, duration);
        } else {
            int endStyle;
            if (animationType == ANIMATE_INTO_POSITION_AND_REMAIN) {
                endStyle = DragLayer.ANIMATION_END_REMAIN_VISIBLE;
            } else {
                endStyle = DragLayer.ANIMATION_END_DISAPPEAR;;
            }

            Runnable onComplete = new Runnable() {
                @Override
                public void run() {
                    if (finalView != null) {
                        finalView.setVisibility(VISIBLE);
                    }
                    if (onCompleteRunnable != null) {
                        onCompleteRunnable.run();
                    }
                }
            };
            dragLayer.animateViewIntoPosition(dragView, from.left, from.top, finalPos[0],
                    finalPos[1], 1, 1, 1, scaleXY[0], scaleXY[1], onComplete, endStyle,
                    duration, this);
        }
    }

    public void setFinalTransitionTransform(CellLayout layout) {
//        if (isSwitchingState()) {
//            int index = indexOfChild(layout);
//            mCurrentScaleX = layout.getScaleX();
//            mCurrentScaleY = layout.getScaleY();
//            mCurrentTranslationX = layout.getTranslationX();
//            mCurrentTranslationY = layout.getTranslationY();
//            mCurrentRotationY = layout.getRotationY();
//            layout.setScaleX(mNewScaleXs[index]);
//            layout.setScaleY(mNewScaleYs[index]);
//            layout.setTranslationX(mNewTranslationXs[index]);
//            layout.setTranslationY(mNewTranslationYs[index]);
//            layout.setRotationY(mNewRotationYs[index]);
//        }
    }
    public void resetTransitionTransform(CellLayout layout) {
//        if (isSwitchingState()) {
//            mCurrentScaleX = layout.getScaleX();
//            mCurrentScaleY = layout.getScaleY();
//            mCurrentTranslationX = layout.getTranslationX();
//            mCurrentTranslationY = layout.getTranslationY();
//            mCurrentRotationY = layout.getRotationY();
//            layout.setScaleX(mCurrentScaleX);
//            layout.setScaleY(mCurrentScaleY);
//            layout.setTranslationX(mCurrentTranslationX);
//            layout.setTranslationY(mCurrentTranslationY);
//            layout.setRotationY(mCurrentRotationY);
//        }
    }

    /**
     * Return the current {@link CellLayout}, correctly picking the destination
     * screen while a scroll is in progress.
     */
    public CellLayout getCurrentDropLayout() {
        return (CellLayout) getPageAt(getNextPage());
    }

    /**
     * Return the current CellInfo describing our current drag; this method exists
     * so that Launcher can sync this object with the correct info when the activity is created/
     * destroyed
     *
     */
    public CellLayout.CellInfo getDragInfo() {
        return mDragInfo;
    }

    /**
     * Calculate the nearest cell where the given object would be dropped.
     *
     * pixelX and pixelY should be in the coordinate system of layout
     */
    protected int[] findNearestArea(int pixelX, int pixelY,
            int spanX, int spanY, CellLayout layout, int[] recycle) {
        return layout.findNearestArea(
                pixelX, pixelY, spanX, spanY, recycle);
    }

    public void setup(DragController dragController/*, Launcher launcher*/) {
        mSpringLoadedDragController = new SpringLoadedDragController(mLauncher);
        mDragController = dragController;
        //mLauncher = launcher;
        //mIconCache = launcher.getIconCache();
        // hardware layers on children are enabled on startup, but should be disabled until
        // needed
        updateChildrenLayersEnabled(false);
        setWallpaperDimension();
    }

    /**
     * Called at the end of a drag which originated on the workspace.
     */
    public void onDropCompleted(View target, DragObject d, boolean isFlingToDelete,
            boolean success) {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onDropCompleted: target = " + target + ", d = " + d
                    + ", isFlingToDelete = " + isFlingToDelete + ", mDragInfo = " + mDragInfo + ", success = " + success);
        }
//        android.util.Log.i("QsLog", "onDropCompleted()=====success:"+success
//                +"==isSmall:"+isSmall()
//                +"==mState:"+mState);
        if(mState == State.SPRING_LOADED)
            mLauncher.exitSpringLoadedDragModeDelayed(false, false, null);
        
        if (success) {
            if (target != this) {
                if (mDragInfo != null) {
                	getParentCellLayoutForView(mDragInfo.cell).removeView(mDragInfo.cell);
                    if (mDragInfo.cell instanceof DropTarget) {
                        mDragController.removeDropTarget((DropTarget) mDragInfo.cell);
                    }
                }
            }
        } else if (mDragInfo != null) {
            CellLayout cellLayout;
            if (mLauncher.isHotseatLayout(target)) {
                cellLayout = mLauncher.getHotseat().getLayout();
            } else {
                cellLayout = (CellLayout) getPageAt(mDragInfo.screen);
            }
            cellLayout.onDropChild(mDragInfo.cell);
            cellLayout.markCellsAsOccupiedForView(mDragInfo.cell);
        }
        if (d.cancelled &&  mDragInfo.cell != null) {
            mDragInfo.cell.setVisibility(VISIBLE);
        }

        /// M: Call the appropriate callback when drop the IMTKWidget completed.
        stopDragAppWidget(mCurrentPage);
        mDragOutline = null;
        mDragInfo = null;

        // Hide the scrolling indicator after you pick up an item
        //hideScrollingIndicator(false);
    }

    public void updateItemLocationsInDatabase(CellLayout cl) {
        int count = cl.getShortcutsAndWidgets().getChildCount();

        int screen = indexOfChild(cl);
        int container = LauncherSettings.Favorites.CONTAINER_DESKTOP;

        if (mLauncher.isHotseatLayout(cl)) {
            screen = -1;
            container = LauncherSettings.Favorites.CONTAINER_HOTSEAT;
        }

        for (int i = 0; i < count; i++) {
            View v = cl.getShortcutsAndWidgets().getChildAt(i);
            ItemInfo info = (ItemInfo) v.getTag();
            // Null check required as the AllApps button doesn't have an item info
            if (info != null && info.requiresDbUpdate) {
                info.requiresDbUpdate = false;
                mLauncher.getModel().modifyItemInDatabase(info, container, screen, info.cellX,
                        info.cellY, info.spanX, info.spanY);
            }
        }
    }

    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }

    @Override
    public void onFlingToDelete(DragObject d, int x, int y, PointF vec) {
        // Do nothing
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // Do nothing
    }

    public boolean isDropEnabled() {
        return true;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onRestoreInstanceState: state = " + state
                    + ", mCurrentPage = " + mCurrentPage);
        }
        Launcher.setScreen(mCurrentPage);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        // We don't dispatch restoreInstanceState to our children using this code path.
        // Some pages will be restored immediately as their items are bound immediately, and 
        // others we will need to wait until after their items are bound.
        mSavedStates = container;
    }

    public void restoreInstanceStateForChild(int child) {
        if (mSavedStates != null) {
            mRestoredPages.add(child);
            CellLayout cl = (CellLayout) getPageAt(child);
            cl.restoreInstanceState(mSavedStates);
        }
    }

    public void restoreInstanceStateForRemainingPages() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            if (!mRestoredPages.contains(i)) {
                restoreInstanceStateForChild(i);
            }
        }
        mRestoredPages.clear();
    }

    @Override
    public void scrollLeft() {
        if (Util.DEBUG_DRAG) {
            Util.Log.w(TAG, "scrollLeft: "
                    + ", mIsSwitchingState = " + mIsSwitchingState
                    +", mState:"+mState
                    +", isSmall:"+isSmall());
        }
        
        if (!isSmall() && !mIsSwitchingState) {
            super.scrollLeft();
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            openFolder.completeDragExit();
        }
    }

    @Override
    public void scrollRight() {
        if (Util.DEBUG_DRAG) {
            Util.Log.w(TAG, "scrollRight: "
                    + ", mIsSwitchingState = " + mIsSwitchingState
                    +", mState:"+mState
                    +", isSmall:"+isSmall());
        }
        
        if (!isSmall() && !mIsSwitchingState) {
            super.scrollRight();
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            openFolder.completeDragExit();
        }
    }

    @Override
    public boolean onEnterScrollArea(int x, int y, int direction) {
        // Ignore the scroll area if we are dragging over the hot seat
        boolean isPortrait = !Launcher.isScreenLandscape(getContext());
        if (mLauncher.getHotseat() != null && isPortrait) {
            Rect r = new Rect();
            mLauncher.getHotseat().getHitRect(r);
            if (Util.DEBUG_DRAG){
                Util.Log.i(TAG, "onEnterScrollArea(3): direction:"+direction+", rect:"+
                        r+" contain:"+r.contains(x, y)
                        +", ["+x+","+y+"]");
            }
            
            if (r.contains(x, y)) {
                return false;
            }
        }

        boolean result = false;
        if (!isSmall() && !mIsSwitchingState) {
            mInScrollArea = true;

            int page = getNextPage() +
                       (direction == DragController.SCROLL_LEFT ? -1 : 1);
            if (isSupportCycleSlidingScreen()) {
				if (direction == DragController.SCROLL_RIGHT && page == getPageCount()) {
					page = 0;
				} else if (direction == DragController.SCROLL_LEFT	&& page == -1) {
					page = getPageCount() - 1;
				}
            }
            // We always want to exit the current layout to ensure parity of enter / exit
            setCurrentDropLayout(null);

            if (0 <= page && page < getPageCount()) {
                CellLayout layout = (CellLayout) getPageAt(page);
                setCurrentDragOverlappingLayout(layout);

                // Workspace is responsible for drawing the edge glow on adjacent pages,
                // so we need to redraw the workspace when this may have changed.
                invalidate();
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean onExitScrollArea() {
        boolean result = false;
        if (mInScrollArea) {
            invalidate();
            CellLayout layout = getCurrentDropLayout();
            setCurrentDropLayout(layout);
            setCurrentDragOverlappingLayout(layout);

            result = true;
            mInScrollArea = false;
        }
        return result;
    }

    protected void onResetScrollArea() {
        setCurrentDragOverlappingLayout(null);
        mInScrollArea = false;
    }

    /**
     * Returns a specific CellLayout
     */
    public CellLayout getParentCellLayoutForView(View v) {
        ArrayList<CellLayout> layouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout layout : layouts) {
            if (layout.getShortcutsAndWidgets().indexOfChild(v) > -1) {
                return layout;
            }
        }
        return null;
    }

    /**
     * Returns a list of all the CellLayouts in the workspace.
     */
    public ArrayList<CellLayout> getWorkspaceAndHotseatCellLayouts() {
        ArrayList<CellLayout> layouts = new ArrayList<CellLayout>();
        int screenCount = getPageCount();
        for (int screen = 0; screen < screenCount; screen++) {
            layouts.add(((CellLayout) getPageAt(screen)));
        }
        if (mLauncher.getHotseat() != null) {
            layouts.add(mLauncher.getHotseat().getLayout());
        }
        return layouts;
    }

    /**
     * We should only use this to search for specific children.  Do not use this method to modify
     * ShortcutsAndWidgetsContainer directly. Includes ShortcutAndWidgetContainers from
     * the hotseat and workspace pages
     */
    public ArrayList<ShortcutAndWidgetContainer> getAllShortcutAndWidgetContainers() {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                new ArrayList<ShortcutAndWidgetContainer>();
        int screenCount = getPageCount();
        for (int screen = 0; screen < screenCount; screen++) {
            childrenLayouts.add(((CellLayout) getPageAt(screen)).getShortcutsAndWidgets());
        }
        if (mLauncher.getHotseat() != null) {
            childrenLayouts.add(mLauncher.getHotseat().getLayout().getShortcutsAndWidgets());
        }
        return childrenLayouts;
    }

    public Folder getFolderForTag(Object tag) {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout: childrenLayouts) {
            int count = layout.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = layout.getChildAt(i);
                if (child instanceof Folder) {
                    Folder f = (Folder) child;
                    if (f.getInfo() == tag && f.getInfo().opened) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public View getViewForTag(Object tag) {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout: childrenLayouts) {
            int count = layout.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = layout.getChildAt(i);
                if (child.getTag() == tag) {
                    return child;
                }
            }
        }
        return null;
    }

    public void clearDropTargets() {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout: childrenLayouts) {
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View v = layout.getChildAt(j);
                if (v instanceof DropTarget) {
                    mDragController.removeDropTarget((DropTarget) v);
                }
            }
        }
    }

    public void removeItems(final List<String> packages) {
        final HashSet<String> packageNames = new HashSet<String>();
        packageNames.addAll(packages);

        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "removeFinalItem: packageNames = " + packageNames);
        }

        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (final CellLayout layoutParent: cellLayouts) {
            final ViewGroup layout = layoutParent.getShortcutsAndWidgets();

            // Avoid ANRs by treating each screen separately
            post(new Runnable() {
                public void run() {
                    final ArrayList<View> childrenToRemove = new ArrayList<View>();
                    childrenToRemove.clear();
                    /// M: added for remove folder.
                    final ArrayList<FolderInfo> delayToRemoveFromFolder = new ArrayList<FolderInfo>();
                    delayToRemoveFromFolder.clear();

                    int childCount = layout.getChildCount();
                    for (int j = 0; j < childCount; j++) {
                        final View view = layout.getChildAt(j);
                        Object tag = view.getTag();

                        if (tag instanceof ShortcutInfo) {
                            final ShortcutInfo info = (ShortcutInfo) tag;
                            final Intent intent = info.intent;
                            final ComponentName name = intent.getComponent();

                            if (name != null) {
                                if (packageNames.contains(name.getPackageName())) {
                                    /// M: we only remove items whose component is in disable state, 
                                    /// this is add to deal the case that there are more than one 
                                    /// activities with LAUNCHER category, and one of them is disabled
                                    /// may cause all activities removed from workspace.
                                    final boolean isComponentEnabled = Util.isComponentEnabled(getContext(), name);
                                    if (Util.ENABLE_DEBUG) {
                                        Util.Log.d(TAG, "removeFinalItem: name = " + name
                                                + ",isComponentEnabled = " + isComponentEnabled);
                                    }
                                    if (!isComponentEnabled) {
                                    	mLauncher.getModel().deleteItemFromDatabase(info);
                                        childrenToRemove.add(view);
                                    }
                                }
                            }
                        } else if (tag instanceof FolderInfo) {
                            final FolderInfo info = (FolderInfo) tag;
                            final ArrayList<ShortcutInfo> contents = info.contents;
                            final int contentsCount = contents.size();
                            final ArrayList<ShortcutInfo> appsToRemoveFromFolder =
                                    new ArrayList<ShortcutInfo>();

                            /// M: If the folder will be removed completely, delay to remove, else remove folder items.
                            if (isNeedToDelayRemoveFolderItems(info, packageNames, appsToRemoveFromFolder)) {
                                delayToRemoveFromFolder.add(info);
                            } else {
                            	removeFolderItems(info, appsToRemoveFromFolder);
                            }
                        } else if (tag instanceof LauncherAppWidgetInfo) {
                            final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) tag;
                            final ComponentName provider = info.providerName;
                            if (provider != null) {
                                if (packageNames.contains(provider.getPackageName())) {
                                	mLauncher.getModel().deleteItemFromDatabase(info);
                                    childrenToRemove.add(view);
                                }
                            }
                        }
                    }
                    
                    /// M: Remove folder.
                    int delayFolderCount = delayToRemoveFromFolder.size();
                    for (int j = 0; j < delayFolderCount; j++) {
                        FolderInfo info = delayToRemoveFromFolder.get(j);
                        final ArrayList<ShortcutInfo> appsToRemoveFromFolder = new ArrayList<ShortcutInfo>();
                        getRemoveFolderItems(info, packageNames, appsToRemoveFromFolder);
                        removeFolderItems(info, appsToRemoveFromFolder);
                    }

                    childCount = childrenToRemove.size();
                    for (int j = 0; j < childCount; j++) {
                        View child = childrenToRemove.get(j);
                        // Note: We can not remove the view directly from CellLayoutChildren as this
                        // does not re-mark the spaces as unoccupied.
                        layoutParent.removeViewInLayout(child);
                        if (child instanceof DropTarget) {
                            mDragController.removeDropTarget((DropTarget)child);
                        }
                    }

                    if (childCount > 0) {
                        layout.requestLayout();
                        layout.invalidate();
                    }
                }
            });
        }

        // Clean up new-apps animation list
        final Context context = getContext();
        post(new Runnable() {
            @Override
            public void run() {
                //String spKey = LauncherApplication.getSharedPreferencesKey();
                SharedPreferences sp = mLauncher.getSharedPreferences();/*context.getSharedPreferences(spKey,
                        Context.MODE_PRIVATE);*/
                Set<String> newApps = sp.getStringSet(IGlobalStaticFunc.NEW_APPS_LIST_KEY,
                        null);

                    // Remove all queued items that match the same package
                    if (newApps != null) {
                        synchronized (newApps) {
                            Iterator<String> iter = newApps.iterator();
                            while (iter.hasNext()) {
                                try {
                                    Intent intent = Intent.parseUri(iter.next(), 0);
                                    String pn = ItemInfo.getPackageName(intent);
                                    if (packageNames.contains(pn)) {
                                        iter.remove();
                                    }

	                                // It is possible that we've queued an item to be loaded, yet it has
	                                // not been added to the workspace, so remove those items as well.
	                                ArrayList<ItemInfo> shortcuts;
	                                shortcuts = mLauncher.getModel().getWorkspaceShortcutItemInfosWithIntent(
	                                        intent);
	                                for (ItemInfo info : shortcuts) {
	                                	mLauncher.getModel().deleteItemFromDatabase(info);
	                                }
                                } catch (URISyntaxException e) {}
                            }
                        }
                    }
                }
        });
    }

    public void updateShortcuts(List<ApplicationInfo> apps) {
        if (Util.DEBUG_LOADERS) {
            Util.Log.i(TAG, "updateShortcuts: apps = " + apps.size());
            for(ApplicationInfo info : apps){
                Util.Log.d(TAG, "updateShortcuts: info = " + info);
            }
        }
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout: childrenLayouts) {
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                final View view = layout.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof ShortcutInfo) {
                    if(true){
                        updateShortcuts(apps, (MtpShortcutView)view);
                    } else {
                        ShortcutInfo info = (ShortcutInfo) tag;
                        // We need to check for ACTION_MAIN otherwise getComponent() might
                        // return null for some shortcuts (for instance, for shortcuts to
                        // web pages.)
                        final Intent intent = info.intent;
                        final ComponentName name = intent.getComponent();
                        if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                                || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) &&
                                Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
                            final int appCount = apps.size();
                            for (int k = 0; k < appCount; k++) {
                                ApplicationInfo app = apps.get(k);
                                if (app.componentName.equals(name)) {
                                    MtpShortcutView shortcut = (MtpShortcutView) view;
                                    info.updateIcon(mIconCache);
                                    info.title = app.title.toString();
                                    shortcut.applyFromShortcutInfo(info, mIconCache);
                                    if(info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT){
                                        mLauncher.getModel().updateItemInDatabase(info);
                                    }
                                }
                            }
                        }
                    }
                } else if(tag instanceof FolderInfo){
                    updateShortcuts(apps, (FolderIcon)view);
                }
            }
        }
    }
    
    private void updateShortcuts(List<ApplicationInfo> apps, FolderIcon view) {
        Folder folder = view.getFolder();
        if(folder != null){
            int count = folder.getItemCount();
            for(int i=0; i<count; i++){
                View child = folder.getItemAt(i);
                Object tag = child.getTag();
                if (tag instanceof ShortcutInfo) {
                    updateShortcuts(apps, (MtpShortcutView)child);
                }
            }
        }
    }
    
    private void updateShortcuts(List<ApplicationInfo> apps, MtpShortcutView shortcut) {
        ShortcutInfo info = (ShortcutInfo) shortcut.getTag();
        // We need to check for ACTION_MAIN otherwise getComponent() might
        // return null for some shortcuts (for instance, for shortcuts to
        // web pages.)
        final Intent intent = info.intent;
        final ComponentName name = intent.getComponent();

        if (Util.DEBUG_LOADERS) {
            Util.Log.d(TAG, "updateShortcuts 000=def:"+info.usingFallbackIcon
                    +", "+intent);
        }
        if (name != null && (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) &&
                Intent.ACTION_MAIN.equals(intent.getAction())) {
            
            if (Util.DEBUG_LOADERS) {
                Util.Log.d(TAG, "updateShortcuts 111=def:"+info.usingFallbackIcon
                        +", "+info);
            }
            final int appCount = apps.size();
            for (int k = 0; k < appCount; k++) {
                ApplicationInfo app = apps.get(k);
                if (name.equals(app.componentName)) {
                    if (Util.DEBUG_LOADERS) {
                        Util.Log.w(TAG, "updateShortcuts: finded = " + name);
                    }
                    //MtpShortcutView shortcut = (MtpShortcutView) view;
                    info.updateIcon(mIconCache);
                    info.title = app.title.toString();
                    shortcut.applyFromShortcutInfo(info, mIconCache);
                    if(info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT){
                        mLauncher.getModel().updateItemInDatabase(info);
                    }
                    return;
                }
            }
            
            if(info.usingFallbackIcon){
                info.updateIcon(mIconCache);
                if (Util.DEBUG_LOADERS) {
                    Util.Log.d(TAG, "updateShortcuts 222=def:"+info.usingFallbackIcon
                            +", "+name);
                }
                if(!info.usingFallbackIcon){
                    shortcut.applyFromShortcutInfo(info, mIconCache);
                    if(info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT){
                        mLauncher.getModel().updateItemInDatabase(info);
                    }
                }
            }
        }
    }

    public void moveToDefaultScreen(boolean animate) {
    	final int def = getDefaultScreenIndex();
    	if (Util.DEBUG_ANIM) {
            Util.Log.d(TAG, "moveToDefaultScreen(start) = def:"+def
                    +"==animate:"+animate
                    +"==isSmall:"+isSmall()
                    +"=="+this);
        }
        if (!isSmall()) {
            if (animate) {
                snapToPage(def);
            } else {
                setCurrentPage(def);
            }
        }
        getPageAt(def).requestFocus();
    }

    @Override
    public void syncPages() {
    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
    }

    @Override
    protected String getCurrentPageDescription() {
        int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        return String.format(mDefaultScrollFormat, //getContext().getString(R.string.workspace_scroll_format),
                page + 1, getPageCount());
    }

    public void getLocationInDragLayer(int[] loc) {
        mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }

    public void setFadeForOverScroll(float fade) {
        if (!isScrollingIndicatorEnabled()) return;

//        mOverscrollFade = fade;
//        float reducedFade = 0.5f + 0.5f * (1 - fade);
//        final ViewGroup parent = (ViewGroup) getParent();
//        final ImageView qsbDivider = (ImageView) (parent.findViewById(R.id.qsb_divider));
//        final ImageView dockDivider = (ImageView) (parent.findViewById(R.id.dock_divider));
        final View scrollIndicator = getScrollingIndicator();
        if(scrollIndicator == null)
        	return;
        cancelScrollingIndicatorAnimations();
//        if (qsbDivider != null) qsbDivider.setAlpha(reducedFade);
//        if (dockDivider != null) dockDivider.setAlpha(reducedFade);
        scrollIndicator.setAlpha(1 - fade);
    }
    
    /**
     * M: Update unread number of shortcuts and folders in workspace and hotseat.
     */
    public void updateShortcutsAndFoldersUnread() {
        if (Util.DEBUG_UNREAD) {
            Util.Log.d(TAG, "updateShortcutsAndFolderUnread: this = " + this);
        }
        final ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
        int childCount = 0;
        View view = null;
        Object tag = null;
        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                view = layout.getChildAt(j);
                tag = view.getTag();
                if (Util.DEBUG_UNREAD) {
                    Util.Log.d(TAG, "updateShortcutsAndFoldersUnread: tag = " + tag + ", j = "
                            + j + ", view = " + view);
                }
                if (tag instanceof ShortcutInfo) {
                    final ShortcutInfo info = (ShortcutInfo) tag;
                    final Intent intent = info.intent;
                    final ComponentName componentName = intent.getComponent();
//                    ((MtpShortcutView) view).updateShortcutUnreadNum(MtpUnreadLoader
//                            .getUnreadNumberOfComponent(componentName));
                } else if (tag instanceof FolderInfo) {
                    ((FolderIcon) view).updateFolderUnreadNum();
                }
            }
        }
    }

    /**
     * M: Update unread number of shortcuts and folders in workspace and hotseat
     * with the given component.
     * 
     * @param component
     * @param unreadNum
     */
    public void updateComponentUnreadChanged(ComponentName component, int unreadNum) {
        if (Util.DEBUG_UNREAD) {
            Util.Log.d(TAG, "updateComponentUnreadChanged: component = " + component
                    + ", unreadNum = " + unreadNum);
        }
        final ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
        int childCount = 0;
        View view = null;
        Object tag = null;
        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                view = layout.getChildAt(j);
                tag = view.getTag();
                if (Util.DEBUG_UNREAD) {
                    Util.Log.d(TAG, "updateComponentUnreadChanged: component = " + component
                            + ",tag = " + tag + ",j = " + j + ",view = " + view);
                }
                if (tag instanceof ShortcutInfo) {
                    final ShortcutInfo info = (ShortcutInfo) tag;
                    final Intent intent = info.intent;
                    final ComponentName componentName = intent.getComponent();
                    if (Util.DEBUG_UNREAD) {
                        Util.Log.d(TAG, "updateComponentUnreadChanged 2: find component = "
                                + component + ",intent = " + intent + ",componentName = " + componentName);
                    }
                    if (componentName != null && componentName.equals(component)) {
                        Util.Log.d(TAG, "updateComponentUnreadChanged 1: find component = "
                                + component + ",tag = " + tag + ",j = " + j + ",cellX = "
                                + info.cellX + ",cellY = " + info.cellY);
                        ((MtpShortcutView) view).updateShortcutUnreadNum(unreadNum);
                    }
                } else if (tag instanceof FolderInfo) {
                    ((FolderIcon) view).updateFolderUnreadNum(component, unreadNum);
                }
            }
        }
    }
    
    public void setShortcutLabelVisiable(boolean visiable){
        int pagecount = super.getPageCount();
        for(int i=0; i<pagecount; i++){
            ShortcutAndWidgetContainer container = ((CellLayout)getPageAt(i)).getShortcutsAndWidgets();
            int count = container.getChildCount();
            for(int j=0; j<count; j++){
                final View view = container.getChildAt(j);
                if(view instanceof IShortcutView){
                    ((IShortcutView)view).setViewLabelVisible(visiable);
                }
            }
        }
    }

    /**
     * M: Whether all the items in folder will be removed or not.
     * 
     * @param info
     * @param packageNames
     * @param appsToRemoveFromFolder
     * @return true, all the items in folder will be removed.
     */
    protected boolean isNeedToDelayRemoveFolderItems(FolderInfo info, HashSet<String> packageNames,
            ArrayList<ShortcutInfo> appsToRemoveFromFolder) {
        final ArrayList<ShortcutInfo> contents = info.contents;
        final int contentsCount = contents.size();
        int removeFolderItemsCount = getRemoveFolderItems(info, packageNames, appsToRemoveFromFolder);
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "isNeedToDelayRemoveFolderItems info = " + info + ", packageNames = " + packageNames
                    + ", contentsCount = " + contentsCount + ", removeFolderItemsCount = " + removeFolderItemsCount);
        }

        return (removeFolderItemsCount >= (contentsCount - 1));
    }

    /**
     * M: When uninstall one app, if the foler item is the shortcut of the app, it will be removed.
     * 
     * @param info
     * @param packageNames
     * @param appsToRemoveFromFolder
     * @return the count of the folder items will be removed.
     */
    protected int getRemoveFolderItems(FolderInfo info, HashSet<String> packageNames,
            ArrayList<ShortcutInfo> appsToRemoveFromFolder) {
        final ArrayList<ShortcutInfo> contents = info.contents;
        final int contentsCount = contents.size();

        for (int k = 0; k < contentsCount; k++) {
            final ShortcutInfo appInfo = contents.get(k);
            final Intent intent = appInfo.intent;
            final ComponentName name = intent.getComponent();

            if (name != null && packageNames.contains(name.getPackageName())) {
                appsToRemoveFromFolder.add(appInfo);
            }
        }

        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "getRemoveFolderItems info = " + info + ", packageNames = " + packageNames
                    + ", appsToRemoveFromFolder.size() = " + appsToRemoveFromFolder.size());
        }
        return appsToRemoveFromFolder.size();
    }

    /**
     * M: Remove folder items.
     * 
     * @param info
     * @param appsToRemoveFromFolder
     */
    protected void removeFolderItems(FolderInfo info, ArrayList<ShortcutInfo> appsToRemoveFromFolder) {
        for (ShortcutInfo item : appsToRemoveFromFolder) {
            info.remove(item);
            mLauncher.getModel().deleteItemFromDatabase(item);
        }
    }
    
    public boolean isEnableStaticWallpaper(){
    	return mIsStaticWallpaper;
    }
    
    private void setEnableStaticWallpaper(boolean enable, boolean update){
        if(mIsStaticWallpaper != enable){
        	mIsStaticWallpaper = enable;
        	
        	if(enable){
        	    syncWallpaperOffsetWithScroll();
            	if (mWindowToken != null) {
                    mWallpaperManager.setWallpaperOffsets(mWindowToken,
                            mWallpaperOffset.getFinalX(), mWallpaperOffset.getFinalY());
                }
        	} else {
        	    updateWallpaperOffsets();
        	}
        }
    }
    
    public void setEnableStaticWallpaper(boolean enable){
        setEnableStaticWallpaper(enable, true);
    }
    /*
     * This interpolator emulates the rate at which the perceived scale of an object changes
     * as its distance from a camera increases. When this interpolator is applied to a scale
     * animation on a view, it evokes the sense that the object is shrinking due to moving away
     * from the camera.
     */
    public static class ZInterpolator implements TimeInterpolator {
        private float focalLength;

        public ZInterpolator(float foc) {
            focalLength = foc;
        }

        public float getInterpolation(float input) {
            return (1.0f - focalLength / (focalLength + input)) /
                (1.0f - focalLength / (focalLength + 1.0f));
        }
    }

    /*
     * The exact reverse of ZInterpolator.
     */
    public static class InverseZInterpolator implements TimeInterpolator {
        private ZInterpolator zInterpolator;
        public InverseZInterpolator(float foc) {
            zInterpolator = new ZInterpolator(foc);
        }
        public float getInterpolation(float input) {
            return 1 - zInterpolator.getInterpolation(1 - input);
        }
    }

    /*
     * ZInterpolator compounded with an ease-out.
     */
    public static class ZoomOutInterpolator implements TimeInterpolator {
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(0.75f);
        private final ZInterpolator zInterpolator = new ZInterpolator(0.13f);

        public float getInterpolation(float input) {
            return decelerate.getInterpolation(zInterpolator.getInterpolation(input));
        }
    }

    /*
     * InvereZInterpolator compounded with an ease-out.
     */
    public static class ZoomInInterpolator implements TimeInterpolator {
        private final InverseZInterpolator inverseZInterpolator = new InverseZInterpolator(0.35f);
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(3.0f);

        public float getInterpolation(float input) {
            return decelerate.getInterpolation(inverseZInterpolator.getInterpolation(input));
        }
    }
    
    public class FolderCreationAlarmListener implements OnAlarmListener {
    	public CellLayout layout;
    	public int cellX;
    	public int cellY;

        public FolderCreationAlarmListener(CellLayout layout, int cellX, int cellY) {
            this.layout = layout;
            this.cellX = cellX;
            this.cellY = cellY;
        }

        public void onAlarm(Alarm alarm) {
            if (mDragFolderRingAnimator == null) {
                mDragFolderRingAnimator = new FolderRingAnimator(mLauncher, null);
            }
            mDragFolderRingAnimator.setCell(cellX, cellY);
            mDragFolderRingAnimator.setCellLayout(layout);
            mDragFolderRingAnimator.animateToAcceptState();
            layout.showFolderAccept(mDragFolderRingAnimator);
            layout.clearDragOutlines();
            setDragMode(DRAG_MODE_CREATE_FOLDER);
        }
    }

    public class ReorderAlarmListener implements OnAlarmListener {
    	public float[] dragViewCenter;
    	public int minSpanX, minSpanY, spanX, spanY;
    	public DragView dragView;
    	public View child;

        public ReorderAlarmListener(float[] dragViewCenter, int minSpanX, int minSpanY, int spanX,
                int spanY, DragView dragView, View child) {
            this.dragViewCenter = dragViewCenter;
            this.minSpanX = minSpanX;
            this.minSpanY = minSpanY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.child = child;
            this.dragView = dragView;
        }

        public void onAlarm(Alarm alarm) {
            int[] resultSpan = new int[2];
            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], spanX, spanY, mDragTargetLayout, mTargetCell);
            mLastReorderX = mTargetCell[0];
            mLastReorderY = mTargetCell[1];

            mTargetCell = mDragTargetLayout.createArea((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY,
                child, mTargetCell, resultSpan, CellLayout.MODE_DRAG_OVER);

            if (mTargetCell[0] < 0 || mTargetCell[1] < 0) {
                mDragTargetLayout.revertTempState();
            } else {
                setDragMode(DRAG_MODE_REORDER);
            }

            boolean resize = resultSpan[0] != spanX || resultSpan[1] != spanY;
            mDragTargetLayout.visualizeDropLocation(child, mDragOutline,
                (int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1],
                mTargetCell[0], mTargetCell[1], resultSpan[0], resultSpan[1], resize,
                dragView.getDragVisualizeOffset(), dragView.getDragRegion());
        }
    }
    
    public class WallpaperOffsetInterpolator {
    	public float mFinalHorizontalWallpaperOffset = 0.0f;
    	public float mFinalVerticalWallpaperOffset = 0.5f;
    	public float mHorizontalWallpaperOffset = 0.0f;
    	public float mVerticalWallpaperOffset = 0.5f;
    	public long mLastWallpaperOffsetUpdateTime;
        public boolean mIsMovingFast;
        public boolean mOverrideHorizontalCatchupConstant;
        public float mHorizontalCatchupConstant = 0.35f;
        public float mVerticalCatchupConstant = 0.35f;

        public WallpaperOffsetInterpolator() {
        }

        public void setOverrideHorizontalCatchupConstant(boolean override) {
            mOverrideHorizontalCatchupConstant = override;
        }

        public void setHorizontalCatchupConstant(float f) {
            mHorizontalCatchupConstant = f;
        }

        public void setVerticalCatchupConstant(float f) {
            mVerticalCatchupConstant = f;
        }

        public boolean computeScrollOffset() {
            if (Float.compare(mHorizontalWallpaperOffset, mFinalHorizontalWallpaperOffset) == 0 &&
                    Float.compare(mVerticalWallpaperOffset, mFinalVerticalWallpaperOffset) == 0) {
                mIsMovingFast = false;
                return false;
            }
            boolean isLandscape = mDisplaySize.x > mDisplaySize.y;

            long currentTime = System.currentTimeMillis();
            long timeSinceLastUpdate = currentTime - mLastWallpaperOffsetUpdateTime;
            timeSinceLastUpdate = Math.min((long) (1000 / 30f), timeSinceLastUpdate);
            timeSinceLastUpdate = Math.max(1L, timeSinceLastUpdate);

            float xdiff = Math.abs(mFinalHorizontalWallpaperOffset - mHorizontalWallpaperOffset);
            if (!mIsMovingFast && xdiff > 0.07) {
                mIsMovingFast = true;
            }

            float fractionToCatchUpIn1MsHorizontal;
            if (mOverrideHorizontalCatchupConstant) {
                fractionToCatchUpIn1MsHorizontal = mHorizontalCatchupConstant;
            } else if (mIsMovingFast) {
                fractionToCatchUpIn1MsHorizontal = isLandscape ? 0.5f : 0.75f;
            } else {
                // slow
                fractionToCatchUpIn1MsHorizontal = isLandscape ? 0.27f : 0.5f;
            }
            float fractionToCatchUpIn1MsVertical = mVerticalCatchupConstant;

            fractionToCatchUpIn1MsHorizontal /= 33f;
            fractionToCatchUpIn1MsVertical /= 33f;

            final float UPDATE_THRESHOLD = 0.00001f;
            float hOffsetDelta = mFinalHorizontalWallpaperOffset - mHorizontalWallpaperOffset;
            float vOffsetDelta = mFinalVerticalWallpaperOffset - mVerticalWallpaperOffset;
            boolean jumpToFinalValue = Math.abs(hOffsetDelta) < UPDATE_THRESHOLD &&
                Math.abs(vOffsetDelta) < UPDATE_THRESHOLD;

            // Don't have any lag between workspace and wallpaper on non-large devices
            if (!Launcher.isScreenLarge() || jumpToFinalValue) {
                mHorizontalWallpaperOffset = mFinalHorizontalWallpaperOffset;
                mVerticalWallpaperOffset = mFinalVerticalWallpaperOffset;
            } else {
                float percentToCatchUpVertical =
                    Math.min(1.0f, timeSinceLastUpdate * fractionToCatchUpIn1MsVertical);
                float percentToCatchUpHorizontal =
                    Math.min(1.0f, timeSinceLastUpdate * fractionToCatchUpIn1MsHorizontal);
                mHorizontalWallpaperOffset += percentToCatchUpHorizontal * hOffsetDelta;
                mVerticalWallpaperOffset += percentToCatchUpVertical * vOffsetDelta;
            }

            mLastWallpaperOffsetUpdateTime = System.currentTimeMillis();
            return true;
        }

        public float getCurrX() {
            return mHorizontalWallpaperOffset;
        }

        public float getFinalX() {
            return mFinalHorizontalWallpaperOffset;
        }

        public float getCurrY() {
            return mVerticalWallpaperOffset;
        }

        public float getFinalY() {
            return mFinalVerticalWallpaperOffset;
        }

        public void setFinalX(float x) {
            mFinalHorizontalWallpaperOffset = Math.max(0f, Math.min(x, 1.0f));
        }

        public void setFinalY(float y) {
            mFinalVerticalWallpaperOffset = Math.max(0f, Math.min(y, 1.0f));
        }

        public void jumpToFinal() {
            mHorizontalWallpaperOffset = mFinalHorizontalWallpaperOffset;
            mVerticalWallpaperOffset = mFinalVerticalWallpaperOffset;
        }
    }
    /*
    protected void onPageScrolled(int leftpage, int rightpage, float offset, int offsetPixels) {
    	if(Util.DEBUG_ANIM){
    		Util.Log.i(TAG, "onPageScrolled()==left:"+((CellLayout)getPageAt(leftpage)).getShortcutsAndWidgets().getLayerType()
    			+", right:"+((CellLayout)getPageAt(rightpage)).getShortcutsAndWidgets().getLayerType()
    			+", leftpage:"+leftpage
    			+", rightpage:"+rightpage
    			+", hard:"+View.LAYER_TYPE_HARDWARE
    			+", soft:"+View.LAYER_TYPE_SOFTWARE
    			+", none:"+View.LAYER_TYPE_NONE);
    	}
    	
    	super.onPageScrolled(leftpage, rightpage, offset, offsetPixels);
    }*/
    
    protected boolean pageScrolled(int xpos, boolean compelete) {
        
        if (mState != State.NORMAL) {
            //onPageScrolled(getCurrentPage(), 0, 0, 0);
            return false;
        }
        
        if(true){
            return super.pageScrolled(xpos, compelete);
        }
        
        final int width = getWidth();
        final int widthWithMargin = width + mPageSpacing;
        
        int left, right;
        //int [] pageindex = new int[2];
        //getVisiblePages(pageindex);
        int currentPage;// = getCurrentPage();
        final float scrollOffset;
        if(xpos < 0){
            if(isSupportCycleSlidingScreen()){
                left = getPageCount()-1;
                right = 0;
            } else {
                left = -1;
                right = -1;
            }
            scrollOffset = width > 0 ? (float) (widthWithMargin+xpos) / widthWithMargin : 0;
        } else {
            left = xpos / widthWithMargin;
            right = left+1;
            if(isSupportCycleSlidingScreen()){
                if(right >= getPageCount())
                    right = 0;
            } else {
                if(right >= getPageCount())
                    right = -1;
                if(xpos >= mMaxScrollX){
                    xpos = mMaxScrollX;
                }
            }
            if(left >= 0){
                scrollOffset = width > 0 ? (float) (xpos - getChildOffset(left)) / widthWithMargin : 0;
            } else {
                scrollOffset = 0;
            }
        }
        
        final float marginOffset = (float) mPageSpacing / width;
        final float pageOffset = scrollOffset / (1.0f + marginOffset);
        final int offsetPixels = (int) (pageOffset * widthWithMargin);
        if(false){
            android.util.Log.i("QsLog", "pageScrolled("+xpos+"),width:"+width
                    +", page:"+"["+left+","+right+"]"+getCurrentPage()
                    +", space:"+mPageSpacing
                    +", marset:"+String.format("%.4f", marginOffset)
                    +", offset:"+String.format("%.4f", pageOffset)
                    +", offsetPx:"+offsetPixels
                    +", Childset:"+(left >= 0 ? getChildOffset(left) : 0)
                    +", relset:"+(left >= 0 ? getRelativeChildOffset(left) : 0)
                    +", mState:"+mState
                    +", TouchState:"+mTouchState);
        }

        onPageScrolled(left, right, pageOffset, offsetPixels);
        
        return true;
    }
    
    
    
}
