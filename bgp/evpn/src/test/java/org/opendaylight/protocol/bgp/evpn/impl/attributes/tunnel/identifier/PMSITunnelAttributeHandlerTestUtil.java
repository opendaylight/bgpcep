/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier;

import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.IPV6;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.MPLS_LABEL;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.OpaqueUtilTest.OPAQUE_TEST;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.OpaqueUtilTest.OPAQUE_TEST2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.routes.evpn.routes.evpn.route.PmsiTunnelAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.routes.evpn.routes.evpn.route.PmsiTunnelAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.PAddressPMulticastGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.PmsiTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.BidirPimTreeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.IngressReplicationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.MldpMp2mpLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.MldpP2mpLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.PimSmTreeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.PimSsmTreeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.RsvpTeP2mpLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.mldp.p2mp.lsp.mldp.p2mp.lsp.OpaqueValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.mldp.p2mp.lsp.mldp.p2mp.lsp.OpaqueValueBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

final class PMSITunnelAttributeHandlerTestUtil {
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 0- MPLS LABEL
     * No tunnel information present
     */
    static final byte[] NO_TUNNEL_INFORMATION_PRESENT_EXPECTED = {
        (byte) 0x80, (byte) 0x16, (byte) 0x05,
        (byte) 0x01, (byte) 0x00, (byte) 0x05, (byte) 0xdc, (byte) 0x10
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 1- MPLS LABEL
     * mLDP P2MP LSP
     */
    static final byte[] RSVP_TE_P2MP_LSP_LSP_EXPECTED = {
        (byte) 0x80, (byte) 0x16, (byte) 0x11,
        (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x00, (byte) 0x00, (byte) 0x0d, (byte) 0x82,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0f,
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 2- MPLS LABEL
     * mLDP P2MP LSP
     */
    static final byte[] M_LDP_P2MP_LSP_EXPECTED_IPV4 = {
        (byte) 0x80, (byte) 0x16, (byte) 0x35,
        (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x04,
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
        (byte) 0x00, (byte) 0x26, //Opaque Values Length
        (byte) 0x01, (byte) 0x00, (byte) 0x0e, // Opaque Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00,
        (byte) 0xff, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x10, // Opaque Type -Ext Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x02
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 2- MPLS LABEL
     * mLDP P2MP LSP
     */
    static final byte[] M_LDP_P2MP_LSP_EXPECTED_WRONG_FAMILY = {
        (byte) 0x80, (byte) 0x16, (byte) 0x20,
        (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x06, (byte) 0x00, (byte) 0xfc, (byte) 0x04,
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
        (byte) 0x00, (byte) 0x11, //Opaque Values Length
        (byte) 0x01, (byte) 0x00, (byte) 0x03, // Opaque Type - Length
        (byte) 0xb5, (byte) 0xeb, (byte) 0x2d,  //Value
        (byte) 0xff, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x06, // Opaque Type -Ext Type - Length
        (byte) 0xb5, (byte) 0xeb, (byte) 0x2d, (byte) 0xd7, (byte) 0x6d, (byte) 0xf8, //Value
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 2- MPLS LABEL
     * mLDP P2MP LSP L2VPN
     */
    static final byte[] M_LDP_P2MP_LSP_EXPECTED_L2VPN = {
        (byte) 0x80, (byte) 0x16, (byte) 0x35,
        (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x06, (byte) 0x00, (byte) 0x19, (byte) 0x04,
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
        (byte) 0x00, (byte) 0x26, //Opaque Values Length
        (byte) 0x01, (byte) 0x00, (byte) 0x0e, // Opaque Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00,
        (byte) 0xff, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x10, // Opaque Type -Ext Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x02
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 2- MPLS LABEL
     * mLDP P2MP LSP IPV4
     */
    static final byte[] M_LDP_P2MP_LSP_EXPECTED_IPV4_2 = {
        (byte) 0x80, (byte) 0x16, (byte) 0x3b,
        (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x04,
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
        (byte) 0x00, (byte) 0x2c, //Opaque Values Length
        (byte) 0x01, (byte) 0x00, (byte) 0x0e, // Opaque Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00,
        (byte) 0xfc, (byte) 0x00, (byte) 0x03, // Wrong Opaque Type - Length
        (byte) 0xb5, (byte) 0xeb, (byte) 0x2d,  //Value
        (byte) 0xff, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x10, // Opaque Type -Ext Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x02
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 2- MPLS LABEL
     * mLDP P2MP LSP IPV6
     */
    static final byte[] M_LDP_P2MP_LSP_EXPECTED_IPV6 = {
        (byte) 0x80, (byte) 0x16, (byte) 0x41,
        (byte) 0x01, (byte) 0x02, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x06, (byte) 0x00, (byte) 0x02, (byte) 0x10,
        (byte) 0x20, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x26, //Opaque Values Length
        (byte) 0x01, (byte) 0x00, (byte) 0x0e, // Opaque Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00,
        (byte) 0xff, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x10, // Opaque Type -Ext Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x02
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 3- MPLS LABEL
     * PIM-SSM Tree
     */
    static final byte[] PIM_SSM_TREE_EXPECTED = {
        (byte) 0x80, (byte) 0x16, (byte) 0x0d,
        (byte) 0x01, (byte) 0x03, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
        (byte) 0x17, (byte) 0x01, (byte) 0x01, (byte) 0x01
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 4- MPLS LABEL
     * PIM-SM Tree
     */
    static final byte[] PIM_SM_TREE_EXPECTED = {
        (byte) 0x80, (byte) 0x16, (byte) 0x0d,
        (byte) 0x01, (byte) 0x04, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
        (byte) 0x17, (byte) 0x01, (byte) 0x01, (byte) 0x01
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 5- MPLS LABEL
     * BIDIR-PIM Tree
     */
    static final byte[] BIDIR_PIM_EXPECTED = {
        (byte) 0x80, (byte) 0x16, (byte) 0x0d,
        (byte) 0x01, (byte) 0x05, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
        (byte) 0x17, (byte) 0x01, (byte) 0x01, (byte) 0x01
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 6- MPLS LABEL
     * Ingress Replication
     */
    static final byte[] INGRESS_REPLICATION_EXPECTED = {
        (byte) 0x80, (byte) 0x16, (byte) 0x09,
        (byte) 0x01, (byte) 0x06, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 7- MPLS LABEL
     * mLDP MP2MP LSP
     */
    static final byte[] M_LDP_MP_2_MP_LSP_EXPECTED = {
        (byte) 0x80, (byte) 0x16, (byte) 0x16,
        (byte) 0x01, (byte) 0x07, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0x01, (byte) 0x00, (byte) 0x0e, // Opaque Type - Length
        (byte) 0x07, (byte) 0x00, (byte) 0x0B, //Value
        (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00
    };
    /**
     * ATT - TYPE - ATT LENGTH
     * PMSI FLAG - PMSI TYPE 7- MPLS LABEL
     * mLDP MP2MP LSP
     */
    static final byte[] M_LDP_MP_2_MP_LSP_WRONG = {
        (byte) 0x80, (byte) 0x16, (byte) 0x0b,
        (byte) 0x01, (byte) 0x07, (byte) 0x05, (byte) 0xdc, (byte) 0x10,
        (byte) 0xfc, (byte) 0x00, (byte) 0x03, // Opaque Type - Length
        (byte) 0xb5, (byte) 0xeb, (byte) 0x2d,  //Value
    };

    static final IpAddress P_MULTICAST = new IpAddress(new Ipv4Address("23.1.1.1"));
    static final IpAddress P_ADDRESS = new IpAddress(new Ipv4Address("1.1.1.1"));
    private static final Short NO_SUPPORTED_OPAQUE = 200;

    static class MockTunnelIdentifier implements TunnelIdentifier {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return MockTunnelIdentifier.class;
        }
    }

    static abstract class NonSupportedAddressFamily extends AddressFamily {
    }

    private static PAddressPMulticastGroup buildPAddressPMulticastGroup() {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier
            .bidir.pim.tree.BidirPimTreeBuilder().setPAddress(P_ADDRESS).setPMulticastGroup(P_MULTICAST).build();
    }

    private static PmsiTunnelBuilder getPmsiTunnelBuilder() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = new PmsiTunnelBuilder();
        pmsiTunnelBuilder.setLeafInformationRequired(true);
        pmsiTunnelBuilder.setMplsLabel(MPLS_LABEL);
        return pmsiTunnelBuilder;
    }

    private static Attributes buildAttribute(final PmsiTunnelBuilder pmsiTunnelBuilder) {
        return new AttributesBuilder().setUnrecognizedAttributes(Collections.emptyList()).addAugmentation(PmsiTunnelAugmentation.class,
            new PmsiTunnelAugmentationBuilder().setPmsiTunnel(pmsiTunnelBuilder.build()).build()).build();
    }

    static Attributes buildBidirPimTreeAttribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        pmsiTunnelBuilder.setTunnelIdentifier(new BidirPimTreeBuilder().setBidirPimTree(new org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.bidir.pim.tree.BidirPimTreeBuilder(buildPAddressPMulticastGroup()).build()).build());
        return buildAttribute(pmsiTunnelBuilder);
    }

    static Attributes buildPimSMTreeAttribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        pmsiTunnelBuilder.setTunnelIdentifier(new PimSmTreeBuilder().setPimSmTree(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
            .yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.pim.sm.tree.PimSmTreeBuilder(buildPAddressPMulticastGroup()).build()).build());
        return buildAttribute(pmsiTunnelBuilder);
    }

    static Attributes buildPimSSMTreeAttribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        pmsiTunnelBuilder.setTunnelIdentifier(new PimSsmTreeBuilder().setPimSsmTree(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
            .yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.pim.ssm.tree.PimSsmTreeBuilder(buildPAddressPMulticastGroup()).build()).build());
        return buildAttribute(pmsiTunnelBuilder);
    }

    static Attributes buildNoSupportedOpaqueAttribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        final List<OpaqueValue> nonSupported = Collections.singletonList(new OpaqueValueBuilder().setOpaque(OPAQUE_TEST).setOpaqueType(NO_SUPPORTED_OPAQUE).build());
        pmsiTunnelBuilder.setTunnelIdentifier(buildMldpP2mpLsp(P_ADDRESS, Ipv4AddressFamily.class, nonSupported));
        return buildAttribute(pmsiTunnelBuilder);
    }

    static Attributes buildNoSupportedFamilyAttribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        pmsiTunnelBuilder.setTunnelIdentifier(buildMldpP2mpLsp(P_ADDRESS, NonSupportedAddressFamily.class, createOpaqueList()));
        return buildAttribute(pmsiTunnelBuilder);
    }

    static Attributes buildMLDPP2MPLSPIpv4Attribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        pmsiTunnelBuilder.setTunnelIdentifier(buildMldpP2mpLsp(P_ADDRESS, Ipv4AddressFamily.class, createOpaqueList()));
        return buildAttribute(pmsiTunnelBuilder);
    }

    static Attributes buildMLDPP2MPLSPIpv6Attribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = new PmsiTunnelBuilder();
        pmsiTunnelBuilder.setLeafInformationRequired(true);
        pmsiTunnelBuilder.setMplsLabel(MPLS_LABEL);
        pmsiTunnelBuilder.setTunnelIdentifier(buildMldpP2mpLsp(IPV6, Ipv6AddressFamily.class, createOpaqueList()));
        return buildAttribute(pmsiTunnelBuilder);
    }

    static Attributes buildMLDPP2MPLSPL2vpnAttribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        pmsiTunnelBuilder.setTunnelIdentifier(buildMldpP2mpLsp(P_ADDRESS, L2vpnAddressFamily.class, createOpaqueList()));
        return buildAttribute(pmsiTunnelBuilder);
    }

    private static TunnelIdentifier buildMldpP2mpLsp(final IpAddress pAddress, final Class<? extends AddressFamily> family, final List<OpaqueValue> opaqueList) {
        return new MldpP2mpLspBuilder().setMldpP2mpLsp(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml
            .ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.mldp.p2mp.lsp.MldpP2mpLspBuilder()
            .setRootNodeAddress(pAddress).setAddressFamily(family).setOpaqueValue(opaqueList).build()).build();
    }

    static Attributes buildRSVPTEP2MPLSPLSPAttribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        pmsiTunnelBuilder.setTunnelIdentifier(new RsvpTeP2mpLspBuilder().setRsvpTeP2mpLps(new org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.rsvp.te.p2mp.lsp.RsvpTeP2mpLpsBuilder()
            .setP2mpId(3458L).setTunnelId(15).setExtendedTunnelId(P_ADDRESS).build()).build());
        return buildAttribute(pmsiTunnelBuilder);
    }

    static Attributes buildINGRESSREPLICATIONAttribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        pmsiTunnelBuilder.setTunnelIdentifier(new IngressReplicationBuilder().setIngressReplication(new org.opendaylight.yang.gen.v1.urn
            .opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.ingress.replication.IngressReplicationBuilder()
            .setReceivingEndpointAddress(P_ADDRESS).build()).build());
        return buildAttribute(pmsiTunnelBuilder);
    }

    static Attributes buildMLDPMP2MPLSPWrongAttribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        pmsiTunnelBuilder.setTunnelIdentifier(new MldpMp2mpLspBuilder().setMldpMp2mpLsp(new org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.mldp.mp2mp.lsp
            .MldpMp2mpLspBuilder().setOpaque(OPAQUE_TEST).setOpaqueType(NO_SUPPORTED_OPAQUE).build()).build());
        return buildAttribute(pmsiTunnelBuilder);
    }

    static Attributes buildMLDPMP2MPLSPAttribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        pmsiTunnelBuilder.setTunnelIdentifier(new MldpMp2mpLspBuilder().setMldpMp2mpLsp(new org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.mldp.mp2mp.lsp
            .MldpMp2mpLspBuilder().setOpaque(OPAQUE_TEST).setOpaqueType(OpaqueUtil.GENERIC_LSP_IDENTIFIER).build()).build());
        return buildAttribute(pmsiTunnelBuilder);
    }

    private static List<OpaqueValue> createOpaqueList() {
        final List<OpaqueValue> opaqueValues = new ArrayList<>();
        opaqueValues.add(new OpaqueValueBuilder().setOpaque(OPAQUE_TEST).setOpaqueType(OpaqueUtil.GENERIC_LSP_IDENTIFIER).build());
        opaqueValues.add(new OpaqueValueBuilder().setOpaque(OPAQUE_TEST2).setOpaqueType((short) 2).setOpaqueType(OpaqueUtil.EXTENDED_TYPE)
            .setOpaqueExtendedType(4).build());
        return opaqueValues;
    }

    static Attributes buildWOTunnelInfAttribute() {
        final PmsiTunnelBuilder pmsiTunnelBuilder = getPmsiTunnelBuilder();
        return buildAttribute(pmsiTunnelBuilder);
    }
}
