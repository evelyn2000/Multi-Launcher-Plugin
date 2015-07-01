package com.jzs.dr.mtplauncher.defaults.widget;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget.DragObject;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.widget.CellLayout;
import com.jzs.dr.mtplauncher.sjar.widget.DeleteDropTarget;
import com.jzs.dr.mtplauncher.sjar.widget.DragLayer;
import com.jzs.dr.mtplauncher.sjar.widget.UninstallDropTarget;
import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.model.ItemInfo;

public class QsUnistallDropTarget extends UninstallDropTarget {
//	PackageManager mPackageManager;
//	Context mContext;
//	String mPackageName;
    DragObject mDropObject;
	
	int mIconWidth = 0;

	public QsUnistallDropTarget(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
//		mPackageManager = context.getPackageManager();
//		mContext = context;
	}

	public QsUnistallDropTarget(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	@Override
    protected void setHoverColor() {
		//super.setHoverColor();
		setBackgroundColor(mHoverColor);
    }
	
	@Override
    protected void resetHoverColor() {
        super.resetHoverColor();
        setBackgroundColor(0x00000000);
    }

	@Override
	public void onDragStart(DragSource source, Object info, int dragAction) {
		super.onDragStart(source, info, dragAction);
		adjustDrawablePadding();
	}
	
//	@Override
//    public boolean acceptDrop(DragObject d) {
//		android.util.Log.e("QsLog", "acceptDrop :" + d);
//		if(mSearchDropTargetBar != null)
//        	mSearchDropTargetBar.onDragEnd();
//        mLauncher.exitSpringLoadedDragMode();
//        mDropObject = d;
//        DragLayer dragLayer = mLauncher.getDragLayer();
//        dragLayer.removeView(mDropObject.dragView);
//        restoreCellInfoVisible();
//        showUnistallDialog(d);
//        onDragExit(d);
//        onDragEnd();
//        return false;
//    }
	
//	public void showUnistallDialog(DragObject d){
//		ItemInfo item = (ItemInfo) d.dragInfo;
//
//        if (item instanceof ShortcutInfo) {
//            ShortcutInfo sInfo = (ShortcutInfo)item;
//            mLauncher.startApplicationUninstallActivity(sInfo.getPackageName(), sInfo.getClassName());
//        }
//		/*Uri packageURI = Uri.parse("package:" + packageName);
//        Intent intent = new Intent(Intent.ACTION_DELETE,packageURI);
//        mContext.startActivity(intent);   */
//	}
//	
//	private void restoreCellInfoVisible(){
//		if(mDropObject == null)
//			return;
//		
//		ItemInfo info = (ItemInfo)mDropObject.dragInfo;
//        CellLayout parent = (CellLayout)mLauncher.getWorkspace().getPageAt(info.screen);
//        View child = parent.getChildAt(info.cellX, info.cellY);
//        if(child != null){
//        	child.setVisibility(View.VISIBLE);
//        }
//        
//        mDropObject = null;
//	}
	
	public void adjustDrawablePadding(){
		if(mUninstallDrawable == null){
			return;
		}
		setCompoundDrawablesWithIntrinsicBounds(null, mUninstallDrawable, null, null);
		Drawable iconDrawable = mUninstallDrawable.getDrawable(0);
		if(iconDrawable != null){
			mIconWidth = ((BitmapDrawable)iconDrawable).getBitmap().getWidth();
		}
	}
}
