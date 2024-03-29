/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import static com.google.common.base.Preconditions.checkArgument;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Uint24;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.esi.arbitrary._case.Arbitrary;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.esi.arbitrary._case.ArbitraryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.EvpnChoice;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class EsiModelUtil {
    static final NodeIdentifier LD_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "local-discriminator").intern());
    static final NodeIdentifier ARB_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "arbitrary").intern());
    static final NodeIdentifier AS_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "as").intern());
    static final NodeIdentifier LACP_MAC_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "ce-lacp-mac-address").intern());
    static final NodeIdentifier PK_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "ce-lacp-port-key").intern());
    static final NodeIdentifier BRIDGE_MAC_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "root-bridge-mac-address").intern());
    static final NodeIdentifier RBP_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "root-bridge-priority").intern());
    static final NodeIdentifier SYSTEM_MAC_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "system-mac-address").intern());
    static final NodeIdentifier RD_NID = NodeIdentifier.create(QName.create(EvpnChoice.QNAME,
            "router-id").intern());

    private EsiModelUtil() {
        // Hidden on purpose
    }

    static Uint32 extractLD(final ContainerNode cont) {
        return (Uint32) cont.getChildByArg(LD_NID).body();
    }

    static Arbitrary extractArbitrary(final ContainerNode esi) {
        final byte[] arbitrary = (byte[]) esi.getChildByArg(ARB_NID).body();
        checkArgument(arbitrary.length == ArbitraryParser.ARBITRARY_LENGTH,
                "Wrong length of array of bytes. Expected: %s Passed: %s "
                        + ";", ArbitraryParser.ARBITRARY_LENGTH, arbitrary.length);
        return new ArbitraryBuilder().setArbitrary(arbitrary).build();
    }

    static AsNumber extractAS(final ContainerNode asGen) {
        return new AsNumber((Uint32) asGen.getChildByArg(AS_NID).body());
    }

    static Uint16 extractPK(final ContainerNode t1) {
        return (Uint16) t1.getChildByArg(PK_NID).body();
    }

    static MacAddress extractLacpMac(final ContainerNode t1) {
        return new MacAddress((String) t1.getChildByArg(LACP_MAC_NID).body());
    }

    static MacAddress extractBrigeMac(final ContainerNode lan) {
        return new MacAddress((String) lan.getChildByArg(BRIDGE_MAC_NID).body());
    }

    static Uint16 extractBP(final ContainerNode lan) {
        return (Uint16) lan.getChildByArg(RBP_NID).body();
    }

    static Uint24 extractUint24LD(final ContainerNode esiVal) {
        return new Uint24((Uint32) esiVal.getChildByArg(LD_NID).body());
    }

    static MacAddress extractSystmeMac(final ContainerNode macGEn) {
        return new MacAddress((String) macGEn.getChildByArg(SYSTEM_MAC_NID).body());
    }

    static Ipv4AddressNoZone extractRD(final ContainerNode t4) {
        return new Ipv4AddressNoZone((String) t4.getChildByArg(RD_NID).body());
    }
}
