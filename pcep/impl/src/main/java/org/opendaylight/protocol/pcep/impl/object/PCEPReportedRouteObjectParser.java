/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.RroBuilder;

/**
 * Parser for {@link Rro}
 */
public class PCEPReportedRouteObjectParser extends AbstractRROWithSubobjectsParser {

    public static final int CLASS = 8;

    public static final int TYPE = 1;

    public PCEPReportedRouteObjectParser(final RROSubobjectRegistry subobjReg) {
        super(subobjReg);
    }

    @Override
    public Rro parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final RroBuilder builder = new RroBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        builder.setSubobject(parseSubobjects(bytes.slice()));
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        if (!(object instanceof Rro)) {
            throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass()
                    + ". Needed ReportedRouteObject.");
        }
        final Rro obj = (Rro) object;
        assert !(obj.getSubobject().isEmpty()) : "Empty Reported Route Object.";
        // FIXME: switch to ByteBuf
        buffer.writeBytes(ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), serializeSubobject(obj.getSubobject())));
    }
}
