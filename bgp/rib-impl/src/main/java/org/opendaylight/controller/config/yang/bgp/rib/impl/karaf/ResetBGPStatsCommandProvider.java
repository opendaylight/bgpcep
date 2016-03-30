/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgp.rib.impl.karaf;

import java.io.PrintStream;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionStats;

@Command(scope = "bgp", name = "reset-stats", description = "Reset BGP stats.")
public class ResetBGPStatsCommandProvider extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        PrintStream out = session.getConsole();
        BGPSessionStats.resetStats();

        out.print("BGP Statistics reset!");
        return null;
    }

}
