/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionAugPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180329.NonTransitiveAttributesFilter;

/**
 * Removes non transitive attributes.
 */
public final class NonTransitiveAttributesFilterHandler implements BgpActionAugPolicy<NonTransitiveAttributesFilter> {
    @Override
    public Attributes applyImportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final NonTransitiveAttributesFilter bgpActions) {
        return filterAttributes(attributes);
    }

    private Attributes filterAttributes(final Attributes attributes) {
        final AttributesBuilder builder = new AttributesBuilder();
        builder.setCNextHop(attributes.getCNextHop());
        builder.setOrigin(attributes.getOrigin());
        builder.setAsPath(attributes.getAsPath());
        builder.setCommunities(attributes.getCommunities());

        final List<UnrecognizedAttributes> oldAtt = attributes.getUnrecognizedAttributes();
        if (oldAtt != null) {
            builder.setUnrecognizedAttributes(attributes.getUnrecognizedAttributes().stream()
                    .filter(UnrecognizedAttributes::isTransitive)
                    .collect(Collectors.toList()));
        }
        final List<ExtendedCommunities> oldExt = attributes.getExtendedCommunities();
        if (oldExt != null) {
            builder.setExtendedCommunities(oldExt.stream()
                    .filter(ExtendedCommunity::isTransitive)
                    .collect(Collectors.toList()));
        }
        return builder.build();
    }

    @Override
    public Attributes applyExportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final NonTransitiveAttributesFilter bgpActions) {
        return filterAttributes(attributes);
    }
}