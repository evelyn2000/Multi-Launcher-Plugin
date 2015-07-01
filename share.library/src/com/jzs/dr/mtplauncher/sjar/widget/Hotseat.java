package com.jzs.dr.mtplauncher.sjar.widget;

import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.Launcher;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.jzs.dr.mtplauncher.sjar.R;
import com.jzs.dr.mtplauncher.sjar.ctrl.HotseatIconKeyEventListener;
import com.jzs.dr.mtplauncher.sjar.model.LauncherModel;
import com.jzs.dr.mtplauncher.sjar.model.ResConfigManager;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

public class Hotseat extends FrameLayout{
	private static final String TAG = "Hotseat";
	
	public final static int ALL_APP_BTN_RANK_NONE = -1;
//	public final static int ALL_APP_BTN_RANK_LEFT = 0;
//	public final static int ALL_APP_BTN_RANK_MIDDLE = 1;
//	public final static int ALL_APP_BTN_RANK_RIGHT = 2;

    private Launcher mLauncher;
    private CellLayout mContent;

    private int mCellCountX;
    private int mCellCountY;
    private int mAllAppsButtonRank;
    private int mScalePercentage;
    private int mAllAppBtnLayout;

    private boolean mTransposeLayoutWithOrientation;
    private boolean mIsLandscape;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        mLauncher = (Launcher)context;
        
        Resources r = context.getResources();
        mIsLandscape = context.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;

        TypedArray a = context.obtainStyledAttributes(attrs,
        		R.styleable.Hotseat, defStyle, 0);
        
        mCellCountX = a.getInt(R.styleable.Hotseat_cellCountX, 5);
        mCellCountY = a.getInt(R.styleable.Hotseat_cellCountY, 1);
        mAllAppBtnLayout = a.getResourceId(R.styleable.Hotseat_allAppButtonLayout, 0);
        
        mAllAppsButtonRank = a.getInt(R.styleable.Hotseat_allAppButtonRank, Math.max(mCellCountY, mCellCountX) / 2);
//        int curRank = a.getInt(R.styleable.Hotseat_allAppButtonRank, ALL_APP_BTN_RANK_MIDDLE);
//    	if(curRank == ALL_APP_BTN_RANK_RIGHT)
//    		mAllAppsButtonRank = (hasVerticalHotseat() ? mCellCountY : mCellCountX);
//		else if(curRank == ALL_APP_BTN_RANK_MIDDLE)
//			mAllAppsButtonRank = Math.max(mCellCountY, mCellCountX) / 2;
//		else if(curRank == ALL_APP_BTN_RANK_LEFT)
//			mAllAppsButtonRank = 0;
//		else if(ALL_APP_BTN_RANK_NONE == curRank)
//			mAllAppsButtonRank = ALL_APP_BTN_RANK_NONE;
    	
        mScalePercentage = a.getInt(R.styleable.Hotseat_scalePercentage, 100);
        a.recycle();
    }

    public void setup(Launcher launcher) {
    	//Util.Log.w(TAG, "setup() launcher:"+launcher);
        mLauncher = launcher;
        //setOnKeyListener(new HotseatIconKeyEventListener());
    }

    public CellLayout getLayout() {
        return mContent;
    }
  
    private boolean hasVerticalHotseat() {
        return (mIsLandscape /*&& mTransposeLayoutWithOrientation*/);
    }

    /* Get the orientation invariant order of the item in the hotseat for persistence. */
    public int getOrderInHotseat(int x, int y) {
        return hasVerticalHotseat() ? (mContent.getCountY() - y - 1) : x;
    }
    /* Get the orientation specific coordinates given an invariant order in the hotseat. */
    public int getCellXFromOrder(int rank) {
        return hasVerticalHotseat() ? 0 : rank;
    }
    
    public int getCellYFromOrder(int rank) {
        return hasVerticalHotseat() ? (mContent.getCountY() - (rank + 1)) : 0;
    }
    
    public boolean isAllAppsButtonRank(int rank) {
    	if(mAllAppBtnLayout == 0 || mAllAppsButtonRank < 0)
    		return false;
        return rank == mAllAppsButtonRank;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
//        if (mCellCountX < 0) mCellCountX = LauncherModel.getCellCountX();
//        if (mCellCountY < 0) mCellCountY = LauncherModel.getCellCountY();
        mContent = (CellLayout) findViewWithTag("content");// findViewById(R.id.layout);
        if(mContent == null){
        	Util.Log.w(TAG, "onFinishInflate() get content view fail...");
        	return;
        }
        mContent.setGridSize(mCellCountX, mCellCountY);
        mContent.setIsHotseat(true);
        mContent.setHotseatScalePercentage(mScalePercentage);
        
        resetLayout();
    }

    public void resetLayout() {
    	mContent.removeAllViewsInLayout();
    	
        if(mAllAppBtnLayout == 0 || mAllAppsButtonRank < 0){
        	Util.Log.w(TAG, "resetLayout() no all btn, mAllAppBtnLayout:0x"+Integer.toHexString(mAllAppBtnLayout)
        			+", mAllAppsButtonRank:"+mAllAppsButtonRank);
        	return;
        }
        // Add the Apps button
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        BubbleTextView allAppsButton = (BubbleTextView)
                inflater.inflate(mAllAppBtnLayout, mContent, false);
        if(allAppsButton == null)
        	return;
//        android.util.Log.i("QsLog", "hotseat====text:"+allAppsButton.getText()
//                +"==visiable:"+mLauncher.getShortcutLableVisiable()
//                +"==viewvisiable:"+allAppsButton.getViewLabelVisible());
        ItemInfo info = new ItemInfo();
        info.title = mLauncher.getResConfigManager().getText(ResConfigManager.STR_ALL_APPS_BTN_LABEL);
        allAppsButton.setTag(info);
        allAppsButton.setViewLabelVisible(mLauncher.getShortcutLableVisiable());
        

        ISharedPrefSettingsManager sharedPref = mLauncher.getSharedPrefSettingsManager();
        int size = sharedPref.getAppIconSize();
        final Drawable[] drawables = allAppsButton.getCompoundDrawables();
        if(drawables != null){
        	for(int i=0; i < drawables.length; i++){
        		if(drawables[i] != null){
        		    if(drawables[i].getIntrinsicWidth() > size 
        		            || drawables[i].getIntrinsicHeight() > size){
//        		        android.util.Log.i("QsLog", "=11==w:"+drawables[i].getIntrinsicWidth()
//        		                +"==h:"+drawables[i].getIntrinsicHeight()
//        		                +"==size:"+size);
        		        
        		        if(Util.scaleBitmapDrawable(drawables[i], size, size)){
        		            allAppsButton.setCompoundDrawablesWithIntrinsicBounds(drawables[0], 
        		                    drawables[1], drawables[2], drawables[3]);
//        		            
//        		            android.util.Log.i("QsLog", "=22==w:"+drawables[i].getIntrinsicWidth()
//                                    +"==h:"+drawables[i].getIntrinsicHeight()
//                                    +"==size:"+size);
        		        }
        		    }
        		    break;
        		}
        	}
        }
        
        allAppsButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mLauncher != null &&
                    (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                    mLauncher.onTouchDownAllAppsButton(v);
                }
                return false;
            }
        });

        allAppsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                if (Util.ENABLE_DEBUG) {
                    Util.Log.d(TAG, "Click on all apps view on hotseat: mLauncher = " + mLauncher);
                }
                if (mLauncher != null) {
                    mLauncher.onClickAllAppsButton(v);
                }
            }
        });

        // Note: We do this to ensure that the hotseat is always laid out in the orientation of
        // the hotseat in order regardless of which orientation they were added
        int x = getCellXFromOrder(mAllAppsButtonRank);
        int y = getCellYFromOrder(mAllAppsButtonRank);
        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(x,y,1,1);
        lp.canReorder = false;
        mContent.addViewToCellLayout(allAppsButton, -1, 0, lp, true);
    }
    
    @Override
    public void getHitRect(android.graphics.Rect outRect) {
        super.getHitRect(outRect);
        int x = outRect.left;
        int y = outRect.top;
        View parent = (View)getParent();
        while(parent != null && !(parent instanceof DragLayer)){
        	final ViewGroup.LayoutParams lp = parent.getLayoutParams();
            if(lp.width == ViewGroup.LayoutParams.MATCH_PARENT && lp.height == ViewGroup.LayoutParams.MATCH_PARENT)
            	break;
            x += parent.getLeft();
            y += parent.getTop();

            parent = (View)parent.getParent();
        }
        
        if(x != outRect.left || y != outRect.top)
        	outRect.offsetTo(x, y);
    }
    
    public void setShortcutLabelVisiable(boolean visiable){
        ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
        int count = container.getChildCount();
        for(int i=0; i<count; i++){
            final View view = container.getChildAt(i);
            if(view instanceof IShortcutView){
                ((IShortcutView)view).setViewLabelVisible(visiable);
            }
        }
    }
}
