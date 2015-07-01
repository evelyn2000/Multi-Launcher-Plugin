/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.jzs.dr.mtplauncher.defaults.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Insets;
import android.graphics.MaskFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.TableMaskFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.jzs.dr.mtplauncher.LauncherActivity;
import com.jzs.dr.mtplauncher.R;
import com.jzs.dr.mtplauncher.ctrl.FocusHelperDefault;
import com.jzs.dr.mtplauncher.defaults.LauncherDefault;
import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.ctrl.AppsCustomizeAsyncTask;
import com.jzs.dr.mtplauncher.sjar.ctrl.AsyncTaskCallback;
import com.jzs.dr.mtplauncher.sjar.ctrl.AsyncTaskPageData;
import com.jzs.dr.mtplauncher.sjar.ctrl.BitmapCache;
import com.jzs.dr.mtplauncher.sjar.ctrl.CanvasCache;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragController;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget.DragObject;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherAnimUtils;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherTransitionable;
import com.jzs.dr.mtplauncher.sjar.ctrl.PaintCache;
import com.jzs.dr.mtplauncher.sjar.ctrl.RectCache;
import com.jzs.dr.mtplauncher.sjar.model.AllAppsList;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.dr.mtplauncher.sjar.model.IconCache;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherModel;
import com.jzs.dr.mtplauncher.sjar.model.LauncherSettings;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.widget.AppWidgetResizeFrame;
import com.jzs.dr.mtplauncher.sjar.widget.AppsCustomizePagedView;
import com.jzs.dr.mtplauncher.sjar.widget.CellLayout;
import com.jzs.dr.mtplauncher.sjar.widget.DragLayer;
import com.jzs.dr.mtplauncher.sjar.widget.FastBitmapDrawable;
import com.jzs.dr.mtplauncher.sjar.widget.MtpAppIconView;
import com.jzs.dr.mtplauncher.sjar.widget.PagedView;
import com.jzs.dr.mtplauncher.sjar.widget.PagedViewCellLayout;
import com.jzs.dr.mtplauncher.sjar.widget.PagedViewCellLayoutChildren;
import com.jzs.dr.mtplauncher.sjar.widget.PagedViewGridLayout;
import com.jzs.dr.mtplauncher.sjar.widget.PagedViewIcon;
import com.jzs.dr.mtplauncher.sjar.widget.PagedViewWidget;
import com.jzs.dr.mtplauncher.sjar.widget.PagedViewWithDraggableItems;
import com.jzs.dr.mtplauncher.sjar.widget.Workspace;
import com.jzs.dr.mtplauncher.LauncherApplicationMain;
import com.jzs.dr.mtplauncher.sjar.widget.DeleteDropTarget;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * The Apps/Customize page that displays all the applications, widgets, and shortcuts.
 */
public class AppsCustomizePagedViewDefault extends AppsCustomizePagedView {
    private static final String TAG = "AppsCustomizePagedViewDefault";

    /**
     * The different content types that this paged view can show.
     */
    public enum ContentType {
        Applications,
        Widgets
    }

    // Refs
    //private LauncherDefault mLauncher;
    //private DragController mDragController;
    

    // Save and Restore
//    private int mSaveInstanceStateItemIndex = -1;
    //private PagedViewIcon mPressedIcon;

    // Content
//    private ArrayList<ApplicationInfo> mApps;
//    private ArrayList<Object> mWidgets;

    // Cling
    private boolean mHasShownAllAppsCling;
    private int mClingFocusedX;
    private int mClingFocusedY;

    // Caching
    private Canvas mCanvas;
    //private IconCache mIconCache;

    // Dimens
//    private int mContentWidth;
//    private int mAppIconSize;
//    private int mMaxAppCellCountX, mMaxAppCellCountY;
//    private int mWidgetCountX, mWidgetCountY;
//    private int mWidgetWidthGap, mWidgetHeightGap;
//    private final float sWidgetPreviewIconPaddingPercentage = 0.25f;
    //private PagedViewCellLayout mWidgetSpacingLayout;
//    private int mNumAppsPages;
//    private int mNumWidgetPages;

    // Relating to the scroll and overscroll effects
    public Workspace.ZInterpolator mZInterpolator = new Workspace.ZInterpolator(0.5f);
    private static float CAMERA_DISTANCE = 6500;
    private static float TRANSITION_SCALE_FACTOR = 0.74f;
    private static float TRANSITION_PIVOT = 0.65f;
    private static float TRANSITION_MAX_ROTATION = 22;
    private static final boolean PERFORM_OVERSCROLL_ROTATION = true;
    private AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);
    private DecelerateInterpolator mLeftScreenAlphaInterpolator = new DecelerateInterpolator(4);

    // Previews & outlines
    
    

    private Runnable mInflateWidgetRunnable = null;
    private Runnable mBindWidgetRunnable = null;
    
    public int mWidgetCleanupState = WIDGET_NO_CLEANUP_REQUIRED;
    public int mWidgetLoadingId = -1;
    public PendingAddWidgetInfo mCreateWidgetInfo = null;
    //private boolean mDraggingWidget = false;


    

    public AppsCustomizePagedViewDefault(Context context, AttributeSet attrs) {
        super(context, attrs);
        
//        mApps = new ArrayList<ApplicationInfo>();
//        mWidgets = new ArrayList<Object>();
//        mIconCache = ((LauncherApplicationMain) context.getApplicationContext()).getIconCache();
        mCanvas = new Canvas();
        

        // Save the default widget preview background
        //Resources resources = context.getResources();
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppsCustomizePagedView, 0, 0);
        mClingFocusedX = a.getInt(R.styleable.AppsCustomizePagedView_clingFocusedX, 0);
        mClingFocusedY = a.getInt(R.styleable.AppsCustomizePagedView_clingFocusedY, 0);
        a.recycle();
        
        
        //mWidgetSpacingLayout = (PagedViewCellLayout)mLayoutInflater.inflate(R.layout.apps_customize_page_screen, this, false);//new PagedViewCellLayout(getContext());

        
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
                int numApps = mApps.size();
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

    /** Returns the page in the current orientation which is expected to contain the specified
     *  item index. */
//    public int getPageForComponent(int index) {
//        if (index < 0) return 0;
//
//        if (index < mApps.size()) {
//            int numItemsPerPage = mCellCountX * mCellCountY;
//            return (index / numItemsPerPage);
//        } else {
//            int numItemsPerPage = mWidgetCountX * mWidgetCountY;
//            return mNumAppsPages + ((index - mApps.size()) / numItemsPerPage);
//        }
//    }

    public void showAllAppsCling() {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "showAllAppsCling: mHasShownAllAppsCling = " + mHasShownAllAppsCling);
        }

//        if (!mHasShownAllAppsCling && isDataReady()) {
//            mHasShownAllAppsCling = true;
//            // Calculate the position for the cling punch through
//            int[] offset = new int[2];
//            int[] pos = mWidgetSpacingLayout.estimateCellPosition(mClingFocusedX, mClingFocusedY);
//            mLauncher.getDragLayer().getLocationInDragLayer(this, offset);
//            // PagedViews are centered horizontally but top aligned
//            // Note we have to shift the items up now that Launcher sits under the status bar
//            pos[0] += (getMeasuredWidth() - mWidgetSpacingLayout.getMeasuredWidth()) / 2 +
//                    offset[0];
//            pos[1] += offset[1] - mLauncher.getDragLayer().getPaddingTop();
//            mLauncher.showFirstRunAllAppsCling(pos);
//        }
    }

    

    

    private void preloadWidget(final PendingAddWidgetInfo info) {
        final AppWidgetProviderInfo pInfo = info.info;
        final Bundle options = getDefaultOptionsForWidget(mLauncher, info);

        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "preloadWidget info = " + info + ", pInfo = " + pInfo + 
        			", pInfo.configure = " + pInfo.configure);
        }

        if (pInfo.configure != null) {
            info.bindOptions = options;
            return;
        }

        mWidgetCleanupState = WIDGET_PRELOAD_PENDING;
        mBindWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                mWidgetLoadingId = mLauncher.getAppWidgetHost().allocateAppWidgetId();
                // Options will be null for platforms with JB or lower, so this serves as an
                // SDK level check.
                if (options == null) {
                    if (AppWidgetManager.getInstance(mLauncher.getActivity()).bindAppWidgetIdIfAllowed(
                            mWidgetLoadingId, info.componentName)) {
                        mWidgetCleanupState = WIDGET_BOUND;
                    }
                } else {
                    if (AppWidgetManager.getInstance(mLauncher.getActivity()).bindAppWidgetIdIfAllowed(
                            mWidgetLoadingId, info.componentName, options)) {
                        mWidgetCleanupState = WIDGET_BOUND;
                    }
                }
            }
        };
        post(mBindWidgetRunnable);

        mInflateWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                if (mWidgetCleanupState != WIDGET_BOUND) {
                    return;
                }
                AppWidgetHostView hostView = mLauncher.
                        getAppWidgetHost().createView(getContext(), mWidgetLoadingId, pInfo);
                info.boundWidget = hostView;
                mWidgetCleanupState = WIDGET_INFLATED;
                hostView.setVisibility(INVISIBLE);
                int[] unScaledSize = mLauncher.getWorkspace().estimateItemSize(info.spanX,
                        info.spanY, info, false);

                // We want the first widget layout to be the correct size. This will be important
                // for width size reporting to the AppWidgetManager.
                DragLayer.LayoutParams lp = new DragLayer.LayoutParams(unScaledSize[0],
                        unScaledSize[1]);
                lp.x = lp.y = 0;
                lp.customPosition = true;
                hostView.setLayoutParams(lp);
                mLauncher.getDragLayer().addView(hostView);
            }
        };
        post(mInflateWidgetRunnable);
    }

    @Override
    public void onShortPress(View v) {
    	if (Util.ENABLE_DEBUG) {
    		Util.Log.d(TAG, "onShortcutPress v = " + v + ", v.getTag() = " + v.getTag());
    	}

        // We are anticipating a long press, and we use this time to load bind and instantiate
        // the widget. This will need to be cleaned up if it turns out no long press occurs.
        if (mCreateWidgetInfo != null) {
            // Just in case the cleanup process wasn't properly executed. This shouldn't happen.
            cleanupWidgetPreloading(false);
        }
        mCreateWidgetInfo = new PendingAddWidgetInfo((PendingAddWidgetInfo) v.getTag());
        preloadWidget(mCreateWidgetInfo);
    }

    protected void cleanupWidgetPreloading(boolean widgetWasAdded) {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "cleanupWidgetPreloading widgetWasAdded = " + widgetWasAdded
                    + ", mCreateWidgetInfo = " + mCreateWidgetInfo + ", mWidgetLoadingId = "
                    + mWidgetLoadingId);
        }

        if (!widgetWasAdded) {
            // If the widget was not added, we may need to do further cleanup.
            PendingAddWidgetInfo info = mCreateWidgetInfo;
            mCreateWidgetInfo = null;

            if (mWidgetCleanupState == WIDGET_PRELOAD_PENDING) {
                // We never did any preloading, so just remove pending callbacks to do so
                removeCallbacks(mBindWidgetRunnable);
                removeCallbacks(mInflateWidgetRunnable);
            } else if (mWidgetCleanupState == WIDGET_BOUND) {
                 // Delete the widget id which was allocated
                if (mWidgetLoadingId != -1) {
                    mLauncher.getAppWidgetHost().deleteAppWidgetId(mWidgetLoadingId);
                }

                // We never got around to inflating the widget, so remove the callback to do so.
                removeCallbacks(mInflateWidgetRunnable);
            } else if (mWidgetCleanupState == WIDGET_INFLATED) {
                // Delete the widget id which was allocated
                if (mWidgetLoadingId != -1) {
                    mLauncher.getAppWidgetHost().deleteAppWidgetId(mWidgetLoadingId);
                }

                // The widget was inflated and added to the DragLayer -- remove it.
                AppWidgetHostView widget = info.boundWidget;
                mLauncher.getDragLayer().removeView(widget);
            }
        }
        mWidgetCleanupState = WIDGET_NO_CLEANUP_REQUIRED;
        mWidgetLoadingId = -1;
        mCreateWidgetInfo = null;
        PagedViewWidget.resetShortPressTarget();
    }

    @Override
    protected boolean beginDraggingWidget(View v) {
    	super.beginDraggingWidget(v);
        mDraggingWidget = true;
        // Get the widget preview as the drag representation
        ImageView image = (ImageView) v.findViewWithTag("widget_preview");//.findViewById(R.id.widget_preview);
        PendingAddItemInfo createItemInfo = (PendingAddItemInfo) v.getTag();

        if (Util.DEBUG_DRAG) {
            Util.Log.d(TAG, "beginDraggingWidget: createItemInfo = " + createItemInfo 
                    + ", v = " + v + ", image = " + image + ", this = " + this);
        }

        // If the ImageView doesn't have a drawable yet, the widget preview hasn't been loaded and
        // we abort the drag.
        if (image == null || image.getDrawable() == null) {
            mDraggingWidget = false;
            return false;
        }

        // Compose the drag image
        Bitmap preview;
        Bitmap outline;
        float scale = 1f;
        if (createItemInfo instanceof PendingAddWidgetInfo) {
            // This can happen in some weird cases involving multi-touch. We can't start dragging
            // the widget if this is null, so we break out.
            if (mCreateWidgetInfo == null) {
                return false;
            }

            PendingAddWidgetInfo createWidgetInfo = mCreateWidgetInfo;
            createItemInfo = createWidgetInfo;
            int spanX = createItemInfo.spanX;
            int spanY = createItemInfo.spanY;
            int[] size = mLauncher.getWorkspace().estimateItemSize(spanX, spanY,
                    createWidgetInfo, true);

            FastBitmapDrawable previewDrawable = (FastBitmapDrawable) image.getDrawable();
            float minScale = 1.25f;
            int maxWidth, maxHeight;
            maxWidth = Math.min((int) (previewDrawable.getIntrinsicWidth() * minScale), size[0]);
            maxHeight = Math.min((int) (previewDrawable.getIntrinsicHeight() * minScale), size[1]);
            preview = getWidgetPreview(createWidgetInfo.componentName, createWidgetInfo.previewImage,
                    createWidgetInfo.icon, spanX, spanY, maxWidth, maxHeight);

            // Determine the image view drawable scale relative to the preview
            float[] mv = new float[9];
            Matrix m = new Matrix();
            m.setRectToRect(
                    new RectF(0f, 0f, (float) preview.getWidth(), (float) preview.getHeight()),
                    new RectF(0f, 0f, (float) previewDrawable.getIntrinsicWidth(),
                            (float) previewDrawable.getIntrinsicHeight()),
                    Matrix.ScaleToFit.START);
            m.getValues(mv);
            scale = (float) mv[0];
        } else {
            PendingAddShortcutInfo createShortcutInfo = (PendingAddShortcutInfo) v.getTag();
            Drawable icon = mIconCache.getFullResIcon(createShortcutInfo.shortcutActivityInfo);
            preview = Bitmap.createBitmap(icon.getIntrinsicWidth(),
                    icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

            mCanvas.setBitmap(preview);
            mCanvas.save();
            renderDrawableToBitmap(icon, preview, 0, 0,
                    icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            mCanvas.restore();
            mCanvas.setBitmap(null);
            createItemInfo.spanX = createItemInfo.spanY = 1;
        }

        // Don't clip alpha values for the drag outline if we're using the default widget preview
        boolean clipAlpha = !(createItemInfo instanceof PendingAddWidgetInfo &&
                (((PendingAddWidgetInfo) createItemInfo).previewImage == 0));

        // Save the preview for the outline generation, then dim the preview
        outline = Bitmap.createScaledBitmap(preview, preview.getWidth(), preview.getHeight(),
                false);

        // Start the drag
        mLauncher.lockScreenOrientation();
        mLauncher.getWorkspace().onDragStartedWithItem(createItemInfo, outline, clipAlpha);
        mDragController.startDrag(image, preview, this, createItemInfo,
                DragController.DRAG_ACTION_COPY, null, scale);
        outline.recycle();
        preview.recycle();
        return true;
    }


//    public void setContentType(int type) {
//        if (type == CONTENTTYPE_WIDGETS) {
//            invalidatePageData(mNumAppsPages, true);
//        } else if (type == CONTENTTYPE_APPS_ALL) {
//            invalidatePageData(0, true);
//        }
//    }    

    protected void updateCurrentTab(int currentPage) {
        AppsCustomizeTabHost tabHost = getTabHost();
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "updateCurrentTab: currentPage = " + currentPage
                    + ", mCurrentPage = " + mCurrentPage + ", this = " + this);
        }

        if (tabHost != null) {
            String tag = tabHost.getCurrentTabTag();
            if (tag != null) {
                if (currentPage >= mNumAppsPages &&
                        !tag.equals(tabHost.getTabTagForContentType(CONTENTTYPE_WIDGETS))) {
                    tabHost.setCurrentTabFromContent(CONTENTTYPE_WIDGETS);
                } else if (currentPage < mNumAppsPages &&
                        !tag.equals(tabHost.getTabTagForContentType(CONTENTTYPE_APPS_ALL))) {
                    tabHost.setCurrentTabFromContent(CONTENTTYPE_APPS_ALL);
                }
            }
        }
    }

    // In apps customize, we have a scrolling effect which emulates pulling cards off of a stack.
    @Override
    protected void screenScrolled(int screenCenter) {
        super.screenScrolled(screenCenter);

        if (Util.DEBUG_LAYOUT) {
            Util.Log.d(TAG, "screenScrolled: screenCenter = " + screenCenter
                    + ", mOverScrollX = " + mOverScrollX + ", mMaxScrollX = " + mMaxScrollX
                    + ", mMinScrollX = " + mMinScrollX
                    + ", mScrollX = " + getScrollX() + ", this = " + this);
        }

        for (int i = 0; i < getChildCount(); i++) {
            View v = getPageAt(i);
            if (v != null) {
                float scrollProgress = getScrollProgress(screenCenter, v, i);

                float interpolatedProgress =
                        mZInterpolator.getInterpolation(Math.abs(Math.min(scrollProgress, 0)));
                float scale = (1 - interpolatedProgress) +
                        interpolatedProgress * TRANSITION_SCALE_FACTOR;
                float translationX = Math.min(0, scrollProgress) * v.getMeasuredWidth();

                float alpha;

                if (scrollProgress < 0) {
                    alpha = scrollProgress < 0 ? mAlphaInterpolator.getInterpolation(
                        1 - Math.abs(scrollProgress)) : 1.0f;
                } else {
                    // On large screens we need to fade the page as it nears its leftmost position
                    alpha = mLeftScreenAlphaInterpolator.getInterpolation(1 - scrollProgress);
                }

                v.setCameraDistance(mDensity * CAMERA_DISTANCE);
                int pageWidth = v.getMeasuredWidth();
                int pageHeight = v.getMeasuredHeight();

                if (PERFORM_OVERSCROLL_ROTATION) {
                	alpha = 1.0f;
//                    if (i == 0 && scrollProgress < 0) {
//                        // Overscroll to the left
//                        v.setPivotX(TRANSITION_PIVOT * pageWidth);
//                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
//                        scale = 1.0f;
//                        alpha = 1.0f;
//                        // On the first page, we don't want the page to have any lateral motion
//                        translationX = 0;
//                    } else if (i == getChildCount() - 1 && scrollProgress > 0) {
//                        // Overscroll to the right
//                        v.setPivotX((1 - TRANSITION_PIVOT) * pageWidth);
//                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
//                        scale = 1.0f;
//                        alpha = 1.0f;
//                        // On the last page, we don't want the page to have any lateral motion.
//                        translationX = 0;
//                    } else {
//                        v.setPivotY(pageHeight / 2.0f);
//                        v.setPivotX(pageWidth / 2.0f);
//                        v.setRotationY(0f);
//                    }
                }

//                v.setTranslationX(translationX);
//                v.setScaleX(scale);
//                v.setScaleY(scale);
                v.setAlpha(alpha);

                // If the view has 0 alpha, we set it to be invisible so as to prevent
                // it from accepting touches
                if (alpha == 0) {
                    v.setVisibility(INVISIBLE);
                } else if (v.getVisibility() != VISIBLE) {
                    v.setVisibility(VISIBLE);
                }
            }
        }
    }


    

    

    public void reset() {
        super.reset();

        AppsCustomizeTabHost tabHost = getTabHost();
        String tag = tabHost.getCurrentTabTag();
        if (tag != null) {
            if (!tag.equals(tabHost.getTabTagForContentType(CONTENTTYPE_APPS_ALL))) {
                tabHost.setCurrentTabFromContent(CONTENTTYPE_APPS_ALL);
            }
        }

        if (mCurrentPage != 0) {
            invalidatePageData(0);
        }
    }

    private AppsCustomizeTabHost getTabHost() {
        return (AppsCustomizeTabHost) mLauncher.findViewById(R.id.apps_customize_pane);
    }

    
    
    

    

    /**
     * M: Reorder apps in applist.
     */
    public void reorderApps() {
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "reorderApps: mApps = " + mApps + ", this = " + this);
        }
//        if (AllAppsList.sTopPackages == null || mApps == null || mApps.isEmpty()
//                || AllAppsList.sTopPackages.isEmpty()) {
//            return;
//        }
//
//        ArrayList<ApplicationInfo> dataReorder = new ArrayList<ApplicationInfo>(
//                AllAppsList.DEFAULT_APPLICATIONS_NUMBER);
//
//        for (AllAppsList.TopPackage tp : AllAppsList.sTopPackages) {
//            for (ApplicationInfo ai : mApps) {
//                if (ai.componentName.getPackageName().equals(tp.packageName)
//                        && ai.componentName.getClassName().equals(tp.className)) {
//                    mApps.remove(ai);
//                    dataReorder.add(ai);
//                    break;
//                }
//            }
//        }
//
//        for (AllAppsList.TopPackage tp : AllAppsList.sTopPackages) {
//            int newIndex = 0;
//            for (ApplicationInfo ai : dataReorder) {
//                if (ai.componentName.getPackageName().equals(tp.packageName)
//                        && ai.componentName.getClassName().equals(tp.className)) {
//                    newIndex = Math.min(Math.max(tp.order, 0), mApps.size());
//                    mApps.add(newIndex, ai);
//                    break;
//                }
//            }
//        }
    }

    

    
    
    
} 
