set(TARGET_NAME VINS_Android)
set(${TARGET_NAME}_SRCS
    ViewController.cpp
)

SET(OpenCV_DIR /Users/patrick/Documents/code/android/VINS_Mobile_AndroidPort/OpenCV-android-sdk_3_4_6/sdk/native/jni)
find_package(OpenCV REQUIRED)

message(STATUS "OpenCV_INCLUDE_DIRS = ${OpenCV_INCLUDE_DIRS}")
message(STATUS "OpenCV_LIBS = ${OpenCV_LIBS}")


add_library(
    ${TARGET_NAME}
    STATIC
    ${${TARGET_NAME}_SRCS}
)

target_link_libraries( 
    ${TARGET_NAME} 
	VINSMobile
    android
    log
    ${OpenCV_LIBS}
)

target_include_directories(${TARGET_NAME} INTERFACE ${CMAKE_CURRENT_SOURCE_DIR})
