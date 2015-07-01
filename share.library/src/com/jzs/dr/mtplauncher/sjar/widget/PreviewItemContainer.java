package com.jzs.dr.mtplauncher.sjar.widget;

import com.jzs.common.launcher.ISharedPrefSettingsManager;
import com.jzs.dr.mtplauncher.sjar.Launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class PreviewItemContainer extends LinearLayout {
	private View mCheckView;
	private ImageView mPreviewView;
	private View.OnClickListener mDefaultScreenClickListener;
	
	public PreviewItemContainer(Context context) {
		super(context, null);
    }

    public PreviewItemContainer(Context context, AttributeSet attrs) {
    	super(context, attrs, 0);
    }

    public PreviewItemContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setDefaultScreenClickListener(View.OnClickListener listener){
    	mDefaultScreenClickListener = listener;
    	if(mCheckView != null){
			mCheckView.setOnClickListener(mDefaultScreenClickListener);
		}
    }
    
    public View getDefaultCheckView(){
    	return mCheckView;
    }
    
    public int getPreviewImageVerticalPadding(){
    	int size = getPaddingTop() + getPaddingBottom();
//    	if(mPreviewView != null){
//    		size = mPreviewView.getPaddingTop() + mPreviewView.getPaddingBottom();
//    	}
    	if(mCheckView != null)
    		size += mCheckView.getMeasuredHeight();
    	return size;
    }
    
    @Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
		mCheckView = findViewWithTag("checkimg");
		mPreviewView = (ImageView)findViewWithTag("previewimg");
		if(mCheckView != null && mDefaultScreenClickListener != null){
			mCheckView.setOnClickListener(mDefaultScreenClickListener);
		}
	}
    
    public void reMeasureLayoutParams(ViewGroup.LayoutParams p){
    	if(p != null){
//    		if(p.height > 0 && mCheckView != null){
//            	p.height += mCheckView.getMeasuredHeight();
//            }
    	}
    }
    
//    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
//    	if(super.checkLayoutParams(p)){
//    		if(mCheckView != null){
//    			p.height += mCheckView.getMeasuredHeight();
//    		}
//    		return true;
//    	}
//        return  false;
//    }
    
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        
//        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
//        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//        if (heightMode != MeasureSpec.EXACTLY && widthMode != MeasureSpec.EXACTLY) {
//            return;
//        }
//        
//        final int width = MeasureSpec.getSize(widthMeasureSpec);
//        final int height = MeasureSpec.getSize(heightMeasureSpec);
//        int newheight = height;
//        
//        if(mCheckView != null){
//        	newheight += mCheckView.getMeasuredHeight();
//        	final ViewGroup.LayoutParams lp =
//	                (ViewGroup.LayoutParams) mCheckView.getLayoutParams();
//        }
//        
//        super.set
//    }

	public void setActivated(boolean activated) {
    	super.setActivated(activated);
    	
    	if(mCheckView != null)
    		mCheckView.setActivated(activated);
    }
	
	public void setImageBitmap(Bitmap bm) {
		//super.setim
		if(mPreviewView != null)
			mPreviewView.setImageBitmap(bm);
	}
	
	public void setImageDrawable(Drawable drawable) {
		if(mPreviewView != null)
			mPreviewView.setImageDrawable(drawable);
	}
}
