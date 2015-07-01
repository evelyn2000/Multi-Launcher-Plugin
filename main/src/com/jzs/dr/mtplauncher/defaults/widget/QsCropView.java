package com.jzs.dr.mtplauncher.defaults.widget;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

import com.jzs.common.manager.IIconUtilities;
import com.jzs.dr.mtplauncher.R;

public class QsCropView extends View{
	
	private static final int COLOR_OUTLINE = 0xFF008AFF;

	private Paint mOutlinePaint = new Paint();
	private Paint mTitlePaint = new Paint();
	private Bitmap mBitmap;
	private Bitmap mCropImage;
	private Bitmap mPreviewBitmap;
	//private Bitmap mCacheBitmap;
	//private Bitmap mMaskBitmap;
	private Rect mImgRect = new Rect();
	private Rect mOutlineRect = new Rect();
	private Rect mCropImageTopRec;
	private Rect mCropImageBottomRec;
	private Rect mCropImageLeftRec;
	private Rect mCropImageRightRec;
	private int mCropImgWidth = 0;
	private int mCropImgHeight = 0;
	private int APP_ICON_SIZE;
	private float mScale = 1.0f;
	private static int DELTA = 8;//dip
	private static int MIN_RECT_SIZE = 72;//dip
	private static int MAX_IMG_SIZE;
	private static final int PREVIEW_MARGIN = 6;
	private static final int OUTLINE_RECT_SIZE  = 200;
	private static final int REFRESH_ZOOM_DELAY_TIME = 200;	
	private static final float MAX_IMG_HEIGHT_SCALE = 1.3f;	
	private String mTitle;
	private ArrayList<ImageView> mPreviewList;
	private Handler mHandler = new Handler();
	
	private IIconUtilities mIconUtils;

	private OnTouchMoveListener mTouchMoveListener;
	private OnZoomChangedListener mOnZoomChangedListener;
	
	Runnable mRefreshZoomRunnable = new Runnable(){
		@Override
		public void run() {
    		refreshZoomEnable();	
		}
	};
	
	private final static boolean DEBUG = false;
	    
	public QsCropView(Context context) {
        this(context, null);
    }

    public QsCropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //init();
        
        mImgRect.setEmpty();
        mOutlineRect.setEmpty();

        mOutlinePaint.setColor(COLOR_OUTLINE);
        //mOutlinePaint.setStrokeWidth(2F * context.getResources().getDisplayMetrics().density);
        mOutlinePaint.setStrokeWidth(3.0f);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setAntiAlias(true);
        
        mTitlePaint.setColor(0xFFFFFFFF);
        mTitlePaint.setAntiAlias(true);
        mTitlePaint.setTextSize((getResources().getDisplayMetrics().density)* 16);
        
        mCropImage = drawable2Bitmap(context.getResources().getDrawable(R.drawable.camera_crop_holo));
        Resources res = getResources();
        DELTA = res.getDimensionPixelSize(R.dimen.qs_crop_icon_touch_delta);
        MIN_RECT_SIZE = res.getDimensionPixelSize(R.dimen.qs_crop_icon_min_size);
        APP_ICON_SIZE = res.getDimensionPixelSize(R.dimen.app_icon_size);
        DisplayMetrics dm = res.getDisplayMetrics();
        MAX_IMG_SIZE = (int)(Math.max(dm.widthPixels, dm.heightPixels) * MAX_IMG_HEIGHT_SCALE);
    }
    
    @Override
    protected void onDetachedFromWindow() {
        // TODO Auto-generated method stub
        super.onDetachedFromWindow();
        recycleCropImage();
        recycleBitmap();
        //recycleResizeBitmap();
        //we should called at finish save because save task is AsyncTask
    }

    public void setCropImageSize(int width, int height){
    	
    	if(mCropImgWidth != width || mCropImgHeight != height){
    		mCropImgWidth = width;
    		mCropImgHeight = height;
    	}
    }
    
    public Bitmap getCropedBitmap(){
    	if(mBitmap == null || mCropImgWidth == 0 || mCropImgHeight == 0 || mOutlineRect.isEmpty() )
    		return null;
    	mCropImgHeight = mCropImgWidth;
    	try {
	    	Bitmap bitmap = Bitmap  
	                .createBitmap(  
	                		mCropImgWidth,  
	                		mCropImgHeight,  
	                        Bitmap.Config.ARGB_8888);  
	    	
	    	bitmap.setDensity(mBitmap.getDensity());
	    	
	    	float scale = (float)mCropImgWidth/mOutlineRect.width();
	    	Rect srcRect = new Rect();
	    	srcRect.right = mCropImgWidth;
	    	srcRect.bottom = mCropImgHeight;
	    	
	    	Rect targetRect = new Rect(srcRect);
	    	
	    	if(DEBUG){
		    	android.util.Log.w("QsLog", "QsCropView::getCropedBitmap====scale:"+mScale
		    			+"==mOutlineRect:"+mOutlineRect.toString()
		    			+"==mImgRect:"+mImgRect.toString()
		    			+"==width:"+mBitmap.getWidth()
		    			+"==hidth:"+mBitmap.getHeight());
	    	}
	    	
	    	int deltaw = (int)((mOutlineRect.left - mImgRect.left) /mScale);
	        int deltah = (int)((mOutlineRect.top - mImgRect.top) /mScale);
	        
	        srcRect.offset(deltaw, deltah);
		 srcRect.right = (int)((mOutlineRect.right - mImgRect.left) /mScale);	
		 srcRect.bottom = (int)((mOutlineRect.bottom - mImgRect.top) /mScale);
	    	

	    	if(DEBUG){
		    	android.util.Log.w("QsLog", "QsCropView::getCropedBitmap===="
		    			+"==targetRect:"+targetRect.toString()
		    			+"==srcRect:"+srcRect.toString()
		    			+"==w:"+targetRect.width()
		    			+"==h:"+targetRect.height()
		    			+"==mCropImgWidth:"+mCropImgWidth
		    			+"==mCropImgWidth:"+mCropImgHeight
		    			+"==deltaw:"+deltaw
		    			+"==deltah:"+deltah);
	    	}

	        
	        Canvas canvas = new Canvas(bitmap);
	        
	        Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC));
            
            //canvas.drawBitmap(mBitmap, targetRect, null, paint);
            canvas.drawBitmap(mBitmap, srcRect, targetRect, paint);
            
	        //bm.recycle();
	        canvas.setBitmap(null);
        
	        return bitmap;
	        
    	} catch (OutOfMemoryError e) {
    		
        }
    	
    	return mBitmap;
    }
    
    public Rect getCropRectangle(){
    	return mOutlineRect;
    }
    
    public void setDrawable(int nResId){
    	setDrawable(super.getResources().getDrawable(nResId));
    }
    
    public void setDrawable(Drawable drawable){
    	if(drawable != null){
    		setBitmapInternal(drawable2Bitmap(drawable));
    	}
    }
    
    private void setBitmapInternal(Bitmap bmp){
    	setBitmap(bmp);
//    	if(mCacheBitmap != null){
//    		mCacheBitmap.recycle();
//    	}
//    	mCacheBitmap = bmp;//create not from app icon
    }
    
    public void setBitmap(Bitmap bmp){
    	if(mBitmap != null){
    		mBitmap.recycle();
    		mBitmap = null;
    	}

    	resetLayoutZoom();
    	
    	if(bmp != null){
			int bmpMaxSize = Math.max(bmp.getWidth(), bmp.getHeight());
	    	if(bmpMaxSize > MAX_IMG_SIZE){
	    		Matrix matrix = new Matrix();
	    		float scaleSize = 1.0f * MAX_IMG_SIZE / bmpMaxSize;
	    		matrix.postScale(scaleSize, scaleSize);
	    		mBitmap = Bitmap.createBitmap(bmp, 0, 0,
	    		        bmp.getWidth(),bmp.getHeight(), matrix, true);
	    		bmp.recycle();
	    	} else {
	    	    mBitmap = bmp;
	    	}
    	}
    	
    	if(DEBUG){
	    	android.util.Log.w("QsLog", "QsCropView::setBitmap====w:"+getMeasuredWidth()
	    			+"==h:"+getMeasuredHeight()
	    			+"==ow:"+super.getWidth()+"==oh:"+super.getHeight());
    	}
    	computeScale(super.getWidth(), super.getHeight());
    	
    	mHandler.postDelayed(mRefreshZoomRunnable, REFRESH_ZOOM_DELAY_TIME) ;
    	
    	super.invalidate();
    }
    
    private void computeScale(int width, int height){
    	if(mBitmap == null || width == 0 || height == 0)
        	return;
    	
    	mImgRect.setEmpty();
    	
    	mImgRect.right = mBitmap.getWidth();
    	mImgRect.bottom = mBitmap.getHeight();
    	if(DEBUG){
	    	android.util.Log.e("QsLog", "width:" + width + ", height:" + height);
	        android.util.Log.i("QsLog", "1.imgRect:" + mImgRect.left + ", " + mImgRect.top
	        		+ ", " + mImgRect.right + ", " + mImgRect.bottom);
        }
    	int deltaw = mImgRect.right - width;
        int deltah = mImgRect.bottom - height;
        if(DEBUG){
	        android.util.Log.w("QsLog", "QsCropView::computeScale(1)====deltaw:"+deltaw
	    			+"==deltah:"+deltah);
        }
        float scale = 1.0f;
        if (deltaw > 0 || deltah > 0) {
            if (deltaw > deltah) {
            	scale = width / (float)mImgRect.right;
            } else {
            	scale = height / (float)mImgRect.bottom;
            }
            mImgRect.right = (int)(mImgRect.right*scale);
            mImgRect.bottom = (int)(mImgRect.bottom*scale);
            deltaw = width - mImgRect.right;
            deltah = height - mImgRect.bottom;
        } else {
        	deltaw = 0 - deltaw;
        	deltah = 0 - deltah;
        }
        mScale = scale;	
        mImgRect.offset(deltaw/2, deltah/2);
        
        mOutlineRect.setEmpty();
        
        //int scaleSize = Math.max((int)((mImgRect.right-mImgRect.left)*scale), MIN_RECT_SIZE);
        mOutlineRect.right = MIN_RECT_SIZE;//scaleSize
        mOutlineRect.bottom = MIN_RECT_SIZE; //scaleSize
        if(DEBUG){
	        android.util.Log.i("QsLog", "2.mOutlineRect:" + mOutlineRect.left + ", " + mOutlineRect.top
	        		+ ", " + mOutlineRect.right + ", " + mOutlineRect.bottom);
        }
        deltaw = mImgRect.width() - mOutlineRect.right;
        deltah = mImgRect.height() - mOutlineRect.bottom;
        
        mOutlineRect.offset(deltaw/2, deltah/2);
        
        mOutlineRect.offset(mImgRect.left, mImgRect.top);
        if(DEBUG){
	        android.util.Log.w("QsLog", "QsCropView::computeScale====img:"+mImgRect.toString()
	    			+"==ow:"+mOutlineRect.toString()
	    			+"==scale:"+scale
	    			+"==mCropImgWidth:"+mCropImgWidth
	    			+"==mCropImgHeight:"+mCropImgHeight);
        }
        createPreviewBitmap();
    }
    
    @Override 
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	super.onSizeChanged(w, h, oldw, oldh);
    	if(DEBUG){
    		android.util.Log.w("QsLog", "QsCropView::onSizeChanged()====w:"+w+"==h:"+h+"==ow:"+oldw+"==oh:"+oldh);
    	}
    	computeScale(w, h);
    }
    
    @Override 
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if(mBitmap == null || mImgRect.isEmpty())
        	return;
        if(DEBUG){
        	android.util.Log.i("QsLog", "imgRect w:" + mImgRect.width() + " h:" + mImgRect.height());
        }
        canvas.drawBitmap(mBitmap, null, mImgRect, null);

        if(mTitle != null)
        	canvas.drawText(mTitle, APP_ICON_SIZE << 1 + PREVIEW_MARGIN , APP_ICON_SIZE >> 1, mTitlePaint);
        
        canvas.drawRect(mOutlineRect, mOutlinePaint);
        
        if(!mIsTouching){
        	int nOffsetX = mCropImage.getWidth()/2;
        	int nOffsetY = mCropImage.getHeight()/2;
        	// top center
        	canvas.drawBitmap(mCropImage, mOutlineRect.left + mOutlineRect.width()/2 - nOffsetX, 
        			mOutlineRect.top - nOffsetY, null);	
		mCropImageTopRec = new Rect(mOutlineRect.left + mOutlineRect.width()/2 - nOffsetX-DELTA,
			mOutlineRect.top - nOffsetY - DELTA,mOutlineRect.left + mOutlineRect.width()/2 + nOffsetX+DELTA,mOutlineRect.top + nOffsetY+DELTA);
        	
        	// left center
        	canvas.drawBitmap(mCropImage, mOutlineRect.left - nOffsetX, 
        			mOutlineRect.top + mOutlineRect.height()/2 - nOffsetY, null);

		mCropImageLeftRec = new Rect(mOutlineRect.left - nOffsetX - DELTA,
			mOutlineRect.top + mOutlineRect.height()/2 - nOffsetY -DELTA , mOutlineRect.left + nOffsetX,mOutlineRect.top + DELTA + mOutlineRect.height()/2 + nOffsetY + DELTA);
        	
        	// right center
        	canvas.drawBitmap(mCropImage, mOutlineRect.right - nOffsetX, 
        			mOutlineRect.top + mOutlineRect.height()/2 - nOffsetY, null);
 
		mCropImageRightRec = new Rect(mOutlineRect.right - nOffsetX - DELTA,
			mOutlineRect.top + mOutlineRect.height()/2 - nOffsetY -DELTA, mOutlineRect.right +nOffsetX + DELTA,mOutlineRect.top + mOutlineRect.height()/2 + nOffsetY+ DELTA);			
        	// bottom center
        	canvas.drawBitmap(mCropImage, mOutlineRect.left + mOutlineRect.width()/2 - nOffsetX, 
        			mOutlineRect.bottom - nOffsetY, null);

		mCropImageBottomRec = new Rect(mOutlineRect.left + mOutlineRect.width()/2 - nOffsetX - DELTA,
			mOutlineRect.bottom - nOffsetY - DELTA,mOutlineRect.left + mOutlineRect.width()/2 + nOffsetX+ DELTA,mOutlineRect.bottom + nOffsetY+ DELTA);	


		if(DEBUG){
		android.util.Log.w("QsLog", "mCropImageTopRec:"+mCropImageTopRec.toString()
				+"mCropImageLeftRec:"+mCropImageLeftRec.toString()
				+"mCropImageRightRec:"+mCropImageRightRec.toString()
				+"mCropImageBottomRec:"+mCropImageBottomRec.toString());
		}

        }
    }
    
    private boolean mIsTouching = false;
    private int mFirstTouchY = 0;
    private int mFirstTouchX = 0;
    private int mTouchPos = -1;
    
    @Override
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction();
		
        final int y = (int)event.getY();
        final int x = (int)event.getX();
		int nOffsetX = mCropImage.getWidth()/2;
		int nOffsetY = mCropImage.getHeight()/2;
        
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:{
		    
			mFirstTouchY = y;
            mFirstTouchX = x;
			int cropTopImageLeft =	mOutlineRect.left + mOutlineRect.width()/2 - nOffsetX;
			int cropTopImageBottom = mOutlineRect.top - nOffsetY;
			mTouchPos =-1;
			
			if(mCropImageTopRec == null)
				return false;

			if(DEBUG){
			        android.util.Log.w("QsLog", "QsCropView::onTouchEvent====:"+mCropImageTopRec.toString()
			    			+"==x:"+x
			    			+"==y:"+y);
       		 }

			if(mCropImageTopRec.contains(x, y))
			{
				mTouchPos = 0;	
			}
			else if(mCropImageLeftRec.contains(x, y))
			{
				mTouchPos = 1;
			}
			else if(mCropImageRightRec.contains(x, y))
			{
				mTouchPos = 2;
			}
			else if(mCropImageBottomRec.contains(x, y))
			{
				mTouchPos = 3;
			}
			else if(mOutlineRect.contains(x, y)){
		            mIsTouching = true;
		            invalidate();
			}
			if(DEBUG){
				        android.util.Log.w("QsLog", "ariel debug ---- onTouchEvent,mTouchPos:"+mTouchPos+"  mIsTouching:"+mIsTouching);
	       	}
			
 		}
			break;
        case MotionEvent.ACTION_MOVE:
		if(DEBUG){
		android.util.Log.w("QsLog", "ariel debug ---- MotionEvent.ACTION_MOVE");
		}

			//let Crop image is in middle of the Outline Rect
			boolean IsBiggerW = mOutlineRect.width() > mImgRect.width();
			boolean IsBiggerH = mOutlineRect.height() > mImgRect.height();
		
            if(mIsTouching){
            	
             	int deltaX = (mOutlineRect.left <= mImgRect.left && mOutlineRect.right >= mImgRect.right) ? 0 : (x - mFirstTouchX);
            	int deltaY = (mOutlineRect.top <= mImgRect.top && mOutlineRect.bottom >= mImgRect.bottom) ? 0 : (y - mFirstTouchY);
            	
            	if(deltaX == 0 && deltaY == 0)
            		break;
            	
            	Rect dstRect = new Rect(mOutlineRect);
            	dstRect.offsetTo(x - dstRect.width()/2, y - dstRect.height()/2);

            	if(!isInImgRect(dstRect)){
            		if(dstRect.left < mImgRect.left){
            			dstRect.left = IsBiggerW ? 
            					(mImgRect.left + mImgRect.width()/2 - mOutlineRect.width()/2) : 
            					mImgRect.left;
            			dstRect.right = dstRect.left + mOutlineRect.width();

            		}else if(dstRect.right > mImgRect.right){
            			dstRect.right = IsBiggerW ? 
                    			(mImgRect.right - mImgRect.width()/2 + mOutlineRect.width()/2) : 
                    			mImgRect.right;
            			dstRect.left = dstRect.right - mOutlineRect.width();
            			if(DEBUG){
	            			android.util.Log.w("QsLog", "1.dstRect: " + dstRect.left + " ," + dstRect.top + " ,"
	                    			+ dstRect.right  + " ," + dstRect.bottom);
            			}
            		}
            		if(dstRect.top < mImgRect.top){
            			dstRect.top = IsBiggerH ? 
            					(mImgRect.top + mImgRect.height()/2 - mOutlineRect.height()/2) : 
            					mImgRect.top;
            			dstRect.bottom = dstRect.top + mOutlineRect.height();
            			if(DEBUG){
	            			android.util.Log.w("QsLog", "1.dstRect: " + dstRect.left + " ," + dstRect.top + " ,"
	                    			+ dstRect.right  + " ," + dstRect.bottom);
            			}
            		}else if(dstRect.bottom > mImgRect.bottom){
            			dstRect.bottom = IsBiggerH ? 
            					(mImgRect.bottom - mImgRect.height()/2 + mOutlineRect.height()/2) : 
            					mImgRect.bottom;
            			dstRect.top = dstRect.bottom - mOutlineRect.height();
            			if(DEBUG){
	            			android.util.Log.w("QsLog", "1.dstRect: " + dstRect.left + " ," + dstRect.top + " ,"
	                    			+ dstRect.right  + " ," + dstRect.bottom);
            			}
            		}
            		mFirstTouchY = dstRect.top + mOutlineRect.height()/2;
            		mFirstTouchX = dstRect.left + mOutlineRect.width()/2;
            		mOutlineRect.set(dstRect);

            		invalidate();
               		return mIsTouching;
            	}
            	dstRect.offset(deltaX, deltaY);
//            	android.util.Log.d("QsLog", "(move)==1==deltaX:"+deltaX
//            			+"==deltaY:"+deltaY
//            			+"==:"+dstRect.toString());
            	
            	//if(mImgRect.contains(dstRect)){
            	if((deltaX != 0 && dstRect.left >= mImgRect.left && dstRect.right <= mImgRect.right)
            			|| (deltaY != 0 && dstRect.top >= mImgRect.top && dstRect.bottom <= mImgRect.bottom)){
            		mFirstTouchY = y;
                    mFirstTouchX = x;
            		mOutlineRect.set(dstRect);
            		invalidate();
            		break;
            	}
            	
            	deltaX = deltaY = 0;
            	if(dstRect.left < mImgRect.left){
            		deltaX = mImgRect.left - dstRect.left;
            	}
            	
            	if(dstRect.right > mImgRect.right){
            		if(deltaX != 0)
            			deltaX = 0;
            		else
            			deltaX = mImgRect.right - dstRect.right;
            	}
            	
            	if(dstRect.top < mImgRect.top){
            		deltaY = mImgRect.top - dstRect.top;
        		} 
            	if(dstRect.bottom > mImgRect.bottom){
            		if(deltaY != 0)
            			deltaY = 0;
            		else
            			deltaY = mImgRect.bottom - dstRect.bottom;
        		}
//            	android.util.Log.d("QsLog", "(move)==2==deltaX:"+deltaX
//            			+"==deltaY:"+deltaY
//            			+"==:"+dstRect.toString());
            	
            	if(deltaX != 0 || deltaY != 0){
            		
	            	dstRect.offset(deltaX, deltaY);
	            	if(!dstRect.equals(mOutlineRect)){
	            		mFirstTouchY = y;
	                    mFirstTouchX = x;
	            		mOutlineRect.set(dstRect);
	            		invalidate();
	            	}
            	}
            }
	     else if (mTouchPos == 0 && !IsBiggerH && !IsBiggerW) //top
	     {
			if(DEBUG){
				        android.util.Log.w("QsLog", "ariel debug ---- onTouchEvent,mImgRect:"+mImgRect.toString()+"  y:"+y);
	       	}
	     
			if((y<mOutlineRect.bottom) && (y>mImgRect.top))
			{
				if(mOutlineRect.bottom - y < MIN_RECT_SIZE){
					break;
				}
				mOutlineRect.top = y;
				int height_delta = mOutlineRect.bottom - mOutlineRect.top;
				mOutlineRect.left = mOutlineRect.right - height_delta;
				if(!isInImgRect()){
					{
						mOutlineRect.left = mImgRect.left;
						height_delta = mOutlineRect.width();
						mOutlineRect.top = mOutlineRect.bottom - height_delta;
					}
				}
				invalidate();
			}
	     }
	     else if (mTouchPos == 1 && !IsBiggerH && !IsBiggerW) //left
	     {	     
			if((x<mOutlineRect.right) && (x>mImgRect.left))
			{
				if(mOutlineRect.right - x < MIN_RECT_SIZE){
					break;
				}
				mOutlineRect.left = x;
				
				int width_delta = mOutlineRect.right - mOutlineRect.left;
				mOutlineRect.top = mOutlineRect.bottom - width_delta;
				if(!isInImgRect()){
					mOutlineRect.top = mImgRect.top;
					width_delta = mOutlineRect.height();
					mOutlineRect.left = mOutlineRect.right - width_delta;
				}
				invalidate();
			}
	     }
	     else if (mTouchPos == 2 && !IsBiggerH && !IsBiggerW) //right
	     {	     
			if((x<mImgRect.right) && (x>mOutlineRect.left))
			{
				if(x - mOutlineRect.left < MIN_RECT_SIZE){
					break;
				}
				mOutlineRect.right = x;
				
				int width_delta = mOutlineRect.right - mOutlineRect.left;
				mOutlineRect.bottom = mOutlineRect.top + width_delta;
				if(!isInImgRect()){
					{
						mOutlineRect.bottom = mImgRect.bottom;
						width_delta = mOutlineRect.height();
						mOutlineRect.right = mOutlineRect.left + width_delta;
					}
				}
				invalidate();
			}
	     }
	     else if (mTouchPos == 3 && !IsBiggerH && !IsBiggerW) //bottom
	     {
			if((y<mImgRect.bottom) && (y>mOutlineRect.top))
			{
				if(y - mOutlineRect.top< MIN_RECT_SIZE){
					break;
				}
				
				mOutlineRect.bottom = y;
				int height_delta = mOutlineRect.bottom - mOutlineRect.top;
				mOutlineRect.right = mOutlineRect.left + height_delta;
				
				if(!isInImgRect()){
					{
						mOutlineRect.right = mImgRect.right;
						height_delta = mOutlineRect.width();
						mOutlineRect.bottom = mOutlineRect.top + height_delta;
					}
				}
				invalidate();
			}
	     }
		 
            break;
        case MotionEvent.ACTION_UP:
            //android.util.Log.d("QsLog", "QsSlidingCtrlCircle::onTouchEvent(up)==mIsTouching:"+mIsTouching);
        case MotionEvent.ACTION_CANCEL:
            //Log.e("QsLog", "QsLockScreenIphone::onTouchEvent(up)==mIsTouching:"+mIsTouching+"==top:"+super.getTop()+"==height:"+super.getHeight());
        	createPreviewBitmap();
        	if(mIsTouching){
            	mIsTouching = false;
                
                invalidate();
            }
	     if(mTouchPos>=0)
	     {
			mTouchPos = -1;
			invalidate();
	     }
            break;
        default:
            break;
		}
		
		if(mTouchMoveListener != null){
			mTouchMoveListener.isOnTouchMove(mIsTouching || (mTouchPos>=0));
		}
        //Log.d("QsLog", "QsLockScreenIphone::onTouchEvent("+action+")==mIsTouching:"+mIsTouching+"==top:"+mChildView.getTop()+"==height:"+mChildView.getHeight());
		return mIsTouching || (mTouchPos>=0) || super.onTouchEvent(event);//mIsTouching;
	}
    
    private Bitmap drawable2Bitmap(Drawable drawable){
    	return drawable2Bitmap(drawable, 0, 0, 0);
    }
    
    private Bitmap drawable2Bitmap(Drawable drawable, int width, int height, int degree){  
    	Bitmap srcBitmap = ((BitmapDrawable)drawable).getBitmap();
    	if(width <= 0){
    		if(srcBitmap != null){
    			width = srcBitmap.getWidth();
    		}else{
    			width = drawable.getIntrinsicWidth();
    		}
    	}
    	
    	if(height <= 0){
    		if(srcBitmap != null){
    			height = srcBitmap.getHeight();
    		}else{
    			height = drawable.getIntrinsicHeight();
    		}
    	}
    	
    	//android.util.Log.e("QsLog", "drawable2Bitmap====width:"+width+"==height:"+height);
        Bitmap bitmap = Bitmap  
                .createBitmap(  
                		width,  
                		height,  
                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888  
                                : Bitmap.Config.RGB_565);  
        
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);  
        drawable.draw(canvas);  
        
    	if(degree != 0){
    		
    		Matrix matrix = new Matrix(); 
    		matrix.setRotate(degree);

    		Bitmap bitmapRotate = Bitmap.createBitmap(bitmap, 0, 0, 
    				bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    		
    		bitmap.recycle();
    		return bitmapRotate;
    	}
        return bitmap;  
    }
    
    private boolean isInImgRect(Rect dstRect){
    	return mImgRect.contains(dstRect);
    }
    
    private boolean isInImgRect(){
    	return isInImgRect(mOutlineRect);
    }
    
    private void createPreviewBitmap(){
        
        Bitmap bitmap = getCropedBitmap();
        
        recycleResizeBitmap();
		
        mPreviewBitmap = bitmap;
		
        if(mPreviewList != null){           
            int iconSize = APP_ICON_SIZE;
            
        	Bitmap bmpWithMask = null;
        	if(bitmap != null){
            	if(mIconUtils == null){
                    Matrix matrix = new Matrix();
                    float scaleSize = 1.0f * iconSize / bitmap.getWidth();
                    matrix.postScale(scaleSize, scaleSize);
                    bmpWithMask = Bitmap.createBitmap(bitmap, 0, 0,
                            bitmap.getWidth(),bitmap.getHeight(), matrix, true);
                } else {
                    bmpWithMask = mIconUtils.createIconBitmapWithMask(bitmap);
                }
        	}
        	
        	for(ImageView icon : mPreviewList){
                icon.setImageBitmap(bmpWithMask);
            }
        }
    }
    
    public void setTitle(String title){
    	mTitle = title + " " + getResources().getString(R.string.crop_view_preview);
    }
    
    public void setPreviewList(ArrayList<ImageView> previewIconList){
    	mPreviewList = previewIconList;
    }
    
    public Bitmap getResizeCropedBitmap(){
    	return mPreviewBitmap;
    }
    
    public void setIconUtils(IIconUtilities iconUtils){
    	mIconUtils = iconUtils;
    }
    
//    public void setMaskBitmap(Bitmap maskBitmap){
//    	mMaskBitmap = maskBitmap;
//    }
    
    public boolean canZoomInOut(boolean isZoomIn){
    	if(isZoomIn)
    		return (mScale != 1.0f && mBitmap != null) ? true : false;
    	else
       		return (mBitmap != null && mImgRect.height() > getMinimumHeight()) ? true : false;
    }
    
    public boolean isZoomIn(){
    	return canZoomInOut(false);
    }
    
    public void zoomIn(){
    	if(mBitmap != null){
    		final float zoomInScale = 1.0f * getResources().getDisplayMetrics().widthPixels / mImgRect.width();
    		FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)getLayoutParams();
    		android.util.Log.i("syq", "zoomInScale = " + zoomInScale);
    		if(zoomInScale > 1.0f){
    			params.height = (int)(getHeight() * zoomInScale);
    			setLayoutParams(params);
    			computeScale(getWidth()
        				,(int)(getHeight() * zoomInScale));
    			invalidate();
    		}
    		mOnZoomChangedListener.onZoomChanged(OnZoomChangedListener.MODE_ZOOM_OUT);
    	}
    }
    
    public void zoomOut(){
    	if(mBitmap != null){
    		resetLayoutZoom();
    		computeScale(getWidth(),getMinimumHeight());
    		invalidate();
    		refreshZoomEnable();
    		mOnZoomChangedListener.onZoomChanged(OnZoomChangedListener.MODE_ZOOM_IN);
    	}
    }
    
    private void resetLayoutZoom(){
    	if(getHeight() != getMinimumHeight()){
	    	int height = getMinimumHeight();
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)getLayoutParams();
			params.height = height;
			params.gravity = Gravity.CENTER_HORIZONTAL;
			setLayoutParams(params);
		}
    }
    
    private void refreshZoomEnable(){
    	if(mOnZoomChangedListener != null){
    		int flag = OnZoomChangedListener.MODE_ZOOM_NONE;
    		flag |= canZoomInOut(false) ? OnZoomChangedListener.MODE_ZOOM_OUT : 0;
    		flag |= canZoomInOut(true) ? OnZoomChangedListener.MODE_ZOOM_IN : 0;
    		mOnZoomChangedListener.onZoomChanged(flag);
    	}
    }
    
    public interface OnTouchMoveListener{
    	public void isOnTouchMove(boolean isMoving);
    }
    
    public interface OnZoomChangedListener{
    	public static final int MODE_ZOOM_NONE = 0;
    	public static final int MODE_ZOOM_IN = 1;
    	public static final int MODE_ZOOM_OUT = 2;
    	
    	public void onZoomChanged(int zoomMode);
    }
    
    public void setOnTouchMoveListener(OnTouchMoveListener listener){
    	mTouchMoveListener = listener;
    }
    
    public void setOnZoomChangedListener(OnZoomChangedListener listener){
    	mOnZoomChangedListener = listener;
    }
    
    private void recycleCropImage(){
    	if(mCropImage != null){
    		if(!mCropImage.isRecycled())
    			mCropImage.recycle();
            mCropImage = null;
        }
    }
    
    private void recycleBitmap(){
        if(mBitmap != null){
        	if(!mBitmap.isRecycled())
        		mBitmap.recycle();
        	mBitmap = null;
        }
    }
    
    public void recycleResizeBitmap(){
        if(mPreviewBitmap != null){
        	if(!mPreviewBitmap.isRecycled())
        		mPreviewBitmap.recycle();
        	mPreviewBitmap = null;
        }
    }
}
