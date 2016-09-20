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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpAddPathTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;

public final class MultiPathSupportUtil {

    private MultiPathSupportUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Check is AFI/SAFI is supported by {@link MultiPathSupport} service.
     *
     * @param constraints Peer specific constraint.
     * @param afiSafi Required AFI/SAFI
     * @return True if AFI/SAFI is supported.
     */
    public static boolean isTableTypeSupported(@Nullable final PeerSpecificParserConstraint constraints, @Nonnull final BgpTableType afiSafi) {
        Preconditions.checkNotNull(afiSafi);
        if (constraints != null) {
            final Optional<MultiPathSupport> peerConstraint = constraints.getPeerConstraint(MultiPathSupport.class);
            return peerConstraint.isPresent() && peerConstraint.get().isTableTypeSupported(afiSafi);
        }
        return false;
    }


    public static Map<TablesKey, SendReceive> mapTableTypesFamilies(final List<AddressFamilies> addPathTablesType) {
        return ImmutableMap.copyOf(addPathTablesType.stream().collect(Collectors.toMap(af -> new TablesKey(af.getAfi(), af.getSafi()),
            BgpAddPathTableType::getSendReceive)));
    }
}
