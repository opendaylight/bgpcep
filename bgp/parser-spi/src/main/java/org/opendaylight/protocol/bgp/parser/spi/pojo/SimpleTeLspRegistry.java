/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.TeLspObjectParser;
import org.opendaylight.protocol.bgp.parser.spi.TeLspObjectSerializer;
import org.opendaylight.protocol.bgp.parser.spi.TeLspRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;

final class SimpleTeLspRegistry implements TeLspRegistry {

    private final Table<Integer, Integer, TeLspObjectParser> parserHandler = HashBasedTable.create();
    private final Table<Class<? extends TeLspObject>, Integer, TeLspObjectSerializer> serializerHandler = HashBasedTable.create();

    void registerTeLspObjectParser(final int classNum, final int cType, final TeLspObjectParser parser) {
        Preconditions.checkArgument(classNum >= 0 && classNum <= Values.UNSIGNED_BYTE_MAX_VALUE);
        Preconditions.checkArgument(cType >= 0 && cType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        this.parserHandler.put(classNum, cType, parser);
    }

    void registerTeLspObjectSerialize(final Class<? extends TeLspObject> objectClass, final int cType, final TeLspObjectSerializer serializer) {
        Preconditions.checkArgument(cType >= 0 && cType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        this.serializerHandler.put(objectClass, cType, serializer);
    }

    @Override
    public TeLspObject parseTeLsp(final int classNum, final int cType, final ByteBuf buffer) throws BGPParsingException {
        final TeLspObjectParser parser = this.parserHandler.get(classNum, cType);
        if (parser == null) {
            return null;
        }
        return parser.parseObject(buffer);
    }

    @Override
    public void serializeTeLsp(final TeLspObject parameter, final int cType, final ByteBuf bytes) {
        final TeLspObjectSerializer serializer = this.serializerHandler.get(parameter.getImplementedInterface(),cType);
        if (serializer == null) {
            return;
        }
        serializer.serializeObject(parameter,bytes);
    }
}
