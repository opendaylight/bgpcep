/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv4Prefix;
import org.opendaylight.protocol.bgp.linkstate.NodeIdentifier;
import org.opendaylight.protocol.bgp.linkstate.PrefixIdentifier;

public final class IPv4PrefixIdentifier extends PrefixIdentifier<IPv4Address> {
	private static final long serialVersionUID = 1L;

	public IPv4PrefixIdentifier(final NodeIdentifier owner, final IPv4Prefix prefix) {
		super(owner, prefix);
	}
}
