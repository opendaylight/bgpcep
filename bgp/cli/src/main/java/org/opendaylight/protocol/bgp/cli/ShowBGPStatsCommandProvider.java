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
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.protocol.bgp.cli.utils.BgpCliUtils;

@Command(scope = "bgp", name = "show-stats", description = "Shows BGP stats.")
public class ShowBGPStatsCommandProvider extends OsgiCommandSupport {

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

                final BgpSessionState state = proxy.getBgpSessionState();

                BgpCliUtils.displayAll(objectName, state, out);
            }
        } catch (final Exception e) {
            out.println("Error getting platform MBeanServer. \n");
            out.println(e.getMessage());
        }

        return null;
    }

}
