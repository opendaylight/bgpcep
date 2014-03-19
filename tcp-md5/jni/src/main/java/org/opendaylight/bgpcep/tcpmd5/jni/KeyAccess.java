/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.jni;

import java.io.IOException;

/**
 * Interface for accessing key information attached to an object.
 */
public interface KeyAccess {
	/**
	 * Retrieve the key.
	 *
	 * @return The key currently attached, null if there is no key attached.
	 * @throws IOException when the retrieve operation fails.
	 */
	byte[] getKey() throws IOException;

	/**
	 * Set the key.
	 *
	 * @param key The key to be attached, null indicates removal.
	 * @throws IOException when the set operation fails.
	 */
	void setKey(byte[] key) throws IOException;
}
