package com.jzs.dr.mtplauncher.sjar.widget;

import java.util.ArrayList;

import com.jzs.common.launcher.IResConfigManager;
import com.jzs.dr.mtplauncher.sjar.Launcher;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AddAdapter extends BaseAdapter {

    //private final LayoutInflater mInflater;

    private final ArrayList<ListItem> mItems = new ArrayList<ListItem>();

    public static final int ITEM_SHORTCUT = 0;
    public static final int ITEM_APPWIDGET = 1;
    public static final int ITEM_APPLICATION = 2;
    public static final int ITEM_WALLPAPER = 3;
    
    protected IResConfigManager mResConfigManager;

    /**
     * Specific item in our list.
     */
    public class ListItem {
        public final CharSequence text;
        public final Drawable image;
        public final int actionTag;

        public ListItem(Resources res, int textResourceId, int imageResourceId, int actionTag) {
            text = res.getString(textResourceId);
            if (imageResourceId != -1) {
                image = res.getDrawable(imageResourceId);
            } else {
                image = null;
            }
            this.actionTag = actionTag;
        }
        
        public ListItem(CharSequence txt, Drawable icon, int actionTag){
        	this.text = txt;
        	this.image = icon;
        	this.actionTag = actionTag;
        }
    }
    
    public AddAdapter(Launcher launcher) {
        super();

        //mInflater = (LayoutInflater) launcher.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResConfigManager = launcher.getResConfigManager();
        
        // Create default actions
        //Resources res = launcher.getResources();
//        mItems.add(new ListItem(res, R.string.group_wallpapers,
//                R.mipmap.ic_launcher_wallpaper, ITEM_WALLPAPER));
        mItems.add(new ListItem(mResConfigManager.getText(IResConfigManager.STR_GROUP_WALLPAPERS),
    		  mResConfigManager.getDrawable(IResConfigManager.IMG_IC_LAUNCHER_WALLPAPER), ITEM_WALLPAPER));
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ListItem item = (ListItem) getItem(position);

        if (convertView == null) {
            convertView = mResConfigManager.inflaterView(IResConfigManager.LAYOUT_ADD_LIST_ITEM, parent);
            //mInflater.inflate(R.layout.add_list_item, parent, false);
        }

        TextView textView = (TextView) convertView;
        textView.setTag(item);
        textView.setText(item.text);
        textView.setCompoundDrawablesWithIntrinsicBounds(item.image, null, null, null);

        return convertView;
    }
    
    public void addItem(ListItem item){
    	mItems.add(item);
    }
    
    public void removeItem(ListItem item){
    	mItems.remove(item);
    }
    
    public void removeByAction(int action){
    	for(ListItem item : mItems){
    		if(item.actionTag == action){
    			mItems.remove(item);
    			break;
    		}
    	}
    }

    public int getCount() {
        return mItems.size();
    }

    public Object getItem(int position) {
        return mItems.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

}
