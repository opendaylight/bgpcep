/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi.pojo;

import com.google.common.base.Preconditions;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
 * POJO for holding BGP Application Peer module instance configuration
 *
 */
public final class BGPAppPeerInstanceConfiguration extends AbstractInstanceConfiguration {

    private final String appRibId;
    private final Ipv4Address bgpId;

    public BGPAppPeerInstanceConfiguration(final InstanceConfigurationIdentifier instanceName, final String appRibId, final Ipv4Address bgpId) {
        super(instanceName);
        this.appRibId = Preconditions.checkNotNull(appRibId);
        this.bgpId = Preconditions.checkNotNull(bgpId);
    }

    public String getAppRibId() {
        return appRibId;
    }

    public Ipv4Address getBgpId() {
        return bgpId;
    }

}
