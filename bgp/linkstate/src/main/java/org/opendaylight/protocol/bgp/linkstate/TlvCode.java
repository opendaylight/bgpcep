/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

public final class TlvCode {

	private TlvCode() {
	}

	public static final short LOCAL_NODE_DESCRIPTORS = 256;

	public static final short REMOTE_NODE_DESCRIPTORS = 257;

	/* Link Descriptor TLVs */

	public static final short LINK_LR_IDENTIFIERS = 258;

	public static final short IPV4_IFACE_ADDRESS = 259;

	public static final short IPV4_NEIGHBOR_ADDRESS = 260;

	public static final short IPV6_IFACE_ADDRESS = 261;

	public static final short IPV6_NEIGHBOR_ADDRESS = 262;

	/* Prefix Descriptor TLVs */

	public static final short MULTI_TOPOLOGY_ID = 263;

	public static final short OSPF_ROUTE_TYPE = 264;

	public static final short IP_REACHABILITY = 265;

	/* Node Descriptor TLVs */

	public static final short AS_NUMBER = 512;

	public static final short BGP_LS_ID = 513;

	public static final short AREA_ID = 514;

	public static final short IGP_ROUTER_ID = 515;
}
