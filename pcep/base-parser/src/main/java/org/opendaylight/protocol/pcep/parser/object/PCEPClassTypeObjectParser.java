/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.classtype.object.ClassTypeBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for {@link ClassType}.
 */
public final class PCEPClassTypeObjectParser extends CommonObjectParser implements ObjectSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPClassTypeObjectParser.class);
    private static final int CLASS = 22;
    private static final int TYPE = 1;

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

    public PCEPClassTypeObjectParser() {
        super(CLASS, TYPE);
    }

    @Override
    public Object parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Cannot be null or empty.");
        if (!header.isProcessingRule()) {
            LOG.debug("Processed bit not set on CLASS TYPE OBJECT, ignoring it");
            return null;
        }
        if (bytes.readableBytes() != SIZE) {
            throw new PCEPDeserializerException("Size of byte array doesn't match defined size. Expected: " + SIZE
                + "; Passed: " + bytes.readableBytes());
        }
        final short ct = (short) bytes.readUnsignedInt();
        final ClassTypeBuilder builder = new ClassTypeBuilder()
                .setIgnore(header.isIgnore())
                .setProcessingRule(header.isProcessingRule())
                .setClassType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                        .ClassType(Uint8.valueOf(ct)));

        if (ct < 0 || ct > Byte.SIZE) {
            LOG.debug("Invalid class type {}", ct);
            return new UnknownObject(PCEPErrors.INVALID_CT, builder.build());
        }
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof ClassType, "Wrong instance of PCEPObject. Passed %s. Needed ClassTypeObject.",
            object.getClass());
        final ByteBuf body = Unpooled.buffer(SIZE);
        body.writeZero(SIZE - 1);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
            .ClassType classType = ((ClassType) object).getClassType();
        checkArgument(classType != null, "ClassType is mandatory.");
        ByteBufUtils.write(body, classType.getValue());
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
