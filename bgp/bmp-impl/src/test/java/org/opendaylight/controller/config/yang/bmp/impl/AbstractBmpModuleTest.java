/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bmp.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import org.junit.Before;
import org.mockito.Mockito;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderContext;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Abstract base class for bmp module tests.
 *
 * @author Thomas Pantelis
 */
public class AbstractBmpModuleTest extends AbstractConfigTest {
    @Before
    public void setUp() throws Exception {
        doAnswer(invocation -> {
            final String str = invocation.getArgumentAt(0, String.class);
            final Filter mockFilter = mock(Filter.class);
            doReturn(str).when(mockFilter).toString();
            return mockFilter;
        }).when(this.mockedContext).createFilter(anyString());

        Mockito.doNothing().when(this.mockedContext).addServiceListener(any(ServiceListener.class), Mockito.anyString());
        Mockito.doNothing().when(this.mockedContext).removeServiceListener(any(ServiceListener.class));

        BGPExtensionProviderContext mockContext = mock(BGPExtensionProviderContext.class);
        doReturn(mock(AddressFamilyRegistry.class)).when(mockContext).getAddressFamilyRegistry();
        doReturn(mock(MessageRegistry.class)).when(mockContext).getMessageRegistry();
        doReturn(mock(SubsequentAddressFamilyRegistry.class)).when(mockContext).getSubsequentAddressFamilyRegistry();
        setupMockService(BGPExtensionProviderContext.class, mockContext);

        setupMockService(BmpExtensionProviderContext.class, new SimpleBmpExtensionProviderContext());

        setupMockService(RIBExtensionProviderContext.class, new SimpleRIBExtensionProviderContext());
    }

    void setupMockService(final Class<?> serviceInterface, final Object instance) throws Exception {
        final ServiceReference<?> mockServiceRef = mock(ServiceReference.class);
        doReturn(new ServiceReference[]{mockServiceRef}).when(this.mockedContext).
                getServiceReferences(anyString(), contains(serviceInterface.getName()));
        doReturn(new ServiceReference[]{mockServiceRef}).when(this.mockedContext).
                getServiceReferences(serviceInterface.getName(), null);
        doReturn(instance).when(this.mockedContext).getService(mockServiceRef);
    }
}
