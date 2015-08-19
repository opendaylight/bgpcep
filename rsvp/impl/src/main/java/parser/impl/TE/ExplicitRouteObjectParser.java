/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package parser.impl.TE;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.explicit.route.object.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.explicit.route.object.ExplicitRouteObjectBuilder;
import parser.impl.subobject.ERO.EROSubobjectListParser;
import parser.spi.EROSubobjectRegistry;
import parser.spi.RSVPParsingException;

public final class ExplicitRouteObjectParser extends EROSubobjectListParser {
    public static final short CLASS_NUM = 20;
    public static final short CTYPE = 1;

    public ExplicitRouteObjectParser(final EROSubobjectRegistry subobjReg) {
        super(subobjReg);
    }

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final ExplicitRouteObjectBuilder exo = new ExplicitRouteObjectBuilder();
        exo.setCType(CTYPE);
        return exo.setSubobjectContainer(parseList(byteBuf)).build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof ExplicitRouteObject, "ExplicitRouteObject is mandatory.");
        final ExplicitRouteObject explicitObject = (ExplicitRouteObject) teLspObject;
        final ByteBuf bufferAux = Unpooled.buffer();
        serializeList(explicitObject.getSubobjectContainer(), bufferAux);
        serializeAttributeHeader(bufferAux.readableBytes(), CLASS_NUM, CTYPE, output);
        output.writeBytes(bufferAux);
    }
}
