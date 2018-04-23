/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

public final class PathIdUtil {
    public static final long NON_PATH_ID_VALUE = 0;
    public static final PathId NON_PATH_ID = new PathId(NON_PATH_ID_VALUE);

    private PathIdUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes path-id value into the buffer when
     * the path-id is not null or does not equal to zero.
     *
     * @param pathId The NLRI Path Identifier.
     * @param buffer The ByteBuf where path-id value can be written.
     */
    public static void writePathId(final PathId pathId, final ByteBuf buffer) {
        if (pathId != null && pathId.getValue() != 0) {
            ByteBufWriteUtil.writeUnsignedInt(pathId.getValue(), buffer);
        }
    }

    /**
     * Reads Path Identifier (4 bytes) from buffer.
     *
     * @param buffer Input buffer.
     * @return Decoded PathId.
     */
    public static PathId readPathId(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(ByteBufWriteUtil.INT_BYTES_LENGTH));
        return new PathId(buffer.readUnsignedInt());
    }

    /**
     * Extract PathId from route change received
     *
     * @param data    Data containing the path Id
     * @param pathNii Path Id NodeIdentifier specific per each Rib support
     * @return The path identifier from data change
     */
    public static Long extractPathId(final NormalizedNode<?, ?> data, final NodeIdentifier pathNii) {
        return (Long) NormalizedNodes.findNode(data, pathNii).map(NormalizedNode::getValue).orElse(null);
    }

    public static NodeIdentifierWithPredicates createNodeIdentifierWithPredicates(final QName routeQname,
            final QName pathidQname, final Object pathId,
            final QName routeKeyQname, final Object keyObject) {
        final ImmutableMap<QName, Object> keyValues = ImmutableMap.of(pathidQname, pathId, routeKeyQname, keyObject);
        return new NodeIdentifierWithPredicates(routeQname, keyValues);
    }

    /**
     * Build Path Id.
     *
     * @param routesCont route container
     * @param pathIdNii  path Id node Identifier
     * @return PathId or null in case is not the container
     */
    public static PathId buildPathId(final DataContainerNode<? extends PathArgument> routesCont,
            final NodeIdentifier pathIdNii) {
        final Long pathIdVal = PathIdUtil.extractPathId(routesCont, pathIdNii);
        return pathIdVal == null ? null : new PathId(pathIdVal);
    }

    /**
     * Build Route Key for supporting mp.
     * Key is composed by 2 elements (route-key + path Id).
     *
     * @param routeQname      route Qname
     * @param routeKeyQname   route key Qname
     * @param pathIdQname     path Id Qname
     * @param routeKeyValue   route key value
     * @param maybePathIdLeaf path id container, it might me supported or not, in that case default 0 value will
     *                        be
     *                        assigned
     * @return Route Key Nid
     */
    public static NodeIdentifierWithPredicates createNidKey(final QName routeQname, final QName routeKeyQname,
            final QName pathIdQname, final Object routeKeyValue,
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf) {
        // FIXME: a cache here would mean we instantiate the same identifier for each route making comparison quicker.
        final Object pathId = maybePathIdLeaf.isPresent() ? maybePathIdLeaf.get().getValue() : NON_PATH_ID_VALUE;
        return createNodeIdentifierWithPredicates(routeQname, pathIdQname, pathId, routeKeyQname, routeKeyValue);
    }
}
