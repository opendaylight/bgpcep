/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.parser.impl.subobject.ERO;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.TE.AbstractProtectionParser;
import org.opendaylight.protocol.bgp.parser.spi.EROSubobjectParser;
import org.opendaylight.protocol.bgp.parser.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.bgp.parser.spi.EROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.explicit.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.explicit.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.explicit.route.subobjects.subobject.type.ProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.explicit.route.subobjects.subobject.type.ProtectionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.protection.subobject.ProtectionSubobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for {@link ProtectionCase}
 */
public class SEROProtectionSubobjectParser extends AbstractProtectionParser implements EROSubobjectParser, EROSubobjectSerializer {

    public static final int TYPE = 37;
    private static final Logger LOG = LoggerFactory.getLogger(SEROProtectionSubobjectParser.class);

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer, final boolean loose) throws BGPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final SubobjectContainerBuilder builder = new SubobjectContainerBuilder();
        builder.setLoose(loose);

        //skip reserved
        buffer.readByte();
        final short cType = buffer.readUnsignedByte();
        switch (cType) {
        case PROTECTION_SUBOBJECT_TYPE_1:
            builder.setSubobjectType(new ProtectionCaseBuilder().setProtectionSubobject
                (parseCommonProtectionBodyType1(buffer)).build());
            break;
        case PROTECTION_SUBOBJECT_TYPE_2:
            builder.setSubobjectType(new ProtectionCaseBuilder().setProtectionSubobject
                (parseCommonProtectionBodyType2(buffer)).build());
            break;
        default:
            LOG.warn("Secondary Record Route Protection Subobject cType {} not supported", cType);
            break;
        }
        return builder.build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof ProtectionCase, "Unknown subobject instance. Passed %s. Needed UnnumberedCase.", subobject.getSubobjectType().getClass());
        final ProtectionSubobject protObj = ((ProtectionCase) subobject.getSubobjectType()).getProtectionSubobject();
        final ByteBuf body = Unpooled.buffer();

        body.writeZero(BYTE_SIZE);
        final Short cType = protObj.getCType();
        writeUnsignedByte(cType, body);
        switch (cType) {
        case PROTECTION_SUBOBJECT_TYPE_1:
            serializeBodyType1(protObj, body);
            break;
        case PROTECTION_SUBOBJECT_TYPE_2:
            serializeBodyType2(protObj, body);
            break;
        }
        EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), body, buffer);
    }

}