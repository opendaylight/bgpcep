/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static java.util.Objects.requireNonNull;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;

/**
 * Utility class identifying a BGP table type. A table type is formed by two identifiers: AFI and SAFI.
 */
public final class BgpTableTypeImpl implements BgpTableType {
    private final SubsequentAddressFamily safi;
    private final AddressFamily afi;

    /**
     * Creates BGP Table type.
     *
     * @param afi Address Family Identifier
     * @param safi Subsequent Address Family Identifier
     */
    public BgpTableTypeImpl(final AddressFamily afi, final SubsequentAddressFamily safi) {
        this.afi = requireNonNull(afi, "Address family may not be null");
        this.safi = requireNonNull(safi, "Subsequent address family may not be null");
    }

    @Override
    public Class<BgpTableType> implementedInterface() {
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
        return "BgpTableTypeImpl [getAfi()=" + getAfi() + ", getSafi()=" + getSafi() + "]";
    }

    /**
     * Returns Address Family Identifier.
     *
     * @return afi AFI
     */
    @Override
    public AddressFamily getAfi() {
        return this.afi;
    }

    /**
     * Returns Subsequent Address Family Identifier.
     *
     * @return safi SAFI
     */
    @Override
    public SubsequentAddressFamily getSafi() {
        return this.safi;
    }
}
