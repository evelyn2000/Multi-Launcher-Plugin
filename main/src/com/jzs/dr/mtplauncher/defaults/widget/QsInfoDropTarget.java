package com.jzs.dr.mtplauncher.defaults.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.jzs.dr.mtplauncher.sjar.R;

import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget.DragObject;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddItemInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.widget.CellLayout;
import com.jzs.dr.mtplauncher.sjar.widget.InfoDropTarget;
import com.jzs.dr.mtplauncher.sjar.widget.Workspace;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.dr.mtplauncher.sjar.widget.Folder;

public class QsInfoDropTarget extends InfoDropTarget {
//	
//	DragSource mDragSource;
//	DragObject mDropObject;
	int mBgColor;

	public QsInfoDropTarget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs,
        		R.styleable.ButtonDropTarget, defStyle, 0);
        
        mHoverColor = a.getColor(R.styleable.ButtonDropTarget_hoverColor, 0xDA0099CC);
        a.recycle();
	}

	public QsInfoDropTarget(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	@Override
    public void onDragEnter(DragObject d) {
		mBgColor = mHoverColor;
        setBackgroundColor(mBgColor);
    }
	
	@Override
	public void onDragExit(DragObject d) {
		mBgColor = 0;
        setBackgroundColor(mBgColor);
    }
	
	protected boolean isFromWorkSpace(DragSource source) {
		return source instanceof Workspace;
	}
	
	protected boolean isFromFolder(DragSource source){
		return source instanceof Folder;
	}
	
    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onDratStart: source = " + source + ", info = " + info
                    + ", dragAction = " + dragAction);
        }
//        
//        mDragSource = source;
        
        boolean isVisible = true;

        // Hide this button unless we are dragging something from AllApps
        if ((!isFromAllApps(source) && !isFromWorkSpace(source) && 
        		!isFromFolder(source))){
            isVisible = false;
        }
        
        if (!isAcceptInfoList(info)){
        	isVisible = false;
        }
        
        super.onDragStart(source, info, dragAction, isVisible);
        
//        mActive = isVisible;
//        ((ViewGroup) getParent()).setVisibility(isVisible ? View.VISIBLE : View.GONE);
//        invalidate();
    }
    
    @Override
    public void onDragEnd() {
        super.onDragEnd();

    }
    
//    @Override
//	public boolean acceptDrop(DragObject d) {
//    	restoreCellInfoVisible(d);
//		return super.acceptDrop(d);
//	}
//
//	private void restoreCellInfoVisible(DragObject d){
//		if((d == null && d.dragInfo != null) || isFromAllApps(d.dragSource))
//			return;
//		
//		d.cancelled = true;
//	}
	
	public boolean isAcceptInfoList(Object info){
		boolean result = false;
		
		if(info instanceof ApplicationInfo ||
			info instanceof ShortcutInfo ||
			info instanceof PendingAddItemInfo){
			result = true;
		}
		
		return result;
	}
	
}
