/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

/**
 *
 */
public interface PolicyRIBBaseParameters {
    /**
     * @return RIB AS
     */
    long getLocalAs();

    /**
     * @return originator Id
     */
    @Nonnull Ipv4Address getOriginatorId();

    /**
     * @return Cluster Identifier
     */
    @Nonnull ClusterIdentifier getClusterId();
}
