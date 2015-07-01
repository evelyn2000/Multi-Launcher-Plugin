package com.jzs.dr.mtplauncher.sjar.model;

import java.util.HashMap;
import java.util.Map;

import com.jzs.common.launcher.IResConfigManager;
import com.jzs.common.launcher.ISharedPrefSettingsManager;
//import com.jzs.dr.mtplauncher.sjar.ISharedPrefSettingsManager;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import com.jzs.dr.mtplauncher.sjar.LauncherApplication;

public class ResConfigManager implements IResConfigManager {
	//global system

	private final IResConfigManager mBase;
	protected Resources mResources;
	protected LayoutInflater mInflater;
	private String mPackageName;
	
	protected float sScreenDensity;	
	protected final ISharedPrefSettingsManager mSharedPrefSettingsManager;
	private final static Map<Integer, String> sCacheInfoMap = new HashMap<Integer, String>();
	
	public ResConfigManager(Context context, ISharedPrefSettingsManager pref, IResConfigManager base){
		mBase = base;
//		if(base == null && !(context instanceof LauncherApplication)){
//			throw new RuntimeException("Jzs.ResConfigManager base cat't be null..");
//		}
		mSharedPrefSettingsManager = pref;
		mPackageName = context.getPackageName();
		attachBaseContext(context);
		sScreenDensity = mResources.getDisplayMetrics().density;
	}
	
	public void attachBaseContext(Context context){
		mResources = context.getResources();
		mInflater = LayoutInflater.from(context);
	}
	
	public void ReleaseInstance(){
		synchronized (sCacheInfoMap) {
			sCacheInfoMap.clear();
		}
	}
	
	public void onTrimMemory(int level){
		synchronized (sCacheInfoMap) {
			sCacheInfoMap.clear();
		}
	}
	
	public final Resources getResources(){
		return mResources;
	}
	
	public final Resources getBaseResources(){
		if(mBase != null)
			return mBase.getResources();
		return null;
	}
	
	public final LayoutInflater getLayoutInflater(){
		return mInflater;
	}
	
	public final IResConfigManager getResConfigManagerBase(){
		return mBase;
	}
//	
//	protected void setResConfigManagerBase(IResConfigManager base){
//		mBase = base;
//	}
	
	public final float getScreenDensity(){
		return sScreenDensity;
	}
	
	public final boolean isLandscape(){
		return (getResources().getConfiguration().orientation ==
	            				Configuration.ORIENTATION_LANDSCAPE);
	}
	
	protected final boolean isInternalResourceId(int res){
		if((res & 0xFFF00000) != 0){
			return true;
		}
		return false;
	}
	
	public int getResourceId(int type){
		if(isInternalResourceId(type)){
			return type;
		}
//		if(mBase != null){
//			int res = mBase.getResourceId(type);
//			
//		}
		return 0;
	}
	
//	public int getLocalResourceId(int type){
//        if(isInternalResourceId(type)){
//            return type;
//        }
//        return 0;
//    }
	
//	private String getResourceEntryName(int resid) throws Resources.NotFoundException{
//	    if(resid != 0){
//	        return getResources().getResourceEntryName(resid);
//	    }
//	    
//	    throw new Resources.NotFoundException("Unable to find resource ID #0x"
//                + Integer.toHexString(resid));
//	}
	
//	private int getLocalIdentifier(String name, String defType, String defPackage) {
//	    if(name != null){
//	        return getResources().getIdentifier(name, defType, defPackage);
//	    }
//	    return 0;
//	}
	
	private int getLocalIdentifier(int resid, String defType) throws Resources.NotFoundException {
        if(resid != 0){
            String name = getBaseResources().getResourceEntryName(resid);
//            
//            android.util.Log.e("QsLog", "getLocalIdentifier==resid:"+Integer.toHexString(resid)
//                    +", name:"+name
//                    +", type:"+defType);
            
            if(name != null){
                int ret = getResources().getIdentifier(name, defType, mPackageName);
                if(ret != 0){
                    return ret;
                }
//                
//                android.util.Log.e("QsLog", "getLocalIdentifier==Unable to find resource ID:"+Integer.toHexString(ret)
//                        +", name:"+name
//                        +", type:"+defType);
                
                throw new Resources.NotFoundException("Unable to find local resource ID #0x"
                        + Integer.toHexString(resid)
                        +", name:"+name);
            }
        }
        
        throw new Resources.NotFoundException("Unable to find local resource ID #0x"
                + Integer.toHexString(resid));
    }
	
	private int getLocalIdentifier(int resid) throws Resources.NotFoundException {
        if(resid != 0){
            String resname = getBaseResources().getResourceName(resid);
           
            if(resname != null){
                int pkgindex = resname.indexOf(':');
                if(pkgindex > 0){
                    String[] nametype = resname.substring(pkgindex+1).split("/");
                    if(nametype != null && nametype.length > 0){
                        int ret = getResources().getIdentifier(nametype[1], nametype[0], mPackageName);
                        if(ret != 0)
                            return ret;
//                        
//                        android.util.Log.e("QsLog", "getLocalIdentifier==Unable to find resource ID:"+Integer.toHexString(ret)
//                                +", name:"+nametype[1]
//                                +", type:"+nametype[0]);
                    }
                }
                
                throw new Resources.NotFoundException("Unable to find local resource ID #0x"
                        + Integer.toHexString(resid)
                        +", name:"+resname);
            }
        }
        
        throw new Resources.NotFoundException("Unable to find local resource ID #0x"
                + Integer.toHexString(resid));
    }

	public final View inflaterView(int type){
		return inflaterView(type, null, false);
	}
	
	public final View inflaterView(int type, ViewGroup parent){
		return inflaterView(type, parent, false);
	}
	
	public final View inflaterView(int type, ViewGroup parent, boolean attachToRoot){
		int resid = getResourceId(type);
		
//		android.util.Log.e("QsLog", "inflaterView==type:"+Integer.toHexString(type)
//                +", resid:"+Integer.toHexString(resid)
//                +", mBase:"+mBase);
		
		if(resid != 0){
		    return mInflater.inflate(resid, parent, attachToRoot);
		}
		
		if(mBase != null){
		    resid = mBase.getResourceId(type);
//		    android.util.Log.e("QsLog", "inflaterView=2=type:"+Integer.toHexString(type)
//	                +", resid:"+Integer.toHexString(resid));
		    if(resid != 0){
                try {
                    return mInflater.inflate(getLocalIdentifier(resid, "layout"), parent, attachToRoot);
                } catch (Resources.NotFoundException e) { }
                
		        return mBase.inflaterView(resid, parent, attachToRoot);
		    }
		}
		return null;
	}

	public final Drawable getDrawable(int type){
		int resid = getResourceId(type);
		if(resid != 0){
			try {
				return getResources().getDrawable(resid);
			} catch(Resources.NotFoundException e){ }
		}
		
		if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    return getResources().getDrawable(getLocalIdentifier(resid, "drawable"));
                } catch (Resources.NotFoundException e) { }
                
                return getBaseResources().getDrawable(resid);
            }
        }
		
		return null;
	}
	
	public final int getDimensionPixelSize(int type){
		return getDimensionPixelSize(type, 0);
	}
	
	public final int getDimensionPixelSize(int type, int defValue){
		try{
			synchronized (sCacheInfoMap) {
				String str = sCacheInfoMap.get(type);
				if(str != null)
					return Integer.parseInt(str);
			}
		} catch (NumberFormatException e){}
		
//		try{
//            int ret = getDimensionPixelSizeLocal(type, defValue);
//            sCacheInfoMap.put(type, String.valueOf(ret));
//            return ret;
//        } catch(Resources.NotFoundException e){}
		
		int resid = getResourceId(type);
		if(resid != 0){
			try{
				int ret = getResources().getDimensionPixelSize(resid);
				sCacheInfoMap.put(type, String.valueOf(ret));
				return ret;
			} catch(Resources.NotFoundException e){}
		}
		
		if(mBase != null){
		    resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    int ret = getResources().getDimensionPixelSize(getLocalIdentifier(resid, "dimen"));
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
                
                try {
                    int ret =  getBaseResources().getDimensionPixelSize(resid);
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
            }
        }
		
		return (int)(defValue * getScreenDensity());
	}
	
	public final int getDimensionPixelOffset(int type){
		return getDimensionPixelOffset(type, 0);
	}
	
	public final int getDimensionPixelOffset(int type, int defValue){
		try{
			synchronized (sCacheInfoMap) {
				String str = sCacheInfoMap.get(type);
				if(str != null)
					return Integer.parseInt(str);
			}
		} catch (NumberFormatException e){}
		
		int resid = getResourceId(type);
        if(resid != 0){
            try{
                int ret = getResources().getDimensionPixelOffset(resid);
                sCacheInfoMap.put(type, String.valueOf(ret));
                return ret;
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    int ret = getResources().getDimensionPixelOffset(getLocalIdentifier(resid, "dimen"));
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
                
                try {
                    int ret =  getBaseResources().getDimensionPixelOffset(resid);
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
            }
        }
		
		return (int)(defValue * getScreenDensity());
	}
	
	public final float getDimension(int type){
		return getDimension(type, 0.0f);
	}
	public final float getDimension(int type, float defValue){
		try{
			synchronized (sCacheInfoMap) {
				String str = sCacheInfoMap.get(type);
				if(str != null)
					return Float.parseFloat(str);
			}
		} catch (NumberFormatException e){}
		
//		int res = getResourceId(type);
//		if(res > 0){
//			try{
//				float ret = mResources.getDimension(res);
//				sCacheInfoMap.put(type, String.valueOf(ret));
//				return ret;
//			} catch(Resources.NotFoundException e){ }
//			if(mBase != null)
//				return mBase.getDimension(type, defValue);
//		}
		
		int resid = getResourceId(type);
        if(resid != 0){
            try{
                float ret = getResources().getDimension(resid);
                sCacheInfoMap.put(type, String.valueOf(ret));
                return ret;
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    float ret = getResources().getDimension(getLocalIdentifier(resid, "dimen"));
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
                
                try {
                    float ret =  getBaseResources().getDimension(resid);
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
            }
        }
		
		return defValue * getScreenDensity();
	}
	
	public final int getInteger(int type){
		return getInteger(type, 0);
	}
	
	public final int getInteger(int type, int defValue){
		try{
			synchronized (sCacheInfoMap) {
				String str = sCacheInfoMap.get(type);
				if(str != null)
					return Integer.parseInt(str);
			}
		} catch (NumberFormatException e){}
		
//		int res = getResourceId(type);
//		if(res > 0){
//			try{
//				int ret = mResources.getInteger(res);
//				sCacheInfoMap.put(type, String.valueOf(ret));
//				return ret;
//			} catch(Resources.NotFoundException e){
//			}
//			if(mBase != null)
//				return mBase.getInteger(type, defValue);
//		}
		
		int resid = getResourceId(type);
        if(resid != 0){
            try{
                int ret = getResources().getInteger(resid);
                sCacheInfoMap.put(type, String.valueOf(ret));
                return ret;
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    int ret = getResources().getInteger(getLocalIdentifier(resid, "integer"));
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
                
                try {
                    int ret =  getBaseResources().getInteger(resid);
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
            }
        }
		
		return defValue;
	}
	
	public final int[] getIntArray(int type){
//		int res = getResourceId(type);
//		if(res > 0){
//			try{
//				return mResources.getIntArray(res);
//			} catch(Resources.NotFoundException e){
//			}
//			if(mBase != null)
//				return mBase.getIntArray(type);
//		}
		
		int resid = getResourceId(type);
        if(resid != 0){
            try {
                return getResources().getIntArray(resid);
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    return getResources().getIntArray(getLocalIdentifier(resid));
                } catch (Resources.NotFoundException e) { }
                
                return getBaseResources().getIntArray(resid);
            }
        }
		
		return null;
	}
	
	public final boolean getBoolean(int type){		
		return getBoolean(type, false);
	}
	
	public final boolean getBoolean(int type, boolean defValue){
		try{
			synchronized (sCacheInfoMap) {
				String str = sCacheInfoMap.get(type);
				if(str != null)
					return Boolean.parseBoolean(str);
			}
		} catch (NumberFormatException e){}
		
//		int res = getResourceId(type);
//		if(res > 0){
//			try{
//				boolean ret = mResources.getBoolean(res);
//				sCacheInfoMap.put(type, String.valueOf(ret));
//				return ret;
//			} catch(Resources.NotFoundException e){
//			}
//			if(mBase != null)
//				return mBase.getBoolean(type, defValue);
//		}
		
		int resid = getResourceId(type);
        if(resid != 0){
            try{
                boolean ret = getResources().getBoolean(resid);
                sCacheInfoMap.put(type, String.valueOf(ret));
                return ret;
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    boolean ret = getResources().getBoolean(getLocalIdentifier(resid, "bool"));
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
                
                try {
                    boolean ret =  getBaseResources().getBoolean(resid);
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
            }
        }
		
		return defValue;
	}
	
	public final int getColor(int type){
		return getInteger(type, 0);
	}
	
	public final int getColor(int type, int defValue){
		try{
			synchronized (sCacheInfoMap) {
				String str = sCacheInfoMap.get(type);
				if(str != null)
					return Integer.parseInt(str);
			}
		} catch (NumberFormatException e){}
		
//		int res = getResourceId(type);
//		if(res > 0){
//			try{
//				int ret = mResources.getColor(res);
//				sCacheInfoMap.put(type, String.valueOf(ret));
//				return ret;
//			} catch(Resources.NotFoundException e){
//			}
//			if(mBase != null)
//				return mBase.getColor(type, defValue);
//		}
		
		int resid = getResourceId(type);
        if(resid != 0){
            try{
                int ret = getResources().getColor(resid);
                sCacheInfoMap.put(type, String.valueOf(ret));
                return ret;
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    int ret = getResources().getColor(getLocalIdentifier(resid, "color"));
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
                
                try {
                    int ret =  getBaseResources().getColor(resid);
                    sCacheInfoMap.put(type, String.valueOf(ret));
                    return ret;
                } catch (Resources.NotFoundException e) { }
            }
        }
		
		return defValue;
	}
	
	public final int getIntegerWithDensity(int type){
		return getIntegerWithDensity(type, 0);
	}
	
	public final int getIntegerWithDensity(int type, int defValue){
		return (int)(getInteger(type, defValue) * getScreenDensity());
	}
	
	public final String getString(int type){
		return getString(type, null);
	}
	
	public final String getString(int type, String defValue){
		try{
			synchronized (sCacheInfoMap) {
				String str = sCacheInfoMap.get(type);
				if(str != null)
					return str;
			}
		} catch (NumberFormatException e){}
		
//		int res = getResourceId(type);
//		if(res > 0){
//			try{
//				String ret = mResources.getString(res);
//				if(ret != null)
//					sCacheInfoMap.put(type, ret);
//				return ret;
//			} catch(Resources.NotFoundException e){
//			}
//			if(mBase != null)
//				return mBase.getString(type);
//		}
		
		int resid = getResourceId(type);
        if(resid != 0){
            try{
                String ret = getResources().getString(resid);
                if(ret != null)
                    sCacheInfoMap.put(type, ret);
                return ret;
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    String ret = getResources().getString(getLocalIdentifier(resid, "string"));
                    if(ret != null)
                        sCacheInfoMap.put(type, ret);
                    return ret;
                } catch (Resources.NotFoundException e) { }
                
                try {
                    String ret =  getBaseResources().getString(resid);
                    if(ret != null)
                        sCacheInfoMap.put(type, ret);
                    return ret;
                } catch (Resources.NotFoundException e) { }
            }
        }
		
		return defValue;
	}
	
	public final CharSequence getText(int type){
		return getText(type, null);
	}
	
	public final CharSequence getText(int type, CharSequence def){
		try{
			synchronized (sCacheInfoMap) {
				String str = sCacheInfoMap.get(type);
				if(str != null)
					return str;
			}
		} catch (NumberFormatException e){}
		
//		int res = getResourceId(type);
//		if(res > 0){
//			try{
//				CharSequence ret = mResources.getText(res);
//				if(ret != null)
//					sCacheInfoMap.put(type, ret.toString());
//				return ret;
//			} catch(Resources.NotFoundException e){ }
//			
//			if(mBase != null)
//				return mBase.getText(type);
//		}
		
		int resid = getResourceId(type);
        if(resid != 0){
            try{
                CharSequence ret = getResources().getText(resid);
                if(ret != null)
                    sCacheInfoMap.put(type, ret.toString());
                return ret;
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    CharSequence ret = getResources().getText(getLocalIdentifier(resid, "string"));
                    if(ret != null)
                        sCacheInfoMap.put(type, ret.toString());
                    return ret;
                } catch (Resources.NotFoundException e) { }
                
                try {
                    CharSequence ret =  getBaseResources().getText(resid);
                    if(ret != null)
                        sCacheInfoMap.put(type, ret.toString());
                    return ret;
                } catch (Resources.NotFoundException e) { }
            }
        }
		
		return def;
	}
	
	public final String[] getStringArray(int type){
//		int res = getResourceId(type);
//		if(res > 0){
//			try{
//				return mResources.getStringArray(res);
//			} catch(Resources.NotFoundException e){
//			}
//			if(mBase != null)
//				return mBase.getStringArray(type);
//		}
		
		int resid = getResourceId(type);
        if(resid != 0){
            try {
                return getResources().getStringArray(resid);
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    return getResources().getStringArray(getLocalIdentifier(resid));
                } catch (Resources.NotFoundException e) { }
                
                return getBaseResources().getStringArray(resid);
            }
        }
		
		return null;
	}
	
	public final CharSequence[] getTextArray(int type){
//        int res = getResourceId(type);
//        if(res > 0){
//            try{
//                return mResources.getTextArray(res);
//            } catch(Resources.NotFoundException e){
//            }
//            if(mBase != null)
//                return mBase.getTextArray(type);
//        }
        
        int resid = getResourceId(type);
        if(resid != 0){
            try {
                return getResources().getTextArray(resid);
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    return getResources().getTextArray(getLocalIdentifier(resid));
                } catch (Resources.NotFoundException e) { }
                
                return getBaseResources().getTextArray(resid);
            }
        }
        
        return null;
    }
	
	public final XmlResourceParser getAnimation(int type){
//		int res = getResourceId(type);
//		if(res > 0){
//			try{
//				return mResources.getAnimation(res);
//			} catch(Resources.NotFoundException e){
//			}
//			if(mBase != null)
//				return mBase.getAnimation(type);
//		}
		
		int resid = getResourceId(type);
        if(resid != 0){
            try{
                return getResources().getAnimation(resid);
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    return getResources().getAnimation(getLocalIdentifier(resid));
                } catch (Resources.NotFoundException e) { }
                
                return getBaseResources().getAnimation(resid);
            }
        }
		
		return null;
	}
	
	public final XmlResourceParser getXml(int type){
//		int res = getResourceId(type);
//		if(res > 0){
//			try{
//				return mResources.getXml(res);
//			} catch(Resources.NotFoundException e){
//			}
//			
//			if(mBase != null)
//				return mBase.getXml(type);
//		}
		
		int resid = getResourceId(type);
        if(resid != 0){
            try{
                return getResources().getXml(resid);
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    return getResources().getXml(getLocalIdentifier(resid));
                } catch (Resources.NotFoundException e) { }
                
                return getBaseResources().getXml(resid);
            }
        }
		return null;
	}
	
	public final TypedArray obtainTypedArray(int type){
//	    int res = getResourceId(type);
//        if(res > 0){
//            try{
//                return mResources.obtainTypedArray(res);
//            } catch(Resources.NotFoundException e){
//            }
//            
//            if(mBase != null)
//                return mBase.obtainTypedArray(type);
//        }
        
        int resid = getResourceId(type);
        if(resid != 0){
            try{
                return getResources().obtainTypedArray(resid);
            } catch(Resources.NotFoundException e){}
        }
        
        if(mBase != null){
            resid = mBase.getResourceId(type);
            if(resid != 0){
                try {
                    return getResources().obtainTypedArray(getLocalIdentifier(resid));
                } catch (Resources.NotFoundException e) { }
                
                return getBaseResources().obtainTypedArray(resid);
            }
        }
        return null;
	}
}
