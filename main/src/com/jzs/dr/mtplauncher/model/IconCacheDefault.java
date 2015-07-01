package com.jzs.dr.mtplauncher.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.jzs.common.manager.IIconUtilities;
import com.jzs.dr.mtplauncher.sjar.model.IconCache;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

import com.jzs.dr.mtplauncher.R;

public class IconCacheDefault extends IconCache {
		
//	private final Drawable[] mIconFrameBg;
//	private final Bitmap mMaskBitmap;
	
	public IconCacheDefault(Context context, IIconUtilities iconUtilities) {
		super(context, iconUtilities);
		
//		final Resources resources = context.getResources();
//		TypedArray array = resources.obtainTypedArray(R.array.config_appIconFrameBackground);
//		if(array != null){
//	        int n = array.length();
//	        mIconFrameBg = new Drawable[n];
//	        for (int i = 0; i < n; ++i) {
//	        	mIconFrameBg[i] = array.getDrawable(i);
//	        }
//	        array.recycle();
//		} else {
//			mIconFrameBg = null;
//		}
//		Drawable dr = resources.getDrawable(R.drawable.ic_app_icon_region_mask);
//		if(dr != null)
//			mMaskBitmap = Util.drawable2Bitmap(dr);
//		else
//			mMaskBitmap = null;

	}
	
//	@Override
//	public Bitmap createIconBitmap(String packageName, Bitmap orgIcon) {
//	    if(packageName != null && !isSystemApp(packageName)){
//    	    if(mIconFrameBg != null)
//                return mIconUtilities.createIconBitmapWithMask(orgIcon, mMaskBitmap, getIconFrameBackground());
//    	    return mIconUtilities.createIconBitmap(orgIcon, getIconFrameBackground());
//	    }
//	    return super.createIconBitmap(packageName, orgIcon);
//	}
//	@Override
//    public Bitmap createIconBitmap(String packageName, Drawable orgIcon) {
//	    if(packageName != null && !isSystemApp(packageName)){
//            if(mIconFrameBg != null)
//                return mIconUtilities.createIconBitmapWithMask(orgIcon, mMaskBitmap, getIconFrameBackground());
//            return mIconUtilities.createIconBitmap(orgIcon, getIconFrameBackground());
//	    }
//        return super.createIconBitmap(packageName, orgIcon);
//    }
	@Override
	protected Bitmap createIconBitmap(boolean isSystemApp, Bitmap orgIcon) {     
	    if(!isSystemApp){
	        return mIconUtilities.createIconBitmapWithMaskAndBackground(orgIcon);//, mMaskBitmap, getIconFrameBackground());
        }
        return super.createIconBitmap(isSystemApp, orgIcon);
    }
	@Override
    protected Bitmap createIconBitmap(boolean isSystemApp, Drawable orgIcon) {
	    if(!isSystemApp){
	       return mIconUtilities.createIconBitmapWithMaskAndBackground(orgIcon);
        }
	    return super.createIconBitmap(isSystemApp, orgIcon);
    }
//	@Override
//	public String createLabelString(ComponentName componentName, ComponentInfo info){        
//        return super.createLabelString(componentName, info);
//    }
	
//	private Drawable getIconFrameBackground(){
//		int i = Util.generateRandomId() % mIconFrameBg.length;
//		return mIconFrameBg[i];
//	}

}
