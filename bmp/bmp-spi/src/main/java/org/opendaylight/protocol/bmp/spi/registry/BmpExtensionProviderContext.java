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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.Tlv;
import org.opendaylight.yangtools.concepts.Registration;

public interface BmpExtensionProviderContext extends BmpMessageRegistrator, BmpExtensionConsumerContext {

    Registration registerBmpStatisticsTlvParser(int tlvType, BmpTlvParser parser);

    Registration registerBmpStatisticsTlvSerializer(Class<? extends Tlv> tlvClass, BmpTlvSerializer serializer);

    Registration registerBmpInitiationTlvParser(int tlvType, BmpTlvParser parser);

    Registration registerBmpInitiationTlvSerializer(Class<? extends Tlv> tlvClass, BmpTlvSerializer serializer);

    Registration registerBmpPeerUpTlvParser(int tlvType, BmpTlvParser parser);

    Registration registerBmpPeerUpTlvSerializer(Class<? extends Tlv> tlvClass, BmpTlvSerializer serializer);

    Registration registerBmpTerminationTlvParser(int tlvType, BmpTlvParser parser);

    Registration registerBmpTerminationTlvSerializer(Class<? extends Tlv> tlvClass, BmpTlvSerializer serializer);

    Registration registerBmpRouteMirroringTlvParser(int tlvType, BmpTlvParser parser);

    Registration registerBmpRouteMirroringTlvSerializer(Class<? extends Tlv> tlvClass, BmpTlvSerializer serializer);
}
