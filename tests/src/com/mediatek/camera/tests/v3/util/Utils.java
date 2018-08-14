package com.mediatek.camera.tests.v3.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.media.ExifInterface;
import android.os.StatFs;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.portability.SystemProperties;
import com.mediatek.camera.portability.storage.StorageManagerExt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

public class Utils {
    public static int STABILITY_REPEAT_TIMES = SystemProperties.getInt(
            "mtk.camera.app.stability.repeat", 100);
    public static String WIFI_NAME = SystemProperties.getString(
            "mtk.camera.app.wifi.name", "AP10");
    public static String WIFI_PASSWORD = SystemProperties.getString(
            "mtk.camera.app.wifi.password", "oss6_ap10");

    public static int TIME_OUT_RIGHT_NOW = 100; // 100ms
    public static int TIME_OUT_SHORT_SHORT = 1000; // 1s
    public static int TIME_OUT_SHORT = 5000; // 5s
    public static int TIME_OUT_NORMAL = 10000; // 10s
    public static int TIME_OUT_LONG = 30000; // 30s
    public static int TIME_OUT_LONG_LONG = 180000; // 3min

    public static int SCROLL_TIMES_LESS = 3;
    public static int SCROLL_TIMES_NORMAL = 10;
    public static int SCROLL_TIMES_MORE = 20;

    private static final int WAIT_POWER_OFF_IN_MS = 30000;
    private static final int BUFFER_SIZE_OF_COPY = 1024 * 1024;

    private static final LogUtil.Tag TAG = getTestTag(Utils.class.getSimpleName());
    private static UiDevice mUiDevice;
    private static Context mContext;
    private static Context mTargetContext;
    private static InputManager mInputManager;
    private static Method mInjectInputMethod;
    private static FeatureSpecParser mFeatureSpecParser;

    static {
        Method getInstanceMethod = ReflectUtils.getMethod(InputManager.class, "getInstance");
        if (getInstanceMethod == null) {
            mInputManager = null;
        }
        mInputManager = (InputManager) ReflectUtils.callMethodOnObject(null, getInstanceMethod);

        mInjectInputMethod = ReflectUtils.getMethod(InputManager.class, "injectInputEvent",
                InputEvent.class, int.class);
    }

    public static LogUtil.Tag getTestTag(String tag) {
        return new LogUtil.Tag("AT_" + tag);
    }

    public static UiDevice getUiDevice() {
        if (mUiDevice == null) {
            mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        }
        mUiDevice.setCompressedLayoutHeirarchy(false);
        return mUiDevice;
    }

    public static Context getContext() {
        if (mContext == null) {
            mContext = InstrumentationRegistry.getContext();
        }
        return mContext;
    }

    public static Context getTargetContext() {
        if (mTargetContext == null) {
            mTargetContext = InstrumentationRegistry.getTargetContext();
        }
        return mTargetContext;
    }

    public static void assertObject(BySelector selector, int timeout) {
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return mUiDevice.findObject(selector) != null;
            }
        };
        condition.assertMe(timeout);
    }


    public static void assertObject(BySelector selector) {
        assertObject(selector, TIME_OUT_NORMAL);
    }

    public static void assertNoObject(BySelector selector, int timeout) {
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return mUiDevice.findObject(selector) == null;
            }
        };
        condition.assertMe(timeout);
    }

    public static void assertNoObject(BySelector selector) {
        assertNoObject(selector, TIME_OUT_NORMAL);
    }


    public static boolean waitObject(BySelector selector, int timeout) {
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return mUiDevice.findObject(selector) != null;
            }
        };
        return condition.waitMe(timeout);
    }

    public static boolean waitNoObject(BySelector selector, int timeout) {
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return mUiDevice.findObject(selector) == null;
            }
        };
        return condition.waitMe(timeout);
    }

    public static boolean waitObject(BySelector selector) {
        return waitObject(selector, TIME_OUT_NORMAL);
    }

    public static boolean waitNoObject(BySelector selector) {
        return waitNoObject(selector, TIME_OUT_NORMAL);
    }

    public static UiObject2 findObject(BySelector selector, int timeout) {
        boolean exist = waitObject(selector, timeout);
        if (exist) {
            return mUiDevice.findObject(selector);
        } else {
            return null;
        }
    }

    public static UiObject2 findNoObject(BySelector selector, int timeout) {
        boolean notExist = waitNoObject(selector, timeout);
        if (!notExist) {
            return mUiDevice.findObject(selector);
        } else {
            return null;
        }
    }

    public static UiObject2 findObject(BySelector selector) {
        return findObject(selector, TIME_OUT_NORMAL);
    }

    public static UiObject2 findNoObject(BySelector selector) {
        return findNoObject(selector, TIME_OUT_NORMAL);
    }

    public static UiObject2 pressBackUtilFindObject(BySelector selector, int maxPressBackTimes) {
        int times = 0;
        UiObject2 object;
        while (true) {
            object = findObject(selector, Utils.TIME_OUT_SHORT);
            if (object == null && times < maxPressBackTimes) {
                getUiDevice().pressBack();
                times++;
                continue;
            } else if (object != null) {
                return object;
            } else if (times >= maxPressBackTimes) {
                return null;
            }
        }
    }

    public static UiObject2 pressBackUtilFindNoObject(BySelector selector, int maxPressBackTimes) {
        int times = 1;
        getUiDevice().pressBack();
        UiObject2 object;
        while (true) {
            object = findNoObject(selector, Utils.TIME_OUT_SHORT);
            if (object != null && times < maxPressBackTimes) {
                getUiDevice().pressBack();
                times++;
                continue;
            } else if (object == null) {
                return object;
            } else if (times >= maxPressBackTimes) {
                return null;
            }
        }
    }

    public static UiObject2 scrollOnScreenToFind(BySelector selector) {
        return scrollOnScreenToFind(selector, SCROLL_TIMES_NORMAL);
    }

    public static UiObject2 scrollOnScreenToFind(BySelector selector, int scrollTimes) {
        int STEPS = 20;
        int startX = Utils.getUiDevice().getDisplayWidth() / 2;
        int endX = Utils.getUiDevice().getDisplayWidth() / 2;
        int startY = Utils.getUiDevice().getDisplayWidth() / 4 * 3;
        int endY = Utils.getUiDevice().getDisplayWidth() / 4;

        UiObject2 res = null;

        // scroll down
        for (int i = 0; i < scrollTimes; i++) {
            res = findObject(selector, TIME_OUT_SHORT_SHORT);
            if (res != null) {
                return res;
            } else {
                getUiDevice().swipe(startX, startY, endX, endY, STEPS);
            }
        }

        // scroll up
        for (int i = 0; i < scrollTimes; i++) {
            res = findObject(selector, TIME_OUT_SHORT_SHORT);
            if (res != null) {
                return res;
            } else {
                getUiDevice().swipe(startX, endY, endX, startY, STEPS);
            }
        }

        return res;
    }

    public static UiObject2 scrollOnObjectToFind(BySelector selector, UiObject2 inObject) {
        return scrollOnObjectToFind(selector, inObject, SCROLL_TIMES_NORMAL);
    }

    public static UiObject2 scrollOnObjectToFind(BySelector selector, UiObject2 inObject,
                                                 int scrollTimes) {
        int STEPS = 20;
        Rect rect = inObject.getVisibleBounds();
        int startX = rect.left + rect.width() / 2;
        int endX = rect.left + rect.width() / 2;
        int startY = rect.top + rect.height() / 4 * 3;
        int endY = rect.top + rect.height() / 4;

        UiObject2 res = null;
        // scroll down
        for (int i = 0; i < scrollTimes; i++) {
            res = findObject(selector, TIME_OUT_SHORT_SHORT);
            if (res != null) {
                return res;
            } else {
                getUiDevice().swipe(startX, startY, endX, endY, STEPS);
            }
        }

        // scroll up
        for (int i = 0; i < scrollTimes; i++) {
            res = findObject(selector, TIME_OUT_SHORT_SHORT);
            if (res != null) {
                return res;
            } else {
                getUiDevice().swipe(startX, endY, endX, startY, STEPS);
            }
        }
        return res;
    }

    public static void scrollDownOnObject(UiObject2 object) {
        int STEPS = 20;
        Rect rect = object.getVisibleBounds();
        int startX = rect.left + rect.width() / 2;
        int endX = rect.left + rect.width() / 2;
        int startY = rect.top + rect.height() / 4 * 3;
        int endY = rect.top + rect.height() / 4;

        getUiDevice().swipe(startX, startY, endX, endY, STEPS);
    }

    public static void scrollUpOnObject(UiObject2 object) {
        int STEPS = 20;
        Rect rect = object.getVisibleBounds();
        int startX = rect.left + rect.width() / 2;
        int endX = rect.left + rect.width() / 2;
        int startY = rect.top + rect.height() / 4;
        int endY = rect.top + rect.height() / 4 * 3;

        getUiDevice().swipe(startX, startY, endX, endY, STEPS);
    }

    public static UiObject2 scrollFromTopOnScreenToFind(BySelector selector) {
        int SCROLL_TIMES = 10;
        int STEPS = 20;
        int startX = Utils.getUiDevice().getDisplayWidth() / 2;
        int endX = Utils.getUiDevice().getDisplayWidth() / 2;
        int startY = 0;
        int endY = Utils.getUiDevice().getDisplayHeight() / 2;

        UiObject2 res = null;
        for (int i = 0; i < SCROLL_TIMES; i++) {
            res = findObject(selector, TIME_OUT_SHORT_SHORT);
            if (res != null) {
                return res;
            } else {
                getUiDevice().swipe(startX, startY, endX, endY, STEPS);
            }
        }
        return res;
    }

    public static UiObject2 scrollOnScreenToFindNo(BySelector selector) {
        return scrollOnScreenToFindNo(selector, SCROLL_TIMES_NORMAL);
    }

    public static UiObject2 scrollOnScreenToFindNo(BySelector selector, int scrollTimes) {
        int STEPS = 20;
        int startX = Utils.getUiDevice().getDisplayWidth() / 2;
        int endX = Utils.getUiDevice().getDisplayWidth() / 2;
        int startY = Utils.getUiDevice().getDisplayWidth() / 4 * 3;
        int endY = Utils.getUiDevice().getDisplayWidth() / 4;

        UiObject2 res = null;
        // scroll down
        for (int i = 0; i < scrollTimes; i++) {
            res = findNoObject(selector, TIME_OUT_SHORT_SHORT);
            if (res != null) {
                return res;
            } else {
                getUiDevice().swipe(startX, startY, endX, endY, STEPS);
            }
        }

        // scroll up
        for (int i = 0; i < scrollTimes; i++) {
            res = findNoObject(selector, TIME_OUT_SHORT_SHORT);
            if (res != null) {
                return res;
            } else {
                getUiDevice().swipe(startX, endY, endX, startY, STEPS);
            }
        }
        return res;
    }

    public static void assertObjectRightNow(final BySelector selector) {
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return getUiDevice().findObject(selector) != null;
            }
        };
        condition.assertMe(TIME_OUT_RIGHT_NOW);
    }

    public static void assertRightNow(boolean assertValue) {
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return assertValue;
            }
        };
        condition.assertMe(TIME_OUT_RIGHT_NOW);
    }

    public static void assertRightNow(boolean assertValue, String failMessage) {
        Condition condition = new Condition() {
            @Override
            public boolean isSatisfied() {
                return assertValue;
            }
        };
        condition.assertMe(TIME_OUT_RIGHT_NOW, failMessage);
    }

    public static boolean waitCondition(Condition condition) {
        return condition.waitMe(TIME_OUT_NORMAL);
    }

    public static boolean waitCondition(Condition condition, int timeout) {
        return condition.waitMe(timeout);
    }

    public static void assertCondition(Condition condition) {
        condition.assertMe(TIME_OUT_NORMAL);
    }

    public static void assertCondition(Condition condition, int timeout) {
        condition.assertMe(timeout);
    }

    public static void waitSafely(int ms) {
        try {
            synchronized (getUiDevice()) {
                getUiDevice().wait(ms);
            }
        } catch (InterruptedException e) {

        }
    }

    public static String getStringInExif(String filePath, String tag, String defaultValue) {
        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            return exifInterface.getAttribute(tag);
        } catch (IOException e) {
            return defaultValue;
        }
    }

    public static int getIntInExif(String filePath, String tag, int defaultValue) {
        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            return exifInterface.getAttributeInt(tag, defaultValue);
        } catch (IOException e) {
            return defaultValue;
        }
    }

    public static double getDoubleInExif(String filePath, String tag, double defaultValue) {
        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            return exifInterface.getAttributeDouble(tag, defaultValue);
        } catch (IOException e) {
            return defaultValue;
        }
    }

    public static boolean isFeatureSupported(String tag) {
        if (mFeatureSpecParser == null) {
            mFeatureSpecParser = new FeatureSpecParser(getContext());
        }
        return mFeatureSpecParser.isFeatureSupported(tag);
    }

    public static void delete(File file) {
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            if (files != null) {
                for (File temp : files) {
                    delete(temp);
                }
            }
        }
    }

    public static void longPress(UiObject2 object2) {
        int defaultDurationMills = 500;
        longPress(object2, defaultDurationMills);
    }

    public static void longPress(UiObject2 object2, int durationMillSeconds) {
        int x = object2.getVisibleBounds().centerX();
        int y = object2.getVisibleBounds().centerY();
        getUiDevice().swipe(x, y, x, y, getSwipeStepsByDuration(durationMillSeconds));
    }

    public static boolean zoom(UiObject2 object, float percentStart, float percentEnd, long
            timeMills) {
        Rect rect = object.getVisibleBounds();
        Point startLeftBottom = getPointAtPercent(rect, (1 - percentStart) / 2);
        Point endLeftBottom = getPointAtPercent(rect, (1 - percentEnd) / 2);
        Point startRightTop = getPointAtPercent(rect, (1 - percentStart) / 2 + percentStart);
        Point endRightTop = getPointAtPercent(rect, (1 - percentEnd) / 2 + percentEnd);

        return zoom(startLeftBottom, endLeftBottom, startRightTop, endRightTop, timeMills);
    }

    private static Point getPointAtPercent(Rect rect, float percent) {
        Point start = new Point(rect.left, rect.bottom);
        Point end = new Point(rect.right, rect.top);

        return lerp(start, end, percent);
    }

    public static boolean zoom(Point start1, Point end1, Point start2, Point end2, long
            timeMills) {
        if (mInputManager == null || mInjectInputMethod == null) {
            LogHelper.d(TAG, "[zoom] mInputManager == null || mInjectInputMethod == " +
                    "null, return false");
            return false;
        }

        long downTime = SystemClock.uptimeMillis();
        int event_time_gap = 30;
        int totalStep = (int) (timeMills / event_time_gap);

        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[2];
        MotionEvent.PointerProperties pointerProperties1 = new MotionEvent.PointerProperties();
        MotionEvent.PointerProperties pointerProperties2 = new MotionEvent.PointerProperties();
        pointerProperties1.id = 0;
        pointerProperties1.toolType = MotionEvent.TOOL_TYPE_FINGER;
        pointerProperties2.id = 1;
        pointerProperties2.toolType = MotionEvent.TOOL_TYPE_FINGER;
        pointerProperties[0] = pointerProperties1;
        pointerProperties[1] = pointerProperties2;

        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[2];
        MotionEvent.PointerCoords pointerCoords1 = new MotionEvent.PointerCoords();
        MotionEvent.PointerCoords pointerCoords2 = new MotionEvent.PointerCoords();
        pointerCoords1.x = start1.x;
        pointerCoords1.y = start1.y;
        pointerCoords1.pressure = 1;
        pointerCoords1.size = 1;
        pointerCoords2.x = start2.x;
        pointerCoords2.y = start2.y;
        pointerCoords2.pressure = 1;
        pointerCoords2.size = 1;
        pointerCoords[0] = pointerCoords1;
        pointerCoords[1] = pointerCoords2;

        // click down point 1
        MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN, 1,
                pointerProperties,
                pointerCoords,
                0, 0, 1, 1, 0, 0, 0, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        injectInputEvent(event);

        // click down point 2
        event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_POINTER_DOWN +
                        (pointerProperties2.id << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                2,
                pointerProperties,
                pointerCoords,
                0, 0, 1, 1, 0, 0, 0, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        injectInputEvent(event);

        // action move
        for (int step = 1; step < totalStep; step++) {
            Point point1 = lerp(start1, end1, step, totalStep);
            Point point2 = lerp(start2, end2, step, totalStep);
            pointerCoords[0].x = point1.x;
            pointerCoords[0].y = point1.y;
            pointerCoords[1].x = point2.x;
            pointerCoords[1].y = point2.y;

            event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_MOVE, 2, pointerProperties,
                    pointerCoords, 0, 0, 1, 1, 0, 0, 0, 0);
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            injectInputEvent(event);

            waitSafely(event_time_gap);
        }

        // click up point 1
        pointerCoords[0].x = end1.x;
        pointerCoords[0].y = end1.y;
        pointerCoords[1].x = end2.x;
        pointerCoords[1].y = end2.y;
        event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_POINTER_UP +
                        (pointerProperties2.id << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                1,
                pointerProperties,
                pointerCoords,
                0, 0, 1, 1, 0, 0, 0, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        injectInputEvent(event);

        // click up point 2
        pointerCoords[0].x = end1.x;
        pointerCoords[0].y = end1.y;
        pointerCoords[1].x = end2.x;
        pointerCoords[1].y = end2.y;
        event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, 2,
                pointerProperties,
                pointerCoords,
                0, 0, 1, 1, 0, 0, 0, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        injectInputEvent(event);

        return true;
    }

    public static boolean injectInputEvent(InputEvent event) {
        if (mInputManager == null || mInjectInputMethod == null) {
            LogHelper.d(TAG, "[injectInputEvent] mInputManager == null || mInjectInputMethod == " +
                    "null, return false");
            return false;
        }
        ReflectUtils.callMethodOnObject(mInputManager, mInjectInputMethod, event, 0);
        return true;
    }

    public static void copyFile(String fromPath, String toPath) {
        int readByteCount = 0;
        try {
            FileInputStream fsFrom = new FileInputStream(fromPath);
            FileOutputStream fsTo = new FileOutputStream(toPath);
            byte[] buffer = new byte[BUFFER_SIZE_OF_COPY];
            LogHelper.d(TAG, "[copyFile] start copy buffer");
            while ((readByteCount = fsFrom.read(buffer)) != -1) {
                fsTo.write(buffer, 0, readByteCount);
            }
            LogHelper.d(TAG, "[copyFile] end copy buffer");
            fsFrom.close();
            fsTo.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    private static Point lerp(Point start, Point end, int currentStep, int totalStep) {
        Point res = new Point();
        float alpha = (float) currentStep / (float) totalStep;
        res.x = (int) ((float) (end.x - start.x) * alpha + start.x);
        res.y = (int) ((float) (end.y - start.y) * alpha + start.y);
        return res;
    }

    private static Point lerp(Point start, Point end, float percent) {
        Point res = new Point();
        res.x = (int) ((float) (end.x - start.x) * percent + start.x);
        res.y = (int) ((float) (end.y - start.y) * percent + start.y);
        return res;
    }

    public static int getSwipeStepsByDuration(int durationMillSeconds) {
        int millsPreStep = 5; // 5ms
        return durationMillSeconds / millsPreStep;
    }

    public static long getReserveSpaceInMB() {
        return getReserveSpaceInByte() / 1024 / 1024;
    }

    public static long getReserveSpaceInByte() {
        StatFs statFs = new StatFs(getDefaultStoragePath());
        long b = statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong();
        return b;
    }

    public static String getDefaultStoragePath() {
        return StorageManagerExt.getDefaultPath();
    }

    public static String getFileName(String filePath) {
        if (filePath == null) {
            return null;
        }

        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    public static void rebootDevice() {
        getContext().sendBroadcast(new Intent("com.mediatek.reboot"));
        if (Utils.waitObject(By.textContains("Shutting down"))) {
            // wait shutting down
            Utils.waitSafely(WAIT_POWER_OFF_IN_MS);
        }
    }

    public static boolean isDenoiseSupported() {
        return SystemProperties.getInt("ro.mtk_cam_dualdenoise_support", 0) == 1;
    }
}
