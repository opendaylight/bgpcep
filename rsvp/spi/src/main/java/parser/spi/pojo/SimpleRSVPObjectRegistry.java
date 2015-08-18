/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package parser.spi.pojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import parser.spi.RSVPParsingException;
import parser.spi.RSVPTeObjectParser;
import parser.spi.RSVPTeObjectRegistry;
import parser.spi.RSVPTeObjectSerializer;

public final class SimpleRSVPObjectRegistry implements RSVPTeObjectRegistry {

    private final Table<Integer, Integer, RSVPTeObjectParser> parserHandler = HashBasedTable.create();
    private final Table<Class<? extends RsvpTeObject>, Integer, RSVPTeObjectSerializer> serializerHandler = HashBasedTable.create();

    public void registerRsvpObjectParser(final int classNum, final int cType, final RSVPTeObjectParser parser) {
        Preconditions.checkArgument(classNum >= 0 && classNum <= Values.UNSIGNED_BYTE_MAX_VALUE);
        Preconditions.checkArgument(cType >= 0 && cType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        this.parserHandler.put(classNum, cType, parser);
    }

    public void registerRsvpObjectSerializer(final Class<? extends RsvpTeObject> objectClass, final int cType, final RSVPTeObjectSerializer serializer) {
        Preconditions.checkArgument(cType >= 0 && cType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        this.serializerHandler.put(objectClass, cType, serializer);
    }

    @Override
    public RsvpTeObject parseRSPVTe(final int classNum, final int cType, final ByteBuf buffer) throws RSVPParsingException {
        final RSVPTeObjectParser parser = this.parserHandler.get(classNum, cType);
        if (parser == null) {
            return null;
        }
        return parser.parseObject(buffer);
    }

    @Override
    public void serializeRSPVTe(final RsvpTeObject parameter, final int cType, final ByteBuf bytes) {
        final RSVPTeObjectSerializer serializer = this.serializerHandler.get(parameter.getImplementedInterface(), cType);
        if (serializer == null) {
            return;
        }
        serializer.serializeObject(parameter, bytes);
    }
}
