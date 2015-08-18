/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package parser.impl.subobject.RRO;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.label._case.Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.label._case.LabelBuilder;
import parser.spi.LabelRegistry;
import parser.spi.RROSubobjectParser;
import parser.spi.RROSubobjectSerializer;
import parser.spi.RSVPParsingException;

public class RROLabelSubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

    public static final int TYPE = 3;

    public static final int FLAGS_SIZE = 8;

    public static final int C_TYPE_F_LENGTH = 1;

    public static final int C_TYPE_F_OFFSET = FLAGS_SIZE / Byte.SIZE;

    public static final int HEADER_LENGTH = C_TYPE_F_OFFSET + C_TYPE_F_LENGTH;

    public static final int U_FLAG_OFFSET = 0;
    public static final int G_FLAG_OFFSET = 7;

    private final LabelRegistry registry;

    public RROLabelSubobjectParser(final LabelRegistry labelReg) {
        this.registry = Preconditions.checkNotNull(labelReg);
    }

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() < HEADER_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >"
                + HEADER_LENGTH + ".");
        }
        final BitArray reserved = BitArray.valueOf(buffer, FLAGS_SIZE);

        final short cType = buffer.readUnsignedByte();

        final LabelType labelType = this.registry.parseLabel(cType, buffer.slice());
        if (labelType == null) {
            throw new RSVPParsingException("Unknown C-TYPE for ero label subobject. Passed: " + cType);
        }
        final LabelBuilder builder = new LabelBuilder();
        builder.setUniDirectional(reserved.get(U_FLAG_OFFSET));
        builder.setGlobal(reserved.get(G_FLAG_OFFSET));
        builder.setLabelType(labelType);
        return new SubobjectContainerBuilder().setSubobjectType(new LabelCaseBuilder().setLabel(builder.build()).build()).build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkNotNull(subobject.getSubobjectType(), "Subobject type cannot be empty.");
        final Label label = ((LabelCase) subobject.getSubobjectType()).getLabel();
        final ByteBuf body = Unpooled.buffer();
        this.registry.serializeLabel(label.isUniDirectional(), label.isGlobal(), label.getLabelType(), body);
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }
}
