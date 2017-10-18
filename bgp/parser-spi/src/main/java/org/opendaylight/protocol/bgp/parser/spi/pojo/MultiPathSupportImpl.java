/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamilies;

public final class MultiPathSupportImpl implements MultiPathSupport {

    private final Set<BgpTableType> supportedTables;

    private MultiPathSupportImpl(final Set<BgpTableType> supportedTables) {
        this.supportedTables = ImmutableSet.copyOf(supportedTables);
    }

    /**
     * Creates instance of {@link MultiPathSupport} holder to be used
     * as a parser constraint, hence only "send" add-path capabilities are
     * taken into the account.
     *
     * @param addPathCapabilities The remote add-path capabilities list.
     * @return MultiPathSupport instance.
     */
    public static MultiPathSupport createParserMultiPathSupport(@Nonnull final List<AddressFamilies> addPathCapabilities) {
        requireNonNull(addPathCapabilities);
        final Set<BgpTableType> support = addPathCapabilities
            .stream()
            .filter(e -> e.getSendReceive() == SendReceive.Both || e.getSendReceive() == SendReceive.Send)
            .map(e -> new BgpTableTypeImpl(e.getAfi(), e.getSafi()))
            .collect(Collectors.toSet());
        return new MultiPathSupportImpl(support);
    }

    @Override
    public boolean isTableTypeSupported(final BgpTableType tableType) {
        return this.supportedTables.contains(tableType);
    }

}
