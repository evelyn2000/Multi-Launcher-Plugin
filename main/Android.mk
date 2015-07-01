
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-common android-support-v13 MtpLauncher.library
# LOCAL_JAVA_LIBRARIES := zlingyin.framework

LOCAL_SRC_FILES := $(call all-java-files-under, src) 
LOCAL_SRC_FILES += $(call all-renderscript-files-under, src)

jzs_jar_dir := ../share.library/res
res_dirs := $(jzs_jar_dir) res

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.jzs.dr.mtplauncher.sjar


LOCAL_AAPT_FLAGS += -c mdpi
LOCAL_AAPT_FLAGS += -c hdpi
LOCAL_AAPT_FLAGS += -c xhdpi
LOCAL_AAPT_FLAGS += -c xxhdpi

LOCAL_PACKAGE_NAME := QsMtpLauncher
# LOCAL_CERTIFICATE := platform
LOCAL_CERTIFICATE := shared
LOCAL_OVERRIDES_PACKAGES := Home
LOCAL_PRIVILEGED_MODULE := true

#LOCAL_PROGUARD_ENABLED := custom
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
