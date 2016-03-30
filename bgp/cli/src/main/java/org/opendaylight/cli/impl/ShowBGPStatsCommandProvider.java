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

import javax.management.ObjectName;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.cli.utils.BgpCliUtils;


@Command(scope = "bgp", name = "show-stats", description = "Shows BGP stats.")
public class ShowBGPStatsCommandProvider extends OsgiCommandSupport {

    @Argument(index = 0, name = "ipAddress", description = "IP Address", required = true, multiValued = false)
    String ipAddress;

    @Override
    protected Object doExecute() throws Exception {
        PrintStream out = session.getConsole();

        ObjectName mbeanName = new ObjectName(BgpCliUtils.BGP_MBEANS_NAME);

        // Read attribute
        Object message = null;

        try{
            message = ManagementFactory.getPlatformMBeanServer().getAttribute(mbeanName, BgpCliUtils.BGP_MBEANS_GET_STATS_OPERATION);
        } catch (Exception e) {
            out.println("Error getting platform MBeanServer. \n");
            out.println(e.getStackTrace());
        }

        if (message == null){
            out.println("No BgpPeerState found.");
        }

        return null;
    }

}
