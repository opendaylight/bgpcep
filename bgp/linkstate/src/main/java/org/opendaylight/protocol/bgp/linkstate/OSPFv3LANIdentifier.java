/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;


/**
 * An OSPFv3 LAN "pseudonode" identifier
 * @see https://tools.ietf.org/html/draft-ietf-idr-ls-distribution-03#section-3.2.1.4
 */
public final class OSPFv3LANIdentifier extends AbstractOSPFLANIdentifier<OSPFInterfaceIdentifier> {
	private static final long serialVersionUID = 1L;

	public OSPFv3LANIdentifier(final OSPFRouterIdentifier dr, final OSPFInterfaceIdentifier lanInterface) {
		super(dr, lanInterface);
	}
}
