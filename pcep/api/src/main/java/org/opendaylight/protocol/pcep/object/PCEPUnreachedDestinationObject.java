/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.List;

import org.opendaylight.protocol.concepts.NetworkAddress;
import org.opendaylight.protocol.pcep.PCEPObject;
import com.google.common.base.Objects.ToStringHelper;

public class PCEPUnreachedDestinationObject<T extends NetworkAddress<T>> extends PCEPObject {

    private final List<T> unreachedDestinations;

    public PCEPUnreachedDestinationObject(List<T> unreachedDestinations, boolean processed, boolean ignored) {
	super(processed, ignored);
	if (unreachedDestinations == null || unreachedDestinations.isEmpty())
	    throw new IllegalArgumentException("At least one destination have to be specified.");

	this.unreachedDestinations = unreachedDestinations;
    }

    public List<T> getUnreachedDestinations() {
	return this.unreachedDestinations;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + ((this.unreachedDestinations == null) ? 0 : this.unreachedDestinations.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (!super.equals(obj))
	    return false;
	if (this.getClass() != obj.getClass())
	    return false;
	final PCEPUnreachedDestinationObject<?> other = (PCEPUnreachedDestinationObject<?>) obj;
	if (this.unreachedDestinations == null) {
	    if (other.unreachedDestinations != null)
		return false;
	} else if (!this.unreachedDestinations.equals(other.unreachedDestinations))
	    return false;
	return true;
    }

    @Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("unreachedDestinations", this.unreachedDestinations);
		return super.addToStringAttributes(toStringHelper);
	}

}
