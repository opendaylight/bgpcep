/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.CommonObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcc.id.req.object.PccIdReq;

public abstract class AbstractPccIdReqObjectParser extends CommonObjectParser implements ObjectSerializer {
    private static final int CLASS = 20;

    public AbstractPccIdReqObjectParser(final int objectType) {
        super(CLASS, objectType);
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof PccIdReq, "Wrong instance of PCEPObject. Passed %s. Needed PccIdReqObject.",
            object.getClass());
        final PccIdReq pccIdReq = (PccIdReq) object;
        if (pccIdReq.getIpAddress().getIpv4AddressNoZone() != null) {
            final ByteBuf body = Unpooled.buffer(Ipv4Util.IP4_LENGTH);
            Ipv4Util.writeIpv4Address(pccIdReq.getIpAddress().getIpv4AddressNoZone(), body);
            ObjectUtil.formatSubobject(getObjectType(), getObjectClass(), object.getProcessingRule(),
                object.getIgnore(), body, buffer);
        } else if (pccIdReq.getIpAddress().getIpv6AddressNoZone() != null) {
            final ByteBuf body = Unpooled.buffer(Ipv6Util.IPV6_LENGTH);
            Ipv6Util.writeIpv6Address(pccIdReq.getIpAddress().getIpv6AddressNoZone(), body);
            ObjectUtil.formatSubobject(getObjectType(), getObjectClass(), object.getProcessingRule(),
                object.getIgnore(), body, buffer);
        }
    }
}
