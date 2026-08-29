/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli.utils;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import org.apache.karaf.shell.support.table.ShellTable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.State;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborTimersStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborTransportStateAugmentation;

//NeighborStateCliUtils sends Neighbor Operational State to PrintStream
final class NeighborStateCliUtils {
    private NeighborStateCliUtils() {
        // Hidden on purpose
    }

    static void displayNeighborOperationalState(final @NonNull String neighborId,
            final @NonNull Neighbor neighbor, final @NonNull PrintStream stream) {
        final State neighborState = neighbor.getState();
        if (neighborState == null) {
            stream.println("No BgpSessionState found for [%s]".formatted(neighborId));
            return;
        }

        final var table = new ShellTable();
        table.column("Attribute").alignLeft();
        table.column("Value").alignLeft();
        table.addRow().addContent("Neighbor Address", neighborId);

        final var stateAug = neighborState.augmentation(NeighborStateAugmentation.class);
        if (stateAug != null) {
            table.addRow().addContent("Session State", stateAug.getSessionState());
            printCapabilitiesState(stateAug.getSupportedCapabilities(), table);
        }

        printTimerState(neighbor.getTimers(), table);
        printTransportState(neighbor.getTransport(), table);
        printMessagesState(neighborState, table);
        printAfiSafisState(neighbor.getAfiSafis().nonnullAfiSafi().values(), table);

        table.print(stream, StandardCharsets.UTF_8, true);
    }

    private static void printCapabilitiesState(final Set<BgpCapability> supportedCapabilities,
            final ShellTable table) {
        if (supportedCapabilities == null) {
            return;
        }
        addHeader(table, "Supported Capabilities");
        supportedCapabilities.forEach(
            capa -> table.addRow().addContent("", capa.implementedInterface().getSimpleName()));
    }

    static void addHeader(final ShellTable table, final String header) {
        table.addRow().addContent("                      ", "");
        table.addRow().addContent(header, "");
        table.addRow().addContent("======================", "");
    }

    private static void printAfiSafisState(final Collection<AfiSafi> afiSafis, final ShellTable table) {
        afiSafis.forEach(afiSafi -> printAfiSafiState(afiSafi, table));
    }

    private static void printAfiSafiState(final AfiSafi afiSafi, final ShellTable table) {
        final var state = afiSafi.getState().augmentation(NeighborAfiSafiStateAugmentation.class);
        addHeader(table, "AFI state");
        table.addRow().addContent("Family", afiSafi.getAfiSafiName().implementedInterface().getSimpleName());
        table.addRow().addContent("Active", state.getActive());
        final var prefixes = state.getPrefixes();
        if (prefixes == null) {
            return;
        }
        table.addRow().addContent("Prefixes", "");
        table.addRow().addContent("Installed", prefixes.getInstalled());
        table.addRow().addContent("Sent", prefixes.getSent());
        table.addRow().addContent("Received", prefixes.getReceived());

    }

    private static void printMessagesState(final State neighborState, final ShellTable table) {
        final var state = neighborState.augmentation(BgpNeighborStateAugmentation.class);
        if (state == null) {
            return;
        }
        addHeader(table, "Messages state");
        final var messages = state.getMessages();
        table.addRow().addContent("Messages Received", "");

        final var received = messages.getReceived();
        table.addRow().addContent("NOTIFICATION", received.getNOTIFICATION());
        table.addRow().addContent("UPDATE", received.getUPDATE());

        final var sent = messages.getSent();
        table.addRow().addContent("Messages Sent", "");
        table.addRow().addContent("NOTIFICATION", sent.getNOTIFICATION());
        table.addRow().addContent("UPDATE", sent.getUPDATE());
    }

    private static void printTransportState(final Transport transport, final ShellTable table) {
        if (transport == null) {
            return;
        }
        final var state = transport.getState().augmentation(NeighborTransportStateAugmentation.class);
        if (state == null) {
            return;
        }
        addHeader(table, "Transport state");

        final var remoteAddress = state.getRemoteAddress();
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

        final var state = timers.getState().augmentation(NeighborTimersStateAugmentation.class);
        if (state == null) {
            return;
        }
        addHeader(table, "Timer state");
        table.addRow().addContent("Negotiated Hold Time", state.getNegotiatedHoldTime());
        table.addRow().addContent("Uptime", state.getUptime().getValue());
    }
}
