/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.subobject;

import java.util.Arrays;

import com.google.common.base.Objects.ToStringHelper;

public class EROPathKeyWith128PCEIDSubobject extends ExplicitRouteSubobject {

    private final int pathKey;

    private final byte[] pceId;

    public EROPathKeyWith128PCEIDSubobject(int pathKey, byte[] pceId, boolean loose) {
	super(loose);
	this.pathKey = pathKey;
	if (pceId == null)
	    throw new IllegalArgumentException("PCE ID can't be null.");

	if (pceId.length != 16)
	    throw new IllegalArgumentException("PCE ID is not 16 bytes long.");

	this.pceId = pceId;
    }

    public int getPathKey() {
	return this.pathKey;
    }

    public byte[] getPceId() {
	return this.pceId;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + this.pathKey;
	result = prime * result + Arrays.hashCode(this.pceId);
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
	final EROPathKeyWith128PCEIDSubobject other = (EROPathKeyWith128PCEIDSubobject) obj;
	if (this.pathKey != other.pathKey)
	    return false;
	if (!Arrays.equals(this.pceId, other.pceId))
	    return false;
	return true;
    }

    @Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("pathKey", this.pathKey);
		toStringHelper.add("pceId", this.pceId);
		return super.addToStringAttributes(toStringHelper);
	}

}
