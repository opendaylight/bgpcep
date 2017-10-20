/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli;

import static java.util.Objects.requireNonNull;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.cli.utils.BGPOperationalStateUtils;

@Command(scope = "bgp", name = "operational-state", description = "Shows BGP Operational State.")
public final class OperationalStateCommandProvider extends OsgiCommandSupport {
    private DataBroker dataBroker;
    @Option(name = "-neighbor", aliases = {"--neighbor"}, description = "Neighbor address")
    private String peer;
    @Option(name = "-rib", aliases = {"--rib"}, description = "Name of RIB", required = true)
    private String ribId;
    @Option(name = "-peer-group", aliases = {"--peer-group"}, description = "Name of Peer Group")
    private String group;

    @Override
    protected Object doExecute() throws Exception {
        BGPOperationalStateUtils.displayBgpOperationalState(this.dataBroker, this.session.getConsole(),
                this.ribId, this.group, this.peer);
        return null;
    }

    public void setDataBroker(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }
}
