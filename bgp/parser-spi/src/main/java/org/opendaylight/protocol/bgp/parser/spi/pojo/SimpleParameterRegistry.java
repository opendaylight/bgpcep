/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataContainer;

final class SimpleParameterRegistry implements ParameterRegistry {
    private final HandlerRegistry<DataContainer, ParameterParser, ParameterSerializer> handlers =
            new HandlerRegistry<>();

    Registration registerParameterParser(final int messageType, final ParameterParser parser) {
        // 255 is explicitly excluded because it is handled in OPEN message parser
        checkArgument(messageType >= 0 && messageType < Values.UNSIGNED_BYTE_MAX_VALUE);
        return this.handlers.registerParser(messageType, parser);
    }

    Registration registerParameterSerializer(final Class<? extends BgpParameters> paramClass,
            final ParameterSerializer serializer) {
        return this.handlers.registerSerializer(paramClass, serializer);
    }

    @Override
    public Optional<ParameterParser> findParser(final int parameterType) {
        return Optional.ofNullable(handlers.getParser(parameterType));
    }

    @Override
    public Optional<ParameterSerializer> findSerializer(final BgpParameters parameter) {
        return Optional.ofNullable(handlers.getSerializer(parameter.implementedInterface()));
    }
}
