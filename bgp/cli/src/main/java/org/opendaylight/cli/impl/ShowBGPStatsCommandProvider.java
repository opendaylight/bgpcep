/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.cli.impl;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.cli.utils.BgpCliUtils;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;

@Command(scope = "bgp", name = "show-stats", description = "Shows BGP stats.")
public class ShowBGPStatsCommandProvider extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        PrintStream out = session.getConsole();

        ObjectName managedBeanName = new ObjectName(BgpCliUtils.BGP_MBEANS_NAME);

        final MBeanServerConnection conn;
        final BGPPeerRuntimeMXBean proxy;
        try{
            conn =
                    ManagementFactory.getPlatformMBeanServer();
            proxy = JMX.newMBeanProxy(conn, managedBeanName, BGPPeerRuntimeMXBean.class);
        } catch (Exception e) {
            out.println("Error getting platform MBeanServer. \n");
            out.println(e.getMessage());
            return null;
        }

        BgpSessionState state = proxy.getBgpSessionState();

        if (state != null) {
            BgpCliUtils.displayAll(state);
        } else {
            out.println("No BgpSessionState found.");
            return null;
        }
        return null;
    }

}
