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
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.protection.object.BasicProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.protection.object.BasicProtectionObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.subobject.ProtectionSubobject;

public final class ProtectionObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 37;
    public static final short CTYPE = 1;
    private static final Integer PROTECTION_BODY_SIZE = 4;

    @Override
    final protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final BasicProtectionObjectBuilder builder = new BasicProtectionObjectBuilder();
        final ProtectionSubobject pSub = ProtectionCommonParser.parseCommonProtectionBodyType1(byteBuf);
        return builder.setProtectionSubobject(pSub).build();
    }

    @Override
    final public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof BasicProtectionObject, "ProtectionObject is mandatory.");
        final BasicProtectionObject protectionObject = (BasicProtectionObject) teLspObject;

        serializeAttributeHeader(PROTECTION_BODY_SIZE, CLASS_NUM, CTYPE, output);
        ProtectionCommonParser.serializeBodyType1(protectionObject.getProtectionSubobject(), output);
    }
}
