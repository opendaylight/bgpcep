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
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AssociationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.association.object.AssociationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.association.object.AssociationObjectBuilder;
import parser.spi.RSVPParsingException;

public final class AssociationObjectParserType2 extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 199;
    public static final short CTYPE = 2;
    private static final Integer BODY_SIZE = 20;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        final AssociationObjectBuilder asso = new AssociationObjectBuilder();
        asso.setCType(CTYPE);
        asso.setAssociationType(AssociationType.forValue(byteBuf.readUnsignedShort()));
        asso.setAssociationId(byteBuf.readUnsignedShort());
        asso.setIpAddress(new IpAddress(Ipv6Util.addressForByteBuf(byteBuf)));
        return asso.build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof AssociationObject, "AssociationObject is mandatory.");
        final AssociationObject assObject = (AssociationObject) teLspObject;
        serializeAttributeHeader(BODY_SIZE, CLASS_NUM, CTYPE, output);
        output.writeShort(assObject.getAssociationType().getIntValue());
        output.writeShort(assObject.getAssociationId());
        output.writeBytes(Ipv6Util.byteBufForAddress(assObject.getIpAddress().getIpv6Address()));

    }
}
