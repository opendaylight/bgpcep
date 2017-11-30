/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.cli;

import static java.util.Objects.requireNonNull;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.protocol.pcep.cli.utils.PcepStateUtils;

@Command(scope = "pcep", name = "node-state", description = "Shows PCEP Topology Node Session State.")
public final class PcepStateCommandProvider extends OsgiCommandSupport {
    private DataBroker dataBroker;
    @Option(name = "-topology-id", aliases = {"--tpi"}, description = "Topology Id", required = true)
    private String tpi;
    @Option(name = "-node-id", aliases = {"--ni"}, description = "Node Id")
    private String ni;

    @Override
    protected Object doExecute() throws Exception {
        PcepStateUtils.displayNodeState(this.dataBroker, this.session.getConsole(), this.tpi, this.ni);
        return null;
    }

    public void setDataBroker(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }
}
