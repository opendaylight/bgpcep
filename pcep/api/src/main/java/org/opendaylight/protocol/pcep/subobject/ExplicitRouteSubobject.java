/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.subobject;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Base class for Explicit route subobjects.
 *
 * @see <a href="http://tools.ietf.org/html/rfc3209#section-4.3">4.3. Explicit
 *      Route Object</a>
 */
public abstract class ExplicitRouteSubobject {
    protected final boolean loose;

    protected ExplicitRouteSubobject() {
	this.loose = false;
    }

    protected ExplicitRouteSubobject(boolean loose) {
	this.loose = loose;
    }

    /**
     * @see <a href="http://tools.ietf.org/html/rfc3209#section-4.3.3.1">Strict
     *      and Loose Subobjects</a>
     *
     * @return true if L flag is set and false if is not.
     */
    public boolean isLoose() {
	return this.loose;
    }

	@Override
	public String toString(){
		return this.addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("loose", this.loose);
		return toStringHelper;
	}

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + (this.loose ? 1231 : 1237);
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (this.getClass() != obj.getClass())
	    return false;
	final ExplicitRouteSubobject other = (ExplicitRouteSubobject) obj;
	if (this.loose != other.loose)
	    return false;
	return true;
    }
}
