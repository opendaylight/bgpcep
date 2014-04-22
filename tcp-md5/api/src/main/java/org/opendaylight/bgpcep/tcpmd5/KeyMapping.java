/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public final class KeyMapping extends HashMap<InetAddress, byte[]> {
	private static final long serialVersionUID = 1L;

	public KeyMapping() {
		super();
	}

	public KeyMapping(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public KeyMapping(final int initialCapacity) {
		super(initialCapacity);
	}

	public KeyMapping(final Map<? extends InetAddress, ? extends byte[]> m) {
		super(m);
	}
}
