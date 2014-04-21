/*
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
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.opendaylight.bgpcep.tcpmd5.KeyAccess;
import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class NativeKeyAccessFactory implements KeyAccessFactory {
	private static final Logger LOG = LoggerFactory.getLogger(NativeKeyAccessFactory.class);
	private static final String LIBNAME = "libtcpmd5-jni.so";
	private static NativeKeyAccessFactory INSTANCE = null;

	private final Map<Channel, KeyAccess> channels = new WeakHashMap<>();

	private NativeKeyAccessFactory() {

	}

	/**
	 * Get the singleton instance.
	 *
	 * @return Singleton service instance.
	 * @throws NativeSupportUnavailableException if run-time does not support
	 *         the functions needed to instantiate an operational factory.
	 */
	public static NativeKeyAccessFactory getInstance() throws NativeSupportUnavailableException {
		if (INSTANCE != null) {
			return INSTANCE;
		}

		synchronized (NativeKeyAccessFactory.class) {
			if (INSTANCE == null) {
				try {
					loadLibrary();
				} catch (IOException | RuntimeException e) {
					throw new NativeSupportUnavailableException("Failed to load library", e);
				}

				int rt = NarSystem.runUnitTests();
				if (rt == 0) {
					throw new NativeSupportUnavailableException("Run-time does not support required functionality");
				}

				LOG.debug("Run-time found {} supported channel classes", rt);
				INSTANCE = new NativeKeyAccessFactory();
			}
			return INSTANCE;
		}
	}

	@Override
	public KeyAccess getKeyAccess(final Channel channel) {
		if (!NativeKeyAccess.isClassSupported0(channel.getClass())) {
			LOG.debug("No support available for class {}", channel.getClass());
			return null;
		}

		synchronized (channels) {
			KeyAccess e = channels.get(channel);
			if (e == null) {
				e = new NativeKeyAccess(channel);
				channels.put(channel, e);
			}

			return e;
		}
	}

	@Override
	public boolean canHandleChannelClass(final Class<? extends Channel> clazz) {
		if (!NativeKeyAccess.isClassSupported0(Preconditions.checkNotNull(clazz))) {
			LOG.debug("No support available for class {}", clazz);
			return false;
		}

		return true;
	}

	private static void loadLibrary() throws IOException {
		final InputStream is = NativeKeyAccessFactory.class.getResourceAsStream('/' + LIBNAME);
		if (is == null) {
			throw new IOException(String.format("Failed to open library resource %s", LIBNAME));
		}

		try {
			final Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-x---");
			final Path p = Files.createTempFile(LIBNAME, null, PosixFilePermissions.asFileAttribute(perms));

			try {
				LOG.debug("Copying {} to {}", is, p);

				Files.copy(is, p, StandardCopyOption.REPLACE_EXISTING);

				Runtime.getRuntime().load(p.toString());
				LOG.info("Library {} loaded", p);
			} finally {
				try {
					Files.deleteIfExists(p);
				} catch (IOException e) {
					LOG.warn("Failed to remove temporary file {}", p, e);
				}
			}
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				LOG.warn("Failed to close library input stream", e);
			}
		}
	}
}
