/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;

public final class MultiPathSupportUtil {
    private MultiPathSupportUtil() {
        // Hidden on purpose
    }

    /**
     * Check is AFI/SAFI is supported by {@link MultiPathSupport} service.
     * @param constraints Peer specific constraint.
     * @param afiSafi Required AFI/SAFI
     * @return True if AFI/SAFI is supported.
     */
    public static boolean isTableTypeSupported(final @Nullable PeerSpecificParserConstraint constraints,
            final @NonNull BgpTableType afiSafi) {
        requireNonNull(afiSafi);
        if (constraints != null) {
            final Optional<MultiPathSupport> peerConstraint = constraints.getPeerConstraint(MultiPathSupport.class);
            return peerConstraint.isPresent() && peerConstraint.get().isTableTypeSupported(afiSafi);
        }
        return false;
    }
}
