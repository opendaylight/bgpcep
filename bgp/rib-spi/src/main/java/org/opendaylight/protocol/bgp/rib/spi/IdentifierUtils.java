/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

public final class IdentifierUtils {
    private static final Predicate<PathArgument> IS_PEER = input -> input
            instanceof NodeIdentifierWithPredicates && Peer.QNAME.equals(input.getNodeType());
    private static final Predicate<PathArgument> IS_TABLES = input -> input
            instanceof NodeIdentifierWithPredicates && Tables.QNAME.equals(input.getNodeType());
    private static final QName PEER_ID = QName.create(Peer.QNAME, "peer-id").intern();

    private IdentifierUtils() {
        throw new UnsupportedOperationException();
    }

    // FIXME: implement as id.firstIdentifierOf(IS_PEER), null indicating not found
    private static NodeIdentifierWithPredicates firstKeyOf(final YangInstanceIdentifier id,
            final Predicate<PathArgument> match) {
        final PathArgument ret = id.getPathArguments().stream().filter(match::apply).findFirst().get();
        Preconditions.checkArgument(ret instanceof NodeIdentifierWithPredicates,
                "Non-key peer identifier %s", ret);
        return (NodeIdentifierWithPredicates) ret;
    }

    private static YangInstanceIdentifier firstIdentifierOf(final YangInstanceIdentifier id,
            final Predicate<PathArgument> match) {
        final int idx = Iterables.indexOf(id.getPathArguments(), match);
        Preconditions.checkArgument(idx != -1, "Failed to find %s in %s", match, id);
        // we want the element at index idx to be included in the list
        return YangInstanceIdentifier.create(Iterables.limit(id.getPathArguments(), idx + 1));
    }

    public static YangInstanceIdentifier peerPath(final YangInstanceIdentifier id) {
        return firstIdentifierOf(id, IS_PEER);
    }

    public static NodeIdentifierWithPredicates peerKey(final YangInstanceIdentifier id) {
        return firstKeyOf(id, IS_PEER);
    }

    public static PeerId peerId(final NodeIdentifierWithPredicates peerKey) {
        // We could use a codec, but this is simple enough
        return new PeerId((String) peerKey.getKeyValues().get(PEER_ID));
    }

    public static PeerId peerKeyToPeerId(final YangInstanceIdentifier id) {
        return peerId(peerKey(id));
    }


    static NodeIdentifierWithPredicates tableKey(final YangInstanceIdentifier id) {
        return firstKeyOf(id, IS_TABLES);
    }

    public static NodeIdentifierWithPredicates domPeerId(final PeerId peer) {
        return new NodeIdentifierWithPredicates(Peer.QNAME, PEER_ID, peer.getValue());
    }
}
