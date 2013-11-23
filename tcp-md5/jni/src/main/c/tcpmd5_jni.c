/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
#include <org_opendaylight_bgpcep_tcpmd5_jni_NarSystem.h>
#include <org_opendaylight_bgpcep_tcpmd5_jni_NativeKeyAccess.h>

#include <errno.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <linux/tcp.h>

struct handler {
	jclass   clazz;
	jfieldID field;
};

static struct handler *handlers = NULL;
static unsigned handler_count = 0;
static jclass illegal_argument = NULL;
static jclass illegal_state = NULL;
static jclass io = NULL;

static void __attribute__ ((format (printf, 3, 4))) jni_exception(JNIEnv *env, jclass clazz, const char *fmt, ...)
{
	char buf[1024] = "";
	va_list ap;

	va_start(ap, fmt);
	vsnprintf(buf, sizeof(buf), fmt, ap);
	va_end(ap);

	(*env)->ThrowNew(env, clazz, buf);
}

#define ILLEGAL_ARGUMENT(...) jni_exception(env, illegal_argument, __VA_ARGS__)
#define ILLEGAL_STATE(...) jni_exception(env, illegal_state, __VA_ARGS__)
#define IO(...) jni_exception(env, io, __VA_ARGS__)

static const struct handler *add_handler(JNIEnv *env, const char *name, const char *field, const char *sig)
{
	// Find the class
	const jclass clazz = (*env)->FindClass(env, name);
	if (clazz == NULL) {
		goto out;
	}

	// Find the field
	const jfieldID fid = (*env)->GetFieldID(env, clazz, field, sig);
	if (fid == NULL) {
		goto out_clazz;
	}

	// Reallocate the array
	struct handler *r = realloc(handlers, (handler_count + 1) * sizeof(struct handler));
	if (r == NULL) {
		// FIXME: print the error
		goto out_clazz;
	}
	handlers = r;

	// Fill the array
	struct handler *ret = handlers + handler_count;
	ret->clazz = (*env)->NewGlobalRef(env, clazz);
	ret->field = fid;

	if (ret->clazz == NULL) {
		goto out_clazz;
	}

	(*env)->DeleteLocalRef(env, clazz);
	return ret;

out_clazz:
	(*env)->DeleteLocalRef(env, clazz);
out:
	(*env)->ExceptionDescribe(env);
	(*env)->ExceptionClear(env);
	return NULL;
}

static const struct handler *find_handler(JNIEnv *env, jobject channel)
{
	const jclass clazz = (*env)->GetObjectClass(env, channel);
	if (clazz != NULL) {
		unsigned i;

		for (i = 0; i < handler_count; ++i) {
			const struct handler *h = handlers + i;
			if ((*env)->IsAssignableFrom(env, clazz, h->clazz) == JNI_TRUE) {
				return h;
			}
		}
	}

	return NULL;
}

static jclass resolve_class(JNIEnv *env, const char *what, jclass *where)
{
	if (*where != NULL) {
		jclass clazz = (*env)->FindClass(env, what);
		if (clazz == NULL) {
			return NULL;
		}

		jclass ref = (*env)->NewGlobalRef(env, clazz);
		(*env)->DeleteLocalRef(env, clazz);
		if (ref == NULL) {
			return NULL;
		}

		*where = ref;
		return ref;
	} else {
		return *where;
	}
}

static void native_error(JNIEnv *env, int err)
{
	char buf[256] = "";
	strerror_r(err, buf, sizeof(buf));
	IO("Native operation failed: %s", buf);
}

/*
 * Class:     org_opendaylight_bgpcep_tcpmd5_jni_NarSystem
 * Method:    runUnitTestsNative
 * Signature: ()I
 */
jint Java_org_opendaylight_bgpcep_tcpmd5_jni_NarSystem_runUnitTestsNative(JNIEnv *env, jobject obj)
{
	if (resolve_class(env, "java/lang/IllegalStateException", &illegal_state) == NULL) {
		return 0;
	}
	if (resolve_class(env, "java/lang/IllegalArgumentException", &illegal_argument) == NULL) {
		return 0;
	}
	if (resolve_class(env, "java/io/IOException", &io) == NULL) {
		return 0;
	}

	int ret = 0;
	if (add_handler(env, "sun/nio/ch/ServerSocketChannelImpl", "fdVal", "I")) {
		++ret;
	}
	if (add_handler(env, "sun/nio/ch/SocketChannelImpl", "fdVal", "I")) {
		++ret;
	}

	return ret;
}

/*
 * Class:     org_opendaylight_bgpcep_tcpmd5_jni_NativeKeyAccess
 * Method:    isChannelSupported0
 * Signature: (Ljava/nio/channels/Channel;)Z
 */
jboolean Java_org_opendaylight_bgpcep_tcpmd5_jni_NativeKeyAccess_isChannelSupported0(JNIEnv *env, jclass clazz, jobject channel)
{
	const struct handler *h = find_handler(env, channel);
	return h == NULL ? JNI_FALSE : JNI_TRUE;
}

/*
 * Class:     org_opendaylight_bgpcep_tcpmd5_jni_NativeKeyAccess
 * Method:    getChannelKey0
 * Signature: (Ljava/nio/channels/Channel;)[B
 */
jbyteArray Java_org_opendaylight_bgpcep_tcpmd5_jni_NativeKeyAccess_getChannelKey0(JNIEnv *env, jclass clazz, jobject channel)
{
	const struct handler *h = find_handler(env, channel);
	if (h == NULL) {
		ILLEGAL_STATE("Failed to find handler");
		return NULL;
	}

	const jint fd = (*env)->GetIntField(env, channel, h->field);
	if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
		return NULL;
	}

	struct tcp_md5sig md5sig;
	memset(&md5sig, 0, sizeof(md5sig));
	socklen_t len = sizeof(md5sig);
	if (getsockopt(fd, IPPROTO_TCP, TCP_MD5SIG, &md5sig, &len) != 0) {
		native_error(env, errno);
		return NULL;
	}

	jbyteArray ret = (*env)->NewByteArray(env, md5sig.tcpm_keylen);
	if (ret != NULL) {
		(*env)->SetByteArrayRegion(env, ret, 0, md5sig.tcpm_keylen, (jbyte *)md5sig.tcpm_key);
	}

	return ret;
}

/*
 * Class:     org_opendaylight_bgpcep_tcpmd5_jni_NativeKeyAccess
 * Method:    setChannelKey0
 * Signature: (Ljava/nio/channels/Channel;[B)V
 */
void Java_org_opendaylight_bgpcep_tcpmd5_jni_NativeKeyAccess_setChannelKey0(JNIEnv *env, jclass clazz, jobject channel, jbyteArray key)
{
	const jsize keylen = (*env)->GetArrayLength(env, key);
	if (keylen < 0) {
		ILLEGAL_ARGUMENT("Negative array length %d encountered", keylen);
		return;
	}
	if (keylen > TCP_MD5SIG_MAXKEYLEN) {
		ILLEGAL_ARGUMENT("Key length %d exceeds platform limit %d", keylen, TCP_MD5SIG_MAXKEYLEN);
		return;
	}

	struct tcp_md5sig md5sig;
	memset(&md5sig, 0, sizeof(md5sig));
	md5sig.tcpm_keylen = (uint16_t) keylen;

	/*
	 * TCP_MD5SIG_MAXKEYLEN may not be an accurate check of key field
	 * length. Check the field size before accessing it.
	 */
	if (keylen > sizeof(md5sig.tcpm_key)) {
		ILLEGAL_ARGUMENT("Key length %d exceeds native buffer limit %zu", keylen, sizeof(md5sig.tcpm_key));
		return;
	}

	const struct handler *h = find_handler(env, channel);
	if (h == NULL) {
		ILLEGAL_STATE("Failed to find handler");
		return;
	}

	(*env)->GetByteArrayRegion(env, key, 0, keylen, (void *) &md5sig.tcpm_key);
	if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
		return;
	}

	const jint fd = (*env)->GetIntField(env, channel, h->field);
	if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
		return;
	}

	const int ret = setsockopt(fd, IPPROTO_TCP, TCP_MD5SIG, &md5sig, sizeof(md5sig));
	if (ret != 0) {
		native_error(env, errno);
	}
}

