/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.cli;

import java.io.PrintStream;
import javax.annotation.Nonnull;
import javax.management.ObjectName;
import org.apache.karaf.shell.commands.Command;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;

@Command(scope = "bgp", name = "reset-stats", description = "Reset BGP stats.")
public class ResetBGPStatsCommandProvider extends AbstractStatsCommandProvider {

    @Override
    protected void onExecution(@Nonnull PrintStream out, @Nonnull BGPPeerRuntimeMXBean peerRuntimeMXBean,
                               @Nonnull ObjectName objectName) {
        peerRuntimeMXBean.resetSession();
        out.println(String.format("BGP Statistics reset for [%s]!", objectName));
    }
}

