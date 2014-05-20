/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yangtools.yang.binding.DataObject;
import static org.junit.Assert.assertTrue;

public class BGPUpdateAttributesSerializationTest {

    private ByteBuf byteAggregator;

    static final List<byte[]> inputBytes = new ArrayList<byte[]>();
    private static BGPUpdateMessageParser updateParser = new BGPUpdateMessageParser(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry());
    private Update message;

    private static int COUNTER = 8;//17;

    private static int MAX_SIZE = 300;


    @Before
    public void setupUpdateMessage() throws Exception{

        for (int i = 1; i <= COUNTER; i++) {
            final String name = "/up" + i + ".bin";
            final InputStream is = BGPParserTest.class.getResourceAsStream(name);
            if (is == null) {
                throw new IOException("Failed to get resource " + name);
            }

            final ByteArrayOutputStream bis = new ByteArrayOutputStream();
            final byte[] data = new byte[MAX_SIZE];
            int nRead = 0;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                bis.write(data, 0, nRead);
            }
            bis.flush();

            inputBytes.add(bis.toByteArray());
        }


    }

    private void readUpdateMesageFromList (int listIndex) throws BGPDocumentedException {
        final byte[] body = ByteArray.cutBytes(inputBytes.get(listIndex), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(inputBytes.get(listIndex), MessageUtil.MARKER_LENGTH,
                MessageUtil.LENGTH_FIELD_LENGTH));
        message =  BGPUpdateAttributesSerializationTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);
    }

    @Test
    public void testUpdateMessageSerialization() throws BGPDocumentedException {
        for (int i=0;i<COUNTER;i++){
            readUpdateMesageFromList(i);
            byteAggregator = updateParser.serializeMessage(message);
            System.out.println("Serialized :"+asHexDump(byteAggregator));
            System.out.println("Original   :"+asHexDump(inputBytes.get(i)));
            assertTrue(Arrays.equals(byteAggregator.array(), inputBytes.get(i)));
        }

    }
    private void serialize(AttributeSerializer serializer,DataObject dataObject){
        byteAggregator = Unpooled.buffer(0);
        serializer.serializeAttribute(dataObject,byteAggregator);
    }

    private String asHexDump(byte[] bytes){
      return asHexDump(Unpooled.copiedBuffer(bytes));
    }
    private String asHexDump(ByteBuf bytes){
        return ByteBufUtil.hexDump(bytes);
    }

}
