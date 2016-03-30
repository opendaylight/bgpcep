/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.cli;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.Set;
import javax.management.JMX;
import javax.management.ObjectName;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;

@Command(scope = "bgp", name = "reset-stats", description = "Reset BGP stats.")
public class ResetBGPStatsCommandProvider extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        final PrintStream out = session.getConsole();

        try{
            final Set<ObjectName> objectNames = AbstractStatsCommandProvider.
                    getObjectNames(ManagementFactory.getPlatformMBeanServer(),
                            AbstractStatsCommandProvider.BGP_PEER_MODULE_PATTERN);

            for (final ObjectName objectName : objectNames) {
                final BGPPeerRuntimeMXBean proxy = JMX.
                        newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
                                objectName, BGPPeerRuntimeMXBean.class);

                proxy.resetStats();
                out.println(String.format("BGP Statistics reset for [%s]!", objectName));
            }
        } catch (final Exception e) {
            out.println("Error getting platform MBeanServer.");
            out.println(e.getMessage());
        }

        return null;
    }

}

