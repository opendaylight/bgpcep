/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgp.rib.impl.karaf;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;

import java.io.PrintStream;


@Command(scope = "bgp", name = "show-stats", description = "Shows BGP stats.")
public class ShowBGPStatsCommandProvider extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        PrintStream out = session.getConsole();
        final BgpSessionState state = null;

        final StringBuilder result = new StringBuilder();

        if (state != null){
            result.append("Hold time: " + state.getHoldtimeCurrent().intValue());
            result.append("\n");
            result.append("Keep alive: " + state.getKeepaliveCurrent().intValue());
            result.append("\n");
            result.append("Session state: " + state.getSessionState());
            result.append("\n");
            result.append("Peer Address: " + state.getPeerPreferences().getAddress());
            result.append("\n");
            result.append("Peer AS: " + state.getPeerPreferences().getAs());
            result.append("\n");
            result.append("Gr Capability " + state.getPeerPreferences().getGrCapability());
            result.append("\n");
            result.append("Speaker Address: " + state.getSpeakerPreferences().getAddress());
            result.append("\n");
            result.append("Speaker Port: " + state.getSpeakerPreferences().getPort().intValue());
            result.append("\n");
            result.append("Total Msgs received: " + state.getMessagesStats().getTotalMsgs().getReceived().getCount().longValue());
            result.append("\n");
            result.append("Total Msgs sent: " + state.getMessagesStats().getTotalMsgs().getSent().getCount().longValue());
            result.append("\n");
            out.print(result.toString());
        } else{
            result.append("No data available.");
            out.print(result.toString());
        }

        return null;
    }

}
