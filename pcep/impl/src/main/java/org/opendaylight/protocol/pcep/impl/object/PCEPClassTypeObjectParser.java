/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for {@link ClassType}
 */
public class PCEPClassTypeObjectParser implements ObjectParser, ObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPClassTypeObjectParser.class);

    public static final int CLASS = 22;

    public static final int TYPE = 1;

    /**
     * Length of Class Type field in bits.
     */
    private static final int CT_F_LENGTH = 3;

    /**
     * Reserved field bit length.
     */
    private static final int RESERVED = 29;

    /**
     * Size of the object in bytes.
     */
    private static final int SIZE = (RESERVED + CT_F_LENGTH) / Byte.SIZE;

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (!header.isProcessingRule()) {
            LOG.debug("Processed bit not set on CLASS TYPE OBJECT, ignoring it");
            return null;
        }
        if (bytes.readableBytes() != SIZE) {
            throw new PCEPDeserializerException("Size of byte array doesn't match defined size. Expected: " + SIZE + "; Passed: "
                    + bytes.readableBytes());
        }
        final ClassTypeBuilder builder = new ClassTypeBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());

        final short ct = (short) bytes.readUnsignedInt();
        builder.setClassType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClassType(ct));

        final Object obj = builder.build();
        if (ct < 0 || ct > Byte.SIZE) {
            LOG.debug("Invalid class type {}", ct);
            return new UnknownObject(PCEPErrors.INVALID_CT, obj);
        }
        return obj;
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof ClassType, "Wrong instance of PCEPObject. Passed %s. Needed ClassTypeObject.", object.getClass());
        final ByteBuf body = Unpooled.buffer(SIZE);
        body.writeZero(SIZE -1);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClassType classType = ((ClassType) object).getClassType();
        Preconditions.checkArgument(classType != null, "ClassType is mandatory.");
        writeUnsignedByte(classType.getValue(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
