/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;

public interface NlriRegistry {
    MpReachNlri parseMpReach(final ByteBuf buffer) throws BGPParsingException;
    MpUnreachNlri parseMpUnreach(final ByteBuf buffer) throws BGPParsingException;
    void serializeMpReach(final MpReachNlri mpReachNlri,final ByteBuf byteAggregator);
    void serializeMpUnReach(final MpUnreachNlri mpUnreachNlri,final ByteBuf byteAggregator);
    Iterable<NlriSerializer> getSerializers();
}
