package com.jzs.dr.mtplauncher.sjar.widget;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragController;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherAnimUtils;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.R;

public class SearchDropTargetBar extends FrameLayout implements DragController.DragListener {
	protected static final String TAG = "SearchDropTargetBar";
    protected static final int sTransitionInDuration = 200;
    protected static final int sTransitionOutDuration = 175;

    protected ObjectAnimator mDropTargetBarAnim;
    protected ObjectAnimator mQSBSearchBarAnim;
    protected static final AccelerateInterpolator sAccelerateInterpolator =
            new AccelerateInterpolator();

    protected boolean mIsSearchBarHidden;
    protected View mQSBSearchBar;
    protected View mDropTargetBar;
    protected ButtonDropTarget mInfoDropTarget;
    protected ButtonDropTarget mDeleteDropTarget;
    protected int mBarHeight;
    protected boolean mDeferOnDragEnd = false;

    protected Drawable mPreviousBackground;
    protected boolean mEnableDropDownDropTargets;

    public SearchDropTargetBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchDropTargetBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        TypedArray a = context.obtainStyledAttributes(attrs,
        		R.styleable.SearchDropTargetBar, defStyle, 0);
        mBarHeight = a.getDimensionPixelSize(R.styleable.SearchDropTargetBar_dropTargetBarSize, 40);
        			//R.dimen.qsb_bar_height
        mEnableDropDownDropTargets = a.getBoolean(R.styleable.SearchDropTargetBar_useDropTargetDownTransition, true);
                //getResources().getBoolean(R.bool.config_useDropTargetDownTransition);
        a.recycle();
    }

    public void setup(Launcher launcher, DragController dragController) {
        dragController.addDragListener(this);
        dragController.addDragListener(mInfoDropTarget);
        dragController.addDragListener(mDeleteDropTarget);
        dragController.addDropTarget(mInfoDropTarget);
        dragController.addDropTarget(mDeleteDropTarget);
        dragController.setFlingToDeleteDropTarget(mDeleteDropTarget);
        mInfoDropTarget.setLauncher(launcher);
        mDeleteDropTarget.setLauncher(launcher);
    }

    protected void prepareStartAnimation(View v) {
        // Enable the hw layers before the animation starts (will be disabled in the onAnimationEnd
        // callback below)
        v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        v.buildLayer();
    }

    protected void setupAnimation(ObjectAnimator anim, final View v) {
        anim.setInterpolator(sAccelerateInterpolator);
        anim.setDuration(sTransitionInDuration);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                v.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Get the individual components
        mQSBSearchBar = findViewWithTag("qsb_search_bar");
        mDropTargetBar = findViewWithTag("drag_target_bar");
        mInfoDropTarget = (ButtonDropTarget) mDropTargetBar.findViewWithTag("info_target_text");
        mDeleteDropTarget = (ButtonDropTarget) mDropTargetBar.findViewWithTag("delete_target_text");
//        mBarHeight = getResources().getDimensionPixelSize(R.dimen.qsb_bar_height);

        mInfoDropTarget.setSearchDropTargetBar(this);
        mDeleteDropTarget.setSearchDropTargetBar(this);

//        mEnableDropDownDropTargets =
//            getResources().getBoolean(R.bool.config_useDropTargetDownTransition);

        // Create the various fade animations
        if (mEnableDropDownDropTargets) {
            mDropTargetBar.setTranslationY(-mBarHeight);
            mDropTargetBarAnim = LauncherAnimUtils.ofFloat(mDropTargetBar, "translationY",
                    -mBarHeight, 0f);
            if(mQSBSearchBar != null)
	            mQSBSearchBarAnim = LauncherAnimUtils.ofFloat(mQSBSearchBar, "translationY", 0,
	                    -mBarHeight);
        } else {
            mDropTargetBar.setAlpha(0f);
            mDropTargetBarAnim = LauncherAnimUtils.ofFloat(mDropTargetBar, "alpha", 0f, 1f);
            if(mQSBSearchBar != null)
            	mQSBSearchBarAnim = LauncherAnimUtils.ofFloat(mQSBSearchBar, "alpha", 1f, 0f);
        }
        setupAnimation(mDropTargetBarAnim, mDropTargetBar);
        if(mQSBSearchBar != null)
        	setupAnimation(mQSBSearchBarAnim, mQSBSearchBar);
    }

    public void finishAnimations() {
        prepareStartAnimation(mDropTargetBar);
        mDropTargetBarAnim.reverse();
        prepareStartAnimation(mQSBSearchBar);
        if(mQSBSearchBar != null)
        	mQSBSearchBarAnim.reverse();
    }

    /*
     * Shows and hides the search bar.
     */
    public void showSearchBar(boolean animated) {
        if (mQSBSearchBar == null || !mIsSearchBarHidden) return;
        if (animated) {
            prepareStartAnimation(mQSBSearchBar);
            mQSBSearchBarAnim.reverse();
        } else {
            mQSBSearchBarAnim.cancel();
            if (mEnableDropDownDropTargets) {
                mQSBSearchBar.setTranslationY(0);
            } else {
                mQSBSearchBar.setAlpha(1f);
            }
        }
        mIsSearchBarHidden = false;
    }
    public void hideSearchBar(boolean animated) {
        if (mQSBSearchBar == null || mIsSearchBarHidden) return;
        if (animated) {
            prepareStartAnimation(mQSBSearchBar);
            mQSBSearchBarAnim.start();
        } else {
            mQSBSearchBarAnim.cancel();
            if (mEnableDropDownDropTargets) {
                mQSBSearchBar.setTranslationY(-mBarHeight);
            } else {
                mQSBSearchBar.setAlpha(0f);
            }
        }
        mIsSearchBarHidden = true;
    }

    /*
     * Gets various transition durations.
     */
    public int getTransitionInDuration() {
        return sTransitionInDuration;
    }
    public int getTransitionOutDuration() {
        return sTransitionOutDuration;
    }

    /*
     * DragController.DragListener implementation
     */
    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        if (Util.DEBUG_DRAG) {
        	Util.Log.d(TAG, "onDragStart: source = " + source + ", info = " + info
                    + ", dragAction = " + dragAction + ", this = " + this);
        }

        // Animate out the QSB search bar, and animate in the drop target bar
        prepareStartAnimation(mDropTargetBar);
        mDropTargetBarAnim.start();
        if (mQSBSearchBar != null && !mIsSearchBarHidden) {
            prepareStartAnimation(mQSBSearchBar);
            mQSBSearchBarAnim.start();
        }
    }

    public void deferOnDragEnd() {
        mDeferOnDragEnd = true;
    }

    @Override
    public void onDragEnd() {
        if (Util.DEBUG_DRAG) {
        	Util.Log.d(TAG, "onDragEnd: mDeferOnDragEnd = " + mDeferOnDragEnd);
        }

        if (!mDeferOnDragEnd) {
            // Restore the QSB search bar, and animate out the drop target bar
            prepareStartAnimation(mDropTargetBar);
            mDropTargetBarAnim.reverse();
            if (mQSBSearchBar != null && !mIsSearchBarHidden) {
                prepareStartAnimation(mQSBSearchBar);
                mQSBSearchBarAnim.reverse();
            }
        } else {
            mDeferOnDragEnd = false;
        }
    }

    public void onSearchPackagesChanged(boolean searchVisible, boolean voiceVisible) {
    	if (Util.DEBUG_DRAG) {
        	Util.Log.d(TAG, "onSearchPackagesChanged: searchVisible = " + searchVisible
                    + ", voiceVisible = " + voiceVisible + ", mQSBSearchBar = " + mQSBSearchBar);
        }

        if (mQSBSearchBar != null) {
            Drawable bg = mQSBSearchBar.getBackground();
            if (bg != null && (!searchVisible && !voiceVisible)) {
                // Save the background and disable it
                mPreviousBackground = bg;
                mQSBSearchBar.setBackgroundResource(0);
            } else if (mPreviousBackground != null && (searchVisible || voiceVisible)) {
                // Restore the background
                mQSBSearchBar.setBackgroundDrawable(mPreviousBackground);
            }
        }
    }

    public Rect getSearchBarBounds() {
        if (mQSBSearchBar != null) {
            final int[] pos = new int[2];
            mQSBSearchBar.getLocationOnScreen(pos);

            final Rect rect = new Rect();
            rect.left = pos[0];
            rect.top = pos[1];
            rect.right = pos[0] + mQSBSearchBar.getWidth();
            rect.bottom = pos[1] + mQSBSearchBar.getHeight();
            return rect;
        } else {
            return null;
        }
    }

}
