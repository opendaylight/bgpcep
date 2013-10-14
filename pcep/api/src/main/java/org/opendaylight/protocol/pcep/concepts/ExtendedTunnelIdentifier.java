/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.concepts;

import org.opendaylight.protocol.concepts.Identifier;
import org.opendaylight.protocol.concepts.NetworkAddress;

/**
 * Interface grouping Extended Tunnel Identifiers.
 *
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-7.2.2">LSP
 *      Identifiers TLVs</a>
 * @param <T> IPv4 or IPv6 address that is wrapped in this tunnel
 */
public interface ExtendedTunnelIdentifier<T extends NetworkAddress<T>> extends Identifier {

	/**
	 * Getter for Identifier of Extended Tunnel.
	 *
	 * @return T IPv4 or IPv6 address
	 */
	public T getIdentifier();
}
