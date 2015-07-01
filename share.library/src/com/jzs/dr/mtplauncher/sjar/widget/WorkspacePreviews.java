package com.jzs.dr.mtplauncher.sjar.widget;

import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.dr.mtplauncher.sjar.Launcher;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.jzs.dr.mtplauncher.sjar.R;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

public class WorkspacePreviews extends LinearLayout implements View.OnDragListener, View.OnClickListener, View.OnLongClickListener {
	private final static String TAG = "WorkspacePreviews";
	private Launcher mLauncher;
	private Workspace mWorkspace;
	
	private final int mMaxScreenCount;
	private final int mPreviewBmpHeight;
	private int mPreviewBmpWidth;
	private WorkspacePreviewContainer mContainer;
	private final int mPreviewImgLayout;
	private final View mPreviewImgAddView;
	private View mDeleteView;
	private final LayoutInflater mInflater;
	private final int mMinMoveDeltaX;
	private float mPrevScale;
	
	
	public WorkspacePreviews(Context context) {
        this(context, null);
    }

    public WorkspacePreviews(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspacePreviews(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        mLauncher = (Launcher) context;
        
        mInflater = LayoutInflater.from(context);
        
        final ISharedPrefSettingsManager sharePref = mLauncher.getSharedPrefSettingsManager();
        
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.WorkspacePreviews, defStyle, 0);
        mMaxScreenCount = a.getInt(R.styleable.WorkspacePreviews_maxScreenCount, sharePref.getWorkspaceScreenMaxCount());
        mPreviewBmpHeight = a.getDimensionPixelSize(R.styleable.WorkspacePreviews_previewImageHeight, 0);
        mPreviewImgLayout = a.getResourceId(R.styleable.WorkspacePreviews_previewImageView, 0);
        int layout = a.getResourceId(R.styleable.WorkspacePreviews_addPreviewImageView, 0);
        if(layout > 0)
        	mPreviewImgAddView = mInflater.inflate(layout, null, false);
        else
        	mPreviewImgAddView = null;//mLauncher.getResConfigManager().inflaterView(IResConfigManager.LAYOUT_ADD_WORKSPACE_PREVIEW_ITEM);
        
        a.recycle();
        
		if(mPreviewImgLayout == 0 || mPreviewBmpHeight == 0){
			throw new RuntimeException("WorkspacePreviews previewImageView and previewImageHeight can't be 0..");
		}
        
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mMinMoveDeltaX = configuration.getScaledTouchSlop();
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        mContainer = (WorkspacePreviewContainer)findViewWithTag("container");
        mDeleteView = findViewWithTag("droptarget");
        
        if(mPreviewImgAddView != null){
	        mPreviewImgAddView.setOnClickListener(this);
	        mPreviewImgAddView.setTag("addtarget");

	        if(mDeleteView != null){
	            mContainer.setOnDragListener(this);
	        }
        } else if(mDeleteView != null){
            mDeleteView.setVisibility(View.GONE);
        }
        //setOnDragListener(this);
        
    }
    
    @Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		super.onDetachedFromWindow();
	}

	@Override
	public boolean onLongClick(View v) {
		// TODO Auto-generated method stub
		String tag = String.valueOf(v.getTag());
		if(tag != null && mWorkspace != null){
			try{
				int index = mContainer.indexOfChild(v);//Integer.parseInt(tag);
				if(index < mWorkspace.getPageCount()){
					if(startDrag(null, new DragShadowBuilder(v), index, 0)){
						return true;
					}
				}
			}catch(NumberFormatException ex){}
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		String tag = String.valueOf(v.getTag());
		if(tag != null && mWorkspace != null){
			if(tag.equals("addtarget")){
			    addScreen();
			} else {
				try{
					int index = mContainer.indexOfChild(v);//Integer.parseInt(tag);
					if(index < mWorkspace.getPageCount()){
						mLauncher.snapToWorkspaceScreen(index, true);
					}
				}catch(NumberFormatException ex){}
			}
		}
	}

	public void show(Workspace workspace){
    	
//    	mLauncher = context;
    	mWorkspace = workspace;
//    	if(LauncherLog.DEBUG_QS_I9300){
//    		android.util.Log.w("QsLog", "QsPreviewScreen::show() = ");
//    	}
//    	
    	initScreenPreviewBmp();

    	super.setVisibility(View.VISIBLE);
    }
    
    public void hide(){
    	super.setVisibility(View.GONE);
    	mContainer.removeAllViewsInLayout();
    }
    
    public boolean onDrag(View v, DragEvent event) {
        final int action = event.getAction();
        switch (action) {        
            case DragEvent.ACTION_DRAG_STARTED: {
//            	if(!v.equals(mCurFocusView))
//            		break;
//            	if(mCurFocusView != null)
//            		mCurFocusView.setVisibility(View.INVISIBLE);
            	int screen = Integer.parseInt(event.getLocalState().toString());
            	if(mDeleteView != null && screen >= 0){
            	    mContainer.getChildAt(screen).setVisibility(View.INVISIBLE);
            	}
            	if(Util.DEBUG_DRAG){
            		Util.Log.d(TAG, "onDrag(ACTION_DRAG_STARTED) ====x:"+event.getX()
            				+"==y:"+event.getY()
            				+"==screen:"+screen
            				+"==screenview:"+mContainer.getChildAt(screen)
            				+"==view:"+v);
            	}
            } break;
//            case DragEvent.ACTION_DRAG_LOCATION: {
//                // we returned true to DRAG_STARTED, so return true here
////            	if(!this.equals(v))
////            		break;
//            	
//            	final int x = (int)event.getX();
//            	final int y = (int)event.getY();
//
//            } break;
            case DragEvent.ACTION_DRAG_ENTERED:
                if(mDeleteView != null && mDeleteView.isEnabled()){
                    mDeleteView.setPressed(false);
                }
                if(Util.DEBUG_DRAG){
                	Util.Log.d(TAG, "onDrag(ACTION_DRAG_ENTERED) ====x:"+event.getX()
            				+"==y:"+event.getY()
            				+"==view:"+v);
                }
            	break;
            case DragEvent.ACTION_DRAG_EXITED:
                if(mDeleteView != null && mDeleteView.isEnabled()){
                    mDeleteView.setPressed(true);
                }
                if(Util.DEBUG_DRAG){
                	Util.Log.d(TAG, "onDrag(ACTION_DRAG_EXITED) ====x:"+event.getX()
            				+"==y:"+event.getY()
            				+"==view:"+v);
                }
            	break;
            case DragEvent.ACTION_DROP:
                if(Util.DEBUG_DRAG){
                	Util.Log.d(TAG, "onDrag(ACTION_DROP) ====x:"+event.getX()
                				+"==y:"+event.getY());
                }
            	break;
            case DragEvent.ACTION_DRAG_ENDED: {
                // Hide the surprise again
                int screen = Integer.parseInt(event.getLocalState().toString());
                if(Util.DEBUG_DRAG){
                    Util.Log.d(TAG, "onDrag(ACTION_DRAG_ENDED) ====x:"+event.getX()
                            +"==y:"+event.getY()
                            +"==screen:"+screen
                           );
                }
                
                if(mDeleteView != null){
                    if(mDeleteView.isEnabled() && mDeleteView.isPressed() && removeScreen(screen)){
                        
                    } else {
                        mContainer.getChildAt(screen).setVisibility(View.VISIBLE);
                    }
                    mDeleteView.setPressed(false);
                }
            } break;
        }
        return true;
    }
    
    private void initScreenPreviewBmp(){

    	mContainer.removeAllViewsInLayout();
    	
    	final CellLayout cell = ((CellLayout) mWorkspace.getChildAt(0));
    	final int nPressedIndex = mWorkspace.getDefaultScreenIndex();//.getCurrentPage();
    	
    	View previewImgView = mInflater.inflate(mPreviewImgLayout, null, false);

        int extraH = previewImgView.getPaddingTop() + previewImgView.getPaddingBottom();
        if(previewImgView instanceof PreviewItemContainer){
        	extraH = ((PreviewItemContainer)previewImgView).getPreviewImageVerticalPadding();
        }
        int width = cell.getWidth();
        int height = cell.getHeight();
        
        int bmpH = mPreviewBmpHeight - extraH;
        
        int x = cell.getPaddingLeft();
        int y = cell.getPaddingTop();
        width -= (x + cell.getPaddingRight());
        height -= (y + cell.getPaddingBottom());

//        android.util.Log.i("Jzs.Test", "=1=height:"+height
//        		+", ==Bottom:"+cell.getPaddingBottom()
//        		+", top:"+y
//        		+", ==Bottom:"+mWorkspace.getPaddingBottom()
//        		+", top:"+mWorkspace.getPaddingTop()
//        		+", h:"+mWorkspace.getHeight()
//        		+", pl:"+previewImgView.getPaddingLeft()
//        		+", pr:"+previewImgView.getPaddingRight()
//        		);
        
        float scale = (float)bmpH / height;
        mPrevScale = scale;
        
        final int sWidth = (int)(width * scale + 0.5f);
        final int sHeight = (int)(height * scale + 0.5f);
        
        mPreviewBmpWidth = sWidth + previewImgView.getPaddingLeft() + previewImgView.getPaddingRight();
        final int nCount = mWorkspace.getPageCount();
        final Canvas canvas = new Canvas();

        if(mDeleteView != null && !mWorkspace.isSupportEditPageScreen()){
            mDeleteView.setVisibility(View.GONE);
        }
        
        ViewGroup.LayoutParams layutparams = new ViewGroup.LayoutParams(mPreviewBmpWidth, mPreviewBmpHeight);
//        android.util.Log.i("Jzs.Test", "==mPreviewBmpHeight:"+mPreviewBmpHeight
//        		+", ==height:"+layutparams.height
//        		+", extraH:"+extraH
//        		+", mPreviewBmpWidth:"+mPreviewBmpWidth
//        		+", sWidth:"+sWidth
//        		+", sHeight:"+sHeight);
        
    	for(int i=0; i<nCount; i++){
    	    Bitmap bitmap = createPreviewBitmap(((CellLayout) mWorkspace.getPageAt(i)), 
                    scale, -x, -y, sWidth, sHeight, canvas);
    	    
    	    addPreviewView(bitmap, i, nPressedIndex, layutparams);
    	}
    	
    	if(mWorkspace.isSupportEditPageScreen()){
        	if(mPreviewImgAddView != null/* && nCount < mWorkspace.getMaxScreenCount()*/){
        		mContainer.addView(mPreviewImgAddView, layutparams);
        	}
        	
        	if(mDeleteView != null && nCount <= mWorkspace.getMinScreenCount()){
        	    mDeleteView.setEnabled(false);
        	}
    	}
    	
    	mContainer.requestLayout();
    }
    
    public static Bitmap createPreviewBitmap(CellLayout cell, float scale, int x, int y, int width, int height){
    	if(cell == null) return null;
    	return createPreviewBitmap(cell, scale, x, y, width, height, new Canvas());
    }
    
    public static Bitmap createPreviewBitmap(CellLayout cell, float scale, int x, int y, int width, int height, Canvas canvas){
    	if(cell == null) return null;
    	
    	final Bitmap bitmap = Bitmap.createBitmap((int) width, (int) height,
				Bitmap.Config.ARGB_8888);
    	
    	canvas.setBitmap(bitmap);
		//final Canvas c = new Canvas(bitmap);
    	canvas.save();
    	
    	canvas.scale(scale, scale);
    	if(x != 0 && y != 0)
    		canvas.translate(x, y);
		cell.dispatchDraw(canvas);
		
		canvas.restore();
		canvas.setBitmap(null);
		
		return bitmap;
    }
    
    private boolean removeScreen(int index){
        if(mWorkspace.getPageCount() > mWorkspace.getMinScreenCount()){
            int oldpressedindex = mWorkspace.getDefaultScreenIndex();//.getCurrentPage();
            if(index >= 0 && index < mWorkspace.getPageCount() && mWorkspace.removeScreen(index, true)){
                mContainer.removeViewAt(index);
                int newoldindex = mWorkspace.getDefaultScreenIndex();
//                android.util.Log.i("Jzs.Test", "==oldpressedindex:"+oldpressedindex
//                		+", newoldindex:"+newoldindex+", index:"+index
//                		+", count:"+mWorkspace.getPageCount());
                if(oldpressedindex != newoldindex || index <= oldpressedindex){
                	if(oldpressedindex >= mWorkspace.getPageCount()){
                		newoldindex = mWorkspace.getCurrentPage();
                		mWorkspace.setDefaultScreenIndex(newoldindex);
                	} else if(index < oldpressedindex){
                		if(oldpressedindex > 0)
                			mContainer.getChildAt(oldpressedindex-1).setActivated(false);
                	} else if(oldpressedindex != newoldindex){
                		mContainer.getChildAt(oldpressedindex).setActivated(false);
                	}
                	
                	mContainer.getChildAt(newoldindex).setActivated(true);
                }
                
//                if(index == oldpressedindex){
//                	if(mWorkspace.getDefaultScreenInternalIndex() < 0){
//                		oldpressedindex = mWorkspace.getDefaultScreenIndex();
//                	} else if(oldpressedindex >= mWorkspace.getPageCount()){
//                		oldpressedindex = mWorkspace.getCurrentPage();
//                		mWorkspace.setDefaultScreenIndex(oldpressedindex);
//                	}
//                    mContainer.getChildAt(oldpressedindex).setActivated(true);
//                    
//                } else if(index < oldpressedindex && oldpressedindex > 0){
//                    if(oldpressedindex < mWorkspace.getPageCount())
//                        mContainer.getChildAt(oldpressedindex).setActivated(false);
//                    
//                    oldpressedindex--;
//                    mWorkspace.setCurrentPage(oldpressedindex);
//                    mContainer.getChildAt(oldpressedindex).setActivated(true);
//                }
                
                if(mPreviewImgAddView != null){
                    mPreviewImgAddView.setVisibility(View.VISIBLE);
                }
                
                if(mDeleteView != null && mWorkspace.getPageCount() <= mWorkspace.getMinScreenCount()){
                    mDeleteView.setEnabled(false);
                }
                return true;
            }
        }
        return false;
    }
    
    private void addScreen(){
        if(mWorkspace == null)
            return;
        
        int oldpressedindex = mWorkspace.getDefaultScreenIndex();//.getCurrentPage();
        View view = mWorkspace.addScreen(true);
        if(view != null){
            int index = mWorkspace.indexOfChild(view);//.getChildCount()-1;
            if(Util.DEBUG_DRAG){
                Util.Log.i(TAG, "addScreen() ====index:"+index
                        +"==count:"+mWorkspace.getPageCount()
                        +"==max:"+mWorkspace.getMaxScreenCount());
            }
            
            int newoldindex = mWorkspace.getDefaultScreenIndex();
            if(oldpressedindex != newoldindex){
            	mContainer.getChildAt(oldpressedindex).setActivated(false);
            	mContainer.getChildAt(newoldindex).setActivated(true);
            }
            
//            CellLayout cell = (CellLayout) view;
//            int x = cell.getPaddingLeft();
//            int y = cell.getPaddingTop();
//            
//            int width = cell.getWidth();
//            int height = cell.getHeight();
//            
//            width = (x + cell.getPaddingRight());
//            height = (y + cell.getPaddingBottom());
//            
//            final int sWidth = (int)(width * mPrevScale + 0.5f);
//            final int sHeight = (int)(height * mPrevScale + 0.5f);
//            
//            Bitmap bitmap = createPreviewBitmap(((CellLayout) view), 
//                    mPrevScale, -x, -y, sWidth, sHeight, new Canvas());
            
            if(mWorkspace.getPageCount() >= mWorkspace.getMaxScreenCount()){
                if(mPreviewImgAddView != null)
                    mPreviewImgAddView.setVisibility(View.GONE);//.removeView(mPreviewImgAddView);
            } else if(mDeleteView != null && !mDeleteView.isEnabled()){
                mDeleteView.setEnabled(true);
            }
            
            //ViewGroup.LayoutParams layutparams = new ViewGroup.LayoutParams(mPreviewBmpWidth, mPreviewBmpHeight);
            addPreviewView(null, index, -1);
            
            
            
            
            //android.util.Log.i("QsLog", "addScreen==index:"+index+"==childcount:"+count);

//            mPreviewImageView[index] = createPreviewImageView(mLauncher/*, mPreviewBitmaps[index]*/);
//            mPreviewImageView[index].setTag(index);
//
//            if(count < mWorkspace.getMaxScreenCount())
//                mPreviewImageView[count] = mPreviewImageViewForAdd;
//            
//            updateScreenPreview();
        }
    }
    
    private void addPreviewView(Bitmap bitmap, int index){
        addPreviewView(bitmap, index, -1);
    }
    
    private void addPreviewView(Bitmap bitmap, int index, int pressindex){
        addPreviewView(bitmap, index, pressindex, 
        		new ViewGroup.LayoutParams(mPreviewBmpWidth, mPreviewBmpHeight));
    }
    
    private void addPreviewView(Bitmap bitmap, int index, int pressindex, ViewGroup.LayoutParams layutparams){
        View view = mInflater.inflate(mPreviewImgLayout, null, false);
        
        if(view instanceof PreviewItemContainer){
        	((PreviewItemContainer)view).setImageBitmap(bitmap);
        	((PreviewItemContainer)view).setDefaultScreenClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(mWorkspace == null) return;
					v.setActivated(true);
					
					//final ISharedPrefSettingsManager sharePref = mLauncher.getSharedPrefSettingsManager();
					int currindex = mWorkspace.getDefaultScreenIndex();
					int index = mContainer.indexOfChild((View)v.getParent());
					if(index >= 0 && index != currindex){
						mWorkspace.setDefaultScreenIndex(index);
						mContainer.getChildAt(currindex).setActivated(false);
					}
				}
			});        			
        } else {
        	((ImageView)view).setImageBitmap(bitmap);
        }
        
        view.setTag(index);
        view.setActivated((index == pressindex ? true : false));
        view.setOnClickListener(this);
        if(mDeleteView != null && mDeleteView.getVisibility() == View.VISIBLE){
        	view.setOnLongClickListener(this);
        }
        mContainer.addView(view, index, layutparams);//.addView(image, layutparams);
    }
}
