/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.AbstractBGPStatementProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryProvider;
import org.opendaylight.protocol.bgp.route.targetcontrain.impl.route.policy.ClientAttributePrependHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.ClientAttributePrepend;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

@Singleton
@Component(immediate = true, service = StatementProviderActivator.class,
           property = "type=org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.StatementActivator")
@MetaInfServices(value = StatementProviderActivator.class)
public final class StatementActivator extends AbstractBGPStatementProviderActivator {
    @Inject
    public StatementActivator() {
        // Exposed for DI
    }

    @Override
    protected synchronized List<Registration> startImpl(final StatementRegistryProvider provider) {
        return List.of(
            provider.registerBgpActionAugmentationPolicy(ClientAttributePrepend.class,
                ClientAttributePrependHandler.getInstance()));
    }
}
