/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public abstract class AbstractRIBSupport implements RIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRIBSupport.class);
    private static final NodeIdentifier ADVERTIZED_ROUTES = new NodeIdentifier(AdvertizedRoutes.QNAME);
    private static final NodeIdentifier WITHDRAWN_ROUTES = new NodeIdentifier(WithdrawnRoutes.QNAME);
    private static final NodeIdentifier DESTINATION_TYPE = new NodeIdentifier(DestinationType.QNAME);
    private final NodeIdentifier routesContainerIdentifier;
    private final NodeIdentifier routeAttributesIdentifier;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param routesContainer QName of the container in routes choice, must not be null.
     */
    protected AbstractRIBSupport(final @Nonnull QName routesContainer) {
        this.routesContainerIdentifier = new NodeIdentifier(routesContainer);
        this.routeAttributesIdentifier = new NodeIdentifier(QName.cachedReference(QName.create(routesContainer, Attributes.QNAME.getLocalName())));
    }

    /**
     * Return the {@link NodeIdentifier} of the AFI/SAFI-specific container under
     * the RIB routes.
     *
     * @return Container identifier, may not be null.
     */
    protected final NodeIdentifier routesContainerIdentifier() {
        return routesContainerIdentifier;
    }

    /**
     * Return the {@link NodeIdentifier} of the AFI/SAFI-specific container under
     * the NLRI destination.
     *
     * @return Container identifier, may not be null.
     */
    @Nonnull protected abstract NodeIdentifier destinationContainerIdentifier();

    protected abstract void deleteDestinationRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tablePath, ContainerNode destination);
    protected abstract void putDestinationRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tablePath, ContainerNode destination, ContainerNode attributes);

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
    public final NodeIdentifier routeAttributesIdentifier() {
        return routeAttributesIdentifier;
    }

    @Override
    public final Collection<DataTreeCandidateNode> changedRoutes(final DataTreeCandidateNode routes) {
        final DataTreeCandidateNode myRoutes = routes.getModifiedChild(routesContainerIdentifier);
        if (myRoutes == null) {
            return Collections.emptySet();
        }

        // Well, given the remote possibility of augmentation, we should perform a filter here,
        // to make sure the type matches what routeType() reports.
        return myRoutes.getChildNodes();
    }

    @Override
    public final YangInstanceIdentifier routePath(final YangInstanceIdentifier routesPath, final PathArgument routeId) {
        return routesPath.node(routesContainerIdentifier).node(routeId);
    }

    @Override
    public final void deleteRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath, final ContainerNode nlri) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = nlri.getChild(WITHDRAWN_ROUTES);
        if (maybeRoutes.isPresent()) {
            final ContainerNode destination = getDestination(maybeRoutes.get(), destinationContainerIdentifier());
            if (destination != null) {
                deleteDestinationRoutes(tx, tablePath, destination);
            }
        } else {
            LOG.debug("Withdrawn routes are not present in NLRI {}", nlri);
        }
    }

    @Override
    public final void putRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath, final ContainerNode nlri, final ContainerNode attributes) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = nlri.getChild(ADVERTIZED_ROUTES);
        if (maybeRoutes.isPresent()) {
            final ContainerNode destination = getDestination(maybeRoutes.get(), destinationContainerIdentifier());
            if (destination != null) {
                putDestinationRoutes(tx, tablePath, destination, attributes);
            }
        } else {
            LOG.debug("Advertized routes are not present in NLRI {}", nlri);
        }
    }
}
