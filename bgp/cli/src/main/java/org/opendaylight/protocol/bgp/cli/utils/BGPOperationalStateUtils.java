/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli.utils;

import static org.opendaylight.protocol.bgp.cli.utils.GlobalStateCliUtils.displayRibOperationalState;
import static org.opendaylight.protocol.bgp.cli.utils.NeighborStateCliUtils.displayNeighborOperationalState;
import static org.opendaylight.protocol.bgp.cli.utils.PeerGroupStateCliUtils.displayPeerOperationalState;

import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BGPOperationalStateUtils reads Operational state from DS and prints to stream.
 */
public final class BGPOperationalStateUtils {

    static final InstanceIdentifier<Protocols> PROTOCOLS_IID;
    private static final Logger LOG = LoggerFactory.getLogger(BGPOperationalStateUtils.class);

    static {
        PROTOCOLS_IID = InstanceIdentifier.create(NetworkInstances.class)
                .child(NetworkInstance.class, new NetworkInstanceKey("global-bgp")).child(Protocols.class);
    }

    @SuppressWarnings("checkstyle:HideUtilityClassConstructor")
    private BGPOperationalStateUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Display to stream operational state, rib Id is mandatory.
     *
     * @param dataBroker data broker
     * @param stream     where to print
     * @param ribId      mandatory, Operational State per given RIB Group will be printed
     *                   if no other parameter is passed
     * @param group      if provided, Operational State per given Neighbor Group will be printed
     * @param neighbor   if provided, Operational State per given Neighbor will be printed
     */
    public static void displayBgpOperationalState(@NonNull final DataBroker dataBroker,
            @NonNull final PrintStream stream, @NonNull final String ribId, @Nullable final String group,
            @Nullable final String neighbor) {
        final Bgp globalBgp = readGlobalFromDataStore(dataBroker, ribId);
        if (globalBgp == null) {
            stream.println(String.format("RIB not found for [%s]", ribId));
            return;
        }
        if (neighbor == null && group == null) {
            displayRibOperationalState(ribId, globalBgp.getGlobal(), stream);
        } else {
            if (neighbor != null) {
                globalBgp.getNeighbors().getNeighbor().stream()
                        .filter(neig -> Ipv4Util.toStringIP(neig.getKey().getNeighborAddress()).matches(neighbor))
                        .findFirst()
                        .ifPresent(neighbor1 -> displayNeighborOperationalState(neighbor, neighbor1, stream));
            } else {
                displayPeerOperationalState(globalBgp.getPeerGroups().getPeerGroup(), stream);
            }
        }

    }

    private static Bgp readGlobalFromDataStore(final DataBroker dataBroker, final String ribId) {
        final InstanceIdentifier<Bgp> bgpIID = PROTOCOLS_IID
                .child(Protocol.class, new ProtocolKey(BGP.class, ribId))
                .augmentation(Protocol1.class).child(Bgp.class);

        final ReadOnlyTransaction rot = dataBroker.newReadOnlyTransaction();

        try {
            return rot.read(LogicalDatastoreType.OPERATIONAL, bgpIID).get().orNull();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.warn("Failed to read rib {}", ribId, e);
        }
        return null;
    }
}
