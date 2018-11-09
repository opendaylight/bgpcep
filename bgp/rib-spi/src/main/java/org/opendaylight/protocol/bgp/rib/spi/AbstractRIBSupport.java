/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public abstract class AbstractRIBSupport<
        C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>,
        I extends Identifier<R>>
        implements RIBSupport<C, S, R, I> {
    public static final String ROUTE_KEY = "route-key";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRIBSupport.class);
    private static final NodeIdentifier ADVERTISED_ROUTES = new NodeIdentifier(AdvertizedRoutes.QNAME);
    private static final NodeIdentifier WITHDRAWN_ROUTES = new NodeIdentifier(WithdrawnRoutes.QNAME);
    private static final NodeIdentifier DESTINATION_TYPE = new NodeIdentifier(DestinationType.QNAME);
    private static final InstanceIdentifier<Tables> TABLES_II = InstanceIdentifier.create(BgpRib.class)
            .child(Rib.class).child(LocRib.class).child(Tables.class);
    private static final NodeIdentifier ROUTES = new NodeIdentifier(Routes.QNAME);
    private static final ApplyRoute DELETE_ROUTE = new DeleteRoute();
    // Instance identifier to table/(choice routes)/(map of route)
    private final LoadingCache<YangInstanceIdentifier, YangInstanceIdentifier> routesPath = CacheBuilder.newBuilder()
            .weakValues().build(new CacheLoader<YangInstanceIdentifier, YangInstanceIdentifier>() {
                @Override
                public YangInstanceIdentifier load(@Nonnull final YangInstanceIdentifier routesTablePaths) {
                    return routesTablePaths.node(routesContainerIdentifier()).node(routeQName());
                }
            });
    private final NodeIdentifier routesContainerIdentifier;
    private final NodeIdentifier routesListIdentifier;
    private final NodeIdentifier routeAttributesIdentifier;
    private final Class<C> cazeClass;
    private final Class<S> containerClass;
    private final Class<R> listClass;
    private final ApplyRoute putRoute = new PutRoute();
    private final MapEntryNode emptyTable;
    private final QName routeQname;
    private final Class<? extends AddressFamily> afiClass;
    private final Class<? extends SubsequentAddressFamily> safiClass;
    private final NodeIdentifier destinationNid;
    private final QName pathIdQname;
    private final NodeIdentifier pathIdNid;
    private final QName routeKeyQname;
    private final NodeIdentifier prefixTypeNid;
    private final NodeIdentifier rdNid;
    protected final BindingNormalizedNodeSerializer mappingService;
    protected final YangInstanceIdentifier routeDefaultYii;
    private final TablesKey tk;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param mappingService   Serialization service
     * @param cazeClass        Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param containerClass   Binding class of the container in routes choice, must not be null.
     * @param listClass        Binding class of the route list, nust not be null;
     * @param afiClass         address Family Class
     * @param safiClass        SubsequentAddressFamily
     * @param destContainerQname destination Container Qname
     */
    protected AbstractRIBSupport(
            final BindingNormalizedNodeSerializer mappingService,
            final Class<C> cazeClass,
            final Class<S> containerClass,
            final Class<R> listClass,
            final Class<? extends AddressFamily> afiClass,
            final Class<? extends SubsequentAddressFamily> safiClass,
            final QName destContainerQname) {
        final QNameModule module = BindingReflections.getQNameModule(cazeClass);
        this.routesContainerIdentifier
                = new NodeIdentifier(BindingReflections.findQName(containerClass).withModule(module));
        this.routeAttributesIdentifier = new NodeIdentifier(Attributes.QNAME.withModule(module));
        this.cazeClass = requireNonNull(cazeClass);
        this.mappingService = requireNonNull(mappingService);
        this.containerClass = requireNonNull(containerClass);
        this.listClass = requireNonNull(listClass);
        this.routeQname = BindingReflections.findQName(listClass).withModule(module);
        this.routesListIdentifier = new NodeIdentifier(this.routeQname);
        this.tk = new TablesKey(afiClass, safiClass);
        this.emptyTable = (MapEntryNode) this.mappingService
                .toNormalizedNode(TABLES_II, new TablesBuilder().withKey(tk)
                        .setAttributes(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib
                                .rev180329.rib.tables.AttributesBuilder().build()).build()).getValue();
        this.afiClass = afiClass;
        this.safiClass = safiClass;
        this.destinationNid = new NodeIdentifier(destContainerQname);
        this.pathIdQname = QName.create(routeQName(), "path-id").intern();
        this.pathIdNid = new NodeIdentifier(this.pathIdQname);
        this.routeKeyQname = QName.create(routeQName(), ROUTE_KEY).intern();
        this.prefixTypeNid = NodeIdentifier.create(QName.create(destContainerQname, "prefix").intern());
        this.rdNid = NodeIdentifier.create(QName.create(destContainerQname, "route-distinguisher").intern());
        this.routeDefaultYii =
                YangInstanceIdentifier.builder()
                        .node(BgpRib.QNAME)
                        .node(Rib.QNAME)
                        .node(Rib.QNAME)
                        .node(LocRib.QNAME)
                        .node(Tables.QNAME)
                        .node(Tables.QNAME)
                        .node(Routes.QNAME)
                        .node(BindingReflections.findQName(containerClass).withModule(module))
                        .node(this.routeQname)
                        .node(this.routeQname).build();
    }

    @Override
    public final TablesKey getTablesKey() {
        return this.tk;
    }

    @Override
    public final Class<C> routesCaseClass() {
        return this.cazeClass;
    }

    @Override
    public final Class<S> routesContainerClass() {
        return this.containerClass;
    }

    @Override
    public final Class<R> routesListClass() {
        return this.listClass;
    }

    @Override
    public final MapEntryNode emptyTable() {
        return this.emptyTable;
    }

    public final QName routeQName() {
        return this.routeQname;
    }

    protected final NodeIdentifier prefixNid() {
        return this.prefixTypeNid;
    }

    protected final NodeIdentifier routeNid() {
        return this.routesListIdentifier;
    }

    @Override
    public final Class<? extends AddressFamily> getAfi() {
        return this.afiClass;
    }

    @Override
    public final Class<? extends SubsequentAddressFamily> getSafi() {
        return this.safiClass;
    }

    /**
     * Build MpReachNlri object from DOM representation.
     *
     * @param routes Collection of MapEntryNode DOM representation of routes
     * @param hop    CNextHop as it was parsed from Attributes, to be included in MpReach object
     * @return MpReachNlri
     */
    private MpReachNlri buildReach(final Collection<MapEntryNode> routes, final CNextHop hop) {
        final MpReachNlriBuilder mb = new MpReachNlriBuilder();
        mb.setAfi(this.getAfi());
        mb.setSafi(this.getSafi());
        mb.setCNextHop(hop);
        mb.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(buildDestination(routes)).build());
        return mb.build();
    }

    /**
     * Build MpUnReachNlri object from DOM representation.
     *
     * @param routes Collection of MapEntryNode DOM representation of routes
     * @return MpUnreachNlri
     */
    private MpUnreachNlri buildUnreach(final Collection<MapEntryNode> routes) {
        final MpUnreachNlriBuilder mb = new MpUnreachNlriBuilder();
        mb.setAfi(this.getAfi());
        mb.setSafi(this.getSafi());
        mb.setWithdrawnRoutes(new WithdrawnRoutesBuilder()
                .setDestinationType(buildWithdrawnDestination(routes)).build());
        return mb.build();
    }

    protected abstract DestinationType buildDestination(Collection<MapEntryNode> routes);

    protected abstract DestinationType buildWithdrawnDestination(Collection<MapEntryNode> routes);

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
    private NodeIdentifier destinationContainerIdentifier() {
        return this.destinationNid;
    }

    /**
     * Given the destination as ContainerNode, implementation needs to parse the DOM model
     * from this point onward:
     *
     * {@code /bgp-mp:mp-unreach-nlri/bgp-mp:withdrawn-routes/bgp-mp:destination-type}
     * and delete the routes from its RIBs.
     *
     * @param tx           DOMDataWriteTransaction to be passed into implementation
     * @param tablePath    YangInstanceIdentifier to be passed into implementation
     * @param destination  ContainerNode DOM representation of NLRI in Update message
     * @param routesNodeId NodeIdentifier
     */
    private void deleteDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
            final ContainerNode destination, final NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, null, DELETE_ROUTE);
    }

    /**
     * Given the destination as ContainerNode, implementation needs to parse the DOM model
     * from this point onward:
     *
     * {@code /bgp-mp:mp-reach-nlri/bgp-mp:advertized-routes/bgp-mp:destination-type}
     * and put the routes to its RIBs.
     *
     * @param tx           DOMDataWriteTransaction to be passed into implementation
     * @param tablePath    YangInstanceIdentifier to be passed into implementation
     * @param destination  ContainerNode DOM representation of NLRI in Update message
     * @param attributes   ContainerNode to be passed into implementation
     * @param routesNodeId NodeIdentifier
     */
    private void putDestinationRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
            final ContainerNode destination, final ContainerNode attributes, final NodeIdentifier routesNodeId) {
        processDestination(tx, tablePath.node(routesNodeId), destination, attributes, this.putRoute);
    }

    protected abstract void processDestination(DOMDataWriteTransaction tx, YangInstanceIdentifier routesPath,
            ContainerNode destination, ContainerNode attributes, ApplyRoute applyFunction);

    private static ContainerNode getDestination(final DataContainerChild<? extends PathArgument, ?> routes,
            final NodeIdentifier destinationId) {
        if (routes instanceof ContainerNode) {
            final java.util.Optional<DataContainerChild<? extends PathArgument, ?>> maybeDestination =
                    ((ContainerNode) routes).getChild(DESTINATION_TYPE);
            if (maybeDestination.isPresent()) {
                final DataContainerChild<? extends PathArgument, ?> destination = maybeDestination.get();
                if (destination instanceof ChoiceNode) {
                    final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRet =
                            ((ChoiceNode) destination).getChild(destinationId);
                    if (maybeRet.isPresent()) {
                        final DataContainerChild<? extends PathArgument, ?> ret = maybeRet.get();
                        if (ret instanceof ContainerNode) {
                            return (ContainerNode) ret;
                        }

                        LOG.debug("Specified node {} is not a container, ignoring it", ret);
                    } else {
                        LOG.debug("Specified container {} is not present in destination {}",
                                destinationId, destination);
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
        final DataTreeCandidateNode myRoutes = routes.getModifiedChild(this.routesContainerIdentifier);
        if (myRoutes == null) {
            return Collections.emptySet();
        }
        final DataTreeCandidateNode routesMap = myRoutes.getModifiedChild(routeNid());
        if (routesMap == null) {
            return Collections.emptySet();
        }
        // Well, given the remote possibility of augmentation, we should perform a filter here,
        // to make sure the type matches what routeType() reports.
        return routesMap.getChildNodes();
    }

    @Override
    public final YangInstanceIdentifier routePath(
            final YangInstanceIdentifier routesTablePaths, final PathArgument routeId) {
        return routesYangInstanceIdentifier(routesTablePaths.node(Routes.QNAME)).node(routeId);
    }

    @Override
    public final InstanceIdentifier<R> createRouteIdentifier(
            final KeyedInstanceIdentifier<Tables, TablesKey> tableIId, final I key) {
        //FIXME Cache
        return tableIId.child(routesCaseClass(), routesContainerClass()).child(routesListClass(), key);
    }

    @Override
    public final void deleteRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
            final ContainerNode nlri) {
        deleteRoutes(tx, tablePath, nlri, ROUTES);
    }

    @Override
    public final void putRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
            final ContainerNode nlri, final ContainerNode attributes) {
        putRoutes(tx, tablePath, nlri, attributes, ROUTES);
    }

    @Override
    public final Update buildUpdate(final Collection<MapEntryNode> advertised, final Collection<MapEntryNode> withdrawn,
            final Attributes attr) {
        final UpdateBuilder ub = new UpdateBuilder();
        final AttributesBuilder ab = new AttributesBuilder(attr);
        final CNextHop hop = ab.getCNextHop();

        LOG.debug("cnextHop before={}", hop);
        // do not preserve next hop in attributes if we are using MpReach
        ab.setCNextHop(null);

        if (!advertised.isEmpty()) {
            final MpReachNlri mb = buildReach(advertised, hop);
            ab.addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(mb).build());
            LOG.debug("mpreach nexthop={}", mb);
        }
        if (!withdrawn.isEmpty()) {
            final MpUnreachNlri mb = buildUnreach(withdrawn);
            ab.addAugmentation(Attributes2.class, new Attributes2Builder().setMpUnreachNlri(mb).build());
            LOG.debug("mpunrach mb={}", mb);
        }

        ub.setAttributes(ab.build());
        LOG.debug("update {}", ub.build());
        return ub.build();
    }

    @Override
    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    public final void deleteRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
            final ContainerNode nlri, final NodeIdentifier routesNodeId) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = nlri.getChild(WITHDRAWN_ROUTES);
        if (maybeRoutes.isPresent()) {
            final ContainerNode destination = getDestination(maybeRoutes.get(), destinationContainerIdentifier());
            if (destination != null) {
                deleteDestinationRoutes(tx, tablePath, destination, routesNodeId);
            }
        } else {
            LOG.debug("Withdrawn routes are not present in NLRI {}", nlri);
        }
    }

    @Override
    public final void putRoutes(final DOMDataWriteTransaction tx, final YangInstanceIdentifier tablePath,
            final ContainerNode nlri, final ContainerNode attributes, final NodeIdentifier routesNodeId) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRoutes = nlri.getChild(ADVERTISED_ROUTES);
        if (maybeRoutes.isPresent()) {
            final ContainerNode destination = getDestination(maybeRoutes.get(), destinationContainerIdentifier());
            if (destination != null) {
                putDestinationRoutes(tx, tablePath, destination, attributes, routesNodeId);
            }
        } else {
            LOG.debug("Advertized routes are not present in NLRI {}", nlri);
        }
    }

    private static final class DeleteRoute implements ApplyRoute {
        @Override
        public void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base,
                final NodeIdentifierWithPredicates routeKey, final DataContainerNode<?> route,
                final ContainerNode attributes) {
            tx.delete(LogicalDatastoreType.OPERATIONAL, base.node(routeKey));
        }
    }

    private final class PutRoute implements ApplyRoute {
        @Override
        public void apply(final DOMDataWriteTransaction tx, final YangInstanceIdentifier base,
                final NodeIdentifierWithPredicates routeKey, final DataContainerNode<?> route,
                final ContainerNode attributes) {
            // Build the DataContainer data
            final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> b =
                    ImmutableNodes.mapEntryBuilder();
            b.withNodeIdentifier(routeKey);

            route.getValue().forEach(b::withChild);
            // Add attributes
            final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cb =
                    Builders.containerBuilder(attributes);
            cb.withNodeIdentifier(routeAttributesIdentifier());
            b.withChild(cb.build());
            tx.put(LogicalDatastoreType.OPERATIONAL, base.node(routeKey), b.build());
        }
    }

    protected final NodeIdentifier routePathIdNid() {
        return this.pathIdNid;
    }

    protected final QName pathIdQName() {
        return this.pathIdQname;
    }

    protected final QName routeKeyQName() {
        return this.routeKeyQname;
    }

    protected final String extractPrefix(final DataContainerNode<? extends PathArgument> route) {
        return (String) route.getChild(prefixTypeNid).get().getValue();
    }

    protected final RouteDistinguisher extractRouteDistinguisher(
            final DataContainerNode<? extends PathArgument> route) {
        if (route.getChild(this.rdNid).isPresent()) {
            return RouteDistinguisherBuilder.getDefaultInstance((String) route.getChild(this.rdNid).get().getValue());
        }
        return null;
    }

    protected final YangInstanceIdentifier routesYangInstanceIdentifier(final YangInstanceIdentifier routesTablePaths) {
        return this.routesPath.getUnchecked(routesTablePaths);
    }
}
