/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.RROSubobjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.RroBuilder;

/**
 * Parser for {@link Rro}.
 */
public final class PCEPReportedRouteObjectParser extends AbstractRROWithSubobjectsParser {
    public static final int CLASS = 8;
    public static final int TYPE = 1;

    public PCEPReportedRouteObjectParser(final RROSubobjectRegistry subobjReg) {
        super(subobjReg, CLASS, TYPE);
    }

    @Override
    public Rro parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        final var builder = new RroBuilder()
            .setIgnore(header.getIgnore())
            .setProcessingRule(header.getProcessingRule());

        // Reported RRO may be empty, in particular when the LSP is Down and not delegated
        if (bytes != null && bytes.isReadable()) {
            builder.setSubobject(parseSubobjects(bytes.slice()))
                .build();
        }

        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        checkArgument(object instanceof Rro, "Wrong instance of PCEPObject. Passed %s. Needed RroObject.",
            object.getClass());
        final Rro obj = (Rro) object;
        final ByteBuf body = Unpooled.buffer();
        serializeSubobject(obj.getSubobject(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.getProcessingRule(), object.getIgnore(), body, buffer);
    }
}
