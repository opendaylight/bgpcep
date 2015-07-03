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
import org.opendaylight.protocol.bgp.parser.spi.RsvpTeObjectParser;
import org.opendaylight.protocol.bgp.parser.spi.RsvpTeObjectRegistry;
import org.opendaylight.protocol.bgp.parser.spi.RsvpTeObjectSerializer;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;

final class SimpleRsvpObjectRegistry implements RsvpTeObjectRegistry {

    private final Table<Integer, Integer, RsvpTeObjectParser> parserHandler = HashBasedTable.create();
    private final Table<Class<? extends RsvpTeObject>, Integer, RsvpTeObjectSerializer> serializerHandler = HashBasedTable.create();

    void registerRsvpObjectParser(final int classNum, final int cType, final RsvpTeObjectParser parser) {
        Preconditions.checkArgument(classNum >= 0 && classNum <= Values.UNSIGNED_BYTE_MAX_VALUE);
        Preconditions.checkArgument(cType >= 0 && cType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        this.parserHandler.put(classNum, cType, parser);
    }

    void registerRsvpObjectSerializer(final Class<? extends RsvpTeObject> objectClass, final int cType, final RsvpTeObjectSerializer serializer) {
        Preconditions.checkArgument(cType >= 0 && cType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        this.serializerHandler.put(objectClass, cType, serializer);
    }

    @Override
    public RsvpTeObject parseRspvTe(final int classNum, final int cType, final ByteBuf buffer) throws
        BGPParsingException {
        final RsvpTeObjectParser parser = this.parserHandler.get(classNum, cType);
        if (parser == null) {
            return null;
        }
        return parser.parseObject(buffer);
    }

    @Override
    public void serializeRspvTe(final RsvpTeObject parameter, final int cType, final ByteBuf bytes) {
        final RsvpTeObjectSerializer serializer = this.serializerHandler.get(parameter.getImplementedInterface(), cType);
        if (serializer == null) {
            return;
        }
        serializer.serializeObject(parameter, bytes);
    }
}
