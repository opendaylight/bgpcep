/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import java.io.Serializable;

import org.opendaylight.protocol.concepts.NetworkAddress;

/**
 * Interface for BGP Next Hop Attribute.
 * @see <a  href="http://tools.ietf.org/html/rfc2545#section-3">Next Hop</a>
 *
 * @param <T> subtype of Network Address
 */
public interface NextHop<T extends NetworkAddress<?>> extends Serializable {
	/**
	 * Return the global address of the next hop. This operation is
	 * always applicable.
	 *
	 * @return T global address
	 */
	public T getGlobal();

	/**
	 * Return the link-local address of the next hop. This operation is
	 * applicable only to some address types. For address types where
	 * it not applicable, null is returned.
	 *
	 * @return T link-local address, null if unsupported
	 */
	public T getLinkLocal();
}

