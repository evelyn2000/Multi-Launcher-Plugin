package com.jzs.dr.mtplauncher.sjar.utils;

import java.util.Random;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.NinePatch;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;

import com.jzs.dr.mtplauncher.sjar.widget.FastBitmapDrawable;

public final class Util {

	public static final String TAG = "MtpLauncher";
	public final static boolean ENABLE_DEBUG = android.os.Build.TYPE.equals("eng");
	public final static boolean DEBUG_LOADERS = ENABLE_DEBUG && true;
	public final static boolean DEBUG_LAYOUT = ENABLE_DEBUG && false;
	public final static boolean DEBUG_MOTION = ENABLE_DEBUG && false;
	public final static boolean DEBUG_DRAG = ENABLE_DEBUG && false;
	public final static boolean DEBUG_KEY = ENABLE_DEBUG && false;
	public final static boolean DEBUG_UNREAD = ENABLE_DEBUG && false;
	public final static boolean DEBUG_DRAW = ENABLE_DEBUG && false;
	public final static boolean DEBUG_PERFORMANCE = ENABLE_DEBUG && false;
	public final static boolean DEBUG_SURFACEWIDGET = ENABLE_DEBUG && false;
	public final static boolean DEBUG_WIDGETS = ENABLE_DEBUG && false;
	public final static boolean DEBUG_ANIM = ENABLE_DEBUG && true;
	public static final int PORTRAIT = 0;
	public static final int LANDSCAPE = 1;
	
	public static ComponentName getComponentNameFromResolveInfo(ResolveInfo info) {
		if(info != null){
	        if (info.activityInfo != null) {
	            return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
	        } else {
	            return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
	        }
		}
		return null;
    }
	
	/**
     * Check whether the given component name is enabled.
     * 
     * @param context
     * @param cmpName
     * @return true if the component is in default or enable state, and the application is also in default or enable state,
     *         false if in disable or disable user state.
     */
	public static boolean isComponentEnabled(final Context context, final ComponentName cmpName) {
        final String pkgName = cmpName.getPackageName();
        final PackageManager pm = context.getPackageManager();
        // Check whether the package has been uninstalled.
        PackageInfo pInfo = null;
        try {
            pInfo = pm.getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, "isComponentEnabled NameNotFoundException: pkgName = " + pkgName);
        }

        if (pInfo == null) {
            Log.d(TAG, "isComponentEnabled return false because package " + pkgName + " has been uninstalled!");
            return false;
        }

        final int pkgEnableState = pm.getApplicationEnabledSetting(pkgName);
        //if (LauncherLog.DEBUG) {
        //    Log.d(TAG, "isComponentEnabled: cmpName = " + cmpName + ",pkgEnableState = " + pkgEnableState);
        //}
        if (pkgEnableState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                || pkgEnableState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            final int cmpEnableState = pm.getComponentEnabledSetting(cmpName);
            //if (LauncherLog.DEBUG) {
             //   Log.d(TAG, "isComponentEnabled: cmpEnableState = " + cmpEnableState);
            //}
            if (cmpEnableState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    || cmpEnableState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return true;
            }
        }

        return false;
    }
	
	public static int generateRandomId() {
        return Math.abs(new Random(System.currentTimeMillis()).nextInt(1 << 24));
    }
	
	public static int[] computeScaleImageSize(int sourceWidth, int sourceHeight, int destWidth, int destHeight){
        
        if (sourceWidth > 0 && sourceHeight > 0) {
            // There are intrinsic sizes.
            if (destWidth < sourceWidth || destHeight < sourceHeight) {
                // It's too big, scale it down.
                final float ratio = (float) sourceWidth / sourceHeight;
                if (sourceWidth > sourceHeight) {
                    destHeight = (int) (destWidth / ratio);
                } else if (sourceHeight > sourceWidth) {
                    destWidth = (int) (destHeight * ratio);
                }
            } else if (sourceWidth < destWidth && sourceHeight < destHeight) {
                // Don't scale up the icon
                destWidth = sourceWidth;
                destHeight = sourceHeight;
            }
        }

        return new int[]{destWidth, destHeight};
    }

//	public static boolean scaleBitmapDrawable(Drawable drawable, int destWidth, int destHeight){
//	    return scaleBitmapDrawable(drawable, destWidth, destHeight);
//    }
//	
//	public static boolean scaleBitmapDrawable(Drawable drawable, int destWidth, int destHeight){
//	    return scaleBitmapDrawable(drawable, destWidth, destHeight);
//    }
	
	public static boolean scaleBitmapDrawable(Drawable drawable, int destWidth, int destHeight){
	    if(drawable == null || (drawable.getIntrinsicWidth() == destWidth
	            && drawable.getIntrinsicHeight() == destHeight)){
	        //android.util.Log.e("QsLog", "scaleBitmapDrawable()=drawable is null==fail==");
	        return false;
	    }
	    
	    if(drawable instanceof BitmapDrawable){ 
	        final BitmapDrawable dr = (BitmapDrawable)drawable;
	        int destDensity = getScaleBitmapDrawableDensity(dr.getBitmap(), destWidth, destHeight);
	        //android.util.Log.i("QsLog", "scaleBitmapDrawable()=BitmapDrawable==destDensity:"+destDensity);
	        if(destDensity > 0){
	            dr.setTargetDensity(destDensity);
	            return true;
	        }
	    } else if(drawable instanceof FastBitmapDrawable){
	        final FastBitmapDrawable dr = (FastBitmapDrawable)drawable;
	        int destDensity = getScaleBitmapDrawableDensity(dr.getBitmap(), destWidth, destHeight);
	        //android.util.Log.i("QsLog", "scaleBitmapDrawable()=FastBitmapDrawable==destDensity:"+destDensity);
            if(destDensity > 0){
                dr.setTargetDensity(destDensity);
                return true;
            }
        /*} else if(drawable instanceof NinePatchDrawable){ */
            
        } else if(drawable instanceof DrawableContainer){
            final DrawableContainer dr = (DrawableContainer)drawable;
//            android.util.Log.i("QsLog", "scaleBitmapDrawable()=DrawableContainer==w:"+dr.getIntrinsicWidth()
//                    +"==h:"+dr.getIntrinsicHeight());
            DrawableContainer.DrawableContainerState state = (DrawableContainer.DrawableContainerState)dr.getConstantState();
            if(state != null){
                Drawable[] drlist = state.getChildren();
                if(drlist != null && drlist.length > 0){
                    boolean ret = false;
                    for(int i=drlist.length-1; i>=0; i--){
                        if(drlist[i] != null && scaleBitmapDrawable(drlist[i], destWidth, destHeight)){
                            //android.util.Log.e("QsLog", "scaleBitmapDrawable()=DrawableContainer==fail==i:"+i);
                            ret = true;
                        }
                    }
                    
                    return ret;
                }
            }
	    } else if(drawable instanceof LayerDrawable){
	        final LayerDrawable dr = (LayerDrawable)drawable;
//	        android.util.Log.i("QsLog", "scaleBitmapDrawable()=DrawableContainer==w:"+dr.getIntrinsicWidth()
//                    +"==h:"+dr.getIntrinsicHeight()
//                    +"==childnum:"+dr.getNumberOfLayers());
	        if(dr.getNumberOfLayers() > 0){
	            boolean ret = false;
    	        for(int i=dr.getNumberOfLayers()-1; i>=0; i--){
    	            Drawable item = dr.getDrawable(i);
    	            if(item != null && scaleBitmapDrawable(item, destWidth, destHeight))
    	                ret = true;
    	        }
    	        return ret;
	        }
	    } else {
	        //android.util.Log.e("QsLog", "scaleBitmapDrawable()=unknow type==fail==");
	    }
	    
	    return false;
	}
	
	private static int getScaleBitmapDrawableDensity(Bitmap bmp, int destWidth, int destHeight){
	    final int sourceWidth = bmp.getWidth();
        final int sourceHeight = bmp.getHeight();
        int destDensity = 0;
        if(destHeight < sourceHeight && destWidth < sourceWidth){
            if(Math.abs(destHeight - sourceHeight) > Math.abs(destWidth - sourceWidth)){
                destDensity = (int)(bmp.getDensity() * destHeight / sourceHeight);
            } else {
                destDensity = (int)(bmp.getDensity() * destWidth / sourceWidth);
            }
        } else if(destHeight < sourceHeight){
            destDensity = (int)(bmp.getDensity() * destHeight / sourceHeight);
        } else if(destWidth < sourceWidth) {
            destDensity = (int)(bmp.getDensity() * destWidth / sourceWidth);
        } else if(destHeight == sourceHeight && destWidth == sourceWidth) {
            destDensity = bmp.getDensity();
        }
//        android.util.Log.v("QsLog", "getScaleBitmapDrawableDensity()=sourceWidth:"+sourceWidth
//                +"==sourceHeight:"+sourceHeight
//                +"==destWidth:"+destWidth
//                +"==destHeight:"+destHeight
//                +"==sourceDens:"+bmp.getDensity()
//                +"==destDens:"+destDensity);
        return destDensity;
	}
//	
//	private static int getScaleNinePatchDrawableDensity(NinePatch bmp, int destWidth, int destHeight){
//        final int sourceWidth = bmp.getWidth();
//        final int sourceHeight = bmp.getHeight();
//        int destDensity = 0;
//        
//        if(destHeight < sourceHeight && destWidth < sourceWidth){
//            if(Math.abs(destHeight - sourceHeight) > Math.abs(destWidth - sourceWidth)){
//                destDensity = (int)(bmp.getDensity() * destHeight / sourceHeight);
//            } else {
//                destDensity = (int)(bmp.getDensity() * destWidth / sourceWidth);
//            }
//        } else if(destHeight < sourceHeight){
//            destDensity = (int)(bmp.getDensity() * destHeight / sourceHeight);
//        } else if(destWidth < sourceWidth) {
//            destDensity = (int)(bmp.getDensity() * destWidth / sourceWidth);
//        }
//        
//        return destDensity;
//    }

	public static Bitmap drawable2Bitmap(Drawable drawable){  
		if(drawable == null) return null;
        return drawable2Bitmap(drawable, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    }
    
	public static Bitmap drawable2Bitmap(Drawable drawable, int width, int height){  
    	if(drawable == null) return null;
    	
        if(drawable instanceof BitmapDrawable){  
            return ((BitmapDrawable)drawable).getBitmap();  
        } else /*if(drawable instanceof NinePatchDrawable)*/{  
        	
        	if(width <= 0)
        		width = drawable.getIntrinsicWidth();
        	
        	if(height <= 0)
        		height = drawable.getIntrinsicHeight();
        	
        	if(width > 0 && height > 0){
	            Bitmap bitmap = Bitmap.createBitmap(
	                    		width, height,  
	                            drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888  
	                                    : Bitmap.Config.RGB_565);  
	            Canvas canvas = new Canvas(bitmap);  
	            drawable.setBounds(0, 0, width, height);  
	            drawable.draw(canvas);
	            
	            return bitmap;  
        	}
        }
        
        return null;
    }
	
	public static final class Log{
    	
		public static void d(String tag, String msg, Throwable tr){
    		if(ENABLE_DEBUG)
    			d(tag + "::" + msg, tr);
    	}
		
		public static void d(String msg, Throwable tr){
    		if(ENABLE_DEBUG)
    			android.util.Log.w(TAG, msg, tr);
    	}

    	public static void d(String tag, String txt){
    		if(ENABLE_DEBUG)
    			d(tag + "::" + txt);
    	}

    	public static void d(String txt){
    		if(ENABLE_DEBUG)
    			android.util.Log.d(TAG, txt);
    	}
    	
    	
    	public static void i(String tag, String txt){
    		if(ENABLE_DEBUG)
    			i(tag + "::" + txt);
    	}
    	
    	public static void i(String txt){
    		if(ENABLE_DEBUG)
    			android.util.Log.i(TAG, txt);
    	}
    	
    	public static void i(String msg, Throwable tr){
    		if(ENABLE_DEBUG)
    			android.util.Log.i(TAG, msg, tr);
    	}
    	
    	public static void w(String tag, String txt){
    		if(ENABLE_DEBUG)
    			w(tag + "::" + txt);
    	}
    	
    	public static void w(String tag, String msg, Throwable tr){
    		if(ENABLE_DEBUG)
    			w(tag + "::" + msg, tr);
    	}
    	
    	public static void w(String txt){
    		if(ENABLE_DEBUG)
    			android.util.Log.w(TAG, txt);
    	}
    	
    	public static void w(String msg, Throwable tr){
    		if(ENABLE_DEBUG)
    			android.util.Log.w(TAG, msg, tr);
    	}
    	
    	public static void e(String tag, String txt){
    		if(ENABLE_DEBUG)
    			e(tag + "::" + txt);
    	}
    	
    	public static void e(String txt){
    		if(ENABLE_DEBUG)
    			android.util.Log.e(TAG, txt);
    	}
    	
    	public static void e(String tag, String msg, Throwable tr){
    		if(ENABLE_DEBUG)
    			e(tag + "::" + msg, tr);
    	}
    	
    	public static void e(String msg, Throwable tr){
    		if(ENABLE_DEBUG)
    			android.util.Log.e(TAG, msg, tr);
    	}
    	
    	public static void v(String tag, String txt){
    		if(ENABLE_DEBUG)
    			v(tag + "::" + txt);
    	}
    	
    	public static void v(String txt){
    		if(ENABLE_DEBUG)
    			android.util.Log.v(TAG, txt);
    	}
    }
}
