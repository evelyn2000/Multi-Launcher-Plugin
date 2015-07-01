package com.jzs.dr.mtplauncher.sjar.widget;

import java.util.HashMap;

import com.jzs.dr.mtplauncher.sjar.ctrl.PageSwitchListener;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.jzs.dr.mtplauncher.sjar.R;

public class PageViewIndicator extends View implements PageSwitchListener{
	private final static String TAG = "PageViewIndicator";
	private int mDirection = 0;

	private Drawable mCurScreenImg;
	private Drawable mDefaultScreenImg;
	private Drawable mMoreScreenImg;
	
	private int mImgGap = 0;

	private int mTextSize = 0;
	private int mScreenPagesCount = 5;
	private int mCurrentScreen = 2;

	private HashMap<Integer, Drawable> mCustomScreenIcon = new HashMap<Integer, Drawable>();
	
	public PageViewIndicator(Context context) {
        this(context, null);
    }

    public PageViewIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageViewIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        //mBitmapForegroud = ((BitmapDrawable) getResources().getDrawable(R.drawable.hud_pageturn_foreground)).getBitmap();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PageViewIndicator, defStyle, 0);
        mDirection = a.getInt(R.styleable.PageViewIndicator_direction, Util.PORTRAIT);
        
        mCurScreenImg = a.getDrawable(R.styleable.PageViewIndicator_curScreenImage);       
        mDefaultScreenImg = a.getDrawable(R.styleable.PageViewIndicator_defaultScreenImage);        
        mMoreScreenImg = a.getDrawable(R.styleable.PageViewIndicator_moreScreenImage);
        mImgGap = a.getDimensionPixelSize(R.styleable.PageViewIndicator_imageGap, 0);
        a.recycle();
        
    }
    
//    public void initial(Context context, PageViewIndicatorLister lister){
//    	mCurrentScreen = lister.getCurrentPage();
//    	mScreenPagesCount = lister.getPageCount();
//    	
//    	lister.setPageViewIndicatorCallback(this);
//    }
    
    public void initial(int currPage, int pageCount){
		
    	mScreenPagesCount = pageCount;
    	mCurrentScreen = currPage;
	}
    
    public void onPageSwitch(View newPage, int newPageIndex){
    	if(mCurrentScreen != newPageIndex && newPageIndex < mScreenPagesCount)
		{
			mCurrentScreen = newPageIndex;
			super.invalidate();
		}
    }
    
    public void onScrollChangedCallback(int l, int t, int oldl, int oldt){
    	//postInvalidate();
    	
    }
    
//	public void onChangeToScreen(int whichScreen){
//		if(mCurrentScreen != whichScreen)
//		{
//			mCurrentScreen = whichScreen;
//			super.invalidate();
//		}
//	}
	
	public boolean onPageCountChanged(int nNewCount){
//		Util.Log.i(TAG, "onPageCountChanged()==nNewCount:"+nNewCount
//				+", cur:"+mScreenPagesCount);
		if(nNewCount != mScreenPagesCount){
			mScreenPagesCount = nNewCount;
			
			if(mCurrentScreen >= mScreenPagesCount){
				mCurrentScreen = mScreenPagesCount - 1;
			}
			
			super.invalidate();
			return true;
		}
		return false;
	}
	
	public void setCustomPageIndicatorIcon(int index, Drawable dr){
		if(dr != null)
			mCustomScreenIcon.put(index, dr);
		else
			mCustomScreenIcon.remove(index);
	}
	
	@Override
    protected void onDraw(Canvas canvas) {
		
		Drawable bg = getBackground();
		if(bg != null)
			bg.draw(canvas);
		else
			canvas.drawColor(Color.TRANSPARENT);
		
		if(mCurScreenImg != null && mDefaultScreenImg != null && mScreenPagesCount > 0){
			if(mDirection == Util.PORTRAIT)//
	        {
	        	drawHorizontal(canvas, mScreenPagesCount, mCurrentScreen);
	        }
	        else
	        {
	        	drawVertical(canvas, mScreenPagesCount, mCurrentScreen);
	        }
		}
		
        super.onDraw(canvas);
    }
	
	protected int drawIcon(Canvas canvas, Drawable icon, int iconSize, int left, int count){
		int top;// = (getHeight() - mDefaultScreenImg.getHeight())/2;
		int height;// = mDefaultScreenImg.getIntrinsicHeight();
		height = icon.getIntrinsicHeight();
		top = (getHeight() - height)/2;
		for(int i=0; i<count; i++){
			icon.setBounds(left, top, left + iconSize, top + height);
			icon.draw(canvas);
			left += iconSize + mImgGap;
		}
		
		return left;
	}
	
	protected void drawHorizontal(Canvas canvas, int screenCount, int curScreenIndex){
		//int layout = getGravity();
		if(screenCount <= 0)
			return;
		
		int fullwidth = getWidth();
	//	int nTotalWidth = (nScreenCount - 1) * mDefaultScreenImg.getWidth() + (nScreenCount + 1) * mImgPadding + mCurScreenImg.getWidth();
		int customIconCount = mCustomScreenIcon.size();
		final Drawable lastIcon = screenCount > 1 ? mCustomScreenIcon.get(CUSTOM_INDICATOR_LAST) : null;
		final Drawable firstIcon = mCustomScreenIcon.get(CUSTOM_INDICATOR_FIRST);
		
		final int curImgSize = mCurScreenImg.getIntrinsicWidth();
		final int defaultImgSize = screenCount > 1 ? mDefaultScreenImg.getIntrinsicWidth() : 0;
		final int moreImgSize = mMoreScreenImg != null ? mMoreScreenImg.getIntrinsicWidth() : 0;
		
		final int firstImgSize = firstIcon != null ? firstIcon.getIntrinsicWidth() : 0;
		final int lastImgSize = lastIcon != null ? lastIcon.getIntrinsicWidth() : 0;
		
		int nTotalWidth = (screenCount - 1) * (mImgGap+defaultImgSize) + curImgSize;
		
		if(lastImgSize > 0){
			if(curScreenIndex == (screenCount-1))
				nTotalWidth += lastImgSize - curImgSize;
			else
				nTotalWidth += lastImgSize - defaultImgSize;
		}
		
		if(firstImgSize > 0){
			if(curScreenIndex == 0)
				nTotalWidth += firstImgSize - curImgSize;
			else
				nTotalWidth += firstImgSize - defaultImgSize;
		}
		
		//nTotalWidth += (nScreenCount - 1) * mDefaultScreenImg.getWidth();
		
		//QsLog.LogD("drawHorizontal()==w:"+width+"=nTotalWidth:"+nTotalWidth);
		boolean bShowMore = false;
		if(nTotalWidth > fullwidth)
			bShowMore = true;
		
		int left = (fullwidth - nTotalWidth)/2;
		int i=0;
		
		if(true){
			if(firstImgSize > 0){
				left = drawIcon(canvas, firstIcon, firstImgSize, left, 1);
				i++;
			}
			
			int count = curScreenIndex - i;
			if(count > 0){
				left = drawIcon(canvas, mDefaultScreenImg, defaultImgSize, left, count);
				i += count;
			}
			
			if(firstImgSize == 0 || (curScreenIndex > 0 && (lastImgSize == 0 || curScreenIndex < (screenCount-1)))){
				left = drawIcon(canvas, mCurScreenImg, curImgSize, left, 1);
				i++;
			}
			
			int lastScreenCount = screenCount - i;
			if(lastScreenCount > 0){
				if(lastImgSize > 0){
					left = drawIcon(canvas, mDefaultScreenImg, defaultImgSize, left, lastScreenCount-1);
					left = drawIcon(canvas, lastIcon, lastImgSize, left, 1);
				} else {
					left = drawIcon(canvas, mDefaultScreenImg, defaultImgSize, left, lastScreenCount);
				}
			}
			
		} else {
			int top;// = (getHeight() - mDefaultScreenImg.getHeight())/2;
			int height;// = mDefaultScreenImg.getIntrinsicHeight();
			if(firstImgSize > 0){
				left = drawIcon(canvas, firstIcon, firstImgSize, left, 1);
	//			height = firstIcon.getIntrinsicHeight();
	//			top = (getHeight() - height)/2;
	//			firstIcon.setBounds(left, top, left + firstImgSize, top + height);
	//			firstIcon.draw(canvas);
	//			left += firstImgSize + mImgGap;
				i++;
			}
			
			height = mDefaultScreenImg.getIntrinsicHeight();
			top = (getHeight() - height)/2;
			while(i<curScreenIndex){
				mDefaultScreenImg.setBounds(left, top, left + defaultImgSize, top + height);
				mDefaultScreenImg.draw(canvas);
				left += defaultImgSize + mImgGap;
				i++;
			}
			
			if(firstImgSize == 0 || (curScreenIndex > 0 && (lastImgSize == 0 || curScreenIndex < (screenCount-1)))){
				height = mCurScreenImg.getIntrinsicHeight();
				top = (getHeight() - height)/2;
				mCurScreenImg.setBounds(left, top, left + curImgSize, top + height);
				mCurScreenImg.draw(canvas);
				left += curImgSize + mImgGap;
				i++;
			}
			
			int lastScreenIndex = lastImgSize > 0 ? (screenCount-2) : (screenCount-1);
	
			height = mDefaultScreenImg.getIntrinsicHeight();
			while(i<=lastScreenIndex){
				top = (getHeight() - height)/2;
				mDefaultScreenImg.setBounds(left, top, left + defaultImgSize, top + height);
				mDefaultScreenImg.draw(canvas);
				left += defaultImgSize + mImgGap;
				i++;
			}
			
			if(lastImgSize > 0){
				height = lastIcon.getIntrinsicHeight();
				top = (getHeight() - height)/2;
				lastIcon.setBounds(left, top, left + lastImgSize, top + height);
				lastIcon.draw(canvas);
				left += lastImgSize + mImgGap;
			}
		}
	}
	
	protected void drawVertical(Canvas canvas, int screenCount, int curScreenIndex){
		if(screenCount <= 0)
			return;
//		int height = getHeight();
//		
//		
//		
//		int nTotalHeight = (nScreenCount - 1) * mDefaultScreenImg.getWidth() + (nScreenCount + 1) * mImgPadding + mCurScreenImg.getWidth();
//		//QsLog.LogD("drawHorizontal()==w:"+width+"=nTotalWidth:"+nTotalWidth);
//		boolean bShowMore = false;
//		if(nTotalHeight > height)
//			bShowMore = true;
//		
//		int nTop = (height - nTotalHeight)/2;
//		int nLeft  = (getWidth() - mDefaultScreenImg.getWidth())/2;
//		for(int i=0; i<mCurrentScreen; i++){
//			canvas.drawBitmap(mDefaultScreenImg,nLeft,nTop, null);
//			nTop += mDefaultScreenImg.getHeight() + mImgPadding;
//		}
//        final Resources resources = getResources();
//        //final float gap_x = resources.getDimension(R.dimen.screenindeictor_Vertical_gap_x);
//        //final float gap_y = resources.getDimension(R.dimen.screenindeictor_Vertical_gap_y);
//        
//		int nCurLeft = (getWidth() - mCurScreenImg.getWidth())/2;
//		canvas.drawBitmap(mCurScreenImg,nCurLeft , nTop, null);
//		if(mTextPaint != null){
//			Rect bounds = new Rect();
//			String str = String.valueOf(mCurrentScreen+1);
//			mTextPaint.getTextBounds(str, 0, str.length(), bounds);
//			int y = nTop + (mCurScreenImg.getHeight() - bounds.height() - mBgPadding.left - mBgPadding.right)/2;// + (int)gap_y;
//			int x = nCurLeft + (mCurScreenImg.getWidth() + bounds.width())/2;// - (int)gap_x ;
//			//QsLog.LogD("drawHorizontal(1)==str:"+str+"=nLeft:"+nLeft+"==nCurTop:"+nCurTop+"==x:"+x+"==y:"+y+"==th:"+bounds.height()+"=ih:"+mCurScreenImg.getHeight());
//			canvas.drawText(str, x, y, mTextPaint);
//		}
//		nTop += mCurScreenImg.getHeight() + mImgPadding;
//		
//		for(int i=mCurrentScreen+1; i<nScreenCount; i++){
//			canvas.drawBitmap(mDefaultScreenImg, nLeft, nTop, null);
//			nTop += mDefaultScreenImg.getWidth() + mImgPadding;
//		}
	}
}
