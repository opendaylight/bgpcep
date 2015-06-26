package org.opendaylight.protocol.bmp.spi.parser;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;

public class BmpMessageConstantsTest {

    @Test(expected=UnsupportedOperationException.class)
    public void testBmpMessageConstantsPrivateConstructor() throws Throwable {
        final Constructor<BmpMessageConstants> c = BmpMessageConstants.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
