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
import java.util.Map;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

/**
 * POJO for holding BGP RIB module instance configuration
 *
 */
public final class BGPRibInstanceConfiguration extends AbstractInstanceConfiguration {

    private final AsNumber localAs;
    private final BgpId bgpRibId;
    private final ClusterIdentifier clusterId;
    private final List<BgpTableType> tableTypes;
    private final Map<TablesKey, PathSelectionMode> pathSelectionModes;

    public BGPRibInstanceConfiguration(final InstanceConfigurationIdentifier identifier, final AsNumber localAs, final BgpId bgpRibId,
            final ClusterIdentifier clusterId, final List<BgpTableType> tableTypes, final Map<TablesKey, PathSelectionMode> pathSelectionModes) {
        super(identifier);
        this.pathSelectionModes = pathSelectionModes;
        this.localAs = Preconditions.checkNotNull(localAs);
        this.bgpRibId = Preconditions.checkNotNull(bgpRibId);
        this.clusterId = clusterId;
        this.tableTypes = Preconditions.checkNotNull(tableTypes);
    }

    public AsNumber getLocalAs() {
        return localAs;
    }

    public BgpId getBgpRibId() {
        return bgpRibId;
    }

    public Optional<ClusterIdentifier> getClusterId() {
        return Optional.fromNullable(clusterId);
    }

    public List<BgpTableType> getTableTypes() {
        return tableTypes;
    }

    public Map<TablesKey, PathSelectionMode> getPathSelectionModes() {
        return pathSelectionModes;
    }

}
