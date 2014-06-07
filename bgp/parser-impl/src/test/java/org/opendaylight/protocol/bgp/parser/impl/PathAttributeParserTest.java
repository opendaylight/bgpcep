/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import io.netty.buffer.Unpooled;

import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;

/*
 * To test incorrect values.
 */
public class PathAttributeParserTest {
    @Test
    public void testOriginParser() throws Exception {
        try {
            ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry().parseAttributes(
                    Unpooled.copiedBuffer(new byte[] { 0x40, 0x01, 0x01, 0x04 }));
            fail("This needs to fail.");
        } catch (final BGPDocumentedException e) {
            assertEquals("Unknown Origin type.", e.getMessage());
            assertArrayEquals(new byte[] { 0x01, 0x01, 0x04 }, e.getData());
        } catch (final BGPParsingException e) {
            fail("This exception should not occur.");
        }
    }
}
