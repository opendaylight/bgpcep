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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;

public final class MultiPathSupportUtil {

    private MultiPathSupportUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Check is AFI/SAFI is supported by {@link MultiPathSupport} service.
     * @param constraints Peer specific constraint.
     * @param afiSafi Required AFI/SAFI
     * @return True if AFI/SAFI is supported.
     */
    public static boolean isTableTypeSupported(@Nullable final PeerSpecificParserConstraint constraints, @Nonnull final BgpTableType afiSafi) {
        requireNonNull(afiSafi);
        if (constraints != null) {
            final Optional<MultiPathSupport> peerConstraint = constraints.getPeerConstraint(MultiPathSupport.class);
            return peerConstraint.isPresent() && peerConstraint.get().isTableTypeSupported(afiSafi);
        }
        return false;

    }

}
