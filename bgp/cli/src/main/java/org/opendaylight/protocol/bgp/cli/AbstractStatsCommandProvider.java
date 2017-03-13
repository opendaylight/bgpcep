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
import javax.annotation.Nonnull;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerModuleFactory;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;

public abstract class AbstractStatsCommandProvider extends OsgiCommandSupport {
    private static final ObjectName BGP_PEER_MODULE_PATTERN = ObjectNameUtil.createRuntimeBeanPattern(BGPPeerModuleFactory.NAME, null);

    private static final MBeanServer MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();

    @Override
    protected final Object doExecute() throws Exception {
        final PrintStream out = this.session.getConsole();
        getObjectNames().forEach(objectName -> onExecution(out, getRuntimeMXBean(objectName), objectName));
        return null;
    }

    protected abstract void onExecution(@Nonnull PrintStream out, @Nonnull BGPPeerRuntimeMXBean peerRuntimeMXBean, @Nonnull ObjectName objectName);

    private static Set<ObjectName> getObjectNames() throws IOException {
        return MBEAN_SERVER.queryNames(BGP_PEER_MODULE_PATTERN, null);
    }

    private static final BGPPeerRuntimeMXBean getRuntimeMXBean(final ObjectName objectName) {
        return JMX.newMXBeanProxy(MBEAN_SERVER, objectName, BGPPeerRuntimeMXBean.class);
    }
}
