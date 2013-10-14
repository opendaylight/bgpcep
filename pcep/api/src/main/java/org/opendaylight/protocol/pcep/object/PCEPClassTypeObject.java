/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import org.opendaylight.protocol.pcep.PCEPObject;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure of ClassType Object.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5455#section-5"> Object
 *      Definition</a>
 */
public class PCEPClassTypeObject extends PCEPObject {

    private final short classType;

    /**
     * Constructs ClassType Object with given class type.
     *
     * @param classType
     *            short, must be positive and less than 8.
     */
    public PCEPClassTypeObject(short classType) {
	super(true, false);
	if (classType < 0 || classType > 7) {
	    throw new IllegalArgumentException("ClassType range overstepped.");
	}
	this.classType = classType;
    }

    /**
     * Gets class type.
     *
     * @return class type
     */
    public short getClassType() {
	return this.classType;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + this.classType;
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (!super.equals(obj))
	    return false;
	if (!(obj instanceof PCEPClassTypeObject))
	    return false;
	final PCEPClassTypeObject other = (PCEPClassTypeObject) obj;
	if (this.classType != other.classType)
	    return false;
	return true;
    }

	@Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("classType", this.classType);
		return super.addToStringAttributes(toStringHelper);
	}
}
