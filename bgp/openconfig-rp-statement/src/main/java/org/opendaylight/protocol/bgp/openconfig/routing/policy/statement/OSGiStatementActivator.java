/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import static com.google.common.base.Verify.verifyNotNull;

import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.AbstractBGPStatementProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryProvider;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = StatementProviderActivator.class,
           property = "type=org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.StatementActivator")
public final class OSGiStatementActivator extends AbstractBGPStatementProviderActivator {
    @Reference
    DataBroker dataBroker;

    private StatementActivator delegate;

    @Override
    protected List<? extends Registration> startImpl(final StatementRegistryProvider context) {
        return verifyNotNull(delegate).startImpl(context);
    }

    @Activate
    void activate() {
        delegate = new StatementActivator(dataBroker);
    }

    @Deactivate
    void deactivate() {
        delegate = null;
    }
}
