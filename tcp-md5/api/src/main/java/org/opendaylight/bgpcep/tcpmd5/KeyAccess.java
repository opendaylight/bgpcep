/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5;

import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Interface for accessing key information attached to an object.
 */
public interface KeyAccess {
	/**
	 * Retrieve the key mapping.
	 *
	 * @return The key mapping currently attached.
	 * @throws IOException when the retrieve operation fails.
	 */
	@Nonnull KeyMapping getKeys() throws IOException;

	/**
	 * Attach key mappings.
	 *
	 * @param keys Mappings which should
	 * @throws IOException when the set operation fails.
	 * @throws IllegalArgumentException if a key length is zero or it exceeds
	 *         platform-supported length (usually 80 bytes).
	 */
	void setKeys(@Nonnull KeyMapping keys) throws IOException;
}
