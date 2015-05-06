package org.opendaylight.protocol.bgp.parser.impl.message.open;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapability;

/**
 * Created by cgasparini on 7.5.2015.
 */
public final class As4CapabilitySerializer implements CapabilitySerializer {

    public As4CapabilitySerializer() {

    }

    private static ByteBuf putAS4BytesParameterValue(final As4BytesCapability param) {
        return Unpooled.copyInt(param.getAsNumber().getValue().intValue());
    }



    @Override
    public void serializeCapability(CParameters capability, ByteBuf byteAggregator) {
        Preconditions.checkArgument(capability.getAs4BytesCapability() != null, "As4BytesCapability is null");
        CapabilityUtil.formatCapability(As4CapabilityHandler.CODE, putAS4BytesParameterValue(capability.getAs4BytesCapability()), byteAggregator);
    }
}
