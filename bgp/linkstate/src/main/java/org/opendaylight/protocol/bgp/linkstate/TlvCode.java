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

    static final int LOCAL_NODE_DESCRIPTORS = 256;

    static final int REMOTE_NODE_DESCRIPTORS = 257;

    /* Link Descriptor TLVs */

    static final int LINK_LR_IDENTIFIERS = 258;

    static final int IPV4_IFACE_ADDRESS = 259;

    static final int IPV4_NEIGHBOR_ADDRESS = 260;

    static final int IPV6_IFACE_ADDRESS = 261;

    static final int IPV6_NEIGHBOR_ADDRESS = 262;

    /* Link Attribute TLVs */

    static final int LOCAL_IPV4_ROUTER_ID = 1028;

    static final int LOCAL_IPV6_ROUTER_ID = 1029;

    static final int REMOTE_IPV4_ROUTER_ID = 1030;

    static final int REMOTE_IPV6_ROUTER_ID = 1031;

    static final int ADMIN_GROUP = 1088;

    static final int MAX_BANDWIDTH = 1089;

    static final int MAX_RESERVABLE_BANDWIDTH = 1090;

    static final int UNRESERVED_BANDWIDTH = 1091;

    static final int TE_METRIC = 1092;

    static final int LINK_PROTECTION_TYPE = 1093;

    static final int MPLS_PROTOCOL = 1094;

    static final int METRIC = 1095;

    static final int SHARED_RISK_LINK_GROUP = 1096;

    static final int LINK_OPAQUE = 1097;

    static final int LINK_NAME = 1098;

    /* Prefix Descriptor TLVs */

    static final int MULTI_TOPOLOGY_ID = 263;

    static final int OSPF_ROUTE_TYPE = 264;

    static final int IP_REACHABILITY = 265;

    /* Prefix Attribute TLVs */

    static final int IGP_FLAGS = 1152;

    static final int ROUTE_TAG = 1153;

    static final int EXTENDED_ROUTE_TAG = 1154;

    static final int PREFIX_METRIC = 1155;

    static final int FORWARDING_ADDRESS = 1156;

    static final int PREFIX_OPAQUE = 1157;

    /* Node Descriptor TLVs */

    static final int AS_NUMBER = 512;

    static final int BGP_LS_ID = 513;

    static final int AREA_ID = 514;

    static final int IGP_ROUTER_ID = 515;

    /* Node Attribute TLVs */

    static final int NODE_FLAG_BITS = 1024;

    static final int NODE_OPAQUE = 1025;

    static final int DYNAMIC_HOSTNAME = 1026;

    static final int ISIS_AREA_IDENTIFIER = 1027;
}
