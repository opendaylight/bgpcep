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
import org.opendaylight.protocol.bgp.evpn.spi.EvpnParser;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnRegistry;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.concepts.MultiRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.EvpnChoice;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;

public final class SimpleEvpnNlriRegistry implements EvpnRegistry {
    private static final SimpleEvpnNlriRegistry SINGLETON = new SimpleEvpnNlriRegistry();
    private final HandlerRegistry<DataContainer, EvpnParser, EvpnSerializer> handlers = new HandlerRegistry<>();
    private final MultiRegistry<NodeIdentifier, EvpnSerializer> modelHandlers = new MultiRegistry<>();

    private SimpleEvpnNlriRegistry() {
    }

    @FunctionalInterface
    private interface SerializerInterface {
        EvpnChoice check(EvpnSerializer serializer, ContainerNode cont);
    }

    public static SimpleEvpnNlriRegistry getInstance() {
        return SINGLETON;
    }

    public Registration registerNlriParser(final NlriType esiType, final EvpnParser parser) {
        return this.handlers.registerParser(esiType.getIntValue(), parser);
    }

    public Registration registerNlriSerializer(final Class<? extends EvpnChoice> evpnClass,
            final EvpnSerializer serializer) {
        return this.handlers.registerSerializer(evpnClass, serializer);
    }

    public Registration registerNlriModelSerializer(final QName qname, final EvpnSerializer serializer) {
        return this.modelHandlers.register(new NodeIdentifier(qname), serializer);
    }

    @Override
    @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "SB does not grok TYPE_USE")
    public EvpnChoice parseEvpn(final NlriType type, final ByteBuf buffer) {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final EvpnParser parser = this.handlers.getParser(type.getIntValue());
        return parser == null ? null : parser.parseEvpn(buffer);
    }

    @Override
    public ByteBuf serializeEvpn(final EvpnChoice evpn, final ByteBuf common) {
        final EvpnSerializer serializer = this.handlers.getSerializer(evpn.implementedInterface());
        return serializer == null ? common : serializer.serializeEvpn(evpn, common);
    }

    @Override
    public EvpnChoice serializeEvpnModel(final ChoiceNode evpnChoice) {
        return getEvpnCase(evpnChoice, EvpnSerializer::serializeEvpnModel);
    }

    @Override
    public EvpnChoice serializeEvpnRouteKey(final ChoiceNode evpnChoice) {
        return getEvpnCase(evpnChoice, EvpnSerializer::createRouteKey);
    }

    private EvpnChoice getEvpnCase(final ChoiceNode evpnChoice, final SerializerInterface serializerInterface) {
        checkArgument(evpnChoice != null, "Evpn case is mandatory, cannot be null");
        final Collection<DataContainerChild<? extends PathArgument, ?>> value = evpnChoice.getValue();
        checkArgument(!value.isEmpty(), "Evpn case is mandatyr, cannot be empty");
        final ContainerNode cont = (ContainerNode) Iterables.getOnlyElement(value);
        final EvpnSerializer serializer = this.modelHandlers.get(cont.getIdentifier());
        return serializer == null ? null : serializerInterface.check(serializer, cont);
    }
}
