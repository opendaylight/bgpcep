/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.Uint24;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.arbitrary._case.Arbitrary;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.arbitrary._case.ArbitraryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class EsiModelUtil {
    static final NodeIdentifier LD_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "local-discriminator").intern());
    static final NodeIdentifier ARB_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "arbitrary").intern());
    static final NodeIdentifier AS_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "as").intern());
    static final NodeIdentifier LACP_MAC_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "ce-lacp-mac-address").intern());
    static final NodeIdentifier PK_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "ce-lacp-port-key").intern());
    static final NodeIdentifier BRIDGE_MAC_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "root-bridge-mac-address").intern());
    static final NodeIdentifier RBP_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "root-bridge-priority").intern());
    static final NodeIdentifier SYSTEM_MAC_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "system-mac-address").intern());
    static final NodeIdentifier RD_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "router-id").intern());

    private EsiModelUtil() {
        throw new UnsupportedOperationException();
    }

    static final Long extractLD(final ContainerNode cont) {
        return (Long) cont.getChild(LD_NID).get().getValue();
    }


    static Arbitrary extractArbitrary(final ContainerNode esi) {
        return new ArbitraryBuilder().setArbitrary((byte[]) esi.getChild(ARB_NID).get().getValue()).build();
    }


    static AsNumber extractAS(final ContainerNode asGen) {
        return new AsNumber((Long) asGen.getChild(AS_NID).get().getValue());
    }


    static Integer extractPK(final ContainerNode t1) {
        return (Integer) t1.getChild(PK_NID).get().getValue();
    }

    static MacAddress extractLacpMac(final ContainerNode t1) {
        return (MacAddress) t1.getChild(LACP_MAC_NID).get().getValue();
    }

    static MacAddress extractBrigeMac(final ContainerNode lan) {
        return (MacAddress) lan.getChild(BRIDGE_MAC_NID).get().getValue();
    }

    static Integer extractBP(final ContainerNode lan) {
        return (Integer) lan.getChild(RBP_NID).get().getValue();
    }

    static Uint24 extractUint24LD(final ContainerNode esiVal) {
        return (Uint24) esiVal.getChild(LD_NID).get().getValue();
    }

    static MacAddress extractSystmeMac(final ContainerNode macGEn) {
        return (MacAddress) macGEn.getChild(SYSTEM_MAC_NID).get().getValue();
    }

    static Ipv4Address extractRD(final ContainerNode t4) {
        return (Ipv4Address) t4.getChild(RD_NID).get().getValue();
    }

}
