package com.jzs.dr.mtplauncher.style.ics.widget;

import java.util.Arrays;

import com.jzs.dr.mtplauncher.sjar.ctrl.InterruptibleInOutAnimator;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.widget.CellLayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.jzs.dr.mtplauncher.style.ics.R;

public class CellLayoutDefault extends CellLayout {
	protected final static String TAG = "CellLayoutDefault";
	
	private final Point mDragCenter = new Point();
	// These arrays are used to implement the drag visualization on x-large screens.
    // They are used as circular arrays, indexed by mDragOutlineCurrent.
    private Rect[] mDragOutlines = new Rect[4];
    private float[] mDragOutlineAlphas = new float[mDragOutlines.length];
    private InterruptibleInOutAnimator[] mDragOutlineAnims =
            new InterruptibleInOutAnimator[mDragOutlines.length];

    // Used as an index into the above 3 arrays; indicates which is the most current value.
    private int mDragOutlineCurrent = 0;
    private final Paint mDragOutlinePaint = new Paint();
    
    private TimeInterpolator mEaseOutInterpolator;
    
 // When a drag operation is in progress, holds the nearest cell to the touch point
    private final int[] mDragCell = new int[2];
	
	public CellLayoutDefault(Context context) {
        this(context, null);
    }

    public CellLayoutDefault(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLayoutDefault(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        final Resources res = context.getResources();
        
        // Initialize the data structures used for the drag visualization.

        mEaseOutInterpolator = new DecelerateInterpolator(2.5f); // Quint ease out
        
        mDragCell[0] = mDragCell[1] = -1;
        for (int i = 0; i < mDragOutlines.length; i++) {
            mDragOutlines[i] = new Rect(-1, -1, -1, -1);
        }

        // When dragging things around the home screens, we show a green outline of
        // where the item will land. The outlines gradually fade out, leaving a trail
        // behind the drag path.
        // Set up all the animations that are used to implement this fading.
        final int duration = res.getInteger(R.integer.config_dragOutlineFadeTime);
        final float fromAlphaValue = 0;
        final float toAlphaValue = (float)res.getInteger(R.integer.config_dragOutlineMaxAlpha);

        Arrays.fill(mDragOutlineAlphas, fromAlphaValue);

        for (int i = 0; i < mDragOutlineAnims.length; i++) {
            final InterruptibleInOutAnimator anim =
                new InterruptibleInOutAnimator(duration, fromAlphaValue, toAlphaValue);
            anim.getAnimator().setInterpolator(mEaseOutInterpolator);
            final int thisIndex = i;
            anim.getAnimator().addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final Bitmap outline = (Bitmap)anim.getTag();

                    // If an animation is started and then stopped very quickly, we can still
                    // get spurious updates we've cleared the tag. Guard against this.
                    if (outline == null) {
                        @SuppressWarnings("all") // suppress dead code warning
                        final boolean debug = false;
                        if (debug) {
                            Object val = animation.getAnimatedValue();
                            Util.Log.d(TAG, "anim " + thisIndex + " update: " + val +
                                     ", isStopped " + anim.isStopped());
                        }
                        // Try to prevent it from continuing to run
                        animation.cancel();
                    } else {
                        mDragOutlineAlphas[thisIndex] = (Float) animation.getAnimatedValue();
                        CellLayoutDefault.this.invalidate(mDragOutlines[thisIndex]);
                    }
                }
            });
            // The animation holds a reference to the drag outline bitmap as long is it's
            // running. This way the bitmap can be GCed when the animations are complete.
            anim.getAnimator().addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if ((Float) ((ValueAnimator) animation).getAnimatedValue() == 0f) {
                        anim.setTag(null);
                    }
                }
            });
            mDragOutlineAnims[i] = anim;
        }
    }
    
    public void visualizeDropLocation(View v, Bitmap dragOutline, int originX, int originY, int cellX,
            int cellY, int spanX, int spanY, boolean resize, Point dragOffset, Rect dragRegion) {
        final int oldDragCellX = mDragCell[0];
        final int oldDragCellY = mDragCell[1];

        if (v != null && dragOffset == null) {
            mDragCenter.set(originX + (v.getWidth() / 2), originY + (v.getHeight() / 2));
        } else {
            mDragCenter.set(originX, originY);
        }

        if (dragOutline == null && v == null) {
            return;
        }

        if (cellX != oldDragCellX || cellY != oldDragCellY) {
            mDragCell[0] = cellX;
            mDragCell[1] = cellY;
            // Find the top left corner of the rect the object will occupy
            final int[] topLeft = mTmpPoint;
            cellToPoint(cellX, cellY, topLeft);

            int left = topLeft[0];
            int top = topLeft[1];

            if (v != null && dragOffset == null) {
                // When drawing the drag outline, it did not account for margin offsets
                // added by the view's parent.
                MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
                left += lp.leftMargin;
                top += lp.topMargin;

                // Offsets due to the size difference between the View and the dragOutline.
                // There is a size difference to account for the outer blur, which may lie
                // outside the bounds of the view.
                top += (v.getHeight() - dragOutline.getHeight()) / 2;
                // We center about the x axis
                left += ((mCellWidth * spanX) + ((spanX - 1) * mWidthGap)
                        - dragOutline.getWidth()) / 2;
            } else {
                if (dragOffset != null && dragRegion != null) {
                    // Center the drag region *horizontally* in the cell and apply a drag
                    // outline offset
                    left += dragOffset.x + ((mCellWidth * spanX) + ((spanX - 1) * mWidthGap)
                             - dragRegion.width()) / 2;
                    top += dragOffset.y;
                } else {
                    // Center the drag outline in the cell
                    left += ((mCellWidth * spanX) + ((spanX - 1) * mWidthGap)
                            - dragOutline.getWidth()) / 2;
                    top += ((mCellHeight * spanY) + ((spanY - 1) * mHeightGap)
                            - dragOutline.getHeight()) / 2;
                }
            }
            final int oldIndex = mDragOutlineCurrent;
            mDragOutlineAnims[oldIndex].animateOut();
            mDragOutlineCurrent = (oldIndex + 1) % mDragOutlines.length;
            Rect r = mDragOutlines[mDragOutlineCurrent];
            r.set(left, top, left + dragOutline.getWidth(), top + dragOutline.getHeight());
            if (resize) {
                cellToRect(cellX, cellY, spanX, spanY, r);
            }

            mDragOutlineAnims[mDragOutlineCurrent].setTag(dragOutline);
            mDragOutlineAnims[mDragOutlineCurrent].animateIn();
        }
    }

    public void clearDragOutlines() {
        final int oldIndex = mDragOutlineCurrent;
        mDragOutlineAnims[oldIndex].animateOut();
        mDragCell[0] = mDragCell[1] = -1;
    }
    
    @Override
    protected void drawDragOutlinePaint(Canvas canvas){
		final Paint paint = mDragOutlinePaint;
		for (int i = 0; i < mDragOutlines.length; i++) {
			final float alpha = mDragOutlineAlphas[i];
			if (alpha > 0) {
				final Rect r = mDragOutlines[i];
				scaleRectAboutCenter(r, temp, getChildrenScale());
				final Bitmap b = (Bitmap) mDragOutlineAnims[i].getTag();
				paint.setAlpha((int) (alpha + .5f));
				canvas.drawBitmap(b, null, temp, paint);
			}
		}
    }
    
    @Override
    public void onDragExit() {
    	super.onDragExit();
    	if (Util.DEBUG_DRAG) {
    		Util.Log.d(TAG, "onDragExit: mDragging = " + mDragging);
        }


        // Invalidate the drag data
        mDragCell[0] = mDragCell[1] = -1;
        mDragOutlineAnims[mDragOutlineCurrent].animateOut();
        mDragOutlineCurrent = (mDragOutlineCurrent + 1) % mDragOutlineAnims.length;
        revertTempState();
        setIsDragOverlapping(false);
    }
}
