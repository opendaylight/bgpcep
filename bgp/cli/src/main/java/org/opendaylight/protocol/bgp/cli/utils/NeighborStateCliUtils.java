/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli.utils;

import java.io.PrintStream;
import java.util.List;
import org.apache.karaf.shell.table.ShellTable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.bgp.neighbor.prefix.counters_state.Prefixes;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpCapability;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTimersStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTransportStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.Received;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.Sent;

//NeighborStateCliUtils sends Neighbor Operational State to PrintStream
final class NeighborStateCliUtils {
    @SuppressWarnings("checkstyle:HideUtilityClassConstructor")
    private NeighborStateCliUtils() {
        throw new UnsupportedOperationException();
    }

    static void displayNeighborOperationalState(@NonNull final String neighborId,
            @NonNull final Neighbor neighbor, @NonNull final PrintStream stream) {
        final State neighborState = neighbor.getState();
        if (neighborState == null) {
            stream.println(String.format("No BgpSessionState found for [%s]", neighborId));
            return;
        }

        final ShellTable table = new ShellTable();
        table.column("Attribute").alignLeft();
        table.column("Value").alignLeft();
        table.addRow().addContent("Neighbor Address", neighborId);

        final NeighborStateAugmentation stateAug = neighborState.getAugmentation(NeighborStateAugmentation.class);
        if (stateAug != null) {
            table.addRow().addContent("Session State", stateAug.getSessionState());
            printCapabilitiesState(stateAug.getSupportedCapabilities(), table);
        }

        printTimerState(neighbor.getTimers(), table);
        printTransportState(neighbor.getTransport(), table);
        printMessagesState(neighborState, table);
        printAfiSafisState(neighbor.getAfiSafis().getAfiSafi(), table);

        table.print(stream);
    }

    private static void printCapabilitiesState(final List<Class<? extends BgpCapability>> supportedCapabilities,
            final ShellTable table) {
        if (supportedCapabilities == null) {
            return;
        }
        addHeader(table, "Supported Capabilities");
        supportedCapabilities.forEach(capa -> table.addRow().addContent("", capa.getSimpleName()));
    }

    static void addHeader(final ShellTable table, final String header) {
        table.addRow().addContent("                      ", "");
        table.addRow().addContent(header, "");
        table.addRow().addContent("======================", "");
    }

    private static void printAfiSafisState(final List<AfiSafi> afiSafis, final ShellTable table) {
        afiSafis.forEach(afiSafi -> printAfiSafiState(afiSafi, table));

    }

    private static void printAfiSafiState(final AfiSafi afiSafi, final ShellTable table) {
        final NeighborAfiSafiStateAugmentation state = afiSafi.getState()
                .getAugmentation(NeighborAfiSafiStateAugmentation.class);
        addHeader(table, "AFI state");
        table.addRow().addContent("Family", afiSafi.getAfiSafiName().getSimpleName());
        table.addRow().addContent("Active", state.isActive());
        final Prefixes prefixes = state.getPrefixes();
        if (prefixes == null) {
            return;
        }
        table.addRow().addContent("Prefixes", "");
        table.addRow().addContent("Installed", prefixes.getInstalled());
        table.addRow().addContent("Sent", prefixes.getSent());
        table.addRow().addContent("Received", prefixes.getReceived());

    }

    private static void printMessagesState(final State neighborState, final ShellTable table) {
        final BgpNeighborStateAugmentation state = neighborState.getAugmentation(BgpNeighborStateAugmentation.class);
        if (state == null) {
            return;
        }
        addHeader(table, "Messages state");
        final Messages messages = state.getMessages();
        table.addRow().addContent("Messages Received", "");

        final Received received = messages.getReceived();
        table.addRow().addContent("NOTIFICATION", received.getNOTIFICATION());
        table.addRow().addContent("UPDATE", received.getUPDATE());

        final Sent sent = messages.getSent();
        table.addRow().addContent("Messages Sent", "");
        table.addRow().addContent("NOTIFICATION", sent.getNOTIFICATION());
        table.addRow().addContent("UPDATE", sent.getUPDATE());
    }

    private static void printTransportState(final Transport transport, final ShellTable table) {
        if (transport == null) {
            return;
        }
        final NeighborTransportStateAugmentation state = transport.getState()
                .getAugmentation(NeighborTransportStateAugmentation.class);
        if (state == null) {
            return;
        }
        addHeader(table, "Transport state");

        final IpAddress remoteAddress = state.getRemoteAddress();
        final String stringRemoteAddress;
        if (remoteAddress.getIpv4Address() == null) {
            stringRemoteAddress = remoteAddress.getIpv6Address().getValue();
        } else {
            stringRemoteAddress = remoteAddress.getIpv4Address().getValue();
        }
        table.addRow().addContent("Remote Address", stringRemoteAddress);
        table.addRow().addContent("Remote Port", state.getRemotePort().getValue());
        table.addRow().addContent("Local Port", state.getLocalPort().getValue());
    }

    private static void printTimerState(final Timers timers, final ShellTable table) {
        if (timers == null) {
            return;
        }

        final NeighborTimersStateAugmentation state = timers.getState()
                .getAugmentation(NeighborTimersStateAugmentation.class);
        if (state == null) {
            return;
        }
        addHeader(table, "Timer state");
        table.addRow().addContent("Negotiated Hold Time", state.getNegotiatedHoldTime());
        table.addRow().addContent("Uptime", state.getUptime().getValue());
    }
}
