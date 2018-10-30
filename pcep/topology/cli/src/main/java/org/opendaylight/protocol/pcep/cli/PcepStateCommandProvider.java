/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.cli;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.pcep.cli.utils.PcepStateUtils;

@Service
@Command(scope = "pcep", name = "node-state", description = "Shows PCEP Topology Node Session State.")
public final class PcepStateCommandProvider implements Action {
    @Reference
    private Session session;
    @Reference
    private DataBroker dataBroker;
    @Option(name = "-topology-id", aliases = {"--tpi"}, description = "Topology Id", required = true)
    private String tpi;
    @Option(name = "-node-id", aliases = {"--ni"}, description = "Node Id")
    private String ni;

    @Override
    public Object execute() {
        PcepStateUtils.displayNodeState(this.dataBroker, this.session.getConsole(), this.tpi, this.ni);
        return null;
    }
}
