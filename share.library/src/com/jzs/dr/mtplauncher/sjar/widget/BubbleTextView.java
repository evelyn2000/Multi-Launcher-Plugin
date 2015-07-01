/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jzs.dr.mtplauncher.sjar.widget;

import com.jzs.common.launcher.IIconCache;
import com.jzs.dr.mtplauncher.sjar.ctrl.CheckLongPressHelper;
import com.jzs.dr.mtplauncher.sjar.ctrl.HolographicOutlineHelper;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.common.launcher.model.ShortcutInfo;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;


import com.jzs.dr.mtplauncher.sjar.R;
/**
 * TextView that draws a bubble behind the text. We cannot use a LineBackgroundSpan
 * because we want to make the bubble taller than the text and TextView's clip is
 * too aggressive.
 */
public class BubbleTextView extends TextView implements IShortcutView {
    private final static String TAG = "BubbleTextView";
	public static final float CORNER_RADIUS = 4.0f;
    public static final float SHADOW_LARGE_RADIUS = 4.0f;
    public static final float SHADOW_SMALL_RADIUS = 1.75f;
    public static final float SHADOW_Y_OFFSET = 2.0f;
    public static final int SHADOW_LARGE_COLOUR = 0xDD000000;
    public static final int SHADOW_SMALL_COLOUR = 0xCC000000;
    public static final float PADDING_H = 8.0f;
    public static final float PADDING_V = 3.0f;

    protected int mPrevAlpha = -1;

    protected final HolographicOutlineHelper mOutlineHelper = new HolographicOutlineHelper();
    protected final Canvas mTempCanvas = new Canvas();
    protected final Rect mTempRect = new Rect();
    protected boolean mDidInvalidateForPressedState;
    protected Bitmap mPressedOrFocusedBackground;
    protected int mFocusedOutlineColor;
    protected int mFocusedGlowColor;
    protected int mPressedOutlineColor;
    protected int mPressedGlowColor;

    protected boolean mBackgroundSizeChanged;
    protected Drawable mBackground;

    protected boolean mStayPressed;
    protected CheckLongPressHelper mLongPressHelper;
    protected boolean mIsShowTitle = true;
    private final boolean mIsSupportIconAutoReSize;
    private int mOrgIconSize;
    private boolean mEnableAutoReSize = true;

    public BubbleTextView(Context context) {
        this(context, null);
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mIsShowTitle = true;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BubbleTextView, defStyle, 0);
        mIsSupportIconAutoReSize = a.getBoolean(R.styleable.BubbleTextView_supportIconAutoReSize, false);
        a.recycle();
        
        mLongPressHelper = new CheckLongPressHelper(this);
        mBackground = getBackground();

        final Resources res = getContext().getResources();
        /// M: modified for theme feature, get the corresponding color for
        /// different themes.
        mPressedGlowColor = res.getColor(android.R.color.holo_blue_light);
        mFocusedOutlineColor = mPressedGlowColor;
        mFocusedGlowColor = mPressedGlowColor;
        mPressedOutlineColor = mPressedGlowColor;

        setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, SHADOW_LARGE_COLOUR);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, IIconCache iconCache) {
        Bitmap b = info.getIcon(iconCache);
        
        if(mIsShowTitle)
            setText(info.title);
        
        if(b != null){
            setCompoundDrawablesWithIntrinsicBounds(null,
                    new FastBitmapDrawable(b),
                    null, null);
        }

        setTag(info);
    }
    
    @Override
    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top,
            Drawable right, Drawable bottom){

        if(isSupportIconAutoReSize() && top != null){
            if(Util.DEBUG_LAYOUT){
                android.util.Log.v(TAG, "setCompoundDrawablesWithIntrinsicBounds(1)==w:"+super.getWidth()
                    +"==h:"+super.getHeight()
                    +"==mOrgIconSize:"+mOrgIconSize
                    +"==topsize:"+top.getIntrinsicHeight()
                    +"==title:"+getText());
            }
            
            mOrgIconSize = top.getIntrinsicHeight();
            if(getWidth() > 0 && getHeight() > 0){
                
                final int txtHeight = getLineHeight() * Math.max(getLineCount(), 1);
                int width = getWidth() - getPaddingLeft() - getPaddingRight();
                int height = getHeight() - getPaddingTop() - getPaddingBottom() - getCompoundDrawablePadding() - txtHeight;
                int size = Math.min(width, height);
                if(size > 0 && size < mOrgIconSize){
                    autoResizeDrawableIcon(top, size, size);
                }
            }
        }
        
        super.setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // TODO Auto-generated method stub
        super.onSizeChanged(w, h, oldw, oldh);
        if(Util.DEBUG_LAYOUT){
            android.util.Log.i(TAG, "onSizeChanged(0)=="
                    +"=w:"+w
                    +"=h:"+h
                    +"=oldw:"+oldw
                    +"=oldh:"+oldh
                    +"=mEnableAutoReSize:"+mEnableAutoReSize
                    +"=isSupportIconAutoReSize:"+isSupportIconAutoReSize()
                    +"=orgsize:"+mOrgIconSize);
        }
        
        if(isSupportIconAutoReSize() && mEnableAutoReSize && mOrgIconSize> 0){
            int txtHeight = getLineHeight() * Math.max(getLineCount(), 1);
            int width = w - getPaddingLeft() - getPaddingRight();// - getCompoundDrawablePadding();
            int height = h - getPaddingTop() - getPaddingBottom() - getCompoundDrawablePadding() - txtHeight;
            
            int size = Math.min(mOrgIconSize, Math.min(width, height));
            if(Util.DEBUG_LAYOUT){
                android.util.Log.i("QsLog", "onSizeChanged(1)=="
                        +"=txtHeight:"+txtHeight
                        +"=exttop:"+getExtendedPaddingTop()
                        +"=top:"+getPaddingTop()
                        +"=bom:"+getPaddingBottom()
                        +"=size:"+size
                        +"=title:"+getText());
            }

            width = oldw - getPaddingLeft() - getPaddingRight();// - getCompoundDrawablePadding();
            height = oldh - getPaddingTop() - getPaddingBottom() - getCompoundDrawablePadding() - txtHeight;
            int oldSize = Math.min(width, height);

            if((size != oldSize) 
                    && ((size > 0 && size < mOrgIconSize) 
                        || (oldSize > 0 && oldSize < mOrgIconSize))){
                
//                android.util.Log.i("QsLog", "onSizeChanged(1)=="
//                        +"==w:"+w
//                        +"==h:"+h
//                        +"==oldw:"+oldw
//                        +"==oldh:"+oldh
//                        +"==size:"+size
//                        +"==oldSize:"+oldSize
//                        +"==orgsize:"+mOrgIconSize
//                        +"==title:"+getText());
                
                autoResizeDrawableIcon(size, size);
            }
        } /*else {
            android.util.Log.i("QsLog", "onSizeChanged(2)=="
                    +"==w:"+w
                    +"==h:"+h
                    +"==oldw:"+oldw
                    +"==oldh:"+oldh
                    +"==orgsize:"+mOrgIconSize
                    +"==title:"+getText());
        }*/
    }

    @Override
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (getLeft() != left || getRight() != right || getTop() != top || getBottom() != bottom) {
            mBackgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mBackground || super.verifyDrawable(who);
    }

    @Override
    public void setTag(Object tag) {
        if (tag != null && (getContext() instanceof com.jzs.common.launcher.LauncherHelper)) {
        	((com.jzs.common.launcher.LauncherHelper)getContext()).getModel().checkItemInfo((ItemInfo) tag);
        }
        super.setTag(tag);
    }

    @Override
    protected void drawableStateChanged() {
        if (isPressed()) {
            // In this case, we have already created the pressed outline on ACTION_DOWN,
            // so we just need to do an invalidate to trigger draw
            if (!mDidInvalidateForPressedState) {
                setCellLayoutPressedOrFocusedIcon();
            }
        } else {
            // Otherwise, either clear the pressed/focused background, or create a background
            // for the focused state
            final boolean backgroundEmptyBefore = mPressedOrFocusedBackground == null;
            if (!mStayPressed) {
                mPressedOrFocusedBackground = null;
            }
            if (isFocused()) {
                if (getLayout() == null) {
                    // In some cases, we get focus before we have been layed out. Set the
                    // background to null so that it will get created when the view is drawn.
                    mPressedOrFocusedBackground = null;
                } else {
                    mPressedOrFocusedBackground = createGlowingOutline(
                            mTempCanvas, mFocusedGlowColor, mFocusedOutlineColor);
                }
                mStayPressed = false;
                setCellLayoutPressedOrFocusedIcon();
            }
            final boolean backgroundEmptyNow = mPressedOrFocusedBackground == null;
            if (!backgroundEmptyBefore && backgroundEmptyNow) {
                setCellLayoutPressedOrFocusedIcon();
            }
        }

        Drawable d = mBackground;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
        super.drawableStateChanged();
    }

    /**
     * Draw this BubbleTextView into the given Canvas.
     *
     * @param destCanvas the canvas to draw on
     * @param padding the horizontal and vertical padding to use when drawing
     */
    protected void drawWithPadding(Canvas destCanvas, int padding) {
        final Rect clipRect = mTempRect;
        getDrawingRect(clipRect);

        // adjust the clip rect so that we don't include the text label
        clipRect.bottom =
            getExtendedPaddingTop() - (int) BubbleTextView.PADDING_V + getLayout().getLineTop(0);

        // Draw the View into the bitmap.
        // The translate of scrollX and scrollY is necessary when drawing TextViews, because
        // they set scrollX and scrollY to large values to achieve centered text
        destCanvas.save();
        destCanvas.scale(getScaleX(), getScaleY(),
                (getWidth() + padding) / 2, (getHeight() + padding) / 2);
        destCanvas.translate(-getScrollX() + padding / 2, -getScrollY() + padding / 2);
        destCanvas.clipRect(clipRect, Op.REPLACE);
        draw(destCanvas);
        destCanvas.restore();
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    protected Bitmap createGlowingOutline(Canvas canvas, int outlineColor, int glowColor) {
        final int padding = HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS;
        final Bitmap b = Bitmap.createBitmap(
                getWidth() + padding, getHeight() + padding, Bitmap.Config.ARGB_8888);

        canvas.setBitmap(b);
        drawWithPadding(canvas, padding);
        mOutlineHelper.applyExtraThickExpensiveOutlineWithBlur(b, canvas, glowColor, outlineColor);
        canvas.setBitmap(null);

        return b;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP
        boolean result = super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // So that the pressed outline is visible immediately when isPressed() is true,
                // we pre-create it on ACTION_DOWN (it takes a small but perceptible amount of time
                // to create it)
                if (mPressedOrFocusedBackground == null) {
                    mPressedOrFocusedBackground = createGlowingOutline(
                            mTempCanvas, mPressedGlowColor, mPressedOutlineColor);
                }
                // Invalidate so the pressed state is visible, or set a flag so we know that we
                // have to call invalidate as soon as the state is "pressed"
                if (isPressed()) {
                    mDidInvalidateForPressedState = true;
                    setCellLayoutPressedOrFocusedIcon();
                } else {
                    mDidInvalidateForPressedState = false;
                }

                mLongPressHelper.postCheckForLongPress();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // If we've touched down and up on an item, and it's still not "pressed", then
                // destroy the pressed outline
                if (!isPressed()) {
                    mPressedOrFocusedBackground = null;
                }

                mLongPressHelper.cancelLongPress();
                break;
        }
        return result;
    }

    public void setStayPressed(boolean stayPressed) {
        mStayPressed = stayPressed;
        if (!stayPressed) {
            mPressedOrFocusedBackground = null;
        }
        setCellLayoutPressedOrFocusedIcon();
    }

    public void setCellLayoutPressedOrFocusedIcon() {
        /// M: modified for Unread feature, the icon is placed in a RelativeLayout,
        /// so first getParent() to get the RelativeLayout, then ShortcutAndWidgetContainer and CellLayout.
        final View shortcutView = (View) getParent();
        if (shortcutView != null) {
            final View container = (View) shortcutView.getParent();
            if (container != null && container instanceof ShortcutAndWidgetContainer) {
                CellLayout layout = (CellLayout) container.getParent();
                layout.setPressedOrFocusedIcon((mPressedOrFocusedBackground != null) ? this : null);
            }
        }
    }

    public void clearPressedOrFocusedBackground() {
        mPressedOrFocusedBackground = null;
        setCellLayoutPressedOrFocusedIcon();
    }

    public Bitmap getPressedOrFocusedBackground() {
        return mPressedOrFocusedBackground;
    }

    public int getPressedOrFocusedBackgroundPadding() {
        return HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS / 2;
    }

    @Override
    public void draw(Canvas canvas) {
        final Drawable background = mBackground;
        if (Util.DEBUG_DRAW) {
            Util.Log.v(TAG, "dispatchDraw(start) = background:"+background
                    +"="+this);
        }
        if (background != null) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();

            if (mBackgroundSizeChanged) {
                background.setBounds(0, 0,  getRight() - getLeft(), getBottom() - getTop());
                mBackgroundSizeChanged = false;
            }

            if ((scrollX | scrollY) == 0) {
                background.draw(canvas);
            } else {
                canvas.translate(scrollX, scrollY);
                background.draw(canvas);
                canvas.translate(-scrollX, -scrollY);
            }
        }

        // If text is transparent, don't draw any shadow
        if (getCurrentTextColor() == 0/*getResources().getColor(android.R.color.transparent)*/) {
            getPaint().clearShadowLayer();
            super.draw(canvas);
            return;
        }

        // We enhance the shadow by drawing the shadow twice
        getPaint().setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, SHADOW_LARGE_COLOUR);
        super.draw(canvas);
        canvas.save(Canvas.CLIP_SAVE_FLAG);
        canvas.clipRect(getScrollX(), getScrollY() + getExtendedPaddingTop(),
                getScrollX() + getWidth(),
                getScrollY() + getHeight(), Region.Op.INTERSECT);
        getPaint().setShadowLayer(SHADOW_SMALL_RADIUS, 0.0f, 0.0f, SHADOW_SMALL_COLOUR);
        super.draw(canvas);
        canvas.restore();
        
        if (Util.DEBUG_DRAW) {
            Util.Log.v(TAG, "dispatchDraw(end) = "
                    +"="+this);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //mIsAttached = true;
        if (mBackground != null) {
            mBackground.setCallback(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        //mIsAttached = false;
        super.onDetachedFromWindow();
        if (mBackground != null) {
            mBackground.setCallback(null);
        }
    }

    @Override
    protected boolean onSetAlpha(int alpha) {
        if (mPrevAlpha != alpha) {
            mPrevAlpha = alpha;
            super.onSetAlpha(alpha);
        }
        return true;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }
    
    @Override
    public void setText(CharSequence text, BufferType type){
        if(!mIsShowTitle && !TextUtils.isEmpty(text) && mLongPressHelper != null){
            
            if(super.getTag() instanceof ItemInfo){
                ItemInfo info = (ItemInfo)super.getTag();
                info.title = text;
            }
            return;
        }
        super.setText(text, type);
    }
//    public CharSequence getText() {
//        if(!mIsShowTitle){
//            ShortcutInfo info = (ShortcutInfo)super.getTag();
//            if(info != null)
//                return info.title;
//        }
//        return super.getText();
//    }
    
    public void setViewLabelVisible(boolean isShowTitle){
        if(mIsShowTitle == isShowTitle)
            return;
        
        mIsShowTitle = isShowTitle;
        if(isShowTitle){
            if(super.getTag() instanceof ItemInfo){
                ItemInfo info = (ItemInfo)super.getTag();
                setText(info.title);
            }
        } else {
            setText(null);
        }
    }
    
    public boolean getViewLabelVisible(){
        return mIsShowTitle;
    }
    
    public boolean isSupportIconAutoReSize(){
        return mIsSupportIconAutoReSize;
    }
    
    public void setEnableIconAutoReSize(boolean enable){
        mEnableAutoReSize = enable;
    }
    
    protected void autoResizeDrawableIcon(final int with, final int height){
        final Drawable[] drawables = getCompoundDrawables();
        if(drawables != null){
            //super.post(action)
            for(int i=0; i < drawables.length; i++){
                if(drawables[i] != null){
//                    android.util.Log.d("QsLog",
//                            "autoResizeDrawableIcon=11==w:" + drawables[i].getIntrinsicWidth()
//                                    + "==h:" + drawables[i].getIntrinsicHeight()
//                                    + "==with:" + with
//                                    + "==height:" + height);

                    if (autoResizeDrawableIcon(drawables[i], with, height)) {
                        super.setCompoundDrawablesWithIntrinsicBounds(drawables[0],
                                drawables[1], drawables[2], drawables[3]);

//                        android.util.Log.v("QsLog",
//                                "autoResizeDrawableIcon=22==w:" + drawables[i].getIntrinsicWidth()
//                                        + "==h:" + drawables[i].getIntrinsicHeight()
//                                        + "==with:" + with
//                                        + "==height:" + height);
                    }
                    break;
                }
            }
        }
    }
    
    private boolean autoResizeDrawableIcon(Drawable drawable, final int with, final int height){
        if (drawable != null && Util.scaleBitmapDrawable(drawable, with, height)) {
            return true;
        }
        return false;
    }
}
