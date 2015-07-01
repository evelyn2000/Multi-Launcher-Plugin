package com.jzs.dr.mtplauncher.sjar.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Stack;

import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.LauncherHelper;
import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherAnimUtils;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherAppWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherModel;
import com.jzs.dr.mtplauncher.sjar.model.PendingAddWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.widget.FolderIcon.FolderRingAnimator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LayoutAnimationController;

import com.jzs.dr.mtplauncher.sjar.R;

public class CellLayout extends ViewGroup {
	protected final static String TAG = "CellLayout";
	protected Launcher mLauncher;
	protected int mCellWidth;
	protected int mCellHeight;
	protected int mOriginalCellWidth;
	protected int mOriginalCellHeight;

	protected int mCountX;
	protected int mCountY;
	
	protected int mOriginalWidthGap;
	protected int mOriginalHeightGap;
	protected int mWidthGap;
	protected int mHeightGap;
	protected int mMaxGap;
	
	protected final Rect mRect = new Rect();
	protected final CellInfo mCellInfo = new CellInfo();
    
	protected final int[] mTmpXY = new int[2];
	protected final int[] mTmpPoint = new int[2];
	protected int[] mTempLocation = new int[2];
    
	protected boolean[][] mOccupied;
	protected boolean[][] mTmpOccupied;
	protected boolean mLastDownOnOccupiedCell = false;
	
	protected boolean mIsDragOverlapping;
	
	protected OnTouchListener mInterceptTouchListener;
	protected ArrayList<View> mIntersectingViews = new ArrayList<View>();
	
	protected ShortcutAndWidgetContainer mShortcutsAndWidgets;
	protected boolean mIsHotseat = false;
	protected float mHotseatScale = 1f;
    
    public static final int MODE_DRAG_OVER = 0;
    public static final int MODE_ON_DROP = 1;
    public static final int MODE_ON_DROP_EXTERNAL = 2;
    public static final int MODE_ACCEPT_DROP = 3;
    
    public static final int LANDSCAPE = Util.LANDSCAPE;
    public static final int PORTRAIT = Util.PORTRAIT;
    
    protected final ArrayList<FolderRingAnimator> mFolderOuterRings = new ArrayList<FolderRingAnimator>();
    protected int[] mFolderLeaveBehindCell = {-1, -1};
    
    protected BubbleTextView mPressedOrFocusedIcon;
	
    protected final static PorterDuffXfermode sAddBlendMode =
            new PorterDuffXfermode(PorterDuff.Mode.ADD);
    protected final static Paint sPaint = new Paint();
    
    protected DropTarget.DragEnforcer mDragEnforcer;
    
    public static final int SCROLL_TO_UNKOWN = 0;
    public static final int SCROLL_TO_LEFT = -1;
    public static final int SCROLL_TO_RIGHT = 1;
    protected int mForegroundAlpha = 0;
    protected float mBackgroundAlpha;
    protected float mBackgroundAlphaMultiplier = 1.0f;

    protected Drawable mNormalBackground;
    protected Drawable mActiveGlowBackground;
    protected Drawable mOverScrollForegroundDrawable;
    protected Drawable mOverScrollLeft;
    protected Drawable mOverScrollRight;
    protected Rect mBackgroundRect;
    protected Rect mForegroundRect;
    protected int mForegroundPadding;
    protected boolean mScrollingTransformsDirty = false;
    
    protected boolean mDragging = false;
    
    public final static boolean HOR_GAP_NEW_STYLE = false;
    
	public CellLayout(Context context) {
        this(context, null);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
     // A ViewGroup usually does not draw, but CellLayout needs to draw a rectangle to show
        // the user where a dragged item will land when dropped.
        setWillNotDraw(false);
        if(context instanceof Launcher){
        	mLauncher = (Launcher) context;
        	mDragEnforcer = new DropTarget.DragEnforcer(mLauncher);
//        	mCountX = mLauncher.getWorkspaceCellCountX();
//            mCountY = mLauncher.getWorkspaceCellCountY();
        } /*else {
        	mCountX = 4;
            mCountY = 4;
        }*/
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);
        
        mCountX = a.getInteger(R.styleable.Hotseat_cellCountX, mLauncher.getWorkspaceCellCountX());
        mCountY = a.getInteger(R.styleable.Hotseat_cellCountY, mLauncher.getWorkspaceCellCountY());

        mCellWidth = mOriginalCellWidth = a.getDimensionPixelSize(R.styleable.CellLayout_cellWidth, -1);
        mCellHeight = mOriginalCellHeight = a.getDimensionPixelSize(R.styleable.CellLayout_cellHeight, -1);
        mWidthGap = mOriginalWidthGap = a.getDimensionPixelSize(R.styleable.CellLayout_widthGap, 0);
        mHeightGap = mOriginalHeightGap = a.getDimensionPixelSize(R.styleable.CellLayout_heightGap, 0);
        mMaxGap = a.getDimensionPixelSize(R.styleable.CellLayout_maxGap, 0);
        
        mNormalBackground = a.getDrawable(R.styleable.CellLayout_normalBackground);
        //(R.drawable.homescreen_blue_normal_holo);
        mActiveGlowBackground = a.getDrawable(R.styleable.CellLayout_activeGlowBackground);
        //(R.drawable.homescreen_blue_strong_holo);

        mOverScrollLeft = a.getDrawable(R.styleable.CellLayout_overScrollLeft);
        //(R.drawable.overscroll_glow_left);
        mOverScrollRight = a.getDrawable(R.styleable.CellLayout_overScrollRight);
        //(R.drawable.overscroll_glow_right);
        mForegroundPadding = a.getDimensionPixelSize(R.styleable.CellLayout_foregroundPadding, 0);
        //(R.dimen.workspace_overscroll_drawable_padding);
        
        a.recycle();
        
        if(mNormalBackground != null)
        	mNormalBackground.setFilterBitmap(true);
        if(mActiveGlowBackground != null)
        	mActiveGlowBackground.setFilterBitmap(true);
        
        setAlwaysDrawnWithCacheEnabled(false);
        
        if(mCountX > 0 && mCountY > 0){
	        mOccupied = new boolean[mCountX][mCountY];
	        mTmpOccupied = new boolean[mCountX][mCountY];
        }
        mPreviousReorderDirection[0] = INVALID_DIRECTION;
        mPreviousReorderDirection[1] = INVALID_DIRECTION;
        
        mBackgroundRect = new Rect();
        mForegroundRect = new Rect();

        mShortcutsAndWidgets = new ShortcutAndWidgetContainer(context);
        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap);
        addView(mShortcutsAndWidgets);
    }
    
    public void setLauncher(Launcher launcher){
    	mLauncher = launcher;
    	mDragEnforcer = new DropTarget.DragEnforcer(launcher);
    	

//    	int newCountX = mLauncher.getWorkspaceCellCountX();
//    	int newCountY = mLauncher.getWorkspaceCellCountY();
    	
    	setGridSize(mLauncher.getWorkspaceCellCountX(), mLauncher.getWorkspaceCellCountY());
//        if(newCountX != mCountX || newCountY != mCountY) {
//        	mCountX = newCountX;
//            mCountY = newCountY;
//            
//            mOccupied = new boolean[mCountX][mCountY];
//            mTmpOccupied = new boolean[mCountX][mCountY];
//            mTempRectStack.clear();
//        }
    }
    
    public void setIsDragOverlapping(boolean isDragOverlapping) {
        if (mIsDragOverlapping != isDragOverlapping) {
            mIsDragOverlapping = isDragOverlapping;
            invalidate();
        }
    }

    public boolean getIsDragOverlapping() {
        return mIsDragOverlapping;
    }
    
    public void setOverScrollAmount(float r, boolean left) {
        if (left && mOverScrollForegroundDrawable != mOverScrollLeft) {
            mOverScrollForegroundDrawable = mOverScrollLeft;
        } else if (!left && mOverScrollForegroundDrawable != mOverScrollRight) {
            mOverScrollForegroundDrawable = mOverScrollRight;
        }

        mForegroundAlpha = (int) Math.round((r * 255));
        if(mOverScrollForegroundDrawable != null){
	        mOverScrollForegroundDrawable.setAlpha(mForegroundAlpha);
	        invalidate();
        }
    }
    
    public void setOverscrollTransformsDirty(boolean dirty) {
        mScrollingTransformsDirty = dirty;
    }

    public void resetOverscrollTransforms() {
        if (mScrollingTransformsDirty) {
            setOverscrollTransformsDirty(false);
            setTranslationX(0);
            setRotationY(0);
            // It doesn't matter if we pass true or false here, the important thing is that we
            // pass 0, which results in the overscroll drawable not being drawn any more.
            setOverScrollAmount(0, false);
            setPivotX(getMeasuredWidth() / 2);
            setPivotY(getMeasuredHeight() / 2);
        }
    }
    
    public float getBackgroundAlpha() {
        return mBackgroundAlpha;
    }

    public void setBackgroundAlphaMultiplier(float multiplier) {
        if (mBackgroundAlphaMultiplier != multiplier) {
            mBackgroundAlphaMultiplier = multiplier;
            invalidate();
        }
    }

    public float getBackgroundAlphaMultiplier() {
        return mBackgroundAlphaMultiplier;
    }

    public void setBackgroundAlpha(float alpha) {
        if (mBackgroundAlpha != alpha) {
            mBackgroundAlpha = alpha;
            invalidate();
        }
    }
    
    public void enableHardwareLayers() {
        if (Util.DEBUG_ANIM) {
            Util.Log.v(TAG, "enableHardwareLayers() = index:"+((ViewGroup)getParent()).indexOfChild(this)
                    +"=="+this);
        }
        if(mShortcutsAndWidgets.getLayerType() == LAYER_TYPE_HARDWARE)
        	return;
        
        mShortcutsAndWidgets.setLayerType(LAYER_TYPE_HARDWARE, sPaint);
    }

    public void disableHardwareLayers() {
        if (Util.DEBUG_ANIM) {
            if (Util.DEBUG_ANIM) {
                Util.Log.v(TAG, "disableHardwareLayers() = index:"+((ViewGroup)getParent()).indexOfChild(this)
                        +"=="+this);
            }
        }
        mShortcutsAndWidgets.setLayerType(LAYER_TYPE_NONE, sPaint);
    }

    public void buildHardwareLayer() {
        mShortcutsAndWidgets.buildLayer();
    }

    public float getChildrenScale() {
        return mIsHotseat ? mHotseatScale : 1.0f;
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void restoreInstanceState(SparseArray<Parcelable> states) {
        dispatchRestoreInstanceState(states);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    public void setOnInterceptTouchListener(View.OnTouchListener listener) {
        mInterceptTouchListener = listener;
    }

    public int getCountX() {
        return mCountX;
    }

    public int getCountY() {
        return mCountY;
    }

    public void setIsHotseat(boolean isHotseat) {
        mIsHotseat = isHotseat;
    }
    
    public void setHotseatScalePercentage(int scalePer) {
    	mHotseatScale = scalePer/100.0f;
    }
    
    public boolean getIsHotseat(){
    	return mIsHotseat;
    }
    
    public boolean setGridSize(int x, int y) {
    	if(x == mCountX && y == mCountY)
    		return false;
    	
        
        boolean[][] occupied = new boolean[x][y];
        if(mOccupied != null){
            int minX = Math.min(x, mCountX);
            int minY = Math.min(y, mCountY);
            for (int i = 0; i < minX; i++) {
                for (int j = 0; j < minY; j++) {
                    occupied[i][j] = mOccupied[i][j];
                }
            }
        }
        
        mCountX = x;
        mCountY = y;
        mOccupied = occupied;
        mTmpOccupied = new boolean[mCountX][mCountY];
        mTempRectStack.clear();
        if(mShortcutsAndWidgets != null)
            mShortcutsAndWidgets.forceLayout();
        requestLayout();
        
        return true;
    }
    
    public void scaleRect(Rect r, float scale) {
        if (scale != 1.0f) {
            r.left = (int) (r.left * scale + 0.5f);
            r.top = (int) (r.top * scale + 0.5f);
            r.right = (int) (r.right * scale + 0.5f);
            r.bottom = (int) (r.bottom * scale + 0.5f);
        }
    }

    protected Rect temp = new Rect();
    public void scaleRectAboutCenter(Rect in, Rect out, float scale) {
        int cx = in.centerX();
        int cy = in.centerY();
        out.set(in);
        out.offset(-cx, -cy);
        scaleRect(out, scale);
        out.offset(cx, cy);
    }
    
    public void setPressedOrFocusedIcon(BubbleTextView icon) {
        // We draw the pressed or focused BubbleTextView's background in CellLayout because it
        // requires an expanded clip rect (due to the glow's blur radius)
        BubbleTextView oldIcon = mPressedOrFocusedIcon;
        mPressedOrFocusedIcon = icon;
        if (oldIcon != null) {
            invalidateBubbleTextView(oldIcon);
        }
        if (mPressedOrFocusedIcon != null) {
            invalidateBubbleTextView(mPressedOrFocusedIcon);
        }
    }
    
    private void invalidateBubbleTextView(BubbleTextView icon) {
        /// M: modified for unread feature, the icon is playced in a RelativeLayout,
        /// so we should invalidate the RelativeLayout, through getParent() to get the RelativeLayout.
        final View parent = (View)icon.getParent();
        final int padding = icon.getPressedOrFocusedBackgroundPadding();
        if (parent != null) {
            invalidate(parent.getLeft() + getPaddingLeft() - padding,
        		    parent.getTop() + getPaddingTop() - padding,
        		    parent.getRight() + getPaddingLeft() + padding,
        		    parent.getBottom() + getPaddingTop() + padding);
        }
    }
    
    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params,
            boolean markCells) {
        final LayoutParams lp = params;

        // Hotseat icons - remove text
//        if (child instanceof BubbleTextView) {
//            BubbleTextView bubbleChild = (BubbleTextView) child;
//
//            Resources res = getResources();
//            if (mIsHotseat) {
//                bubbleChild.setTextColor(res.getColor(android.R.color.transparent));
//            } else {
//                bubbleChild.setTextColor(res.getColor(R.color.workspace_icon_text_color));
//            }
//        }

        child.setScaleX(getChildrenScale());
        child.setScaleY(getChildrenScale());

        // Generate an id for each view, this assumes we have at most 256x256 cells
        // per workspace screen
        if (lp.cellX >= 0 && lp.cellX <= mCountX - 1 && lp.cellY >= 0 && lp.cellY <= mCountY - 1) {
            // If the horizontal or vertical span is set to -1, it is taken to
            // mean that it spans the extent of the CellLayout
            if (lp.cellHSpan < 0) lp.cellHSpan = mCountX;
            if (lp.cellVSpan < 0) lp.cellVSpan = mCountY;

            child.setId(childId);

            mShortcutsAndWidgets.addView(child, index, lp);

            if (markCells) markCellsAsOccupiedForView(child);

            return true;
        }
        return false;
    }
    
    @Override
    public void removeAllViews() {
        clearOccupiedCells();
        mShortcutsAndWidgets.removeAllViews();
    }

    @Override
    public void removeAllViewsInLayout() {
        if (mShortcutsAndWidgets.getChildCount() > 0) {
            clearOccupiedCells();
            mShortcutsAndWidgets.removeAllViewsInLayout();
        }
    }

    public void removeViewWithoutMarkingCells(View view) {
        mShortcutsAndWidgets.removeView(view);
    }

    @Override
    public void removeView(View view) {
        markCellsAsUnoccupiedForView(view);
        mShortcutsAndWidgets.removeView(view);
    }

    @Override
    public void removeViewAt(int index) {
        markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(index));
        mShortcutsAndWidgets.removeViewAt(index);
    }

    @Override
    public void removeViewInLayout(View view) {
        markCellsAsUnoccupiedForView(view);
        mShortcutsAndWidgets.removeViewInLayout(view);
    }

    @Override
    public void removeViews(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(i));
        }
        mShortcutsAndWidgets.removeViews(start, count);
    }

    @Override
    public void removeViewsInLayout(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(i));
        }
        mShortcutsAndWidgets.removeViewsInLayout(start, count);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mCellInfo.screen = ((ViewGroup) getParent()).indexOfChild(this);
    }

    public void setTagToCellInfoForPoint(int touchX, int touchY) {
        final CellInfo cellInfo = mCellInfo;
        Rect frame = mRect;
        final int x = touchX + getScrollX();
        final int y = touchY + getScrollY();
        final int count = mShortcutsAndWidgets.getChildCount();

        boolean found = false;
        for (int i = count - 1; i >= 0; i--) {
            final View child = mShortcutsAndWidgets.getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if ((child.getVisibility() == VISIBLE || child.getAnimation() != null) &&
                    lp.isLockedToGrid) {
                child.getHitRect(frame);

                float scale = child.getScaleX();
                frame = new Rect(child.getLeft(), child.getTop(), child.getRight(),
                        child.getBottom());
                // The child hit rect is relative to the CellLayoutChildren parent, so we need to
                // offset that by this CellLayout's padding to test an (x,y) point that is relative
                // to this view.
                frame.offset(getPaddingLeft(), getPaddingTop());
                frame.inset((int) (frame.width() * (1f - scale) / 2),
                        (int) (frame.height() * (1f - scale) / 2));

                if (frame.contains(x, y)) {
                    cellInfo.cell = child;
                    cellInfo.cellX = lp.cellX;
                    cellInfo.cellY = lp.cellY;
                    cellInfo.spanX = lp.cellHSpan;
                    cellInfo.spanY = lp.cellVSpan;
                    found = true;
                    break;
                }
            }
        }

        mLastDownOnOccupiedCell = found;

        if (!found) {
            final int cellXY[] = mTmpXY;
            pointToCellExact(x, y, cellXY);

            cellInfo.cell = null;
            cellInfo.cellX = cellXY[0];
            cellInfo.cellY = cellXY[1];
            cellInfo.spanX = 1;
            cellInfo.spanY = 1;
        }
        setTag(cellInfo);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // First we clear the tag to ensure that on every touch down we start with a fresh slate,
        // even in the case where we return early. Not clearing here was causing bugs whereby on
        // long-press we'd end up picking up an item from a previous drag operation.
        final int action = ev.getAction();
        

        if (action == MotionEvent.ACTION_DOWN) {
            clearTagCellInfo();
        }

        if (mInterceptTouchListener != null && mInterceptTouchListener.onTouch(this, ev)) {
            if (Util.DEBUG_MOTION) {
                Util.Log.d(TAG, "onInterceptTouchEvent: action = " + action 
                        +", onTouch return true"
                        + ", mInterceptTouchListener = " 
                        + mInterceptTouchListener);
            }
            return true;
        } else if (Util.DEBUG_MOTION){
            Util.Log.v(TAG, "onInterceptTouchEvent: action = " + action 
                    +", onTouch return false"
                    + ", mInterceptTouchListener = " + mInterceptTouchListener);
        }

        if (action == MotionEvent.ACTION_DOWN) {
            setTagToCellInfoForPoint((int) ev.getX(), (int) ev.getY());
        }

        if (Util.DEBUG_MOTION){
            Util.Log.v(TAG, "onInterceptTouchEvent(2): action = " + action 
                    +", onTouch return false"
                    + ", mInterceptTouchListener = " + mInterceptTouchListener);
        }
        return false;
    }

    private void clearTagCellInfo() {
        final CellInfo cellInfo = mCellInfo;
        cellInfo.cell = null;
        cellInfo.cellX = -1;
        cellInfo.cellY = -1;
        cellInfo.spanX = 0;
        cellInfo.spanY = 0;
        setTag(cellInfo);
    }

    public CellInfo getTag() {
        return (CellInfo) super.getTag();
    }

    /**
     * Given a point, return the cell that strictly encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    public void pointToCellExact(int x, int y, int[] result) {
        final int hStartPadding = getPaddingLeft() + (HOR_GAP_NEW_STYLE ? mWidthGap : 0);
        final int vStartPadding = getPaddingTop();
        
        result[0] = (x - hStartPadding) / (mCellWidth + mWidthGap);
        result[1] = (y - vStartPadding) / (mCellHeight + mHeightGap);

        final int xAxis = mCountX;
        final int yAxis = mCountY;

        if (result[0] < 0) result[0] = 0;
        if (result[0] >= xAxis) result[0] = xAxis - 1;
        if (result[1] < 0) result[1] = 0;
        if (result[1] >= yAxis) result[1] = yAxis - 1;
    }

    /**
     * Given a point, return the cell that most closely encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    public void pointToCellRounded(int x, int y, int[] result) {
        pointToCellExact(x + (mCellWidth / 2), y + (mCellHeight / 2), result);
    }

    /**
     * Given a cell coordinate, return the point that represents the upper left corner of that cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    public void cellToPoint(int cellX, int cellY, int[] result) {
        final int hStartPadding = getPaddingLeft() + (HOR_GAP_NEW_STYLE ? mWidthGap : 0);
        final int vStartPadding = getPaddingTop();
        result[0] = hStartPadding + cellX * (mCellWidth + mWidthGap);
        result[1] = vStartPadding + cellY * (mCellHeight + mHeightGap);
    }

    /**
     * Given a cell coordinate, return the point that represents the center of the cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    public void cellToCenterPoint(int cellX, int cellY, int[] result) {
        regionToCenterPoint(cellX, cellY, 1, 1, result);
    }

    /**
     * Given a cell coordinate and span return the point that represents the center of the regio
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    public void regionToCenterPoint(int cellX, int cellY, int spanX, int spanY, int[] result) {
        final int hStartPadding = getPaddingLeft() + (HOR_GAP_NEW_STYLE ? mWidthGap : 0);
        final int vStartPadding = getPaddingTop();
        result[0] = hStartPadding + cellX * (mCellWidth + mWidthGap) +
                    (spanX * mCellWidth + (spanX - 1) * mWidthGap) / 2;
        result[1] = vStartPadding + cellY * (mCellHeight + mHeightGap) +
                (spanY * mCellHeight + (spanY - 1) * mHeightGap) / 2;
    }

     /**
     * Given a cell coordinate and span fills out a corresponding pixel rect
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     * @param result Rect in which to write the result
     */
    public void regionToRect(int cellX, int cellY, int spanX, int spanY, Rect result) {
        final int hStartPadding = getPaddingLeft() + (HOR_GAP_NEW_STYLE ? mWidthGap : 0);
        final int vStartPadding = getPaddingTop();
        final int left = hStartPadding + cellX * (mCellWidth + mWidthGap);
        final int top = vStartPadding + cellY * (mCellHeight + mHeightGap);
        result.set(left, top, left + (spanX * mCellWidth + (spanX - 1) * mWidthGap),
                top + (spanY * mCellHeight + (spanY - 1) * mHeightGap));
    }

    public float getDistanceFromCell(float x, float y, int[] cell) {
        cellToCenterPoint(cell[0], cell[1], mTmpPoint);
        float distance = (float) Math.sqrt( Math.pow(x - mTmpPoint[0], 2) +
                Math.pow(y - mTmpPoint[1], 2));
        return distance;
    }

    public int getCellWidth() {
        return mCellWidth;
    }

    public int getCellHeight() {
        return mCellHeight;
    }

    public int getWidthGap() {
        return mWidthGap;
    }

    public int getHeightGap() {
        return mHeightGap;
    }

    public Rect getContentRect(Rect r) {
        if (r == null) {
            r = new Rect();
        }
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = left + getWidth() - getPaddingLeft() - getPaddingRight();
        int bottom = top + getHeight() - getPaddingTop() - getPaddingBottom();
        r.set(left, top, right, bottom);
        return r;
    }
    
    protected final Stack<Rect> mTempRectStack = new Stack<Rect>();
    protected void lazyInitTempRectStack() {
        if (mTempRectStack.isEmpty()) {
            for (int i = 0; i < mCountX * mCountY; i++) {
                mTempRectStack.push(new Rect());
            }
        }
    }

    protected void recycleTempRects(Stack<Rect> used) {
        while (!used.isEmpty()) {
            mTempRectStack.push(used.pop());
        }
    }
    
    public void requestChildLayout() {
        if (mShortcutsAndWidgets != null) {
            mShortcutsAndWidgets.requestLayout();
        }
    }

    public static void getMetrics(Rect metrics, IResConfigManager res, int measureWidth, int measureHeight,
            int countX, int countY, int orientation) {
        int numWidthGaps = HOR_GAP_NEW_STYLE ? (countX+1) : (countX - 1);
        int numHeightGaps = countY - 1;

        int widthGap;
        int heightGap;
        int cellWidth;
        int cellHeight;
        int paddingLeft;
        int paddingRight;
        int paddingTop;
        int paddingBottom;

        int maxGap = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_MAX_GAP);
        										//R.dimen.workspace_max_gap);
        if (orientation == Util.LANDSCAPE) {
            cellWidth = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_CELL_WIDTH_LAND);
            											//R.dimen.workspace_cell_width_land);
            cellHeight = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_CELL_HEIGHT_LAND);
														//R.dimen.workspace_cell_height_land);
            widthGap = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_WIDTH_GAP_LAND);
														//R.dimen.workspace_width_gap_land);
            heightGap = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_HEIGHT_GAP_LAND);
														//R.dimen.workspace_height_gap_land);
            paddingLeft = res.getDimensionPixelSize(IResConfigManager.DIM_CELLLAYOUT_LEFT_PADDING_LAND);
														//R.dimen.cell_layout_left_padding_land);
            paddingRight = res.getDimensionPixelSize(IResConfigManager.DIM_CELLLAYOUT_RIGHT_PADDING_LAND);
														//R.dimen.cell_layout_right_padding_land);
            paddingTop = res.getDimensionPixelSize(IResConfigManager.DIM_CELLLAYOUT_TOP_PADDING_LAND);
														//R.dimen.cell_layout_top_padding_land);
            paddingBottom = res.getDimensionPixelSize(IResConfigManager.DIM_CELLLAYOUT_BOTTOM_PADDING_LAND);
														//R.dimen.cell_layout_bottom_padding_land);
        } else {
            // PORTRAIT
            cellWidth = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_CELL_WIDTH_PORT);
			//R.dimen.workspace_cell_width_port);
            cellHeight = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_CELL_HEIGHT_PORT);
			//R.dimen.workspace_cell_height_port);
            widthGap = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_WIDTH_GAP_PORT);
			//R.dimen.workspace_width_gap_port);
            heightGap = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_HEIGHT_GAP_PORT);
			//R.dimen.workspace_height_gap_port);
            paddingLeft = res.getDimensionPixelSize(IResConfigManager.DIM_CELLLAYOUT_LEFT_PADDING_PORT);
			//R.dimen.cell_layout_left_padding_port);
            paddingRight = res.getDimensionPixelSize(IResConfigManager.DIM_CELLLAYOUT_RIGHT_PADDING_PORT);
			//R.dimen.cell_layout_right_padding_port);
            paddingTop = res.getDimensionPixelSize(IResConfigManager.DIM_CELLLAYOUT_TOP_PADDING_PORT);
			//R.dimen.cell_layout_top_padding_port);
            paddingBottom = res.getDimensionPixelSize(IResConfigManager.DIM_CELLLAYOUT_BOTTOM_PADDING_PORT);
			//R.dimen.cell_layout_bottom_padding_port);
        }
        
        

        if(cellWidth <= 0 || cellHeight <= 0){
        	int hSpace = measureWidth - paddingLeft - paddingRight;
            int vSpace = measureHeight - paddingTop - paddingBottom;
            int hFreeSpace = hSpace - (numWidthGaps * widthGap);
            int vFreeSpace = vSpace - (numHeightGaps * heightGap);
            
            cellWidth = hFreeSpace / countX;
            cellHeight = vFreeSpace / countY;

        	if(numWidthGaps > 0 && (hFreeSpace % countX) > 0){
        		widthGap = Math.min(maxGap, (widthGap + (hFreeSpace % countX) / numWidthGaps));
        	}
        	
        	if(numHeightGaps > 0 && (vFreeSpace % countY) > 0){
        		heightGap = Math.min(maxGap, (heightGap + (vFreeSpace % countY) / numHeightGaps));
        	}
        } else if (widthGap < 0 || heightGap < 0) {
            int hSpace = measureWidth - paddingLeft - paddingRight;
            int vSpace = measureHeight - paddingTop - paddingBottom;
            int hFreeSpace = hSpace - (countX * cellWidth);
            int vFreeSpace = vSpace - (countY * cellHeight);
            widthGap = Math.min(maxGap, numWidthGaps > 0 ? (hFreeSpace / numWidthGaps) : 0);
            heightGap = Math.min(maxGap, numHeightGaps > 0 ? (vFreeSpace / numHeightGaps) : 0);
        }
        metrics.set(cellWidth, cellHeight, widthGap, heightGap);
    }
    
    /**
     * Computes a bounding rectangle for a range of cells
     *
     * @param cellX X coordinate of upper left corner expressed as a cell position
     * @param cellY Y coordinate of upper left corner expressed as a cell position
     * @param cellHSpan Width in cells
     * @param cellVSpan Height in cells
     * @param resultRect Rect into which to put the results
     */
    public void cellToRect(int cellX, int cellY, int cellHSpan, int cellVSpan, Rect resultRect) {
        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;
        final int widthGap = mWidthGap;
        final int heightGap = mHeightGap;

        final int hStartPadding = getPaddingLeft() + (HOR_GAP_NEW_STYLE ? widthGap : 0);
        final int vStartPadding = getPaddingTop();

        int width = cellHSpan * cellWidth + ((cellHSpan - 1) * widthGap);
        int height = cellVSpan * cellHeight + ((cellVSpan - 1) * heightGap);

        int x = hStartPadding + cellX * (cellWidth + widthGap);
        int y = vStartPadding + cellY * (cellHeight + heightGap);

        resultRect.set(x, y, x + width, y + height);
    }

    /**
     * Computes the required horizontal and vertical cell spans to always
     * fit the given rectangle.
     *
     * @param width Width in pixels
     * @param height Height in pixels
     * @param result An array of length 2 in which to store the result (may be null).
     */
    public int[] rectToCell(int width, int height, int[] result) {
        //return rectToCell(getResources(), width, height, result);
    	//int actualWidth = resources.getDimensionPixelSize(R.dimen.workspace_cell_width);
        //int actualHeight = resources.getDimensionPixelSize(R.dimen.workspace_cell_height);
        int smallerSize = Math.min(mCellWidth, mCellHeight);

        // Always round up to next largest cell
        int spanX = (int) Math.ceil(width / (float) smallerSize);
        int spanY = (int) Math.ceil(height / (float) smallerSize);

        if (result == null) {
            return new int[] { spanX, spanY };
        }
        result[0] = spanX;
        result[1] = spanY;
        return result;
    }

    public static int[] rectToCell(LauncherHelper launcher, int width, int height, int[] result) {
        // Always assume we're working with the smallest span to make sure we
        // reserve enough space in both orientations.
    	final IResConfigManager res = launcher.getResConfigManager();
        int actualWidth = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_CELL_WIDTH, -1);//R.dimen.workspace_cell_width);
        int actualHeight = res.getDimensionPixelSize(IResConfigManager.DIM_WORKSPACE_CELL_HEIGHT, -1);//R.dimen.workspace_cell_height);
        if(actualWidth <= 0 || actualHeight <= 0){
        	Rect rect = AppWidgetResizeFrame.getCellLayoutMetrics(launcher, res.isLandscape() ? Util.LANDSCAPE : Util.PORTRAIT);
        	if(rect != null){
	        	actualWidth = rect.left;
	        	actualHeight = rect.top;
        	} else {
        		actualWidth = (int)(80 * res.getScreenDensity());
	        	actualHeight = (int)(100 * res.getScreenDensity());
        	}
        }
        int smallerSize = Math.min(actualWidth, actualHeight);

        // Always round up to next largest cell
        int spanX = (int) Math.ceil(width / (float) smallerSize);
        int spanY = (int) Math.ceil(height / (float) smallerSize);

        if (result == null) {
            return new int[] { spanX, spanY };
        }
        result[0] = spanX;
        result[1] = spanY;
        return result;
    }

    public int[] cellSpansToSize(int hSpans, int vSpans) {
        int[] size = new int[2];
        size[0] = hSpans * mCellWidth + (hSpans - 1) * mWidthGap;
        size[1] = vSpans * mCellHeight + (vSpans - 1) * mHeightGap;
        return size;
    }

    /**
     * Calculate the grid spans needed to fit given item
     */
    public void calculateSpans(ItemInfo info) {
        final int minWidth;
        final int minHeight;

        if (info instanceof LauncherAppWidgetInfo) {
            minWidth = ((LauncherAppWidgetInfo) info).minWidth;
            minHeight = ((LauncherAppWidgetInfo) info).minHeight;
        } else if (info instanceof PendingAddWidgetInfo) {
            minWidth = ((PendingAddWidgetInfo) info).minWidth;
            minHeight = ((PendingAddWidgetInfo) info).minHeight;
        } else {
            // It's not a widget, so it must be 1x1
            info.spanX = info.spanY = 1;
            return;
        }
        int[] spans = rectToCell(minWidth, minHeight, null);
        info.spanX = spans[0];
        info.spanY = spans[1];
    }

    /**
     * Find the first vacant cell, if there is one.
     *
     * @param vacant Holds the x and y coordinate of the vacant cell
     * @param spanX Horizontal cell span.
     * @param spanY Vertical cell span.
     *
     * @return True if a vacant cell was found
     */
    public boolean getVacantCell(int[] vacant, int spanX, int spanY) {

        return findVacantCell(vacant, spanX, spanY, mCountX, mCountY, mOccupied);
    }

    public static boolean findVacantCell(int[] vacant, int spanX, int spanY,
            int xCount, int yCount, boolean[][] occupied) {
    	if(occupied == null)
    		return false;
        for (int y = 0; y < yCount; y++) {
            for (int x = 0; x < xCount; x++) {
                boolean available = !occupied[x][y];
out:            for (int i = x; i < x + spanX - 1 && x < xCount; i++) {
                    for (int j = y; j < y + spanY - 1 && y < yCount; j++) {
                        available = available && !occupied[i][j];
                        if (!available) break out;
                    }
                }

                if (available) {
                    vacant[0] = x;
                    vacant[1] = y;
                    return true;
                }
            }
        }

        return false;
    }

    protected void clearOccupiedCells() {
    	if(mOccupied != null){
	        for (int x = 0; x < mCountX; x++) {
	            for (int y = 0; y < mCountY; y++) {
	                mOccupied[x][y] = false;
	            }
	        }
    	}
    }
    
    protected void drawDragOutlinePaint(Canvas canvas){
//		final Paint paint = mDragOutlinePaint;
//		for (int i = 0; i < mDragOutlines.length; i++) {
//			final float alpha = mDragOutlineAlphas[i];
//			if (alpha > 0) {
//				final Rect r = mDragOutlines[i];
//				scaleRectAboutCenter(r, temp, getChildrenScale());
//				final Bitmap b = (Bitmap) mDragOutlineAnims[i].getTag();
//				paint.setAlpha((int) (alpha + .5f));
//				canvas.drawBitmap(b, null, temp, paint);
//			}
//		}
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        if (Util.DEBUG_DRAW) {
            Util.Log.v(TAG, "onDraw(start) = mBackgroundAlpha:"+mBackgroundAlpha
                    +"="+this);
        }
        
    	if (mBackgroundAlpha > 0.0f) {
            Drawable bg;

            if (mIsDragOverlapping) {
                // In the mini case, we draw the active_glow bg *over* the active background
                bg = mActiveGlowBackground;
            } else {
                bg = mNormalBackground;
            }

            bg.setAlpha((int) (mBackgroundAlpha * mBackgroundAlphaMultiplier * 255));
            bg.setBounds(mBackgroundRect);
            bg.draw(canvas);
        }

    	drawDragOutlinePaint(canvas);
    	if (Util.DEBUG_DRAW) {
            Util.Log.v(TAG, "onDraw(2) = mPressedOrFocusedIcon:"+mPressedOrFocusedIcon);
        }
    	if (mPressedOrFocusedIcon != null) {
            final int padding = mPressedOrFocusedIcon.getPressedOrFocusedBackgroundPadding();
            final Bitmap b = mPressedOrFocusedIcon.getPressedOrFocusedBackground();
            /// M: modified for unread feature, the icon is playced in a RelativeLayout,
            /// so we should draw bitmap using the rect of the RelativeLayout.
            final View focusParent = (View) mPressedOrFocusedIcon.getParent();
            if (focusParent != null && b != null) {
                canvas.drawBitmap(b, focusParent.getLeft() + getPaddingLeft() - padding,
                        focusParent.getTop() + getPaddingTop() - padding, null);
            }
        }
    	
    	int previewOffset = FolderRingAnimator.sPreviewSize;
    	if (Util.DEBUG_DRAW) {
            Util.Log.v(TAG, "onDraw(3) = FolderOuterRings size:"+mFolderOuterRings.size());
        }
        // The folder outer / inner ring image(s)
        for (int i = 0; i < mFolderOuterRings.size(); i++) {
            FolderRingAnimator fra = mFolderOuterRings.get(i);
            int width, height;
            int centerX, centerY;
            // Draw outer ring
            Drawable d = FolderRingAnimator.sSharedOuterRingDrawable;
            if(d != null){
                width = (int) fra.getOuterRingSize();
                if(width > 0){
                    height = width;
                    cellToPoint(fra.mCellX, fra.mCellY, mTempLocation);
        
                    centerX = mTempLocation[0] + mCellWidth / 2;
                    centerY = mTempLocation[1] + previewOffset / 2;
                    if(d != null){
                        canvas.save();
                        canvas.translate(centerX - width / 2, centerY - height / 2);
                        d.setBounds(0, 0, width, height);
                        d.draw(canvas);
                        canvas.restore();
                    }
                }
            }

            // Draw inner ring
            d = FolderRingAnimator.sSharedInnerRingDrawable;
            if(d != null){
                width = (int) fra.getInnerRingSize();
                if(width > 0){
                    height = width;
                    cellToPoint(fra.mCellX, fra.mCellY, mTempLocation);
        
                    centerX = mTempLocation[0] + mCellWidth / 2;
                    centerY = mTempLocation[1] + previewOffset / 2;
                    if(d != null){
                        canvas.save();
                        canvas.translate(centerX - width / 2, centerY - width / 2);
                        d.setBounds(0, 0, width, height);
                        d.draw(canvas);
                        canvas.restore();
                    }
                }
            }
        }

        if (mFolderLeaveBehindCell[0] >= 0 && mFolderLeaveBehindCell[1] >= 0) {
            Drawable d = FolderIcon.sSharedFolderLeaveBehind;
            if(d != null){
                int width = d.getIntrinsicWidth();
                int height = d.getIntrinsicHeight();
    
                cellToPoint(mFolderLeaveBehindCell[0], mFolderLeaveBehindCell[1], mTempLocation);
                int centerX = mTempLocation[0] + mCellWidth / 2;
                int centerY = mTempLocation[1] + previewOffset / 2;
    
                canvas.save();
                canvas.translate(centerX - width / 2, centerY - width / 2);
                d.setBounds(0, 0, width, height);
                d.draw(canvas);
                canvas.restore();
            }
        }
        
        if (Util.DEBUG_DRAW) {
            Util.Log.v(TAG, "onDraw(end) = "+this);
        }
    }
    
    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (Util.DEBUG_DRAW) {
            Util.Log.v(TAG, "dispatchDraw(start) = "+this);
        }
        super.dispatchDraw(canvas);
        
        if (Util.DEBUG_DRAW) {
            Util.Log.v(TAG, "dispatchDraw(1) = mForegroundAlpha:"+mForegroundAlpha
                    +"=dr:"+mOverScrollForegroundDrawable
                    +"="+this);
        }
        if (mForegroundAlpha > 0 && mOverScrollForegroundDrawable != null) {
            mOverScrollForegroundDrawable.setBounds(mForegroundRect);
            if(mOverScrollForegroundDrawable instanceof NinePatchDrawable){
            	Paint p = ((NinePatchDrawable) mOverScrollForegroundDrawable).getPaint();
            	p.setXfermode(sAddBlendMode);
                mOverScrollForegroundDrawable.draw(canvas);
                p.setXfermode(null);
            }
        }
        if (Util.DEBUG_DRAW) {
            Util.Log.v(TAG, "dispatchDraw(end) = "+this);
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }
        if (Util.DEBUG_LAYOUT) {
            Util.Log.v(TAG, "onMeasure(start) = "+this);
        }
        final int numWidthGaps = (HOR_GAP_NEW_STYLE ? (mCountX + 1) : (mCountX - 1));
        final int numHeightGaps = mCountY - 1;
        
        if(mOriginalCellWidth <= 0 || mOriginalCellHeight <= 0){
        	int hSpace = widthSpecSize - getPaddingLeft() - getPaddingRight();
            int vSpace = heightSpecSize - getPaddingTop() - getPaddingBottom();
            int hFreeSpace = hSpace - (numWidthGaps * mWidthGap);
            int vFreeSpace = vSpace - (numHeightGaps * mHeightGap);
            
            mCellWidth = hFreeSpace / mCountX;
            mCellHeight = vFreeSpace / mCountY;
            
            //if (mOriginalWidthGap >= 0 && mOriginalHeightGap >= 0){
            	if(numWidthGaps > 0 && (hFreeSpace % mCountX) > 0){
            		mWidthGap = Math.min(mMaxGap, (mWidthGap + (hFreeSpace % mCountX) / numWidthGaps));
            	}
            	
            	if(numHeightGaps > 0 && (vFreeSpace % mCountY) > 0){
            		mHeightGap = Math.min(mMaxGap, (mHeightGap + (vFreeSpace % mCountY) / numHeightGaps));
            	}
            	
            	mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap);
            //}
        } else if (mOriginalWidthGap < 0 || mOriginalHeightGap < 0) {
            int hSpace = widthSpecSize - getPaddingLeft() - getPaddingRight();
            int vSpace = heightSpecSize - getPaddingTop() - getPaddingBottom();
            int hFreeSpace = hSpace - (mCountX * mCellWidth);
            int vFreeSpace = vSpace - (mCountY * mCellHeight);
            mWidthGap = Math.min(mMaxGap, numWidthGaps > 0 ? (hFreeSpace / numWidthGaps) : 0);
            mHeightGap = Math.min(mMaxGap,numHeightGaps > 0 ? (vFreeSpace / numHeightGaps) : 0);
            mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap);
        } else {
            mWidthGap = mOriginalWidthGap;
            mHeightGap = mOriginalHeightGap;
        }
        
        
        // Initial values correspond to widthSpecMode == MeasureSpec.EXACTLY
        int newWidth = widthSpecSize;
        int newHeight = heightSpecSize;
        if (widthSpecMode == MeasureSpec.AT_MOST) {
            newWidth = getPaddingLeft() + getPaddingRight() + (mCountX * mCellWidth) +
                (numWidthGaps * mWidthGap);
            newHeight = getPaddingTop() + getPaddingBottom() + (mCountY * mCellHeight) +
                (numHeightGaps * mHeightGap);
            setMeasuredDimension(newWidth, newHeight);
        }

        int count = getChildCount();
        
//        android.util.Log.v("QsLog", "Celllayout::onMeasure====="
//                +"==mCellWidth:"+mCellWidth
//                +"==mCellHeight:"+mCellHeight
//                +"==x:"+mCountX+"==y:"+mCountY
//                +"==childcount:"+count
//                +"==newWidth:"+newWidth
//                +"==newHeight:"+newHeight
//                +"==short:"+mShortcutsAndWidgets.equals(getChildAt(0)));
        
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(newWidth - getPaddingLeft() -
                    getPaddingRight(), MeasureSpec.EXACTLY);
            int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(newHeight - getPaddingTop() -
                    getPaddingBottom(), MeasureSpec.EXACTLY);
            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        }
        setMeasuredDimension(newWidth, newHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.layout(getPaddingLeft(), getPaddingTop(),
                    r - l - getPaddingRight(), b - t - getPaddingBottom());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBackgroundRect.set(0, 0, w, h);
        mForegroundRect.set(mForegroundPadding, mForegroundPadding,
                w - mForegroundPadding, h - mForegroundPadding);
    }

    @Override
    public void setChildrenDrawingCacheEnabled(boolean enabled) {
        if (Util.DEBUG_ANIM) {
            Util.Log.e(TAG, "setChildrenDrawingCacheEnabled(1) = enabled:"+enabled);
        }
        mShortcutsAndWidgets.setChildrenDrawingCacheEnabled(enabled);
    }

    @Override
    public void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        mShortcutsAndWidgets.setChildrenDrawnWithCacheEnabled(enabled);
    }
    
    public void setShortcutAndWidgetAlpha(float alpha) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).setAlpha(alpha);
        }
    }

    public ShortcutAndWidgetContainer getShortcutsAndWidgets() {
        if (getChildCount() > 0) {
            return (ShortcutAndWidgetContainer) getChildAt(0);
        }
        return null;
    }

    public View getChildAt(int x, int y) {
        return mShortcutsAndWidgets.getChildAt(x, y);
    }
    /**
     * Estimate where the top left cell of the dragged item will land if it is dropped.
     *
     * @param originX The X value of the top left corner of the item
     * @param originY The Y value of the top left corner of the item
     * @param spanX The number of horizontal cells that the item spans
     * @param spanY The number of vertical cells that the item spans
     * @param result The estimated drop cell X and Y.
     */
    public void estimateDropCell(int originX, int originY, int spanX, int spanY, int[] result) {
        final int countX = mCountX;
        final int countY = mCountY;

        // pointToCellRounded takes the top left of a cell but will pad that with
        // cellWidth/2 and cellHeight/2 when finding the matching cell
        pointToCellRounded(originX, originY, result);

        // If the item isn't fully on this screen, snap to the edges
        int rightOverhang = result[0] + spanX - countX;
        if (rightOverhang > 0) {
            result[0] -= rightOverhang; // Snap to right
        }
        result[0] = Math.max(0, result[0]); // Snap to left
        int bottomOverhang = result[1] + spanY - countY;
        if (bottomOverhang > 0) {
            result[1] -= bottomOverhang; // Snap to bottom
        }
        result[1] = Math.max(0, result[1]); // Snap to top
    }
    
    public void visualizeDropLocation(View v, Bitmap dragOutline, int originX, int originY, int cellX,
            int cellY, int spanX, int spanY, boolean resize, Point dragOffset, Rect dragRegion) {
    	
    }
    
    public void clearDragOutlines() {
    	
    }
    /*
     * Returns a pair (x, y), where x,y are in {-1, 0, 1} corresponding to vector between
     * the provided point and the provided cell
     */
    protected void computeDirectionVector(float deltaX, float deltaY, int[] result) {
        double angle = Math.atan(((float) deltaY) / deltaX);

        result[0] = 0;
        result[1] = 0;
        if (Math.abs(Math.cos(angle)) > 0.5f) {
            result[0] = (int) Math.signum(deltaX);
        }
        if (Math.abs(Math.sin(angle)) > 0.5f) {
            result[1] = (int) Math.signum(deltaY);
        }
    }

    protected void copyOccupiedArray(boolean[][] occupied) {
    	if(mOccupied != null){
	        for (int i = 0; i < mCountX; i++) {
	            for (int j = 0; j < mCountY; j++) {
	                occupied[i][j] = mOccupied[i][j];
	            }
	        }
    	}
    }
    
    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    public int[] findNearestArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
            View ignoreView, boolean ignoreOccupied, int[] result, int[] resultSpan,
            boolean[][] occupied) {
        lazyInitTempRectStack();
        // mark space take by ignoreView as available (method checks if ignoreView is null)
        markCellsAsUnoccupiedForView(ignoreView, occupied);

        // For items with a spanX / spanY > 1, the passed in point (pixelX, pixelY) corresponds
        // to the center of the item, but we are searching based on the top-left cell, so
        // we translate the point over to correspond to the top-left.
        pixelX -= (mCellWidth + mWidthGap) * (spanX - 1) / 2f;
        pixelY -= (mCellHeight + mHeightGap) * (spanY - 1) / 2f;

        // Keep track of best-scoring drop area
        final int[] bestXY = result != null ? result : new int[2];
        double bestDistance = Double.MAX_VALUE;
        final Rect bestRect = new Rect(-1, -1, -1, -1);
        final Stack<Rect> validRegions = new Stack<Rect>();

        final int countX = mCountX;
        final int countY = mCountY;

        if (minSpanX <= 0 || minSpanY <= 0 || spanX <= 0 || spanY <= 0 ||
                spanX < minSpanX || spanY < minSpanY) {
            return bestXY;
        }

        for (int y = 0; y < countY - (minSpanY - 1); y++) {
            inner:
            for (int x = 0; x < countX - (minSpanX - 1); x++) {
                int ySize = -1;
                int xSize = -1;
                if (ignoreOccupied) {
                    // First, let's see if this thing fits anywhere
                	if(occupied != null){
	                    for (int i = 0; i < minSpanX; i++) {
	                        for (int j = 0; j < minSpanY; j++) {
	                            if (occupied[x + i][y + j]) {
	                                continue inner;
	                            }
	                        }
	                    }
                	}
                    xSize = minSpanX;
                    ySize = minSpanY;

                    // We know that the item will fit at _some_ acceptable size, now let's see
                    // how big we can make it. We'll alternate between incrementing x and y spans
                    // until we hit a limit.
                    boolean incX = true;
                    boolean hitMaxX = xSize >= spanX;
                    boolean hitMaxY = ySize >= spanY;
                    while (!(hitMaxX && hitMaxY)) {
                        if (incX && !hitMaxX) {
                            for (int j = 0; j < ySize; j++) {
                                if (x + xSize > countX -1 || occupied[x + xSize][y + j]) {
                                    // We can't move out horizontally
                                    hitMaxX = true;
                                }
                            }
                            if (!hitMaxX) {
                                xSize++;
                            }
                        } else if (!hitMaxY) {
                            for (int i = 0; i < xSize; i++) {
                                if (y + ySize > countY - 1 || occupied[x + i][y + ySize]) {
                                    // We can't move out vertically
                                    hitMaxY = true;
                                }
                            }
                            if (!hitMaxY) {
                                ySize++;
                            }
                        }
                        hitMaxX |= xSize >= spanX;
                        hitMaxY |= ySize >= spanY;
                        incX = !incX;
                    }
                    incX = true;
                    hitMaxX = xSize >= spanX;
                    hitMaxY = ySize >= spanY;
                }
                final int[] cellXY = mTmpXY;
                cellToCenterPoint(x, y, cellXY);

                // We verify that the current rect is not a sub-rect of any of our previous
                // candidates. In this case, the current rect is disqualified in favour of the
                // containing rect.
                Rect currentRect = mTempRectStack.pop();
                currentRect.set(x, y, x + xSize, y + ySize);
                boolean contained = false;
                for (Rect r : validRegions) {
                    if (r.contains(currentRect)) {
                        contained = true;
                        break;
                    }
                }
                validRegions.push(currentRect);
                double distance = Math.sqrt(Math.pow(cellXY[0] - pixelX, 2)
                        + Math.pow(cellXY[1] - pixelY, 2));

                if ((distance <= bestDistance && !contained) ||
                        currentRect.contains(bestRect)) {
                    bestDistance = distance;
                    bestXY[0] = x;
                    bestXY[1] = y;
                    if (resultSpan != null) {
                        resultSpan[0] = xSize;
                        resultSpan[1] = ySize;
                    }
                    bestRect.set(currentRect);
                }
            }
        }
        // re-mark space taken by ignoreView as occupied
        markCellsAsOccupiedForView(ignoreView, occupied);

        // Return -1, -1 if no suitable location found
        if (bestDistance == Double.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        recycleTempRects(validRegions);
        return bestXY;
    }

     /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location, and will also weigh in a suggested direction vector of the
     * desired location. This method computers distance based on unit grid distances,
     * not pixel distances.
     *
     * @param cellX The X cell nearest to which you want to search for a vacant area.
     * @param cellY The Y cell nearest which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param direction The favored direction in which the views should move from x, y
     * @param exactDirectionOnly If this parameter is true, then only solutions where the direction
     *        matches exactly. Otherwise we find the best matching direction.
     * @param occoupied The array which represents which cells in the CellLayout are occupied
     * @param blockOccupied The array which represents which cells in the specified block (cellX,
     *        cellY, spanX, spanY) are occupied. This is used when try to move a group of views. 
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    protected int[] findNearestArea(int cellX, int cellY, int spanX, int spanY, int[] direction,
            boolean[][] occupied, boolean blockOccupied[][], int[] result) {
        // Keep track of best-scoring drop area
        final int[] bestXY = result != null ? result : new int[2];
        float bestDistance = Float.MAX_VALUE;
        int bestDirectionScore = Integer.MIN_VALUE;

        final int countX = mCountX;
        final int countY = mCountY;

        for (int y = 0; y < countY - (spanY - 1); y++) {
            inner:
            for (int x = 0; x < countX - (spanX - 1); x++) {
                // First, let's see if this thing fits anywhere
            	if(occupied != null){
	                for (int i = 0; i < spanX; i++) {
	                    for (int j = 0; j < spanY; j++) {
	                        if (occupied[x + i][y + j] && (blockOccupied == null || blockOccupied[i][j])) {
	                            continue inner;
	                        }
	                    }
	                }
            	}

                float distance = (float)
                        Math.sqrt((x - cellX) * (x - cellX) + (y - cellY) * (y - cellY));
                int[] curDirection = mTmpPoint;
                computeDirectionVector(x - cellX, y - cellY, curDirection);
                // The direction score is just the dot product of the two candidate direction
                // and that passed in.
                int curDirectionScore = direction[0] * curDirection[0] +
                        direction[1] * curDirection[1];
                boolean exactDirectionOnly = false;
                boolean directionMatches = direction[0] == curDirection[0] &&
                        direction[0] == curDirection[0];
                if ((directionMatches || !exactDirectionOnly) &&
                        Float.compare(distance,  bestDistance) < 0 || (Float.compare(distance,
                        bestDistance) == 0 && curDirectionScore > bestDirectionScore)) {
                    bestDistance = distance;
                    bestDirectionScore = curDirectionScore;
                    bestXY[0] = x;
                    bestXY[1] = y;
                }
            }
        }

        // Return -1, -1 if no suitable location found
        if (bestDistance == Float.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        return bestXY;
    }
    
    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    public int[] findNearestVacantArea(int pixelX, int pixelY, int spanX, int spanY,
            int[] result) {
        return findNearestVacantArea(pixelX, pixelY, spanX, spanY, null, result);
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    public int[] findNearestVacantArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX,
            int spanY, int[] result, int[] resultSpan) {
        return findNearestVacantArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, null,
                result, resultSpan);
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    public int[] findNearestArea(int pixelX, int pixelY, int spanX, int spanY, View ignoreView,
            boolean ignoreOccupied, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY,
                spanX, spanY, ignoreView, ignoreOccupied, result, null, mOccupied);
    }
    
    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreView Considers space occupied by this view as unoccupied
     * @param result Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    public int[] findNearestVacantArea(
            int pixelX, int pixelY, int spanX, int spanY, View ignoreView, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY, ignoreView, true, result);
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreView Considers space occupied by this view as unoccupied
     * @param result Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    public int[] findNearestVacantArea(int pixelX, int pixelY, int minSpanX, int minSpanY,
            int spanX, int spanY, View ignoreView, int[] result, int[] resultSpan) {
        return findNearestArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, ignoreView, true,
                result, resultSpan, mOccupied);
    }

    /**
     * Find a starting cell position that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreView Considers space occupied by this view as unoccupied
     * @param result Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    public int[] findNearestArea(
            int pixelX, int pixelY, int spanX, int spanY, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY, null, false, result);
    }

    public boolean existsEmptyCell() {
        return findCellForSpan(null, 1, 1);
    }

    /**
     * Finds the upper-left coordinate of the first rectangle in the grid that can
     * hold a cell of the specified dimensions. If intersectX and intersectY are not -1,
     * then this method will only return coordinates for rectangles that contain the cell
     * (intersectX, intersectY)
     *
     * @param cellXY The array that will contain the position of a vacant cell if such a cell
     *               can be found.
     * @param spanX The horizontal span of the cell we want to find.
     * @param spanY The vertical span of the cell we want to find.
     *
     * @return True if a vacant cell of the specified dimension was found, false otherwise.
     */
    public boolean findCellForSpan(int[] cellXY, int spanX, int spanY) {
        return findCellForSpanThatIntersectsIgnoring(cellXY, spanX, spanY, -1, -1, null, mOccupied);
    }

    /**
     * Like above, but ignores any cells occupied by the item "ignoreView"
     *
     * @param cellXY The array that will contain the position of a vacant cell if such a cell
     *               can be found.
     * @param spanX The horizontal span of the cell we want to find.
     * @param spanY The vertical span of the cell we want to find.
     * @param ignoreView The home screen item we should treat as not occupying any space
     * @return
     */
    public boolean findCellForSpanIgnoring(int[] cellXY, int spanX, int spanY, View ignoreView) {
        return findCellForSpanThatIntersectsIgnoring(cellXY, spanX, spanY, -1, -1,
                ignoreView, mOccupied);
    }

    /**
     * Like above, but if intersectX and intersectY are not -1, then this method will try to
     * return coordinates for rectangles that contain the cell [intersectX, intersectY]
     *
     * @param spanX The horizontal span of the cell we want to find.
     * @param spanY The vertical span of the cell we want to find.
     * @param ignoreView The home screen item we should treat as not occupying any space
     * @param intersectX The X coordinate of the cell that we should try to overlap
     * @param intersectX The Y coordinate of the cell that we should try to overlap
     *
     * @return True if a vacant cell of the specified dimension was found, false otherwise.
     */
    public boolean findCellForSpanThatIntersects(int[] cellXY, int spanX, int spanY,
            int intersectX, int intersectY) {
        return findCellForSpanThatIntersectsIgnoring(
                cellXY, spanX, spanY, intersectX, intersectY, null, mOccupied);
    }

    /**
     * The superset of the above two methods
     */
    public boolean findCellForSpanThatIntersectsIgnoring(int[] cellXY, int spanX, int spanY,
            int intersectX, int intersectY, View ignoreView, boolean occupied[][]) {
        // mark space take by ignoreView as available (method checks if ignoreView is null)
        markCellsAsUnoccupiedForView(ignoreView, occupied);

        boolean foundCell = false;
        while (true) {
            int startX = 0;
            if (intersectX >= 0) {
                startX = Math.max(startX, intersectX - (spanX - 1));
            }
            int endX = mCountX - (spanX - 1);
            if (intersectX >= 0) {
                endX = Math.min(endX, intersectX + (spanX - 1) + (spanX == 1 ? 1 : 0));
            }
            int startY = 0;
            if (intersectY >= 0) {
                startY = Math.max(startY, intersectY - (spanY - 1));
            }
            int endY = mCountY - (spanY - 1);
            if (intersectY >= 0) {
                endY = Math.min(endY, intersectY + (spanY - 1) + (spanY == 1 ? 1 : 0));
            }

            for (int y = startY; y < endY && !foundCell; y++) {
                inner:
                for (int x = startX; x < endX; x++) {
                	if(occupied != null){
	                    for (int i = 0; i < spanX; i++) {
	                        for (int j = 0; j < spanY; j++) {
	                            if (occupied[x + i][y + j]) {
	                                // small optimization: we can skip to after the column we just found
	                                // an occupied cell
	                                x += i;
	                                continue inner;
	                            }
	                        }
	                    }
                	}
                    if (cellXY != null) {
                        cellXY[0] = x;
                        cellXY[1] = y;
                    }
                    foundCell = true;
                    break;
                }
            }
            if (intersectX == -1 && intersectY == -1) {
                break;
            } else {
                // if we failed to find anything, try again but without any requirements of
                // intersecting
                intersectX = -1;
                intersectY = -1;
                continue;
            }
        }

        // re-mark space taken by ignoreView as occupied
        markCellsAsOccupiedForView(ignoreView, occupied);
        return foundCell;
    }
    
    public void prepareChildForDrag(View child) {
        markCellsAsUnoccupiedForView(child);
    }

    /* This seems like it should be obvious and straight-forward, but when the direction vector
    needs to match with the notion of the dragView pushing other views, we have to employ
    a slightly more subtle notion of the direction vector. The question is what two points is
    the vector between? The center of the dragView and its desired destination? Not quite, as
    this doesn't necessarily coincide with the interaction of the dragView and items occupying
    those cells. Instead we use some heuristics to often lock the vector to up, down, left
    or right, which helps make pushing feel right.
    */
    protected void getDirectionVectorForDrop(int dragViewCenterX, int dragViewCenterY, int spanX,
            int spanY, View dragView, int[] resultDirection) {
        int[] targetDestination = new int[2];

        findNearestArea(dragViewCenterX, dragViewCenterY, spanX, spanY, targetDestination);
        Rect dragRect = new Rect();
        regionToRect(targetDestination[0], targetDestination[1], spanX, spanY, dragRect);
        dragRect.offset(dragViewCenterX - dragRect.centerX(), dragViewCenterY - dragRect.centerY());

        Rect dropRegionRect = new Rect();
        getViewsIntersectingRegion(targetDestination[0], targetDestination[1], spanX, spanY,
                dragView, dropRegionRect, mIntersectingViews);

        int dropRegionSpanX = dropRegionRect.width();
        int dropRegionSpanY = dropRegionRect.height();

        regionToRect(dropRegionRect.left, dropRegionRect.top, dropRegionRect.width(),
                dropRegionRect.height(), dropRegionRect);

        int deltaX = (dropRegionRect.centerX() - dragViewCenterX) / spanX;
        int deltaY = (dropRegionRect.centerY() - dragViewCenterY) / spanY;

        if (dropRegionSpanX == mCountX || spanX == mCountX) {
            deltaX = 0;
        }
        if (dropRegionSpanY == mCountY || spanY == mCountY) {
            deltaY = 0;
        }

        if (deltaX == 0 && deltaY == 0) {
            // No idea what to do, give a random direction.
            resultDirection[0] = 1;
            resultDirection[1] = 0;
        } else {
            computeDirectionVector(deltaX, deltaY, resultDirection);
        }
    }

    // For a given cell and span, fetch the set of views intersecting the region.
    protected void getViewsIntersectingRegion(int cellX, int cellY, int spanX, int spanY,
            View dragView, Rect boundingRect, ArrayList<View> intersectingViews) {
        if (boundingRect != null) {
            boundingRect.set(cellX, cellY, cellX + spanX, cellY + spanY);
        }
        intersectingViews.clear();
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        final int count = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            r1.set(lp.cellX, lp.cellY, lp.cellX + lp.cellHSpan, lp.cellY + lp.cellVSpan);
            if (Rect.intersects(r0, r1)) {
                mIntersectingViews.add(child);
                if (boundingRect != null) {
                    boundingRect.union(r1);
                }
            }
        }
    }

    public boolean isNearestDropLocationOccupied(int pixelX, int pixelY, int spanX, int spanY,
            View dragView, int[] result) {
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);
        getViewsIntersectingRegion(result[0], result[1], spanX, spanY, dragView, null,
                mIntersectingViews);
        return !mIntersectingViews.isEmpty();
    }

    public void onMove(View view, int newCellX, int newCellY, int newSpanX, int newSpanY) {
        markCellsAsUnoccupiedForView(view);
        markCellsForView(newCellX, newCellY, newSpanX, newSpanY, mOccupied, true);
    }

    public void markCellsAsOccupiedForView(View view) {
        markCellsAsOccupiedForView(view, mOccupied);
    }
    public void markCellsAsOccupiedForView(View view, boolean[][] occupied) {
        if (view == null || view.getParent() != mShortcutsAndWidgets) return;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, occupied, true);
    }

    public void markCellsAsUnoccupiedForView(View view) {
        markCellsAsUnoccupiedForView(view, mOccupied);
    }
    public void markCellsAsUnoccupiedForView(View view, boolean occupied[][]) {
        if (view == null || view.getParent() != mShortcutsAndWidgets) return;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, occupied, false);
    }

    protected void markCellsForView(int cellX, int cellY, int spanX, int spanY, boolean[][] occupied,
            boolean value) {
        if (cellX < 0 || cellY < 0 || occupied == null) return;
        for (int x = cellX; x < cellX + spanX && x < mCountX; x++) {
            for (int y = cellY; y < cellY + spanY && y < mCountY; y++) {
                occupied[x][y] = value;
            }
        }
    }
    
    public boolean lastDownOnOccupiedCell() {
        return mLastDownOnOccupiedCell;
    }
    
    public int getDesiredWidth() {
        if(HOR_GAP_NEW_STYLE){
            return getPaddingLeft() + getPaddingRight() + (mCountX * mCellWidth) +
                    (Math.max((mCountX + 1), 0) * mWidthGap);
        }
        return getPaddingLeft() + getPaddingRight() + (mCountX * mCellWidth) +
                (Math.max((mCountX - 1), 0) * mWidthGap);
    }

    public int getDesiredHeight()  {
        return getPaddingTop() + getPaddingBottom() + (mCountY * mCellHeight) +
                (Math.max((mCountY - 1), 0) * mHeightGap);
    }

    public boolean isOccupied(int x, int y) {
        if (mOccupied != null && x < mCountX && y < mCountY) {
            return mOccupied[x][y];
        } else {
            throw new RuntimeException("Position exceeds the bound of this CellLayout");
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CellLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CellLayout.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new CellLayout.LayoutParams(p);
    }

    public static class CellLayoutAnimationController extends LayoutAnimationController {
        public CellLayoutAnimationController(Animation animation, float delay) {
            super(animation, delay);
        }

        @Override
        protected long getDelayForView(View view) {
            return (int) (Math.random() * 150);
        }
    }
    
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        /**
         * Horizontal location of the item in the grid.
         */
        //@ViewDebug.ExportedProperty
        public int cellX;

        /**
         * Vertical location of the item in the grid.
         */
        //@ViewDebug.ExportedProperty
        public int cellY;

        /**
         * Temporary horizontal location of the item in the grid during reorder
         */
        public int tmpCellX;

        /**
         * Temporary vertical location of the item in the grid during reorder
         */
        public int tmpCellY;

        /**
         * Indicates that the temporary coordinates should be used to layout the items
         */
        public boolean useTmpCoords;

        /**
         * Number of cells spanned horizontally by the item.
         */
        //@ViewDebug.ExportedProperty
        public int cellHSpan;

        /**
         * Number of cells spanned vertically by the item.
         */
        //@ViewDebug.ExportedProperty
        public int cellVSpan;

        /**
         * Indicates whether the item will set its x, y, width and height parameters freely,
         * or whether these will be computed based on cellX, cellY, cellHSpan and cellVSpan.
         */
        public boolean isLockedToGrid = true;

        /**
         * Indicates whether this item can be reordered. Always true except in the case of the
         * the AllApps button.
         */
        public boolean canReorder = true;

        // X coordinate of the view in the layout.
        //@ViewDebug.ExportedProperty
        public int x;
        // Y coordinate of the view in the layout.
        //@ViewDebug.ExportedProperty
        public int y;

        public boolean dropped;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.cellX = source.cellX;
            this.cellY = source.cellY;
            this.cellHSpan = source.cellHSpan;
            this.cellVSpan = source.cellVSpan;
        }

        public LayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            this.cellX = cellX;
            this.cellY = cellY;
            this.cellHSpan = cellHSpan;
            this.cellVSpan = cellVSpan;
        }

        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap) {
            if (isLockedToGrid) {
                final int myCellHSpan = cellHSpan;
                final int myCellVSpan = cellVSpan;
                final int myCellX = useTmpCoords ? tmpCellX : cellX;
                final int myCellY = useTmpCoords ? tmpCellY : cellY;

                width = myCellHSpan * cellWidth + ((myCellHSpan - 1) * widthGap) -
                        leftMargin - rightMargin;
                height = myCellVSpan * cellHeight + ((myCellVSpan - 1) * heightGap) -
                        topMargin - bottomMargin;
                if(HOR_GAP_NEW_STYLE)
                    x = (int) (widthGap + myCellX * (cellWidth + widthGap) + leftMargin);
                else
                    x = (int) (myCellX * (cellWidth + widthGap) + leftMargin);
                y = (int) (myCellY * (cellHeight + heightGap) + topMargin);
            }
        }

        public String toString() {
            return "(" + this.cellX + ", " + this.cellY + ")";
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getY() {
            return y;
        }
    }
    
    
    public static final class CellInfo {
    	public View cell;
    	public int cellX = -1;
    	public int cellY = -1;
    	public int spanX;
    	public int spanY;
    	public int screen;
    	public long container;

        @Override
        public String toString() {
            return "Cell[view=" + (cell == null ? "null" : cell.getClass())
                    + ", x=" + cellX + ", y=" + cellY + "]";
        }
    }
    
    public void showFolderAccept(FolderRingAnimator fra) {
        mFolderOuterRings.add(fra);
        //android.util.Log.i("QsLog", "showFolderAccept()===size:"+mFolderOuterRings.size());
    }

    public void hideFolderAccept(FolderRingAnimator fra) {
        //android.util.Log.i("QsLog", "hideFolderAccept(0)===size:"+mFolderOuterRings.size());
        if (mFolderOuterRings.contains(fra)) {
            mFolderOuterRings.remove(fra);
        }
        //android.util.Log.i("QsLog", "hideFolderAccept(1)===size:"+mFolderOuterRings.size());
        invalidate();
    }

    public void setFolderLeaveBehindCell(int x, int y) {
        mFolderLeaveBehindCell[0] = x;
        mFolderLeaveBehindCell[1] = y;
        invalidate();
    }

    public void clearFolderLeaveBehind() {
        mFolderLeaveBehindCell[0] = -1;
        mFolderLeaveBehindCell[1] = -1;
        invalidate();
    }
    
    /**
     * A drag event has begun over this layout.
     * It may have begun over this layout (in which case onDragChild is called first),
     * or it may have begun on another layout.
     */
    public void onDragEnter() {
        if (Util.DEBUG_DRAG) {
        	Util.Log.d(TAG, "onDragEnter: mDragging = " + mDragging);
        }

        mDragEnforcer.onDragEnter();
        mDragging = true;
    }

    /**
     * Called when drag has left this CellLayout or has been completed (successfully or not)
     */
    public void onDragExit() {
    	if (Util.DEBUG_DRAG) {
    		Util.Log.d(TAG, "onDragExit: mDragging = " + mDragging);
        }

        mDragEnforcer.onDragExit();
        // This can actually be called when we aren't in a drag, e.g. when adding a new
        // item to this layout via the customize drawer.
        // Guard against that case.
        if (mDragging) {
            mDragging = false;
        }

        // Invalidate the drag data
//        mDragCell[0] = mDragCell[1] = -1;
//        mDragOutlineAnims[mDragOutlineCurrent].animateOut();
//        mDragOutlineCurrent = (mDragOutlineCurrent + 1) % mDragOutlineAnims.length;
//        revertTempState();
        setIsDragOverlapping(false);
    }

    /**
     * Mark a child as having been dropped.
     * At the beginning of the drag operation, the child may have been on another
     * screen, but it is re-parented before this method is called.
     *
     * @param child The child that is being dropped
     */
    public void onDropChild(View child) {
    	if (Util.DEBUG_DRAG) {
    		Util.Log.d(TAG, "onDropChild: child = " + child);
        }

        if (child != null) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.dropped = true;
            child.requestLayout();
        }
    }
    
    public boolean animateChildToPosition(final View child, int cellX, int cellY, int duration,
            int delay, boolean permanent, boolean adjustOccupied) {
        ShortcutAndWidgetContainer clc = getShortcutsAndWidgets();
        boolean[][] occupied = mOccupied;
        if (!permanent) {
            occupied = mTmpOccupied;
        }

        if (occupied != null && clc.indexOfChild(child) != -1) {
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final ItemInfo info = (ItemInfo) child.getTag();

            // We cancel any existing animations
            if (mReorderAnimators.containsKey(lp)) {
                mReorderAnimators.get(lp).cancel();
                mReorderAnimators.remove(lp);
            }

            final int oldX = lp.x;
            final int oldY = lp.y;
            if (adjustOccupied) {
                occupied[lp.cellX][lp.cellY] = false;
                occupied[cellX][cellY] = true;
            }
            lp.isLockedToGrid = true;
            if (permanent) {
                lp.cellX = info.cellX = cellX;
                lp.cellY = info.cellY = cellY;
            } else {
                lp.tmpCellX = cellX;
                lp.tmpCellY = cellY;
            }
            clc.setupLp(lp);
            lp.isLockedToGrid = false;
            final int newX = lp.x;
            final int newY = lp.y;

            lp.x = oldX;
            lp.y = oldY;

            // Exit early if we're not actually moving the view
            if (oldX == newX && oldY == newY) {
                lp.isLockedToGrid = true;
                return true;
            }

            ValueAnimator va = LauncherAnimUtils.ofFloat(0f, 1f);
            va.setDuration(duration);
            mReorderAnimators.put(lp, va);

            va.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = ((Float) animation.getAnimatedValue()).floatValue();
                    lp.x = (int) ((1 - r) * oldX + r * newX);
                    lp.y = (int) ((1 - r) * oldY + r * newY);
                    child.requestLayout();
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                boolean cancelled = false;
                public void onAnimationEnd(Animator animation) {
                    // If the animation was cancelled, it means that another animation
                    // has interrupted this one, and we don't want to lock the item into
                    // place just yet.
                    if (!cancelled) {
                        lp.isLockedToGrid = true;
                        child.requestLayout();
                    }
                    if (mReorderAnimators.containsKey(lp)) {
                        mReorderAnimators.remove(lp);
                    }
                }
                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }
            });
            va.setStartDelay(delay);
            va.start();
            return true;
        }
        return false;
    }
    
    
    protected static final boolean DESTRUCTIVE_REORDER = false;
    protected boolean mItemPlacementDirty = false;
    
    protected static final float REORDER_HINT_MAGNITUDE = 0.12f;
    protected static final int REORDER_ANIMATION_DURATION = 150;
    protected HashMap<CellLayout.LayoutParams, Animator> mReorderAnimators = new
            HashMap<CellLayout.LayoutParams, Animator>();
    protected HashMap<View, ReorderHintAnimation>
            mShakeAnimators = new HashMap<View, ReorderHintAnimation>();
    
    protected float mReorderHintAnimationMagnitude;
    protected Rect mOccupiedRect = new Rect();
    protected int[] mDirectionVector = new int[2];
    protected int[] mPreviousReorderDirection = new int[2];
    protected static final int INVALID_DIRECTION = -100;
    
    protected void completeAndClearReorderHintAnimations() {
        for (ReorderHintAnimation a: mShakeAnimators.values()) {
            a.completeAnimationImmediately();
        }
        mShakeAnimators.clear();
    }

    protected void commitTempPlacement() {
    	if(mOccupied != null){
	        for (int i = 0; i < mCountX; i++) {
	            for (int j = 0; j < mCountY; j++) {
	                mOccupied[i][j] = mTmpOccupied[i][j];
	            }
	        }
    	}
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            ItemInfo info = (ItemInfo) child.getTag();
            // We do a null check here because the item info can be null in the case of the
            // AllApps button in the hotseat.
            if (info != null) {
                if (info.cellX != lp.tmpCellX || info.cellY != lp.tmpCellY ||
                        info.spanX != lp.cellHSpan || info.spanY != lp.cellVSpan) {
                    info.requiresDbUpdate = true;
                }
                info.cellX = lp.cellX = lp.tmpCellX;
                info.cellY = lp.cellY = lp.tmpCellY;
                info.spanX = lp.cellHSpan;
                info.spanY = lp.cellVSpan;
            }
        }
        if(mLauncher != null)
        	mLauncher.getWorkspace().updateItemLocationsInDatabase(this);
    }
        
    protected boolean pushViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
            int[] direction, View dragView, ItemConfiguration currentState) {

        ViewCluster cluster = new ViewCluster(views, currentState);
        Rect clusterRect = cluster.getBoundingRect();
        int whichEdge;
        int pushDistance;
        boolean fail = false;

        // Determine the edge of the cluster that will be leading the push and how far
        // the cluster must be shifted.
        if (direction[0] < 0) {
            whichEdge = ViewCluster.LEFT;
            pushDistance = clusterRect.right - rectOccupiedByPotentialDrop.left;
        } else if (direction[0] > 0) {
            whichEdge = ViewCluster.RIGHT;
            pushDistance = rectOccupiedByPotentialDrop.right - clusterRect.left;
        } else if (direction[1] < 0) {
            whichEdge = ViewCluster.TOP;
            pushDistance = clusterRect.bottom - rectOccupiedByPotentialDrop.top;
        } else {
            whichEdge = ViewCluster.BOTTOM;
            pushDistance = rectOccupiedByPotentialDrop.bottom - clusterRect.top;
        }

        // Break early for invalid push distance.
        if (pushDistance <= 0) {
            return false;
        }

        // Mark the occupied state as false for the group of views we want to move.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        }

        // We save the current configuration -- if we fail to find a solution we will revert
        // to the initial state. The process of finding a solution modifies the configuration
        // in place, hence the need for revert in the failure case.
        currentState.save();

        // The pushing algorithm is simplified by considering the views in the order in which
        // they would be pushed by the cluster. For example, if the cluster is leading with its
        // left edge, we consider sort the views by their right edge, from right to left.
        cluster.sortConfigurationForEdgePush(whichEdge);

        while (pushDistance > 0 && !fail) {
            for (View v: currentState.sortedViews) {
                // For each view that isn't in the cluster, we see if the leading edge of the
                // cluster is contacting the edge of that view. If so, we add that view to the
                // cluster.
                if (!cluster.views.contains(v) && v != dragView) {
                    if (cluster.isViewTouchingEdge(v, whichEdge)) {
                        LayoutParams lp = (LayoutParams) v.getLayoutParams();
                        if (!lp.canReorder) {
                            // The push solution includes the all apps button, this is not viable.
                            fail = true;
                            break;
                        }
                        cluster.addView(v);
                        CellAndSpan c = currentState.map.get(v);

                        // Adding view to cluster, mark it as not occupied.
                        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
                    }
                }
            }
            pushDistance--;

            // The cluster has been completed, now we move the whole thing over in the appropriate
            // direction.
            cluster.shift(whichEdge, 1);
        }

        boolean foundSolution = false;
        clusterRect = cluster.getBoundingRect();

        // Due to the nature of the algorithm, the only check required to verify a valid solution
        // is to ensure that completed shifted cluster lies completely within the cell layout.
        if (!fail && clusterRect.left >= 0 && clusterRect.right <= mCountX && clusterRect.top >= 0 &&
                clusterRect.bottom <= mCountY) {
            foundSolution = true;
        } else {
            currentState.restore();
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (View v: cluster.views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        }

        return foundSolution;
    }
    protected boolean addViewToTempLocation(View v, Rect rectOccupiedByPotentialDrop,
            int[] direction, ItemConfiguration currentState) {
        CellAndSpan c = currentState.map.get(v);
        boolean success = false;
        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        markCellsForRect(rectOccupiedByPotentialDrop, mTmpOccupied, true);

        findNearestArea(c.x, c.y, c.spanX, c.spanY, direction, mTmpOccupied, null, mTempLocation);

        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            c.x = mTempLocation[0];
            c.y = mTempLocation[1];
            success = true;
        }
        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        return success;
    }
    protected boolean addViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
            int[] direction, View dragView, ItemConfiguration currentState) {
        if (views.size() == 0) return true;

        boolean success = false;
        Rect boundingRect = null;
        // We construct a rect which represents the entire group of views passed in
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            if (boundingRect == null) {
                boundingRect = new Rect(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            } else {
                boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            }
        }

        // Mark the occupied state as false for the group of views we want to move.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        }

        boolean[][] blockOccupied = new boolean[boundingRect.width()][boundingRect.height()];
        int top = boundingRect.top;
        int left = boundingRect.left;
        // We mark more precisely which parts of the bounding rect are truly occupied, allowing
        // for interlocking.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x - left, c.y - top, c.spanX, c.spanY, blockOccupied, true);
        }

        markCellsForRect(rectOccupiedByPotentialDrop, mTmpOccupied, true);

        findNearestArea(boundingRect.left, boundingRect.top, boundingRect.width(),
                boundingRect.height(), direction, mTmpOccupied, blockOccupied, mTempLocation);

        // If we successfuly found a location by pushing the block of views, we commit it
        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            int deltaX = mTempLocation[0] - boundingRect.left;
            int deltaY = mTempLocation[1] - boundingRect.top;
            for (View v: views) {
                CellAndSpan c = currentState.map.get(v);
                c.x += deltaX;
                c.y += deltaY;
            }
            success = true;
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        }
        return success;
    }

    protected void markCellsForRect(Rect r, boolean[][] occupied, boolean value) {
        markCellsForView(r.left, r.top, r.width(), r.height(), occupied, value);
    }

    // This method tries to find a reordering solution which satisfies the push mechanic by trying
    // to push items in each of the cardinal directions, in an order based on the direction vector
    // passed.
    protected boolean attemptPushInDirection(ArrayList<View> intersectingViews, Rect occupied,
            int[] direction, View ignoreView, ItemConfiguration solution) {
        if ((Math.abs(direction[0]) + Math.abs(direction[1])) > 1) {
            // If the direction vector has two non-zero components, we try pushing 
            // separately in each of the components.
            int temp = direction[1];
            direction[1] = 0;

            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;

            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Revert the direction
            direction[0] = temp;

            // Now we try pushing in each component of the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            temp = direction[1];
            direction[1] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }

            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // revert the direction
            direction[0] = temp;
            direction[0] *= -1;
            direction[1] *= -1;
            
        } else {
            // If the direction vector has a single non-zero component, we push first in the
            // direction of the vector
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Then we try the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Switch the direction back
            direction[0] *= -1;
            direction[1] *= -1;
            
            // If we have failed to find a push solution with the above, then we try 
            // to find a solution by pushing along the perpendicular axis.

            // Swap the components
            int temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }

            // Then we try the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Switch the direction back
            direction[0] *= -1;
            direction[1] *= -1;

            // Swap the components back
            temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
        }
        return false;
    }

    protected boolean rearrangementExists(int cellX, int cellY, int spanX, int spanY, int[] direction,
            View ignoreView, ItemConfiguration solution) {
        // Return early if get invalid cell positions
        if (cellX < 0 || cellY < 0) return false;

        mIntersectingViews.clear();
        mOccupiedRect.set(cellX, cellY, cellX + spanX, cellY + spanY);

        // Mark the desired location of the view currently being dragged.
        if (ignoreView != null) {
            CellAndSpan c = solution.map.get(ignoreView);
            if (c != null) {
                c.x = cellX;
                c.y = cellY;
            }
        }
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        for (View child: solution.map.keySet()) {
            if (child == ignoreView) continue;
            CellAndSpan c = solution.map.get(child);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            r1.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            if (Rect.intersects(r0, r1)) {
                if (!lp.canReorder) {
                    return false;
                }
                mIntersectingViews.add(child);
            }
        }

        // First we try to find a solution which respects the push mechanic. That is, 
        // we try to find a solution such that no displaced item travels through another item
        // without also displacing that item.
        if (attemptPushInDirection(mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        // Next we try moving the views as a block, but without requiring the push mechanic.
        if (addViewsToTempLocation(mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        // Ok, they couldn't move as a block, let's move them individually
        for (View v : mIntersectingViews) {
            if (!addViewToTempLocation(v, mOccupiedRect, direction, solution)) {
                return false;
            }
        }
        return true;
    }
    
    public ItemConfiguration simpleSwap(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX,
            int spanY, int[] direction, View dragView, boolean decX, ItemConfiguration solution) {
        // Copy the current state into the solution. This solution will be manipulated as necessary.
        copyCurrentStateToSolution(solution, false);
        // Copy the current occupied array into the temporary occupied array. This array will be
        // manipulated as necessary to find a solution.
        copyOccupiedArray(mTmpOccupied);

        // We find the nearest cell into which we would place the dragged item, assuming there's
        // nothing in its way.
        int result[] = new int[2];
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);

        boolean success = false;
        // First we try the exact nearest position of the item being dragged,
        // we will then want to try to move this around to other neighbouring positions
        success = rearrangementExists(result[0], result[1], spanX, spanY, direction, dragView,
                solution);

        if (!success) {
            // We try shrinking the widget down to size in an alternating pattern, shrink 1 in
            // x, then 1 in y etc.
            if (spanX > minSpanX && (minSpanY == spanY || decX)) {
                return simpleSwap(pixelX, pixelY, minSpanX, minSpanY, spanX - 1, spanY, direction,
                        dragView, false, solution);
            } else if (spanY > minSpanY) {
                return simpleSwap(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY - 1, direction,
                        dragView, true, solution);
            }
            solution.isSolution = false;
        } else {
            solution.isSolution = true;
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = spanX;
            solution.dragViewSpanY = spanY;
        }
        return solution;
    }

    protected void copyCurrentStateToSolution(ItemConfiguration solution, boolean temp) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            CellAndSpan c;
            if (temp) {
                c = new CellAndSpan(lp.tmpCellX, lp.tmpCellY, lp.cellHSpan, lp.cellVSpan);
            } else {
                c = new CellAndSpan(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan);
            }
            solution.add(child, c);
        }
    }

    protected void copySolutionToTempState(ItemConfiguration solution, View dragView) {
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                mTmpOccupied[i][j] = false;
            }
        }

        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                lp.tmpCellX = c.x;
                lp.tmpCellY = c.y;
                lp.cellHSpan = c.spanX;
                lp.cellVSpan = c.spanY;
                markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
            }
        }
        markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX,
                solution.dragViewSpanY, mTmpOccupied, true);
    }

    private void animateItemsToSolution(ItemConfiguration solution, View dragView, boolean
            commitDragView) {

        boolean[][] occupied = DESTRUCTIVE_REORDER ? mOccupied : mTmpOccupied;
        if(occupied != null){
	        for (int i = 0; i < mCountX; i++) {
	            for (int j = 0; j < mCountY; j++) {
	                occupied[i][j] = false;
	            }
	        }
        }

        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                animateChildToPosition(child, c.x, c.y, REORDER_ANIMATION_DURATION, 0,
                        DESTRUCTIVE_REORDER, false);
                markCellsForView(c.x, c.y, c.spanX, c.spanY, occupied, true);
            }
        }
        if (commitDragView) {
            markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX,
                    solution.dragViewSpanY, occupied, true);
        }
    }
    
    public void setUseTempCoords(boolean useTempCoords) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) mShortcutsAndWidgets.getChildAt(i).getLayoutParams();
            lp.useTmpCoords = useTempCoords;
        }
    }

    public ItemConfiguration findConfigurationNoShuffle(int pixelX, int pixelY, int minSpanX, int minSpanY,
            int spanX, int spanY, View dragView, ItemConfiguration solution) {
        int[] result = new int[2];
        int[] resultSpan = new int[2];
        findNearestVacantArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, null, result,
                resultSpan);
        if (result[0] >= 0 && result[1] >= 0) {
            copyCurrentStateToSolution(solution, false);
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = resultSpan[0];
            solution.dragViewSpanY = resultSpan[1];
            solution.isSolution = true;
        } else {
            solution.isSolution = false;
        }
        return solution;
    }
    
    public void revertTempState() {
        if (!isItemPlacementDirty() || DESTRUCTIVE_REORDER) return;
        final int count = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY) {
                lp.tmpCellX = lp.cellX;
                lp.tmpCellY = lp.cellY;
                animateChildToPosition(child, lp.cellX, lp.cellY, REORDER_ANIMATION_DURATION,
                        0, false, false);
            }
        }
        completeAndClearReorderHintAnimations();
        setItemPlacementDirty(false);
    }

    public boolean createAreaForResize(int cellX, int cellY, int spanX, int spanY,
            View dragView, int[] direction, boolean commit) {
        int[] pixelXY = new int[2];
        regionToCenterPoint(cellX, cellY, spanX, spanY, pixelXY);

        // First we determine if things have moved enough to cause a different layout
        ItemConfiguration swapSolution = simpleSwap(pixelXY[0], pixelXY[1], spanX, spanY,
                 spanX,  spanY, direction, dragView,  true,  new ItemConfiguration());

        setUseTempCoords(true);
        if (swapSolution != null && swapSolution.isSolution) {
            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            copySolutionToTempState(swapSolution, dragView);
            setItemPlacementDirty(true);
            animateItemsToSolution(swapSolution, dragView, commit);

            if (commit) {
                commitTempPlacement();
                completeAndClearReorderHintAnimations();
                setItemPlacementDirty(false);
            } else {
                beginOrAdjustHintAnimations(swapSolution, dragView,
                        REORDER_ANIMATION_DURATION);
            }
            mShortcutsAndWidgets.requestLayout();
        }
        return swapSolution.isSolution;
    }

    public int[] createArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
            View dragView, int[] result, int resultSpan[], int mode) {
        // First we determine if things have moved enough to cause a different layout
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);

        if (resultSpan == null) {
            resultSpan = new int[2];
        }

        // When we are checking drop validity or actually dropping, we don't recompute the
        // direction vector, since we want the solution to match the preview, and it's possible
        // that the exact position of the item has changed to result in a new reordering outcome.
        if ((mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL || mode == MODE_ACCEPT_DROP)
               && mPreviousReorderDirection[0] != INVALID_DIRECTION) {
            mDirectionVector[0] = mPreviousReorderDirection[0];
            mDirectionVector[1] = mPreviousReorderDirection[1];
            // We reset this vector after drop
            if (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                mPreviousReorderDirection[0] = INVALID_DIRECTION;
                mPreviousReorderDirection[1] = INVALID_DIRECTION;
            }
        } else {
            getDirectionVectorForDrop(pixelX, pixelY, spanX, spanY, dragView, mDirectionVector);
            mPreviousReorderDirection[0] = mDirectionVector[0];
            mPreviousReorderDirection[1] = mDirectionVector[1];
        }

        ItemConfiguration swapSolution = simpleSwap(pixelX, pixelY, minSpanX, minSpanY,
                 spanX,  spanY, mDirectionVector, dragView,  true,  new ItemConfiguration());

        // We attempt the approach which doesn't shuffle views at all
        ItemConfiguration noShuffleSolution = findConfigurationNoShuffle(pixelX, pixelY, minSpanX,
                minSpanY, spanX, spanY, dragView, new ItemConfiguration());

        ItemConfiguration finalSolution = null;
        if (swapSolution.isSolution && swapSolution.area() >= noShuffleSolution.area()) {
            finalSolution = swapSolution;
        } else if (noShuffleSolution.isSolution) {
            finalSolution = noShuffleSolution;
        }

        boolean foundSolution = true;
        if (!DESTRUCTIVE_REORDER) {
            setUseTempCoords(true);
        }

        if (finalSolution != null) {
            result[0] = finalSolution.dragViewX;
            result[1] = finalSolution.dragViewY;
            resultSpan[0] = finalSolution.dragViewSpanX;
            resultSpan[1] = finalSolution.dragViewSpanY;

            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            if (mode == MODE_DRAG_OVER || mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                if (!DESTRUCTIVE_REORDER) {
                    copySolutionToTempState(finalSolution, dragView);
                }
                setItemPlacementDirty(true);
                animateItemsToSolution(finalSolution, dragView, mode == MODE_ON_DROP);

                if (!DESTRUCTIVE_REORDER &&
                        (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL)) {
                    commitTempPlacement();
                    completeAndClearReorderHintAnimations();
                    setItemPlacementDirty(false);
                } else {
                    beginOrAdjustHintAnimations(finalSolution, dragView,
                            REORDER_ANIMATION_DURATION);
                }
            }
        } else {
            foundSolution = false;
            result[0] = result[1] = resultSpan[0] = resultSpan[1] = -1;
        }

        if ((mode == MODE_ON_DROP || !foundSolution) && !DESTRUCTIVE_REORDER) {
            setUseTempCoords(false);
        }

        mShortcutsAndWidgets.requestLayout();
        return result;
    }

    public void setItemPlacementDirty(boolean dirty) {
        mItemPlacementDirty = dirty;
    }
    public boolean isItemPlacementDirty() {
        return mItemPlacementDirty;
    }

    // This method starts or changes the reorder hint animations
    protected void beginOrAdjustHintAnimations(ItemConfiguration solution, View dragView, int delay) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (c != null) {
                ReorderHintAnimation rha = new ReorderHintAnimation(child, lp.cellX, lp.cellY,
                        c.x, c.y, c.spanX, c.spanY);
                rha.animate();
            }
        }
    }
    
    protected class ItemConfiguration {
        public HashMap<View, CellAndSpan> map = new HashMap<View, CellAndSpan>();
        private HashMap<View, CellAndSpan> savedMap = new HashMap<View, CellAndSpan>();
        public ArrayList<View> sortedViews = new ArrayList<View>();
        public boolean isSolution = false;
        public int dragViewX, dragViewY, dragViewSpanX, dragViewSpanY;

        public void save() {
            // Copy current state into savedMap
            for (View v: map.keySet()) {
                map.get(v).copy(savedMap.get(v));
            }
        }

        public void restore() {
            // Restore current state from savedMap
            for (View v: savedMap.keySet()) {
                savedMap.get(v).copy(map.get(v));
            }
        }

        public void add(View v, CellAndSpan cs) {
            map.put(v, cs);
            savedMap.put(v, new CellAndSpan());
            sortedViews.add(v);
        }

        public int area() {
            return dragViewSpanX * dragViewSpanY;
        }
    }

    protected class CellAndSpan {
    	public int x, y;
    	public int spanX, spanY;

        public CellAndSpan() {
        }

        public void copy(CellAndSpan copy) {
            copy.x = x;
            copy.y = y;
            copy.spanX = spanX;
            copy.spanY = spanY;
        }

        public CellAndSpan(int x, int y, int spanX, int spanY) {
            this.x = x;
            this.y = y;
            this.spanX = spanX;
            this.spanY = spanY;
        }

        public String toString() {
            return "(" + x + ", " + y + ": " + spanX + ", " + spanY + ")";
        }
    }
    
    // Class which represents the reorder hint animations. These animations show that an item is
    // in a temporary state, and hint at where the item will return to.
    public class ReorderHintAnimation {
    	public View child;
    	public float finalDeltaX;
    	public float finalDeltaY;
    	public float initDeltaX;
    	public float initDeltaY;
    	public float finalScale;
    	public float initScale;
        private static final int DURATION = 300;
        public Animator a;

        public ReorderHintAnimation(View child, int cellX0, int cellY0, int cellX1, int cellY1,
                int spanX, int spanY) {
            regionToCenterPoint(cellX0, cellY0, spanX, spanY, mTmpPoint);
            final int x0 = mTmpPoint[0];
            final int y0 = mTmpPoint[1];
            regionToCenterPoint(cellX1, cellY1, spanX, spanY, mTmpPoint);
            final int x1 = mTmpPoint[0];
            final int y1 = mTmpPoint[1];
            final int dX = x1 - x0;
            final int dY = y1 - y0;
            finalDeltaX = 0;
            finalDeltaY = 0;
            if (dX == dY && dX == 0) {
            } else {
                if (dY == 0) {
                    finalDeltaX = - Math.signum(dX) * mReorderHintAnimationMagnitude;
                } else if (dX == 0) {
                    finalDeltaY = - Math.signum(dY) * mReorderHintAnimationMagnitude;
                } else {
                    double angle = Math.atan( (float) (dY) / dX);
                    finalDeltaX = (int) (- Math.signum(dX) *
                            Math.abs(Math.cos(angle) * mReorderHintAnimationMagnitude));
                    finalDeltaY = (int) (- Math.signum(dY) *
                            Math.abs(Math.sin(angle) * mReorderHintAnimationMagnitude));
                }
            }
            initDeltaX = child.getTranslationX();
            initDeltaY = child.getTranslationY();
            finalScale = getChildrenScale() - 4.0f / child.getWidth();
            initScale = child.getScaleX();
            this.child = child;
        }

        public void animate() {
            if (mShakeAnimators.containsKey(child)) {
                ReorderHintAnimation oldAnimation = mShakeAnimators.get(child);
                oldAnimation.cancel();
                mShakeAnimators.remove(child);
                if (finalDeltaX == 0 && finalDeltaY == 0) {
                    completeAnimationImmediately();
                    return;
                }
            }
            if (finalDeltaX == 0 && finalDeltaY == 0) {
                return;
            }
            ValueAnimator va = LauncherAnimUtils.ofFloat(0f, 1f);
            a = va;
            va.setRepeatMode(ValueAnimator.REVERSE);
            va.setRepeatCount(ValueAnimator.INFINITE);
            va.setDuration(DURATION);
            va.setStartDelay((int) (Math.random() * 60));
            va.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = ((Float) animation.getAnimatedValue()).floatValue();
                    float x = r * finalDeltaX + (1 - r) * initDeltaX;
                    float y = r * finalDeltaY + (1 - r) * initDeltaY;
                    child.setTranslationX(x);
                    child.setTranslationY(y);
                    float s = r * finalScale + (1 - r) * initScale;
                    child.setScaleX(s);
                    child.setScaleY(s);
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                public void onAnimationRepeat(Animator animation) {
                    // We make sure to end only after a full period
                    initDeltaX = 0;
                    initDeltaY = 0;
                    initScale = getChildrenScale();
                }
            });
            mShakeAnimators.put(child, this);
            va.start();
        }

        private void cancel() {
            if (a != null) {
                a.cancel();
            }
        }

        private void completeAnimationImmediately() {
            if (a != null) {
                a.cancel();
            }

            AnimatorSet s = LauncherAnimUtils.createAnimatorSet();
            a = s;
            s.playTogether(
                LauncherAnimUtils.ofFloat(child, "scaleX", getChildrenScale()),
                LauncherAnimUtils.ofFloat(child, "scaleY", getChildrenScale()),
                LauncherAnimUtils.ofFloat(child, "translationX", 0f),
                LauncherAnimUtils.ofFloat(child, "translationY", 0f)
            );
            s.setDuration(REORDER_ANIMATION_DURATION);
            s.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
            s.start();
        }
    }
    
    /**
     * This helper class defines a cluster of views. It helps with defining complex edges
     * of the cluster and determining how those edges interact with other views. The edges
     * essentially define a fine-grained boundary around the cluster of views -- like a more
     * precise version of a bounding box.
     */
    protected class ViewCluster {
        public final static int LEFT = 0;
        public final static int TOP = 1;
        public final static int RIGHT = 2;
        public final static int BOTTOM = 3;

        public ArrayList<View> views;
        public ItemConfiguration config;
        public Rect boundingRect = new Rect();

        public int[] leftEdge = new int[mCountY];
        public int[] rightEdge = new int[mCountY];
        public int[] topEdge = new int[mCountX];
        public int[] bottomEdge = new int[mCountX];
        public boolean leftEdgeDirty, rightEdgeDirty, topEdgeDirty, bottomEdgeDirty, boundingRectDirty;

        @SuppressWarnings("unchecked")
        public ViewCluster(ArrayList<View> views, ItemConfiguration config) {
            this.views = (ArrayList<View>) views.clone();
            this.config = config;
            resetEdges();
        }

        public void resetEdges() {
            for (int i = 0; i < mCountX; i++) {
                topEdge[i] = -1;
                bottomEdge[i] = -1;
            }
            for (int i = 0; i < mCountY; i++) {
                leftEdge[i] = -1;
                rightEdge[i] = -1;
            }
            leftEdgeDirty = true;
            rightEdgeDirty = true;
            bottomEdgeDirty = true;
            topEdgeDirty = true;
            boundingRectDirty = true;
        }

        public void computeEdge(int which, int[] edge) {
            int count = views.size();
            int size;// = edge.length;
            for (int i = 0; i < count; i++) {
                CellAndSpan cs = config.map.get(views.get(i));
                switch (which) {
                    case LEFT:
                        int left = cs.x;
                        size = Math.min(cs.y + cs.spanY, edge.length);
                        for (int j = cs.y; j < size; j++) {
                            if (left < edge[j] || edge[j] < 0) {
                                edge[j] = left;
                            }
                        }
                        break;
                    case RIGHT:
                        int right = cs.x + cs.spanX;
                        size = Math.min(cs.y + cs.spanY, edge.length);
                        for (int j = cs.y; j < size; j++) {
                            if (right > edge[j]) {
                                edge[j] = right;
                            }
                        }
                        break;
                    case TOP:
                        int top = cs.y;
                        size = Math.min(cs.x + cs.spanX, edge.length);
                        for (int j = cs.x; j < size; j++) {
                            if (top < edge[j] || edge[j] < 0) {
                                edge[j] = top;
                            }
                        }
                        break;
                    case BOTTOM:
                        int bottom = cs.y + cs.spanY;
                        size = Math.min(cs.x + cs.spanX, edge.length);
                        for (int j = cs.x; j < size; j++) {
                            if (bottom > edge[j]) {
                                edge[j] = bottom;
                            }
                        }
                        break;
                }
            }
        }

        public boolean isViewTouchingEdge(View v, int whichEdge) {
            CellAndSpan cs = config.map.get(v);

            int[] edge = getEdge(whichEdge);
            int size = edge.length;
            switch (whichEdge) {
                case LEFT:
                    size = Math.min(cs.y + cs.spanY, size);
                    for (int i = cs.y; i < size; i++) {
                        if (edge[i] == cs.x + cs.spanX) {
                            return true;
                        }
                    }
                    break;
                case RIGHT:
                    size = Math.min(cs.y + cs.spanY, size);
                    for (int i = cs.y; i < size; i++) {
                        if (edge[i] == cs.x) {
                            return true;
                        }
                    }
                    break;
                case TOP:
                    size = Math.min(cs.x + cs.spanX, size);
                    for (int i = cs.x; i < size; i++) {
                        if (edge[i] == cs.y + cs.spanY) {
                            return true;
                        }
                    }
                    break;
                case BOTTOM:
                    size = Math.min(cs.x + cs.spanX, size);
                    for (int i = cs.x; i < size; i++) {
                        if (edge[i] == cs.y) {
                            return true;
                        }
                    }
                    break;
            }
            return false;
        }

        public void shift(int whichEdge, int delta) {
            for (View v: views) {
                CellAndSpan c = config.map.get(v);
                switch (whichEdge) {
                    case LEFT:
                        c.x -= delta;
                        break;
                    case RIGHT:
                        c.x += delta;
                        break;
                    case TOP:
                        c.y -= delta;
                        break;
                    case BOTTOM:
                    default:
                        c.y += delta;
                        break;
                }
            }
            resetEdges();
        }

        public void addView(View v) {
            views.add(v);
            resetEdges();
        }

        public Rect getBoundingRect() {
            if (boundingRectDirty) {
                boolean first = true;
                for (View v: views) {
                    CellAndSpan c = config.map.get(v);
                    if (first) {
                        boundingRect.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                        first = false;
                    } else {
                        boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                    }
                }
            }
            return boundingRect;
        }

        public int[] getEdge(int which) {
            switch (which) {
                case LEFT:
                    return getLeftEdge();
                case RIGHT:
                    return getRightEdge();
                case TOP:
                    return getTopEdge();
                case BOTTOM:
                default:
                    return getBottomEdge();
            }
        }

        public int[] getLeftEdge() {
            if (leftEdgeDirty) {
                computeEdge(LEFT, leftEdge);
            }
            return leftEdge;
        }

        public int[] getRightEdge() {
            if (rightEdgeDirty) {
                computeEdge(RIGHT, rightEdge);
            }
            return rightEdge;
        }

        public int[] getTopEdge() {
            if (topEdgeDirty) {
                computeEdge(TOP, topEdge);
            }
            return topEdge;
        }

        public int[] getBottomEdge() {
            if (bottomEdgeDirty) {
                computeEdge(BOTTOM, bottomEdge);
            }
            return bottomEdge;
        }

        public PositionComparator comparator = new PositionComparator();
        public class PositionComparator implements Comparator<View> {
            int whichEdge = 0;
            public int compare(View left, View right) {
                CellAndSpan l = config.map.get(left);
                CellAndSpan r = config.map.get(right);
                switch (whichEdge) {
                    case LEFT:
                        return (r.x + r.spanX) - (l.x + l.spanX);
                    case RIGHT:
                        return l.x - r.x;
                    case TOP:
                        return (r.y + r.spanY) - (l.y + l.spanY);
                    case BOTTOM:
                    default:
                        return l.y - r.y;
                }
            }
        }

        public void sortConfigurationForEdgePush(int edge) {
            comparator.whichEdge = edge;
            Collections.sort(config.sortedViews, comparator);
        }
    }
}
