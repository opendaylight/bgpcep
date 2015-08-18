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
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.EROSubobjectListParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.primary.path.route.object.PrimaryPathRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.primary.path.route.object.PrimaryPathRouteObjectBuilder;

public final class PrimaryPathRouteObjectParser extends EROSubobjectListParser {
    public static final short CLASS_NUM = 38;
    public static final short CTYPE = 1;

    public PrimaryPathRouteObjectParser(final EROSubobjectRegistry subobjReg) {
        super(subobjReg);
    }

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final PrimaryPathRouteObjectBuilder ppo = new PrimaryPathRouteObjectBuilder();
        return ppo.setSubobjectContainer(parseList(byteBuf)).build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof PrimaryPathRouteObject, "ExplicitRouteObject is mandatory.");
        final PrimaryPathRouteObject explicitObject = (PrimaryPathRouteObject) teLspObject;
        final ByteBuf bufferAux = Unpooled.buffer();
        serializeList(explicitObject.getSubobjectContainer(), bufferAux);
        serializeAttributeHeader(bufferAux.readableBytes(), CLASS_NUM, CTYPE, output);
        output.writeBytes(bufferAux);
    }
}