/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.util;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev150515.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev150515.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.Ipv4LabelledUnicast;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.Ipv4Unicast;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.Ipv6LabelledUnicast;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.Ipv6Unicast;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev150930.Ipv4Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev150930.Ipv6Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev150930.Linkstate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class ToOpenConfigUtil {

    public static final InstanceIdentifier<Bgp> BGP_IID = InstanceIdentifier.builder(Bgp.class).build();

    //only single instance of Global
    public static final String BGP_GLOBAL_ID = "GLOBAL";

    private ToOpenConfigUtil() {
        throw new UnsupportedOperationException();
    }

    public static Global toGlobalConfiguration(final AsNumber localAs, final Ipv4Address bgpRibId, final Ipv4Address clusterId,
            final List<BgpTableType> bgpTables) {
        return new GlobalBuilder()
        .setAfiSafis(new AfiSafisBuilder().setAfiSafi(toAfiSafis(bgpTables)).build())
        .setConfig(
            new ConfigBuilder()
                .setAs(localAs)
                .setRouterId(bgpRibId).build()).build();
    }

    public static AfiSafi toAfiSafi(final BgpTableType tableType) {
        if (tableType.getAfi() == Ipv4AddressFamily.class
                && tableType.getSafi() == UnicastSubsequentAddressFamily.class) {
            return new AfiSafiBuilder().setAfiSafiName(Ipv4Unicast.class).build();
        } else if (tableType.getAfi() == Ipv6AddressFamily.class
                && tableType.getSafi() == UnicastSubsequentAddressFamily.class) {
            return new AfiSafiBuilder().setAfiSafiName(Ipv6Unicast.class).build();
        } else if (tableType.getAfi() == Ipv4AddressFamily.class
                && tableType.getSafi() == LabeledUnicastSubsequentAddressFamily.class) {
            return new AfiSafiBuilder().setAfiSafiName(Ipv4LabelledUnicast.class).build();
        } else if (tableType.getAfi() == Ipv6AddressFamily.class
                && tableType.getSafi() == LabeledUnicastSubsequentAddressFamily.class) {
            return new AfiSafiBuilder().setAfiSafiName(Ipv6LabelledUnicast.class).build();
        } else if (tableType.getAfi() == LinkstateAddressFamily.class
                && tableType.getSafi() == LinkstateSubsequentAddressFamily.class) {
            return new AfiSafiBuilder().setAfiSafiName(Linkstate.class).build();
        } else if (tableType.getAfi() == Ipv4AddressFamily.class
                && tableType.getSafi() == FlowspecSubsequentAddressFamily.class) {
            return new AfiSafiBuilder().setAfiSafiName(Ipv4Flow.class).build();
        } else if (tableType.getAfi() == Ipv6AddressFamily.class
                && tableType.getSafi() == FlowspecSubsequentAddressFamily.class) {
            return new AfiSafiBuilder().setAfiSafiName(Ipv6Flow.class).build();
        } else {
            //TODO log unsupported AFI/SAFI in
        }
        return null;
    }

    private static List<AfiSafi> toAfiSafis(final List<BgpTableType> advertiedTables) {
        final List<AfiSafi> afiSafis = new ArrayList<>();
        for (final BgpTableType tableType : advertiedTables) {
            afiSafis.add(toAfiSafi(tableType));
        }
        return afiSafis;
    }

}
