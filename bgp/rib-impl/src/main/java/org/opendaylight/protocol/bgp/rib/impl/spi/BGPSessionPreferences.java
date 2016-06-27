/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;

/**
 * DTO for BGP Session preferences, that contains BGP Open message.
 */
public final class BGPSessionPreferences {

    private final AsNumber as;

    private final int hold;

    private final Ipv4Address bgpId;

    private final List<BgpParameters> params;

    private final AsNumber remoteAs;

    /**
     * Creates a new DTO for Open message.
     *
     * @param as local AS number
     * @param hold preferred hold timer value, in seconds
     * @param bgpId local BGP Identifier
     * @param remoteAs expected remote As Number
     * @param params list of advertised parameters
     */
    public BGPSessionPreferences(final AsNumber as, final int hold, final Ipv4Address bgpId, final AsNumber remoteAs,
        final List<BgpParameters> params) {
        this.as = as;
        this.hold = hold;
        this.bgpId = bgpId;
        this.remoteAs = remoteAs;
        this.params = params;
    }

    /**
     * Returns my AS number.
     *
     * @return AS number
     */
    public AsNumber getMyAs() {
        return this.as;
    }

    /**
     * Returns initial value of HoldTimer.
     *
     * @return initial value of HoldTimer
     */
    public int getHoldTime() {
        return this.hold;
    }

    /**
     * Returns my BGP Identifier.
     *
     * @return BGP identifier
     */
    public Ipv4Address getBgpId() {
        return this.bgpId;
    }

    /**
     * Returns expected remote AS number.
     *
     * @return AS number
     */
    public AsNumber getExpectedRemoteAs() {
        return this.remoteAs;
    }

    /**
     * Gets a list of advertised bgp parameters.
     *
     * @return a list of advertised bgp parameters
     */
    public List<BgpParameters> getParams() {
        return this.params;
    }
}
