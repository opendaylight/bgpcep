/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.spi.pojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.evpn.spi.EsiParser;
import org.opendaylight.protocol.bgp.evpn.spi.EsiRegistry;
import org.opendaylight.protocol.bgp.evpn.spi.EsiSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.concepts.MultiRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleEsiTypeRegistry implements EsiRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleEsiTypeRegistry.class);
    private static final SimpleEsiTypeRegistry SINGLETON = new SimpleEsiTypeRegistry();
    private static final int CONTENT_LENGTH = 10;
    private static final int ESI_LENGTH = 9;
    private final HandlerRegistry<DataContainer, EsiParser, EsiSerializer> handlers = new HandlerRegistry<>();
    private final MultiRegistry<NodeIdentifier, EsiSerializer> modelHandlers = new MultiRegistry<>();

    private SimpleEsiTypeRegistry() {
    }

    public static SimpleEsiTypeRegistry getInstance() {
        return SINGLETON;
    }

    public AutoCloseable registerEsiParser(final EsiType esiType, final EsiParser parser) {
        return this.handlers.registerParser(esiType.getIntValue(), parser);
    }

    public AutoCloseable registerEsiSerializer(final Class<? extends Esi> esiType, final EsiSerializer serializer) {
        return this.handlers.registerSerializer(esiType, serializer);
    }

    public AutoCloseable registerEsiModelSerializer(final QName qName, final EsiSerializer serializer) {
        return this.modelHandlers.register(new NodeIdentifier(qName), serializer);
    }

    @Override
    public Esi parseEsi(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        Preconditions.checkArgument(buffer.readableBytes() == CONTENT_LENGTH, "Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");

        final EsiParser parser = this.handlers.getParser(EsiType.forValue(buffer.readByte()).getIntValue());
        if (parser == null) {
            return null;
        }
        return parser.parseEsi(buffer.readSlice(ESI_LENGTH));
    }

    @Override
    public Esi parseEsiModel(final ChoiceNode esiChoice) {
        Preconditions.checkArgument(esiChoice != null && !esiChoice.getValue().isEmpty(), "ESI is mandatory. Can't be null or empty.");
        final ContainerNode cont = (ContainerNode) Iterables.getOnlyElement(esiChoice.getValue());
        final EsiSerializer serializer = this.modelHandlers.get(cont.getIdentifier());
        if (serializer != null) {
            return serializer.serializeEsi(cont);
        }

        LOG.warn("Unrecognized ESI {}", esiChoice);
        return null;
    }

    @Override
    public void serializeEsi(final Esi esi, final ByteBuf buffer) {
        final EsiSerializer serializer = this.handlers.getSerializer(esi.getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeEsi(esi, buffer);
    }
}
