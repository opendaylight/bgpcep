/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.auto.bandwidth.extension;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeFloat32;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.bandwidth.usage.object.BandwidthUsage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.bandwidth.usage.object.BandwidthUsageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;

public class BandwidthUsageObjectCodec implements ObjectParser, ObjectSerializer {

    public static final int CLASS = 5;

    private static final int BW_LENGTH = 4;

    private final int type;

    public BandwidthUsageObjectCodec(final int type) {
        this.type = type;
    }

    @Override
    public BandwidthUsage parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (bytes.readableBytes() % BW_LENGTH != 0) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.readableBytes() + "; Expected multiple of "
                    + BW_LENGTH + ".");
        }
        final BandwidthUsageBuilder builder = new BandwidthUsageBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        final List<Bandwidth> bwSamples = new ArrayList<>(bytes.readableBytes() / BW_LENGTH);
        while (bytes.isReadable()) {
            bwSamples.add(new Bandwidth(ByteArray.readBytes(bytes, BW_LENGTH)));
        }
        builder.setBwSample(bwSamples);
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof BandwidthUsage, "Wrong instance of PCEPObject. Passed %s. Needed BandwidthUsage.", object.getClass());
        final List<Bandwidth> bwSample = ((BandwidthUsage) object).getBwSample();
        final ByteBuf body = Unpooled.buffer(bwSample.size() * BW_LENGTH);
        for (final Bandwidth bw : bwSample) {
            writeFloat32(bw, body);
        }
        ObjectUtil.formatSubobject(getType(), CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }

    public int getType() {
        return this.type;
    }
}
