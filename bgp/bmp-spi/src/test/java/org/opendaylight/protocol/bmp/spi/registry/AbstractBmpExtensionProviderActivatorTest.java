package org.opendaylight.protocol.bmp.spi.registry;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;

public class AbstractBmpExtensionProviderActivatorTest {

    private final SimpleAbstractBmpExtensionProviderActivator activator = new SimpleAbstractBmpExtensionProviderActivator();
    private static final SimpleBmpExtensionProviderContext context = new SimpleBmpExtensionProviderContext();

    @Test
    public void testStartActivator() throws BmpDeserializationException {
        activator.start(context);
        activator.close();
    }

    @Test(expected=IllegalStateException.class)
    public void testStopActivator() {
        activator.close();
    }

    static class SimpleAbstractBmpExtensionProviderActivator extends AbstractBmpExtensionProviderActivator {

        @Override
        protected List<AutoCloseable> startImpl(BmpExtensionProviderContext context) {
            return new ArrayList<AutoCloseable>();
        }
    }

}
