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
 * Structure of Label subobject.
 *
 * @see <a href="http://tools.ietf.org/html/rfc3473#section-5.1">Label ERO
 *      subobject</a>
 */
public abstract class RROLabelSubobject extends ReportedRouteSubobject {

    private final boolean upStream;

    /**
     * Constructs new Label subobject.
     *
     * @param upStream
     *            if set label is upstream
     * @param label
     *            Label
     * @param loose
     *            boolean
     */
    public RROLabelSubobject(boolean upStream) {
	this.upStream = upStream;
    }

    public boolean isUpStream() {
	return this.upStream;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + (this.upStream ? 1231 : 1237);
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
	final RROLabelSubobject other = (RROLabelSubobject) obj;
	if (this.upStream != other.upStream)
	    return false;
	return true;
    }

    @Override
	public String toString(){
		return this.addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("upStream", this.upStream);
		return toStringHelper;
	}
}
