/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.base.Optional;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;

/**
 * DTO for BGP Session preferences, that contains BGP Open message.
 */
public final class BGPSessionPreferences {

    private final AsNumber as;

    private final int hold;

    private final BgpId bgpId;

    private final List<BgpParameters> params;

    private final AsNumber remoteAs;

    private final Optional<byte[]> md5Password;

    /**
     * Creates a new DTO for Open message.
     *
     * @param as local AS number
     * @param hold preferred hold timer value, in seconds
     * @param bgpId local BGP Identifier
     * @param remoteAs expected remote As Number
     * @param params list of advertised parameters
     * @param md5Password - md5password
     */
    public BGPSessionPreferences(final AsNumber as, final int hold, final BgpId bgpId, final AsNumber remoteAs,
            final List<BgpParameters> params, final Optional<byte[]> md5Password) {
        this.as = as;
        this.hold = hold;
        this.bgpId = (bgpId != null) ? new BgpId(bgpId) : null;
        this.remoteAs = remoteAs;
        this.params = params;
        this.md5Password = md5Password;
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
    public BgpId getBgpId() {
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

    /**
     * Optionally returns peer's MD5 password.
     * @return Encoded MD5 password.
     */
    public Optional<byte[]> getMd5Password() {
        return this.md5Password;
    }
}
