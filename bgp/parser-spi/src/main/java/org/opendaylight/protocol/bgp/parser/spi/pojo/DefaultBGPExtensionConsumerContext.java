/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.ServiceLoader;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Starts and stops {@link BGPExtensionProviderActivator} instances for an {@link BGPExtensionProviderContext}.
 */
@Singleton
@Component(immediate = true, service = BGPExtensionConsumerContext.class)
@MetaInfServices
public final class DefaultBGPExtensionConsumerContext implements BGPExtensionConsumerContext {
    private final @NonNull SimpleBGPExtensionProviderContext delegate = new SimpleBGPExtensionProviderContext();

    public DefaultBGPExtensionConsumerContext() {
        this(ImmutableList.copyOf(ServiceLoader.load(BGPExtensionProviderActivator.class)));
    }

    @Inject
    @Activate
    public DefaultBGPExtensionConsumerContext(
            @Reference(policyOption = ReferencePolicyOption.GREEDY)
            final List<BGPExtensionProviderActivator> extensionActivators) {
        extensionActivators.forEach(activator -> activator.start(delegate));
    }

    @Override
    public AddressFamilyRegistry getAddressFamilyRegistry() {
        return delegate.getAddressFamilyRegistry();
    }

    @Override
    public AttributeRegistry getAttributeRegistry() {
        return delegate.getAttributeRegistry();
    }

    @Override
    public CapabilityRegistry getCapabilityRegistry() {
        return delegate.getCapabilityRegistry();
    }

    @Override
    public MessageRegistry getMessageRegistry() {
        return delegate.getMessageRegistry();
    }

    @Override
    public NlriRegistry getNlriRegistry() {
        return delegate.getNlriRegistry();
    }

    @Override
    public ParameterRegistry getParameterRegistry() {
        return delegate.getParameterRegistry();
    }

    @Override
    public SubsequentAddressFamilyRegistry getSubsequentAddressFamilyRegistry() {
        return delegate.getSubsequentAddressFamilyRegistry();
    }

    @Override
    public ExtendedCommunityRegistry getExtendedCommunityRegistry() {
        return delegate.getExtendedCommunityRegistry();
    }

    @Override
    public BgpPrefixSidTlvRegistry getBgpPrefixSidTlvRegistry() {
        return delegate.getBgpPrefixSidTlvRegistry();
    }
}
