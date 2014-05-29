/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
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

static void __attribute__ ((format (printf, 3, 4))) log_message(JNIEnv *env, jobject logger, const char *fmt, ...)
{
	va_list ap;
	va_start(ap, fmt);
	vfprintf(stderr, fmt, ap);
	va_end(ap);
}

#define ERROR(...) log_message(env, logger, __VA_ARGS__)
#define WARN(...) log_message(env, logger, __VA_ARGS__)

static const struct handler *add_handler(JNIEnv *env, jobject logger, const char *name, const char *field, const char *sig)
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
		ERROR("Failed to allocate handler: %s", strerror(errno));
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

	handler_count += 1;
	(*env)->DeleteLocalRef(env, clazz);
	return ret;

out_clazz:
	(*env)->DeleteLocalRef(env, clazz);
out:
	(*env)->ExceptionDescribe(env);
	(*env)->ExceptionClear(env);
	return NULL;
}

static const struct handler *find_handler(JNIEnv *env, jclass clazz)
{
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
	if (*where == NULL) {
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

static void native_error(JNIEnv *env, const char *what, int err)
{
	char buf[256] = "";
	strerror_r(err, buf, sizeof(buf));
	IO("Native operation %s failed: %s", what, buf);
}

static int resolve_exceptions(JNIEnv *env)
{
	// First resolve Exception classes, these are mandatory
	if (resolve_class(env, "java/lang/IllegalStateException", &illegal_state) == NULL) {
		fprintf(stderr, "Failed to resolve java.lang.IllegalStateException\n");
		return 1;
	}
	if (resolve_class(env, "java/lang/IllegalArgumentException", &illegal_argument) == NULL) {
		fprintf(stderr, "Failed to resolve java.lang.IllegalArgumentException\n");
		return 1;
	}
	if (resolve_class(env, "java/io/IOException", &io) == NULL) {
		fprintf(stderr, "Failed to resolve java.io.IOException\n");
		return 1;
	}

	return 0;
}

static jobject resolve_logging(JNIEnv *env, jclass clazz)
{
	jclass lf = (*env)->FindClass(env, "org/slf4j/LoggerFactory");
	if (lf != NULL) {
		jmethodID mid = (*env)->GetStaticMethodID(env, lf, "getLogger", "(Ljava/lang/Class;)Lorg/slf4j/Logger;");
		if (mid != NULL) {
			return (*env)->CallStaticObjectMethod(env, lf, mid, clazz);
		}
	}

	return NULL;
}

static jint sanity_check(JNIEnv *env, jobject logger)
{
	struct tcp_md5sig md5sig = {
		.tcpm_addr = {
			.ss_family = AF_INET,
		},
	};

	// Sanity-check maximum key size against the structure
	if (TCP_MD5SIG_MAXKEYLEN > sizeof(md5sig.tcpm_key)) {
		ERROR("Structure key size %zu is less than %d", sizeof(md5sig.tcpm_key), TCP_MD5SIG_MAXKEYLEN);
		return -1;
	}

	// Now run a quick check to see if we can really the getsockopt/setsockopt calls are
	// supported.
	const int fd = socket(AF_INET, SOCK_STREAM, 0);
	if (fd == -1) {
		ERROR("Failed to open a socket for sanity tests: %s", strerror(errno));
		return -1;
	}

	// Attempt to set a new key with maximum size
	md5sig.tcpm_keylen = TCP_MD5SIG_MAXKEYLEN;
	if (setsockopt(fd, IPPROTO_TCP, TCP_MD5SIG, &md5sig, sizeof(md5sig)) != 0) {
		ERROR("Failed to set TCP_MD5SIG option: %s", strerror(errno));
		close(fd);
		return -1;
	}

	// Attempt to remove the key
	md5sig.tcpm_keylen = 0;
	if (setsockopt(fd, IPPROTO_TCP, TCP_MD5SIG, &md5sig, sizeof(md5sig)) != 0) {
		ERROR("Failed to clear TCP_MD5SIG option: %s", strerror(errno));
		close(fd);
		return -1;
	}

	// All done
	close(fd);
	return 0;
}

/*
 * Class:     org_opendaylight_bgpcep_tcpmd5_jni_NarSystem
 * Method:    runUnitTestsNative
 * Signature: ()I
 */
jint Java_org_opendaylight_bgpcep_tcpmd5_jni_NarSystem_runUnitTestsNative(JNIEnv *env, jobject obj)
{
	if (resolve_exceptions(env)) {
		return 0;
	}

	jobject logger = resolve_logging(env, (*env)->GetObjectClass(env, obj));
	if (sanity_check(env, logger)) {
		return 0;
	}

	int ret = 0;
	if (add_handler(env, logger, "sun/nio/ch/ServerSocketChannelImpl", "fdVal", "I")) {
		++ret;
	}
	if (add_handler(env, logger, "sun/nio/ch/SocketChannelImpl", "fdVal", "I")) {
		++ret;
	}

	return ret;
}

/*
 * Class:     org_opendaylight_bgpcep_tcpmd5_jni_NativeKeyAccess
 * Method:    isClassSupported0
 * Signature: (Ljava/lang/Class;)Z
 */
jboolean Java_org_opendaylight_bgpcep_tcpmd5_jni_NativeKeyAccess_isClassSupported0(JNIEnv *env, jclass clazz, jclass arg)
{
	const struct handler *h = find_handler(env, arg);
	return h == NULL ? JNI_FALSE : JNI_TRUE;
}

/*
 * Class:     org_opendaylight_bgpcep_tcpmd5_jni_NativeKeyAccess
 * Method:    setChannelKey0
 * Signature: (Ljava/nio/channels/Channel;[B)V
 */
void Java_org_opendaylight_bgpcep_tcpmd5_jni_NativeKeyAccess_setChannelKey0(JNIEnv *env, jclass clazz, jobject channel, jbyteArray key)
{
	jsize keylen = 0;

	if (key != NULL) {
		keylen = (*env)->GetArrayLength(env, key);
		if (keylen < 0) {
			ILLEGAL_ARGUMENT("Negative array length %d encountered", keylen);
			return;
		}
		if (keylen > TCP_MD5SIG_MAXKEYLEN) {
			ILLEGAL_ARGUMENT("Key length %d exceeds platform limit %d", keylen, TCP_MD5SIG_MAXKEYLEN);
			return;
		}
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

	if (keylen != 0) {
		(*env)->GetByteArrayRegion(env, key, 0, keylen, (void *) &md5sig.tcpm_key);
		if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
			return;
		}
	}

	const struct handler *h = find_handler(env, (*env)->GetObjectClass(env, channel));
	if (h == NULL) {
		ILLEGAL_STATE("Failed to find handler");
		return;
	}

	const jint fd = (*env)->GetIntField(env, channel, h->field);
	if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
		return;
	}

	socklen_t al = sizeof(md5sig.tcpm_addr);
	if (getsockname(fd, (struct sockaddr *)&md5sig.tcpm_addr, &al) == -1) {
		native_error(env, "getsockname", errno);
		return;
	}

	if (setsockopt(fd, IPPROTO_TCP, TCP_MD5SIG, &md5sig, sizeof(md5sig)) == -1) {
		native_error(env, "setsockopt", errno);
	}
}

