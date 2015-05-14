import com.google.common.collect.Lists;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;

import org.junit.Test;
import org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl.message.TerminationHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Termination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessageBuilder;

/**
 * Created by cgasparini on 14.5.2015.
 */
public class HandlerTest {
    @Test
    public void testParserHandler() throws Exception {

        final byte[] tMessageBytes = {
            (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x74, (byte) 0x65, (byte) 0x73,
            (byte) 0x74, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x74, (byte) 0x65, (byte) 0x73,
            (byte) 0x74, (byte) 0x31, (byte) 0x00
        };

        TerminationMessageBuilder tMessage = new TerminationMessageBuilder();
        final List<String> TLVstring = Lists.newArrayList();
        TLVstring.add("test0");
        TLVstring.add("test1");
        final ByteBuf result = Unpooled.buffer();
        new TerminationHandler().serializeMessage(tMessage.setReason(Termination.Reason.forValue(0)).setStringInfo
            (TLVstring).build(), result);
        //Assert.assertArrayEquals(tMessageBytes, result.array());
    }
}
