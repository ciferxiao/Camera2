package com.mediatek.camera.tests.v3.checker;

import com.mediatek.camera.tests.v3.util.Utils;

/**
 * AIS exited checker.
 */
public class AisExistedChecker extends SettingItemExistedChecker {

    /**
     * The constuctor of AisExistedChecker.
     *
     * @param isLaunchFromIntent Camera is launch from intent or not.
     */
    public AisExistedChecker(boolean isLaunchFromIntent) {
        super("Anti-shake", isLaunchFromIntent);
    }

    @Override
    public boolean isSupported(int index) {
        if (index == SettingItemExistedChecker.INDEX_EXISTED) {
            return Utils.isFeatureSupported("com.mediatek.camera.at.anti-shake");
        } else {
            return true;
        }
    }
}
