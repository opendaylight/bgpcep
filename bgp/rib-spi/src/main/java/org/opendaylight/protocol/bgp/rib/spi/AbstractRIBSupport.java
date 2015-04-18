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
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public abstract class AbstractRIBSupport implements RIBSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRIBSupport.class);
    private static final NodeIdentifier ADVERTIZED_ROUTES = new NodeIdentifier(AdvertizedRoutes.QNAME);
    private static final NodeIdentifier WITHDRAWN_ROUTES = new NodeIdentifier(WithdrawnRoutes.QNAME);
    private static final NodeIdentifier DESTINATION_TYPE = new NodeIdentifier(DestinationType.QNAME);
    protected static final NodeIdentifier ROUTES = new NodeIdentifier(Routes.QNAME);

    private final NodeIdentifier routesContainerIdentifier;
    private final NodeIdentifier routesListIdentifier;
    private final NodeIdentifier routeAttributesIdentifier;
    private final Class<? extends Routes> cazeClass;
    private final Class<? extends DataObject> containerClass;
    private final Class<? extends Route> listClass;


    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param cazeClass Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param containerClass Binding class of the container in routes choice, must not be null.
     * @param listClass Binding class of the route list, nust not be null;
     */
    protected AbstractRIBSupport(final Class<? extends Routes> cazeClass, final Class<? extends DataObject> containerClass, final Class<? extends Route> listClass) {
        final QName qname = BindingReflections.findQName(containerClass);
        this.routesContainerIdentifier = new NodeIdentifier(qname);
        this.routeAttributesIdentifier = new NodeIdentifier(QName.cachedReference(QName.create(qname, Attributes.QNAME.getLocalName())));
        this.cazeClass = Preconditions.checkNotNull(cazeClass);
        this.containerClass = Preconditions.checkNotNull(containerClass);
        this.listClass = Preconditions.checkNotNull(listClass);
        this.routesListIdentifier = new NodeIdentifier(BindingReflections.findQName(listClass));
    }

    @Override
    public final Class<? extends Routes> routesCaseClass() {
        return this.cazeClass;
    }

    @Override
    public final Class<? extends DataObject> routesContainerClass() {
        return this.containerClass;
    }

    @Override
    public final Class<? extends Route> routesListClass() {
        return this.listClass;
    }

    /**
     * Return the {@link NodeIdentifier} of the AFI/SAFI-specific container under
     * the RIB routes.
     *
     * @return Container identifier, may not be null.
     */
    protected final NodeIdentifier routesContainerIdentifier() {
        return this.routesContainerIdentifier;
    }

    /**
     * Return the {@link NodeIdentifier} of the AFI/SAFI-specific container under
     * the NLRI destination.
     *
     * @return Container identifier, may not be null.
     */
    @Nonnull protected abstract NodeIdentifier destinationContainerIdentifier();

    /**
     * Given the destination as ContainerNode, implementation needs to parse the DOM model
     * from this point onward:
     *
     * {@code /bgp-mp:mp-unreach-nlri/bgp-mp:withdrawn-routes/bgp-mp:destination-type }
     *
     * and delete the routes from its RIBs.
     *
     * @param tx DOMDataWriteTransaction to be passed into implementation
     * @param tablePath YangInstanceIdentifier to be passed into implementation
     * @param destination ContainerNode DOM representation of NLRI in Update message
     */
    protected abstract void deleteDestinationRoutes(DOMDataWriteTransaction tx, YangInstanceIdentifier tablePath, ContainerNode destination);

    /**
     * Given the destination as ContainerNode, implementation needs to parse the DOM model
     * from this point onward:
     *
     * {@code /bgp-mp:mp-reach-nlri/bgp-mp:advertized-routes/bgp-mp:destination-type }
     *
     * and put the routes to its RIBs.
     *
     * @param tx DOMDataWriteTransaction to be passed into implementation
     * @param tablePath YangInstanceIdentifier to be passed into implementation
     * @param destination ContainerNode DOM representation of NLRI in Update message
     * @param attributes ContainerNode to be passed into implementation
     */
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
        return this.routeAttributesIdentifier;
    }

    @Override
    public final Collection<DataTreeCandidateNode> changedRoutes(final DataTreeCandidateNode routes) {
        LOG.trace("Changed routes called with {} identifier {}", routes, routes.getIdentifier());
        final DataTreeCandidateNode myRoutes = routes.getModifiedChild(this.routesContainerIdentifier);
        if (myRoutes == null) {
            return Collections.emptySet();
        }
        LOG.trace("MyRoutes {} identifier {}", myRoutes, myRoutes.getIdentifier());
        final DataTreeCandidateNode routesMap = myRoutes.getModifiedChild(this.routesListIdentifier);
        if (routesMap == null) {
            return Collections.emptySet();
        }
        LOG.trace("RoutesMap {} identifier {}", routesMap, routesMap.getIdentifier());
        // Well, given the remote possibility of augmentation, we should perform a filter here,
        // to make sure the type matches what routeType() reports.
        LOG.trace("Returning children {}", routesMap.getChildNodes());
        return routesMap.getChildNodes();
    }

    @Override
    public final YangInstanceIdentifier routePath(final YangInstanceIdentifier routesPath, final PathArgument routeId) {
        return routesPath.node(this.routesContainerIdentifier).node(this.routesListIdentifier).node(routeId);
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

    /**
     * Build MpReachNlri object from DOM representation.
     *
     * @param routes Collection of MapEntryNode DOM representation of routes
     * @param hop CNextHop as it was parsed from Attributes, to be included in MpReach object
     * @return MpReachNlri
     */
    @Nonnull protected abstract MpReachNlri buildReach(Collection<MapEntryNode> routes, CNextHop hop);

    /**
     * Build MpUnReachNlri object from DOM representation.
     *
     * @param routes Collection of MapEntryNode DOM representation of routes
     * @return MpUnreachNlri
     */
    @Nonnull protected abstract MpUnreachNlri buildUnreach(Collection<MapEntryNode> routes);

    @Override
    public Update buildUpdate(final Collection<MapEntryNode> advertised, final Collection<MapEntryNode> withdrawn, final Attributes attr) {
        final UpdateBuilder ub = new UpdateBuilder();
        final AttributesBuilder ab = new AttributesBuilder(attr);
        final CNextHop hop = ab.getCNextHop();

        // do not preserve next hop in attributes if we are using MpReach
        ab.setCNextHop(null);

        if (!advertised.isEmpty()) {
            MpReachNlri mb = buildReach(advertised, hop);
            ab.addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(mb).build());
        }
        if (!withdrawn.isEmpty()) {
            MpUnreachNlri mb = buildUnreach(withdrawn);
            ab.addAugmentation(Attributes2.class, new Attributes2Builder().setMpUnreachNlri(mb).build());
        }

        ub.setAttributes(ab.build());
        return ub.build();
    }
}
