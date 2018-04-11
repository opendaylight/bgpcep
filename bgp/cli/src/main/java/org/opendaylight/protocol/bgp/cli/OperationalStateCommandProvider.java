/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.cli.utils.BGPOperationalStateUtils;

@Service
@Command(scope = "bgp", name = "operational-state", description = "Shows BGP Operational State.")
public final class OperationalStateCommandProvider implements Action {
    @Reference
    private Session session;
    @Reference
    private DataBroker dataBroker;
    @Option(name = "-neighbor", aliases = {"--neighbor"}, description = "Neighbor address")
    private String peer;
    @Option(name = "-rib", aliases = {"--rib"}, description = "Name of RIB", required = true)
    private String ribId;
    @Option(name = "-peer-group", aliases = {"--peer-group"}, description = "Name of Peer Group")
    private String group;

    @Override
    public Object execute() {
        BGPOperationalStateUtils.displayBgpOperationalState(this.dataBroker, this.session.getConsole(),
                this.ribId, this.group, this.peer);
        return null;
    }
}
