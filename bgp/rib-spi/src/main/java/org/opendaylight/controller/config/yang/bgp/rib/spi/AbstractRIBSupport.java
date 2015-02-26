/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.spi;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public abstract class AbstractRIBSupport implements RIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRIBSupport.class);
    private static final NodeIdentifier ADVERTIZED_ROUTES = new NodeIdentifier(AdvertizedRoutes.QNAME);
    private static final NodeIdentifier WITHDRAWN_ROUTES = new NodeIdentifier(WithdrawnRoutes.QNAME);
    private static final NodeIdentifier DESTINATION_TYPE = new NodeIdentifier(DestinationType.QNAME);

    protected AbstractRIBSupport() {

    }

    protected abstract NodeIdentifier destinationIdentifier();
    protected abstract void deleteDestinationRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tableId, ContainerNode destination);
    protected abstract void putDestinationRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tableId, ContainerNode destination, ContainerNode attributes);

    private static ContainerNode getDestination(final DataContainerChild<? extends PathArgument, ?> routes, final NodeIdentifier destinationId) {
        if (routes instanceof ContainerNode) {
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybeDestination = ((ContainerNode)routes).getChild(DESTINATION_TYPE);
            if (maybeDestination.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> destination = maybeDestination.get();
                if (destination instanceof ChoiceNode) {
                    final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRet = ((ChoiceNode)destination).getChild(destinationId);
                    if (maybeRet.isPresent()) {
                        final DataContainerChild<? extends PathArgument, ?> ret = maybeRet.get();
                        if (ret instanceof ContainerNode) {
                            return (ContainerNode)ret;
                        } else {
                            LOG.debug("Specified node {} is not a container, ignoring it", ret);
                        }
                    } else {
                        LOG.debug("Specified container {} is not present in destination {}", destinationId, destination);
                    }
                } else {
                    LOG.warn("Destination {} is not a choice, ignoring it", destination);
                }
            } else {
                LOG.debug("Destination is not present in routes {}", routes);
            }
        } else {
            LOG.warn("Advertized routes {} are not a container, ignoring it", routes);
        }

        return null;
    }

    @Override
    public final void deleteRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tableId, final ContainerNode nlri) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = nlri.getChild(WITHDRAWN_ROUTES);
        if (maybeRoutes.isPresent()) {
            final ContainerNode destination = getDestination(maybeRoutes.get(), destinationIdentifier());
            if (destination != null) {
                deleteDestinationRoutes(tx, tableId, destination);
            }
        } else {
            LOG.debug("Withdrawn routes are not present in NLRI {}", nlri);
        }
    }

    @Override
    public final void putRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tableId, final ContainerNode nlri, final ContainerNode attributes) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = nlri.getChild(ADVERTIZED_ROUTES);
        if (maybeRoutes.isPresent()) {
            final ContainerNode destination = getDestination(maybeRoutes.get(), destinationIdentifier());
            if (destination != null) {
                putDestinationRoutes(tx, tableId, destination, attributes);
            }
        } else {
            LOG.debug("Advertized routes are not present in NLRI {}", nlri);
        }
    }
}
