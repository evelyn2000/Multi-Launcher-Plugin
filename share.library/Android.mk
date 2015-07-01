LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_STATIC_JAVA_LIBRARIES := android-common 
LOCAL_JAVA_LIBRARIES := zlingyin.framework

LOCAL_MODULE := MtpLauncher.library


JZS_LOCAL_FORCE_INSTALLABLE_MODULE := true
LOCAL_MODULE_PATH := $(LOCAL_PATH)/prebuild

include $(BUILD_STATIC_JAVA_LIBRARY)


