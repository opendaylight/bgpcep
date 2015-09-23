/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.util;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev150515.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev150515.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.Ipv4LabelledUnicast;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.Ipv4Unicast;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.Ipv6LabelledUnicast;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.Ipv6Unicast;
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

public final class OpenConfigUtil {

    public static final InstanceIdentifier<Bgp> BGP_IID = InstanceIdentifier.builder(Bgp.class).build();

    public static final String APPLICATION_PEER_GROUP_NAME = "application-peers";

    private static final Map<BgpTableType, AfiSafi> TABLETYPE_TO_AFISAFI = Maps.newHashMap();

    static {
        TABLETYPE_TO_AFISAFI.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class),
                new AfiSafiBuilder().setAfiSafiName(Ipv4Unicast.class).build());
        TABLETYPE_TO_AFISAFI.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class),
                new AfiSafiBuilder().setAfiSafiName(Ipv6Unicast.class).build());
        TABLETYPE_TO_AFISAFI.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class),
                new AfiSafiBuilder().setAfiSafiName(Ipv4LabelledUnicast.class).build());
        TABLETYPE_TO_AFISAFI.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class),
                new AfiSafiBuilder().setAfiSafiName(Ipv6LabelledUnicast.class).build());
        TABLETYPE_TO_AFISAFI.put(new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class),
                new AfiSafiBuilder().setAfiSafiName(Linkstate.class).build());
        TABLETYPE_TO_AFISAFI.put(new BgpTableTypeImpl(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class),
                new AfiSafiBuilder().setAfiSafiName(Ipv4Flow.class).build());
        TABLETYPE_TO_AFISAFI.put(new BgpTableTypeImpl(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class),
                new AfiSafiBuilder().setAfiSafiName(Ipv6Flow.class).build());
    }

    private OpenConfigUtil() {
        throw new UnsupportedOperationException();
    }

    public static Optional<AfiSafi> toAfiSafi(final BgpTableType tableType) {
        return Optional.fromNullable(TABLETYPE_TO_AFISAFI.get(tableType));
    }

    public static List<AfiSafi> toAfiSafis(final List<BgpTableType> advertizedTables) {
        final List<AfiSafi> afiSafis = new ArrayList<>(advertizedTables.size());
        for (final BgpTableType tableType : advertizedTables) {
            final Optional<AfiSafi> afiSafi = toAfiSafi(new BgpTableTypeImpl(tableType.getAfi(), tableType.getSafi()));
            if (afiSafi.isPresent()) {
                afiSafis.add(afiSafi.get());
            }
        }
        return afiSafis;
    }

    public static String getModuleName(final String provider) {
        return provider.substring(provider.lastIndexOf("=") + 2, provider.length() - 2);
    }
}
