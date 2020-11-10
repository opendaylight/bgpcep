/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.Registration;

public class DefaultRIBExtensionProviderContextTest extends AbstractRIBActivatorTest {
    private volatile boolean ribActivated;

    @Test
    public void test() {
        new DefaultRIBExtensionConsumerContext(context.currentSerializer(), new RibActivator());
        assertTrue(ribActivated);
    }

    private class RibActivator implements RIBExtensionProviderActivator {
        @Override
        public List<Registration> startRIBExtensionProvider(final RIBExtensionProviderContext context,
                final BindingNormalizedNodeSerializer mappingService) {
            ribActivated = true;
            return List.of(() -> ribActivated = false);
        }
    }
}
