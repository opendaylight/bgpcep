/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * @author Kevin Wang
 */
public class VpnIpv6NlriParser implements NlriParser, NlriSerializer {
    @Override
    public void parseNlri(ByteBuf nlri, MpUnreachNlriBuilder builder) throws BGPParsingException {

    }

    @Override
    public void parseNlri(ByteBuf nlri, MpReachNlriBuilder builder) throws BGPParsingException {

    }

    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {

    }
}
