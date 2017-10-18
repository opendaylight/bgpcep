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
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.open.message.BgpParameters;
import org.opendaylight.yangtools.yang.binding.DataContainer;

final class SimpleParameterRegistry implements ParameterRegistry {
    private final HandlerRegistry<DataContainer, ParameterParser, ParameterSerializer> handlers = new HandlerRegistry<>();

    AutoCloseable registerParameterParser(final int messageType, final ParameterParser parser) {
        Preconditions.checkArgument(messageType >= 0 && messageType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        return this.handlers.registerParser(messageType, parser);
    }

    AutoCloseable registerParameterSerializer(final Class<? extends BgpParameters> paramClass, final ParameterSerializer serializer) {
        return this.handlers.registerSerializer(paramClass, serializer);
    }

    @Override
    public BgpParameters parseParameter(final int parameterType, final ByteBuf buffer) throws BGPParsingException, BGPDocumentedException {
        final ParameterParser parser = this.handlers.getParser(parameterType);
        if (parser == null) {
            return null;
        }
        return parser.parseParameter(buffer);
    }

    public void serializeParameter(final BgpParameters parameter, ByteBuf bytes) {
        final ParameterSerializer serializer = this.handlers.getSerializer(parameter.getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeParameter(parameter,bytes);
    }
}
