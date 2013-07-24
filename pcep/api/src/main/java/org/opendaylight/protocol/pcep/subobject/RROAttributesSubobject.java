/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.subobject;

import java.util.Arrays;

public class RROAttributesSubobject extends ReportedRouteSubobject {

    private final byte[] attributes;

    public RROAttributesSubobject(byte[] attributes) {
	super();

	if (attributes.length % 4 != 0)
	    throw new IllegalArgumentException("Attributes have to be multiple of 4.");

	this.attributes = attributes;
    }

    public byte[] getAttributes() {
	return this.attributes;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + Arrays.hashCode(this.attributes);
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
	final RROAttributesSubobject other = (RROAttributesSubobject) obj;
	if (!Arrays.equals(this.attributes, other.attributes))
	    return false;
	return true;
    }

    @Override
    public String toString() {
	final StringBuilder builder = new StringBuilder();
	builder.append("RROAttributesSubobject [attributes=");
	builder.append(Arrays.toString(this.attributes));
	builder.append("]");
	return builder.toString();
    }

}
