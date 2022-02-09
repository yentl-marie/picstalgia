#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include "opencv2/imgcodecs.hpp"

extern "C" JNIEXPORT jstring JNICALL
Java_com_app_picstalgia_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


extern "C"
JNIEXPORT void JNICALL
Java_com_app_picstalgia_BlindWatermark_RGB2YCbCr(JNIEnv *env, jobject obj, jstring image_path) {
    const char *path = (*env).GetStringUTFChars(image_path, 0);
    cv::Mat imgIn = imread(path, cv::IMREAD_COLOR);

    

    cv::Mat imgOut;
}