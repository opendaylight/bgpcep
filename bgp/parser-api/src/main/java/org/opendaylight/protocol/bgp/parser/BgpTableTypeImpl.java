/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
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
    public BgpTableTypeImpl(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
        this.afi = Preconditions.checkNotNull(afi, "Address family may not be null");
        this.safi = Preconditions.checkNotNull(safi, "Subsequent address family may not be null");
    }

    @Override
    public Class<BgpTableType> getImplementedInterface() {
        return BgpTableType.class;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.afi == null) ? 0 : this.afi.hashCode());
        result = prime * result + ((this.safi == null) ? 0 : this.safi.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BgpTableTypeImpl other = (BgpTableTypeImpl) obj;
        if (this.afi == null) {
            if (other.afi != null) {
                return false;
            }
        } else if (!this.afi.equals(other.afi)) {
            return false;
        }
        if (this.safi == null) {
            if (other.safi != null) {
                return false;
            }
        } else if (!this.safi.equals(other.safi)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.afi.toString() + "." + this.safi.toString();
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
