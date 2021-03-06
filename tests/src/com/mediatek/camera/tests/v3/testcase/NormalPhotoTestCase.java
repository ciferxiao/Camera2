package com.mediatek.camera.tests.v3.testcase;

import com.mediatek.camera.tests.v3.annotation.group.CameraBasicTest;
import com.mediatek.camera.tests.v3.annotation.module.NormalPhotoTest;
import com.mediatek.camera.tests.v3.annotation.module.ThirdPartyLaunchPhotoTest;
import com.mediatek.camera.tests.v3.annotation.type.FunctionTest;
import com.mediatek.camera.tests.v3.annotation.type.StabilityTest;
import com.mediatek.camera.tests.v3.arch.MetaCase;
import com.mediatek.camera.tests.v3.checker.CameraFacingChecker;
import com.mediatek.camera.tests.v3.checker.CameraLaunchedChecker;
import com.mediatek.camera.tests.v3.checker.CapturedPhotoLocationChecker;
import com.mediatek.camera.tests.v3.checker.CapturedPhotoPictureSizeChecker;
import com.mediatek.camera.tests.v3.checker.FlashExifChecker;
import com.mediatek.camera.tests.v3.checker.IsoExifChecker;
import com.mediatek.camera.tests.v3.checker.PreviewChecker;
import com.mediatek.camera.tests.v3.checker.ThumbnailChecker;
import com.mediatek.camera.tests.v3.checker.ThumbnailShownInGalleryChecker;
import com.mediatek.camera.tests.v3.observer.MediaSavedObserver;
import com.mediatek.camera.tests.v3.observer.PhotoSavedObserver;
import com.mediatek.camera.tests.v3.observer.ZoomUiObserver;
import com.mediatek.camera.tests.v3.operator.BackToCameraOperator;
import com.mediatek.camera.tests.v3.operator.CapturePhotoOperator;
import com.mediatek.camera.tests.v3.operator.ConnectWifiOperator;
import com.mediatek.camera.tests.v3.operator.ContinuousShotOperator;
import com.mediatek.camera.tests.v3.operator.FlashOperator;
import com.mediatek.camera.tests.v3.operator.FontSizeOperator;
import com.mediatek.camera.tests.v3.operator.GoToGalleryOperator;
import com.mediatek.camera.tests.v3.operator.IsoOperator;
import com.mediatek.camera.tests.v3.operator.LocationOperator;
import com.mediatek.camera.tests.v3.operator.OnSingleTapUpOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByBackKeyOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByHomeKeyOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByPowerKeyOperator;
import com.mediatek.camera.tests.v3.operator.PauseResumeByRecentAppOperator;
import com.mediatek.camera.tests.v3.operator.PictureSizeOperator;
import com.mediatek.camera.tests.v3.operator.SwitchCameraOperator;
import com.mediatek.camera.tests.v3.operator.SwitchPhotoVideoOperator;
import com.mediatek.camera.tests.v3.operator.VolumeKeyDownOperator;
import com.mediatek.camera.tests.v3.operator.ZoomOperator;
import com.mediatek.camera.tests.v3.util.Utils;

import org.junit.Test;

/**
 * Normal photo mode test case.
 */

public class NormalPhotoTestCase extends BaseCameraTestCase {

    /**
     * Test enter and exit function.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testEntryAndExit() {
        new MetaCase("TC_Camera_NormalPhoto_0001")
                .addChecker(new PreviewChecker())
                .addOperator(new SwitchCameraOperator())
                .run();
    }

    /**
     * Test Entry and exit stability.
     */
    @Test
    @StabilityTest
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testEntryAndExitStability() {
        new MetaCase("TC_Camera_NormalPhoto_0002")
                .addOperator(new PauseResumeByBackKeyOperator())
                .addChecker(new PreviewChecker())
                .runForTimes(Utils.STABILITY_REPEAT_TIMES);
    }

    /**
     * Test switch mode between photo and video.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testSwitchMode() {
        new MetaCase("TC_Camera_NormalPhoto_0004")
                .addChecker(new PreviewChecker())

                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)

                .addOperator(new SwitchPhotoVideoOperator(), SwitchPhotoVideoOperator.INDEX_VIDEO)
                .addChecker(new PreviewChecker())
                .addOperator(new SwitchPhotoVideoOperator(), SwitchPhotoVideoOperator.INDEX_PHOTO)

                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)

                .run();
    }

    /**
     * Change the system font size to huge and test capture function.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testCaptureWithHugeFontSize() {
        new MetaCase("TC_Camera_NormalPhoto_0008")
                .addOperator(new FontSizeOperator(), FontSizeOperator.INDEX_HUGE).run();

        new MetaCase("TC_Camera_NormalPhoto_0008")
                .addChecker(new PreviewChecker())

                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .run();
    }

    /**
     * Test single capture function.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testSingleCapture() {
        new MetaCase("TC_Camera_NormalPhoto_0013")
                .addChecker(new PreviewChecker())

                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .run();
    }

    /**
     * Test long press shutter to capture more than one picture.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testLongPressShutter() {
        new MetaCase("TC_Camera_NormalPhoto_0014")
                .addChecker(new PreviewChecker())
                .observeBegin(new MediaSavedObserver(), MediaSavedObserver.INDEX_MULTI_SAVED)
                .addOperator(new ContinuousShotOperator())
                .observeEnd()
                .run();
    }

    /**
     * Test zoom in and zoom out.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testZoom() {
        new MetaCase("TC_Camera_NormalPhoto_0015")
                .addChecker(new PreviewChecker())

                .observeBegin(new ZoomUiObserver())
                .addOperator(new ZoomOperator(), ZoomOperator.INDEX_ZOOM_IN)
                .observeEnd()

                .observeBegin(new ZoomUiObserver())
                .addOperator(new ZoomOperator(), ZoomOperator.INDEX_ZOOM_OUT)
                .observeEnd()
                .run();
    }

    /**
     * Test capture stability.
     */
    @Test
    @StabilityTest
    @CameraBasicTest
    @NormalPhotoTest
    public void testCaptureStability() {
        new MetaCase("TC_Camera_NormalPhoto_0018")
                .addChecker(new PreviewChecker())
                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)

                .runForTimes(Utils.STABILITY_REPEAT_TIMES);
    }

    /**
     * Test capture with different picture size.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testCaptureWithDifferentSize() {
        new MetaCase("TC_Camera_NormalPhoto_0022")
                .addOperator(new PictureSizeOperator())
                .addChecker(new PreviewChecker())
                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .addChecker(new PreviewChecker())
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .run();
    }

    /**
     * Test capture with location information.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testCaptureWithLocation() {
        new MetaCase("TC_Camera_NormalPhoto_0023")
                .addChecker(new PreviewChecker())

                .addOperator(new ConnectWifiOperator())
                .addOperator(new LocationOperator())

                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()

                .addChecker(new CapturedPhotoLocationChecker())
                .addChecker(new PreviewChecker())
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .run();
    }

    /**
     * Test capture with different picture size, flash and iso setting.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testCaptureExif() {
        new MetaCase("TC_Camera_NormalPhoto_0024")
                .addOperator(new PictureSizeOperator())
                .acrossBegin()
                .addOperator(new FlashOperator())
                .acrossBegin()
                .addOperator(new IsoOperator())

                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()

                .addChecker(new IsoExifChecker())
                .acrossEnd()
                .addChecker(new FlashExifChecker())
                .acrossEnd()

                .addChecker(new CapturedPhotoPictureSizeChecker())
                .run();
    }

    /**
     * Test third party launch and capture with back camera.
     */
    @Test
    @CameraBasicTest
    @ThirdPartyLaunchPhotoTest
    @FunctionTest
    public void testCaptureWithBackCamera() {
        new MetaCase("TC_Camera_NormalPhoto_0030")
                .addChecker(new CameraFacingChecker(), CameraFacingChecker.INDEX_BACK)
                .addChecker(new PreviewChecker())
                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()

                .addChecker(new PreviewChecker())
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)

                .addOperator(new SwitchCameraOperator(), SwitchCameraOperator.INDEX_FRONT)
                .addChecker(new CameraFacingChecker(), CameraFacingChecker.INDEX_FRONT)
                .addChecker(new PreviewChecker())
                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()

                .addChecker(new PreviewChecker())
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .run();
    }

    /**
     * Test switch camera stability.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @StabilityTest
    public void testSwitchCameraStability() {
        new MetaCase("TC_Camera_NormalPhoto_0031")
                .addChecker(new PreviewChecker())
                .acrossBegin()
                .addOperator(new SwitchCameraOperator())
                .acrossEnd()
                .addChecker(new PreviewChecker())
                .runForTimes(Utils.STABILITY_REPEAT_TIMES);
    }

    /**
     * Test capture and goto gallery.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testCaptureAndGotoGallery() {
        new MetaCase("TC_Camera_NormalPhoto_0033")
                .addChecker(new PreviewChecker())
                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)

                .addOperator(new GoToGalleryOperator())
                .addChecker(new ThumbnailShownInGalleryChecker())

                .addOperator(new BackToCameraOperator())
                .addChecker(new CameraLaunchedChecker())
                .addChecker(new PreviewChecker())

                .run();
    }

    /**
     * Test touch focus and capture.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testCaptureWithTouchFocus() {
        new MetaCase("TC_Camera_NormalPhoto_0034")
                .addChecker(new PreviewChecker())
                .addOperator(new OnSingleTapUpOperator())

                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new CapturePhotoOperator())
                .observeEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .run();
    }

    /**
     * Test pause/resume with home key and power key.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testPauseAndResume() {
        new MetaCase("TC_Camera_NormalPhoto_0035")
                .addOperator(new SwitchCameraOperator())
                .addOperator(new PauseResumeByHomeKeyOperator())
                .addChecker(new PreviewChecker())
                .addOperator(new PauseResumeByPowerKeyOperator())
                .addChecker(new PreviewChecker())
                .run();
    }

    /**
     * Test pause/resume with recent app.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testPauseAndResumeWithRecentApp() {
        new MetaCase("TC_Camera_NormalPhoto_0035")
                .addOperator(new SwitchCameraOperator())
                .addOperator(new PauseResumeByRecentAppOperator())
                .run();
    }


    /**
     * Test pause/resume stability.
     */
    @Test
    @StabilityTest
    @CameraBasicTest
    @NormalPhotoTest
    public void testPauseAndResumeStability() {
        new MetaCase("TC_Camera_NormalPhoto_0036")
                .addOperator(new PauseResumeByHomeKeyOperator())
                .addChecker(new PreviewChecker())
                .runForTimes(Utils.STABILITY_REPEAT_TIMES);
    }

    /**
     * Test pause when capturing.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testPauseWhenCapturing() {
        new MetaCase("TC_Camera_NormalPhoto_0037")
                .addChecker(new PreviewChecker())
                .addOperator(new CapturePhotoOperator())
                .addOperator(new PauseResumeByHomeKeyOperator())
                .addChecker(new PreviewChecker())
                .run();
    }

    /**
     * Test pause when switch camera.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testPauseWhenSwitchCamera() {
        new MetaCase("TC_Camera_NormalPhoto_0038")
                .addChecker(new PreviewChecker())
                .addOperator(new SwitchCameraOperator(), SwitchCameraOperator.INDEX_FRONT)
                .addOperator(new PauseResumeByHomeKeyOperator())
                .addChecker(new PreviewChecker())
                .run();
    }

    /**
     * Test capture with volume key.
     */
    @Test
    @CameraBasicTest
    @NormalPhotoTest
    @FunctionTest
    public void testCaptureWithVolumeKey() {
        new MetaCase("TC_Camera_NormalPhoto_0041")
                .addChecker(new PreviewChecker())
                .observeBegin(new PhotoSavedObserver(1))
                .addOperator(new VolumeKeyDownOperator())
                .observeEnd()
                .addChecker(new ThumbnailChecker(), ThumbnailChecker.INDEX_HAS_THUMB)
                .run();
    }
}