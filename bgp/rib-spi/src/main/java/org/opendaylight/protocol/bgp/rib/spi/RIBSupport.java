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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.SubsequentAddressFamily;
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
    @Nonnull
    MapEntryNode emptyTable();

    /**
     * Return the localized identifier of the attributes route member, as expanded
     * from the route grouping in the specific augmentation of the base routes choice.
     *
     * @return The attributes identifier, may not be null.
     */
    @Nonnull
    NodeIdentifier routeAttributesIdentifier();

    /**
     * Return class object of the Routes Case statement.
     *
     * @return Class
     */
    @Nonnull
    Class<C> routesCaseClass();

    /**
     * Return class object of the Routes Container statement.
     *
     * @return Class
     */
    @Nonnull
    Class<S> routesContainerClass();

    /**
     * Return class object of the Routes List statement.
     *
     * @return Class
     */
    @Nonnull
    Class<R> routesListClass();

    @Nullable List<R> routesFromContainer(@Nonnull S container);

    @Nonnull
    default ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return ImmutableSet.of();
    }

    @Nonnull
    default ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
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
    void deleteRoutes(@Nonnull DOMDataWriteTransaction tx, @Nonnull YangInstanceIdentifier tablePath,
            @Nonnull ContainerNode nlri);


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
    void deleteRoutes(@Nonnull DOMDataWriteTransaction tx, @Nonnull YangInstanceIdentifier tablePath,
            @Nonnull ContainerNode nlri, @Nonnull NodeIdentifier routesNodeId);

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
    Collection<NodeIdentifierWithPredicates> putRoutes(@Nonnull DOMDataWriteTransaction tx,
            @Nonnull YangInstanceIdentifier tablePath, @Nonnull ContainerNode nlri, @Nonnull ContainerNode attributes);

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
    Collection<NodeIdentifierWithPredicates> putRoutes(@Nonnull DOMDataWriteTransaction tx,
            @Nonnull YangInstanceIdentifier tablePath, @Nonnull ContainerNode nlri, @Nonnull ContainerNode attributes,
            @Nonnull NodeIdentifier routesNodeId);

    /**
     * Returns routes that were modified within this RIB support instance.
     *
     * @param routes DataTreeCandidateNode
     * @return collection of modified nodes or empty collection if no node was modified
     */
    @Nonnull
    Collection<DataTreeCandidateNode> changedRoutes(@Nonnull DataTreeCandidateNode routes);

    /**
     * Constructs an instance identifier path to routeId.
     *
     * @param routesPath YangInstanceIdentifier base path
     * @param routeId    PathArgument leaf path
     * @return YangInstanceIdentifier with routesPath + specific RIB support routes path + routeId
     */
    @Nonnull
    default YangInstanceIdentifier routePath(@Nonnull final YangInstanceIdentifier routesPath,
                                             @Nonnull final PathArgument routeId) {
        return routesPath(routesPath).node(routeId);
    }

    /**
     * Constructs an instance identifier path to routes list.
     *
     * @param routesPath YangInstanceIdentifier base path
     * @return YangInstanceIdentifier with routesPath + specific RIB support routes path
     */
    @Nonnull
    YangInstanceIdentifier routesPath(@Nonnull YangInstanceIdentifier routesPath);

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
    @Nonnull
    Update buildUpdate(
            @Nonnull Collection<MapEntryNode> advertised,
            @Nonnull Collection<MapEntryNode> withdrawn,
            @Nonnull Attributes attr);

    @Nonnull
    Class<? extends AddressFamily> getAfi();

    @Nonnull
    Class<? extends SubsequentAddressFamily> getSafi();

    /**
     * Creates Route table Peer InstanceIdentifier.
     *
     * @param tableKey    table InstanceIdentifier
     * @param newRouteKey route key
     * @return InstanceIdentifier
     */
    @Nonnull
    InstanceIdentifier<R> createRouteIdentifier(
            @Nonnull KeyedInstanceIdentifier<Tables, TablesKey> tableKey,
            @Nonnull I newRouteKey);

    /**
     * Creates a route with new path Id and attributes.
     *
     * @param route route
     * @param key route key
     * @param attributes route attributes
     * @return Route List key
     */
    @Nonnull
    R createRoute(@Nullable R route, @Nonnull I key, @Nonnull Attributes attributes);

    /**
     * Returns TablesKey which we are providing support.
     *
     * @return TablesKey
     */
    TablesKey getTablesKey();

    interface ApplyRoute {
        void apply(@Nonnull DOMDataWriteTransaction tx, @Nonnull YangInstanceIdentifier base,
                   @Nonnull NodeIdentifierWithPredicates routeKey,
                   @Nonnull DataContainerNode<?> route, ContainerNode attributes);
    }

    /**
     * Return the table-type-specific empty routes container, as augmented into the
     * bgp-peer model under /peer/effect-rib-in/tables/routes choice node. This needs to include all
     * the skeleton nodes under which the individual routes will be stored.
     *
     * @return Protocol-specific case in the routes choice, may not be null.
     */
    @Nonnull
    @Deprecated
    C emptyRoutesCase();

    /**
     * Return the table-type-specific empty routes container, as augmented into the
     * bgp-peer model under /peer/effect-rib-in/tables/routes choice node/routes container. This needs to include all
     * the skeleton nodes under which the individual routes will be stored.
     *
     * @return Protocol-specific container in the routes, may not be null.
     */
    @Nonnull
    S emptyRoutesContainer();

    /**
     * Construct a Route List Key using new path Id for Families.
     *
     * @param pathId   The path identifier
     * @param routeKey RouteKey
     * @return route list Key (RouteKey + pathId)
     */
    @Nonnull
    I createRouteListKey(@Nonnull PathId pathId, @Nonnull String routeKey);

    /**
     * Construct a Route List Key.
     *
     * @param routeKey RouteKey
     * @return route list Key (RouteKey + empty pathId)
     */
    @Nonnull
    default I createRouteListKey(@Nonnull final String routeKey) {
        return createRouteListKey(NON_PATH_ID, routeKey);
    }

    /**
     * Given a route list key, return the associated path ID.
     *
     * @param routeListKey Route list key
     * @return Path ID
     */
    @Nonnull
    PathId extractPathId(@Nonnull I routeListKey);

    /**
     * Given a route list key, return the associated path ID.
     *
     * @param routeListKey Route list key
     * @return RouteKey
     */
    @Nonnull
    String extractRouteKey(@Nonnull I routeListKey);
}
