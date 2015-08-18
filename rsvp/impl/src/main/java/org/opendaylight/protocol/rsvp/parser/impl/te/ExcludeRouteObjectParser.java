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
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.XROSubobjectListParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.ExcludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.ExcludeRouteObjectBuilder;

public class ExcludeRouteObjectParser extends XROSubobjectListParser {
    public static final short CLASS_NUM = 232;
    public static final short CTYPE = 1;

    public ExcludeRouteObjectParser(final XROSubobjectRegistry subobjReg) {
        super(subobjReg);
    }

    @Override
    final protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final ExcludeRouteObjectBuilder ero = new ExcludeRouteObjectBuilder();
        return ero.setSubobjectContainer(parseList(byteBuf)).build();
    }

    @Override
    final public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof ExcludeRouteObject, "DetourObject is mandatory.");
        final ExcludeRouteObject excludeObject = (ExcludeRouteObject) teLspObject;
        final ByteBuf bufferAux = Unpooled.buffer();
        serializeList(excludeObject.getSubobjectContainer(), bufferAux);
        serializeAttributeHeader(bufferAux.readableBytes(), CLASS_NUM, CTYPE, output);
        output.writeBytes(bufferAux);
    }
}
