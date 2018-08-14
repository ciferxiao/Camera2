LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := mediatek-framework
LOCAL_JAVA_LIBRARIES += com.mediatek.camera.ext

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_MODULE := com.mediatek.camera.portability

include $(BUILD_STATIC_JAVA_LIBRARY)
