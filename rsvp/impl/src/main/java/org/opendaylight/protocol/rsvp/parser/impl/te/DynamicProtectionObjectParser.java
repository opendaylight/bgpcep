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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.protection.object.DynamicControlProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.object.protection.object.DynamicControlProtectionObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.protection.subobject.ProtectionSubobject;

public final class DynamicProtectionObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 37;
    public static final short CTYPE = 2;
    private static final int PROTECTION_TYPE2_BODY_SIZE = 8;

    @Override
    final protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final DynamicControlProtectionObjectBuilder builder = new DynamicControlProtectionObjectBuilder();
        final ProtectionSubobject pSub = ProtectionCommonParser.parseCommonProtectionBodyType2(byteBuf);
        return builder.setProtectionSubobject(pSub).build();
    }

    @Override
    final public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof DynamicControlProtectionObject, "ProtectionObject is mandatory.");
        final DynamicControlProtectionObject protectionObject = (DynamicControlProtectionObject) teLspObject;

        serializeAttributeHeader(PROTECTION_TYPE2_BODY_SIZE, CLASS_NUM, CTYPE, output);
        ProtectionCommonParser.serializeBodyType2(protectionObject.getProtectionSubobject(), output);
    }
}
