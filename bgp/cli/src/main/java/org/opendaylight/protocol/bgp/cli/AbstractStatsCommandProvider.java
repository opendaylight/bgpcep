/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.Set;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerModuleFactory;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;

public class AbstractStatsCommandProvider {
    public static final ObjectName BGP_PEER_MODULE_PATTERN = ObjectNameUtil.createRuntimeBeanPattern(BGPPeerModuleFactory.NAME, null);

    final Set<ObjectName> getObjectNames() throws IOException {
        return ManagementFactory.getPlatformMBeanServer().queryNames(BGP_PEER_MODULE_PATTERN, null);
    }

    final BGPPeerRuntimeMXBean getProxy(final ObjectName objectName) {
        return JMX.
                newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
                        objectName, BGPPeerRuntimeMXBean.class);
    }

    void execute(final PrintStream printStream) throws IOException, JMException { }
}
