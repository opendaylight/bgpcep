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
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;
import org.opendaylight.protocol.bgp.cli.utils.BgpCliUtils;

@Command(scope = "bgp", name = "reset-stats", description = "Reset BGP stats.")
public class ResetBGPStatsCommandProvider extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        final PrintStream out = session.getConsole();
        ObjectName managedBeanName = new ObjectName(BgpCliUtils.BGP_MBEANS_NAME);

        try{
            final MBeanServerConnection conn =
                    ManagementFactory.getPlatformMBeanServer();

            final BGPPeerRuntimeMXBean proxy = JMX
                    .newMXBeanProxy(conn, managedBeanName, BGPPeerRuntimeMXBean.class);
            proxy.resetStats();
            out.println("BGP Statistics reset!");
        } catch (final Exception e) {
            out.println("Error getting platform MBeanServer.");
            out.println(e.getMessage());
            return null;
        }

        return null;
    }

}

