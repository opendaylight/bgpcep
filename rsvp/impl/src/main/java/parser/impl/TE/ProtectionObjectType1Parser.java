/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package parser.impl.TE;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.ProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.ProtectionObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.subobject.ProtectionSubobject;
import parser.spi.RSVPParsingException;

public final class ProtectionObjectType1Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 37;
    public static final short CTYPE = 1;
    private static final Integer PROTECTION_BODY_SYZE = 4;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        ProtectionObjectBuilder builder = new ProtectionObjectBuilder();
        builder.setCType(CTYPE);
        final ProtectionSubobject pSub = AbstractProtectionParser.parseCommonProtectionBodyType1(byteBuf);
        return builder.setProtectionSubobject(pSub).build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof ProtectionObject, "ProtectionObject is mandatory.");
        final ProtectionObject protectionObject = (ProtectionObject) teLspObject;

        serializeAttributeHeader(PROTECTION_BODY_SYZE, CLASS_NUM, protectionObject.getCType(), output);
        AbstractProtectionParser.serializeBodyType1(protectionObject.getProtectionSubobject(), output);
    }
}
