/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.parser.impl.TE;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.session.attribute.object.SessionAttributeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.session.attribute.object.SessionAttributeObjectBuilder;

public final class SessionAttributeObjectType1Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 207;
    public static final short CTYPE = 1;
    private static final Short BODY_SIZE_C1 = 16;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        final SessionAttributeObjectBuilder builder = new SessionAttributeObjectBuilder();
        builder.setCType(CTYPE);
        builder.setIncludeAny(new AttributeFilter(byteBuf.readUnsignedInt()));
        builder.setExcludeAny(new AttributeFilter(byteBuf.readUnsignedInt()));
        builder.setIncludeAll(new AttributeFilter(byteBuf.readUnsignedInt()));
        SessionAttributeObjectType7Parser.parseSessionAttributeObject(builder, byteBuf);
        return builder.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof SessionAttributeObject, "SessionAttributeObject is mandatory.");

        final SessionAttributeObject sessionObject = (SessionAttributeObject) teLspObject;

        serializeAttributeHeader(BODY_SIZE_C1 + sessionObject.getNameLength(), CLASS_NUM, CTYPE, output);
        writeAttributeFilter(sessionObject.getIncludeAny(), output);
        writeAttributeFilter(sessionObject.getExcludeAny(), output);
        writeAttributeFilter(sessionObject.getIncludeAll(), output);
        SessionAttributeObjectType7Parser.serializeSessionObject(sessionObject, output);
    }
}
