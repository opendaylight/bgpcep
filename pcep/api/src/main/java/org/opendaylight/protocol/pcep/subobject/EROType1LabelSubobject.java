/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.subobject;

import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure of Type 1 Label subobject
 *
 * @see <a href="http://tools.ietf.org/html/rfc3209#section-4.1">4.1. Label
 *      Object</a>
 */
public class EROType1LabelSubobject extends EROLabelSubobject {

    private final long label;

    public EROType1LabelSubobject(long label, boolean upStream, boolean loose) {
	super(upStream);
	this.label = label;
    }

    public long getLabel() {
	return this.label;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + (int) (this.label ^ (this.label >>> 32));
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
	final EROType1LabelSubobject other = (EROType1LabelSubobject) obj;
	if (this.label != other.label)
	    return false;
	return true;
    }

    @Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("label", this.label);
		return super.addToStringAttributes(toStringHelper);
	}
}
