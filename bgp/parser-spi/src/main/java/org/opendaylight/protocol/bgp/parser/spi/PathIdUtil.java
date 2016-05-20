/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;

public final class PathIdUtil {
    public static final long NON_PATH_ID = 0;

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
     * @param data Data containing the path Id
     * @param pathNii Path Id NodeIdentifier specific per each Rib support
     * @return The path identifier from data change, in case its not provided or supported return null
     */
    @Nullable
    public static Long extractPathId(final NormalizedNode<?, ?> data, final NodeIdentifier pathNii) {
        final NormalizedNode<?, ?> pathId = NormalizedNodes.findNode(data, pathNii).orNull();
        if (pathId == null) {
            return null;
        }
        return (Long) pathId.getValue();
    }

    /**
     * Create a Add Path PathArgument Key(prefix+pathId)
     *
     * @param pathId Path Id value
     * @param routeId Route Id value
     * @param routeQname route QName provided per each RibSupport
     * @param pathidQname Path Id QName provided per each RibSupport
     * @param prefixQname Prefix QName provided per each RibSupport
     * @return
     */
    public static PathArgument createNiiKey(final long pathId, final PathArgument routeId, final QName routeQname, final QName pathidQname,
        final QName prefixQname) {
        final String prefix = (String) (((NodeIdentifierWithPredicates) routeId).getKeyValues()).get(prefixQname);
        return createNodeIdentifierWithPredicates(routeQname, pathidQname, pathId, prefixQname, prefix);
    }

    private static NodeIdentifierWithPredicates createNodeIdentifierWithPredicates(final QName routeQname, final QName pathidQname, final Object pathId,
        final QName prefixQname, final Object prefix) {
        final ImmutableMap<QName, Object> keyValues = ImmutableMap.of(pathidQname, pathId, prefixQname, prefix);
        return new NodeIdentifierWithPredicates(routeQname, keyValues);
    }

    /**
     * Build Path Id
     *
     * @param routesCont route container
     * @param pathIdNii path Id node Identifier
     * @return PathId or null in case is not the container
     */
    public static PathId buildPathId(final DataContainerNode<? extends PathArgument> routesCont, final NodeIdentifier pathIdNii) {
        final Long pathIdVal = PathIdUtil.extractPathId(routesCont, pathIdNii);
        return pathIdVal == null ? null : new PathId(pathIdVal);
    }

    public static NodeIdentifierWithPredicates createNiiKey(final UnkeyedListEntryNode prefixes, final QName routeQname,
        final NodeIdentifier prefixNii, final NodeIdentifier pathIdNii) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybePrefixLeaf = prefixes.getChild(prefixNii);
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybePathIdLeaf = prefixes.getChild(pathIdNii);
        Preconditions.checkState(maybePrefixLeaf.isPresent());

        // FIXME: a cache here would mean we instantiate the same identifier for each route
        //        making comparison quicker.
        final Object prefixValue = (maybePrefixLeaf.get()).getValue();
        Object pathId = 0L;
        if(maybePathIdLeaf.isPresent()) {
            pathId = (maybePathIdLeaf.get()).getValue();
        }
        return createNodeIdentifierWithPredicates(routeQname, pathIdNii.getNodeType(), pathId, prefixNii.getNodeType(), prefixValue);
    }
}
