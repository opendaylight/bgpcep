/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.BidirPimTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.IngressReplication;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.MldpMp2mpLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.MldpP2mpLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.PimSmTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.PimSsmTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.RsvpTeP2mpLsp;


enum TunnelType {
    RSVP_TE_P2MP_LSP(1, RsvpTeP2mpLsp.class),
    MLDP_P2MP_LSP(2, MldpP2mpLsp.class),
    PIM_SSM_TREE(3, PimSsmTree.class),
    PIM_SM_TREE(4, PimSmTree.class),
    BIDIR_PIM_TREE(5, BidirPimTree.class),
    INGRESS_REPLICATION(6, IngressReplication.class),
    M_LDP_MP_2_MP_LSP(7, MldpMp2mpLsp.class);

    final Class<? extends TunnelIdentifier> clazz;
    final int value;

    TunnelType(final int value, final Class<? extends TunnelIdentifier> clazz) {
        this.value = value;
        this.clazz = clazz;
    }

    public Class<? extends TunnelIdentifier> getClazz() {
        return this.clazz;
    }

    public int getIntValue() {
        return this.value;
    }
}
