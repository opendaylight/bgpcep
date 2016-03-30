/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.cli;

import java.io.IOException;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerModuleFactory;

public class AbstractStatsCommandProvider {
    public static final ObjectName BGP_PEER_MODULE_PATTERN = ObjectNameUtil.createRuntimeBeanPattern(BGPPeerModuleFactory.NAME, null);

    public static Set<ObjectName> getObjectNames(final MBeanServer mBeanServer, final ObjectName moduleName) throws IOException {
        return mBeanServer.queryNames(moduleName, null);
    }
}
