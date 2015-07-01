package com.jzs.dr.mtplauncher.defaults.widget;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget.DragObject;
import com.jzs.dr.mtplauncher.sjar.widget.DeleteDropTarget;
import com.jzs.dr.mtplauncher.sjar.widget.DragLayer;
import com.jzs.dr.mtplauncher.sjar.widget.Workspace;
import com.jzs.dr.mtplauncher.sjar.widget.Folder;

public class QsDeleteDropTarget extends DeleteDropTarget {

	int mIconWidth = 0;
	
	public QsDeleteDropTarget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public QsDeleteDropTarget(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	@Override
    protected void setHoverColor() {
		setBackgroundColor(mHoverColor);
    }
	
	public void onDragExit(DragObject d) {
	    super.onDragExit(d);
	    setBackgroundColor(0x0);
	}
	
	 @Override
	 public void onDragStart(DragSource source, Object info, int dragAction) {
		boolean isVisible = false;
		
		if(isFromWorkSpace(source) || isFromFolder(source)){
			isVisible = true;
		}
		
		if(!isVisible){
			onDragEnd();
		}
		super.onDragStart(source, info, dragAction, isVisible, false);
		adjustDrawablePadding();
	 }
	
	 public void adjustDrawablePadding(){
		if(mRemoveDrawable == null){
			return;
		}
		setCompoundDrawablesWithIntrinsicBounds(null, mRemoveDrawable, null, null);
		Drawable iconDrawable = mRemoveDrawable.getDrawable(0);
		if(iconDrawable != null){
			mIconWidth = ((BitmapDrawable)iconDrawable).getBitmap().getWidth();
		}
	}
	
	@Override
	protected void animateToTrashAndCompleteDrop(final DragObject d) {
        DragLayer dragLayer = mLauncher.getDragLayer();
        Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);
        Rect to = new Rect(getWidth()/2 - mIconWidth*3/2, 0, getWidth()/2, getHeight());
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
                285, new DecelerateInterpolator(2),
                new LinearInterpolator(), onAnimationEndRunnable,
                DragLayer.ANIMATION_END_DISAPPEAR, null);
    }
	
	protected boolean isFromWorkSpace(DragSource source) {
		return source instanceof Workspace;
	}
	
	protected boolean isFromFolder(DragSource source){
		return source instanceof Folder;
	}
	

}
