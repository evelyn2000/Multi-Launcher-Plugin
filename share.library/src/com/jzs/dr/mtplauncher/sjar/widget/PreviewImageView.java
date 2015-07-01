package com.jzs.dr.mtplauncher.sjar.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.jzs.dr.mtplauncher.sjar.R;

public class PreviewImageView extends ImageView {
	private Drawable mCheckDrawable;
	
	public PreviewImageView(Context context) {
        this(context, null);
    }

    public PreviewImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreviewImageView, defStyle, 0);
        mCheckDrawable = a.getDrawable(R.styleable.PreviewImageView_checkDrawable);
        a.recycle();
        
        //mCheckDrawable = context.getResources().getDrawable(R.drawable.mode_check_state_icon);
        if(mCheckDrawable != null)
        	mCheckDrawable.setBounds(0, 0, mCheckDrawable.getIntrinsicWidth(), mCheckDrawable.getIntrinsicHeight());
    }
    
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
//        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//        if (widthMode != MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
//            throw new IllegalStateException("PreviewImageView only canmCurScreen run at EXACTLY mode!");
//        }
//    }
    
    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        
        Drawable d = mCheckDrawable;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
    }
    
    @Override 
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (mCheckDrawable == null) {
            return; // couldn't resolve the URI
        }

        canvas.save();
        
        float dx = super.getWidth() - super.getPaddingRight()/2 - mCheckDrawable.getIntrinsicWidth();
        float dy = super.getPaddingTop()/2;
        canvas.translate(dx, dy);
        mCheckDrawable.draw(canvas);
        
        canvas.restore();
    }
}
