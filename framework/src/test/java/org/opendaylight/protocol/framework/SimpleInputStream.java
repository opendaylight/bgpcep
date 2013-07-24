/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.IOException;
import java.io.PipedInputStream;

public class SimpleInputStream implements ProtocolInputStream {
	public static final ProtocolInputStreamFactory FACTORY = new ProtocolInputStreamFactory() {
		@Override
		public ProtocolInputStream getProtocolInputStream(final PipedInputStream pis, final ProtocolMessageFactory pmf) {
			return new SimpleInputStream();
		}
	};

	private SimpleInputStream() {
	}

	@Override
	public boolean isMessageAvailable() throws IOException {
		return true;
	}

	@Override
	public ProtocolMessage getMessage() throws DeserializerException, IOException, DocumentedException {
		return null;
	}
}
