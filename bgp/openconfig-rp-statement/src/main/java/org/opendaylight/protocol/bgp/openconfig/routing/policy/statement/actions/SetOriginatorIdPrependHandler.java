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
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180329.SetOriginatorIdPrepend;

/**
 * Prepend Originator Id.
 */
public final class SetOriginatorIdPrependHandler implements BgpActionAugPolicy<SetOriginatorIdPrepend> {
    @Override
    public Attributes applyImportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final SetOriginatorIdPrepend bgpActions) {

        final Ipv4Address defOri = bgpActions.getSetOriginatorIdPrepend().getOriginatorId();
        return prependOriginatorId(attributes, defOri == null
                ? routeEntryInfo.getOriginatorId() : defOri);
    }

    private Attributes prependOriginatorId(final Attributes attributes, final Ipv4Address originatorId) {
        if (attributes.getOriginatorId() != null) {
            return attributes;
        }
        final AttributesBuilder newAtt = new AttributesBuilder(attributes);
        return newAtt.setOriginatorId(new OriginatorIdBuilder().setOriginator(originatorId).build()).build();
    }

    @Override
    public Attributes applyExportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final SetOriginatorIdPrepend bgpActions) {
        final Ipv4Address defOri = bgpActions.getSetOriginatorIdPrepend().getOriginatorId();
        return prependOriginatorId(attributes, defOri == null
                ? routeEntryInfo.getOriginatorId() : defOri);
    }
}