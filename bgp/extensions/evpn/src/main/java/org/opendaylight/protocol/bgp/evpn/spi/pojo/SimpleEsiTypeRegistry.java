/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.spi.pojo;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Iterables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import org.opendaylight.protocol.bgp.evpn.spi.EsiParser;
import org.opendaylight.protocol.bgp.evpn.spi.EsiRegistry;
import org.opendaylight.protocol.bgp.evpn.spi.EsiSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.concepts.MultiRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.Esi;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
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

    public Registration registerEsiParser(final EsiType esiType, final EsiParser parser) {
        return this.handlers.registerParser(esiType.getIntValue(), parser);
    }

    public Registration registerEsiSerializer(final Class<? extends Esi> esiType, final EsiSerializer serializer) {
        return this.handlers.registerSerializer(esiType, serializer);
    }

    public Registration registerEsiModelSerializer(final QName qname, final EsiSerializer serializer) {
        return this.modelHandlers.register(new NodeIdentifier(qname), serializer);
    }

    @Override
    @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "SB does not grok TYPE_USE")
    public Esi parseEsi(final ByteBuf buffer) {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        checkArgument(buffer.readableBytes() == CONTENT_LENGTH,
                "Wrong length of array of bytes. Passed: %s;", buffer.readableBytes());

        final EsiParser parser = this.handlers.getParser(EsiType.forValue(buffer.readByte()).getIntValue());
        return parser == null ? null : parser.parseEsi(buffer.readSlice(ESI_LENGTH));
    }

    @Override
    @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "SB does not grok TYPE_USE")
    public Esi parseEsiModel(final ChoiceNode esiChoice) {
        checkArgument(esiChoice != null, "ESI cannot be null");
        final Collection<DataContainerChild<?, ?>> value = esiChoice.getValue();
        checkArgument(!value.isEmpty(), "ESI may not be empty");
        final ContainerNode cont = (ContainerNode) Iterables.getOnlyElement(value);
        final EsiSerializer serializer = this.modelHandlers.get(cont.getIdentifier());
        if (serializer != null) {
            return serializer.serializeEsi(cont);
        }

        LOG.warn("Unrecognized ESI {}", esiChoice);
        return null;
    }

    @Override
    public void serializeEsi(final Esi esi, final ByteBuf buffer) {
        final EsiSerializer serializer = this.handlers.getSerializer(esi.implementedInterface());
        if (serializer != null) {
            serializer.serializeEsi(esi, buffer);
        }
    }
}
