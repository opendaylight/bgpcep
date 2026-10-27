/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.function.Predicate;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
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
        // Hidden on purpose
    }

    // FIXME: implement as id.firstIdentifierOf(IS_PEER), null indicating not found
    private static @NonNull NodeIdentifierWithPredicates firstKeyOf(final YangInstanceIdentifier id,
            final Predicate<PathArgument> match) {
        final var arg = id.getPathArguments().stream().filter(match).findFirst().orElseThrow();
        if (arg instanceof NodeIdentifierWithPredicates nip) {
            return nip;
        }
        throw new IllegalArgumentException("Non-key peer identifier " + arg);
    }

    private static @NonNull YangInstanceIdentifier firstIdentifierOf(final YangInstanceIdentifier id,
            final Predicate<PathArgument> match) {
        final var args = id.getPathArguments();
        final var it = args.iterator();
        if (it.hasNext()) {
            int idx = 0;
            do {
                if (match.test(it.next())) {
                    // we want the element at index idx to be included in the list
                    return YangInstanceIdentifier.of(args.subList(0, idx + 1));
                }
                idx++;
            } while (it.hasNext());
        }

        throw new IllegalArgumentException("Failed to find %s in %s".formatted(match, id));
    }

    public static @NonNull YangInstanceIdentifier peerPath(final YangInstanceIdentifier id) {
        return firstIdentifierOf(id, IS_PEER);
    }

    public static @NonNull NodeIdentifierWithPredicates peerKey(final YangInstanceIdentifier id) {
        return firstKeyOf(id, IS_PEER);
    }

    @NonNullByDefault
    public static PeerId peerId(final NodeIdentifierWithPredicates peerKey) {
        // We could use a codec, but this is simple enough
        final var value = peerKey.getValue(PEER_ID);
        if (value == null) {
            throw new IllegalArgumentException("Missing %s in %s".formatted(PEER_ID, peerKey));
        }
        return new PeerId((String) value);
    }

    public static @NonNull PeerId peerKeyToPeerId(final YangInstanceIdentifier id) {
        return peerId(peerKey(id));
    }

    static @NonNull NodeIdentifierWithPredicates tableKey(final YangInstanceIdentifier id) {
        return firstKeyOf(id, IS_TABLES);
    }

    @NonNullByDefault
    public static NodeIdentifierWithPredicates domPeerId(final PeerId peer) {
        return NodeIdentifierWithPredicates.of(Peer.QNAME, PEER_ID, peer.getValue());
    }
}
