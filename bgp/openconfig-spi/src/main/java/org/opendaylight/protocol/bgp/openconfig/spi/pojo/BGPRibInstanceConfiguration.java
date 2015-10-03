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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;

/**
 * POJO for holding BGP RIB module instance configuration
 *
 */
public final class BGPRibInstanceConfiguration extends AbstractInstanceConfiguration {

    private final AsNumber localAs;
    private final Ipv4Address bgpRibId;
    private final Ipv4Address clusterId;
    private final List<BgpTableType> tableTypes;

    public BGPRibInstanceConfiguration(final InstanceConfigurationIdentifier identifier, final AsNumber localAs, final Ipv4Address bgpRibId,
            final Ipv4Address clusterId, final List<BgpTableType> tableTypes) {
        super(identifier);
        this.localAs = Preconditions.checkNotNull(localAs);
        this.bgpRibId = Preconditions.checkNotNull(bgpRibId);
        this.clusterId = clusterId;
        this.tableTypes = Preconditions.checkNotNull(tableTypes);
    }

    public AsNumber getLocalAs() {
        return localAs;
    }

    public Ipv4Address getBgpRibId() {
        return bgpRibId;
    }

    public Optional<Ipv4Address> getClusterId() {
        return Optional.fromNullable(clusterId);
    }

    public List<BgpTableType> getTableTypes() {
        return tableTypes;
    }

}
