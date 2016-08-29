/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider.spi;

import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

public interface BgpTopologyDeployer {

    AbstractRegistration registerTopologyProvider(BgpTopologyProvider topologyBuilder);

    DataBroker getDataBroker();

    AbstractRegistration registerTopologyReference(TopologyReference topologyReference);

}
