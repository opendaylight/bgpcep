/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.linkstate.attribute.objects;

import static org.junit.Assert.assertArrayEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.TELspAttributeUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.te.lsp.rev150706.TeLspObject;
import junit.framework.TestCase;

public class BGPSessionAttributeObjectType7ParserTest extends TestCase {
    @Test
    public void testParser() throws BGPParsingException {
        final BGPSessionAttributeObjectType7Parser parser = new BGPSessionAttributeObjectType7Parser();
        final TeLspObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TELspAttributeUtil.TE_LSP_SESSION_C7, 4, TELspAttributeUtil.TE_LSP_SESSION_C7
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TELspAttributeUtil.TE_LSP_SESSION_C7, ByteArray.getAllBytes(output));
    }
}