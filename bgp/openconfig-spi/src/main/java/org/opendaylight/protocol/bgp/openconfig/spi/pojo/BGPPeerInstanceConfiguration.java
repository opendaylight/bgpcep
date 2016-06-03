/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi.pojo;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.List;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rfc2385.cfg.rev160324.Rfc2385Key;

/**
 * POJO for holding BGP Peer module instance configuration
 *
 */
public final class BGPPeerInstanceConfiguration extends AbstractInstanceConfiguration {

    private final IpAddress host;
    private final PortNumber port;
    private final int holdTimer;
    private final PeerRole peerRole;
    private final boolean active;
    private final List<BgpTableType> advertizedTables;
    private final AsNumber asNumber;
    private final Optional<Rfc2385Key> password;
    private final List<AddressFamilies> addPathCapabilities;

    public BGPPeerInstanceConfiguration(final InstanceConfigurationIdentifier identifier, final IpAddress host, final PortNumber port, final int holdTimer, final PeerRole peerRole,
            final boolean active, final List<BgpTableType> advertizedTables, final AsNumber asNumber, final Optional<Rfc2385Key> password,
            final List<AddressFamilies> addPathCapabilities) {
        super(identifier);
        this.addPathCapabilities = addPathCapabilities;
        this.host = Preconditions.checkNotNull(host);
        this.port = Preconditions.checkNotNull(port);
        this.holdTimer = Preconditions.checkNotNull(holdTimer);
        this.peerRole = Preconditions.checkNotNull(peerRole);
        this.active = Preconditions.checkNotNull(active);
        this.advertizedTables = Preconditions.checkNotNull(advertizedTables);
        this.asNumber = Preconditions.checkNotNull(asNumber);
        this.password = Preconditions.checkNotNull(password);
    }

    public IpAddress getHost() {
        return host;
    }

    public PortNumber getPort() {
        return port;
    }

    public int getHoldTimer() {
        return holdTimer;
    }

    public PeerRole getPeerRole() {
        return peerRole;
    }

    public boolean isActive() {
        return active;
    }

    public List<BgpTableType> getAdvertizedTables() {
        return advertizedTables;
    }

    public AsNumber getAsNumber() {
        return asNumber;
    }

    public Optional<Rfc2385Key> getPassword() {
        return password;
    }

    public List<AddressFamilies> getAddPathCapabilities() {
        return addPathCapabilities;
    }

}
