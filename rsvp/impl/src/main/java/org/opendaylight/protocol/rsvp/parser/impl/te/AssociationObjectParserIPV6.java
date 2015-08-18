/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.te;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;

public final class AssociationObjectParserIPV6 extends AbstractAssociationParser {
    @Override
    protected IpAddress parseAssociationIpAddress(final ByteBuf byteBuf) {
        return new IpAddress(Ipv6Util.addressForByteBuf(byteBuf));
    }
}
