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

/**
 * Structure of Generalized Label subobject
 *
 * @see <a href="http://tools.ietf.org/html/rfc3471#section-3.2">3.2.
 *      Generalized Label</a>
 */
public class EROGeneralizedLabelSubobject extends EROLabelSubobject {

    private final byte[] label;

    public EROGeneralizedLabelSubobject(byte[] label, boolean upStream, boolean loose) {
	super(upStream);

	if (label.length % 4 != 0)
	    throw new IllegalArgumentException("Length of label is not multiple of 4.");

	this.label = label;
    }

    public byte[] getLabel() {
	return this.label;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + Arrays.hashCode(this.label);
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
	final EROGeneralizedLabelSubobject other = (EROGeneralizedLabelSubobject) obj;
	if (!Arrays.equals(this.label, other.label))
	    return false;
	return true;
    }

    @Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("label", this.label);
		return super.addToStringAttributes(toStringHelper);
	}

}
