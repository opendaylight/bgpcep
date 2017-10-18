/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;

final class SimpleCapabilityRegistry implements CapabilityRegistry {
    private final HandlerRegistry<DataContainer, CapabilityParser, CapabilitySerializer> handlers = new HandlerRegistry<>();

    AutoCloseable registerCapabilityParser(final int messageType, final CapabilityParser parser) {
        Preconditions.checkArgument(messageType >= 0 && messageType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        return this.handlers.registerParser(messageType, parser);
    }

    AutoCloseable registerCapabilitySerializer(final Class<? extends DataObject> paramClass, final CapabilitySerializer serializer) {
        return this.handlers.registerSerializer(paramClass, serializer);
    }

    @Override
    public CParameters parseCapability(final int type, final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final CapabilityParser parser = this.handlers.getParser(type);
        if (parser == null) {
            return null;
        }
        return parser.parseCapability(buffer);
    }

    @Override
    public void serializeCapability(final CParameters capability, ByteBuf bytes) {
        for (CapabilitySerializer s : this.handlers.getAllSerializers()) {
            s.serializeCapability(capability, bytes);
        }
    }
}
