/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.collect.ImmutableMap;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Implements common methods for Advertisement of Multiple Paths on ribSupport
 */
public abstract class MultiPathAbstractRIBSupport extends AbstractRIBSupport {
    private final QName routeKeyQname;
    private final QName pathIdQname;
    private final NodeIdentifier pathIdNid;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     *
     * @param cazeClass Binding class of the AFI/SAFI-specific case statement, must not be null
     * @param containerClass Binding class of the container in routes choice, must not be null.
     * @param listClass Binding class of the route list, nust not be null;
     * @param addressFamilyClass address Family Class
     * @param safiClass SubsequentAddressFamily
     * @param routeKeyNaming Route Key name (prefix/ route-key / etc..)
     * @param destinationQname destination Qname
     */
    protected MultiPathAbstractRIBSupport(final Class<? extends Routes> cazeClass, final Class<? extends DataObject> containerClass,
        final Class<? extends Route> listClass, final Class<? extends AddressFamily> addressFamilyClass,
        final Class<? extends SubsequentAddressFamily> safiClass, final String routeKeyNaming, final QName destinationQname) {
        super(cazeClass, containerClass, listClass, addressFamilyClass, safiClass, destinationQname);
        this.routeKeyQname = QName.create(routeQName(), routeKeyNaming).intern();
        this.pathIdQname = QName.create(routeQName(), "path-id").intern();
        this.pathIdNid = new NodeIdentifier(this.pathIdQname);
    }

    protected final NodeIdentifier routePathIdNid() {
        return this.pathIdNid;
    }

    protected final QName pathIdQName() {
        return this.pathIdQname;
    }

    public final QName routeKeyQName() {
        return this.routeKeyQname;
    }

    @Override
    public final Long extractPathId(final NormalizedNode<?, ?> data) {
        final Long pathId = PathIdUtil.extractPathId(data, this.routePathIdNid());
        if(pathId == null) {
            return PathIdUtil.NON_PATH_ID;
        }
        return pathId;
    }

    @Nonnull
    @Override
    public final PathArgument getRouteIdAddPath(final long pathId, final PathArgument routeId) {
        return PathIdUtil.createNidKey(pathId, routeId, routeQName(), pathIdQName(), routeKeyQName());
    }

    @Override
    public final PathArgument createRouteKeyPathArgument(final PathArgument routeKey) {
        final ImmutableMap<QName, Object> keyValues = ImmutableMap.of(routeKeyQName(), PathIdUtil.getObjectKey(routeKey, routeKeyQName()));
        return new NodeIdentifierWithPredicates(routeQName(), keyValues);
    }

}
