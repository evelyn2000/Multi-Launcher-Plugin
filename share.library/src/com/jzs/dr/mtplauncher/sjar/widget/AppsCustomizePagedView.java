package com.jzs.dr.mtplauncher.sjar.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.jzs.common.launcher.IIconCache;
import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.dr.mtplauncher.sjar.ctrl.AppsCustomizeAsyncTask;
import com.jzs.dr.mtplauncher.sjar.ctrl.AsyncTaskCallback;
import com.jzs.dr.mtplauncher.sjar.ctrl.AsyncTaskPageData;
import com.jzs.dr.mtplauncher.sjar.ctrl.BitmapCache;
import com.jzs.dr.mtplauncher.sjar.ctrl.CanvasCache;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragController;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget.DragObject;
import com.jzs.dr.mtplauncher.sjar.ctrl.FocusHelper;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherAnimUtils;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherTransitionable;
import com.jzs.dr.mtplauncher.sjar.ctrl.PaintCache;
import com.jzs.dr.mtplauncher.sjar.ctrl.RectCache;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.dr.mtplauncher.sjar.model.IconCache;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherModel;
import com.jzs.dr.mtplauncher.sjar.model.LauncherSettings;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.animation.AccelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.LauncherApplication;
import com.jzs.dr.mtplauncher.sjar.R;

public abstract class AppsCustomizePagedView extends PagedViewWithDraggableItems implements
					View.OnClickListener, View.OnKeyListener, DragSource,
					PagedViewIcon.PressedCallback, PagedViewWidget.ShortPressListener,
					LauncherTransitionable{
	private static final String TAG = "AppsCustomizePagedView";
	
	public static final int WIDGET_NO_CLEANUP_REQUIRED = -1;
    public static final int WIDGET_PRELOAD_PENDING = 0;
    public static final int WIDGET_BOUND = 1;
    public static final int WIDGET_INFLATED = 2;
	
	public final static int CONTENTTYPE_UNKOWN = 0;
	public final static int CONTENTTYPE_APPS_ALL = 0x01;
    public final static int CONTENTTYPE_WIDGETS = 0x02;
    
    public final static int CONTENTTYPE_APPS_FREQUENT = (0x10|CONTENTTYPE_APPS_ALL);
    public final static int CONTENTTYPE_APPS_DOWNLOAD = (0x20|CONTENTTYPE_APPS_ALL);
    
    public final static int CONTENTTYPE_WIDGETS_SHORTCUT = (0x40|CONTENTTYPE_WIDGETS);
    
//    public final static int CONTENTTYPE_SUPPORT_APPS = (CONTENTTYPE_APPS_FREQUENT
//    													|CONTENTTYPE_APPS_DOWNLOAD
//    													|CONTENTTYPE_APPS_ALL);
    
    public final static int CONTENTTYPE_SUPPORT_APPS_AND_WIDGET = (CONTENTTYPE_APPS_ALL | CONTENTTYPE_WIDGETS);
    
    private final int mSupportContentType;
    protected int mCurrentContentType;
    
    // Content
    protected List<ApplicationInfo> mApps;
    protected ArrayList<Object> mWidgets;
    protected int mNumAppsPages;
    protected int mNumWidgetPages;
    
    protected Launcher mLauncher;
    protected DragController mDragController;
    
    protected IIconCache mIconCache;
    protected final LayoutInflater mLayoutInflater;
    protected final PackageManager mPackageManager;
    
    protected static final int sPageSleepDelay = 200;
    protected final float sWidgetPreviewIconPaddingPercentage = 0.25f;
    
    protected Rect mTmpRect = new Rect();
    protected PagedViewIcon mPressedIcon;
    
    // Deferral of loading widget previews during launcher transitions
    protected boolean mInTransition;
 // Save and Restore
    protected int mSaveInstanceStateItemIndex = -1;
    
	// Dimens
    protected int mContentWidth;
    protected int mAppIconSize;
    protected int mMaxAppCellCountX, mMaxAppCellCountY;
    protected int mWidgetCountX, mWidgetCountY;
    protected int mWidgetWidthGap, mWidgetHeightGap;
    
    protected PagedViewCellLayout mWidgetSpacingLayout;
    
    protected final ArrayList<AppsCustomizeAsyncTask> mRunningTasks;
    protected final ArrayList<AsyncTaskPageData> mDeferredSyncWidgetPageItems;
    protected final ArrayList<Runnable> mDeferredPrepareLoadWidgetPreviewsTasks;
    
 // Used for drawing shortcut previews
    public final BitmapCache mCachedShortcutPreviewBitmap = new BitmapCache();
    public final PaintCache mCachedShortcutPreviewPaint = new PaintCache();
    public final CanvasCache mCachedShortcutPreviewCanvas = new CanvasCache();

    // Used for drawing widget previews
    public final CanvasCache mCachedAppWidgetPreviewCanvas = new CanvasCache();
    public final RectCache mCachedAppWidgetPreviewSrcRect = new RectCache();
    public final RectCache mCachedAppWidgetPreviewDestRect = new RectCache();
    public final PaintCache mCachedAppWidgetPreviewPaint = new PaintCache();
    
    protected Toast mWidgetInstructionToast;
  /// M: Flag to record whether the app list data has been set to AppsCustomizePagedView.  
    protected boolean mAppsHasSet = false;
    protected boolean mDraggingWidget = false;
    private boolean mSlideAppAndWidgetTogether = true;
    
	public AppsCustomizePagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
	}
	
	public AppsCustomizePagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        mLayoutInflater = LayoutInflater.from(context);
        mPackageManager = context.getPackageManager();
        
        mLauncher = (Launcher)context;
        mIconCache = mLauncher.getIconCache();

        final IResConfigManager res = mLauncher.getResConfigManager();
        mAppIconSize = res.getDimensionPixelSize(IResConfigManager.DIM_APP_ICON_SIZE);//R.dimen.app_icon_size);
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppsCustomizePagedView, 0, 0);
        mSupportContentType = a.getInt(R.styleable.AppsCustomizePagedView_ContentType, CONTENTTYPE_SUPPORT_APPS_AND_WIDGET);
        mMaxAppCellCountX = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountX, -1);
        mMaxAppCellCountY = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountY, -1);
        mWidgetWidthGap =
            a.getDimensionPixelSize(R.styleable.AppsCustomizePagedView_widgetCellWidthGap, 0);
        mWidgetHeightGap =
            a.getDimensionPixelSize(R.styleable.AppsCustomizePagedView_widgetCellHeightGap, 0);
        mWidgetCountX = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountX, 2);
        mWidgetCountY = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountY, 2);
//        mClingFocusedX = a.getInt(R.styleable.AppsCustomizePagedView_clingFocusedX, 0);
//        mClingFocusedY = a.getInt(R.styleable.AppsCustomizePagedView_clingFocusedY, 0);
        a.recycle();
        
        mRunningTasks = new ArrayList<AppsCustomizeAsyncTask>();
		if(isSupportWidget()){
			mDeferredSyncWidgetPageItems = new ArrayList<AsyncTaskPageData>();
			mDeferredPrepareLoadWidgetPreviewsTasks = new ArrayList<Runnable>();
			mWidgets = new ArrayList<Object>();
		} else {
			mDeferredSyncWidgetPageItems = null;
			mDeferredPrepareLoadWidgetPreviewsTasks = null;
		}
		
		if(isSupportApps()){
			mCurrentContentType = CONTENTTYPE_APPS_ALL;
			mApps = new ArrayList<ApplicationInfo>();
		}
		
		final ISharedPrefSettingsManager sharedPrefManager = mLauncher.getSharedPrefSettingsManager();
		setSupportCycleSlidingScreen(sharedPrefManager.getAppsSupportCycleSliding());
        setSlideAppAndWidgetTogether(sharedPrefManager.getSlideAppAndWidgetTogether());
		
		mWidgetSpacingLayout = (PagedViewCellLayout)res.inflaterView(IResConfigManager.LAYOUT_APPS_CUSTOMIZE_PAGE_SCREEN, this, false);
				//mLayoutInflater.inflate(R.layout.apps_customize_page_screen, this, false);
		
		// The padding on the non-matched dimension for the default widget preview icons
        // (top + bottom)
        mFadeInAdjacentScreens = false;

        // Unless otherwise specified this view is important for accessibility.
        if (getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
	}
	
	@Override
    protected void init() {
        super.init();
        mCenterPagesVertically = false;
        Launcher launcher = (Launcher)getContext();
        final IResConfigManager res = launcher.getResConfigManager();
//        Context context = getContext();
//        Resources r = context.getResources();
        //setDragSlopeThreshold(r.getInteger(R.integer.config_appsCustomizeDragSlopeThreshold)/100f);
        setDragSlopeThreshold(res.getInteger(IResConfigManager.CONFIG_APPS_CUSTOMIZE_DRAGSLOPETHRESHOLD)/100f);
    }
	
//	public int getAppsPageCount(){
//		if(!isSlideAppAndWidgetTogether())
//			return mNumAppsPages;
//		return super.getAppsPageCount();
//    }
	
	public boolean isSlideAppAndWidgetTogether(){
		return mSlideAppAndWidgetTogether;
	}
	
	public void setSlideAppAndWidgetTogether(boolean value){
		if(mSlideAppAndWidgetTogether != value)
			mSlideAppAndWidgetTogether = value;
	}
	
	public boolean isSupportWidget(){
    	return ((mSupportContentType&CONTENTTYPE_WIDGETS) > 0);
    }
	
	public boolean isSupportApps(){
    	return ((mSupportContentType&CONTENTTYPE_APPS_ALL) > 0);
    }
    
    public int getSupportContentType(){
    	return mSupportContentType;
    }
    
    public int getCurrentContentType(){
    	return mCurrentContentType;
    }
    
    public int getContentPageCount(){
    	if (mCurrentContentType == CONTENTTYPE_APPS_ALL) {
            return mNumAppsPages;
        } else if(mCurrentContentType == CONTENTTYPE_WIDGETS){
			return mNumWidgetPages;
		}
    	return getPageCount();
    }
//    @Override
//    public void scrollTo(int x, int y) {
//    	if(isSlideAppAndWidgetTogether()){
//    		super.scrollTo(x, y);
//    		return;
//    	}
//    	
//        mUnboundedScrollX = x;
//
//        if (x < mMinScrollX) {
//        	if (isSupportCycleSlidingScreen()) {
//            	mOverScrollX = mMinScrollX;
//            	super.scrollTo(x, y);
//            } else {
//	            super.scrollTo(mMinScrollX, y);
//	            if (mAllowOverScroll) {
//	                overScroll(x);
//	            }
//            }
//        } else if (x > mMaxScrollX) {
//        	if (isSupportCycleSlidingScreen()) {        		
//            	mOverScrollX = mMinScrollX;
//            	super.scrollTo(x, y);
//            } else {
//	            super.scrollTo(mMaxScrollX, y);
//	            if (mAllowOverScroll) {
//	                overScroll(x - mMaxScrollX);
//	            }
//            }
//        } else {
//            mOverScrollX = x;
//            super.scrollTo(x, y);
//        }
//
//        mTouchX = x;
//        mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
//    }
    @Override
    public int indexToPage(int index) {
    	if(index >= 0 && isSupportWidget() && isSupportApps())
    		return getChildCount() - index - 1;
    	return super.indexToPage(index);
    }
    @Override
    public void getVisiblePages(int[] range) {
    	if(isSlideAppAndWidgetTogether()){
    		super.getVisiblePages(range);
    		return;
    	}
        final int pageCount = getPageCount();

        if (pageCount > 0) {
    		final int screenWidth = getMeasuredWidth();
            int leftScreen = 0;
            int rightScreen = 0;

            View currPage = getPageAt(leftScreen);
            int fadePageCount = pageCount;
            if(getCurrentPage() < mNumAppsPages){
            	fadePageCount = mNumAppsPages;
            }
            
            while (leftScreen < fadePageCount - 1 &&
                    currPage.getX() + currPage.getWidth()/* -
                    currPage.getPaddingRight()*/ < getScrollX()) {
                leftScreen++;
                currPage = getPageAt(leftScreen);
            }
            rightScreen = leftScreen;
            currPage = getPageAt(rightScreen + 1);
            while (rightScreen < fadePageCount - 1 &&
                    currPage.getX()/* - currPage.getPaddingLeft()*/ < getScrollX() + screenWidth) {
                rightScreen++;
                currPage = getPageAt(rightScreen + 1);
            }
//            android.util.Log.v("QsLog", "====leftScreen:"+leftScreen
//                    +"===rightScreen:"+rightScreen
//                    +"==curX:"+getScrollX()
//                    +"==minX:"+mMinScrollX
//                    +"==maxX:"+mMaxScrollX);
            if(isSupportCycleSlidingScreen()){
	            if(getCurrentPage() < mNumAppsPages){
	            	// in apps
	            	if((leftScreen == mNumAppsPages-1) && (rightScreen >= mNumAppsPages-1)){
	            		rightScreen = 0;
	            	} else if(leftScreen == rightScreen && rightScreen == 0){
	            		leftScreen = mNumAppsPages-1;
	            	}
	            } else {
	            	// in widget pages
	            	if(leftScreen == pageCount-1)
	            		rightScreen = mNumAppsPages;
	            	if(rightScreen == mNumAppsPages && leftScreen == mNumAppsPages-1)
	            		leftScreen = pageCount-1;
	            }
            }
//            android.util.Log.i("QsLog", "====leftScreen:"+leftScreen
//                    +"===rightScreen:"+rightScreen);
            range[0] = leftScreen;
            range[1] = rightScreen;

        } else {
            range[0] = -1;
            range[1] = -1;
        }
    }
    @Override
    protected void dispatchDrawCycleChildView(Canvas canvas, final int pageCount, final int leftScreen, final int rightScreen){
    	if(isSlideAppAndWidgetTogether()){
    		super.dispatchDrawCycleChildView(canvas, pageCount, leftScreen, rightScreen);
    		return;
    	}
    	
    	int width = getWidth();
		final int max, min, offset;
		if(mCurrentPage < mNumAppsPages){
			// in apps
			offset = mNumAppsPages * width;
			max = offset - width;
			min = mMinScrollX;
		} else {
			// in widgets
			offset = (pageCount - mNumAppsPages) * width;
			max = mMaxScrollX;
			min = mMinScrollX;//offset;
		}
		
		final long drawingTime = getDrawingTime();
		if (getScrollX() >= max) {
		    View child = getPageAt(leftScreen);
            if(shouldDrawChild(child))
                drawChild(canvas, child, drawingTime);
            
			canvas.translate(offset, 0);
			
			child = getPageAt(rightScreen);
            if(shouldDrawChild(child))
                drawChild(canvas, child, drawingTime);
			//canvas.translate(-offset, 0);
		} else if (getScrollX() <= min) {
		    View child = getPageAt(rightScreen);
            if(shouldDrawChild(child))
                drawChild(canvas, child, drawingTime);

			canvas.translate(-offset, 0);
			
			child = getPageAt(leftScreen);
            if(shouldDrawChild(child))
                drawChild(canvas, child, drawingTime);
			//canvas.translate(+offset, 0);
		}
    }
    @Override
    protected int getSnapToPageSlideDelta(int whichPage, int newX){
    	if(isSlideAppAndWidgetTogether()){
    		return super.getSnapToPageSlideDelta(whichPage, newX);
    	}
    	
    	int delta = newX - mUnboundedScrollX;    	
    	if (isSupportCycleSlidingScreen()) {
    		final int pageWidth = getScaledMeasuredWidth(getPageAt(0));
        	int width = getWidth();
        	int childCount = getPageCount();
        	int offset;
        	int AppOffset = (mNumAppsPages - 1) * width;
    		if(mCurrentPage < mNumAppsPages){
    			// all apps
    			offset = AppOffset;
    			if(mUnboundedScrollX > offset && mCurrentPage == (mNumAppsPages - 1) && whichPage == 0){
    				mUnboundedScrollX = offset - mUnboundedScrollX;
    				if(newX == 0)
    					delta = width + mUnboundedScrollX;
    				mUnboundedScrollX = -delta;
    			} else if(mUnboundedScrollX < 0 && mCurrentPage == 0 && whichPage == (mNumAppsPages - 1)){
    				int temp = mUnboundedScrollX;
    				mUnboundedScrollX = offset + width + mUnboundedScrollX;
    				delta = -(width + temp);
    			}
    		} else {
    			// in widgets
    			offset = (childCount-1) * width;
    			if(mUnboundedScrollX > offset && mCurrentPage == (childCount - 1) && whichPage == mNumAppsPages){
    				int temp = mUnboundedScrollX = offset - mUnboundedScrollX;
    				mUnboundedScrollX = AppOffset - mUnboundedScrollX;
    				delta = width + temp;
    			} else if(mUnboundedScrollX < (AppOffset + width)  && mCurrentPage == mNumAppsPages && whichPage == (childCount-1)){
    				int temp = AppOffset + width - mUnboundedScrollX;
    				delta = temp - width;
    				mUnboundedScrollX = offset + (-delta);
    			}
    		}
        } 

    	return delta;
    }
    @Override
    protected int getScrollToRightPage(boolean returnToOriginalPage){
    	if(isSlideAppAndWidgetTogether())
    		return super.getScrollToRightPage(returnToOriginalPage);

    	int finalPage = mCurrentPage;
    	if (mCurrentPage < getPageCount() - 1) {
			if(mCurrentPage != mNumAppsPages-1)
				finalPage = returnToOriginalPage ? mCurrentPage : mCurrentPage + 1;
			else if(isSupportCycleSlidingScreen())
				finalPage = returnToOriginalPage ? mCurrentPage : 0;
    	} else if (isSupportCycleSlidingScreen()){
    		finalPage = returnToOriginalPage ? mCurrentPage : mNumAppsPages;
    	}
    	
    	return finalPage;
    }
    @Override
    protected int getScrollToLeftPage(boolean returnToOriginalPage){
    	if(isSlideAppAndWidgetTogether())
    		return super.getScrollToLeftPage(returnToOriginalPage);
    		
    	int finalPage = mCurrentPage;
    	if (mCurrentPage > 0) {
    		if(mCurrentPage != mNumAppsPages)
				finalPage = returnToOriginalPage ? mCurrentPage : mCurrentPage - 1;
			else if(isSupportCycleSlidingScreen())
				finalPage = returnToOriginalPage ? mCurrentPage : getPageCount() - 1;
    	} else if (isSupportCycleSlidingScreen()){
    		finalPage = mNumAppsPages - 1;
    	}
    	
    	return finalPage;
    }
    @Override
    public void scrollLeft() {
    	if(isSlideAppAndWidgetTogether()){
    		super.scrollLeft();
    		return;
    	}
    	
        if (mScroller.isFinished()) {
        	if (mCurrentPage > 0 && mCurrentPage != mNumAppsPages) {
            	snapToPage(mCurrentPage - 1);
            } else if (isSupportCycleSlidingScreen()) {
            	if (mCurrentPage == 0) {
					snapToPage(mNumAppsPages - 1);
				} else if (mCurrentPage == mNumAppsPages){
					snapToPage(getPageCount() - 1);
				} 
            }
        } else {
        	if (mNextPage > 0 && mNextPage != mNumAppsPages) {
            	snapToPage(mNextPage - 1);
            } else if (isSupportCycleSlidingScreen()) {
            	if (mNextPage == 0) {
					snapToPage(mNumAppsPages - 1);
				} else if (mNextPage == mNumAppsPages){
					snapToPage(getPageCount() - 1);
				}
            }
        }
    }
    @Override
    public void scrollRight() {
    	if(isSlideAppAndWidgetTogether()){
    		super.scrollRight();
    		return;
    	}
        if (mScroller.isFinished()) {
        	if ((mCurrentPage != mNumAppsPages-1) && (mCurrentPage < getPageCount() - 1)) {
            	snapToPage(mCurrentPage + 1);
            } else if (isSupportCycleSlidingScreen()/* && mCurrentPage == getPageCount() - 1*/) {
            	if (mCurrentPage == mNumAppsPages - 1) {
					snapToPage(0);
				} else if (mCurrentPage == getPageCount() - 1){
					snapToPage(mNumAppsPages);
				}
            }
        } else {
        	if ((mNextPage != mNumAppsPages-1) && (mNextPage < getPageCount() - 1)) {
            	snapToPage(mNextPage + 1);
            } else if(isSupportCycleSlidingScreen()/* && mNextPage == getPageCount() - 1*/) {
            	if (mNextPage == mNumAppsPages - 1) {
					snapToPage(0);
				} else if (mNextPage == getPageCount() - 1){
					snapToPage(mNumAppsPages);
				}
            }
        }
    }
    @Override
    protected void notifyPageSwitchListener() {
    	if(isSlideAppAndWidgetTogether()){
    		super.notifyPageSwitchListener();
    	} else if (mPageSwitchListener != null) {
    		if(mCurrentPage < mNumAppsPages)
    			mPageSwitchListener.onPageSwitch(getPageAt(mCurrentPage), mCurrentPage);
    		else
    			mPageSwitchListener.onPageSwitch(getPageAt(mCurrentPage), mCurrentPage - mNumAppsPages);
        }
    }
    @Override
    protected boolean notifyPageCountChangedListener(int count) {
    	if(isSlideAppAndWidgetTogether())
    		return super.notifyPageCountChangedListener(count);
        
        return super.notifyPageCountChangedListener(getContentPageCount());
    }
    @Override
    public void onChildViewRemoved(View parent, View child) {
    	if(isSlideAppAndWidgetTogether())
    		super.onChildViewRemoved(parent, child);
    }
    /**
     * Used by the parent to get the content width to set the tab bar to
     * @return
     */
    public int getPageContentWidth() {
        return mContentWidth;
    }
    
    public void setup(Launcher launcher, DragController dragController) {
        mLauncher = launcher;
        mDragController = dragController;
    }
    
    // We want our pages to be z-ordered such that the further a page is to the left, the higher
    // it is in the z-order. This is important to insure touch events are handled correctly.
    @Override
    public View getPageAt(int index) {
        return getChildAt(indexToPage(index));
    }
    
    protected void overScroll(float amount) {
        acceleratedOverScroll(amount);
    }
    
    @Override
    public void onClick(View v) {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onClick: v = " + v + ", v.getTag() = " + v.getTag());
        }

        // When we have exited all apps or are in transition, disregard clicks
        if (!mLauncher.isAllAppsVisible() ||
                mLauncher.getWorkspace().isSwitchingState()) return;

        /// M: Add for unread feature, the icon is placed in a RealtiveLayout.
        if (v instanceof MtpAppIconView) {
        	v = ((MtpAppIconView)v).mAppIcon;
        }
        
        if (v instanceof PagedViewIcon) {
            // Animate some feedback to the click
            final ApplicationInfo appInfo = (ApplicationInfo) v.getTag();

            // Lock the drawable state to pressed until we return to Launcher
            if (mPressedIcon != null) {
                mPressedIcon.lockDrawableState();
            }

            // NOTE: We want all transitions from launcher to act as if the wallpaper were enabled
            // to be consistent.  So re-enable the flag here, and we will re-disable it as necessary
            // when Launcher resumes and we are still in AllApps.
            mLauncher.updateWallpaperVisibility(true);
            mLauncher.startActivitySafely(v, appInfo.intent, appInfo);

        } else if (v instanceof PagedViewWidget) {
            // Let the user know that they have to long press to add a widget
            if (mWidgetInstructionToast != null) {
                mWidgetInstructionToast.cancel();
            }
            final IResConfigManager res = mLauncher.getResConfigManager();
            mWidgetInstructionToast = Toast.makeText(getContext(), 
            		res.getString(IResConfigManager.STR_LONG_PRESS_WIDGET_TO_ADD),//R.string.long_press_widget_to_add,
            		Toast.LENGTH_SHORT);
            mWidgetInstructionToast.show();

            // Create a little animation to show that the widget can move
            float offsetY = res.getDimensionPixelSize(IResConfigManager.DIM_DRAG_VIEW_OFFSETY);//getResources().getDimensionPixelSize(R.dimen.dragViewOffsetY);
            final ImageView p = (ImageView) v.findViewWithTag("widget_preview");//v.findViewById(R.id.widget_preview);
            AnimatorSet bounce = LauncherAnimUtils.createAnimatorSet();
            ValueAnimator tyuAnim = LauncherAnimUtils.ofFloat(p, "translationY", offsetY);
            tyuAnim.setDuration(125);
            ValueAnimator tydAnim = LauncherAnimUtils.ofFloat(p, "translationY", 0f);
            tydAnim.setDuration(100);
            bounce.play(tyuAnim).before(tydAnim);
            bounce.setInterpolator(new AccelerateInterpolator());
            bounce.start();
        }
    }
    
    /** Returns the item index of the center item on this page so that we can restore to this
     *  item index when we rotate. */
    protected int getMiddleComponentIndexOnCurrentPage() {
        int i = -1;
        if (getPageCount() > 0) {
            int currentPage = getCurrentPage();
            if (currentPage < mNumAppsPages) {
                PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(currentPage);
                PagedViewCellLayoutChildren childrenLayout = layout.getChildrenLayout();
                int numItemsPerPage = mCellCountX * mCellCountY;
                int childCount = childrenLayout.getChildCount();
                if (childCount > 0) {
                    i = (currentPage * numItemsPerPage) + (childCount / 2);
                }
            } else {
                int numApps = mApps != null ? mApps.size() : 0;
                PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(currentPage);
                int numItemsPerPage = mWidgetCountX * mWidgetCountY;
                int childCount = layout.getChildCount();
                if (childCount > 0) {
                    i = numApps +
                        ((currentPage - mNumAppsPages) * numItemsPerPage) + (childCount / 2);
                }
            }
        }
        return i;
    }
    
    protected void onDataReady(int width, int height) {
        // Note that we transpose the counts in portrait so that we get a similar layout
        boolean isLandscape = getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
        int maxCellCountX = Integer.MAX_VALUE;
        int maxCellCountY = Integer.MAX_VALUE;
        if (Launcher.isScreenLarge()) {
            maxCellCountX = (isLandscape ? mLauncher.getWorkspaceCellCountX() :
            	mLauncher.getWorkspaceCellCountY());
            maxCellCountY = (isLandscape ? mLauncher.getWorkspaceCellCountY() :
            	mLauncher.getWorkspaceCellCountX());
        }
        if (mMaxAppCellCountX > -1) {
            maxCellCountX = Math.min(maxCellCountX, mMaxAppCellCountX);
        }
        // Temp hack for now: only use the max cell count Y for widget layout
        int maxWidgetCellCountY = maxCellCountY;
        if (mMaxAppCellCountY > -1) {
            maxWidgetCellCountY = Math.min(maxWidgetCellCountY, mMaxAppCellCountY);
        }

        // Now that the data is ready, we can calculate the content width, the number of cells to
        // use for each page
        mWidgetSpacingLayout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        mWidgetSpacingLayout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);
        mWidgetSpacingLayout.calculateCellCount(width, height, maxCellCountX, maxCellCountY);
        mCellCountX = mWidgetSpacingLayout.getCellCountX();
        mCellCountY = mWidgetSpacingLayout.getCellCountY();
        updatePageCounts();

        // Force a measure to update recalculate the gaps
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        mWidgetSpacingLayout.calculateCellCount(width, height, maxCellCountX, maxWidgetCellCountY);
        mWidgetSpacingLayout.measure(widthSpec, heightSpec);
        mContentWidth = mWidgetSpacingLayout.getContentWidth();

        IAppsCustomizePanel host = mLauncher.getAppsCustomizePanel();//(AppsCustomizeTabHost) getTabHost();
        final boolean hostIsTransitioning = host.isTransitioning();

        // Restore the page
        int page = getPageForComponent(mSaveInstanceStateItemIndex);
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onDataReady: height = " + height + ", width = " + width
                    + ", isLandscape = " + isLandscape + ", page = " + page
                    + ", hostIsTransitioning = " + hostIsTransitioning + ", mContentWidth = "
                    + mContentWidth + ", mNumAppsPages = " + 0 + ", mNumWidgetPages = "
                    + 0 + ", this = " + this);
        }
        invalidatePageData(Math.max(0, page), hostIsTransitioning);
    }
    
    protected void updatePageCounts() {
    	if(mWidgets != null){
	        mNumWidgetPages = (int) Math.ceil(mWidgets.size() /
	                (float) (mWidgetCountX * mWidgetCountY));
    	}
    	if(mApps != null){
    		mNumAppsPages = (int) Math.ceil((float) mApps.size() / (mCellCountX * mCellCountY));
    	}
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "updatePageCounts end: mNumWidgetPages = " + mNumWidgetPages
                    + ", mNumAppsPages = " + mNumAppsPages + ", mApps.size() = " + mApps.size()
                    + ", mCellCountX = " + mCellCountX + ", mCellCountY = " + mCellCountY
                    + ", this = " + this);
        }
    }
	
	public int getPageForComponent(int index) {
        if (index < 0) return 0;
        if(mApps != null){
	        if (index < mApps.size()) {
	            int numItemsPerPage = mCellCountX * mCellCountY;
	            return (index / numItemsPerPage);
	        } else {
	            int numItemsPerPage = mWidgetCountX * mWidgetCountY;
	            return mNumAppsPages + ((index - mApps.size()) / numItemsPerPage);
	        }
        }
        int numItemsPerPage = mWidgetCountX * mWidgetCountY;
        return ((index) / numItemsPerPage);
    }
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (!isDataReady()) {
            if ((!isSupportApps() || (!mApps.isEmpty() && mAppsHasSet)) && (!isSupportWidget() || !mWidgets.isEmpty())) {
                setDataIsReady();
                setMeasuredDimension(width, height);
                onDataReady(width, height);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        if(!isSlideAppAndWidgetTogether()){
        	if(mNumAppsPages > 0){
	        	if(getCurrentContentType() == CONTENTTYPE_APPS_ALL){//if(getCurrentPage() < mNumAppsPages){
	        		mMaxScrollX = getChildOffset(mNumAppsPages - 1) - getRelativeChildOffset(mNumAppsPages - 1);
	        	} else if(getCurrentContentType() == CONTENTTYPE_WIDGETS) {
	        		mMinScrollX = getChildOffset(mNumAppsPages) - getRelativeChildOffset(mNumAppsPages);
	        	}
        	}
        }
//        Util.Log.e(TAG, "onMeasure()==mMaxScrollX:"+mMaxScrollX+"==mMinScrollX:" + mMinScrollX
//        		+"==currentpage:"+getCurrentPage()
//        		+"==mNumAppsPages:"+mNumAppsPages);
    }

	public void onPackagesUpdated() {
        // Get the list of widgets and shortcuts
		if(mWidgets == null)
			return;
		
        mWidgets.clear();
        List<AppWidgetProviderInfo> widgets =
            AppWidgetManager.getInstance(mLauncher.getActivity()).getInstalledProviders();
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "updatePackages: widgets size = " + widgets.size());
        }

        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<ResolveInfo> shortcuts = mPackageManager.queryIntentActivities(shortcutsIntent, 0);
        for (AppWidgetProviderInfo widget : widgets) {
            if (widget.minWidth > 0 && widget.minHeight > 0) {
                // Ensure that all widgets we show can be added on a workspace of this size
                int[] spanXY = Launcher.getSpanForWidget(mLauncher, widget);
                int[] minSpanXY = Launcher.getMinSpanForWidget(mLauncher, widget);
                int minSpanX = Math.min(spanXY[0], minSpanXY[0]);
                int minSpanY = Math.min(spanXY[1], minSpanXY[1]);
                if (minSpanX <= mLauncher.getWorkspaceCellCountX() &&
                        minSpanY <= mLauncher.getWorkspaceCellCountY()) {
                    mWidgets.add(widget);
                } else {
                    Util.Log.e(TAG, "Widget " + widget.provider + " can not fit on this device (" + widget.minWidth
                            + ", " + widget.minHeight + "), min span is (" + minSpanX + ", " + minSpanY + ")"
                            + "), span is (" + spanXY[0] + ", " + spanXY[1] + ")");
                }
            } else {
                Util.Log.e(TAG, "Widget " + widget.provider + " has invalid dimensions (" +
                        widget.minWidth + ", " + widget.minHeight + ")");
            }
        }
        mWidgets.addAll(shortcuts);
        Collections.sort(mWidgets,
                new LauncherModel.WidgetAndShortcutNameComparator(mPackageManager));
        updatePageCounts();
        invalidateOnDataChange();
    }
	
    /** Get the index of the item to restore to if we need to restore the current page. */
    public int getSaveInstanceStateIndex() {
        if (mSaveInstanceStateItemIndex == -1) {
            mSaveInstanceStateItemIndex = getMiddleComponentIndexOnCurrentPage();
        }
        return mSaveInstanceStateItemIndex;
    }
    
    /** Restores the page for an item at the specified index */
    public void restorePageForIndex(int index) {
        if (index < 0) return;
        mSaveInstanceStateItemIndex = index;
    }
    
    /*
     * PagedViewWithDraggableItems implementation
     */
    @Override
    protected void determineDraggingStart(android.view.MotionEvent ev) {
        // Disable dragging by pulling an app down for now.
    }
    
    @Override
    protected boolean beginDragging(final View v) {
        if (Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "beginDragging: v = " + v + ", this = " + this);
        }

        if (!super.beginDragging(v)) return false;

        if (v instanceof PagedViewIcon) {
            beginDraggingApplication(v);
        } else if (v instanceof PagedViewWidget) {
            if (!beginDraggingWidget(v)) {
            	mDraggingWidget = false;
                return false;
            }
        }

        // We delay entering spring-loaded mode slightly to make sure the UI
        // thready is free of any work.
        postDelayed(new Runnable() {
            @Override
            public void run() {
                // We don't enter spring-loaded mode if the drag has been cancelled
                if (mLauncher.getDragController().isDragging()) {
                    // Dismiss the cling
                    //mLauncher.dismissAllAppsCling(null);

                    // Reset the alpha on the dragged icon before we drag
                    resetDrawableState();

                    // Go into spring loaded mode (must happen before we startDrag())
                    mLauncher.enterSpringLoadedDragMode();
                }
            }
        }, 150);

        return true;
    }
    
    protected void beginDraggingApplication(View v) {
        mLauncher.getWorkspace().onDragStartedWithItem(v);
        mLauncher.getWorkspace().beginDragShared(v, this);
    }
    
    protected boolean beginDraggingWidget(View v){
    	mDraggingWidget = true;
    	return false;
    }
    
    protected void cleanupWidgetPreloading(boolean widgetWasAdded) {
    	
    }
    
    @Override
    public void onDropCompleted(View target, DragObject d, boolean isFlingToDelete,
            boolean success) {
        if (Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "onDropCompleted: target = " + target + ", d = " + d + ", isFlingToDelete = " + isFlingToDelete + ", success = " + success);
        }

        // Return early and wait for onFlingToDeleteCompleted if this was the result of a fling
        if (isFlingToDelete) return;

        endDragging(target, false, success);

        // Display an error message if the drag failed due to there not being enough space on the
        // target layout we were dropping on.
        if (!success) {
            boolean showOutOfSpaceMessage = false;
            if (target instanceof Workspace) {
                int currentScreen = mLauncher.getCurrentWorkspaceScreen();
                Workspace workspace = (Workspace) target;
                CellLayout layout = (CellLayout) workspace.getPageAt(currentScreen);
                ItemInfo itemInfo = (ItemInfo) d.dragInfo;
                if (layout != null) {
                    layout.calculateSpans(itemInfo);
                    showOutOfSpaceMessage =
                            !layout.findCellForSpan(null, itemInfo.spanX, itemInfo.spanY);
                }
                /// M: Display an error message if the drag failed due to exist one IMTKWidget 
                /// which providerName equals the providerName of the dragInfo.
//                if (d.dragInfo instanceof PendingAddWidgetInfo) {
//                    PendingAddWidgetInfo info = (PendingAddWidgetInfo) d.dragInfo;
//                    if (workspace.searchIMTKWidget(workspace, info.componentName.getClassName()) != null) {
//                        ((LauncherDefault)mLauncher).showOnlyOneWidgetMessage(info);
//                    }
//                }
            }
            if (showOutOfSpaceMessage) {
                mLauncher.showOutOfSpaceMessage(false);
            }

            d.deferDragViewCleanupPostAnimation = false;
        }
        cleanupWidgetPreloading(success);
        mDraggingWidget = false;
    }
    
    @Override
    public void onFlingToDeleteCompleted() {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onFlingToDeleteCompleted.");
        }

        // We just dismiss the drag when we fling, so cleanup here
        endDragging(null, true, true);
        
        cleanupWidgetPreloading(false);
        mDraggingWidget = false;
    }
    
    @Override
    public void cleanUpShortPress(View v) {
        if (!mDraggingWidget) {
            cleanupWidgetPreloading(false);
        }
    }
    
    public void setContentType(int contentType) {
    	mCurrentContentType = contentType;
    	if(contentType == CONTENTTYPE_WIDGETS)
    		invalidatePageData(mNumAppsPages, true);
    	else
    		invalidatePageData(0, true);
    }

    public void snapToPage(int whichPage, int delta, int duration) {
        super.snapToPage(whichPage, delta, duration);
        updateCurrentTab(whichPage);
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "snapToPage: whichPage = " + whichPage + ", delta = "
                    + delta + ", duration = " + duration + ", this = " + this);
        }

        // Update the thread priorities given the direction lookahead
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int pageIndex = task.page;
            if ((mNextPage > mCurrentPage && pageIndex >= mCurrentPage) ||
                (mNextPage < mCurrentPage && pageIndex <= mCurrentPage)) {
                task.setThreadPriority(getThreadPriorityForPage(pageIndex));
            } else {
                task.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            }
        }
    }

    protected void updateCurrentTab(int currentPage) {
//    	if(!isSlideAppAndWidgetTogether())
//    		notifyPageSwitchListener();
    }
    
    /**
     * Clean up after dragging.
     *
     * @param target where the item was dragged to (can be null if the item was flung)
     */
    protected void endDragging(View target, boolean isFlingToDelete, boolean success) {
        if (Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "endDragging: target = " + target + ", isFlingToDelete = " + isFlingToDelete + ", success = " + success);
        }

        if (isFlingToDelete || !success || (target != mLauncher.getWorkspace() &&
                !(target instanceof DeleteDropTarget))) {
            // Exit spring loaded mode if we have not successfully dropped or have not handled the
            // drop in Workspace
            mLauncher.exitSpringLoadedDragMode();
        }
        mLauncher.unlockScreenOrientation(false);
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
    	if (Util.ENABLE_DEBUG) {
    		Util.Log.d(TAG, "onLauncherTransitionPrepare l = " + l + ", animated = " + animated + 
    				", toWorkspace = " + toWorkspace);
    	}

        mInTransition = true;
        if (toWorkspace) {
            cancelAllTasks();
        }
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
    	if (Util.ENABLE_DEBUG) {
    		Util.Log.d(TAG, "onLauncherTransitionEnd l = " + l + ", animated = " + animated + 
    				", toWorkspace = " + toWorkspace);
    	}

        mInTransition = false;
        if(mDeferredSyncWidgetPageItems != null){
	        for (AsyncTaskPageData d : mDeferredSyncWidgetPageItems) {
	            onSyncWidgetPageItems(d);
	        }
	        mDeferredSyncWidgetPageItems.clear();
        }
        if(mDeferredPrepareLoadWidgetPreviewsTasks != null){
	        for (Runnable r : mDeferredPrepareLoadWidgetPreviewsTasks) {
	            r.run();
	        }
	        mDeferredPrepareLoadWidgetPreviewsTasks.clear();
        }
        mForceDrawAllChildrenNextFrame = !toWorkspace;
    }
    
    public Bundle getDefaultOptionsForWidget(Launcher launcher, PendingAddWidgetInfo info) {
        Bundle options = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            AppWidgetResizeFrame.getWidgetSizeRanges(mLauncher, info.spanX, info.spanY, mTmpRect);
            Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(mLauncher.getActivity(),
                    info.componentName, null);

            float density = getResources().getDisplayMetrics().density;
            int xPaddingDips = (int) ((padding.left + padding.right) / density);
            int yPaddingDips = (int) ((padding.top + padding.bottom) / density);

            options = new Bundle();
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                    mTmpRect.left - xPaddingDips);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                    mTmpRect.top - yPaddingDips);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                    mTmpRect.right - xPaddingDips);
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                    mTmpRect.bottom - yPaddingDips);
        }
        return options;
    }
    
    @Override
    public View getContent() {
        return null;
    }
    
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return FocusHelper.handleAppsCustomizeKeyEvent(v,  keyCode, event);
    }
    
    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }
    
    /*
     * Apps PagedView implementation
     */
    protected void setVisibilityOnChildren(ViewGroup layout, int visibility) {
        int childCount = layout.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            layout.getChildAt(i).setVisibility(visibility);
        }
    }

    protected void setupPage(PagedViewCellLayout layout) {
        layout.setCellCount(mCellCountX, mCellCountY);
        layout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

        // Note: We force a measure here to get around the fact that when we do layout calculations
        // immediately after syncing, we don't have a proper width.  That said, we already know the
        // expected page width, so we can actually optimize by hiding all the TextView-based
        // children that are expensive to measure, and let that happen naturally later.
        setVisibilityOnChildren(layout, View.GONE);
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        layout.setMinimumWidth(getPageContentWidth());
        layout.measure(widthSpec, heightSpec);
        setVisibilityOnChildren(layout, View.VISIBLE);
    }
    
    /*
     * Widgets PagedView implementation
     */
    protected void setupPage(PagedViewGridLayout layout) {
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

        // Note: We force a measure here to get around the fact that when we do layout calculations
        // immediately after syncing, we don't have a proper width.
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        layout.setMinimumWidth(getPageContentWidth());
        layout.measure(widthSpec, heightSpec);
    }
    
    public void reset() {
        // If we have reset, then we should not continue to restore the previous state
        mSaveInstanceStateItemIndex = -1;
    }
    
    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();
        
        if(true && mAnimateState != AnimateState.IDLE){
            int size = getPageCount();
            int min = 0;
            if(!isSlideAppAndWidgetTogether()){
                if(getCurrentPage() >= mNumAppsPages){
                    min = mNumAppsPages;
                } else {
                    size = mNumAppsPages;
                }
            }

            for(int i=min; i<size; i++){
                final View view = getPageAt(i);
                if(false){
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
                            +", ScaleY:"+view.getScaleY()
                            +", vis:"+(view.getVisibility() == View.VISIBLE)
                            +", cur:"+getCurrentPage());
                    animateReset(view);
                } else {
                    animateReset(view);
                }
            }
        }
        mForceDrawAllChildrenNextFrame = true;
        // We reset the save index when we change pages so that it will be recalculated on next
        // rotation
        mSaveInstanceStateItemIndex = -1;
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onDetachedFromWindow.");
        }
        cancelAllTasks();
    }
    
    public void clearAllPages() {
        cancelAllTasks();
        int count = getChildCount();
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "clearAllPages: count = " + count);
        }

        for (int i = 0; i < count; i++) {
            View v = getPageAt(i);
            if (v instanceof PagedViewGridLayout) {
                ((PagedViewGridLayout) v).removeAllViewsOnPage();
                mDirtyPageContent.set(i, true);
            /// M: Clear app pages
            } else if (v instanceof PagedViewCellLayout) {
                ((PagedViewCellLayout) v).removeAllViewsOnPage();
                mDirtyPageContent.set(i, true);
            }
        }
    }
    
    public void clearAllWidgetPages() {
        cancelAllTasks();
        int count = getChildCount();
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "clearAllWidgetPages: count = " + count);
        }

        for (int i = 0; i < count; i++) {
            View v = getPageAt(i);
            if (v instanceof PagedViewGridLayout) {
                ((PagedViewGridLayout) v).removeAllViewsOnPage();
                mDirtyPageContent.set(i, true);
            }
        }
    }

    public void cancelAllTasks() {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "cancelAllTasks: mRunningTasks size = " + mRunningTasks.size());
        }

        // Clean up all the async tasks
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            task.cancel(false);
            iter.remove();
            mDirtyPageContent.set(task.page, true);

            // We've already preallocated the views for the data to load into, so clear them as well
            View v = getPageAt(task.page);
            if (v instanceof PagedViewGridLayout) {
                ((PagedViewGridLayout) v).removeAllViewsOnPage();
            }
        }
        if(mDeferredSyncWidgetPageItems != null)
        	mDeferredSyncWidgetPageItems.clear();
        if(mDeferredPrepareLoadWidgetPreviewsTasks != null)
        	mDeferredPrepareLoadWidgetPreviewsTasks.clear();
    }
    
    public void dumpState() {
        // TODO: Dump information related to current list of Applications, Widgets, etc.
    	if(mApps != null)
    		ApplicationInfo.dumpApplicationInfoList(TAG, "mApps", mApps);
    	if(mWidgets != null)
    		dumpAppWidgetProviderInfoList(TAG, "mWidgets", mWidgets);
    }
    
    protected void dumpAppWidgetProviderInfoList(String tag, String label,
            List<Object> list) {
    	if(list == null)
    		return;
        Util.Log.d(tag, label + " size=" + list.size());
        for (Object i: list) {
            if (i instanceof AppWidgetProviderInfo) {
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) i;
                Util.Log.d(tag, "   label=\"" + info.label + "\" previewImage=" + info.previewImage
                        + " resizeMode=" + info.resizeMode + " configure=" + info.configure
                        + " initialLayout=" + info.initialLayout
                        + " minWidth=" + info.minWidth + " minHeight=" + info.minHeight);
            } else if (i instanceof ResolveInfo) {
                ResolveInfo info = (ResolveInfo) i;
                Util.Log.d(tag, "   label=\"" + info.loadLabel(mPackageManager) + "\" icon="
                        + info.icon);
            }
        }
    }

    public void surrender() {
        // TODO: If we are in the middle of any process (ie. for holographic outlines, etc) we
        // should stop this now.

        // Stop all background tasks
        cancelAllTasks();
    }

    @Override
    public void iconPressed(PagedViewIcon icon) {
        // Reset the previously pressed icon and store a reference to the pressed icon so that
        // we can reset it on return to Launcher (in Launcher.onResume())
        if (mPressedIcon != null) {
            mPressedIcon.resetDrawableState();
        }
        mPressedIcon = icon;
    }

    public void resetDrawableState() {
        if (mPressedIcon != null) {
            mPressedIcon.resetDrawableState();
            mPressedIcon = null;
        }
    }
    /**
     * We should call thise method whenever the core data changes (mApps, mWidgets) so that we can
     * appropriately determine when to invalidate the PagedView page data.  In cases where the data
     * has yet to be set, we can requestLayout() and wait for onDataReady() to be called in the
     * next onMeasure() pass, which will trigger an invalidatePageData() itself.
     */
    protected void invalidateOnDataChange() {
        if (!isDataReady()) {
            // The next layout pass will trigger data-ready if both widgets and apps are set, so
            // request a layout to trigger the page data when ready.
            requestLayout();
        } else {
            cancelAllTasks();
            invalidatePageData();
        }
    }
    
    /**
     * A helper to return the priority for loading of the specified widget page.
     */
    protected int getWidgetPageLoadPriority(int page) {
        // If we are snapping to another page, use that index as the target page index
        int toPage = mCurrentPage;
        if (mNextPage > -1) {
            toPage = mNextPage;
        }

        // We use the distance from the target page as an initial guess of priority, but if there
        // are no pages of higher priority than the page specified, then bump up the priority of
        // the specified page.
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        int minPageDiff = Integer.MAX_VALUE;
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            minPageDiff = Math.abs(task.page - toPage);
        }

        int rawPageDiff = Math.abs(page - toPage);
        return rawPageDiff - Math.min(rawPageDiff, minPageDiff);
    }
    /**
     * Return the appropriate thread priority for loading for a given page (we give the current
     * page much higher priority)
     */
    protected int getThreadPriorityForPage(int page) {
        // TODO-APPS_CUSTOMIZE: detect number of cores and set thread priorities accordingly below
        int pageDiff = getWidgetPageLoadPriority(page);
        if (pageDiff <= 0) {
            return Process.THREAD_PRIORITY_LESS_FAVORABLE;
        } else if (pageDiff <= 1) {
            return Process.THREAD_PRIORITY_LOWEST;
        } else {
            return Process.THREAD_PRIORITY_LOWEST;
        }
    }

    protected int getSleepForPage(int page) {
        int pageDiff = getWidgetPageLoadPriority(page);
        return Math.max(0, pageDiff * sPageSleepDelay);
    }
    
    protected void renderDrawableToBitmap(Drawable d, Bitmap bitmap, int x, int y, int w, int h) {
        renderDrawableToBitmap(d, bitmap, x, y, w, h, 1f);
    }

    protected void renderDrawableToBitmap(Drawable d, Bitmap bitmap, int x, int y, int w, int h,
            float scale) {
        if (bitmap != null) {
            Canvas c = new Canvas(bitmap);
            c.scale(scale, scale);
            Rect oldBounds = d.copyBounds();
            d.setBounds(x, y, x + w, y + h);
            d.draw(c);
            d.setBounds(oldBounds); // Restore the bounds
            c.setBitmap(null);
        }
    }

    protected Bitmap getShortcutPreview(ResolveInfo info, int maxWidth, int maxHeight) {
        Bitmap tempBitmap = mCachedShortcutPreviewBitmap.get();
        final Canvas c = mCachedShortcutPreviewCanvas.get();
        if (tempBitmap == null ||
                tempBitmap.getWidth() != maxWidth ||
                tempBitmap.getHeight() != maxHeight) {
            tempBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Config.ARGB_8888);
            mCachedShortcutPreviewBitmap.set(tempBitmap);
        } else {
            c.setBitmap(tempBitmap);
            c.drawColor(0, PorterDuff.Mode.CLEAR);
            c.setBitmap(null);
        }
        // Render the icon
        Drawable icon = mIconCache.getFullResIcon(info);

        final IResConfigManager res = mLauncher.getResConfigManager();
        int paddingTop = res.getDimensionPixelOffset(IResConfigManager.DIM_SHORTCUT_PREVIEW_PADDING_TOP);
                //getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_top);
        int paddingLeft = res.getDimensionPixelOffset(IResConfigManager.DIM_SHORTCUT_PREVIEW_PADDING_LEFT);
                //getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_left);
        int paddingRight = res.getDimensionPixelOffset(IResConfigManager.DIM_SHORTCUT_PREVIEW_PADDING_RIGHT);
                //getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_right);

        int scaledIconWidth = (maxWidth - paddingLeft - paddingRight);

        renderDrawableToBitmap(
                icon, tempBitmap, paddingLeft, paddingTop, scaledIconWidth, scaledIconWidth);

        Bitmap preview = Bitmap.createBitmap(maxWidth, maxHeight, Config.ARGB_8888);
        c.setBitmap(preview);
        Paint p = mCachedShortcutPreviewPaint.get();
        if (p == null) {
            p = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            p.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            p.setAlpha((int) (255 * 0.06f));
            //float density = 1f;
            //p.setMaskFilter(new BlurMaskFilter(15*density, BlurMaskFilter.Blur.NORMAL));
            mCachedShortcutPreviewPaint.set(p);
        }
        c.drawBitmap(tempBitmap, 0, 0, p);
        c.setBitmap(null);

        renderDrawableToBitmap(icon, preview, 0, 0, mAppIconSize, mAppIconSize);

        return preview;
    }

    protected Bitmap getWidgetPreview(ComponentName provider, int previewImage,
            int iconId, int cellHSpan, int cellVSpan, int maxWidth,
            int maxHeight) {
        // Load the preview image if possible
        String packageName = provider.getPackageName();
        ///M:maxWidth & maxHeight maybe zero which can lead to createBitmap JE
        if (maxWidth <= 0) {
            Util.Log.w(TAG, "getWidgetPreview packageName=" + packageName +", maxWidth:" + maxWidth);
            maxWidth = Integer.MAX_VALUE;
        }
        if (maxHeight <= 0) {
        	Util.Log.w(TAG, "getWidgetPreview packageName=" + packageName +", maxHeight:" + maxHeight);
            maxHeight = Integer.MAX_VALUE;
        }

        Drawable drawable = null;
        if (previewImage != 0) {
            drawable = mPackageManager.getDrawable(packageName, previewImage, null);
            if (drawable == null) {
            	Util.Log.w(TAG, "Can't load widget preview drawable 0x" +
                        Integer.toHexString(previewImage) + " for provider: " + provider);
            }
        }

        ///M: initialized to 0 for build pass
        int bitmapWidth = 0;
        int bitmapHeight = 0;
        Bitmap defaultPreview = null;
        boolean widgetPreviewExists = (drawable != null);
        ///M:getIntrinsicWidth & getIntrinsicHeight maybe return -1 which can lead to createBitmap JE
        boolean useWidgetPreview = false;
        if (widgetPreviewExists) {
            bitmapWidth = drawable.getIntrinsicWidth();
            bitmapHeight = drawable.getIntrinsicHeight();
            if ((bitmapWidth <= 0) || (bitmapHeight <= 0)) {
            	Util.Log.w(TAG, "getWidgetPreview packageName=" + packageName +", getIntrinsicWidth():" +
                        bitmapWidth + ", getIntrinsicHeight(): " + bitmapHeight);
            } else {
                useWidgetPreview = true;
            }
        } 

        if (useWidgetPreview == false) {
            // Generate a preview image if we couldn't load one
            if (cellHSpan < 1) cellHSpan = 1;
            if (cellVSpan < 1) cellVSpan = 1;

//            BitmapDrawable previewDrawable = (BitmapDrawable) getResources()
//                    .getDrawable(R.drawable.widget_preview_tile);
            BitmapDrawable previewDrawable = (BitmapDrawable) mLauncher.getResConfigManager().getDrawable(IResConfigManager.IMG_WIDGET_PREVIEW_TILE);
            final int previewDrawableWidth = previewDrawable
                    .getIntrinsicWidth();
            final int previewDrawableHeight = previewDrawable
                    .getIntrinsicHeight();
            bitmapWidth = previewDrawableWidth * cellHSpan; // subtract 2 dips
            bitmapHeight = previewDrawableHeight * cellVSpan;

            defaultPreview = Bitmap.createBitmap(bitmapWidth, bitmapHeight,
                    Config.ARGB_8888);
            final Canvas c = mCachedAppWidgetPreviewCanvas.get();
            c.setBitmap(defaultPreview);
            previewDrawable.setBounds(0, 0, bitmapWidth, bitmapHeight);

            /**
             * M: Since the previous setTileModeXY function creates shader and paint which shared by many preview bitmaps,
             * native exception happens in libskia.so because race condition between multi-thread. We create fresh new
             * objects to avoid this, but will need allocate more memory than before. @{
             */
            final Bitmap previewBitmap = previewDrawable.getBitmap();
            final BitmapShader shader = new BitmapShader(previewBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            final Paint shaderPaint = new Paint();
            shaderPaint.setShader(shader);
            c.drawPaint(shaderPaint);
            /** @} */

            c.setBitmap(null);

            // Draw the icon in the top left corner
            int minOffset = (int) (mAppIconSize * sWidgetPreviewIconPaddingPercentage);
            int smallestSide = Math.min(bitmapWidth, bitmapHeight);
            float iconScale = Math.min((float) smallestSide
                    / (mAppIconSize + 2 * minOffset), 1f);

            //try {
                Drawable icon = null;
                int hoffset =
                        (int) ((previewDrawableWidth - mAppIconSize * iconScale) / 2);
                int yoffset =
                        (int) ((previewDrawableHeight - mAppIconSize * iconScale) / 2);
                if (iconId > 0) {
                    icon = mIconCache.getFullResIcon(packageName, iconId);
                }
                if (icon != null) {
                    renderDrawableToBitmap(icon, defaultPreview, hoffset,
                            yoffset, (int) (mAppIconSize * iconScale),
                            (int) (mAppIconSize * iconScale));
                }
//            } catch (Resources.NotFoundException e) {
//            }
        }

        // Scale to fit width only - let the widget preview be clipped in the
        // vertical dimension
        float scale = 1f;
        if (bitmapWidth > maxWidth) {
            scale = maxWidth / (float) bitmapWidth;
        }
        if (scale != 1f) {
            bitmapWidth = (int) (scale * bitmapWidth);
            bitmapHeight = (int) (scale * bitmapHeight);
        }

        Bitmap preview = Bitmap.createBitmap(bitmapWidth, bitmapHeight,
                Config.ARGB_8888);

        // Draw the scaled preview into the final bitmap
        if (widgetPreviewExists) {
            renderDrawableToBitmap(drawable, preview, 0, 0, bitmapWidth,
                    bitmapHeight);
        } else {
            final Canvas c = mCachedAppWidgetPreviewCanvas.get();
            final Rect src = mCachedAppWidgetPreviewSrcRect.get();
            final Rect dest = mCachedAppWidgetPreviewDestRect.get();
            c.setBitmap(preview);
            src.set(0, 0, defaultPreview.getWidth(), defaultPreview.getHeight());
            dest.set(0, 0, preview.getWidth(), preview.getHeight());

            Paint p = mCachedAppWidgetPreviewPaint.get();
            if (p == null) {
                p = new Paint();
                p.setFilterBitmap(true);
                mCachedAppWidgetPreviewPaint.set(p);
            }
            c.drawBitmap(defaultPreview, src, dest, p);
            c.setBitmap(null);
        }
        return preview;
    }
    
    protected void loadWidgetPreviewsInBackground(AppsCustomizeAsyncTask task,
            AsyncTaskPageData data) {
        // loadWidgetPreviewsInBackground can be called without a task to load a set of widget
        // previews synchronously
        if (task != null) {
            // Ensure that this task starts running at the correct priority
            task.syncThreadPriority();
        }

        // Load each of the widget/shortcut previews
        List<Object> items = data.items;
        List<Bitmap> images = data.generatedImages;
        int count = items.size();
        for (int i = 0; i < count; ++i) {
            if (task != null) {
                // Ensure we haven't been cancelled yet
                if (task.isCancelled()) break;
                // Before work on each item, ensure that this task is running at the correct
                // priority
                task.syncThreadPriority();
            }

            Object rawInfo = items.get(i);
            if (rawInfo instanceof AppWidgetProviderInfo) {
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) rawInfo;
                int[] cellSpans = Launcher.getSpanForWidget(mLauncher, info);

                int maxWidth = Math.min(data.maxImageWidth,
                        mWidgetSpacingLayout.estimateCellWidth(cellSpans[0]));
                int maxHeight = Math.min(data.maxImageHeight,
                        mWidgetSpacingLayout.estimateCellHeight(cellSpans[1]));
                Bitmap b = getWidgetPreview(info.provider, info.previewImage, info.icon,
                        cellSpans[0], cellSpans[1], maxWidth, maxHeight);
                images.add(b);
            } else if (rawInfo instanceof ResolveInfo) {
                // Fill in the shortcuts information
                ResolveInfo info = (ResolveInfo) rawInfo;
                images.add(getShortcutPreview(info, data.maxImageWidth, data.maxImageHeight));
            }
        }
    }
    
    @Override
    public void syncPages() {
    	if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "syncPages: mNumWidgetPages = " + mNumWidgetPages + ", mNumAppsPages = "
                    + mNumAppsPages + ", this = " + this);
        }
    	removeAllViews();
        cancelAllTasks();

        /// M: notify launcher that apps pages were recreated.
        mLauncher.notifyPagesWereRecreated();
        
        final Context context = getContext();
        
        if(isSupportWidget())
        	syncWidgetPages(context);
        
        if(isSupportApps())
        	syncAppsPages(context);
        
        if(!isSlideAppAndWidgetTogether())
        	notifyPageCountChangedListener(getContentPageCount());
    }
    
    protected void syncAppsPages(Context context){
    	final IResConfigManager res = mLauncher.getResConfigManager();
    	for (int i = 0; i < mNumAppsPages; ++i) {
            PagedViewCellLayout layout = (PagedViewCellLayout)res.inflaterView(IResConfigManager.LAYOUT_APPS_CUSTOMIZE_PAGE_SCREEN, this, false);
            		//mLayoutInflater.inflate(R.layout.apps_customize_page_screen, this, false);//new PagedViewCellLayout(context);
            setupPage(layout);
            addView(layout);
			if (Util.ENABLE_DEBUG) {
				Util.Log.d(TAG, "syncAppsPages: PagedViewCellLayout layout = " + layout);
			}
        }
    }
    
    protected void syncWidgetPages(Context context){
    	for (int j = 0; j < mNumWidgetPages; ++j) {
            PagedViewGridLayout layout = new PagedViewGridLayout(context, mWidgetCountX,
                    mWidgetCountY);
            setupPage(layout);
            addView(layout, new PagedView.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            if (Util.ENABLE_DEBUG) {
				Util.Log.d(TAG, "syncWidgetPages: PagedViewCellLayout layout = " + layout);
			}
        }
    }
    
    @Override
    public void syncPageItems(int page, boolean immediate) {
    	if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "syncPageItems: page = " + page + ", immediate = " + immediate
                    + ", mNumAppsPages = " + mNumAppsPages);
        }

        if (isSupportApps() && page < mNumAppsPages) {
            syncAppsPageItems(page, immediate);
        } else if(isSupportWidget()) {
            syncWidgetPageItems(page, immediate);
        }
    }
    
    public void syncAppsPageItems(int page, boolean immediate){
    	if(mApps != null)
    		syncAppsPageItems(page, immediate, mApps);
    }
    
    public void syncWidgetPageItems(final int page, final boolean immediate) {
    	if(mWidgets == null)
    		return;
    	
    	int numItemsPerPage = mWidgetCountX * mWidgetCountY;

        // Calculate the dimensions of each cell we are giving to each widget
        final List<Object> items = new ArrayList<Object>();

        // Prepare the set of widgets to load previews for in the background
        int offset = (page - mNumAppsPages) * numItemsPerPage;
        for (int i = offset; i < Math.min(offset + numItemsPerPage, mWidgets.size()); ++i) {
            items.add(mWidgets.get(i));
        }
        
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "syncWidgetPageItems: page = " + page + ", immediate = " + immediate
                    + ", numItemsPerPage = " + numItemsPerPage
                    + ", offset = " + offset + ", this = " + this);
        }
        
        syncWidgetPageItems(page, immediate, items);
    }
    
    public void syncAppsPageItems(int page, boolean immediate, final List<ApplicationInfo> apps) {
        // ensure that we have the right number of items on the pages
        int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, apps.size());
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "syncAppsPageItems: page = " + page + ", immediate = " + immediate
                    + ", numCells = " + numCells + ", startIndex = " + startIndex + ", endIndex = "
                    + endIndex + ", app size = " + apps.size() + ", child count = "
                    + getChildCount() + ", this = " + this);
        }

        final IResConfigManager res = mLauncher.getResConfigManager();
        
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page);

        layout.removeAllViewsOnPage();
        List<Object> items = new ArrayList<Object>();
        List<Bitmap> images = new ArrayList<Bitmap>();
        for (int i = startIndex; i < endIndex; ++i) {
            ApplicationInfo info = apps.get(i);
            MtpAppIconView icon = (MtpAppIconView) res.inflaterView(IResConfigManager.LAYOUT_APPS_CUSTOMIZE_APP_WITH_UNREAD, layout, false);
            		//mLayoutInflater.inflate(R.layout.apps_customize_application_with_unread, layout, false);
            icon.applyFromApplicationInfo(info, true, this);
            icon.mAppIcon.setOnClickListener(this);
            icon.mAppIcon.setOnLongClickListener(this);
            icon.setOnTouchListener(this);
            icon.mAppIcon.setOnKeyListener(this);

            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayout(icon, -1, i, new PagedViewCellLayout.LayoutParams(x,y, 1,1));

            items.add(info);
            images.add(info.iconBitmap);
        }

        layout.createHardwareLayers();
    }
    
    public void syncWidgetPageItems(final int page, final boolean immediate, final List<Object> items){
    	
    	int contentWidth = mWidgetSpacingLayout.getContentWidth();
        final int cellWidth = ((contentWidth - mPageLayoutPaddingLeft - mPageLayoutPaddingRight
                - ((mWidgetCountX - 1) * mWidgetWidthGap)) / mWidgetCountX);
        int contentHeight = mWidgetSpacingLayout.getContentHeight();
        final int cellHeight = ((contentHeight - mPageLayoutPaddingTop - mPageLayoutPaddingBottom
                - ((mWidgetCountY - 1) * mWidgetHeightGap)) / mWidgetCountY);
        
    	if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "syncWidgetPageItems: page = " + page + ", immediate = " + immediate
                    + ", cellWidth = " + cellWidth
                    + ", contentHeight = " + contentHeight + ", cellHeight = " + cellHeight
                    + ", this = " + this);
        }

    	final IResConfigManager res = mLauncher.getResConfigManager();
    	
        // Prepopulate the pages with the other widget info, and fill in the previews later
        final PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(page);
        layout.setColumnCount(layout.getCellCountX());
        for (int i = 0; i < items.size(); ++i) {
            Object rawInfo = items.get(i);
            PendingAddItemInfo createItemInfo = null;
            PagedViewWidget widget = (PagedViewWidget) res.inflaterView(IResConfigManager.LAYOUT_APPS_CUSTOMIZE_WIDGET, layout, false);
            	//mLayoutInflater.inflate(R.layout.apps_customize_widget, layout, false);
            if (rawInfo instanceof AppWidgetProviderInfo) {
                // Fill in the widget information
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) rawInfo;
                createItemInfo = new PendingAddWidgetInfo(info, null, null);

                // Determine the widget spans and min resize spans.
                int[] spanXY = Launcher.getSpanForWidget(mLauncher, info);
                createItemInfo.spanX = spanXY[0];
                createItemInfo.spanY = spanXY[1];
                int[] minSpanXY = Launcher.getMinSpanForWidget(mLauncher, info);
                createItemInfo.minSpanX = minSpanXY[0];
                createItemInfo.minSpanY = minSpanXY[1];

                widget.applyFromAppWidgetProviderInfo(info, -1, spanXY);
                widget.setTag(createItemInfo);
                widget.setShortPressListener(this);
            } else if (rawInfo instanceof ResolveInfo) {
                // Fill in the shortcuts information
                ResolveInfo info = (ResolveInfo) rawInfo;
                createItemInfo = new PendingAddShortcutInfo(info.activityInfo);
                createItemInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
                createItemInfo.componentName = new ComponentName(info.activityInfo.packageName,
                        info.activityInfo.name);
                widget.applyFromResolveInfo(mPackageManager, info);
                widget.setTag(createItemInfo);
            }
            widget.setOnClickListener(this);
            widget.setOnLongClickListener(this);
            widget.setOnTouchListener(this);
            widget.setOnKeyListener(this);

            // Layout each widget
            int ix = i % mWidgetCountX;
            int iy = i / mWidgetCountX;
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(iy, GridLayout.LEFT),
                    GridLayout.spec(ix, GridLayout.TOP));
            lp.width = cellWidth;
            lp.height = cellHeight;
            lp.setGravity(Gravity.TOP | Gravity.LEFT);
            if (ix > 0) lp.leftMargin = mWidgetWidthGap;
            if (iy > 0) lp.topMargin = mWidgetHeightGap;
            layout.addView(widget, lp);
        }

        // wait until a call on onLayout to start loading, because
        // PagedViewWidget.getPreviewSize() will return 0 if it hasn't been laid out
        // TODO: can we do a measure/layout immediately?
        layout.setOnLayoutListener(new Runnable() {
            public void run() {
                // Load the widget previews
                int maxPreviewWidth = cellWidth;
                int maxPreviewHeight = cellHeight;
                if (layout.getChildCount() > 0) {
                    PagedViewWidget w = (PagedViewWidget) layout.getChildAt(0);
                    int[] maxSize = w.getPreviewSize();
                    maxPreviewWidth = maxSize[0];
                    maxPreviewHeight = maxSize[1];
                    if ((maxPreviewWidth <= 0) || (maxPreviewHeight <= 0)) {
                        if (Util.ENABLE_DEBUG) {
                            Util.Log.d(TAG, "syncWidgetPageItems: maxPreviewWidth = " + maxPreviewWidth 
                                + ", maxPreviewHeight = " + maxPreviewHeight);
                        }
                    }
                }
                if (immediate) {
                    AsyncTaskPageData data = new AsyncTaskPageData(page, items,
                            maxPreviewWidth, maxPreviewHeight, null, null);
                    loadWidgetPreviewsInBackground(null, data);
                    onSyncWidgetPageItems(data);
                } else {
                    if (mInTransition) {
                    	if(mDeferredPrepareLoadWidgetPreviewsTasks != null)
                    		mDeferredPrepareLoadWidgetPreviewsTasks.add(this);
                    } else {
                        prepareLoadWidgetPreviewsTask(page, items,
                                maxPreviewWidth, maxPreviewHeight, mWidgetCountX);
                    }
                }
            }
        });
    }
    
    protected void onSyncWidgetPageItems(AsyncTaskPageData data) {
    	if(!isSupportWidget())
    		return;
    	
        if (mInTransition) {
        	if(mDeferredSyncWidgetPageItems != null)
        		mDeferredSyncWidgetPageItems.add(data);
            return;
        }
        try {
            int page = data.page;
            PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(page);

            List<Object> items = data.items;
            int count = items.size();
            for (int i = 0; i < count; ++i) {
                PagedViewWidget widget = (PagedViewWidget) layout.getChildAt(i);
                if (widget != null) {
                    Bitmap preview = data.generatedImages.get(i);
                    widget.applyPreview(new FastBitmapDrawable(preview), i);
                }
            }

            if (Util.ENABLE_DEBUG) {
                Util.Log.d(TAG, "onSyncWidgetPageItems: page = " + page + ", layout = " + layout
                    + ", count = " + count + ", this = " + this);
            }

            layout.createHardwareLayer();
            invalidate();

            // Update all thread priorities
            Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
            while (iter.hasNext()) {
                AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
                int pageIndex = task.page;
                task.setThreadPriority(getThreadPriorityForPage(pageIndex));
            }
        } finally {
            data.cleanup(false);
        }
    }
    
    /**
     * Creates and executes a new AsyncTask to load a page of widget previews.
     */
    protected void prepareLoadWidgetPreviewsTask(int page, List<Object> widgets,
            int cellWidth, int cellHeight, int cellCountX) {
        // Prune all tasks that are no longer needed
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int taskPage = task.page;
            if (taskPage < getAssociatedLowerPageBound(getCurrentPage()) ||
                    taskPage > getAssociatedUpperPageBound(getCurrentPage())) {
                task.cancel(false);
                iter.remove();
            } else {
                task.setThreadPriority(getThreadPriorityForPage(taskPage));
            }
        }

        // We introduce a slight delay to order the loading of side pages so that we don't thrash
        final int sleepMs = getSleepForPage(page);
        AsyncTaskPageData pageData = new AsyncTaskPageData(page, widgets, cellWidth, cellHeight,
            new AsyncTaskCallback() {
                @Override
                public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                    try {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (Exception e) {}
                        loadWidgetPreviewsInBackground(task, data);
                    } finally {
                        if (task.isCancelled()) {
                            data.cleanup(true);
                        }
                    }
                }
            },
            new AsyncTaskCallback() {
                @Override
                public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                    mRunningTasks.remove(task);
                    if (task.isCancelled()) {
                        return;
                    }
                    // do cleanup inside onSyncWidgetPageItems
                    onSyncWidgetPageItems(data);
                }
            });

        // Ensure that the task is appropriately prioritized and runs in parallel
        AppsCustomizeAsyncTask t = new AppsCustomizeAsyncTask(page,
                AsyncTaskPageData.Type.LoadWidgetPreviewData);
        t.setThreadPriority(getThreadPriorityForPage(page));
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pageData);
        mRunningTasks.add(t);
    }

    public void setApps(List<ApplicationInfo> list) {
    	if(!isSupportApps())
    		return;
    	
        mApps = list;
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "setApps : mApps = " + mApps.size() + ", mAppsHasSet = "
                    + mAppsHasSet + ", this = " + this);
        }
        mAppsHasSet = true;
        mLauncher.getModel().sortApplicationItems(mApps);
        //Collections.sort(mApps, mLauncher.getModel().getApplicationComparator());
        //reorderApps();
        updatePageCounts();
        invalidateOnDataChange();
    }

    protected void addAppsWithoutInvalidate(List<ApplicationInfo> list) {
        // We add it in place, in alphabetical order
    	if(mApps == null)
    		return;
    	
    	final Comparator<ApplicationInfo> comparator = mLauncher.getModel().getApplicationComparator();
        int count = list.size();
        for (int i = 0; i < count; ++i) {
            ApplicationInfo info = list.get(i);
            int index = Collections.binarySearch(mApps, info, comparator);//LauncherModel.getAppNameComparator());
            if (index < 0) {
                mApps.add(-(index + 1), info);
                if (Util.ENABLE_DEBUG) {
                    Util.Log.d(TAG, "addAppsWithoutInvalidate: mApps size = " + mApps.size()
                            + ", index = " + index + ", info = " + info + ", this = " + this);
                }
            }
        }
    }

    public void addApps(List<ApplicationInfo> list) {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "addApps: list = " + list + ", this = " + this);
        }
        if(!isSupportApps())
    		return;
        
        addAppsWithoutInvalidate(list);
        //reorderApps();
        updatePageCounts();
        invalidateOnDataChange();
    }

    protected void removeAppsWithoutInvalidate(List<ApplicationInfo> list) {
        // loop through all the apps and remove apps that have the same component
    	if(mApps == null)
    		return;
    	
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
            int removeIndex = findAppByComponent(mApps, info);
            if (removeIndex > -1) {
                mApps.remove(removeIndex);
                if (Util.ENABLE_DEBUG) {
                    Util.Log.d(TAG, "removeAppsWithoutInvalidate: removeIndex = " + removeIndex
                            + ", ApplicationInfo info = " + info + ", this = " + this);
                }
            }
        }
    }

    protected void removeAppsWithPackageNameWithoutInvalidate(List<String> packageNames) {
        // loop through all the package names and remove apps that have the same package name
    	if(mApps == null)
    		return;
    	
        for (String pn : packageNames) {
            int removeIndex = findAppByPackage(mApps, pn);
            while (removeIndex > -1) {
                mApps.remove(removeIndex);
                removeIndex = findAppByPackage(mApps, pn);
            }
        }
    }

    public void removeApps(List<String> packageNames) {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "removeApps: packageNames = " + packageNames
                    + ",size = " + mApps.size() + ", this = " + this);
        }

        removeAppsWithPackageNameWithoutInvalidate(packageNames);
        //reorderApps();
        updatePageCounts();
        invalidateOnDataChange();
    }

    public void updateApps(List<ApplicationInfo> list) {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "updateApps: list = " + list + ", this = " + this);
        }

        // We remove and re-add the updated applications list because it's properties may have
        // changed (ie. the title), and this will ensure that the items will be in their proper
        // place in the list.
        removeAppsWithoutInvalidate(list);
        addAppsWithoutInvalidate(list);
        updatePageCounts();
        //reorderApps();
        invalidateOnDataChange();
    }
    
    protected int findAppByComponent(List<ApplicationInfo> list, ApplicationInfo item) {
        ComponentName removeComponent = item.intent.getComponent();
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
            if (info.intent.getComponent().equals(removeComponent)) {
                return i;
            }
        }
        return -1;
    }
    
    protected int findAppByPackage(List<ApplicationInfo> list, String packageName) {
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
            if (ItemInfo.getPackageName(info.intent).equals(packageName)) {
                /// M: we only remove items whose component is in disable state,
                /// this is add to deal the case that there are more than one
                /// activities with LAUNCHER category, and one of them is
                /// disabled may cause all activities removed from app list.
                final boolean isComponentEnabled = Util.isComponentEnabled(getContext(),
                        info.intent.getComponent());
                Util.Log.d(TAG, "findAppByPackage: i = " + i + ",name = "
                        + info.intent.getComponent() + ",isComponentEnabled = "
                        + isComponentEnabled);
                if (!isComponentEnabled) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * M: Update unread number of the given component in app customize paged view
     * with the given value, first find the icon, and then update the number.
     * NOTES: since maybe not all applications are added in the customize paged
     * view, we should update the apps info at the same time.
     * 
     * @param component
     * @param unreadNum
     */
    public void updateAppsUnreadChanged(ComponentName component, int unreadNum) {
        if (Util.DEBUG_UNREAD) {
            Util.Log.d(TAG, "updateAppsUnreadChanged: component = " + component
                    + ",unreadNum = " + unreadNum + ",mNumAppsPages = " + mNumAppsPages);
        }
        updateUnreadNumInAppInfo(component, unreadNum);
        for (int i = 0; i < mNumAppsPages; i++) {
            PagedViewCellLayout cl = (PagedViewCellLayout) getPageAt(i);
            if (cl == null) {
                return;
            }
            final int count = cl.getPageChildCount();
            MtpAppIconView appIcon = null;
            ApplicationInfo appInfo = null;
            for (int j = 0; j < count; j++) {
                appIcon = (MtpAppIconView) cl.getChildOnPageAt(j);
                appInfo = (ApplicationInfo) appIcon.getTag();
                if (Util.DEBUG_UNREAD) {
                    Util.Log.d(TAG, "updateAppsUnreadChanged: component = " + component
                            + ", appInfo = " + appInfo.componentName + ", appIcon = " + appIcon);
                }
                if (appInfo != null && appInfo.componentName.equals(component)) {
                    appIcon.updateUnreadNum(unreadNum);
                }
            }
        }
    }

    /**
     * M: Update unread number of all application info with data in MTKUnreadLoader.
     */
    public void updateAppsUnread() {
        if (Util.DEBUG_UNREAD) {
            Util.Log.d(TAG, "updateAppsUnreadChanged: mNumAppsPages = " + mNumAppsPages);
        }
        if(mApps == null)
    		return;
        
        updateUnreadNumInAppInfo(mApps);
        // Update apps which already shown in the customized pane.
        for (int i = 0; i < mNumAppsPages; i++) {
            PagedViewCellLayout cl = (PagedViewCellLayout) getPageAt(i);
            if (cl == null) {
                return;
            }
            final int count = cl.getPageChildCount();
            MtpAppIconView appIcon = null;
            ApplicationInfo appInfo = null;
            int unreadNum = 0;
            for (int j = 0; j < count; j++) {
                appIcon = (MtpAppIconView) cl.getChildOnPageAt(j);
                appInfo = (ApplicationInfo) appIcon.getTag();
//                unreadNum = MTKUnreadLoader.getUnreadNumberOfComponent(appInfo.componentName);
//                appIcon.updateUnreadNum(unreadNum);
//                if (Util.ENABLE_UNREAD) {
//                    Util.Log.d(TAG, "updateAppsUnreadChanged: i = " + i + ", appInfo = "
//                            + appInfo.componentName + ", unreadNum = " + unreadNum);
//                }
            }
        }
    }
    
    /**
     * M: Update the unread number of the app info with given component.
     * 
     * @param component
     * @param unreadNum
     */
    protected void updateUnreadNumInAppInfo(ComponentName component, int unreadNum) {
    	if(mApps == null)
    		return;
    	
        final int size = mApps.size();
        ApplicationInfo appInfo = null;
        for (int i = 0; i < size; i++) {
            appInfo = mApps.get(i);
            if (appInfo.intent.getComponent().equals(component)) {
                appInfo.unreadNum = unreadNum;
            }
        }
    }
    /**
     * M: Update unread number of all application info with data in MTKUnreadLoader.
     * 
     * @param apps
     */
    public static void updateUnreadNumInAppInfo(final List<ApplicationInfo> apps) {
        final int size = apps.size();
        ApplicationInfo appInfo = null;
        for (int i = 0; i < size; i++) {
            appInfo = apps.get(i);
            //appInfo.unreadNum = MTKUnreadLoader.getUnreadNumberOfComponent(appInfo.componentName);
        }
    }
    
    /**
     * M: invalidate app page items.
     */
    public void invalidateAppPages(int currentPage, boolean immediateAndOnly) {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "invalidateAppPages: currentPage = " + currentPage + ", immediateAndOnly = " + immediateAndOnly);
        }
        invalidatePageData(currentPage, immediateAndOnly);
    }
    
    @Override
    protected String getCurrentPageDescription() {
        int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        IResConfigManager res = mLauncher.getResConfigManager();
        String str = res.getString(IResConfigManager.STR_DEFAULT_SCROLL_FORMAT);
        //int stringId = R.string.default_scroll_format;
        int count = 0;
        
        if (this.isSupportApps() && page < mNumAppsPages) {
        	str = res.getString(IResConfigManager.STR_APPS_CUSTOMIZE_APPS_SCROLL_FORMAT);//R.string.apps_customize_apps_scroll_format;
            count = mNumAppsPages;
        } else if(isSupportWidget()) {
            page -= mNumAppsPages;
            str = res.getString(IResConfigManager.STR_APPS_CUSTOMIZE_WIDGET_SCROLL_FORMAT);//stringId = R.string.apps_customize_widgets_scroll_format;
            count = mNumWidgetPages;
        }

        return String.format(str, page + 1, count);
    }
    
    /*
     * We load an extra page on each side to prevent flashes from scrolling and loading of the
     * widget previews in the background with the AsyncTasks.
     */
    protected final static int sLookBehindPageCount = 2;
    protected final static int sLookAheadPageCount = 2;
    protected int getAssociatedLowerPageBound(int page) {
    	if(isSupportCycleSlidingScreen()){
    		if(isSlideAppAndWidgetTogether())
    			return 0;
    		
	    	if(getCurrentPage() < mNumAppsPages){
	        	return 0;//return Math.max(0, page - 1);
	        } else {
	        	return mNumAppsPages;//Math.max(mNumAppsPages, page - 1);
	        }
    	}
    	
        final int count = getPageCount();
        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
        int windowMinIndex = Math.max(Math.min(page - sLookBehindPageCount, count - windowSize), 0);
        return windowMinIndex;
    }
    protected int getAssociatedUpperPageBound(int page) {
    	if(isSupportCycleSlidingScreen()){
    		if(isSlideAppAndWidgetTogether())
    			return getPageCount() - 1;
    		if(getCurrentPage() < mNumAppsPages){
            	return mNumAppsPages-1;//Math.min(page + 1, mNumAppsPages-1);
            } else {
            	return getPageCount() - 1;//Math.min(page + 1, getPageCount() - 1);
            }
    	}
    	
    	final int count = getPageCount();
        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
        int windowMaxIndex = Math.min(Math.max(page + sLookAheadPageCount, windowSize - 1),
                count - 1);
        return windowMaxIndex;
    }
    
    protected boolean pageScrolled(int xpos, boolean compelete) {

        if(!isSlideAppAndWidgetTogether()){
            if((getCurrentPage() < mNumAppsPages && mNumAppsPages < 2)
                    || (getCurrentPage() >= mNumAppsPages && (getPageCount() - mNumAppsPages) < 2)){
                return false;
            }
        }
        
        return super.pageScrolled(xpos, compelete);
    }
    
    protected int getPositionPageIndex(int xpos, int pagewidth, int[] pages){
        if(xpos < mMinScrollX || xpos > mMaxScrollX){
            if(isSupportCycleSlidingScreen()){
                if(!isSlideAppAndWidgetTogether()){
                    if(getCurrentPage() < mNumAppsPages){
                        pages[0] = mNumAppsPages-1;
                        pages[1] = 0;
                    } else {
                        pages[0] = getPageCount()-1;
                        pages[1] = mNumAppsPages;
                    }
                } else {
                    pages[0] = getPageCount()-1;
                    pages[1] = 0;
                }
            } else {
                pages[0] = -1;
                pages[1] = -1;
                
                if(xpos > mMaxScrollX)
                    return mMaxScrollX;
            }
        } else {
            int maxindex = getPageCount()-1;
            int minindex = 0;
            if(!isSlideAppAndWidgetTogether()){
                if(getCurrentPage() < mNumAppsPages){
                    maxindex = mNumAppsPages-1;
                } else {
                    minindex = mNumAppsPages;
                    //maxindex = (getPageCount() - mNumAppsPages);
                }
            }
            
            pages[0] = Math.min(maxindex, (pagewidth > 0 ? (xpos / pagewidth) : 0));
            
            /*if(pagecount == 2 && (xpos == mMinScrollX || xpos == mMaxScrollX)){
                pages[1] = pages[0];
            } else*/ {
                pages[1] = pages[0]+1;
                if(pages[1] > maxindex)
                    pages[1] = isSupportCycleSlidingScreen() ? minindex : -1;
            }
        }

        return xpos;
    }
//    
//    protected int getPositionPageIndex(int xpos){
//        if(xpos < mMinScrollX){
//            if(isSupportCycleSlidingScreen()){
//                
//                if(!isSlideAppAndWidgetTogether()){
//                    if(getCurrentPage() < mNumAppsPages){
//                        return mNumAppsPages-1;
//                    }
//                }
//                return getPageCount()-1;
//            } 
//            return -1;
//        } else if(xpos > mMaxScrollX){
//            if(!isSlideAppAndWidgetTogether()){
//                if(getCurrentPage() < mNumAppsPages){
//                    return mNumAppsPages-1;
//                }
//            }
//            return getPageCount()-1;
//        } else if(xpos == mMinScrollX || xpos == mMaxScrollX){
//            if(!isSlideAppAndWidgetTogether()){
//                if((getCurrentPage() < mNumAppsPages && mNumAppsPages == 2)
//                        || (getCurrentPage() >= mNumAppsPages && (getPageCount() - mNumAppsPages) == 2)){
//                    return -1;
//                }
//            } else if(getPageCount() == 2){
//                return -1;
//            }
//        }
//        
//        final int width = getWidth();
//        return width > 0 ? (xpos / (width + mPageSpacing)) : 0;
//    }
//    
//    protected int getLeftPageIndex(int page, int xpos){
//        if(!isSlideAppAndWidgetTogether()){
//            if(isSupportCycleSlidingScreen()){
//                if(page == 0){
//                    return mNumAppsPages-1;
//                } else if(page == mNumAppsPages){
//                    return getPageCount()-1;
//                }
//            } else if(page == 0 || (page == mNumAppsPages)){
//                return -1;
//            }
//            return (page - 1);
//        }
//        if(page == 0 && isSupportCycleSlidingScreen()){
//            return getPageCount()-1;
//        }
//        return (page - 1);
//    }
//    
//    protected int getRightPageIndex(int page, int xpos){
//        int ret = page+1;
//        if(!isSlideAppAndWidgetTogether()){
//            if(isSupportCycleSlidingScreen()){
//                if(ret >= getPageCount()){
//                    return mNumAppsPages;
//                } else if(ret == mNumAppsPages){
//                    return 0;
//                }
//            } else if(ret == getPageCount() || (ret == mNumAppsPages)){
//                return -1;
//            }
//            return ret;
//        }
//        
//        if(ret >= getPageCount()){
//            return isSupportCycleSlidingScreen() ? 0 : -1;
//        }
//        return ret;
//    }
}
