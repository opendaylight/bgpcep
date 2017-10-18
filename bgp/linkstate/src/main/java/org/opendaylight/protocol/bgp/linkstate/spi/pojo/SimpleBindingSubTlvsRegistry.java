/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.spi.pojo;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.BindingSubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.BindingSubTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleBindingSubTlvsRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleBindingSubTlvsRegistry.class);
    private static final SimpleBindingSubTlvsRegistry SINGLETON = new SimpleBindingSubTlvsRegistry();
    private final HandlerRegistry<DataContainer, BindingSubTlvsParser, BindingSubTlvsSerializer> handlers = new HandlerRegistry<>();

    private SimpleBindingSubTlvsRegistry() {
    }


    public static SimpleBindingSubTlvsRegistry getInstance() {
        return SINGLETON;
    }

    public AutoCloseable registerBindingSubTlvsParser(final int bindingSubTlvsType, final BindingSubTlvsParser parser) {
        return this.handlers.registerParser(bindingSubTlvsType, parser);
    }

    public AutoCloseable registerBindingSubTlvsSerializer(final Class<? extends BindingSubTlv> esiType, final BindingSubTlvsSerializer serializer) {
        return this.handlers.registerSerializer(esiType, serializer);
    }

    public void serializeBindingSubTlvs(final List<BindingSubTlvs> bindingSubTlvs, final ByteBuf aggregator) {
        for (final BindingSubTlvs subTlv : bindingSubTlvs) {
            final BindingSubTlv bindingSubTlv = subTlv.getBindingSubTlv();
            final BindingSubTlvsSerializer serializer = this.handlers.getSerializer(bindingSubTlv.getImplementedInterface());
            if (serializer == null) {
                LOG.info("Unknown binding sub Tlv type {}", subTlv);
                return;
            }
            serializer.serializeSubTlv(bindingSubTlv, aggregator);
        }
    }

    public List<BindingSubTlvs> parseBindingSubTlvs(final ByteBuf buffer, final ProtocolId protocolId) {
        final List<BindingSubTlvs> subTlvs = new ArrayList<>();
        if (buffer != null) {
            while (buffer.isReadable()) {
                final int type = buffer.readUnsignedShort();
                final int length = buffer.readUnsignedShort();
                final ByteBuf slice = buffer.readSlice(length);
                final BindingSubTlvsParser parser = this.handlers.getParser(type);
                if (parser == null) {
                    return null;
                }
                subTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(parser.parseSubTlv(slice, protocolId)).build());
            }
        }
        return subTlvs;
    }
}
