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

        ObjectName managedBeanName = new ObjectName(BgpCliUtils.BGP_MBEANS_NAME);

        // Read attribute
        Object message = null;

        try{
            message = ManagementFactory.getPlatformMBeanServer()
                    .getAttribute(managedBeanName, BgpCliUtils.BGP_MBEANS_GET_STATS_OPERATION);
        } catch (Exception e) {
            out.println("Error getting platform MBeanServer. \n");
            out.println(e.getStackTrace());
        }

        if (message == null){
            out.println("No BgpPeerState found.");
        }

        final StringBuilder result = new StringBuilder();
        BgpCliUtils.BGPStatisticsData state = BgpCliUtils.parseMessage(message);

        if (state != null) {
            result.append("Hold time: " + state.getHoldtimeCurrent());
            result.append("\n");
            result.append("Keep alive: " + state.getKeepAliveCurrent());
            result.append("\n");
            result.append("Session state: " + state.getSessionState());
            result.append("\n");
            result.append("Peer Address: " + state.getPeerAddress());
            result.append("\n");
            result.append("Peer AS: " + state.getPeerAs());
            result.append("\n");
            result.append("Gr Capability " + state.getPeerGrCapability());
            result.append("\n");
            result.append("Speaker Address: " + state.getSpeakerAddress());
            result.append("\n");
            result.append("Speaker Port: " + state.getSpeakerPort());
            result.append("\n");
            result.append("Total Msgs received: " + state.getTotalMsgsReceived());
            result.append("\n");
            result.append("Total Msgs sent: " + state.getTotalMsgsSent());
            result.append("\n");
            out.print(result.toString());
        }
        return null;
    }

}
