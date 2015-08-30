/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.subobject.xro;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.CommonPathKeyParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainerBuilder;

/**
 * Parser for { PathKey}
 */
public class XROPathKey32SubobjectParser extends CommonPathKeyParser implements XROSubobjectParser,
    XROSubobjectSerializer {
    public static final int TYPE = 64;

    private static final int PCE_ID_F_LENGTH = 4;

    private static final int CONTENT_LENGTH = 2 + PCE_ID_F_LENGTH;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer, final boolean mandatory) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >"
                + CONTENT_LENGTH + ".");
        }

        final SubobjectContainerBuilder builder = new SubobjectContainerBuilder();
        builder.setMandatory(mandatory);
        builder.setSubobjectType(new PathKeyCaseBuilder().setPathKey(parsePathKey(PCE_ID_F_LENGTH, buffer)).build());
        return builder.build();
    }

    @Override
    public void serializeSubobject(final SubobjectContainer subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof PathKeyCase, "Unknown subobject instance. Passed %s. Needed PathKey.", subobject.getSubobjectType().getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.path.key._case.PathKey pk = ((PathKeyCase) subobject.getSubobjectType()).getPathKey();
        final ByteBuf body = serializePathKey(pk);
        if (pk.getPceId().getBinary().length == PCE_ID_F_LENGTH) {
            XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), body, buffer);
        } else if (pk.getPceId().getBinary().length == XROPathKey128SubobjectParser.PCE128_ID_F_LENGTH) {
            XROSubobjectUtil.formatSubobject(XROPathKey128SubobjectParser.TYPE, subobject.isMandatory(), body, buffer);
        }
    }
}
