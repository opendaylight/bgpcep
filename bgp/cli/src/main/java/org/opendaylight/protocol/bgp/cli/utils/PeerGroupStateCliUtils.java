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
import java.util.List;
import org.apache.karaf.shell.table.ShellTable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.PeerGroupStateAugmentation;

//PeerGroupStateCliUtils sends Peer Group Operational State to PrintStream
final class PeerGroupStateCliUtils {
    @SuppressWarnings("checkstyle:HideUtilityClassConstructor")
    private PeerGroupStateCliUtils() {
        throw new UnsupportedOperationException();
    }

    static void displayPeerOperationalState(@NonNull final List<PeerGroup> peerGroupList,
            @NonNull final PrintStream stream) {
        final ShellTable table = new ShellTable();
        table.column("Attribute").alignLeft();
        table.column("Value").alignLeft();

        peerGroupList.forEach(group -> displayState(group, table));
        table.print(stream);
    }

    private static void displayState(final PeerGroup group, final ShellTable table) {
        addHeader(table, "Peer Group state");
        table.addRow().addContent("Peer Group Name", group.getPeerGroupName());
        final State state = group.getState();
        if (state == null) {
            return;
        }
        final PeerGroupStateAugmentation aug = state.getAugmentation(PeerGroupStateAugmentation.class);
        table.addRow().addContent("Total Prefixes", aug.getTotalPrefixes());
    }
}
