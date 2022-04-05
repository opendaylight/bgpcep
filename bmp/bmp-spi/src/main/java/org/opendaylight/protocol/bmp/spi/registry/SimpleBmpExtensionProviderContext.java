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
import org.opendaylight.yangtools.yang.binding.DataObject;
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
        return bmpMessageRegistry.registerBmpMessageParser(messageType, parser);
    }

    @Override
    public <T extends Notification<T> & DataObject> Registration registerBmpMessageSerializer(
            final Class<T> messageClass, final BmpMessageSerializer serializer) {
        return bmpMessageRegistry.registerBmpMessageSerializer(messageClass, serializer);
    }

    @Override
    public BmpMessageRegistry getBmpMessageRegistry() {
        return bmpMessageRegistry;
    }

    @Override
    public Registration registerBmpStatisticsTlvParser(final int tlvType, final BmpTlvParser parser) {
        return bmpStatisticsTlvRegistry.registerBmpTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerBmpStatisticsTlvSerializer(final Class<? extends Tlv> tlvClass,
            final BmpTlvSerializer serializer) {
        return bmpStatisticsTlvRegistry.registerBmpTlvSerializer(tlvClass, serializer);
    }

    @Override
    public Registration registerBmpPeerUpTlvParser(final int tlvType, final BmpTlvParser parser) {
        return bmpPeerUpTlvRegistry.registerBmpTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerBmpPeerUpTlvSerializer(final Class<? extends Tlv> tlvClass,
            final BmpTlvSerializer serializer) {
        return bmpPeerUpTlvRegistry.registerBmpTlvSerializer(tlvClass, serializer);
    }

    @Override
    public Registration registerBmpInitiationTlvParser(final int tlvType, final BmpTlvParser parser) {
        return bmpInitiationTlvRegistry.registerBmpTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerBmpInitiationTlvSerializer(final Class<? extends Tlv> tlvClass,
            final BmpTlvSerializer serializer) {
        return bmpInitiationTlvRegistry.registerBmpTlvSerializer(tlvClass, serializer);
    }

    @Override
    public Registration registerBmpTerminationTlvParser(final int tlvType, final BmpTlvParser parser) {
        return bmpTerminationTlvRegistry.registerBmpTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerBmpTerminationTlvSerializer(final Class<? extends Tlv> tlvClass,
            final BmpTlvSerializer serializer) {
        return bmpTerminationTlvRegistry.registerBmpTlvSerializer(tlvClass, serializer);
    }

    @Override
    public Registration registerBmpRouteMirroringTlvParser(final int tlvType, final BmpTlvParser parser) {
        return bmpRouteMirroringTlvRegistry.registerBmpTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerBmpRouteMirroringTlvSerializer(final Class<? extends Tlv> tlvClass,
            final BmpTlvSerializer serializer) {
        return bmpRouteMirroringTlvRegistry.registerBmpTlvSerializer(tlvClass, serializer);
    }

    @Override
    public BmpTlvRegistry getBmpStatisticsTlvRegistry() {
        return bmpStatisticsTlvRegistry;
    }

    @Override
    public BmpTlvRegistry getBmpInitiationTlvRegistry() {
        return bmpInitiationTlvRegistry;
    }

    @Override
    public BmpTlvRegistry getBmpPeerUpTlvRegistry() {
        return bmpPeerUpTlvRegistry;
    }

    @Override
    public BmpTlvRegistry getBmpTerminationTlvRegistry() {
        return bmpTerminationTlvRegistry;
    }

    @Override
    public BmpTlvRegistry getBmpRouteMirroringTlvRegistry() {
        return bmpRouteMirroringTlvRegistry;
    }
}
