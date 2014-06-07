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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.SrlgCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.SrlgCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.srlg._case.SrlgBuilder;

/**
 * Parser for {@link SrlgCase}
 */
public class XROSRLGSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

    public static final int TYPE = 34;

    private static final int SRLG_ID_NUMBER_LENGTH = 4;
    private static final int ATTRIBUTE_LENGTH = 1;

    private static final int SRLG_ID_NUMBER_OFFSET = 0;
    private static final int ATTRIBUTE_OFFSET = SRLG_ID_NUMBER_OFFSET + SRLG_ID_NUMBER_LENGTH;

    private static final int CONTENT_LENGTH = SRLG_ID_NUMBER_LENGTH + ATTRIBUTE_LENGTH;

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean mandatory) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: "
                    + CONTENT_LENGTH + ".");
        }

        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setMandatory(mandatory);
        builder.setAttribute(Attribute.Srlg);
        builder.setSubobjectType(new SrlgCaseBuilder().setSrlg(new SrlgBuilder().setSrlgId(new SrlgId(buffer.readUnsignedInt())).build()).build());
        return builder.build();
    }

    @Override
    public byte[] serializeSubobject(final Subobject subobject) {
        if (!(subobject.getSubobjectType() instanceof SrlgCase)) {
            throw new IllegalArgumentException("Unknown PCEPXROSubobject instance. Passed " + subobject.getSubobjectType().getClass()
                    + ". Needed SrlgCase.");
        }

        byte[] retBytes;
        retBytes = new byte[CONTENT_LENGTH];
        final SrlgSubobject specObj = ((SrlgCase) subobject.getSubobjectType()).getSrlg();

        ByteArray.copyWhole(ByteArray.longToBytes(specObj.getSrlgId().getValue(), SRLG_ID_NUMBER_LENGTH), retBytes, SRLG_ID_NUMBER_OFFSET);
        retBytes[ATTRIBUTE_OFFSET] = UnsignedBytes.checkedCast(subobject.getAttribute().getIntValue());

        return XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), retBytes);
    }
}
