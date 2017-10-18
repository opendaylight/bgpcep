/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli.utils;

import static org.opendaylight.protocol.bgp.cli.utils.NeighborStateCliUtils.addHeader;

import java.io.PrintStream;
import org.apache.karaf.shell.table.ShellTable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.GlobalAfiSafiStateAugmentation;

//GlobalStateCliUtils sends Global Operational State to PrintStream
final class GlobalStateCliUtils {

    @SuppressWarnings("checkstyle:HideUtilityClassConstructor")
    private GlobalStateCliUtils() {
        throw new UnsupportedOperationException();
    }


    static void displayRibOperationalState(@NonNull final String ribId, @NonNull final Global global,
            @NonNull final PrintStream stream) {
        final State globalState = global.getState();

        final ShellTable table = new ShellTable();
        table.column("Attribute").alignLeft();
        table.column("Value").alignLeft();

        addHeader(table, "RIB state");
        table.addRow().addContent("Router Id", ribId);
        table.addRow().addContent("As", globalState.getAs());
        table.addRow().addContent("Total Paths", globalState.getTotalPaths());
        table.addRow().addContent("Total Prefixes", globalState.getTotalPrefixes());
        global.getAfiSafis().getAfiSafi().forEach(afiSafi -> displayAfiSafi(afiSafi, table));
        table.print(stream);
    }

    private static void displayAfiSafi(final AfiSafi afiSafi, final ShellTable table) {
        final GlobalAfiSafiStateAugmentation state = afiSafi.getState()
                .getAugmentation(GlobalAfiSafiStateAugmentation.class);
        addHeader(table, "AFI/SAFI state");
        table.addRow().addContent("Family", afiSafi.getAfiSafiName().getSimpleName());
        if (state == null) {
            return;
        }
        table.addRow().addContent("Total Paths", state.getTotalPaths());
        table.addRow().addContent("Total Prefixes", state.getTotalPrefixes());
    }
}
