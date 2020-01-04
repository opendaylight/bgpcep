/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.te;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AssociationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.association.object.AssociationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.association.object.AssociationObjectBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public abstract class AbstractAssociationParser extends AbstractRSVPObjectParser {
    public static final short CLASS_NUM = 199;
    public static final short CTYPE_IPV4 = 1;
    public static final short CTYPE_IPV6 = 2;

    private static final Integer BODY_SIZE_IPV4 = 8;
    private static final Integer BODY_SIZE_IPV6 = 20;

    protected abstract IpAddressNoZone parseAssociationIpAddress(ByteBuf byteBuf);

    @Override
    protected final void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf output) {
        checkArgument(teLspObject instanceof AssociationObject, "AssociationObject is mandatory.");
        final AssociationObject assObject = (AssociationObject) teLspObject;

        if (assObject.getIpAddress().getIpv4AddressNoZone() != null) {
            serializeAttributeHeader(BODY_SIZE_IPV4, CLASS_NUM, CTYPE_IPV4, output);
            output.writeShort(assObject.getAssociationType().getIntValue());
            output.writeShort(assObject.getAssociationId().toJava());
            output.writeBytes(Ipv4Util.byteBufForAddress(assObject.getIpAddress().getIpv4AddressNoZone()));
        } else {
            serializeAttributeHeader(BODY_SIZE_IPV6, CLASS_NUM, CTYPE_IPV6, output);
            output.writeShort(assObject.getAssociationType().getIntValue());
            output.writeShort(assObject.getAssociationId().toJava());
            output.writeBytes(Ipv6Util.byteBufForAddress(assObject.getIpAddress().getIpv6AddressNoZone()));
        }
    }

    @Override
    protected final RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
        return new AssociationObjectBuilder()
                .setAssociationType(AssociationType.forValue(byteBuf.readUnsignedShort()))
                .setAssociationId(ByteBufUtils.readUint16(byteBuf))
                .setIpAddress(parseAssociationIpAddress(byteBuf))
                .build();
    }
}
