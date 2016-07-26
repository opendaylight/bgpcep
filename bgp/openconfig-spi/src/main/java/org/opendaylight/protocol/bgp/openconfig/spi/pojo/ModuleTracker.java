/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.spi.pojo;

import org.opendaylight.protocol.bgp.openconfig.spi.BGPConfigModuleTracker;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;

public final class ModuleTracker<T extends AbstractInstanceConfiguration> implements BGPConfigModuleTracker {

    private final BGPOpenconfigMapper<T> globalWriter;
    private final T bgpConfig;

    public ModuleTracker(final BGPOpenconfigMapper<T> globalWriter, final T bgpConfig) {
        this.globalWriter = globalWriter;
        this.bgpConfig = bgpConfig;
    }

    @Override
    public void onInstanceCreate() {
        if (globalWriter != null) {
            globalWriter.writeConfiguration(this.bgpConfig);
        }
    }

    @Override
    public void onInstanceClose() {
        if (globalWriter != null) {
            globalWriter.removeConfiguration(this.bgpConfig);
        }
    }
}
