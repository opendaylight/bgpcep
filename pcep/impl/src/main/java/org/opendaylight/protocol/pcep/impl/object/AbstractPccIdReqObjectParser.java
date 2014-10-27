/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ip.address.ip.address.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ip.address.ip.address.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcc.id.req.object.PccIdReq;

public abstract class AbstractPccIdReqObjectParser implements ObjectSerializer, ObjectParser {
    public static final int CLASS = 20;

    public static final int IPV4_TYPE = 1;
    public static final int IPV6_TYPE = 2;

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof PccIdReq, "Wrong instance of PCEPObject. Passed %s. Needed PccIdReqObject.", object.getClass());
        final PccIdReq pccIdReq = (PccIdReq) object;
        if (pccIdReq.getIpAddress() instanceof Ipv4Address) {
            final ByteBuf body = Unpooled.buffer(Ipv4Util.IP4_LENGTH);
            ByteBufWriteUtil.writeIpv4Address(((Ipv4Address) pccIdReq.getIpAddress()).getIpv4Address(), body);
            ObjectUtil.formatSubobject(IPV4_TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
        } else if (pccIdReq.getIpAddress() instanceof Ipv6Address) {
            final ByteBuf body = Unpooled.buffer(Ipv6Util.IPV6_LENGTH);
            ByteBufWriteUtil.writeIpv6Address(((Ipv6Address) pccIdReq.getIpAddress()).getIpv6Address(), body);
            ObjectUtil.formatSubobject(IPV6_TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
        }
    }
}
