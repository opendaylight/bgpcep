/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.Registration;

public class SimpleRIBExtensionProviderContextActivatorTest extends AbstractRIBActivatorTest {
    private static boolean RIBACTIVATED;

    @Test
    public void test() {
        final List<RIBExtensionProviderActivator> extensionActivators = Collections.singletonList(new RibActivator());
        final SimpleRIBExtensionProviderContextActivator activator =
                new SimpleRIBExtensionProviderContextActivator(new SimpleRIBExtensionProviderContext(),
                        extensionActivators, context.currentSerializer());
        activator.start();
        assertTrue(RIBACTIVATED);
        activator.close();
        assertFalse(RIBACTIVATED);
    }

    private static class RibActivator extends AbstractRIBExtensionProviderActivator {
        @Override
        protected List<Registration> startRIBExtensionProviderImpl(final RIBExtensionProviderContext context,
                final BindingNormalizedNodeSerializer mappingService) {
            RIBACTIVATED = true;
            return Collections.singletonList(() -> RIBACTIVATED = false);
        }
    }
}