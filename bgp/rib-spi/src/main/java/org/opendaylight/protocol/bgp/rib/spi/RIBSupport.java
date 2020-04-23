/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.BindingObject;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;

/**
 * Interface implemented for AFI/SAFI-specific RIB extensions. The extensions need
 * to register an implementation of this class and the RIB core then calls into it
 * to inquire about details specific to that particular model.
 */
public interface RIBSupport<
        C extends Routes & DataObject & ChoiceIn<Tables>,
        S extends ChildOf<? super C>,
        R extends Route & ChildOf<? super S> & Identifiable<I>,
        I extends Identifier<R>> {
    /**
     * Return the table-type-specific empty table with routes empty container, as augmented into the
     * bgp-rib model under /rib/tables/routes choice node. This needs to include all
     * the skeleton nodes under which the individual routes will be stored.
     *
     * @return Protocol-specific case in the routes choice, may not be null.
     */
    @NonNull MapEntryNode emptyTable();

    /**
     * Return the localized identifier of the attributes route member, as expanded
     * from the route grouping in the specific augmentation of the base routes choice.
     *
     * @return The attributes identifier, may not be null.
     */
    @NonNull NodeIdentifier routeAttributesIdentifier();

    /**
     * Return class object of the Routes Case statement.
     *
     * @return Class
     */
    @NonNull Class<C> routesCaseClass();

    /**
     * Return class object of the Routes Container statement.
     *
     * @return Class
     */
    @NonNull Class<S> routesContainerClass();

    /**
     * Return class object of the Routes List statement.
     *
     * @return Class
     */
    @NonNull Class<R> routesListClass();

    default @NonNull ImmutableCollection<Class<? extends BindingObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    default @NonNull ImmutableCollection<Class<? extends BindingObject>> cacheableNlriObjects() {
        return ImmutableSet.of();
    }

    /**
     * Given the NLRI as ContainerNode, this method should extract withdrawn routes
     * from the DOM model and delete them from RIBs.
     *
     * @param tx        DOMDataWriteTransaction
     * @param tablePath YangInstanceIdentifier
     * @param nlri      ContainerNode DOM representation of NLRI in Update message
     */
    void deleteRoutes(@NonNull DOMDataTreeWriteTransaction tx, @NonNull YangInstanceIdentifier tablePath,
            @NonNull ContainerNode nlri);

    /**
     * Given the NLRI as ContainerNode, this method should extract withdrawn routes
     * from the DOM model and delete them from RIBs.
     * <p>
     * Use this method when removing routes stored in RIBs out of the "bgp-rib" module.
     * Provide {@link NodeIdentifier} with customized "routes" QName.
     * For default "bgp-rib" RIBs use {@link #deleteRoutes}
     * </p>
     *
     * @param tx           DOMDataWriteTransaction
     * @param tablePath    YangInstanceIdentifier
     * @param nlri         ContainerNode DOM representation of NLRI in Update message
     * @param routesNodeId NodeIdentifier of "routes" data node
     */
    void deleteRoutes(@NonNull DOMDataTreeWriteTransaction tx, @NonNull YangInstanceIdentifier tablePath,
            @NonNull ContainerNode nlri, @NonNull NodeIdentifier routesNodeId);

    /**
     * Given the NLRI as ContainerNode, this method should extract advertised routes
     * from the DOM model and put them into RIBs.
     *
     * @param tx         DOMDataWriteTransaction
     * @param tablePath  YangInstanceIdentifier
     * @param nlri       ContainerNode DOM representation of NLRI in Update message
     * @param attributes ContainerNode
     * @return List of processed route Identifiers
     */
    Collection<NodeIdentifierWithPredicates> putRoutes(@NonNull DOMDataTreeWriteTransaction tx,
            @NonNull YangInstanceIdentifier tablePath, @NonNull ContainerNode nlri, @NonNull ContainerNode attributes);

    /**
     * Given the NLRI as ContainerNode, this method should extract advertised routes
     * from the DOM model and put them into RIBs.
     * <p>
     * Use this method when putting routes stored in RIBs out of the "bgp-rib" module.
     * Provide {@link NodeIdentifier} with customized "routes" QName.
     * For default "bgp-rib" RIBs use {@link #putRoutes}
     * </p>
     *
     * @param tx           DOMDataWriteTransaction
     * @param tablePath    YangInstanceIdentifier
     * @param nlri         ContainerNode DOM representation of NLRI in Update message
     * @param attributes   ContainerNode
     * @param routesNodeId NodeIdentifier of "routes" data node
     * @return List of processed routes identifiers
     */
    Collection<NodeIdentifierWithPredicates> putRoutes(@NonNull DOMDataTreeWriteTransaction tx,
            @NonNull YangInstanceIdentifier tablePath, @NonNull ContainerNode nlri, @NonNull ContainerNode attributes,
            @NonNull NodeIdentifier routesNodeId);

    /**
     * Returns routes that were modified within this RIB support instance.
     *
     * @param routes DataTreeCandidateNode
     * @return collection of modified nodes or empty collection if no node was modified
     */
    @NonNull Collection<DataTreeCandidateNode> changedRoutes(@NonNull DataTreeCandidateNode routes);

    /**
     * Constructs an instance identifier path to routeId.
     *
     * @param routesPath YangInstanceIdentifier base path
     * @param routeId    PathArgument leaf path
     * @return YangInstanceIdentifier with routesPath + specific RIB support routes path + routeId
     */
    default @NonNull YangInstanceIdentifier routePath(final @NonNull YangInstanceIdentifier routesPath,
                                             final @NonNull PathArgument routeId) {
        return routesPath(routesPath).node(routeId);
    }

    /**
     * Constructs an instance identifier path to routes list.
     *
     * @param routesPath YangInstanceIdentifier base path
     * @return YangInstanceIdentifier with routesPath + specific RIB support routes path
     */
    @NonNull YangInstanceIdentifier routesPath(@NonNull YangInstanceIdentifier routesPath);

    /**
     * Return the relative path from the generic routes container to the AFI/SAFI specific route list.
     *
     * @return Relative path.
     */
    @NonNull List<PathArgument> relativeRoutesPath();

    /**
     * To send routes out, we'd need to transform the DOM representation of route to
     * binding-aware format. This needs to be done per each AFI/SAFI.
     *
     * @param advertised Collection of advertised routes in DOM format
     * @param withdrawn  Collection of withdrawn routes in DOM format
     * @param attr       Attributes MpReach is part of Attributes so we need to pass
     *                   it as argument, create new AttributesBuilder with existing
     *                   attributes and add MpReach
     * @return Update message ready to be sent out
     */
    @NonNull Update buildUpdate(@NonNull Collection<MapEntryNode> advertised,
            @NonNull Collection<MapEntryNode> withdrawn, @NonNull Attributes attr);

    @NonNull Class<? extends AddressFamily> getAfi();

    @NonNull Class<? extends SubsequentAddressFamily> getSafi();

    /**
     * Creates Route table Peer InstanceIdentifier.
     *
     * @param tableKey    table InstanceIdentifier
     * @param newRouteKey route key
     * @return InstanceIdentifier
     */
    @NonNull InstanceIdentifier<R> createRouteIdentifier(@NonNull KeyedInstanceIdentifier<Tables, TablesKey> tableKey,
            @NonNull I newRouteKey);

    /**
     * Creates a route with new path Id and attributes.
     *
     * @param route route
     * @param key route key
     * @param attributes route attributes
     * @return Route List key
     */
    @NonNull R createRoute(@Nullable R route, @NonNull I key, @NonNull Attributes attributes);

    /**
     * Returns TablesKey which we are providing support.
     *
     * @return TablesKey
     */
    TablesKey getTablesKey();

    /**
     * Translates supplied YANG Instance Identifier and NormalizedNode into Binding Route.
     *
     * @param routerId Binding Instance Identifier
     * @param normalizedNode NormalizedNode representing Route
     * @return Route
     */
    R fromNormalizedNode(YangInstanceIdentifier routerId, NormalizedNode<?, ?> normalizedNode);

    /**
     * Translates supplied YANG Instance Identifier and NormalizedNode into Binding data Attribute.
     * @param advertisedAttrs NormalizedNode representing attributes
     * @return Attribute
     */
    Attributes attributeFromContainerNode(ContainerNode advertisedAttrs);

    /**
     * Translates supplied Binding Instance Identifier and data into NormalizedNode representation.
     * @param routePath Binding Instance Identifier pointing to data
     * @param attributes Data object representing Attributes
     * @return NormalizedNode representation
     */
    ContainerNode attributeToContainerNode(YangInstanceIdentifier routePath, Attributes attributes);

    interface ApplyRoute {
        void apply(@NonNull DOMDataTreeWriteTransaction tx, @NonNull YangInstanceIdentifier base,
                @NonNull NodeIdentifierWithPredicates routeKey, @NonNull DataContainerNode<?> route,
                ContainerNode attributes);
    }

    /**
     * Return the table-type-specific empty routes container, as augmented into the
     * bgp-peer model under /peer/effect-rib-in/tables/routes choice node/routes container. This needs to include all
     * the skeleton nodes under which the individual routes will be stored.
     *
     * @return Protocol-specific container in the routes, may not be null.
     */
    @NonNull S emptyRoutesContainer();

    /**
     * Construct a Route List Key using new path Id for Families.
     *
     * @param pathId   The path identifier
     * @param routeKey RouteKey
     * @return route list Key (RouteKey + pathId)
     */
    @NonNull I createRouteListKey(@NonNull PathId pathId, @NonNull String routeKey);

    /**
     * Construct a Route List Key.
     *
     * @param routeKey RouteKey
     * @return route list Key (RouteKey + empty pathId)
     */
    default @NonNull I createRouteListKey(final @NonNull String routeKey) {
        return createRouteListKey(NON_PATH_ID, routeKey);
    }

    /**
     * Given a route list key, return the associated path ID.
     *
     * @param routeListKey Route list key
     * @return Path ID
     */
    @NonNull PathId extractPathId(@NonNull I routeListKey);

    /**
     * Given a route list key, return the associated path ID.
     *
     * @param routeListKey Route list key
     * @return RouteKey
     */
    @NonNull String extractRouteKey(@NonNull I routeListKey);

    /**
     * Extract a route list from the adj-rib-in instantiation of table routes.
     *
     * @param routes Table route choice
     * @return A potentially empty list of routes
     */
    @NonNull Map<I, R> extractAdjRibInRoutes(Routes routes);
}
