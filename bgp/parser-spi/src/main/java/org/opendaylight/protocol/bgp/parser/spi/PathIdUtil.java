/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yangtools.util.ImmutableOffsetMapTemplate;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

public final class PathIdUtil {
    public static final Uint32 NON_PATH_ID_VALUE = Uint32.ZERO;
    public static final PathId NON_PATH_ID = new PathId(NON_PATH_ID_VALUE);

    private PathIdUtil() {
        // Hidden on purpose
    }

    /**
     * Writes path-id value into the buffer when
     * the path-id is not null or does not equal to zero.
     *
     * @param pathId The NLRI Path Identifier.
     * @param buffer The ByteBuf where path-id value can be written.
     */
    public static void writePathId(final PathId pathId, final ByteBuf buffer) {
        if (pathId != null) {
            final Uint32 value = pathId.getValue();
            if (value.toJava() != 0) {
                ByteBufWriteUtil.writeUnsignedInt(value, buffer);
            }
        }
    }

    /**
     * Reads Path Identifier (4 bytes) from buffer.
     *
     * @param buffer Input buffer.
     * @return Decoded PathId.
     */
    public static PathId readPathId(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(Integer.BYTES));
        return new PathId(ByteBufUtils.readUint32(buffer));
    }

    /**
     * Extract PathId from route change received.
     *
     * @param data    Data containing the path Id
     * @param pathNii Path Id NodeIdentifier specific per each Rib support
     * @return The path identifier from data change
     */
    public static Uint32 extractPathId(final NormalizedNode<?, ?> data, final NodeIdentifier pathNii) {
        return (Uint32) NormalizedNodes.findNode(data, pathNii).map(NormalizedNode::getValue).orElse(null);
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
        final Uint32 pathIdVal = PathIdUtil.extractPathId(routesCont, pathIdNii);
        return pathIdVal == null ? null : new PathId(pathIdVal);
    }

    /**
     * Build Route Key for supporting mp.
     * Key is composed by 2 elements (route-key + path Id).
     *
     * @param routeQName       route QName
     * @param routeKeyTemplate route key template
     * @param routeKeyValue    route key value
     * @param maybePathIdLeaf  path id container, it might me supported or not, in that case default 0 value will
     *                         be assigned
     * @return Route Key Nid
     */
    public static NodeIdentifierWithPredicates createNidKey(final QName routeQName,
            final ImmutableOffsetMapTemplate<QName> routeKeyTemplate, final Object routeKeyValue,
            final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf) {
        // FIXME: a cache here would mean we instantiate the same identifier for each route making comparison quicker.
        final Object pathId = maybePathIdLeaf.isPresent() ? maybePathIdLeaf.get().getValue() : NON_PATH_ID_VALUE;
        return NodeIdentifierWithPredicates.of(routeQName,
            routeKeyTemplate.instantiateWithValues(pathId, routeKeyValue));
    }
}
