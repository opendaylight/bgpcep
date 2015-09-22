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
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.CommonPathKeyParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCaseBuilder;

/**
 * Parser for { PathKey}
 */
public class EROPathKey128SubobjectParser extends CommonPathKeyParser implements EROSubobjectParser{

    public static final int TYPE = 65;

    protected static final int PCE128_ID_F_LENGTH = 16;

    private static final int CONTENT128_LENGTH = 2 + PCE128_ID_F_LENGTH;

    @Override
    public SubobjectContainer parseSubobject(final ByteBuf buffer, final boolean loose) throws RSVPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() != CONTENT128_LENGTH) {
            throw new RSVPParsingException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >"
                + CONTENT128_LENGTH + ".");
        }
        final SubobjectContainerBuilder builder = new SubobjectContainerBuilder();
        builder.setLoose(loose);
        builder.setSubobjectType(new PathKeyCaseBuilder().setPathKey(parsePathKey(PCE128_ID_F_LENGTH, buffer)).build());
        return builder.build();
    }
}
