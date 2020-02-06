/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.spi.registry;

import org.opendaylight.protocol.bmp.spi.parser.BmpMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpMessageSerializer;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvRegistry;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.Tlv;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Notification;

public class SimpleBmpExtensionProviderContext implements BmpExtensionConsumerContext, BmpExtensionProviderContext {
    private final BmpMessageRegistry bmpMessageRegistry = new SimpleBmpMessageRegistry();
    private final BmpTlvRegistry bmpStatisticsTlvRegistry = new SimpleBmpTlvRegistry();
    private final BmpTlvRegistry bmpInitiationTlvRegistry = new SimpleBmpTlvRegistry();
    private final BmpTlvRegistry bmpTerminationTlvRegistry = new SimpleBmpTlvRegistry();
    private final BmpTlvRegistry bmpRouteMirroringTlvRegistry = new SimpleBmpTlvRegistry();
    private final BmpTlvRegistry bmpPeerUpTlvRegistry = new SimpleBmpTlvRegistry();

    @Override
    public Registration registerBmpMessageParser(final int messageType, final BmpMessageParser parser) {
        return this.bmpMessageRegistry.registerBmpMessageParser(messageType, parser);
    }

    @Override
    public Registration registerBmpMessageSerializer(final Class<? extends Notification> messageClass,
            final BmpMessageSerializer serializer) {
        return this.bmpMessageRegistry.registerBmpMessageSerializer(messageClass, serializer);
    }

    @Override
    public BmpMessageRegistry getBmpMessageRegistry() {
        return this.bmpMessageRegistry;
    }

    @Override
    public Registration registerBmpStatisticsTlvParser(final int tlvType, final BmpTlvParser parser) {
        return this.bmpStatisticsTlvRegistry.registerBmpTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerBmpStatisticsTlvSerializer(final Class<? extends Tlv> tlvClass,
            final BmpTlvSerializer serializer) {
        return this.bmpStatisticsTlvRegistry.registerBmpTlvSerializer(tlvClass, serializer);
    }

    @Override
    public Registration registerBmpPeerUpTlvParser(final int tlvType, final BmpTlvParser parser) {
        return this.bmpPeerUpTlvRegistry.registerBmpTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerBmpPeerUpTlvSerializer(final Class<? extends Tlv> tlvClass,
            final BmpTlvSerializer serializer) {
        return this.bmpPeerUpTlvRegistry.registerBmpTlvSerializer(tlvClass, serializer);
    }

    @Override
    public Registration registerBmpInitiationTlvParser(final int tlvType, final BmpTlvParser parser) {
        return this.bmpInitiationTlvRegistry.registerBmpTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerBmpInitiationTlvSerializer(final Class<? extends Tlv> tlvClass,
            final BmpTlvSerializer serializer) {
        return this.bmpInitiationTlvRegistry.registerBmpTlvSerializer(tlvClass, serializer);
    }

    @Override
    public Registration registerBmpTerminationTlvParser(final int tlvType, final BmpTlvParser parser) {
        return this.bmpTerminationTlvRegistry.registerBmpTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerBmpTerminationTlvSerializer(final Class<? extends Tlv> tlvClass,
            final BmpTlvSerializer serializer) {
        return this.bmpTerminationTlvRegistry.registerBmpTlvSerializer(tlvClass, serializer);
    }

    @Override
    public Registration registerBmpRouteMirroringTlvParser(final int tlvType, final BmpTlvParser parser) {
        return this.bmpRouteMirroringTlvRegistry.registerBmpTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerBmpRouteMirroringTlvSerializer(final Class<? extends Tlv> tlvClass,
            final BmpTlvSerializer serializer) {
        return this.bmpRouteMirroringTlvRegistry.registerBmpTlvSerializer(tlvClass, serializer);
    }

    @Override
    public BmpTlvRegistry getBmpStatisticsTlvRegistry() {
        return this.bmpStatisticsTlvRegistry;
    }

    @Override
    public BmpTlvRegistry getBmpInitiationTlvRegistry() {
        return this.bmpInitiationTlvRegistry;
    }

    @Override
    public BmpTlvRegistry getBmpPeerUpTlvRegistry() {
        return this.bmpPeerUpTlvRegistry;
    }

    @Override
    public BmpTlvRegistry getBmpTerminationTlvRegistry() {
        return this.bmpTerminationTlvRegistry;
    }

    @Override
    public BmpTlvRegistry getBmpRouteMirroringTlvRegistry() {
        return this.bmpRouteMirroringTlvRegistry;
    }
}
