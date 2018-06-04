/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class SimpleRIBExtensionTest extends AbstractConcurrentDataBrokerTest {
    private BindingToNormalizedNodeCodec codec;

    @Override
    protected final AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final AbstractDataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        this.codec = customizer.getBindingToNormalized();
        return customizer;
    }

    @Test
    public void testExtensionProvider() {
        final ServiceLoaderRIBExtensionConsumerContext ctx =
                ServiceLoaderRIBExtensionConsumerContext.createConsumerContext(this.codec);
        Assert.assertNull(ctx.getRIBSupport(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        final TestActivator act = new TestActivator();
        act.startRIBExtensionProvider(ctx, this.codec);
        Assert.assertNotNull(ctx.getRIBSupport(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        act.close();
        Assert.assertNull(ctx.getRIBSupport(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        ctx.close();
    }

    private final class TestActivator extends AbstractRIBExtensionProviderActivator {
        @Override
        protected List<AutoCloseable> startRIBExtensionProviderImpl(
                final RIBExtensionProviderContext context,
                final BindingNormalizedNodeSerializer mappingService) {
            final RIBSupport<?, ?, ?, ?> support = Mockito.mock(RIBSupport.class);
            Mockito.doReturn(Route.class).when(support).routesListClass();
            Mockito.doReturn(DataObject.class).when(support).routesContainerClass();
            Mockito.doReturn(DataObject.class).when(support).routesCaseClass();
            return Collections.singletonList((AutoCloseable) context.registerRIBSupport(Ipv4AddressFamily.class,
                    UnicastSubsequentAddressFamily.class, support));
        }
    }
}
