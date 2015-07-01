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

package com.jzs.dr.mtplauncher.defaults.widget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
//import android.widget.IMTKWidget;
import android.widget.TextView;

import com.jzs.dr.mtplauncher.LauncherActivity;
import com.jzs.dr.mtplauncher.R;
//import com.jzs.dr.mtplauncher.ctrl.InstallShortcutReceiver;
import com.jzs.dr.mtplauncher.ctrl.MtpUnreadLoader;
//import com.jzs.dr.mtplauncher.ctrl.UninstallShortcutReceiver;
import com.jzs.dr.mtplauncher.defaults.LauncherDefault;
import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.LauncherApplication;
import com.jzs.dr.mtplauncher.sjar.ctrl.Alarm;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragScroller;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragController;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget.DragObject;
import com.jzs.dr.mtplauncher.sjar.ctrl.HolographicOutlineHelper;
import com.jzs.dr.mtplauncher.sjar.ctrl.IconKeyEventListener;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherAnimUtils;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherAnimatorUpdateListener;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherTransitionable;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherViewPropertyAnimator;
import com.jzs.dr.mtplauncher.sjar.ctrl.OnAlarmListener;
import com.jzs.dr.mtplauncher.sjar.ctrl.PageSwitchListener;
import com.jzs.dr.mtplauncher.sjar.ctrl.SpringLoadedDragController;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.FolderInfo;
import com.jzs.dr.mtplauncher.sjar.model.IconCache;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherAppWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherModel;
import com.jzs.dr.mtplauncher.sjar.model.LauncherSettings;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddWidgetInfo;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.widget.AppWidgetResizeFrame;
import com.jzs.dr.mtplauncher.sjar.widget.BubbleTextView;
import com.jzs.dr.mtplauncher.sjar.widget.CellLayout;
import com.jzs.dr.mtplauncher.sjar.widget.CellLayout.CellInfo;
import com.jzs.dr.mtplauncher.sjar.widget.DragLayer;
import com.jzs.dr.mtplauncher.sjar.widget.DragView;
import com.jzs.dr.mtplauncher.sjar.widget.Folder;
import com.jzs.dr.mtplauncher.sjar.widget.FolderIcon;
import com.jzs.dr.mtplauncher.sjar.widget.FolderIcon.FolderRingAnimator;
import com.jzs.dr.mtplauncher.sjar.widget.Hotseat;
import com.jzs.dr.mtplauncher.sjar.widget.LauncherAppWidgetHostView;
import com.jzs.dr.mtplauncher.sjar.widget.MtpShortcutView;
import com.jzs.dr.mtplauncher.sjar.widget.PageViewIndicator;
import com.jzs.dr.mtplauncher.sjar.widget.ShortcutAndWidgetContainer;
import com.jzs.dr.mtplauncher.sjar.widget.PagedViewIcon;
import com.jzs.dr.mtplauncher.sjar.widget.SmoothPagedView;
import com.jzs.dr.mtplauncher.sjar.widget.Workspace;
//import com.android.launcher2.FolderIcon.FolderRingAnimator;
//import com.android.launcher2.LauncherSettings.Favorites;
//import com.jzs.dr.mtplauncher.sjar.ctrl.PageSwitchListener;
import com.jzs.dr.mtplauncher.sjar.widget.SearchDropTargetBar;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The workspace is a wide area with a wallpaper and a finite number of pages.
 * Each page contains a number of icons, folders or widgets the user can
 * interact with. A workspace is meant to be used with a fixed width only.
 */
public class WorkspaceDefault extends Workspace {
    private static final String TAG = "Workspace";

    // Y rotation to apply to the workspace screens
    private static final float WORKSPACE_OVERSCROLL_ROTATION = 24f;

    private static final int CHILDREN_OUTLINE_FADE_OUT_DELAY = 0;
    private static final int CHILDREN_OUTLINE_FADE_OUT_DURATION = 375;
    private static final int CHILDREN_OUTLINE_FADE_IN_DURATION = 100;
    
    private static final int DEFAULT_CELL_COUNT_X_HOR = 6;
    private static final int DEFAULT_CELL_COUNT_Y_HOR = 3;
   

    // These animators are used to fade the children's outlines
    private ObjectAnimator mChildrenOutlineFadeInAnimation;
    private ObjectAnimator mChildrenOutlineFadeOutAnimation;

    // These properties refer to the background protection gradient used for AllApps and Customize
    //private ValueAnimator mBackgroundFadeInAnimation;
    //private ValueAnimator mBackgroundFadeOutAnimation;
    
    private int mOriginalPageSpacing;

    private float mOverscrollFade = 0;
    private boolean mOverscrollTransformsSet;

    private int mSpringLoadedPageSpacing;
    private int mCameraDistance;


    // These variables are used for storing the initial and final values during workspace animations
    private int mSavedScrollX;
    private float mSavedRotationY;
    private float mSavedTranslationX;
    private float mCurrentScaleX;
    private float mCurrentScaleY;
    private float mCurrentRotationY;
    private float mCurrentTranslationX;
    private float mCurrentTranslationY;
    private float[] mOldTranslationXs;
    private float[] mOldTranslationYs;
    private float[] mOldScaleXs;
    private float[] mOldScaleYs;
    private float[] mOldBackgroundAlphas;
    private float[] mOldAlphas;
    private float[] mNewTranslationXs;
    private float[] mNewTranslationYs;
    private float[] mNewScaleXs;
    private float[] mNewScaleYs;
    private float[] mNewBackgroundAlphas;
    private float[] mNewAlphas;
    private float[] mNewRotationYs;
    private float mTransitionProgress;

    

    /// M: added for scene feature, the string of the default wallpaper.
    private static final String DEFAULT_WALLPAPER = "default_wallpaper";

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     */
    public WorkspaceDefault(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     * @param defStyle Unused.
     */
    public WorkspaceDefault(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        mOriginalPageSpacing = mPageSpacing;

        //mLauncher = (LauncherDefault) context;
        final Resources res = getResources();
        mWorkspaceFadeInAdjacentScreens = res.getBoolean(R.bool.config_workspaceFadeAdjacentScreens);
        mFadeInAdjacentScreens = false;
        //mWallpaperManager = WallpaperManager.getInstance(context);

        int cellCountX = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
        		? DEFAULT_CELL_COUNT_X_HOR : DEFAULT_CELL_COUNT_X;
        int cellCountY = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
        		? DEFAULT_CELL_COUNT_Y_HOR :DEFAULT_CELL_COUNT_Y;

//        TypedArray a = context.obtainStyledAttributes(attrs,
//                R.styleable.Workspace, defStyle, 0);

//        if (LauncherApplication.isScreenLarge()) {
//            // Determine number of rows/columns dynamically
//            // TODO: This code currently fails on tablets with an aspect ratio < 1.3.
//            // Around that ratio we should make cells the same size in portrait and
//            // landscape
//            TypedArray actionBarSizeTypedArray =
//                context.obtainStyledAttributes(new int[] { android.R.attr.actionBarSize });
//            final float actionBarHeight = actionBarSizeTypedArray.getDimension(0, 0f);
//
//            Point minDims = new Point();
//            Point maxDims = new Point();
//            mLauncher.getWindowManager().getDefaultDisplay().getCurrentSizeRange(minDims, maxDims);
//
//            cellCountX = 1;
//            while (CellLayout.widthInPortrait(res, cellCountX + 1) <= minDims.x) {
//                cellCountX++;
//            }
//
//            cellCountY = 1;
//            while (actionBarHeight + CellLayout.heightInLandscape(res, cellCountY + 1)
//                <= minDims.y) {
//                cellCountY++;
//            }
//        }

        mSpringLoadedShrinkFactor =
            res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f;
        mSpringLoadedPageSpacing =
                res.getDimensionPixelSize(R.dimen.workspace_spring_loaded_page_spacing);
        mCameraDistance = res.getInteger(R.integer.config_cameraDistance);

//        a.recycle();

        setHapticFeedbackEnabled(false);

        initWorkspace();

        // Disable multitouch across the workspace/all apps/customize tray
        setMotionEventSplittingEnabled(true);

        // Unless otherwise specified this view is important for accessibility.
        if (getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
        

    }
    /**
     * Initializes various states for this workspace.
     */
    protected void initWorkspace() {
    	super.initWorkspace();
        Context context = getContext();
//        mCurrentPage = getDefaultScreen();
//        Launcher.setScreen(mCurrentPage);
////        LauncherApplication app = (LauncherApplication)context.getApplicationContext();
//        mIconCache = mLauncher.getIconCache();
//        setWillNotDraw(false);
//        setChildrenDrawnWithCacheEnabled(true);

        final Resources res = getResources();
        try {
            mBackground = res.getDrawable(R.drawable.apps_customize_bg);
        } catch (Resources.NotFoundException e) {
            // In this case, we will skip drawing background protection
        }

//        mWallpaperOffset = new WallpaperOffsetInterpolator();
//        Display display = mLauncher.getWindowManager().getDefaultDisplay();
//        display.getSize(mDisplaySize);
//        mWallpaperTravelWidth = (int) (mDisplaySize.x *
//                wallpaperTravelToScreenWidthRatio(mDisplaySize.x, mDisplaySize.y));
//
//        mMaxDistanceForFolderCreation = (0.55f * res.getDimensionPixelSize(R.dimen.app_icon_size));
//        mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * mDensity);
    }


    public void showOutlines() {
        if (!isSmall() && !isSwitchingState()) {
            if (mChildrenOutlineFadeOutAnimation != null) mChildrenOutlineFadeOutAnimation.cancel();
            if (mChildrenOutlineFadeInAnimation != null) mChildrenOutlineFadeInAnimation.cancel();
            mChildrenOutlineFadeInAnimation = LauncherAnimUtils.ofFloat(this, "childrenOutlineAlpha", 1.0f);
            mChildrenOutlineFadeInAnimation.setDuration(CHILDREN_OUTLINE_FADE_IN_DURATION);
            mChildrenOutlineFadeInAnimation.start();
        }
    }

    public void hideOutlines() {
        if (!isSmall() && !isSwitchingState()) {
            if (mChildrenOutlineFadeInAnimation != null) mChildrenOutlineFadeInAnimation.cancel();
            if (mChildrenOutlineFadeOutAnimation != null) mChildrenOutlineFadeOutAnimation.cancel();
            mChildrenOutlineFadeOutAnimation = LauncherAnimUtils.ofFloat(this, "childrenOutlineAlpha", 0.0f);
            mChildrenOutlineFadeOutAnimation.setDuration(CHILDREN_OUTLINE_FADE_OUT_DURATION);
            mChildrenOutlineFadeOutAnimation.setStartDelay(CHILDREN_OUTLINE_FADE_OUT_DELAY);
            mChildrenOutlineFadeOutAnimation.start();
        }
    }

    

    private void animateBackgroundGradient(float finalAlpha, boolean animated) {
        if (mBackground == null) return;
//        if (mBackgroundFadeInAnimation != null) {
//            mBackgroundFadeInAnimation.cancel();
//            mBackgroundFadeInAnimation = null;
//        }
//        if (mBackgroundFadeOutAnimation != null) {
//            mBackgroundFadeOutAnimation.cancel();
//            mBackgroundFadeOutAnimation = null;
//        }
//        float startAlpha = getBackgroundAlpha();
//        if (finalAlpha != startAlpha) {
//            if (animated) {
//                mBackgroundFadeOutAnimation = LauncherAnimUtils.ofFloat(startAlpha, finalAlpha);
//                mBackgroundFadeOutAnimation.addUpdateListener(new AnimatorUpdateListener() {
//                    public void onAnimationUpdate(ValueAnimator animation) {
//                        setBackgroundAlpha(((Float) animation.getAnimatedValue()).floatValue());
//                    }
//                });
//                mBackgroundFadeOutAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
//                mBackgroundFadeOutAnimation.setDuration(BACKGROUND_FADE_OUT_DURATION);
//                mBackgroundFadeOutAnimation.start();
//            } else {
//                setBackgroundAlpha(finalAlpha);
//            }
//        }
    }

    

    @Override
    protected void screenScrolled(int screenCenter) {
        super.screenScrolled(screenCenter);

        updatePageAlphaValues(screenCenter);
        enableHwLayersOnVisiblePages();

        if (mOverScrollX < 0 || mOverScrollX > mMaxScrollX) {
            int index = mOverScrollX < 0 ? 0 : getChildCount() - 1;
            CellLayout cl = (CellLayout) getChildAt(index);
            float scrollProgress = getScrollProgress(screenCenter, cl, index);
            cl.setOverScrollAmount(Math.abs(scrollProgress), index == 0);
            float rotation = - WORKSPACE_OVERSCROLL_ROTATION * scrollProgress;
            cl.setRotationY(rotation);
            setFadeForOverScroll(Math.abs(scrollProgress));
            if (!mOverscrollTransformsSet) {
                mOverscrollTransformsSet = true;
                cl.setCameraDistance(mDensity * mCameraDistance);
                cl.setPivotX(cl.getMeasuredWidth() * (index == 0 ? 0.75f : 0.25f));
                cl.setPivotY(cl.getMeasuredHeight() * 0.5f);
                cl.setOverscrollTransformsDirty(true);
            }
        } else {
            if (mOverscrollFade != 0) {
                setFadeForOverScroll(0);
            }
            if (mOverscrollTransformsSet) {
                mOverscrollTransformsSet = false;
                ((CellLayout) getChildAt(0)).resetOverscrollTransforms();
                ((CellLayout) getChildAt(getChildCount() - 1)).resetOverscrollTransforms();
            }
        }
    }

       

    

    private final ZoomInInterpolator mZoomInInterpolator = new ZoomInInterpolator();

    

    @Override
    public void onChildViewAdded(View parent, View child) {
        super.onChildViewAdded(parent, child);
        mOldTranslationXs = null;
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        super.onChildViewRemoved(parent, child);
        mOldTranslationXs = null;
    }

    private void initAnimationArrays() {
        final int childCount = /*super.isSupportEditPageScreen() ? getMaxScreenCount() : */getChildCount();
        if (mOldTranslationXs != null) return;
        mOldTranslationXs = new float[childCount];
        mOldTranslationYs = new float[childCount];
        mOldScaleXs = new float[childCount];
        mOldScaleYs = new float[childCount];
        mOldBackgroundAlphas = new float[childCount];
        mOldAlphas = new float[childCount];
        mNewTranslationXs = new float[childCount];
        mNewTranslationYs = new float[childCount];
        mNewScaleXs = new float[childCount];
        mNewScaleYs = new float[childCount];
        mNewBackgroundAlphas = new float[childCount];
        mNewAlphas = new float[childCount];
        mNewRotationYs = new float[childCount];
    }

    

    public Animator getChangeStateAnimation(final State state, boolean animated, int delay) {
        if (mState == state) {
            return null;
        }
        if (Util.DEBUG_ANIM) {
            Util.Log.w(TAG, "getChangeStateAnimation(start) = state:"+state
                    +", oldState:"+mState
                    +", delay:"+delay+"=="+this);
        }
        // Initialize animation arrays for the first time if necessary
        initAnimationArrays();

        AnimatorSet anim = animated ? LauncherAnimUtils.createAnimatorSet() : null;

        // Stop any scrolling, move to the current page right away
        setCurrentPage(getNextPage());

        final State oldState = mState;
        final boolean oldStateIsNormal = (oldState == State.NORMAL);
        final boolean oldStateIsSpringLoaded = (oldState == State.SPRING_LOADED);
        final boolean oldStateIsSmall = (oldState == State.SMALL);
        mState = state;
        final boolean stateIsNormal = (state == State.NORMAL);
        final boolean stateIsSpringLoaded = (state == State.SPRING_LOADED);
        final boolean stateIsSmall = (state == State.SMALL);
        float finalScaleFactor = 1.0f;
        float finalBackgroundAlpha = stateIsSpringLoaded ? 1.0f : 0f;
        float translationX = 0;
        float translationY = 0;
        boolean zoomIn = true;

        if (state != State.NORMAL) {
            finalScaleFactor = mSpringLoadedShrinkFactor - (stateIsSmall ? 0.1f : 0);
            setPageSpacing(mSpringLoadedPageSpacing);
            if (oldStateIsNormal && stateIsSmall) {
                zoomIn = false;
                setLayoutScale(finalScaleFactor);
                updateChildrenLayersEnabled(false);
            } else {
                finalBackgroundAlpha = 1.0f;
                setLayoutScale(finalScaleFactor);
            }
        } else {
            setPageSpacing(mOriginalPageSpacing);
            setLayoutScale(1.0f);
        }
        
        final int duration = zoomIn ?
                getResources().getInteger(R.integer.config_workspaceUnshrinkTime) :
                getResources().getInteger(R.integer.config_appsCustomizeWorkspaceShrinkTime);
        for (int i = 0; i < getChildCount(); i++) {
            final CellLayout cl = (CellLayout) getChildAt(i);
            if(false){
	            android.util.Log.e("QsLog", "getChangeStateAnimation()"
	                    +", i:"+i
	                    +", PivotX:"+cl.getPivotX()
	                    +", PivotY:"+cl.getPivotY()
	                    +", TransX:"+cl.getTranslationX()
	                    +", TransY:"+cl.getTranslationY()
	                    +", Rot:"+cl.getRotation()
	                    +", RotX:"+cl.getRotationX()
	                    +", RotY:"+cl.getRotationY()
	                    +", ScaleX:"+cl.getScaleX()
	                    +", ScaleY:"+cl.getScaleY()
	                    +", mState:"+mState);
			}
            
            float finalAlpha = (!mWorkspaceFadeInAdjacentScreens || stateIsSpringLoaded ||
                    (i == mCurrentPage)) ? 1f : 0f;
            float currentAlpha = cl.getShortcutsAndWidgets().getAlpha();
            float initialAlpha = currentAlpha;

            // Determine the pages alpha during the state transition
            if ((oldStateIsSmall && stateIsNormal) ||
                (oldStateIsNormal && stateIsSmall)) {
                // To/from workspace - only show the current page unless the transition is not
                //                     animated and the animation end callback below doesn't run;
                //                     or, if we're in spring-loaded mode
                if (i == mCurrentPage || !animated || oldStateIsSpringLoaded) {
                    finalAlpha = 1f;
                } else {
                    initialAlpha = 0f;
                    finalAlpha = 0f;
                }
            }

            mOldAlphas[i] = initialAlpha;
            mNewAlphas[i] = finalAlpha;
            if (animated) {
                mOldTranslationXs[i] = cl.getTranslationX();
                mOldTranslationYs[i] = cl.getTranslationY();
                mOldScaleXs[i] = cl.getScaleX();
                mOldScaleYs[i] = cl.getScaleY();
                mOldBackgroundAlphas[i] = cl.getBackgroundAlpha();

                mNewTranslationXs[i] = translationX;
                mNewTranslationYs[i] = translationY;
                mNewScaleXs[i] = finalScaleFactor;
                mNewScaleYs[i] = finalScaleFactor;
                mNewBackgroundAlphas[i] = finalBackgroundAlpha;
            } else {
                cl.setPivotX(cl.getWidth()*0.5f);
                cl.setPivotY(cl.getHeight()*0.5f);
                cl.setTranslationX(translationX);
                cl.setTranslationY(translationY);
                cl.setScaleX(finalScaleFactor);
                cl.setScaleY(finalScaleFactor);
                cl.setBackgroundAlpha(finalBackgroundAlpha);
                cl.setShortcutAndWidgetAlpha(finalAlpha);
            }
        }

        if (animated) {
            for (int index = 0; index < getChildCount(); index++) {
                final int i = index;
                final CellLayout cl = (CellLayout) getChildAt(i);
                float currentAlpha = cl.getShortcutsAndWidgets().getAlpha();
                
                cl.setPivotX(cl.getWidth()*0.5f);
                cl.setPivotY(cl.getHeight()*0.5f);
                
                if (mOldAlphas[i] == 0 && mNewAlphas[i] == 0) {
                    cl.setTranslationX(mNewTranslationXs[i]);
                    cl.setTranslationY(mNewTranslationYs[i]);
                    cl.setScaleX(mNewScaleXs[i]);
                    cl.setScaleY(mNewScaleYs[i]);
                    cl.setBackgroundAlpha(mNewBackgroundAlphas[i]);
                    cl.setShortcutAndWidgetAlpha(mNewAlphas[i]);
                    cl.setRotationY(mNewRotationYs[i]);
                } else {
                    LauncherViewPropertyAnimator a = new LauncherViewPropertyAnimator(cl);
                    a.translationX(mNewTranslationXs[i])
                        .translationY(mNewTranslationYs[i])
                        .scaleX(mNewScaleXs[i])
                        .scaleY(mNewScaleYs[i])
                        .setDuration(duration)
                        .setInterpolator(mZoomInInterpolator);
                    anim.play(a);

                    if (mOldAlphas[i] != mNewAlphas[i] || currentAlpha != mNewAlphas[i]) {
                        LauncherViewPropertyAnimator alphaAnim =
                            new LauncherViewPropertyAnimator(cl.getShortcutsAndWidgets());
                        alphaAnim.alpha(mNewAlphas[i])
                            .setDuration(duration)
                            .setInterpolator(mZoomInInterpolator);
                        anim.play(alphaAnim);
                    }
                    if (mOldBackgroundAlphas[i] != 0 ||
                        mNewBackgroundAlphas[i] != 0) {
                        ValueAnimator bgAnim = LauncherAnimUtils.ofFloat(0f, 1f).setDuration(duration);
                        bgAnim.setInterpolator(mZoomInInterpolator);
                        bgAnim.addUpdateListener(new LauncherAnimatorUpdateListener() {
                                public void onAnimationUpdate(float a, float b) {
                                    cl.setBackgroundAlpha(
                                            a * mOldBackgroundAlphas[i] +
                                            b * mNewBackgroundAlphas[i]);
                                }
                            });
                        anim.play(bgAnim);
                    }
                }
            }
            
            if (oldStateIsSmall && stateIsNormal){
                
            } else  {
                buildPageHardwareLayers();
            }
            anim.setStartDelay(delay);
        }

        if (stateIsSpringLoaded) {
            // Right now we're covered by Apps Customize
            // Show the background gradient immediately, so the gradient will
            // be showing once AppsCustomize disappears
            animateBackgroundGradient(getResources().getInteger(
                    R.integer.config_appsCustomizeSpringLoadedBgAlpha) / 100f, false);
        } else {
            // Fade the background gradient away
            animateBackgroundGradient(0f, true);
        }
        return anim;
    }

    

    public void setFinalScrollForPageChange(int screen) {
        if (screen >= 0) {
            mSavedScrollX = getScrollX();
            CellLayout cl = (CellLayout) getChildAt(screen);
            mSavedTranslationX = cl.getTranslationX();
            mSavedRotationY = cl.getRotationY();
            final int newX = getChildOffset(screen) - getRelativeChildOffset(screen);
            setScrollX(newX);
            cl.setTranslationX(0f);
            cl.setRotationY(0f);
        }
    }

    public void resetFinalScrollForPageChange(int screen) {
        if (screen >= 0) {
            CellLayout cl = (CellLayout) getChildAt(screen);
            setScrollX(mSavedScrollX);
            cl.setTranslationX(mSavedTranslationX);
            cl.setRotationY(mSavedRotationY);
        }
    }

    

    

    

    public void setFinalTransitionTransform(CellLayout layout) {
        if (isSwitchingState()) {
            int index = indexOfChild(layout);
            mCurrentScaleX = layout.getScaleX();
            mCurrentScaleY = layout.getScaleY();
            mCurrentTranslationX = layout.getTranslationX();
            mCurrentTranslationY = layout.getTranslationY();
            mCurrentRotationY = layout.getRotationY();
            
            layout.setPivotX(layout.getWidth()*0.5f);
            layout.setPivotY(layout.getHeight()*0.5f);
            
            layout.setScaleX(mNewScaleXs[index]);
            layout.setScaleY(mNewScaleYs[index]);
            layout.setTranslationX(mNewTranslationXs[index]);
            layout.setTranslationY(mNewTranslationYs[index]);
            layout.setRotationY(mNewRotationYs[index]);
        }
    }
    
    public void resetTransitionTransform(CellLayout layout) {
        if (isSwitchingState()) {
            mCurrentScaleX = layout.getScaleX();
            mCurrentScaleY = layout.getScaleY();
            mCurrentTranslationX = layout.getTranslationX();
            mCurrentTranslationY = layout.getTranslationY();
            mCurrentRotationY = layout.getRotationY();
            layout.setScaleX(mCurrentScaleX);
            layout.setScaleY(mCurrentScaleY);
            layout.setTranslationX(mCurrentTranslationX);
            layout.setTranslationY(mCurrentTranslationY);
            layout.setRotationY(mCurrentRotationY);
			
			layout.setPivotX(0f);
            layout.setPivotY(0f);
        }
    }

    

    

    public void setFadeForOverScroll(float fade) {
        if (!isScrollingIndicatorEnabled()) return;

        mOverscrollFade = fade;
//        float reducedFade = 0.5f + 0.5f * (1 - fade);
//        final ViewGroup parent = (ViewGroup) getParent();
//        final ImageView qsbDivider = (ImageView) (parent.findViewById(R.id.qsb_divider));
//        final ImageView dockDivider = (ImageView) (parent.findViewById(R.id.dock_divider));
//        final View scrollIndicator = getScrollingIndicator();
//
//        cancelScrollingIndicatorAnimations();
//        if (qsbDivider != null) qsbDivider.setAlpha(reducedFade);
//        if (dockDivider != null) dockDivider.setAlpha(reducedFade);
//        //scrollIndicator.setAlpha(1 - fade);
//        if(scrollIndicator != null)
//        	scrollIndicator.setAlpha(1.0f);
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
                    ((MtpShortcutView) view).updateShortcutUnreadNum(MtpUnreadLoader
                            .getUnreadNumberOfComponent(componentName));
                } else if (tag instanceof FolderInfo) {
                    ((FolderIcon) view).updateFolderUnreadNum();
                }
            }
        }
    }
//    public void beginDragShared(View child, DragSource source) {
//    	super.beginDragShared(child, source);
//    	// Show the scrolling indicator when you pick up an item
//        //showScrollingIndicator(false);
//    }
//    
//    protected void onPageEndMoving() {
//        super.onPageEndMoving();
//
//        if (isHardwareAccelerated()) {
//            updateChildrenLayersEnabled(false);
//        } else {
//            clearChildrenCache();
//        }
//
//        if (mDragController.isDragging()) {
//            if (isSmall()) {
//                // If we are in springloaded mode, then force an event to check if the current touch
//                // is under a new page (to scroll to)
//                mDragController.forceMoveEvent();
//            }
//        } else {
//            // If we are not mid-dragging, hide the page outlines if we are on a large screen
//            if (Launcher.isScreenLarge()) {
//                hideOutlines();
//            }
//
//            // Hide the scroll indicator as you pan the page
//            if (!mDragController.isDragging()) {
//                hideScrollingIndicator(false);
//            }
//        }
//        mOverScrollMaxBackgroundAlpha = 0.0f;
//
//        if (mDelayedResizeRunnable != null) {
//            mDelayedResizeRunnable.run();
//            mDelayedResizeRunnable = null;
//        }
//
//        if (mDelayedSnapToPageRunnable != null) {
//            mDelayedSnapToPageRunnable.run();
//            mDelayedSnapToPageRunnable = null;
//        }
//    }
//    
//    public void onDropCompleted(View target, DragObject d, boolean isFlingToDelete,
//            boolean success) {
//        super.onDropCompleted(target, d, isFlingToDelete, success);
//
//        // Hide the scrolling indicator after you pick up an item
//        //hideScrollingIndicator(false);
//    }

    //add by syq for qishang style bug
//    QsDropTargetBar mQsDropTargetBar;
//    boolean isDroped = false;
//    
//    @Override
//	public void startDrag(CellInfo cellInfo) {
//		super.startDrag(cellInfo);
//		isDroped = false;
//	}
//
//	@Override
//	public void onDrop(DragObject d) {
//		super.onDrop(d);
//		isDroped = true;
//	}
//
//	@Override
//	public void onDropCompleted(View target, DragObject d,
//			boolean isFlingToDelete, boolean success) {
//		if(mQsDropTargetBar == null){
//			mQsDropTargetBar = (QsDropTargetBar)mLauncher.findViewById(R.id.qsb_bar);
//			mQsDropTargetBar.QsSetLauncher(mLauncher);
//		}
//		//android.util.Log.i("QsLog", "isDroped:" + isDroped + " success:" + success);
//		if(!isDroped && !success){
//			mQsDropTargetBar.onDragEnd();//hide info bar
//			mQsDropTargetBar.QsRestoreCellInfoVisible(d);
//			//remove dragview and set celllayout view visible
//		}
//		super.onDropCompleted(target, d, isFlingToDelete, success);
//	}
    
    
    //for qishang style by syq delay continuously snap page
//    private static final int QS_DELAY_TIME_MS = 300;
//    private Handler mQsHandler = new Handler(){
//
//		@Override
//		public void handleMessage(Message msg) {
//			switch(msg.what){
//				case 0:
//					if(!mIsDragOccuring)
//						break;
//					View dragView = mDragController.getDragView();
//					if(mDragInfo != null && dragView != null){
//						int move_x = (int)dragView.getX() + dragView.getWidth()/2;
//						int move_y = (int)dragView.getY();
//						/*android.util.Log.e("QsLog","mCurrentPage.w: "+ 
//								getPageAt(mCurrentPage).getWidth() + " move_x:" 
//								+ move_x + " dragView.w: " + dragView.getWidth());*/
//						if(!hitsPage(mCurrentPage, move_x, move_y)){
//							WorkspaceDefault.super.snapToPage(msg.arg1, PAGE_SNAP_ANIMATION_DURATION);
//						}else{
//							//android.util.Log.e("QsLog","focus changed !stop snap");
//						}
//					}
//					break;
//				default:
//					break;
//			}
//			super.handleMessage(msg);
//		}
//    };
//    
//    @Override
//    public void snapToPage(int whichPage, int duration) {
//    	if(!mIsDragOccuring || mDragInfo == null){
//    		super.snapToPage(whichPage, duration);
//    		return;
//    	}
//		Message msg = mQsHandler.obtainMessage();
//		msg.what = 0;
//		msg.arg1 = whichPage;
//		mQsHandler.sendMessageDelayed(msg, QS_DELAY_TIME_MS);
//    }

    protected int getRelativeChildOffset(int index) {
    	final int padding = getPaddingLeft() + getPaddingRight();
        final int offset = getPaddingLeft() +
                (getMeasuredWidth() - padding - getChildWidth(index)) / 2;
        if(offset < 0){
	    	setPadding(0, getPaddingTop(), 0 , getPaddingBottom());
	    	return 0;
    	}else{
    		return super.getRelativeChildOffset(index);
    	}
    }
} 
