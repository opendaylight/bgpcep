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
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.admin.status.object.AdminStatusObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.admin.status.object.AdminStatusObjectBuilder;

public final class AdminStatusObjectParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 196;
    public static final short CTYPE = 1;
    private static final int REFLECT = 0;
    private static final int TESTING = 5;
    private static final int DOWN = 6;
    private static final int DELETION = 7;
    private static final Integer BODY_SIZE = 4;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final AdminStatusObjectBuilder adm = new AdminStatusObjectBuilder();
        final BitArray reflect = BitArray.valueOf(byteBuf, FLAGS_SIZE);
        adm.setReflect(reflect.get(REFLECT));
        byteBuf.skipBytes(Short.BYTES);
        final BitArray flags = BitArray.valueOf(byteBuf, FLAGS_SIZE);
        adm.setTesting(flags.get(TESTING));
        adm.setAdministrativelyDown(flags.get(DOWN));
        adm.setDeletionInProgress(flags.get(DELETION));
        return adm.build();
    }

    @Override
    protected void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof AdminStatusObject, "AssociationObject is mandatory.");
        final AdminStatusObject addObject = (AdminStatusObject) teLspObject;
        serializeAttributeHeader(BODY_SIZE, CLASS_NUM, CTYPE, output);
        final BitArray reflect = new BitArray(FLAGS_SIZE);
        reflect.set(REFLECT, addObject.getReflect());
        reflect.toByteBuf(output);
        output.writeShort(0);
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(TESTING, addObject.getTesting());
        flags.set(DOWN, addObject.getAdministrativelyDown());
        flags.set(DELETION, addObject.getDeletionInProgress());
        flags.toByteBuf(output);
    }
}
