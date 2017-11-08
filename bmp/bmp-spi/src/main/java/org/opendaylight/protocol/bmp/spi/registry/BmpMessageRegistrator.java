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

public interface BmpMessageRegistrator {

    AutoCloseable registerBmpMessageParser(int messageType, BmpMessageParser parser);

    AutoCloseable registerBmpMessageSerializer(Class<? extends Notification> messageClass,
            BmpMessageSerializer serializer);
}
