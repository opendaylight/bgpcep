package org.opendaylight.protocol.bmp.spi.registry;

import static org.junit.Assert.assertArrayEquals;
import static org.opendaylight.protocol.bmp.impl.message.InitiationHandlerTest.createInitMsg;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bmp.impl.BmpActivator;
import org.opendaylight.protocol.util.ByteArray;

public class AbstractBmpExtensionProviderActivatorTest {

    private static BmpMessageRegistry messageRegistry;
    private static BmpActivator act;

    @BeforeClass
    public static void init() {
        final BGPActivator bgpActivator = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        bgpActivator.start(context);
        final SimpleBmpExtensionProviderContext ctx = new SimpleBmpExtensionProviderContext();
        act = new BmpActivator(context.getMessageRegistry());
        act.start(ctx);
        messageRegistry = ctx.getBmpMessageRegistry();
    }

    @Test
    public void testStop() {
        act.close();
        ByteBuf data = Unpooled.buffer();
        messageRegistry.serializeMessage(createInitMsg(), data);
        assertArrayEquals(new byte[] {}, ByteArray.getAllBytes(data));
    }
}
