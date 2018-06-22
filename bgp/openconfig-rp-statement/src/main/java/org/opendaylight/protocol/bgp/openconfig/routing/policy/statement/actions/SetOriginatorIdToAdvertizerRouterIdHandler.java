/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionAugPolicy;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180329.SetOriginatorIdToAdvertizerRouterId;

/**
 * Originator attribute shall be set to the router-id of the advertiser.
 * https://tools.ietf.org/html/rfc4684
 *
 * @author Claudio D. Gasparini
 */
public final class SetOriginatorIdToAdvertizerRouterIdHandler
        implements BgpActionAugPolicy<SetOriginatorIdToAdvertizerRouterId> {
    private static SetOriginatorIdToAdvertizerRouterIdHandler INSTANCE
            = new SetOriginatorIdToAdvertizerRouterIdHandler();

    private SetOriginatorIdToAdvertizerRouterIdHandler() {

    }

    public static SetOriginatorIdToAdvertizerRouterIdHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Attributes applyImportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeBaseParameters,
            final Attributes attributes,
            final SetOriginatorIdToAdvertizerRouterId actions) {
        return setOriginatorId(attributes, routeBaseParameters.getFromPeerId());
    }

    private Attributes setOriginatorId(final Attributes attributes, final PeerId peerId) {
        if (attributes.getOriginatorId() != null) {
            return attributes;
        }
        return new AttributesBuilder(attributes)
                .setOriginatorId(new OriginatorIdBuilder()
                        .setOriginator(RouterIds.inetFromPeerId(peerId)).build()).build();
    }

    @Override
    public Attributes applyExportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeBaseParameters,
            final Attributes attributes,
            final SetOriginatorIdToAdvertizerRouterId actions) {
        return setOriginatorId(attributes, routeBaseParameters.getFromPeerId());
    }
}
