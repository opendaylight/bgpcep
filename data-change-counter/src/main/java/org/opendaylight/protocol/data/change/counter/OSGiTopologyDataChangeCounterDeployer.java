/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.data.change.counter;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public final class OSGiTopologyDataChangeCounterDeployer {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiTopologyDataChangeCounterDeployer.class);

    @Reference
    DataBroker dataBroker;

    private TopologyDataChangeCounterDeployer deployer;

    @Activate
    void activate() {
        deployer = new TopologyDataChangeCounterDeployer(dataBroker);
        deployer.register();
        LOG.info("Topolocy data change counter activated");

    }

    @Deactivate
    void deactivate() {
        deployer.close();
        LOG.info("Topolocy data change counter deactivated");
    }
}
