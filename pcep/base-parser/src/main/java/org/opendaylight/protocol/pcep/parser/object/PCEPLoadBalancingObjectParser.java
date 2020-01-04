/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.CommonObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.load.balancing.object.LoadBalancing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.load.balancing.object.LoadBalancingBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link LoadBalancing}.
 */
public final class PCEPLoadBalancingObjectParser extends CommonObjectParser implements ObjectSerializer {
    private static final int CLASS = 14;
    private static final int TYPE = 1;
    private static final int RESERVED = 2;
    private static final int FLAGS_F_LENGTH = 1;
    private static final int SIZE = 8;

    public PCEPLoadBalancingObjectParser() {
        super(CLASS, TYPE);
    }

    @Override
    public LoadBalancing parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(),
            "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() != SIZE) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: "
                + bytes.readableBytes() + "; Expected: " + SIZE + ".");
        }
        bytes.skipBytes(RESERVED + FLAGS_F_LENGTH);
        final LoadBalancingBuilder builder = new LoadBalancingBuilder()
                .setIgnore(header.isIgnore())
                .setProcessingRule(header.isProcessingRule())
                .setMaxLsp(ByteBufUtils.readUint8(bytes))
                .setMinBandwidth(new Bandwidth(ByteArray.readAllBytes(bytes)));
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof LoadBalancing,
            "Wrong instance of PCEPObject. Passed %s. Needed LoadBalancingObject.",
            object.getClass());
        final LoadBalancing specObj = (LoadBalancing) object;
        final ByteBuf body = Unpooled.buffer(SIZE);
        body.writeZero(RESERVED + FLAGS_F_LENGTH);
        writeUnsignedByte(specObj.getMaxLsp(), body);
        writeFloat32(specObj.getMinBandwidth(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
