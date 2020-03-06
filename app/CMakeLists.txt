# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)



#message(FATAL_ERROR "HA HAHAHAHHAHAAH?")
#Xukan use native-lib
set(LIB_NAME NativeLib)
#set(LIB_NAME native-lib)
set(
    ${LIB_NAME}_SRCS
    src/main/cpp/jni/nativelib.cpp
)

add_subdirectory(libs/VINS-Mobile-master)
#message(FATAL_ERROR "Done with VINS-Mobile-master")
add_subdirectory(src/main/cpp/VINS_Android)
# for tf libs
#add_subdirectory(jni)
# Gradle automatically packages shared libraries with your APK.
add_library( 
    ${LIB_NAME}
    SHARED
    ${${LIB_NAME}_SRCS}
)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( 
    # Sets the name of the path variable.
    log-lib
    
    # Specifies the name of the NDK library that
    # you want CMake to locate.
    log 
)

#add_dependencies(nativelib VINS_Android)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

#xukan add opencv
#add_library( opencv SHARED IMPORTED )
#set_target_properties(opencv PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)

target_link_libraries( 
    # Specifies the target library.
    NativeLib
    #xukan use following instead of up
    #native-lib
    #xukan add opencv lib
	#opencv
	# VINSMobile
    VINS_Android
    log
	#tf_jni
    # Links the target library to the log library
    # included in the NDK.
    #${log-lib}
)
