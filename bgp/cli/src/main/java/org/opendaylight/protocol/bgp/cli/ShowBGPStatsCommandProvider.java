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

import org.apache.felix.gogo.commands.Command;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.protocol.bgp.cli.utils.BgpCliUtils;

@Command(scope = "bgp", name = "show-stats", description = "Shows BGP stats.")
public class ShowBGPStatsCommandProvider extends AbstractStatsCommandProvider {

    @Override
    protected void onExecution(@Nonnull PrintStream out, @Nonnull BGPPeerRuntimeMXBean peerRuntimeMXBean,
                               @Nonnull ObjectName objectName) {
        final BgpSessionState state = peerRuntimeMXBean.getBgpSessionState();
        BgpCliUtils.displayAll(objectName, state, out);
    }
}
