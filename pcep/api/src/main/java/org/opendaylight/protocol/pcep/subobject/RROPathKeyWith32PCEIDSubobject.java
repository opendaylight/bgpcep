/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.subobject;

import java.util.Arrays;

public class RROPathKeyWith32PCEIDSubobject extends ReportedRouteSubobject {

    private final int pathKey;

    private final byte[] pceId;

    public RROPathKeyWith32PCEIDSubobject(int pathKey, byte[] pceId) {
	super();
	this.pathKey = pathKey;
	if (pceId == null)
	    throw new IllegalArgumentException("PCE ID can't be null.");

	if (pceId.length != 4)
	    throw new IllegalArgumentException("PCE ID is not 4 bytes long.");

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
	int result = 1;
	result = prime * result + this.pathKey;
	result = prime * result + Arrays.hashCode(this.pceId);
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
	final RROPathKeyWith32PCEIDSubobject other = (RROPathKeyWith32PCEIDSubobject) obj;
	if (this.pathKey != other.pathKey)
	    return false;
	if (!Arrays.equals(this.pceId, other.pceId))
	    return false;
	return true;
    }

    @Override
    public String toString() {
	final StringBuilder builder = new StringBuilder();
	builder.append("RROPathKeyWith32PCEIDSubobject [pathKey=");
	builder.append(this.pathKey);
	builder.append(", pceId=");
	builder.append(Arrays.toString(this.pceId));
	builder.append("]");
	return builder.toString();
    }

}
