package com.jzs.dr.mtplauncher.sjar.widget;

import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.model.ResConfigManager;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WorkspaceCustomSettings extends LinearLayout implements View.OnClickListener {
	private Launcher mLauncher;
	private Workspace mWorkspace;
	private WorkspaceCustomSettingsPreviewView mPreviewView;
	private View mAppearanceContainer;
	private ViewGroup mWorkspaceGridContainer;
	private boolean mIsAttached = false;

	private final ISharedPrefSettingsManager mSharePrefManager;
//	private final Drawable[] mGridIconPreview;
	
	public WorkspaceCustomSettings(Context context) {
        this(context, null);
    }

    public WorkspaceCustomSettings(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspaceCustomSettings(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        mLauncher = (Launcher) context;
        mSharePrefManager = mLauncher.getSharedPrefSettingsManager();
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        mPreviewView = (WorkspaceCustomSettingsPreviewView)findViewWithTag("preview");
        mAppearanceContainer = findViewWithTag("container_appearance");
        mWorkspaceGridContainer = (ViewGroup)findViewWithTag("container_grid_list");
    }
    
    @Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
		mIsAttached = true;
	}

	@Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
	    mIsAttached = false;
		//super.removeCallbacks(mRefreshPreview);
		super.onDetachedFromWindow();
	}
	
	public boolean onBackPressed(){
	    if(mWorkspaceGridContainer != null && mWorkspaceGridContainer.getVisibility() == View.VISIBLE){
	        mAppearanceContainer.setVisibility(View.VISIBLE);
	        mWorkspaceGridContainer.setVisibility(View.GONE);
//	        View view = mAppearanceContainer.findViewWithTag("grid");
//            if(view != null)
//                view.setEnabled(true);
                
	        return true;
	    }
	    return false;
	}
	
	public void show(Workspace workspace){
    	
    	mWorkspace = workspace;
    	Runnable processIntent = new Runnable() {
            public void run() {
            	initStatus();
            }
    	};
    	super.post(processIntent);
    	
    	super.setVisibility(View.VISIBLE);
    	
    	if(mPreviewView != null)
    		mPreviewView.show(workspace);
    }
    
    public void hide(){
    	
        if(mPreviewView != null)
            mPreviewView.hide();
    	//super.removeCallbacks(mRefreshPreview);
    	super.setVisibility(View.GONE);
    	
    	mAppearanceContainer.setVisibility(View.VISIBLE);
    	if(mWorkspaceGridContainer != null)
    	    mWorkspaceGridContainer.setVisibility(View.GONE);
    }
    
    @Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		String tag = String.valueOf(v.getTag());
		if(tag != null){
			if(tag.equals("appicons")){
				mLauncher.showAppIconAndTitleCustomActivity();
				Runnable processIntent = new Runnable() {
		            public void run() {
		            	mLauncher.hideCustomSettingsScreen(false);
		            }
		    	};
		    	super.post(processIntent);
			} else if(tag.equals("cycleslide")){
				if(mWorkspace != null){
					boolean enable = !mWorkspace.isSupportCycleSlidingScreen();
					mWorkspace.setSupportCycleSlidingScreen(enable);
					mSharePrefManager.setWorkspaceSupportCycleSliding(enable);
					v.setActivated(enable);
				}
				
			} else if(tag.equals("hotseat")){
				boolean enable = mLauncher.isHotseatVisiable();
				if(enable)
					mLauncher.hideHotseat(false);
				else
					mLauncher.showHotseat(false);
				mSharePrefManager.setWorkspaceShowHotseatBar(!enable);
				mWorkspace.updateLayoutCustomSettingsChanged(true, null);
				v.setActivated(!enable);
				refreshPreviewView();
				
			} else if(tag.equals("indicator")){
				if(mWorkspace != null){
					boolean enable = !mWorkspace.isScrollingIndicatorEnabled();
					mWorkspace.setScrollingIndicatorEnabled(enable);
					mSharePrefManager.setWorkspaceEnableScreenIndicatorBar(enable);
					v.setActivated(enable);
					mWorkspace.updateLayoutCustomSettingsChanged(true, null);
					refreshPreviewView();
				}
			} else if(tag.equals("label")){
			    
			    boolean visiable = mSharePrefManager.getBoolean(ISharedPrefSettingsManager.KEY_WORKSPACE_SHOW_LABEL, 
	                    ResConfigManager.CONFIG_SHOW_WORKSPACE_LABEL, true);
			    
			    mLauncher.setShortcutLableVisiable(!visiable, mUpdateLabelComplete);
			    v.setActivated(!visiable);
			    v.setEnabled(false);
			    
//			    mSharePrefManager.setBoolean(ISharedPrefSettingsManager.KEY_WORKSPACE_SHOW_LABEL, !visiable);
//			    
//				refreshPreviewView();
			} else if(tag.equals("grid")){
			    
//			    mLauncher.changeWorkspaceGridSize(mUpdateGridComplete);
			    //v.setEnabled(false);
			    mWorkspaceGridContainer.setVisibility(View.VISIBLE);
			    mAppearanceContainer.setVisibility(View.GONE);
			    
			} else if(tag.equals("statusbar")){
				final boolean isfull = mSharePrefManager.getBoolean(ISharedPrefSettingsManager.KEY_IS_FULLSCREEN, false);
				//android.util.Log.i("QsLog", "onClick======isfull:"+isfull);
				mLauncher.setFullscreen(!isfull);
				//android.util.Log.i("QsLog", "onClick===2===isfull:"+isfull);
				v.setActivated(isfull);
				mSharePrefManager.setBoolean(ISharedPrefSettingsManager.KEY_IS_FULLSCREEN, !isfull);
			} else if(tag.equals("lock")){
				if(mWorkspace != null){
					boolean enable = !mWorkspace.isEnableStaticWallpaper();
					mWorkspace.setEnableStaticWallpaper(enable);
					mSharePrefManager.setEnableStaticWallpaper(enable);
					v.setActivated(enable);
					mWorkspace.syncWallpaperOffsetWithScroll();
				}
			}
		}
	}
    
    private void initStatus(){
    	View view = mAppearanceContainer.findViewWithTag("cycleslide");
    	if(view != null && mWorkspace != null){
    		view.setActivated(mWorkspace.isSupportCycleSlidingScreen());
    		view.setOnClickListener(this);
    		view.setEnabled(true);
    	}
    	
    	view = mAppearanceContainer.findViewWithTag("appicons");
    	if(view != null){
    		view.setOnClickListener(this);
    		view.setEnabled(true);
    	}
    	
    	view = mAppearanceContainer.findViewWithTag("hotseat");
    	if(view != null){
    		view.setActivated(mLauncher.isHotseatVisiable());
    		view.setOnClickListener(this);
    		view.setEnabled(true);
    	}
    	
    	view = mAppearanceContainer.findViewWithTag("indicator");
    	if(view != null && mWorkspace != null){
    		view.setActivated(mWorkspace.isScrollingIndicatorEnabled());
    		view.setOnClickListener(this);
    		view.setEnabled(true);
    	}
    	
    	view = mAppearanceContainer.findViewWithTag("label");
    	if(view != null){
    	    boolean visiable = mSharePrefManager.getBoolean(ISharedPrefSettingsManager.KEY_WORKSPACE_SHOW_LABEL, 
                    ResConfigManager.CONFIG_SHOW_WORKSPACE_LABEL, true);
    		view.setActivated(visiable);    		
    		view.setOnClickListener(this);
    		view.setEnabled(true);
    	}
    	
    	view = mAppearanceContainer.findViewWithTag("grid");
    	if(view != null){
    		view.setActivated(true);
    		view.setOnClickListener(this);
    		//view.setEnabled(true);
    		initWorkspaceGridSize(view);
    	}
    	
    	view = mAppearanceContainer.findViewWithTag("statusbar");
    	if(view != null){
    		boolean isFull = mSharePrefManager.getBoolean(ISharedPrefSettingsManager.KEY_IS_FULLSCREEN, 
    				ResConfigManager.CONFIG_IS_FULLSCREEN, false);
    		//android.util.Log.i("QsLog", "initStatus======isfull:"+isFull);
    		view.setActivated(!isFull);
    		view.setOnClickListener(this);
    		view.setEnabled(true);
    	}
    	
    	view = mAppearanceContainer.findViewWithTag("lock");
    	if(view != null){
    		view.setActivated(mWorkspace.isEnableStaticWallpaper());
    		view.setOnClickListener(this);
    		view.setEnabled(true);
    	}
    	
    	if(mWorkspaceGridContainer != null){
    	    int count = mWorkspaceGridContainer.getChildCount();
    	    for(int i=0; i<count; i++){
    	        view = mWorkspaceGridContainer.getChildAt(i);
    	        view.setOnClickListener(mGridContainerClickListener);
    	        view.setEnabled(true);
    	    }
    	}
    }
    
    protected void changeWorkspaceGridSize(int x, int y){
//        android.util.Log.i("QsLog", "changeWorkspaceGridSize====="
//                +"==x:"+mSharePrefManager.getWorkspaceCountCellX()
//                +"==y:"+mSharePrefManager.getWorkspaceCountCellY()
//                +"==x:"+x
//                  +"==y:"+y);
        
        if(x > 0 && y > 0 && (x != mSharePrefManager.getWorkspaceCountCellX() || 
            y != mSharePrefManager.getWorkspaceCountCellY())){

            mLauncher.changeWorkspaceGridSize(mSharePrefManager, x, y, mUpdateGridComplete);

            int count = mWorkspaceGridContainer.getChildCount();
            for(int i=0; i<count; i++){
                final View child = mWorkspaceGridContainer.getChildAt(i);
                child.setEnabled(false);
            }
        }
    }
    
    private final View.OnClickListener mGridContainerClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if(v.getTag() != null){
                String[] xy = v.getTag().toString().split("x");
                if(xy != null && xy.length == 2){
                    try {
                        changeWorkspaceGridSize(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
                    } catch (NumberFormatException ex){
                        
                    }
                }
            }
        }
    };
    
    private void refreshPreviewView(){
    	//postDelayed(mRefreshPreview, 200);
    }
    
    public static int getSelectWorkspaceGridSize(String[] strValuesArrays, int x, int y){
        String strSize = x + "x" + y;
        if(strValuesArrays != null){
            for(int i=strValuesArrays.length-1; i>=0; i--){
                if(strSize.equals(strValuesArrays[i])){
                    return i;
                }
            }
        }
        return -1;
    }
    
    private void initWorkspaceGridSize(View view){
        //TextView
        final int x = mSharePrefManager.getWorkspaceCountCellX();
        final int y = mSharePrefManager.getWorkspaceCountCellY();

        if(mWorkspaceGridContainer != null){
            int count = mWorkspaceGridContainer.getChildCount();
            for(int i=0; i<count; i++){
                final View child = mWorkspaceGridContainer.getChildAt(i);
                child.setEnabled(true);
                child.setActivated(false);
            }
            
            String strSize = x + "x" + y;
            TextView checkView = (TextView)mWorkspaceGridContainer.findViewWithTag(strSize);
            if(checkView != null){
                checkView.setActivated(true);
                final Drawable[] dr =  checkView.getCompoundDrawables();
                ((TextView)view).setCompoundDrawablesWithIntrinsicBounds(dr[0], dr[1], dr[2], dr[3]);
            }
        }
        
//        final IResConfigManager resources = mLauncher.getResConfigManager();
//        final String[] strArrays = resources.getStringArray(IResConfigManager.ARRAY_WORKSPACE_GRID_SIZE_VALUES);
//        int index = -1;
//        if(strArrays != null){
//            
//            index = getSelectWorkspaceGridSize(strArrays, x, y);
//            if(index < 0)
//                index = strArrays.length-1;
//            
////            android.util.Log.i("QsLog", "initWorkspaceGridSize====="
////                    +"==index:"+index
////                    +"==x:"+x
////                    +"==y:"+y);
//            
//            Drawable icon = null;
//            final TypedArray array = resources.obtainTypedArray(IResConfigManager.ARRAY_WORKSPACE_GRID_SIZE_PREVIEWS);
//            if(array != null){
//                int n = array.length();
//                if(index < n){
//                    icon = array.getDrawable(index);
//                }
//                array.recycle();
//            }
//            
//            if(icon != null){
//                ((TextView)view).setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
//            }
//        }
    }
//    
//    private final Runnable mRefreshPreview = new Runnable(){
//
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//			if(mPreviewView != null)
//				mPreviewView.refreshPreviewView();
//		}
//    	
//    };
    
    private final Runnable mUpdateLabelComplete = new Runnable(){
        @Override
        public void run() {
            // TODO Auto-generated method stub
            if(!mIsAttached)
                return;
            
            boolean visiable = mSharePrefManager.getBoolean(ISharedPrefSettingsManager.KEY_WORKSPACE_SHOW_LABEL, 
                    ResConfigManager.CONFIG_SHOW_WORKSPACE_LABEL, true);
            
            mSharePrefManager.setBoolean(ISharedPrefSettingsManager.KEY_WORKSPACE_SHOW_LABEL, !visiable);
            
            View view = mAppearanceContainer.findViewWithTag("label");
            if(view != null)
                view.setEnabled(true);
        }
    };
    
    private final Runnable mUpdateGridComplete = new Runnable(){
        @Override
        public void run() {
            // TODO Auto-generated method stub
            if(!mIsAttached)
                return;

            View view = mAppearanceContainer.findViewWithTag("grid");
            if(view != null){
                initWorkspaceGridSize(view);
                //view.setEnabled(true);
            }
        }
    };
}
