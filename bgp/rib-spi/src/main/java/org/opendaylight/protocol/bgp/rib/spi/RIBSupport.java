/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID_VALUE;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
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
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;

/**
 * Interface implemented for AFI/SAFI-specific RIB extensions. The extensions need
 * to register an implementation of this class and the RIB core then calls into it
 * to inquire about details specific to that particular model.
 */
public interface RIBSupport<C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>> {
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
    @NonNull Class<? extends Route> routesListClass();

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
     * @param tablePath table InstanceIdentifier
     * @param newRouteKey route key
     * @return InstanceIdentifier
     */
    @NonNull YangInstanceIdentifier createRouteIdentifier(@NonNull YangInstanceIdentifier tablePath,
            @NonNull NodeIdentifierWithPredicates newRouteKey);

    /**
     * Creates a route with new path Id and attributes.
     *
     * @param route route
     * @param key route key
     * @param attributes route attributes
     * @return Route List key
     */
    @NonNull MapEntryNode createRoute(@Nullable MapEntryNode route, @NonNull NodeIdentifierWithPredicates key,
        @NonNull ContainerNode attributes);

    /**
     * Returns TablesKey which we are providing support.
     *
     * @return TablesKey
     */
    @NonNull TablesKey getTablesKey();

    @NonNull NodeIdentifierWithPredicates tablesKey();

    /**
     * Translates supplied YANG Instance Identifier and NormalizedNode into Binding Route.
     *
     * @param routerId Binding Instance Identifier
     * @param normalizedNode NormalizedNode representing Route
     * @return Route
     */
    Route fromNormalizedNode(YangInstanceIdentifier routerId, NormalizedNode normalizedNode);

    /**
     * Translates supplied YANG Instance Identifier and NormalizedNode into Binding data Attribute.
     * @param advertisedAttrs NormalizedNode representing attributes
     * @return Attribute
     */
    @NonNull Attributes attributeFromContainerNode(ContainerNode advertisedAttrs);

    /**
     * Translates supplied Binding Instance Identifier and data into NormalizedNode representation.
     * @param routePath Binding Instance Identifier pointing to data
     * @param attributes Data object representing Attributes
     * @return NormalizedNode representation
     */
    @NonNull ContainerNode attributeToContainerNode(YangInstanceIdentifier routePath, Attributes attributes);

    interface ApplyRoute {
        void apply(@NonNull DOMDataTreeWriteTransaction tx, @NonNull YangInstanceIdentifier base,
                @NonNull NodeIdentifierWithPredicates routeKey, @NonNull DataContainerNode route,
                ContainerNode attributes);
    }

    /**
     * Construct a Route List Key using new path Id for Families.
     *
     * @param pathId   The path identifier
     * @param routeKey RouteKey
     * @return route list Key (RouteKey + pathId)
     */
    @NonNull NodeIdentifierWithPredicates createRouteListArgument(@NonNull Uint32 pathId, @NonNull String routeKey);

    /**
     * Construct a Route List Key.
     *
     * @param routeKey RouteKey
     * @return route list Key (RouteKey + empty pathId)
     */
    default @NonNull NodeIdentifierWithPredicates createRouteListArgument(final @NonNull String routeKey) {
        return createRouteListArgument(NON_PATH_ID_VALUE, routeKey);
    }

    default @NonNull NodeIdentifierWithPredicates toAddPathListArgument(
            final @NonNull NodeIdentifierWithPredicates routeListKey) {
        return createRouteListArgument(extractPathId(routeListKey), extractRouteKey(routeListKey));
    }

    default @NonNull NodeIdentifierWithPredicates toNonPathListArgument(
            final @NonNull NodeIdentifierWithPredicates routeListKey) {
        final Uint32 pathId = extractPathId(routeListKey);
        return pathId.equals(NON_PATH_ID_VALUE) ? routeListKey : createRouteListArgument(extractRouteKey(routeListKey));
    }

    /**
     * Given a route list key, return the associated path ID.
     *
     * @param routeListKey Route list key
     * @return Path ID
     */
    @NonNull Uint32 extractPathId(@NonNull NodeIdentifierWithPredicates routeListKey);

    /**
     * Given a route list key, return the associated path ID.
     *
     * @param routeListKey Route list key
     * @return RouteKey
     */
    @NonNull String extractRouteKey(@NonNull NodeIdentifierWithPredicates routeListKey);

    /**
     * Extract attributes from an route entry.
     *
     * @param route Route entry
     * @return Associated attributes, potentially null
     * @throws NullPointerException if route is null
     */
    @Nullable ContainerNode extractAttributes(@NonNull MapEntryNode route);
}
