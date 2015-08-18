/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectSerializer;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;

public final class SimpleRSVPObjectRegistry implements RSVPTeObjectRegistry {

    private final Table<Integer, Integer, RSVPTeObjectParser> parserHandler = HashBasedTable.create();
    private final Map<Class<? extends RsvpTeObject>, RSVPTeObjectSerializer> serializerHandler = new HashMap<>();

    public void registerRsvpObjectParser(final int classNum, final int cType, final RSVPTeObjectParser parser) {
        Preconditions.checkArgument(classNum >= 0 && classNum <= Values.UNSIGNED_BYTE_MAX_VALUE);
        Preconditions.checkArgument(cType >= 0 && cType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        this.parserHandler.put(classNum, cType, parser);
    }

    public void registerRsvpObjectSerializer(final Class<? extends RsvpTeObject> objectClass, final RSVPTeObjectSerializer serializer) {
        this.serializerHandler.put(objectClass, serializer);
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
    public void serializeRSPVTe(final RsvpTeObject parameter, final ByteBuf bytes) {
        if (parameter == null) {
            return;
        }
        final RSVPTeObjectSerializer serializer = this.serializerHandler.get(parameter.getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeObject(parameter, bytes);
    }
}
