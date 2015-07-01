package com.jzs.dr.mtplauncher.sjar.ctrl;

import android.graphics.drawable.Drawable;
import android.view.View;

public interface PageSwitchListener {
	void initial(int currPage, int pageCount);
	
	void onScrollChangedCallback(int l, int t, int oldl, int oldt);
	
	void onPageSwitch(View newPage, int newPageIndex);
	boolean onPageCountChanged(int nNewCount);
	
	public final static int CUSTOM_INDICATOR_FIRST = 0;
	public final static int CUSTOM_INDICATOR_LAST = -1;
	
	void setCustomPageIndicatorIcon(int index, Drawable dr);
}
