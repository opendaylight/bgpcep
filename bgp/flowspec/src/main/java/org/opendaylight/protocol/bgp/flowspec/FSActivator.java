/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.impl.message.update.ExtendedCommunitiesAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;

public final class FSActivator extends AbstractBGPExtensionProviderActivator {

    private static final int FLOWSPEC_SAFI = 133;

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        regs.add(context.registerSubsequentAddressFamily(FlowspecSubsequentAddressFamily.class, FLOWSPEC_SAFI));

        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class,
                new FSNlriParser()));
        regs.add(context.registerNlriSerializer(PathAttributes.class, new FSNlriParser()));

        final ExtendedCommunitiesAttributeParser extendedCommunitiesAttributeParser = new FSExtendedCommunitiesAttributeParser(context.getReferenceCache());
        regs.add(context.registerAttributeSerializer(ExtendedCommunities.class, extendedCommunitiesAttributeParser));
        regs.add(context.registerAttributeParser(ExtendedCommunitiesAttributeParser.TYPE, extendedCommunitiesAttributeParser));

        return regs;
    }
}
