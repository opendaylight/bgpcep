/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionAugPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.LocalAsPathPrepend;

public final class LocalAsPathPrependHandler extends AbstractPrependAsPath
        implements BgpActionAugPolicy<LocalAsPathPrepend> {
    private static final LocalAsPathPrependHandler INSTANCE = new LocalAsPathPrependHandler();

    private LocalAsPathPrependHandler() {
        // hidden on purpose
    }

    public static LocalAsPathPrependHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Attributes applyImportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters parameters, final Attributes attributes,
            final LocalAsPathPrepend actions) {
        return prependIfPeerLocalAs(attributes, parameters.getFromPeerLocalAs());
    }

    @Override
    public Attributes applyExportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters parameters, final Attributes attributes,
            final LocalAsPathPrepend actions) {
        return prependIfPeerLocalAs(attributes, parameters.getToPeerLocalAs());
    }

    private static Attributes prependIfPeerLocalAs( final Attributes attributes, final @Nullable AsNumber peerLocalAs) {
        return peerLocalAs == null ? attributes : prependAS(attributes, peerLocalAs);
    }
}
