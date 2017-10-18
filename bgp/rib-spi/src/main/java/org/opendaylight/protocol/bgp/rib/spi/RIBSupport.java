/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.collect.ImmutableCollection;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;

/**
 * Interface implemented for AFI/SAFI-specific RIB extensions. The extensions need
 * to register an implementation of this class and the RIB core then calls into it
 * to inquire about details specific to that particular model.
 */
public interface RIBSupport extends AddPathRibSupport {
    /**
     * Return the table-type-specific empty routes container, as augmented into the
     * bgp-rib model under /rib/tables/routes choice node. This needs to include all
     * the skeleton nodes under which the individual routes will be stored.
     *
     * @return Protocol-specific case in the routes choice, may not be null.
     */
    @Nonnull ChoiceNode emptyRoutes();

    /**
     * Return the localized identifier of the attributes route member, as expanded
     * from the route grouping in the specific augmentation of the base routes choice.
     *
     * @return The attributes identifier, may not be null.
     */
    @Nonnull NodeIdentifier routeAttributesIdentifier();

    /**
     * Return class object of the Routes Case statement.
     *
     * @return Class
     */
    @Nonnull Class<? extends Routes> routesCaseClass();

    /**
     * Return class object of the Routes Container statement.
     *
     * @return Class
     */
    @Nonnull Class<? extends DataObject> routesContainerClass();

    /**
     * Return class object of the Routes List statement.
     *
     * @return Class
     */
    @Nonnull Class<? extends Route> routesListClass();

    @Nonnull ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects();
    @Nonnull ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects();

    /**
     * Given the NLRI as ContainerNode, this method should extract withdrawn routes
     * from the DOM model and delete them from RIBs.
     *
     * @param tx DOMDataWriteTransaction
     * @param tablePath YangInstanceIdentifier
     * @param nlri ContainerNode DOM representation of NLRI in Update message
     */
    void deleteRoutes(@Nonnull DOMDataWriteTransaction tx, @Nonnull YangInstanceIdentifier tablePath, @Nonnull ContainerNode nlri);


    /**
     * Given the NLRI as ContainerNode, this method should extract withdrawn routes
     * from the DOM model and delete them from RIBs.
     * <p>
     * Use this method when removing routes stored in RIBs out of the "bgp-rib" module.
     * Provide {@link NodeIdentifier} with customized "routes" QName.
     * For default "bgp-rib" RIBs use {@link #deleteRoutes}
     * </p>
     *
     * @param tx DOMDataWriteTransaction
     * @param tablePath YangInstanceIdentifier
     * @param nlri ContainerNode DOM representation of NLRI in Update message
     * @param routesNodeId NodeIdentifier of "routes" data node
     */
    void deleteRoutes(@Nonnull DOMDataWriteTransaction tx, @Nonnull YangInstanceIdentifier tablePath, @Nonnull ContainerNode nlri, @Nonnull NodeIdentifier routesNodeId);

    /**
     * Given the NLRI as ContainerNode, this method should extract advertised routes
     * from the DOM model and put them into RIBs.
     *
     * @param tx DOMDataWriteTransaction
     * @param tablePath YangInstanceIdentifier
     * @param nlri ContainerNode DOM representation of NLRI in Update message
     * @param attributes ContainerNode
     */
    void putRoutes(@Nonnull DOMDataWriteTransaction tx, @Nonnull YangInstanceIdentifier tablePath, @Nonnull ContainerNode nlri, @Nonnull ContainerNode attributes);

    /**
     * Given the NLRI as ContainerNode, this method should extract advertised routes
     * from the DOM model and put them into RIBs.
     * <p>
     * Use this method when putting routes stored in RIBs out of the "bgp-rib" module.
     * Provide {@link NodeIdentifier} with customized "routes" QName.
     * For default "bgp-rib" RIBs use {@link #putRoutes}
     * </p>
     *
     * @param tx DOMDataWriteTransaction
     * @param tablePath YangInstanceIdentifier
     * @param nlri ContainerNode DOM representation of NLRI in Update message
     * @param attributes ContainerNode
     * @param routesNodeId NodeIdentifier of "routes" data node
     */
    void putRoutes(@Nonnull DOMDataWriteTransaction tx, @Nonnull YangInstanceIdentifier tablePath, @Nonnull ContainerNode nlri,
            @Nonnull ContainerNode attributes, @Nonnull NodeIdentifier routesNodeId);

    /**
     * Returns routes that were modified within this RIB support instance.
     *
     * @param routes DataTreeCandidateNode
     * @return collection of modified nodes or empty collection if no node was modified
     */
    @Nonnull Collection<DataTreeCandidateNode> changedRoutes(@Nonnull DataTreeCandidateNode routes);

    /**
     * Constructs an instance identifier path to routeId.
     *
     * @param routesPath YangInstanceIdentifier base path
     * @param routeId PathArgument leaf path
     * @return YangInstanceIdentifier with routesPath + specific RIB support routes path + routeId
     */
    @Nonnull YangInstanceIdentifier routePath(@Nonnull YangInstanceIdentifier routesPath, @Nonnull PathArgument routeId);

    /**
     * Indicate whether this AFI/SAFI combination is a complex route. Simple routes are those which
     * only have their key and attributes, complex routes are those which include more structured data.
     *
     * @return True if this is a complex route, false otherwise.
     */
    boolean isComplexRoute();

    /**
     * To send routes out, we'd need to transform the DOM representation of route to
     * binding-aware format. This needs to be done per each AFI/SAFI.
     *
     * @param advertised Collection of advertised routes in DOM format
     * @param withdrawn Collection of withdrawn routes in DOM format
     * @param attr Attributes MpReach is part of Attributes so we need to pass
     *             it as argument, create new AttributesBuilder with existing
     *             attributes and add MpReach
     * @return Update message ready to be sent out
     */
    @Nonnull Update buildUpdate(@Nonnull Collection<MapEntryNode> advertised, @Nonnull Collection<MapEntryNode> withdrawn, @Nonnull Attributes attr);

    @Nonnull Class<? extends AddressFamily> getAfi();

    @Nonnull Class<? extends SubsequentAddressFamily> getSafi();

    interface ApplyRoute {
        void apply(DOMDataWriteTransaction tx, YangInstanceIdentifier base, NodeIdentifierWithPredicates routeKey, DataContainerNode<?> route,
            ContainerNode attributes);
    }
}
