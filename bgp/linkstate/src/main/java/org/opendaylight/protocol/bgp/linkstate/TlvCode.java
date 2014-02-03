/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

final class TlvCode {

	private TlvCode() {
	}

	static final short LOCAL_NODE_DESCRIPTORS = 256;

	static final short REMOTE_NODE_DESCRIPTORS = 257;

	/* Link Descriptor TLVs */

	static final short LINK_LR_IDENTIFIERS = 258;

	static final short IPV4_IFACE_ADDRESS = 259;

	static final short IPV4_NEIGHBOR_ADDRESS = 260;

	static final short IPV6_IFACE_ADDRESS = 261;

	static final short IPV6_NEIGHBOR_ADDRESS = 262;

	/* Link Attribute TLVs */

	static final short LOCAL_IPV4_ROUTER_ID = 1028;

	static final short LOCAL_IPV6_ROUTER_ID = 1029;

	static final short REMOTE_IPV4_ROUTER_ID = 1030;

	static final short REMOTE_IPV6_ROUTER_ID = 1031;

	static final short ADMIN_GROUP = 1088;

	static final short MAX_BANDWIDTH = 1089;

	static final short MAX_RESERVABLE_BANDWIDTH = 1090;

	static final short UNRESERVED_BANDWIDTH = 1091;

	static final short TE_METRIC = 1092;

	static final short LINK_PROTECTION_TYPE = 1093;

	static final short MPLS_PROTOCOL = 1094;

	static final short METRIC = 1095;

	static final short SHARED_RISK_LINK_GROUP = 1096;

	static final short LINK_OPAQUE = 1097;

	static final short LINK_NAME = 1098;

	/* Prefix Descriptor TLVs */

	static final short MULTI_TOPOLOGY_ID = 263;

	static final short OSPF_ROUTE_TYPE = 264;

	static final short IP_REACHABILITY = 265;

	/* Prefix Attribute TLVs */

	static final short IGP_FLAGS = 1152;

	static final short ROUTE_TAG = 1153;

	static final short EXTENDED_ROUTE_TAG = 1154;

	static final short PREFIX_METRIC = 1155;

	static final short FORWARDING_ADDRESS = 1156;

	static final short PREFIX_OPAQUE = 1157;

	/* Node Descriptor TLVs */

	static final short AS_NUMBER = 512;

	static final short BGP_LS_ID = 513;

	static final short AREA_ID = 514;

	static final short IGP_ROUTER_ID = 515;

	/* Node Attribute TLVs */

	static final short NODE_FLAG_BITS = 1024;

	static final short NODE_OPAQUE = 1025;

	static final short DYNAMIC_HOSTNAME = 1026;

	static final short ISIS_AREA_IDENTIFIER = 1027;
}
