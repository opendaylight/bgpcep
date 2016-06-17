/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import org.opendaylight.protocol.bgp.rib.impl.ApplicationPeer;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

final class OpenConfigMappingUtil {

    private static final QName APP_ID_QNAME = QName.create(ApplicationRib.QNAME, "id").intern();

    private OpenConfigMappingUtil() {
        throw new UnsupportedOperationException();
    }

    public static ApplicationPeer createAppPeer(final Neighbor neighbor, final RIBImpl rib) {
        final String neighborAddress = getNeighborAddress(neighbor.getNeighborAddress());
        return new ApplicationPeer(new ApplicationRibId(neighborAddress), new Ipv4Address(neighborAddress), rib);
    }

    public static String getNeighborAddress(final IpAddress ipAddress) {
        if (ipAddress.getIpv4Address() != null) {
            return ipAddress.getIpv4Address().getValue();
        }
        return ipAddress.getIpv6Address().getValue();
    }

    public static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress getPeerAddress(final IpAddress ipAddress) {
        return IetfInetUtil.INSTANCE.ipAddressFor(InetAddresses.forString(getNeighborAddress(ipAddress)));
    }

    public static int getHoldTimer(final Neighbor neighbor) {
        return neighbor.getTimers().getConfig().getHoldTime().intValue();
    }

    public static long getPeerAs(final Neighbor neighbor, final RIB rib) {
        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber peerAs = neighbor.getConfig().getPeerAs();
        if (peerAs != null) {
            return peerAs.getValue();
        }
        return rib.getLocalAs().getValue();
    }

    public static boolean isActive(final Neighbor neighbor) {
        return !neighbor.getTransport().getConfig().isPassiveMode();
    }

    public static int getRetryTimer(final Neighbor neighbor) {
        return neighbor.getTimers().getConfig().getConnectRetry().intValue();
    }

    //FIXME migrate to new API once md5 commit is merged
    public static Optional<KeyMapping> getKeys(final Neighbor neighbor, final InetAddress peerAddress) {
        final String authPassword = neighbor.getConfig().getAuthPassword();
        if (authPassword != null) {
            final KeyMapping keyMapping = new KeyMapping();
            keyMapping.put(peerAddress, authPassword.getBytes(Charsets.US_ASCII));
            return Optional.of(keyMapping);
        }
        return Optional.absent();
    }

    public static YangInstanceIdentifier createApplicationPeerIId(final ApplicationPeer newPeer) {
        return YangInstanceIdentifier.builder().node(ApplicationRib.QNAME).nodeWithKey(ApplicationRib.QNAME,
                APP_ID_QNAME, newPeer.getName()).node(Tables.QNAME).node(Tables.QNAME).build();
    }

}
