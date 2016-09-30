/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil.INSTANCE;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.BgpCommonAfiSafiList;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.transport.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborConfigAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class OpenConfigMappingUtil {

    private static final AfiSafi IPV4_AFISAFI = new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build();
    private static final List<AfiSafi> DEFAULT_AFISAFI = ImmutableList.of(IPV4_AFISAFI);
    private static final int HOLDTIMER = 90;
    private static final int CONNECT_RETRY = 30;
    private static final PortNumber PORT = new PortNumber(179);

    private OpenConfigMappingUtil() {
        throw new UnsupportedOperationException();
    }

    public static String getRibInstanceName(final InstanceIdentifier<?> rootIdentifier) {
        return rootIdentifier.firstKeyOf(Protocol.class).getName();
    }

    public static int getHoldTimer(final Neighbor neighbor) {
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers.Config config =
                getTimersConfig(neighbor);
        if (config != null && config.getHoldTime() != null) {
            return config.getHoldTime().intValue();
        }
        return HOLDTIMER;
    }

    public static AsNumber getPeerAs(final Neighbor neighbor, final RIB rib) {
        if (neighbor.getConfig() != null) {
            final AsNumber peerAs = neighbor.getConfig().getPeerAs();
            if (peerAs != null) {
                return peerAs;
            }
        }
        return rib.getLocalAs();
    }

    public static boolean isActive(final Neighbor neighbor) {
        if (neighbor.getTransport() != null) {
            final Config config = neighbor.getTransport().getConfig();
            if (config != null && config.isPassiveMode() != null) {
                return !config.isPassiveMode();
            }
        }
        return true;
    }

    public static int getRetryTimer(final Neighbor neighbor) {
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers.Config config =
                getTimersConfig(neighbor);
        if (config != null && config.getConnectRetry() != null) {
            return config.getConnectRetry().intValue();
        }
        return CONNECT_RETRY;
    }

    public static KeyMapping getNeighborKey(final Neighbor neighbor) {
        if (neighbor.getConfig() != null) {
            final String authPassword = neighbor.getConfig().getAuthPassword();
            if (authPassword != null) {
                return KeyMapping.getKeyMapping(INSTANCE.inetAddressFor(neighbor.getNeighborAddress()), authPassword);
            }
        }
        return null;
    }

    public static InstanceIdentifier<Neighbor> getNeighborInstanceIdentifier(final InstanceIdentifier<Bgp> rootIdentifier,
            final NeighborKey neighborKey) {
        return rootIdentifier.child(Neighbors.class).child(Neighbor.class, neighborKey);
    }

    public static String getNeighborInstanceName(final InstanceIdentifier<?> rootIdentifier) {
        return Ipv4Util.toStringIP(rootIdentifier.firstKeyOf(Neighbor.class).getNeighborAddress());
    }

    public static PortNumber getPort(final Neighbor neighbor) {
        if (neighbor.getTransport() != null) {
            final Config config = neighbor.getTransport().getConfig();
            if (config != null && config.getAugmentation(Config1.class) != null) {
                final PortNumber remotePort = config.getAugmentation(Config1.class).getRemotePort();
                if (remotePort != null) {
                    return remotePort;
                }
            }
        }
        return PORT;
    }

    //make sure IPv4 Unicast (RFC 4271) when required
    public static List<AfiSafi> getAfiSafiWithDefault(final BgpCommonAfiSafiList afiSAfis, final boolean setDeafultIPv4) {
        if (afiSAfis == null || afiSAfis.getAfiSafi() == null) {
            return setDeafultIPv4 ? DEFAULT_AFISAFI : Collections.emptyList();
        }
        final List<AfiSafi> afiSafi = afiSAfis.getAfiSafi();
        if (setDeafultIPv4) {
            final boolean anyMatch = afiSafi.stream().anyMatch(input -> input.getAfiSafiName().equals(IPV4UNICAST.class));
            if (!anyMatch) {
                afiSafi.add(IPV4_AFISAFI);
            }
        }
        return afiSafi;
    }

    public static ClusterIdentifier getClusterIdentifier(final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.Config globalConfig) {
        final GlobalConfigAugmentation globalConfigAugmentation = globalConfig.getAugmentation(GlobalConfigAugmentation.class);
        if (globalConfigAugmentation != null && globalConfigAugmentation.getRouteReflectorClusterId() != null) {
            return new ClusterIdentifier(globalConfigAugmentation.getRouteReflectorClusterId().getIpv4Address());
        }
        return new ClusterIdentifier(globalConfig.getRouterId());
    }

    public static SimpleRoutingPolicy getSimpleRoutingPolicy(final Neighbor neighbor) {
        if (neighbor.getConfig() != null) {
            final NeighborConfigAugmentation augmentation = neighbor.getConfig().getAugmentation(NeighborConfigAugmentation.class);
            if (augmentation != null) {
                return augmentation.getSimpleRoutingPolicy();
            }
        }
        return null;
    }

    private static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.timers.Config getTimersConfig(final Neighbor neighbor) {
        final Timers timers = neighbor.getTimers();
        return timers != null ? timers.getConfig() : null;
    }

}
