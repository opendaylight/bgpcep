/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others. All rights reserved.
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
import org.opendaylight.protocol.pcep.spi.XROSubobjectRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Xro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Xro.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.XroBuilder;

/**
 * Parser for {@link Xro}
 */
public final class PCEPExcludeRouteObjectParser extends AbstractXROWithSubobjectsParser {

    public static final int CLASS = 7;

    public static final int TYPE = 1;

    private static final int FLAGS_OFFSET = 3;

    public PCEPExcludeRouteObjectParser(final XROSubobjectRegistry registry) {
        super(registry);
    }

    @Override
    public Xro parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        Preconditions.checkArgument(bytes != null && bytes.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final XroBuilder builder = new XroBuilder();
        builder.setIgnore(header.isIgnore());
        builder.setProcessingRule(header.isProcessingRule());
        bytes.readerIndex(bytes.readerIndex() + FLAGS_OFFSET);
        builder.setFlags(new Flags(bytes.readBoolean()));
        builder.setSubobject(parseSubobjects(bytes.slice()));
        return builder.build();
    }

    @Override
    public byte[] serializeObject(final Object object) {
        if (!(object instanceof Xro)) {
            throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed ExcludeRouteObject.");
        }
        final Xro obj = (Xro) object;
        assert !(obj.getSubobject().isEmpty()) : "Empty Excluded Route Object.";
        final byte[] bytes = serializeSubobject(obj.getSubobject());
        final byte[] result = new byte[FLAGS_OFFSET + 1 + bytes.length];
        if (obj.getFlags().isFail()) {
            result[FLAGS_OFFSET] = 1;
        }
        ByteArray.copyWhole(bytes, result, FLAGS_OFFSET + 1);
        return ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), result);
    }
}
