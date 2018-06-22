/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionAugPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180329.SetLocalAddressAsNextHop;

/**
 * Local Address shall be set as next Hop.
 * https://tools.ietf.org/html/rfc4684
 *
 * @author Claudio D. Gasparini
 */
public final class SetLocalAddressAsNextHopHandler implements BgpActionAugPolicy<SetLocalAddressAsNextHop> {
    private static final SetLocalAddressAsNextHopHandler INSTANCE = new SetLocalAddressAsNextHopHandler();

    private SetLocalAddressAsNextHopHandler() {

    }

    public static SetLocalAddressAsNextHopHandler getINSTANCE() {
        return INSTANCE;
    }

    @Override
    public Attributes applyImportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeBaseParameters,
            final Attributes attributes,
            final SetLocalAddressAsNextHop actions) {
        return setNextHop(attributes, routeEntryInfo.getOriginatorId());
    }

    private Attributes setNextHop(final Attributes attributes, final Ipv4Address localAddress) {
        return new AttributesBuilder(attributes)
                .setCNextHop(new Ipv4NextHopCaseBuilder()
                        .setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(localAddress).build())
                        .build()).build();
    }

    @Override
    public Attributes applyExportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters exportParameters,
            final Attributes attributes,
            final SetLocalAddressAsNextHop actions) {
        return setNextHop(attributes, routeEntryInfo.getOriginatorId());
    }
}
