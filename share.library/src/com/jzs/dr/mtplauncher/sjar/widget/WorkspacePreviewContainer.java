package com.jzs.dr.mtplauncher.sjar.widget;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import com.jzs.dr.mtplauncher.sjar.R;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

public class WorkspacePreviewContainer extends ViewGroup {
	private final static String TAG = "WorkspacePreviewContainer";
	private final int mWidthGap;
	private final int mHeightGap;
	private final int mGravity;
//	private final int mMaxCellCountX;
//	private final int mMaxCellCountY;
	private final int[] mMaxCellCount;
	
	public WorkspacePreviewContainer(Context context) {
        this(context, null);
    }

    public WorkspacePreviewContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspacePreviewContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        final Resources res = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WorkspacePreviewContainer, defStyle, 0);

        final int defValue = (int)(res.getDisplayMetrics().density * 12);
        mWidthGap = a.getDimensionPixelSize(R.styleable.WorkspacePreviewContainer_widthGap, defValue);
        mHeightGap = a.getDimensionPixelSize(R.styleable.WorkspacePreviewContainer_heightGap, defValue);
        mGravity = a.getInt(R.styleable.WorkspacePreviewContainer_android_gravity, Gravity.CENTER);
        int cellXId = a.getResourceId(R.styleable.WorkspacePreviewContainer_maxCellX, 0);
        a.recycle();
        
        if(cellXId > 0){
       		mMaxCellCount = res.getIntArray(cellXId);
        } else {
        	mMaxCellCount = new int[]{2, 3, 2};
        }
        
        
        final LayoutTransition transitioner = new LayoutTransition();
        this.setLayoutTransition(transitioner);
        
        Animator defaultChangingAppearingAnim =
                transitioner.getAnimator(LayoutTransition.CHANGE_APPEARING);
        Animator defaultChangingDisappearingAnim =
                transitioner.getAnimator(LayoutTransition.CHANGE_DISAPPEARING);
        
        transitioner.setAnimator(LayoutTransition.APPEARING, null);
        transitioner.setAnimator(LayoutTransition.DISAPPEARING, null);
        
        transitioner.setAnimator(LayoutTransition.CHANGE_APPEARING, defaultChangingAppearingAnim);
        transitioner.setAnimator(LayoutTransition.CHANGE_DISAPPEARING, defaultChangingDisappearingAnim);
    }
    
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        
        int nVisiableChildCount = 0;
        int nPaddingLeft = 0, nStartIndex = 0;
        int horGap = mWidthGap;
        int verGap = mHeightGap;
        final int width = r - l - super.getPaddingLeft() - super.getPaddingRight();
        final int height = b - t - super.getPaddingTop() - super.getPaddingBottom();
        int childCount = getChildCount();
        for (int i = childCount-1; i >= 0; i--) {
            if(getChildAt(i).getVisibility() == View.GONE)
                childCount--;
        }
        
        if(childCount <= 0)
            return;

        int cellCountY = mMaxCellCount.length;
        final int[] childLeft = new int[cellCountY];
        final int[] childTop = new int[cellCountY];
        int[] childCountPerRow = new int[cellCountY];
        int count = childCount;
        for(int i=0; i<cellCountY; i++){
        	if(count >= mMaxCellCount[i]){
        		childCountPerRow[i] = mMaxCellCount[i];
        	} else {
        		childCountPerRow[i] = count;
        	}
        	count -= childCountPerRow[i];
        	
        	if(count <= 0){
	        	cellCountY = i+1;
	    		break;
        	}
        }
        
        View childView = super.getChildAt(0);
        final int childWidth = childView.getMeasuredWidth();
        final int childHeight = childView.getMeasuredHeight();
        
        final int hor = mGravity&Gravity.HORIZONTAL_GRAVITY_MASK;
        final int ver = mGravity&Gravity.VERTICAL_GRAVITY_MASK;
        
//        if (Util.ENABLE_DEBUG) {
//        	Util.Log.d(TAG, "onLayout: cellCountY = " + cellCountY
//        			+"==childCount:"+childCount
//        			+"==childWidth:"+childWidth
//        			+"==childHeight:"+childHeight
//        			+"==width:"+width
//        			+"==height:"+height
//        			+"==t:"+t
//        			+"==l:"+l
//        			+"==hor:"+hor
//        			+"==ver:"+ver
//        			);
//        }

        switch(ver){
        case Gravity.TOP:
        	childTop[0] = t + getPaddingTop();
        	for(int i=1; i<cellCountY; i++){
        		childTop[i] = childTop[i-1] + childHeight + verGap;
        	}
        	break;
        case Gravity.BOTTOM:
        	childTop[cellCountY-1] = b - super.getPaddingBottom() - childHeight;
        	if(cellCountY > 1){
	        	for(int i=cellCountY-2; i>=0; i--){
	        		childTop[i] = childTop[i+1] - childHeight - verGap;
	        	}
        	}
        	break;
    	default:
    		//verGap = height / (cellCountY + 1);
    		childTop[0] = t + getPaddingTop() +(height - cellCountY * childHeight - (cellCountY-1) * verGap) /2;
        	for(int i=1; i<cellCountY; i++){
        		childTop[i] = childTop[i-1] + childHeight + verGap;
        	}
        	
    		break;
        }
        
        switch(hor){
        case Gravity.LEFT:
        	for(int i=0; i<cellCountY; i++){
        		childLeft[0] = l + getPaddingLeft();
        	}
        	break;
        case Gravity.RIGHT:
        	for(int i=0; i<cellCountY; i++){
        		childLeft[i] = r - super.getPaddingRight() - childCountPerRow[i]*childWidth - (childCountPerRow[i]-1) * horGap;
        	}
        	break;
    	default:    		
    		for(int i=0; i<cellCountY; i++){
        		childLeft[i] = l + getPaddingLeft() + (width - childCountPerRow[i]*childWidth - (childCountPerRow[i]-1) * horGap)/2;
        	}
    		break;
        }
        
        if (Util.ENABLE_DEBUG) {
        	for(int i=0; i<cellCountY; i++){
        		Util.Log.d(TAG, "onLayout: ==i:"+i+"==childLeft = " + childLeft[i]
            			+"==childTop:"+childTop[i]
            			);
        	}
        }
        int index = 0;
        for(int y=0; y<cellCountY; y++){
        	int left = childLeft[y];
        	int top = childTop[y];
        	for(int x=0; x<childCountPerRow[y]; x++){
        	    do {
    	        	childView = getChildAt(index++);
    	        	if(childView != null){
    	        	    if(childView.getVisibility() != View.GONE){
        		        	childView.layout(left, top, left + childWidth, top + childHeight);
        		        	left += childWidth + horGap;
        		        	break;
    	        	    }
    	        	}
        	    } while(childView != null);
        	}
        }
    }
    
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);        
        if (heightMode != MeasureSpec.EXACTLY && widthMode != MeasureSpec.EXACTLY) {
            return;
        }
        
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        
        int nChildMeasureWidth = 0;
        int nChildMeasureHeight = 0;
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
        	
        	final View childView = getChildAt(i);
        	if(childView.getVisibility() == View.GONE)
        	    continue;
        	
        	if(nChildMeasureWidth == 0 || nChildMeasureHeight == 0){
	        	final ViewGroup.LayoutParams lp =
	                (ViewGroup.LayoutParams) childView.getLayoutParams();
	
	        	if(lp.width > 0)
	        		nChildMeasureWidth = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
	        	else if(lp.width == ViewGroup.LayoutParams.MATCH_PARENT || lp.width == ViewGroup.LayoutParams.FILL_PARENT)
	        		nChildMeasureWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
	        	else
	        		nChildMeasureWidth = MeasureSpec.makeMeasureSpec(childView.getMeasuredWidth(), MeasureSpec.UNSPECIFIED);
	
	        	if(lp.height > 0)
	        		nChildMeasureHeight = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
	        	else if(lp.height == ViewGroup.LayoutParams.MATCH_PARENT || lp.height == ViewGroup.LayoutParams.FILL_PARENT)
	        		nChildMeasureHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
	        	else
	        		nChildMeasureHeight = MeasureSpec.makeMeasureSpec(childView.getMeasuredHeight(), MeasureSpec.UNSPECIFIED);
//	        	android.util.Log.d("Jzs.con", "onMeasure==i:"+i+"=width:"+childView.getMeasuredWidth()+"==height:"+childView.getMeasuredHeight()
//	        			+"=lpw:"+lp.width+"=lph:"+lp.height);
        	}
        	childView.measure(nChildMeasureWidth, nChildMeasureHeight);
        }
    }
}
