/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static com.google.common.base.Verify.verifyNotNull;

import java.util.List;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import org.kohsuke.MetaInfServices;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(immediate = true, service = RIBExtensionConsumerContext.class)
@MetaInfServices
public final class OSGiRIBExtensionConsumerContext extends ForwardingRIBExtensionConsumerContext  {

    private SimpleRIBExtensionProviderContext delegate;

    @Inject
    @Activate
    public OSGiRIBExtensionConsumerContext(final @Reference BindingNormalizedNodeSerializer mappingCodec,
            final @Reference(policyOption = ReferencePolicyOption.GREEDY)
                    List<RIBExtensionProviderActivator> extensionActivators) {
        delegate = new SimpleRIBExtensionProviderContext();
        extensionActivators.forEach(activator -> activator.startRIBExtensionProvider(delegate, mappingCodec));
    }

    @Deactivate
    @PreDestroy
    void deactivate() {
        delegate = null;
    }

    @Override
    RIBExtensionProviderContext delegate() {
        return verifyNotNull(delegate);
    }
}
