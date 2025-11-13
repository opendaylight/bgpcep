/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.spi.registry;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.ServiceLoader;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Singleton
@Component(immediate = true, service = BmpExtensionConsumerContext.class)
@MetaInfServices
public final class DefaultBmpExtensionConsumerContext implements BmpExtensionConsumerContext {
    private final @NonNull SimpleBmpExtensionProviderContext delegate = new SimpleBmpExtensionProviderContext();

    public DefaultBmpExtensionConsumerContext() {
        this(ImmutableList.copyOf(ServiceLoader.load(BmpExtensionProviderActivator.class)));
    }

    @Inject
    @Activate
    public DefaultBmpExtensionConsumerContext(
            @Reference(policyOption = ReferencePolicyOption.GREEDY)
            final List<BmpExtensionProviderActivator> extensionActivators) {
        extensionActivators.forEach(activator -> activator.start(delegate));
    }

    @Override
    public BmpMessageRegistry getBmpMessageRegistry() {
        return delegate.getBmpMessageRegistry();
    }

    @Override
    public BmpTlvRegistry getBmpStatisticsTlvRegistry() {
        return delegate.getBmpStatisticsTlvRegistry();
    }

    @Override
    public BmpTlvRegistry getBmpInitiationTlvRegistry() {
        return delegate.getBmpInitiationTlvRegistry();
    }

    @Override
    public BmpTlvRegistry getBmpPeerUpTlvRegistry() {
        return delegate.getBmpPeerUpTlvRegistry();
    }

    @Override
    public BmpTlvRegistry getBmpTerminationTlvRegistry() {
        return delegate.getBmpTerminationTlvRegistry();
    }

    @Override
    public BmpTlvRegistry getBmpRouteMirroringTlvRegistry() {
        return delegate.getBmpRouteMirroringTlvRegistry();
    }
}
