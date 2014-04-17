/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.jni;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Implementation of KeyAccess using Java Native Interface plugin to talk
 * directly to the underlying operating system.
 */
public final class NativeKeyAccess implements KeyAccess {
	private static final String LIBNAME = "libtcpmd5-jni.so";
	private static final Logger LOG = LoggerFactory.getLogger(NativeKeyAccess.class);
	private static boolean AVAILABLE = false;

	private static InputStream getLibraryStream() {
		return Preconditions.checkNotNull(NativeKeyAccess.class.getResourceAsStream('/' + LIBNAME),
				String.format("Failed to open library resource %s", LIBNAME));
	}

	static {
		final Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-x---");

		try (final InputStream is = getLibraryStream()) {
			try {
				final Path p = Files.createTempFile(LIBNAME, null, PosixFilePermissions.asFileAttribute(perms));

				LOG.info("Copying {} to {}", is, p);

				try {
					Files.copy(is, p, StandardCopyOption.REPLACE_EXISTING);

					try {
						Runtime.getRuntime().load(p.toString());

						LOG.info("Library {} loaded", p);

						int rt = NarSystem.runUnitTests();
						if (rt == 0) {
							LOG.warn("Run-time initialization failed");
						} else {
							LOG.debug("Run-time found {} supported channel classes", rt);
							AVAILABLE = true;
						}
					} catch (RuntimeException e) {
						LOG.error("Failed to load native library", e);
					}
				} catch (IOException e) {
					LOG.error("Failed to extract native library", e);
				} finally {
					try {
						Files.deleteIfExists(p);
					} catch (IOException e) {
						LOG.warn("Failed to remove temporary file", e);
					}
				}
			} catch (IOException e2) {
				LOG.error("Failed to create temporary file {}", LIBNAME, e2);
			}
		} catch (IOException e1) {
			LOG.error("Failed to find native library {}", LIBNAME, e1);
		}
	}

	private static native boolean isClassSupported0(Class<?> channel);
	private static native byte[] getChannelKey0(Channel channel) throws IOException;
	private static native void setChannelKey0(Channel channel, byte[] key) throws IOException;

	private final Channel channel;

	private NativeKeyAccess(final Channel channel) {
		this.channel = Preconditions.checkNotNull(channel);
	}

	public static KeyAccess create(final Channel channel) {
		if (!AVAILABLE) {
			LOG.debug("Native library not available");
			return null;
		}

		if (!isClassSupported0(channel.getClass())) {
			LOG.debug("No support available for class {}", channel.getClass());
			return null;
		}

		return new NativeKeyAccess(channel);
	}

	public static boolean isAvailableForClass(final Class<?> clazz) {
		if (!AVAILABLE) {
			LOG.debug("Native library not available");
			return false;
		}

		if (!isClassSupported0(clazz)) {
			LOG.debug("No support available for class {}", clazz);
			return false;
		}

		return true;
	}

	@Override
	public byte[] getKey() throws IOException {
		synchronized (channel) {
			return getChannelKey0(channel);
		}
	}

	@Override
	public void setKey(final byte[] key) throws IOException {
		synchronized (channel) {
			setChannelKey0(channel, Preconditions.checkNotNull(key));
		}
	}
}
