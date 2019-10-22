/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeBoolean;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.Xro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.Xro.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.XroBuilder;

/**
 * Parser for {@link Xro}.
 */
public final class PCEPExcludeRouteObjectParser extends AbstractXROWithSubobjectsParser {

    private static final int CLASS = 17;
    private static final int TYPE = 1;
    private static final int FLAGS_OFFSET = 3;

    public PCEPExcludeRouteObjectParser(final XROSubobjectRegistry registry) {
        super(registry, CLASS, TYPE);
    }

    @Override
    public Xro parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(),
            "Array of bytes is mandatory. Can't be null or empty.");
        final XroBuilder builder = new XroBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        bytes.skipBytes(FLAGS_OFFSET);
        builder.setFlags(new Flags(bytes.readBoolean()));
        builder.setSubobject(parseSubobjects(bytes.slice()));
        return builder.build();
    }

    @Override
    public void serializeObject(final Object object, final ByteBuf buffer) {
        Preconditions.checkArgument(object instanceof Xro,
            "Wrong instance of PCEPObject. Passed %s. Needed XroObject.", object.getClass());
        final Xro obj = (Xro) object;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(FLAGS_OFFSET);
        writeBoolean(obj.getFlags() != null ? obj.getFlags().isFail() : null, body);
        serializeSubobject(obj.getSubobject(), body);
        ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), body, buffer);
    }
}
