/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import org.opendaylight.protocol.concepts.NetworkAddress;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Parameterized structure of PCEP End Points Object.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.6">PCEP
 *      EndPointsObject</a>
 * @param <T>
 *            subtype of NetworkAddress
 */
public class PCEPEndPointsObject<T extends NetworkAddress<T>> extends PCEPEndPoints {

    private final T sourceAddress;

    private final T destinationAddress;

    /**
     * Constructs Close Object with mandatory object.
     *
     * @param sourceAddress
     *            T. Cant't be null.
     * @param destinationAddress
     *            T. Cant't be null.
     */
    public PCEPEndPointsObject(T sourceAddress, T destinationAddress) {
	super(true, false);
	if (sourceAddress == null)
	    throw new IllegalArgumentException("Source address is mantadory.");
	this.sourceAddress = sourceAddress;
	if (destinationAddress == null)
	    throw new IllegalArgumentException("Destination address is mantadory.");
	this.destinationAddress = destinationAddress;
    }

    /**
     * Gets source address of type T.
     *
     * @return T. Can't be null.
     */
    public T getSourceAddress() {
	return this.sourceAddress;
    }

    /**
     * Gets destination address of type T.
     *
     * @return T. Can't be null.
     */
    public T getDestinationAddress() {
	return this.destinationAddress;
    }

	@Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("sourceAddress", this.sourceAddress);
		toStringHelper.add("destinationAddress", this.destinationAddress);
		return super.addToStringAttributes(toStringHelper);
	}

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + ((this.destinationAddress == null) ? 0 : this.destinationAddress.hashCode());
	result = prime * result + ((this.sourceAddress == null) ? 0 : this.sourceAddress.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (!super.equals(obj))
	    return false;
	if (!(obj instanceof PCEPEndPointsObject))
	    return false;
	final PCEPEndPointsObject<?> other = (PCEPEndPointsObject<?>) obj;
	if (this.destinationAddress == null) {
	    if (other.destinationAddress != null)
		return false;
	} else if (!this.destinationAddress.equals(other.destinationAddress))
	    return false;
	if (this.sourceAddress == null) {
	    if (other.sourceAddress != null)
		return false;
	} else if (!this.sourceAddress.equals(other.sourceAddress))
	    return false;
	return true;
    }
}
