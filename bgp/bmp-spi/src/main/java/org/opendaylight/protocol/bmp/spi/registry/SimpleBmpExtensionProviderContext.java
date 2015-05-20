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
import org.opendaylight.yangtools.yang.binding.Notification;

public class SimpleBmpExtensionProviderContext implements BmpExtensionConsumerContext, BmpExtensionProviderContext {

    private final BmpMessageRegistry bmpMessageRegistry = new SimpleBmpMessageRegistry();

    @Override
    public AutoCloseable registerBmpMessageParser(final int messageType, final BmpMessageParser parser) {
        return this.bmpMessageRegistry.registerBmpMessageParser(messageType, parser);
    }

    @Override
    public AutoCloseable registerBmpMessageSerializer(final Class<? extends Notification> messageClass, final BmpMessageSerializer serializer) {
        return this.bmpMessageRegistry.registerBmpMessageSerializer(messageClass, serializer);
    }

    @Override
    public BmpMessageRegistry getBmpMessageRegistry() {
        return this.bmpMessageRegistry;
    }

}
