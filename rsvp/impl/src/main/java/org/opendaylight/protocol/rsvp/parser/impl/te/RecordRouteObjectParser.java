/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.te;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.RROSubobjectListParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.object.RecordRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.object.RecordRouteObjectBuilder;

public final class RecordRouteObjectParser extends RROSubobjectListParser {
    public static final short CLASS_NUM = 21;
    public static final short CTYPE = 1;

    public RecordRouteObjectParser(RROSubobjectRegistry subobjReg) {
        super(subobjReg);
    }

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final RecordRouteObjectBuilder rro = new RecordRouteObjectBuilder();
        return rro.setSubobjectContainer(parseList(byteBuf)).build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof RecordRouteObject, "RecordRouteObject is mandatory.");
        final RecordRouteObject recordObject = (RecordRouteObject) teLspObject;
        final ByteBuf bufferAux = Unpooled.buffer();
        serializeList(recordObject.getSubobjectContainer(), bufferAux);
        serializeAttributeHeader(bufferAux.readableBytes(), CLASS_NUM, CTYPE, output);
        output.writeBytes(bufferAux);
    }
}
