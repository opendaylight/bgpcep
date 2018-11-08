/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.parser.object;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcc.id.req.object.PccIdReqBuilder;

/**
 * Parser for {@link org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcc.id.req.object.PccIdReq} with IPv4 address
 * @see <a href="https://tools.ietf.org/html/rfc5886#section-4.2">PCC-ID-REQ Object</a>
 */
public final class PCEPPccIdReqIPv4ObjectParser extends AbstractPccIdReqObjectParser {
    private static final int IPV4_TYPE = 1;

    public PCEPPccIdReqIPv4ObjectParser() {
        super(IPV4_TYPE);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf buffer) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(),
            "Array of bytes is mandatory. Can't be null or empty.");
        final PccIdReqBuilder builder = new PccIdReqBuilder();
        builder.setIpAddress(new IpAddressNoZone(Ipv4Util.noZoneAddressForByteBuf(buffer)));
        return builder.build();
    }
}
