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

public interface BmpTlvRegistrator {

    Registration registerBmpTlvParser(int tlvType, BmpTlvParser parser);

    Registration registerBmpTlvSerializer(Class<? extends Tlv> tlvClass, BmpTlvSerializer serializer);
}
