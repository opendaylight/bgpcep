/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Import policy invoked on routes which we get from outside of our home AS from an External Peer.
 */
public final class FromExternalImportPolicy extends AbstractImportAction {
    @Override
    public ContainerNode applyImportAction(final long localAs, final Ipv4Address originatorId,
        final ClusterIdentifier clusterId, final YangInstanceIdentifier.PathArgument routeKey, final PeerId fromPeerId,
        final PeerRole fromPeerRole, final ContainerNode attributes, final Augmentation<Actions> action) {

        /*
         * Filter out non-transitive attributes, so they do not cross inter-AS
         * boundaries.
         *
         * FIXME: to be completely flexible, we need to allow for retaining
         *        the MED attribute. @see https://tools.ietf.org/html/rfc4271#section-5.1.4.
         */
        return AttributeOperations.getInstance(attributes).transitiveAttributes(attributes);
    }
}