/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.te;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;

public final class AssociationObjectParserIPV4 extends AbstractAssociationParser {
    @Override
    protected IpAddressNoZone parseAssociationIpAddress(final ByteBuf byteBuf) {
        return new IpAddressNoZone(Ipv4Util.noZoneAddressForByteBuf(byteBuf));
    }
}
