/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static java.util.Objects.requireNonNull;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

/**
 * Utility class identifying a BGP table type. A table type is formed by two identifiers: AFI and SAFI.
 */
public final class BgpTableTypeImpl implements BgpTableType {

    private final Class<? extends SubsequentAddressFamily> safi;

    private final Class<? extends AddressFamily> afi;

    /**
     * Creates BGP Table type.
     *
     * @param afi Address Family Identifier
     * @param safi Subsequent Address Family Identifier
     */
    public BgpTableTypeImpl(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        this.afi = requireNonNull(afi, "Address family may not be null");
        this.safi = requireNonNull(safi, "Subsequent address family may not be null");
    }

    @Override
    public Class<BgpTableType> getImplementedInterface() {
        return BgpTableType.class;
    }

    @Override
    public int hashCode() {
        final int prime = 3;
        int ret = prime * this.afi.hashCode();
        ret += this.safi.hashCode();
        return ret;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof BgpTableTypeImpl) {
            final BgpTableTypeImpl o = (BgpTableTypeImpl) obj;
            return this.afi.equals(o.afi) && this.safi.equals(o.safi);
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BgpTableTypeImpl [getAfi()=");
        builder.append(getAfi());
        builder.append(", getSafi()=");
        builder.append(getSafi());
        builder.append("]");
        return builder.toString();
    }

    /**
     * Returns Address Family Identifier.
     *
     * @return afi AFI
     */
    @Override
    public Class<? extends AddressFamily> getAfi() {
        return this.afi;
    }

    /**
     * Returns Subsequent Address Family Identifier.
     *
     * @return safi SAFI
     */
    @Override
    public Class<? extends SubsequentAddressFamily> getSafi() {
        return this.safi;
    }
}
