/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.load.balancing.object.LoadBalancing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.load.balancing.object.LoadBalancingBuilder;

/**
 * Parser for {@link LoadBalancing}
 */
public class PCEPLoadBalancingObjectParser extends AbstractObjectWithTlvsParser<LoadBalancingBuilder> {

    public static final int CLASS = 14;

    public static final int TYPE = 1;

    private static final int RESERVED = 2;
    private static final int FLAGS_F_LENGTH = 1;

    private static final int SIZE = 8;

    public PCEPLoadBalancingObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public LoadBalancing parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() != SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.readableBytes() + "; Expected: " + SIZE
                    + ".");
        }
        final LoadBalancingBuilder builder = new LoadBalancingBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        bytes.readerIndex(bytes.readerIndex() + RESERVED + FLAGS_F_LENGTH);
        builder.setMaxLsp((short) UnsignedBytes.toInt(bytes.readByte()));
        builder.setMinBandwidth(new Bandwidth(ByteArray.readAllBytes(bytes)));
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof LoadBalancing, String.format("Wrong instance of PCEPObject. Passed %s . Needed LoadBalancingObject.", object.getClass()));
        final LoadBalancing specObj = (LoadBalancing) object;
        final ByteBuf body = Unpooled.buffer(SIZE);
        body.writeZero(RESERVED + FLAGS_F_LENGTH);
        body.writeByte(specObj.getMaxLsp());
        body.writeBytes(specObj.getMinBandwidth().getValue());
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
