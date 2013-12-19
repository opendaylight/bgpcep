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

	/* Link Attribute TLVs */

	public static final short LOCAL_IPV4_ROUTER_ID = 1028;

	public static final short LOCAL_IPV6_ROUTER_ID = 1029;

	public static final short REMOTE_IPV4_ROUTER_ID = 1030;

	public static final short REMOTE_IPV6_ROUTER_ID = 1031;

	public static final short ADMIN_GROUP = 1088;

	public static final short MAX_BANDWIDTH = 1089;

	public static final short MAX_RESERVABLE_BANDWIDTH = 1090;

	public static final short UNRESERVED_BANDWIDTH = 1091;

	public static final short TE_METRIC = 1092;

	public static final short LINK_PROTECTION_TYPE = 1093;

	public static final short MPLS_PROTOCOL = 1094;

	public static final short METRIC = 1095;

	public static final short SHARED_RISK_LINK_GROUP = 1096;

	public static final short LINK_OPAQUE = 1097;

	public static final short LINK_NAME = 1098;

	/* Prefix Descriptor TLVs */

	public static final short MULTI_TOPOLOGY_ID = 263;

	public static final short OSPF_ROUTE_TYPE = 264;

	public static final short IP_REACHABILITY = 265;

	/* Prefix Attribute TLVs */

	public static final short IGP_FLAGS = 1152;

	public static final short ROUTE_TAG = 1153;

	public static final short EXTENDED_ROUTE_TAG = 1154;

	public static final short PREFIX_METRIC = 1155;

	public static final short FORWARDING_ADDRESS = 1156;

	public static final short PREFIX_OPAQUE = 1157;

	/* Node Descriptor TLVs */

	public static final short AS_NUMBER = 512;

	public static final short BGP_LS_ID = 513;

	public static final short AREA_ID = 514;

	public static final short IGP_ROUTER_ID = 515;

	/* Node Attribute TLVs */

	public static final short NODE_FLAG_BITS = 1024;

	public static final short NODE_OPAQUE = 1025;

	public static final short DYNAMIC_HOSTNAME = 1026;

	public static final short ISIS_AREA_IDENTIFIER = 1027;
}
