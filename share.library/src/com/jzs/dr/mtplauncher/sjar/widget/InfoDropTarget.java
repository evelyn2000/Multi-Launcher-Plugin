package com.jzs.dr.mtplauncher.sjar.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.LauncherApplication;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddItemInfo;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.R;

public class InfoDropTarget extends ButtonDropTarget {

    private ColorStateList mOriginalTextColor;
    private TransitionDrawable mDrawable;

    public InfoDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InfoDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        TypedArray a = context.obtainStyledAttributes(attrs,
        		R.styleable.ButtonDropTarget, defStyle, 0);
        
        mHoverColor = a.getColor(R.styleable.ButtonDropTarget_hoverColor, 0xDA0099CC);
        //R.color.info_target_hover_tint
        a.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mOriginalTextColor = getTextColors();

//        // Get the hover color
//        Resources r = getResources();
//        /// M: modified for theme feature, get the different hover color for different themes.
//        mHoverColor = Launcher.getThemeColor(r, R.color.info_target_hover_tint);
        mDrawable = (TransitionDrawable) getCurrentDrawable();
        mDrawable.setCrossFadeEnabled(true);

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
    
    protected boolean isFromWorkSpace(DragSource source) {
        return source instanceof Workspace;
    }

    @Override
    public boolean acceptDrop(DragObject d) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "acceptDrop: d = " + d + ", d.dragInfo = " + d.dragInfo);
        }

        // acceptDrop is called just before onDrop. We do the work here, rather than
        // in onDrop, because it allows us to reject the drop (by returning false)
        // so that the object being dragged isn't removed from the drag source.
        ComponentName componentName = null;
        if (d.dragInfo instanceof ApplicationInfo) {
            componentName = ((ApplicationInfo) d.dragInfo).componentName;
        } else if (d.dragInfo instanceof ShortcutInfo) {
            componentName = ((ShortcutInfo) d.dragInfo).intent.getComponent();
        } else if (d.dragInfo instanceof PendingAddItemInfo) {
            componentName = ((PendingAddItemInfo) d.dragInfo).componentName;
        }
        if (componentName != null) {
            mLauncher.startApplicationDetailsActivity(componentName);
        }

        if(isFromWorkSpace(d.dragSource))       
            d.cancelled = true;
        // There is no post-drop animation, so clean up the DragView now
        d.deferDragViewCleanupPostAnimation = false;
        return false;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onDratStart: source = " + source + ", info = " + info
                    + ", dragAction = " + dragAction);
        }

        boolean isVisible = true;

        // Hide this button unless we are dragging something from AllApps
        if (!isFromAllApps(source)) {
            isVisible = false;
        }

        onDragStart(source, info, dragAction, isVisible);
    }
    
    protected void onDragStart(DragSource source, Object info, int dragAction, boolean isVisible) {
        mActive = isVisible;
        mDrawable.resetTransition();
        setTextColor(mOriginalTextColor);
        ((ViewGroup) getParent()).setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDragEnd() {
        super.onDragEnd();
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onDragEnd.");
        }
        mActive = false;
    }

    public void onDragEnter(DragObject d) {
        super.onDragEnter(d);
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onDragEnter: d = " + d);
        }

        mDrawable.startTransition(mTransitionDuration);
        setTextColor(mHoverColor);
    }

    public void onDragExit(DragObject d) {
        super.onDragExit(d);
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onDragExit: d = " + d + ", d.dragComplete = " + d.dragComplete);
        }

        if (!d.dragComplete) {
            mDrawable.resetTransition();
            setTextColor(mOriginalTextColor);
        }
    }

}
