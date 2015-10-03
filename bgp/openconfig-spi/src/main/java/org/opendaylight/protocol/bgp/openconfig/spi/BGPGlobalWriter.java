/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;

/**
 * Provides put/delete operations of the BGP OpenConfig Global configuration, based on BGP RIB configuration module instance.
 *
 */
public interface BGPGlobalWriter {

    /**
     * Transform input parameters to BGP OpenConfig Global instance and writes the new/changed configuration.
     * @param instanceName BGP RIB configuration module instance name
     * @param localAs Local Autonomous System Number
     * @param bgpRibId BGP RIB identifier
     * @param clusterId BGP Cluster Identifier
     * @param list
     */
    void writeGlobal(String instanceName, AsNumber localAs, Ipv4Address bgpRibId, Ipv4Address clusterId, List<BgpTableType> list);

    /**
     * Delete BGP OpenConfig Global configuration instance.
     * @param instanceName BGP RIB configuration module instance name
     */
    void removeGlobal(String instanceName);

}
