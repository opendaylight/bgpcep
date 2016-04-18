/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.Ipv6SidTlv;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>(2);
        final Ipv6BgpPrefixSidParser tlvHandler = new Ipv6BgpPrefixSidParser();
        regs.add(context.registerBgpPrefixSidTlvParser(Ipv6BgpPrefixSidParser.IPV6_SID_TYPE, tlvHandler));
        regs.add(context.registerBgpPrefixSidTlvSerializer(Ipv6SidTlv.class, tlvHandler));
        return regs;
    }

}
