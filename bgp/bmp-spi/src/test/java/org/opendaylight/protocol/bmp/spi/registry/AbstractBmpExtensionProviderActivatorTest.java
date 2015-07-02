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
        this.activator.start(context);
        this.activator.close();
    }

    @Test(expected=IllegalStateException.class)
    public void testStopActivator() {
        this.activator.close();
    }

    private static class SimpleAbstractBmpExtensionProviderActivator extends AbstractBmpExtensionProviderActivator {
        @Override
        protected List<AutoCloseable> startImpl(final BmpExtensionProviderContext context) {
            final List<AutoCloseable> reg = new ArrayList<AutoCloseable>();
            reg.add(context.registerBmpMessageParser(1, new SimpleBmpMessageRegistry()));
            return reg;
        }
    }

}