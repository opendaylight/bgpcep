/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.registry;

import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;

public interface BmpExtensionProviderContext extends BmpMessageRegistrator, BmpExtensionConsumerContext {

    AutoCloseable registerBmpStatisticsTlvParser(int tlvType, BmpTlvParser parser);

    AutoCloseable registerBmpStatisticsTlvSerializer(Class<? extends Tlv> tlvClass, BmpTlvSerializer serializer);

    AutoCloseable registerBmpInitiationTlvParser(int tlvType, BmpTlvParser parser);

    AutoCloseable registerBmpInitiationTlvSerializer(Class<? extends Tlv> tlvClass, BmpTlvSerializer serializer);

    AutoCloseable registerBmpPeerUpTlvParser(int tlvType, BmpTlvParser parser);

    AutoCloseable registerBmpPeerUpTlvSerializer(Class<? extends Tlv> tlvClass, BmpTlvSerializer serializer);

    AutoCloseable registerBmpTerminationTlvParser(int tlvType, BmpTlvParser parser);

    AutoCloseable registerBmpTerminationTlvSerializer(Class<? extends Tlv> tlvClass, BmpTlvSerializer serializer);

    AutoCloseable registerBmpRouteMirroringTlvParser(int tlvType, BmpTlvParser parser);

    AutoCloseable registerBmpRouteMirroringTlvSerializer(Class<? extends Tlv> tlvClass, BmpTlvSerializer serializer);
}
