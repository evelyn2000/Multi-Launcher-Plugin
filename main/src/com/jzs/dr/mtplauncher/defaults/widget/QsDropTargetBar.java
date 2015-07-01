package com.jzs.dr.mtplauncher.defaults.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragController;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.widget.ButtonDropTarget;
import com.jzs.dr.mtplauncher.sjar.widget.CellLayout;
import com.jzs.dr.mtplauncher.sjar.widget.DragLayer;
import com.jzs.dr.mtplauncher.sjar.widget.SearchDropTargetBar;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget.DragObject;

public class QsDropTargetBar extends SearchDropTargetBar {
	
    protected ButtonDropTarget mUninstallDropTarget;
    private Launcher mLauncher;

	public QsDropTargetBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public QsDropTargetBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mUninstallDropTarget = (ButtonDropTarget) mDropTargetBar.findViewWithTag("uninstall_target_text");
		mUninstallDropTarget.setSearchDropTargetBar(this);
		
	}

	@Override
	public void setup(Launcher launcher, DragController dragController) {
		super.setup(launcher, dragController);
		 dragController.addDragListener(mUninstallDropTarget);
	     dragController.addDropTarget(mUninstallDropTarget);
	     mUninstallDropTarget.setLauncher(launcher);
	}

	@Override
	public void onDragEnd() {
		super.onDragEnd();
		if(mInfoDropTarget != null)
			mInfoDropTarget.onDragEnd();
		if(mUninstallDropTarget != null)
			mUninstallDropTarget.onDragEnd();
	}

	
//	public void QsSetLauncher(Launcher launcher){
//		mLauncher = launcher;
//	}
//	
//	public void QsRestoreCellInfoVisible(DragObject dragObject){
//		if(mLauncher == null || dragObject == null)
//			return;
//		if(dragObject.dragView == null)
//			return;
//		
//		DragLayer dragLayer = mLauncher.getDragLayer();
//        dragLayer.removeView(dragObject.dragView);
//        
//		ItemInfo info = (ItemInfo)dragObject.dragInfo;
//
//		if(info == null)
//			return;
//		
//        CellLayout parent = (CellLayout)mLauncher.getWorkspace().getPageAt(info.screen);
//        View child = parent.getChildAt(info.cellX, info.cellY);
//        if(child != null){
//        	child.setVisibility(View.VISIBLE);
//        }
//	}

	
}
