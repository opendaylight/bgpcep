/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.LabelRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionConsumerContext;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 *  Starts and stops PCEPExtensionProviderActivator instances for a PCEPExtensionProviderContext.
 *
 * @author Thomas Pantelis
 */
@Singleton
@MetaInfServices
@Component(immediate = true)
public final class DefaultPCEPExtensionConsumerContext implements PCEPExtensionConsumerContext {
    private final @NonNull SimplePCEPExtensionProviderContext delegate = new SimplePCEPExtensionProviderContext();

    public DefaultPCEPExtensionConsumerContext() {
        this(ServiceLoader.load(PCEPExtensionProviderActivator.class));
    }

    @VisibleForTesting
    public DefaultPCEPExtensionConsumerContext(final PCEPExtensionProviderActivator... extensionActivators) {
        this(Arrays.asList(extensionActivators));
    }

    @Inject
    public DefaultPCEPExtensionConsumerContext(final Iterable<PCEPExtensionProviderActivator> extensionActivators) {
        extensionActivators.forEach(activator -> activator.start(delegate));
    }

    @Activate
    public DefaultPCEPExtensionConsumerContext(
            @Reference(policyOption = ReferencePolicyOption.GREEDY)
            final List<PCEPExtensionProviderActivator> extensionActivators) {
        extensionActivators.forEach(activator -> activator.start(delegate));
    }

    @Override
    public LabelRegistry getLabelHandlerRegistry() {
        return delegate.getLabelHandlerRegistry();
    }

    @Override
    public MessageRegistry getMessageHandlerRegistry() {
        return delegate.getMessageHandlerRegistry();
    }

    @Override
    public ObjectRegistry getObjectHandlerRegistry() {
        return delegate.getObjectHandlerRegistry();
    }

    @Override
    public EROSubobjectRegistry getEROSubobjectHandlerRegistry() {
        return delegate.getEROSubobjectHandlerRegistry();
    }

    @Override
    public RROSubobjectRegistry getRROSubobjectHandlerRegistry() {
        return delegate.getRROSubobjectHandlerRegistry();
    }

    @Override
    public XROSubobjectRegistry getXROSubobjectHandlerRegistry() {
        return delegate.getXROSubobjectHandlerRegistry();
    }

    @Override
    public TlvRegistry getTlvHandlerRegistry() {
        return delegate.getTlvHandlerRegistry();
    }

    @Override
    public VendorInformationTlvRegistry getVendorInformationTlvRegistry() {
        return delegate.getVendorInformationTlvRegistry();
    }

    @Override
    public VendorInformationObjectRegistry getVendorInformationObjectRegistry() {
        return delegate.getVendorInformationObjectRegistry();
    }
}
