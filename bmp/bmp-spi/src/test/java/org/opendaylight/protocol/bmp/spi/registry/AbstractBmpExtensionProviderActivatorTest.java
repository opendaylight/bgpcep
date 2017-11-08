/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.spi.registry;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;

public class AbstractBmpExtensionProviderActivatorTest {

    private final SimpleAbstractBmpExtensionProviderActivator activator =
            new SimpleAbstractBmpExtensionProviderActivator();
    private static final SimpleBmpExtensionProviderContext CONTEXT = new SimpleBmpExtensionProviderContext();


    @Test
    public void testStartActivator() throws BmpDeserializationException {
        this.activator.start(CONTEXT);
        this.activator.close();
    }

    @Test(expected = IllegalStateException.class)
    public void testStopActivator() {
        this.activator.close();
    }

    private static class SimpleAbstractBmpExtensionProviderActivator extends AbstractBmpExtensionProviderActivator {
        @Override
        protected List<AutoCloseable> startImpl(final BmpExtensionProviderContext context) {
            final List<AutoCloseable> reg = new ArrayList<>();
            reg.add(context.registerBmpMessageParser(1, new SimpleBmpMessageRegistry()));
            return reg;
        }
    }

}