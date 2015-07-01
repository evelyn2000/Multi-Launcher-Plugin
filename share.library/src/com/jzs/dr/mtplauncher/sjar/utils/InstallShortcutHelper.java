package com.jzs.dr.mtplauncher.sjar.utils;

import com.jzs.common.launcher.LauncherHelper;

public class InstallShortcutHelper {
    private static final String TAG = "InstallShortcutHelper";
    private static boolean sInstallingShortcut = false;
    private static int sInstallingCount = 0;
    private static int sSuccessCount = 0;

    /// M: Set the installing shortcut flag, some actions should be forbidden when installing shortcut, 
    ///    due to this will lead to the database changing. The flag will be set when start installing and
    ///    reset when binding workspace is done or installing count is decreased to zero.
    public static void setInstallingShortcut(boolean bInstalling) {
        sInstallingShortcut = bInstalling;
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "setInstallingShortcut: sInstallingShortcut=" + sInstallingShortcut);
        }
    }

    /// M: Check the installing process is ongoing or not
    public static boolean isInstallingShortcut() {
        return sInstallingShortcut;
    }

    /// M: Increase the installing count
    public static void increaseInstallingCount(int count) {
        sInstallingCount += count;
        if (sInstallingCount > 0) {
            sInstallingShortcut = true;
        }
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "increaseInstallingCount: sInstallingCount=" + sInstallingCount
                    + ", sInstallingShortcut=" + sInstallingShortcut);
        }
    }

    /// M: Decrease the installing count, split the successful and failed item, and will trigger loading
    ///    database when successful items is not zero.
    public static void decreaseInstallingCount(LauncherHelper launcher, boolean bSuccess) {
        sInstallingCount--;
        if (bSuccess) {
            sSuccessCount++;
        }
        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "decreaseInstallingCount: sInstallingCount=" + sInstallingCount 
                    + ", sSuccessCount=" + sSuccessCount);
        }

        if (sInstallingCount <= 0) {
            if (sSuccessCount != 0) {
                Util.Log.d(TAG, "decreaseInstallingCount: triggerLoadingDatabaseManually");
                //LauncherApplication app = (LauncherApplication) context.getApplicationContext();
                launcher.triggerLoadingDatabaseManually();
            } else {
                Util.Log.d(TAG, "decreaseInstallingCount: all failed, and reset sInstallingShortcut");
                sInstallingShortcut = false;
            }
            sInstallingCount = 0;
            sSuccessCount = 0;
        }

        if (Util.ENABLE_DEBUG) {
            Util.Log.d(TAG, "decreaseInstallingCount: sInstallingShortcut=" + sInstallingShortcut);
        }
    }
}
