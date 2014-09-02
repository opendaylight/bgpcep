/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.collect.Lists;
import java.util.List;
import junit.framework.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class SimpleRIBExtensionTest {

    @Test
    public void testExtensionProvider() {
        final ServiceLoaderRIBExtensionConsumerContext ctx = ServiceLoaderRIBExtensionConsumerContext.createConsumerContext();
        Assert.assertNull(ctx.getAdjRIBsInFactory(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        final TestActivator act = new TestActivator();
        act.startRIBExtensionProvider(ctx);
        Assert.assertNotNull(ctx.getAdjRIBsInFactory(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        act.close();
        Assert.assertNull(ctx.getAdjRIBsInFactory(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        ctx.close();
    }

    private final class TestActivator extends AbstractRIBExtensionProviderActivator {
        @Override
        protected List<AutoCloseable> startRIBExtensionProviderImpl(final RIBExtensionProviderContext context) {
            return Lists.newArrayList(context.registerAdjRIBsInFactory(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, new AdjRIBsFactory() {
                @Override
                public AdjRIBsIn<?, ?> createAdjRIBs(final KeyedInstanceIdentifier<Tables, TablesKey> basePath) {
                    return Mockito.mock(AbstractAdjRIBs.class);
                }
            }));
        }
    }
}
