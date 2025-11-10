#include <jni.h>
#include <string>
#include "test.h"

#include <android/log.h>
#define TAG "baiyang"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__);
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__);
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__);

// 3. C++层实现接口
extern "C"  // 使用C的方式来编译代码
JNIEXPORT   // 该方法可以被外部调用（不能少）
jstring     // 返回值
JNICALL     // 约束函数入栈顺序，和堆栈内存清理的规则（可以少）  // 包名+类名+方法名
Java_com_example_cameraopengl_MainActivity_stringFromJNI(JNIEnv* env, jobject /* this */) {
    std::string hello = "Hello from C++";

    Test test;
    test.getTest();
    // 如果是 native-lib.c，env是二级指针，则需要 （env*）->NewStringUTF()
    // 如果是 native-lib.cpp，env是一级指针，则直接 env->NewStringUTF()
    // 因为在cpp和c文件中，env是不一样的
    // cpp的JNIEnv就是原始的结构体，这里只有一个指针；而c的JNIEnv就已经是一级指针了
    // c是没有对象的，想持有env环境，就必须将env传递进去：(env*)->NewStringUTF(env, "test")
    // 而cpp只需要：env->NewStringUTF("test")

    return env->NewStringUTF(test.getTest().c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_cameraopengl_MainActivity_stringGetJNI(JNIEnv* env, jobject /* this */) {
    std::string hello = "Hello from C++";

    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_cameraopengl_MainActivity_stringSetJNI(JNIEnv *env, jobject, jstring name) {
    const char* str;
    str = env->GetStringUTFChars(name, NULL);
    if (!str){
        return NULL;
    }
    std::string res = std::string("set JNI ") + std::string(str);
    env->ReleaseStringChars(name, reinterpret_cast<const jchar*>(str));
    return env->NewStringUTF(res.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_cameraopengl_MainActivity_staticStringFromJNI(JNIEnv *env, jclass clazz) {
    std::string hello = "Hello static function";

    // 从string到jstring
    return env->NewStringUTF(hello.c_str());
}


// C++调用java的数据与方法，数据签名
extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameraopengl_MainActivity_changeName(JNIEnv *env, jobject thiz) {
    jclass mainActivityClass = env->GetObjectClass(thiz);  // 方式1
    // jclass mainActivityClass = env->FindClass("com/example/cameraopengl/MainActivity");  // 方式2

    jfieldID nameFid = env->GetFieldID(mainActivityClass, "name", "Ljava/lang/String;");

    // 从char*到jstring    引用类型需要JNI这个中转站
    const char* value= "Beyond";
    jstring newName = env->NewStringUTF(value);

    env->SetObjectField(thiz, nameFid, newName);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameraopengl_MainActivity_changeAge(JNIEnv *env, jclass clazz) {
    jfieldID ageFid = env->GetStaticFieldID(clazz, "age", "I");
    int age = env->GetStaticIntField(clazz, ageFid);  // C++层获取之前的age

    // int就是jint，所以可以直接用；但是String，必须需要jstring
    env->SetStaticIntField(clazz, ageFid, age + 1);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameraopengl_MainActivity_callAddMethod(JNIEnv *env, jobject thiz) {
    jclass mainActivityClass = env->GetObjectClass(thiz);
    jmethodID addMid = env->GetMethodID(mainActivityClass, "add", "(II)I");

    int res = env->CallIntMethod(thiz, addMid, 1, 1);
    LOGI("两数之和：%d\n", res);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_cameraopengl_MainActivity_callShowStringMethod(JNIEnv *env, jobject thiz) {
     jclass mainActivityClass = env->GetObjectClass(thiz);
     jmethodID showStringMid = env->GetMethodID(mainActivityClass, "showString", "(Ljava/lang/String;)Ljava/lang/String;");

     // 从char*到jstring
     const char* value= "C++ call java method!";
     jstring tmpJstr1 = env->NewStringUTF(value);

    // 从jstring到char*
     jstring tmpJstr2 = (jstring)env->CallObjectMethod(thiz, showStringMid, tmpJstr1);
     const char* res = env->GetStringUTFChars(tmpJstr2, NULL);
     LOGI("字符串：%s\n", res);
}