/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgp.rib.impl.karaf;

import java.io.PrintStream;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionStats;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionStatistics;

@Command(scope = "bgp", name = "show-stats", description = "Shows BGP stats.")
public class ShowBGPStatsCommandProvider extends OsgiCommandSupport {

    @Argument(index = 0, name = "ipAddress", description = "IP Address", required = true, multiValued = false)
    String ipAddress;

    @Override
    protected Object doExecute() throws Exception {
        PrintStream out = session.getConsole();

        final BGPSessionStatistics state = BGPSessionStats.getBgpSession(ipAddress);

        final StringBuilder result = new StringBuilder();

        if (state != null && state.getBgpSesionState() != null){
            result.append("Hold time: " + state.getBgpSesionState().getHoldtimeCurrent().intValue());
            result.append("\n");
            result.append("Keep alive: " + state.getBgpSesionState().getKeepaliveCurrent().intValue());
            result.append("\n");
            result.append("Session state: " + state.getBgpSesionState().getSessionState());
            result.append("\n");
            result.append("Peer Address: " + state.getBgpSesionState().getPeerPreferences().getAddress());
            result.append("\n");
            result.append("Peer AS: " + state.getBgpSesionState().getPeerPreferences().getAs());
            result.append("\n");
            result.append("Gr Capability " + state.getBgpSesionState().getPeerPreferences().getGrCapability());
            result.append("\n");
            result.append("Speaker Address: " + state.getBgpSesionState().getSpeakerPreferences().getAddress());
            result.append("\n");
            result.append("Speaker Port: " + state.getBgpSesionState().getSpeakerPreferences().getPort().intValue());
            result.append("\n");
            result.append("Total Msgs received: " + state.getBgpSesionState().getMessagesStats().getTotalMsgs().getReceived().getCount().longValue());
            result.append("\n");
            result.append("Total Msgs sent: " + state.getBgpSesionState().getMessagesStats().getTotalMsgs().getSent().getCount().longValue());
            result.append("\n");
            out.print(result.toString());
        } else{
            result.append("No data available.");
            out.print(result.toString());
        }

        return null;
    }

}
