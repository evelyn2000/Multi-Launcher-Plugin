package com.jzs.dr.mtplauncher.sjar.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.dr.mtplauncher.sjar.Launcher;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.jzs.dr.mtplauncher.sjar.R;

public class WorkspaceCustomSettingsPreviewView extends View/* implements WorkspaceContainer.IScreenUpdateCallback*/ {
	private Launcher mLauncher;
	private Workspace mWorkspace;

	private int mPreviewImageWidth;
	private int mPreviewImageHeight;
	private float mPreviewScale = 1.0f;
	private boolean mIsTouching;
	private final ISharedPrefSettingsManager mSharePrefManager;
	private final int mUserPaddingTop;
	private final int mUserPaddingBottom;
	private final Drawable mPreviewBg;

	
	private Bitmap mFrontCacheBmp;
	private Bitmap mBackCacheBmp;
	private Bitmap mDrawCacheBmp;
	
	private static final Canvas sCanvas = new Canvas();
	
	private final Object mLock = new Object();
	
	private final Rect mBgPadding = new Rect();
	private View mWorkspaceContainer;

	private int mRefreshTimer = 800;
	private boolean mIsVisiable = false;
	
	protected static Handler sHandler;
	private final static int MSG_REFRESH = 10;
	
	public WorkspaceCustomSettingsPreviewView(Context context) {
        this(context, null);
    }

    public WorkspaceCustomSettingsPreviewView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspaceCustomSettingsPreviewView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLauncher = (Launcher) context;
        mSharePrefManager = mLauncher.getSharedPrefSettingsManager();
        
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.WorkspaceCustomSettingsPreviewView, defStyle, 0);
        mUserPaddingTop = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingTop, getPaddingTop());
        mUserPaddingBottom = a.getDimensionPixelSize(com.android.internal.R.styleable.View_paddingBottom, getPaddingBottom());
        mPreviewBg = a.getDrawable(R.styleable.WorkspaceCustomSettingsPreviewView_previewBackground);
        if(mPreviewBg != null)
        	mPreviewBg.getPadding(mBgPadding);
        else
        	mBgPadding.setEmpty();
        a.recycle();
        
        sHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_REFRESH:
                        refreshPreviewView();
                        break;
                    default:
                        break;
                }
            }
        };
//        android.util.Log.i("QsLog", "===mUserPaddingTop:"+mUserPaddingTop
//        		+"==mUserPaddingBottom:"+mUserPaddingBottom);
    }
    
    public void show(Workspace workspace){
    	mWorkspace = workspace;
//    	android.util.Log.i("QsLog", "show()=====mPreviewImageWidth:"+mPreviewImageWidth
//				+"==mPreviewImageHeight:"+mPreviewImageHeight);
    	mIsVisiable = true;
    	mWorkspaceContainer = mLauncher.getWorkspaceContainer();
		initScreenPreviewBmp();

    	super.invalidate();
    	
    	refreshPreviewView();
    }
    
    @Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
    	mIsVisiable = false;
    	sHandler.removeMessages(MSG_REFRESH);
		super.onDetachedFromWindow();
	}
    
    public void hide(){
    	mIsVisiable = false;

    	sHandler.removeMessages(MSG_REFRESH);
    	mWorkspace = null;
    	mWorkspaceContainer = null;
    	synchronized (mLock) {
    		mDrawCacheBmp = null;
    	}
    	
    	if(mFrontCacheBmp != null){
			mFrontCacheBmp.recycle();
			mFrontCacheBmp = null;
    	}
		
		if(mBackCacheBmp != null){
    		mBackCacheBmp.recycle();
    		mBackCacheBmp = null;
    	}
    }
    

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		// TODO Auto-generated method stub
		super.onLayout(changed, left, top, right, bottom);
		
		if(mIsVisiable && (mPreviewImageWidth <= 0 || mPreviewImageHeight <= 0)){
			initScreenPreviewBmp();
			invalidate();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		//super.onDraw(canvas);
	    //android.util.Log.v("QsLog", "Preview::onDraw()===mIsVisiable:"+mIsVisiable);
		if(mPreviewImageWidth == 0 || mPreviewImageHeight == 0){
			super.onDraw(canvas);
			return;
		}
		
		final int width = super.getWidth();
		final int height = super.getHeight();
		
		canvas.save();
		
		final int left = getPaddingLeft() + (width - super.getPaddingLeft() - super.getPaddingRight() - mPreviewImageWidth)/2;
		final int top = mUserPaddingTop + (height - mUserPaddingTop - mUserPaddingBottom - mPreviewImageHeight)/2;
		if(mPreviewBg != null){
			mPreviewBg.setBounds(left - mBgPadding.left, 
	        		top - mBgPadding.top, 
					left + mPreviewImageWidth+mBgPadding.right, 
					top + mPreviewImageHeight+mBgPadding.bottom);
			mPreviewBg.draw(canvas);
		}
		synchronized (mLock) {
			if(mDrawCacheBmp != null)
				canvas.drawBitmap(mDrawCacheBmp, left, top, null);
		}
		
		canvas.restore();
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		final int action = event.getAction();
    	
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
        	mIsTouching = true;
        	if(mWorkspace != null){
            	mWorkspace.setTouchState(PagedView.TOUCH_STATE_SCROLLING);
            	mRefreshTimer = 50;
            	refreshPreviewView();
        	}
        	break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
        	mIsTouching = false;
        	mRefreshTimer = 200;
        	break;
        }
        
        if(mWorkspace != null){
        	mWorkspace.onTouchEvent(event);
        }
        
		return mIsTouching || super.onTouchEvent(event);
	}
	
	private void initScreenPreviewBmp(){
		if(mWorkspaceContainer == null){
			android.util.Log.i("QsLog", "initScreenPreviewBmp()==container is null===");
			return;
		}
		
		mPreviewImageHeight = super.getHeight() - mUserPaddingTop - mUserPaddingTop;
		if(mPreviewImageHeight <= 0){
            android.util.Log.i("QsLog", "initScreenPreviewBmp()==width:"+super.getWidth()
                    +" or height:"+super.getHeight()+" size error===");
            return;
        }
		
		final int width = mWorkspaceContainer.getWidth();
        final int height = mWorkspaceContainer.getHeight();
        
        float scale = (float)mPreviewImageHeight / height;
        
        mPreviewScale = scale;
        int sWidth = mPreviewImageWidth = (int)(width * scale + 0.5f);
        int sHeight = mPreviewImageHeight = (int)(height * scale + 0.5f);
		
//        android.util.Log.i("QsLog", "initScreenPreviewBmp(1)==="
//				+"==sWidth:"+sWidth
//				+"==sHeight:"+sHeight
//				+"==scale:"+scale
//				+"==width:"+getWidth()
//				+"==height:"+getHeight());
        
        if(mFrontCacheBmp != null){
            mFrontCacheBmp.recycle();
            mFrontCacheBmp = null;
        }
        if(mBackCacheBmp != null){
            mBackCacheBmp.recycle();
            mBackCacheBmp = null;
        }
        
        mFrontCacheBmp = Bitmap.createBitmap(mPreviewImageWidth, mPreviewImageHeight, Bitmap.Config.ARGB_8888);
        mBackCacheBmp = Bitmap.createBitmap(mPreviewImageWidth, mPreviewImageHeight, Bitmap.Config.ARGB_8888);
        
        refreshCache();
	}

    protected void refreshCache(){

        if(mWorkspaceContainer == null || !mIsVisiable)
            return;
        
        if(false){
            synchronized (mLock) {
                if(mDrawCacheBmp != null)
                    cacheWorkspaceContainerBmp(mDrawCacheBmp);
            }
        } else {
            if(mDrawCacheBmp == mFrontCacheBmp){
                if(cacheWorkspaceContainerBmp(mBackCacheBmp)){
                    synchronized (mLock) {
                        mDrawCacheBmp = mBackCacheBmp;
                    }
                }
            } else {
                if(cacheWorkspaceContainerBmp(mFrontCacheBmp)){
                    synchronized (mLock) {
                        mDrawCacheBmp = mFrontCacheBmp;
                    }
                }
            }
        }
        
        invalidate();
    }
    
	public void refreshPreviewView(){
		if(mIsVisiable){
		    refreshCache();
		    sHandler.sendMessageDelayed(sHandler.obtainMessage(MSG_REFRESH), mRefreshTimer);
			//postDelayed(mRefreshPreview, mRefreshTimer);
		}
    }
    
//    private final Runnable mRefreshPreview = new Runnable(){
//		@Override
//		public void run() {
//			refreshCache();
//			refreshPreviewView();
//		}
//    };
	
	public Bitmap createPreviewBitmap(View view, float scale){
		return createPreviewBitmap(view, scale, new Canvas());
	}
	public Bitmap createPreviewBitmap(View view, float scale, Canvas canvas){
		if(view == null) return null;
		return createPreviewBitmap(view, scale, 
				(int)(view.getWidth() * scale + 0.5f), 
				(int)(view.getHeight() * scale + 0.5f), canvas);
	}
	public Bitmap createPreviewBitmap(View view, float scale, int width, int height){
		return createPreviewBitmap(view, scale, width, height, new Canvas());
	}
	public Bitmap createPreviewBitmap(View view, float scale, int width, int height, Canvas canvas){
    	if(view == null) return null;
    	//if(height == 0 && width == 0)
    	final Bitmap bitmap = Bitmap.createBitmap((int) width, (int) height,
				Bitmap.Config.ARGB_8888);
    	    	
    	canvas.setBitmap(bitmap);
    	
    	canvas.save();
    	//final Canvas canvas = new Canvas(bitmap);
    	canvas.scale(scale, scale);
    	view.draw(canvas);
    	
    	canvas.restore();
		canvas.setBitmap(null);
		
		return bitmap;
    }
	
	public boolean cacheWorkspaceContainerBmp(Bitmap bmp){
        if(bmp != null && mWorkspaceContainer != null){
            //Bitmap bmp = Bitmap.createBitmap(mPreviewImageWidth, mPreviewImageHeight, Bitmap.Config.ARGB_8888);
            
            bmp.eraseColor(android.graphics.Color.TRANSPARENT);
            
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bmp);

            canvas.save();
            canvas.scale(mPreviewScale, mPreviewScale);
            mWorkspaceContainer.draw(canvas);
            canvas.restore();
            
            canvas.setBitmap(null);
            
            return true;
        }
        
        return false;
    }
	
	public Bitmap cacheView(View view){
//		if(mIsVisiable && mPreviewImageWidth > 0 && mPreviewImageHeight > 0){
//			Bitmap bmp = Bitmap.createBitmap(mPreviewImageWidth, mPreviewImageHeight, Bitmap.Config.ARGB_8888);
//			final Canvas canvas = new Canvas(bmp);
//			//canvas.drawColor(android.graphics.Color.TRANSPARENT);
//			canvas.scale(mPreviewScale, mPreviewScale);
//	    	view.draw(canvas);
//	    	canvas.setBitmap(null);
//	    	
//	    	return bmp;
//		}
		
		return null;
	}

}
