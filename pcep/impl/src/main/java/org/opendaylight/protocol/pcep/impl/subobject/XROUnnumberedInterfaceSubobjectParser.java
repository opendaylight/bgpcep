/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.impl.object.XROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.UnnumberedSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;

/**
 * Parser for {@link UnnumberedCase}
 */
public class XROUnnumberedInterfaceSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

    public static final int TYPE = 4;

    private static final int ATTRIBUTE_LENGTH = 1;
    private static final int ROUTER_ID_NUMBER_LENGTH = 4;
    private static final int INTERFACE_ID_NUMBER_LENGTH = 4;

    private static final int ATTRIBUTE_OFFSET = 1;
    private static final int ROUTER_ID_NUMBER_OFFSET = ATTRIBUTE_OFFSET + ATTRIBUTE_LENGTH;
    private static final int INTERFACE_ID_NUMBER_OFFSET = ROUTER_ID_NUMBER_OFFSET + ROUTER_ID_NUMBER_LENGTH;

    private static final int CONTENT_LENGTH = INTERFACE_ID_NUMBER_OFFSET + INTERFACE_ID_NUMBER_LENGTH;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean mandatory) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: "
                    + CONTENT_LENGTH + ".");
        }
        buffer.readerIndex(buffer.readerIndex() + ATTRIBUTE_OFFSET);
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setMandatory(mandatory);
        builder.setAttribute(Attribute.forValue(UnsignedBytes.toInt(buffer.readByte())));
        final UnnumberedBuilder ubuilder = new UnnumberedBuilder();
        ubuilder.setRouterId(buffer.readUnsignedInt());
        ubuilder.setInterfaceId(buffer.readUnsignedInt());
        builder.setSubobjectType(new UnnumberedCaseBuilder().setUnnumbered(ubuilder.build()).build());
        return builder.build();
    }

    @Override
    public byte[] serializeSubobject(final Subobject subobject) {
        if (!(subobject.getSubobjectType() instanceof UnnumberedCase)) {
            throw new IllegalArgumentException("Unknown PCEPXROSubobject instance. Passed " + subobject.getSubobjectType().getClass()
                    + ". Needed UnnumberedCase.");
        }

        final byte[] retBytes = new byte[CONTENT_LENGTH];
        final UnnumberedSubobject specObj = ((UnnumberedCase) subobject.getSubobjectType()).getUnnumbered();

        retBytes[ATTRIBUTE_OFFSET] = UnsignedBytes.checkedCast(subobject.getAttribute().getIntValue());
        ByteArray.copyWhole(ByteArray.longToBytes(specObj.getRouterId(), ROUTER_ID_NUMBER_LENGTH), retBytes, ROUTER_ID_NUMBER_OFFSET);
        System.arraycopy(ByteArray.longToBytes(specObj.getInterfaceId(), INTERFACE_ID_NUMBER_LENGTH), 0, retBytes,
                INTERFACE_ID_NUMBER_OFFSET, INTERFACE_ID_NUMBER_LENGTH);
        return XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), retBytes);
    }
}
