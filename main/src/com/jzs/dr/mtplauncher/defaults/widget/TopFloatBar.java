package com.jzs.dr.mtplauncher.defaults.widget;

import com.jzs.dr.mtplauncher.LauncherApplicationMain;
import com.jzs.dr.mtplauncher.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

public class TopFloatBar extends LinearLayout {
	
	Context mContext;
	ImageView mDelete;
	ImageView mUnInstall;
	ImageView mInfo;
	View mLastSelected;
	boolean mUninstallEnable;

	public TopFloatBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initView();
		mUninstallEnable = true;
	}

	public TopFloatBar(Context context) {
		super(context, null);
	}
	
	protected void initView(){
		LinearLayout.LayoutParams params = createLayoutParams();
		mDelete = (ImageView)new ImageView(mContext);
		mDelete.setTag("delete");
		mDelete.setImageResource(R.drawable.app_remove_selector);
		mDelete.setScaleType(ScaleType.CENTER_INSIDE);
		mDelete.setBackgroundResource(R.drawable.top_float_bar_red_bg_selector);
		addView(mDelete, params);
		params = createLayoutParams();
		mUnInstall = (ImageView)new ImageView(mContext);
		mUnInstall.setTag("unInstall");
		mUnInstall.setScaleType(ScaleType.CENTER_INSIDE);
		mUnInstall.setImageResource(R.drawable.app_uninstall_selector);
		mUnInstall.setBackgroundResource(R.drawable.top_float_bar_blue_bg_selector);
		addView(mUnInstall, params);
		params = createLayoutParams();
		mInfo = (ImageView)new ImageView(mContext);
		mInfo.setTag("info");
		mInfo.setScaleType(ScaleType.CENTER_INSIDE);
		mInfo.setImageResource(R.drawable.app_info_selector);
		mInfo.setBackgroundResource(R.drawable.top_float_bar_blue_bg_selector);
		addView(mInfo, params);
	}

	private LinearLayout.LayoutParams createLayoutParams(){
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LayoutParams.FILL_PARENT);
		params.weight = 1.0f;
		params.gravity = Gravity.CENTER_VERTICAL;
		return params;
	}
	
	protected boolean isInRect(View view, int x, int y){
		if(view.getVisibility() == View.GONE)
			return false;
		if(x >= view.getX() && x <= (view.getX() + view.getWidth())
			&& (y <= view.getBottom())
			/*x >= view.getX() && x <= (view.getX() + view.getWidth())
			&& (y >= view.getY() && y <= view.getY() + view.getHeight())*/){
			if(!view.isSelected()){
				if(mLastSelected != null)
					mLastSelected.setSelected(false);
				view.setSelected(true);
				mLastSelected = view;
			}
			return true;
		}
		if(view.isSelected()){
			view.setSelected(false);
		}
		return false;
	}
	
	public void showUnistallDialog(String title, final String packageName){
		Uri packageURI = Uri.parse("package:" + packageName);
        Intent intent = new Intent(Intent.ACTION_DELETE,packageURI);
        mContext.startActivity(intent);   
        mLastSelected.setSelected(false);
        mLastSelected = null;
	}
	
	public void setUninstallEnable(boolean enable){
		mUninstallEnable = enable;
		mUnInstall.setVisibility(mUninstallEnable ? View.VISIBLE : View.GONE);
	}
	
	public void setShowInfoEnable(boolean enable){
		mInfo.setVisibility(enable ? View.VISIBLE : View.GONE);
	}
}
