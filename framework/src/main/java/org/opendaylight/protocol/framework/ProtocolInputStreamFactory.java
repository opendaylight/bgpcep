/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.PipedInputStream;

/**
 * Factory for creating Protocol input streams. Should be implemented to return protocol
 * specific input stream.
 */
public interface ProtocolInputStreamFactory {

	/**
	 * Creates and returns protocol input stream.
	 * @param pis underlying piped input stream
	 * @param pmf protocol message factory
	 * @return protocol specific input stream
	 */
	ProtocolInputStream getProtocolInputStream(PipedInputStream pis, ProtocolMessageFactory pmf);
}
