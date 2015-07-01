package com.jzs.dr.mtplauncher.sjar.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;

import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

public class UninstallDropTarget extends DeleteDropTarget {
    protected PackageManager mPackageManager;
    
    public UninstallDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UninstallDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPackageManager = context.getPackageManager();
    }
    
    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        boolean isVisible = false;
        boolean isUninstall = false;

        // If we are dragging a widget from AppsCustomize, hide the delete target
        if (isAllAppsWidget(source, info)) {
            isVisible = false;
        }

        // If we are dragging an application from AppsCustomize, only show the control if we can
        // delete the app (it was downloaded), and rename the string to "uninstall" in such a case
        if((info instanceof ApplicationInfo)){//if (isAllAppsApplication(source, info)) {
            ApplicationInfo appInfo = (ApplicationInfo) info;
            if(appInfo.isDownloadApp()){//if ((appInfo.flags & ApplicationInfo.DOWNLOADED_FLAG) != 0) {
                isUninstall = true;
                isVisible = true;
            }
        } else if(info instanceof ShortcutInfo){
            ShortcutInfo sInfo = (ShortcutInfo)info;
            if(ApplicationInfo.isDownloadApp(mPackageManager, sInfo.getPackageName())){
                isVisible = true;
                isUninstall = true;
            }
        }

        super.onDragStart(source, info, dragAction, isVisible, isUninstall);
    }
    
    protected void completeDrop(DragObject d) {
        ItemInfo item = (ItemInfo) d.dragInfo;
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "completeDrop: item = " + item + ", d = " + d);
        }
        final boolean isFromApps = isFromAllApps(d.dragSource);
        super.completeDrop(d);

        if (!isFromApps && (item instanceof ShortcutInfo)) {
            ShortcutInfo sInfo = (ShortcutInfo)item;
            mLauncher.startApplicationUninstallActivity(sInfo.getPackageName(), sInfo.getClassName());
        }
    }
}
