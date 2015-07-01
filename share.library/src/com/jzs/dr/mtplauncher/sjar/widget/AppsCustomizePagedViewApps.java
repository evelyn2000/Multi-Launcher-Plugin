package com.jzs.dr.mtplauncher.sjar.widget;

import com.jzs.dr.mtplauncher.sjar.utils.Util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public class AppsCustomizePagedViewApps extends AppsCustomizePagedView {
	private final static String TAG = "AppsCustomizePagedViewApps";
	
	public Workspace.ZInterpolator mZInterpolator = new Workspace.ZInterpolator(0.5f);
	private static float CAMERA_DISTANCE = 6500;
    private static float TRANSITION_SCALE_FACTOR = 0.74f;
    private static float TRANSITION_PIVOT = 0.65f;
    private static float TRANSITION_MAX_ROTATION = 22;
    private static final boolean PERFORM_OVERSCROLL_ROTATION = true;
    private AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);
    private DecelerateInterpolator mLeftScreenAlphaInterpolator = new DecelerateInterpolator(4);
	
	public AppsCustomizePagedViewApps(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
	}
	
	public AppsCustomizePagedViewApps(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

	}
	
	public boolean isSupportWidget(){
    	return false;
    }
	
	public boolean isSupportApps(){
    	return true;
    }
	
	// In apps customize, we have a scrolling effect which emulates pulling cards off of a stack.
    @Override
    protected void screenScrolled(int screenCenter) {
        super.screenScrolled(screenCenter);

        if (Util.DEBUG_LAYOUT) {
            Util.Log.d(TAG, "screenScrolled: screenCenter = " + screenCenter
                    + ", mOverScrollX = " + mOverScrollX + ", mMaxScrollX = " + mMaxScrollX
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
                    if (i == 0 && scrollProgress < 0) {
                        // Overscroll to the left
                        v.setPivotX(TRANSITION_PIVOT * pageWidth);
                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
                        scale = 1.0f;
                        alpha = 1.0f;
                        // On the first page, we don't want the page to have any lateral motion
                        translationX = 0;
                    } else if (i == getChildCount() - 1 && scrollProgress > 0) {
                        // Overscroll to the right
                        v.setPivotX((1 - TRANSITION_PIVOT) * pageWidth);
                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
                        scale = 1.0f;
                        alpha = 1.0f;
                        // On the last page, we don't want the page to have any lateral motion.
                        translationX = 0;
                    } else {
                        v.setPivotY(pageHeight / 2.0f);
                        v.setPivotX(pageWidth / 2.0f);
                        v.setRotationY(0f);
                    }
                }

                v.setTranslationX(translationX);
                v.setScaleX(scale);
                v.setScaleY(scale);
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
    
    @Override
    public void onShortPress(View v) {
    	if (Util.ENABLE_DEBUG) {
    		Util.Log.d(TAG, "onShortcutPress v = " + v + ", v.getTag() = " + v.getTag());
    	}
    }

    public void reset() {
        super.reset();

//        AppsCustomizeTabHost tabHost = getTabHost();
//        String tag = tabHost.getCurrentTabTag();
//        if (tag != null) {
//            if (!tag.equals(tabHost.getTabTagForContentType(ContentType.Applications))) {
//                tabHost.setCurrentTabFromContent(ContentType.Applications);
//            }
//        }

        if (mCurrentPage != 0) {
            invalidatePageData(0);
        }
    }
}
