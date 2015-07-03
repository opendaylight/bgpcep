/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.linkstate.attribute.objects;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.association.object.AssociationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.association.object.AssociationObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.association.object.association.object.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.association.object.association.object.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.association.object.association.object.address.family.Ipv4CaseBuilder;

public final class BGPAssociationObjectParserType1 extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 199;
    public static final short CTYPE = 1;

    private static final int IPV4_CTYPE_LENGTH = 12;
    private static final Integer BODY_SIZE = 8;

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        final AssociationObjectBuilder asso = new AssociationObjectBuilder();
        asso.setClassNum(CLASS_NUM);
        asso.setCType(CTYPE);
        asso.setAssociationType(byteBuf.readUnsignedShort());
        asso.setAssociationId(byteBuf.readUnsignedShort());
        Ipv4CaseBuilder ipv4Builder = new Ipv4CaseBuilder();
        ipv4Builder.setIpv4AssociationSource(Ipv4Util.addressForByteBuf(byteBuf));
        asso.setAddressFamily(ipv4Builder.build());

        return asso.build();
    }

    @Override
    public void localSerializeObject(final TeLspObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof AssociationObject, "AssociationObject is mandatory.");
        final AssociationObject assObject = (AssociationObject) teLspObject;
        final AddressFamily address = assObject.getAddressFamily();
        Preconditions.checkArgument(address instanceof Ipv4Case, "Ipv4 Address is mandatory.");
        serializeAttributeHeader(BODY_SIZE, assObject.getClassNum(), assObject.getCType(), output);
        output.writeShort(assObject.getAssociationType());
        output.writeShort(assObject.getAssociationId());
        output.writeBytes(Ipv4Util.byteBufForAddress(((Ipv4Case) address).getIpv4AssociationSource()));
    }
}
