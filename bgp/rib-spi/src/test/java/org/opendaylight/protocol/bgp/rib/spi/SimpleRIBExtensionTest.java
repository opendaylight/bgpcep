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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class SimpleRIBExtensionTest {

    @Test
    public void testExtensionProvider() {
        final ServiceLoaderRIBExtensionConsumerContext ctx = ServiceLoaderRIBExtensionConsumerContext.createConsumerContext();
        Assert.assertNull(ctx.getRIBSupport(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        final TestActivator act = new TestActivator();
        act.startRIBExtensionProvider(ctx);
        Assert.assertNotNull(ctx.getRIBSupport(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        act.close();
        Assert.assertNull(ctx.getRIBSupport(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        ctx.close();
    }

    private final class TestActivator extends AbstractRIBExtensionProviderActivator {
        @Override
        protected List<AutoCloseable> startRIBExtensionProviderImpl(final RIBExtensionProviderContext context) {
            RIBSupport support = Mockito.mock(RIBSupport.class);
            Mockito.doReturn(Ipv4Route.class).when(support).routesListClass();
            Mockito.doReturn(Ipv4Routes.class).when(support).routesContainerClass();
            Mockito.doReturn(Ipv4RoutesCase.class).when(support).routesCaseClass();
            return Lists.newArrayList((AutoCloseable)context.registerRIBSupport(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, support));
        }
    }
}
