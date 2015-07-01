package com.jzs.dr.mtplauncher.sjar.widget;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.jzs.common.launcher.IResConfigManager;
import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.LauncherApplication;

import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.FolderInfo;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherAppWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherModel;
import com.jzs.dr.mtplauncher.sjar.model.LauncherSettings;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddItemInfo;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

import com.jzs.dr.mtplauncher.sjar.R;

public class DeleteDropTarget extends ButtonDropTarget {
    protected static final String TAG = "DeleteDropTarget";
    public static int DELETE_ANIMATION_DURATION = 285;
    public static int FLING_DELETE_ANIMATION_DURATION = 350;
    public static float FLING_TO_DELETE_FRICTION = 0.035f;
    public final static int MODE_FLING_DELETE_TO_TRASH = 0;
    public final static int MODE_FLING_DELETE_ALONG_VECTOR = 1;

    protected final int mFlingDeleteMode = MODE_FLING_DELETE_ALONG_VECTOR;

    protected ColorStateList mOriginalTextColor;
    protected TransitionDrawable mUninstallDrawable;
    protected TransitionDrawable mRemoveDrawable;
    protected TransitionDrawable mCurrentDrawable;

    public DeleteDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeleteDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        TypedArray a = context.obtainStyledAttributes(attrs,
        		R.styleable.DeleteDropTarget, defStyle, 0);
        
        mHoverColor = a.getColor(R.styleable.ButtonDropTarget_hoverColor, 0xDAFF0000);
        //R.color.delete_target_hover_tint
        mUninstallDrawable = (TransitionDrawable) a.getDrawable(R.styleable.DeleteDropTarget_uninstallDrawable);
        //R.drawable.uninstall_target_selector
        mRemoveDrawable = (TransitionDrawable) a.getDrawable(R.styleable.DeleteDropTarget_removeDrawable);
        //R.drawable.remove_target_selector
        a.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Get the drawable
        mOriginalTextColor = getTextColors();

        // Get the hover color
        //Resources r = getResources();
        //mHoverColor = r.getColor(R.color.delete_target_hover_tint);
//        mUninstallDrawable = (TransitionDrawable) 
//                r.getDrawable(R.drawable.uninstall_target_selector);
//        mRemoveDrawable = (TransitionDrawable) r.getDrawable(R.drawable.remove_target_selector);

        if(mRemoveDrawable != null)
        	mRemoveDrawable.setCrossFadeEnabled(true);
        if(mUninstallDrawable != null)
        	mUninstallDrawable.setCrossFadeEnabled(true);

        // The current drawable is set to either the remove drawable or the uninstall drawable 
        // and is initially set to the remove drawable, as set in the layout xml.
        mCurrentDrawable = (TransitionDrawable) getCurrentDrawable();

        // Remove the text in the Phone UI in landscape
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!Launcher.isScreenLarge()) {
                setText("");
            }
        }
    }

    protected boolean isFromAllApps(DragSource source) {
        return (source instanceof AppsCustomizePagedView);
    }
    
    protected boolean isAllAppsApplication(DragSource source, Object info) {
        return isFromAllApps(source) && (info instanceof ApplicationInfo);
    	//return (mLauncher.isDragFromAllApps(source)) && (info instanceof ApplicationInfo);
    }
    
    protected boolean isAllAppsWidget(DragSource source, Object info) {
        if (isFromAllApps(source)) {
    	//if (mLauncher.isDragFromAllApps(source)) {
            if (info instanceof PendingAddItemInfo) {
                PendingAddItemInfo addInfo = (PendingAddItemInfo) info;
                switch (addInfo.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                        return true;
                }
            }
        }
        return false;
    }
    protected boolean isDragSourceWorkspaceOrFolder(DragObject d) {
        return (d.dragSource instanceof Workspace) || (d.dragSource instanceof Folder);
    }
    protected boolean isWorkspaceOrFolderApplication(DragObject d) {
        return isDragSourceWorkspaceOrFolder(d) && (d.dragInfo instanceof ShortcutInfo);
    }
    protected boolean isWorkspaceOrFolderWidget(DragObject d) {
        return isDragSourceWorkspaceOrFolder(d) && (d.dragInfo instanceof LauncherAppWidgetInfo);
    }
    protected boolean isWorkspaceFolder(DragObject d) {
        return (d.dragSource instanceof Workspace) && (d.dragInfo instanceof FolderInfo);
    }

    protected void setHoverColor() {
        mCurrentDrawable.startTransition(mTransitionDuration);
        setTextColor(mHoverColor);
    }
    protected void resetHoverColor() {
        mCurrentDrawable.resetTransition();
        setTextColor(mOriginalTextColor);
    }

    @Override
    public boolean acceptDrop(DragObject d) {
        // We can remove everything including App shortcuts, folders, widgets, etc.
        return true;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        boolean isVisible = true;
        boolean isUninstall = false;

        // If we are dragging a widget from AppsCustomize, hide the delete target
        if (isAllAppsWidget(source, info)) {
            isVisible = false;
        }

        // If we are dragging an application from AppsCustomize, only show the control if we can
        // delete the app (it was downloaded), and rename the string to "uninstall" in such a case
        if (isAllAppsApplication(source, info)) {
            ApplicationInfo appInfo = (ApplicationInfo) info;
            if(appInfo.isDownloadApp()){//if ((appInfo.flags & ApplicationInfo.DOWNLOADED_FLAG) != 0) {
                isUninstall = true;
            } else {
                isVisible = false;
            }
        }

        onDragStart(source, info, dragAction, isVisible, isUninstall);
    }
    
    protected void onDragStart(DragSource source, Object info, int dragAction, boolean isVisible, boolean isUninstall) {
        
        if (isUninstall) {
            if(mUninstallDrawable != null)
                setCompoundDrawablesWithIntrinsicBounds(mUninstallDrawable, null, null, null);
        } else {
            if(mRemoveDrawable != null)
                setCompoundDrawablesWithIntrinsicBounds(mRemoveDrawable, null, null, null);
        }
        mCurrentDrawable = (TransitionDrawable) getCurrentDrawable();

        mActive = isVisible;
        resetHoverColor();
        
        ((ViewGroup) getParent()).setVisibility(isVisible ? View.VISIBLE : View.GONE);
        if (getText().length() > 0) {
            final IResConfigManager res = mLauncher.getResConfigManager();
            
            setText(isUninstall ? res.getString(IResConfigManager.STR_DELETE_TARGET_UNINSTALL_LABEL)
                    : res.getString(IResConfigManager.STR_DELETE_TARGET_LABEL));
            
//            setText(isUninstall ? R.string.delete_target_uninstall_label
//                : R.string.delete_target_label);
        }
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "onDragStart: isUninstall = " + isUninstall + ", isVisible = "
                    + isVisible + ", info = " + info);
        }
    }

    @Override
    public void onDragEnd() {
        super.onDragEnd();
        mActive = false;
    }

    public void onDragEnter(DragObject d) {
        super.onDragEnter(d);

        setHoverColor();
    }

    public void onDragExit(DragObject d) {
        super.onDragExit(d);

        if (!d.dragComplete) {
            resetHoverColor();
        } else {
            // Restore the hover color if we are deleting
            d.dragView.setColor(mHoverColor);
        }
    }

    protected void animateToTrashAndCompleteDrop(final DragObject d) {
        DragLayer dragLayer = mLauncher.getDragLayer();
        Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);
        Rect to = getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight(),
                mCurrentDrawable.getIntrinsicWidth(), mCurrentDrawable.getIntrinsicHeight());
        float scale = (float) to.width() / from.width();

        if(mSearchDropTargetBar != null)
        	mSearchDropTargetBar.deferOnDragEnd();
        Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
            	if(mSearchDropTargetBar != null)
                	mSearchDropTargetBar.onDragEnd();
                mLauncher.exitSpringLoadedDragMode();
                completeDrop(d);
            }
        };
        dragLayer.animateView(d.dragView, from, to, scale, 1f, 1f, 0.1f, 0.1f,
                DELETE_ANIMATION_DURATION, new DecelerateInterpolator(2),
                new LinearInterpolator(), onAnimationEndRunnable,
                DragLayer.ANIMATION_END_DISAPPEAR, null);
    }

    protected void completeDrop(DragObject d) {
        ItemInfo item = (ItemInfo) d.dragInfo;
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "completeDrop: item = " + item + ", d = " + d);
        }

        if (isAllAppsApplication(d.dragSource, item)) {
            // Uninstall the application if it is being dragged from AppsCustomize
            mLauncher.startApplicationUninstallActivity((ApplicationInfo) item);
        } else if (isWorkspaceOrFolderApplication(d)) {  
        	mLauncher.getModel().deleteItemFromDatabase(item);
        } else if (isWorkspaceFolder(d)) {
            // Remove the folder from the workspace and delete the contents from launcher model
            FolderInfo folderInfo = (FolderInfo) item;
            mLauncher.removeFolder(folderInfo);
            mLauncher.getModel().deleteFolderContentsFromDatabase(folderInfo);
        } else if (isWorkspaceOrFolderWidget(d)) {
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

    public void onDrop(DragObject d) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onDrop: d = " + d);
        }
        animateToTrashAndCompleteDrop(d);
    }

    /**
     * Creates an animation from the current drag view to the delete trash icon.
     */
    protected AnimatorUpdateListener createFlingToTrashAnimatorListener(final DragLayer dragLayer,
            DragObject d, PointF vel, ViewConfiguration config) {
        final Rect to = getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight(),
                mCurrentDrawable.getIntrinsicWidth(), mCurrentDrawable.getIntrinsicHeight());
        final Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);

        // Calculate how far along the velocity vector we should put the intermediate point on
        // the bezier curve
        float velocity = Math.abs(vel.length());
        float vp = Math.min(1f, velocity / (config.getScaledMaximumFlingVelocity() / 2f));
        int offsetY = (int) (-from.top * vp);
        int offsetX = (int) (offsetY / (vel.y / vel.x));
        final float y2 = from.top + offsetY;                        // intermediate t/l
        final float x2 = from.left + offsetX;
        final float x1 = from.left;                                 // drag view t/l
        final float y1 = from.top;
        final float x3 = to.left;                                   // delete target t/l
        final float y3 = to.top;

        final TimeInterpolator scaleAlphaInterpolator = new TimeInterpolator() {
            @Override
            public float getInterpolation(float t) {
                return t * t * t * t * t * t * t * t;
            }
        };
        return new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final DragView dragView = (DragView) dragLayer.getAnimatedView();
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                float tp = scaleAlphaInterpolator.getInterpolation(t);
                float initialScale = dragView.getInitialScale();
                float finalAlpha = 0.5f;
                float scale = dragView.getScaleX();
                float x1o = ((1f - scale) * dragView.getMeasuredWidth()) / 2f;
                float y1o = ((1f - scale) * dragView.getMeasuredHeight()) / 2f;
                float x = (1f - t) * (1f - t) * (x1 - x1o) + 2 * (1f - t) * t * (x2 - x1o) +
                        (t * t) * x3;
                float y = (1f - t) * (1f - t) * (y1 - y1o) + 2 * (1f - t) * t * (y2 - x1o) +
                        (t * t) * y3;

                dragView.setTranslationX(x);
                dragView.setTranslationY(y);
                dragView.setScaleX(initialScale * (1f - tp));
                dragView.setScaleY(initialScale * (1f - tp));
                dragView.setAlpha(finalAlpha + (1f - finalAlpha) * (1f - tp));
            }
        };
    }

    /**
     * Creates an animation from the current drag view along its current velocity vector.
     * For this animation, the alpha runs for a fixed duration and we update the position
     * progressively.
     */
    protected static class FlingAlongVectorAnimatorUpdateListener implements AnimatorUpdateListener {
        private DragLayer mDragLayer;
        private PointF mVelocity;
        private Rect mFrom;
        private long mPrevTime;
        private boolean mHasOffsetForScale;
        private float mFriction;

        private final TimeInterpolator mAlphaInterpolator = new DecelerateInterpolator(0.75f);

        public FlingAlongVectorAnimatorUpdateListener(DragLayer dragLayer, PointF vel, Rect from,
                long startTime, float friction) {
            mDragLayer = dragLayer;
            mVelocity = vel;
            mFrom = from;
            mPrevTime = startTime;
            mFriction = 1f - (dragLayer.getResources().getDisplayMetrics().density * friction);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            final DragView dragView = (DragView) mDragLayer.getAnimatedView();
            float t = ((Float) animation.getAnimatedValue()).floatValue();
            long curTime = AnimationUtils.currentAnimationTimeMillis();

            if (!mHasOffsetForScale) {
                mHasOffsetForScale = true;
                float scale = dragView.getScaleX();
                float xOffset = ((scale - 1f) * dragView.getMeasuredWidth()) / 2f;
                float yOffset = ((scale - 1f) * dragView.getMeasuredHeight()) / 2f;

                mFrom.left += xOffset;
                mFrom.top += yOffset;
            }

            mFrom.left += (mVelocity.x * (curTime - mPrevTime) / 1000f);
            mFrom.top += (mVelocity.y * (curTime - mPrevTime) / 1000f);

            dragView.setTranslationX(mFrom.left);
            dragView.setTranslationY(mFrom.top);
            dragView.setAlpha(1f - mAlphaInterpolator.getInterpolation(t));

            mVelocity.x *= mFriction;
            mVelocity.y *= mFriction;
            mPrevTime = curTime;
        }
    };
    protected AnimatorUpdateListener createFlingAlongVectorAnimatorListener(final DragLayer dragLayer,
            DragObject d, PointF vel, final long startTime, final int duration,
            ViewConfiguration config) {
        final Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);

        return new FlingAlongVectorAnimatorUpdateListener(dragLayer, vel, from, startTime,
                FLING_TO_DELETE_FRICTION);
    }

    public void onFlingToDelete(final DragObject d, int x, int y, PointF vel) {
    	if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onFlingToDelete: d = " + d);
    	}

        //final boolean isAllApps = mLauncher.isDragFromAllApps(d.dragSource);//d.dragSource instanceof AppsCustomizePagedView;
    	final boolean isAllApps = isFromAllApps(d.dragSource);//d.dragSource instanceof AppsCustomizePagedView;

        // Don't highlight the icon as it's animating
        d.dragView.setColor(0);
        d.dragView.updateInitialScaleToCurrentScale();
        // Don't highlight the target if we are flinging from AllApps
        if (isAllApps) {
            resetHoverColor();
        }

        if (mFlingDeleteMode == MODE_FLING_DELETE_TO_TRASH && mSearchDropTargetBar != null) {
            // Defer animating out the drop target if we are animating to it
            mSearchDropTargetBar.deferOnDragEnd();
            mSearchDropTargetBar.finishAnimations();
        }

        final ViewConfiguration config = ViewConfiguration.get(mLauncher);
        final DragLayer dragLayer = mLauncher.getDragLayer();
        final int duration = FLING_DELETE_ANIMATION_DURATION;
        final long startTime = AnimationUtils.currentAnimationTimeMillis();

        // NOTE: Because it takes time for the first frame of animation to actually be
        // called and we expect the animation to be a continuation of the fling, we have
        // to account for the time that has elapsed since the fling finished.  And since
        // we don't have a startDelay, we will always get call to update when we call
        // start() (which we want to ignore).
        final TimeInterpolator tInterpolator = new TimeInterpolator() {
            private int mCount = -1;
            private float mOffset = 0f;

            @Override
            public float getInterpolation(float t) {
                if (mCount < 0) {
                    mCount++;
                } else if (mCount == 0) {
                    mOffset = Math.min(0.5f, (float) (AnimationUtils.currentAnimationTimeMillis() -
                            startTime) / duration);
                    mCount++;
                }
                return Math.min(1f, mOffset + t);
            }
        };
        AnimatorUpdateListener updateCb = null;
        if (mFlingDeleteMode == MODE_FLING_DELETE_TO_TRASH) {
            updateCb = createFlingToTrashAnimatorListener(dragLayer, d, vel, config);
        } else if (mFlingDeleteMode == MODE_FLING_DELETE_ALONG_VECTOR) {
            updateCb = createFlingAlongVectorAnimatorListener(dragLayer, d, vel, startTime,
                    duration, config);
        }
        Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
            	if(mSearchDropTargetBar != null)
            		mSearchDropTargetBar.onDragEnd();

                // If we are dragging from AllApps, then we allow AppsCustomizePagedView to clean up
                // itself, otherwise, complete the drop to initiate the deletion process
                if (!isAllApps) {
                    mLauncher.exitSpringLoadedDragMode();
                    completeDrop(d);
                }
                mLauncher.getDragController().onDeferredEndFling(d);
            }
        };
        dragLayer.animateView(d.dragView, updateCb, duration, tInterpolator, onAnimationEndRunnable,
                DragLayer.ANIMATION_END_DISAPPEAR, null);
    }

}
