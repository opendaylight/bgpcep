/*
 * Copyright (c) 2016 AT&T Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.open.message.bgp.parameters.optional.capabilities.c.parameters.BgpExtendedMessageCapability;

public final class BgpExtendedMessageCapabilityHandler implements CapabilityParser, CapabilitySerializer {

    public static final int CODE = 6;

    @Override
    public CParameters parseCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        return BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY;
    }

    @Override
    public void serializeCapability(final CParameters capability, final ByteBuf byteAggregator) {
        final BgpExtendedMessageCapability bgpExtendedMessageCapability = capability.getBgpExtendedMessageCapability();
        if (bgpExtendedMessageCapability != null) {
            CapabilityUtil.formatCapability(CODE, Unpooled.EMPTY_BUFFER, byteAggregator);
        }
    }
}