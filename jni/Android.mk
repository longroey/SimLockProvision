LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_C_INCLUDES := $(MTK_PATH_SOURCE)/external/nvram/libnvram $(MTK_PATH_SOURCE)/kernel/include/ $(MTK_PATH_SOURCE)/external/nvram/libfile_op $(MTK_PATH_CUSTOM)/cgen/inc $(MTK_PATH_CUSTOM)/cgen/cfgfileinc $(MTK_PATH_CUSTOM)/cgen/cfgdefault
LOCAL_SHARED_LIBRARIES := libnvram libfile_op libutils liblog
LOCAL_SRC_FILES := simlock_nvram_jni.cpp
LOCAL_MODULE := libsimlock_nvram
LOCAL_PRELINK_MODULE := true
include $(BUILD_SHARED_LIBRARY)