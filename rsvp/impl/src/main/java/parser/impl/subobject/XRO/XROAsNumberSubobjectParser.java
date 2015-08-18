/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package parser.impl.subobject.XRO;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.spi.XROSubobjectUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.exclude.route.object.exclude.route.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.exclude.route.object.exclude.route.object.SubobjectContainerBuilder;
import parser.impl.subobject.AsNumberCaseParser;
import parser.spi.RSVPParsingException;
import parser.spi.XROSubobjectParser;
import parser.spi.XROSubobjectSerializer;

/**
 * Parser for {@link AsNumberCase}
 */
public class XROAsNumberSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {
    public static final int TYPE = 32;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer, final boolean mandatory) throws RSVPParsingException {
        return new SubobjectContainerBuilder().setMandatory(mandatory).setSubobjectType(AsNumberCaseParser.
            parseSubobject(buffer)).build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof AsNumberCase, "Unknown subobject instance. Passed %s. Needed AsNumberCase.", subobject.getSubobjectType().getClass());
        final ByteBuf body = AsNumberCaseParser.serializeSubobject((AsNumberCase) subobject.getSubobjectType());
        XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), body, buffer);
    }
}
