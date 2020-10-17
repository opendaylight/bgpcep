/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.protocols;

import org.opendaylight.bgpcep.config.loader.spi.AbstractOSGiConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
// FIXME: merge with ProtocolsConfigFileProcessor once we have constructor injection
public final class OSGiProtocolsConfigFileProcessor extends AbstractOSGiConfigFileProcessor {
    @Reference
    DataBroker dataBroker;
    @Reference
    ConfigLoader configLoader;

    @Activate
    void activate() {
        start(new ProtocolsConfigFileProcessor(configLoader, dataBroker));
    }

    @Deactivate
    void deactivate() {
        stop();
    }
}
