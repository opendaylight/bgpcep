/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject.ero;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.impl.te.ProtectionCommonParser;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.DynamicControlProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.subobject.ProtectionSubobject;

/**
 * Parser for {@link DynamicControlProtectionCase}
 */
public class SERODynamicProtectionSubobjectParser extends ProtectionCommonParser implements EROSubobjectSerializer {

    public static final int TYPE = 37;
    public static final Short CTYPE = 2;

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof DynamicControlProtectionCase, "Unknown " +
            "subobject instance. Passed %s. Needed UnnumberedCase.", subobject.getSubobjectType().getClass());
        final ProtectionSubobject protObj = ((DynamicControlProtectionCase) subobject.getSubobjectType())
            .getDynamicControlProtection().getProtectionSubobject();
        final ByteBuf body = Unpooled.buffer();
        serializeBody(CTYPE, protObj, body);
        EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), body, buffer);
    }

}