/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

/**
 * Interface for registering Network Layer Reachability Information handlers.
 * In order for a model-driven RIB implementation to work correctly, it has
 * to know how to handle individual NLRI fields, whose encoding is specific
 * to a AFI/SAFI pair. This interface exposes an interface for registration
 * retrieval of these handlers.
 */
public interface NLRIHandlerRegistry {

	/**
	 * Register a handler for a particular AFI/SAFI combination.
	 * 
	 * @param afi Address Family identifier
	 * @param safi Subsequent Address Family identifier
	 * @param handler NLRI handler
	 * @return Registration handle. Call its close() method to remove it.
	 */
	public AutoCloseable registerHandler(Class<? extends AddressFamily> afi,
			Class<? extends SubsequentAddressFamily> safi, NLRIHandler<?> handler);

	public NLRIHandler<?> getHandler(Class<? extends AddressFamily> afi,
			Class<? extends SubsequentAddressFamily> safi);

}