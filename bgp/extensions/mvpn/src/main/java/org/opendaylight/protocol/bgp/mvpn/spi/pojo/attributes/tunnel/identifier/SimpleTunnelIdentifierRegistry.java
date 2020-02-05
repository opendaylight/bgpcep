/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.spi.pojo.attributes.tunnel.identifier;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.mvpn.spi.attributes.tunnel.identifier.TunnelIdentifierParser;
import org.opendaylight.protocol.bgp.mvpn.spi.attributes.tunnel.identifier.TunnelIdentifierSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleTunnelIdentifierRegistry {
    public static final int NO_TUNNEL_INFORMATION_PRESENT = 0;
    private static final SimpleTunnelIdentifierRegistry SINGLETON = new SimpleTunnelIdentifierRegistry();
    private static final Logger LOG = LoggerFactory.getLogger(SimpleTunnelIdentifierRegistry.class);
    private final HandlerRegistry<DataContainer, TunnelIdentifierParser<?>, TunnelIdentifierSerializer<?>> handlers =
            new HandlerRegistry<>();

    private SimpleTunnelIdentifierRegistry() {
    }

    public static SimpleTunnelIdentifierRegistry getInstance() {
        return SINGLETON;
    }

    public TunnelIdentifier parse(final int tunnelType, final ByteBuf buffer) {
        final TunnelIdentifierParser<?> parser = this.handlers.getParser(tunnelType);
        if (!buffer.isReadable() || parser == null) {
            LOG.debug("Skipping parsing of PMSI Tunnel Attribute type {}", tunnelType);
            return null;
        }
        return parser.parse(buffer);
    }

    public int serialize(final TunnelIdentifier tunnel, final ByteBuf tunnelBuffer) {
        if (tunnel == null) {
            LOG.debug("Skipping serialization of null PMSI Tunnel Attribute");
            return NO_TUNNEL_INFORMATION_PRESENT;
        }
        final TunnelIdentifierSerializer serializer = this.handlers.getSerializer(tunnel.implementedInterface());
        if (serializer == null) {
            LOG.debug("Skipping serialization of PMSI Tunnel Attribute {}", tunnel);
            return NO_TUNNEL_INFORMATION_PRESENT;
        }
        return serializer.serialize(tunnel, tunnelBuffer);
    }

    public Registration registerParser(final TunnelIdentifierParser<?> parser) {
        return this.handlers.registerParser(parser.getType(), parser);
    }

    public Registration registerSerializer(final TunnelIdentifierSerializer<?> serializer) {
        return this.handlers.registerSerializer(serializer.getClazz(), serializer);
    }
}
