/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.protocol.bgp.parser.spi.ParameterUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for BGP Capability Parameter.
 */
public final class CapabilityParameterParser implements ParameterParser, ParameterSerializer {
    public static final int TYPE = 2;

    private static final Logger LOG = LoggerFactory.getLogger(CapabilityParameterParser.class);
    private final CapabilityRegistry reg;

    public CapabilityParameterParser(final CapabilityRegistry reg) {
        this.reg = Preconditions.checkNotNull(reg);
    }

    @Override
    public BgpParameters parseParameter(final ByteBuf buffer) throws BGPParsingException, BGPDocumentedException {
        Preconditions.checkArgument(buffer != null && buffer.readableBytes() != 0, "Byte array cannot be null or empty.");
        LOG.trace("Started parsing of BGP Capabilities: {}", Arrays.toString(ByteArray.getAllBytes(buffer)));
        final List<OptionalCapabilities> optionalCapas = Lists.newArrayList();
        while (buffer.isReadable()) {
            final OptionalCapabilities optionalCapa = parseOptionalCapability(buffer);
            if (optionalCapa != null) {
                optionalCapas.add(optionalCapa);
            }
        }
        return new BgpParametersBuilder().setOptionalCapabilities(optionalCapas).build();
    }

    private OptionalCapabilities parseOptionalCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final int capCode = buffer.readUnsignedByte();
        final int capLength = buffer.readUnsignedByte();
        final ByteBuf paramBody = buffer.readSlice(capLength);
        final CParameters ret = this.reg.parseCapability(capCode, paramBody);
        if (ret == null) {
            LOG.debug("Ignoring unsupported capability {}", capCode);
            return null;
        }
        return new OptionalCapabilitiesBuilder().setCParameters(ret).build();
    }

    @Override
    public void serializeParameter(final BgpParameters parameter, final ByteBuf byteAggregator) {
        if (parameter.getOptionalCapabilities() != null && !parameter.getOptionalCapabilities().isEmpty()) {
            LOG.trace("Started serializing BGP Capability: {}", parameter.getOptionalCapabilities());
            final ByteBuf buffer = Unpooled.buffer();
            for (final OptionalCapabilities optionalCapa : parameter.getOptionalCapabilities()) {
                serializeOptionalCapability(optionalCapa, buffer);
            }
            ParameterUtil.formatParameter(TYPE, buffer, byteAggregator);
        }
    }

    private void serializeOptionalCapability(final OptionalCapabilities optionalCapa, final ByteBuf byteAggregator) {
        if (optionalCapa.getCParameters() != null) {
            final CParameters cap = optionalCapa.getCParameters();
            final ByteBuf bytes = Unpooled.buffer();
            this.reg.serializeCapability(cap, bytes);
            if (bytes == null) {
                throw new IllegalArgumentException("Unhandled capability class" + cap.getImplementedInterface());
            }
            LOG.trace("BGP capability serialized to: {}", ByteBufUtil.hexDump(bytes));
            byteAggregator.writeBytes(bytes);
        }
    }
}
