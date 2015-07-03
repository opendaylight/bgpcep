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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.protection.object.ProtectionObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.protection.object.ProtectionObjectBuilder;

public final class BGPProtectionObjectType1Parser extends AbstractBGPObjectParser {
    public static final short CLASS_NUM = 37;
    public static final short CTYPE = 1;
    private static final Integer PROTECTION_BODY_SYZE = 4;

    @Override
    protected TeLspObject localParseObject(final ByteBuf byteBuf) throws BGPParsingException {
        ProtectionObjectBuilder builder = new ProtectionObjectBuilder();
        builder.setCType(CTYPE);
        builder.setClassNum(CLASS_NUM);
        return builder.setCommonProtectionObject(BGPCommonExplicitRouteObjectParser.parseCommonProtectionBodyType1(byteBuf)).build();
    }

    @Override
    public void localSerializeObject(final TeLspObject teLspObject, final ByteBuf output) {
        Preconditions.checkArgument(teLspObject instanceof ProtectionObject, "ProtectionObject is mandatory.");
        final ProtectionObject protectionObject = (ProtectionObject) teLspObject;

        serializeAttributeHeader(PROTECTION_BODY_SYZE, protectionObject.getClassNum(), protectionObject.getCType(), output);
        BGPCommonExplicitRouteObjectParser.serializeBodyType1(protectionObject.getCommonProtectionObject(), output);
    }
}
