package com.github.ephemient.logcat

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toLong
import platform.android.JNIEnv
import platform.android.jint
import platform.android.jlong
import platform.android.jobject
import platform.android.jstring
import platform.android.logging.android_log_id_to_name
import platform.android.logging.android_logger_list_alloc
import platform.android.logging.android_logger_list_free
import platform.android.logging.android_logger_list_read
import platform.android.logging.android_logger_open
import platform.android.logging.android_name_to_log_id
import platform.posix.getpid

@ExperimentalForeignApi
@ExperimentalNativeApi
@CName("Java_com_github_ephemient_logcat_Logcat_nativeAlloc")
fun logcatAlloc(env: CPointer<CPointerVarOf<JNIEnv>>, obj: jobject, mode: jint, tail: jint): jlong =
    android_logger_list_alloc(mode, tail.toUInt(), getpid()).toLong()

@ExperimentalForeignApi
@ExperimentalNativeApi
@CName("Java_com_github_ephemient_logcat_Logcat_nativeOpen")
fun logcatOpen(env: CPointer<CPointerVarOf<JNIEnv>>, obj: jobject, ptr: jlong, logId: jint): jlong =
    android_logger_open(ptr.toCPointer(), logId.toUInt()).toLong()

@ExperimentalForeignApi
@ExperimentalNativeApi
@CName("Java_com_github_ephemient_logcat_Logcat_nativeReadMessage")
fun logcatReadMessage(env: CPointer<CPointerVarOf<JNIEnv>>, obj: jobject, ptr: jlong, buf: jobject): jint {
    val buf = env.pointed.pointed?.GetDirectBufferAddress?.invoke(env, buf)
    return android_logger_list_read(ptr.toCPointer(), buf?.reinterpret())
}

@ExperimentalForeignApi
@ExperimentalNativeApi
@CName("Java_com_github_ephemient_logcat_Logcat_nativeClose")
fun logcatClose(env: CPointer<CPointerVarOf<JNIEnv>>, obj: jobject, ptr: jlong) {
    android_logger_list_free(ptr.toCPointer())
}

@ExperimentalForeignApi
@ExperimentalNativeApi
@CName("Java_com_github_ephemient_logcat_Logcat\$Companion_logId")
fun logcatLogId(env: CPointer<CPointerVarOf<JNIEnv>>, obj: jobject, logName: jstring): jint {
    val bytes = env.pointed.pointed?.GetStringUTFChars?.invoke(env, logName, null)
    return try {
        android_name_to_log_id(bytes)
    } finally {
        env.pointed.pointed?.ReleaseStringUTFChars?.invoke(env, logName, bytes)
    }.convert()
}

@ExperimentalForeignApi
@ExperimentalNativeApi
@CName("Java_com_github_ephemient_logcat_Logcat\$Companion_logName")
fun logcatLogName(env: CPointer<CPointerVarOf<JNIEnv>>, obj: jobject, logId: jint): jstring? =
    env.pointed.pointed?.NewStringUTF?.invoke(env, android_log_id_to_name(logId.convert()))
