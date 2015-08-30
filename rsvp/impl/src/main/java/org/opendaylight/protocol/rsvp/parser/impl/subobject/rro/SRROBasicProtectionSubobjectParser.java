/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject.rro;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.impl.te.ProtectionCommonParser;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.subobject.ProtectionSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.BasicProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.BasicProtectionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.DynamicControlProtectionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.basic.protection._case.BasicProtectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.dynamic.control.protection._case.DynamicControlProtectionBuilder;

/**
 * Parser for {@link BasicProtectionCase}
 */
public class SRROBasicProtectionSubobjectParser extends ProtectionCommonParser implements RROSubobjectParser, RROSubobjectSerializer {

    public static final int TYPE = 37;
    public static final Short CTYPE = 1;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final SubobjectContainerBuilder builder = new SubobjectContainerBuilder();
        //skip reserved
        buffer.readByte();
        final short cType = buffer.readUnsignedByte();
        final ProtectionSubobject prot = parseCommonProtectionBody(cType, buffer);
        if (cType == CTYPE) {
            builder.setSubobjectType(new BasicProtectionCaseBuilder().setBasicProtection(new BasicProtectionBuilder()
                .setProtectionSubobject(prot).build()).build());
        } else if (cType == SRRODynamicProtectionSubobjectParser.CTYPE) {
            builder.setSubobjectType(new DynamicControlProtectionCaseBuilder().setDynamicControlProtection(new
                DynamicControlProtectionBuilder().setProtectionSubobject(prot).build()).build());
        }
        return builder.build();
    }


    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof BasicProtectionCase, "Unknown subobject instance. Passed %s. Needed UnnumberedCase.", subobject.getSubobjectType().getClass());
        final ProtectionSubobject protObj = ((BasicProtectionCase) subobject.getSubobjectType()).getBasicProtection()
            .getProtectionSubobject();
        final ByteBuf body = Unpooled.buffer();
        serializeBody(CTYPE, protObj, body);
        RROSubobjectUtil.formatSubobject(TYPE, body, buffer);
    }
}
