/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.osgi.service.component.annotations.Component;

@Singleton
@Component(immediate = true)
@MetaInfServices
public final class PCEPTopologySessionListenerFactory implements TopologySessionListenerFactory {
    @Inject
    public PCEPTopologySessionListenerFactory() {
        // Visible for DI
    }

    @Override
    public TopologySessionListener createTopologySessionListener(final ServerSessionManager manager) {
        return new PCEPTopologySessionListener(manager);
    }
}
