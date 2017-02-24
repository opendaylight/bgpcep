/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer.WriteConfiguration;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;

/**
 * Common interface for BgpPeer and AppPeer beans
 *
 */
public interface PeerBean {

    void start(RIB rib, Neighbor neighbor, BGPTableTypeRegistryConsumer tableTypeRegistry, WriteConfiguration configurationWriter);

    void restart(RIB rib, BGPTableTypeRegistryConsumer tableTypeRegistry);

    ListenableFuture<Void> close();

    Boolean containsEqualConfiguration(Neighbor neighbor);

    void instantiateServiceInstance();
}
