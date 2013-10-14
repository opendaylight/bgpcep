/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;
import com.google.common.base.Objects.ToStringHelper;

public class PCEPSecondaryRecordRouteObject extends PCEPObject {

    private final List<ReportedRouteSubobject> subobjects;

    /**
     * Constructs Secondary Record Route Object.
     *
     * @param subobjects
     *            List<ReportedRouteSubobject>. Can't be null or empty.
     * @param ignored
     *            boolean
     */
    public PCEPSecondaryRecordRouteObject(List<ReportedRouteSubobject> subobjects, boolean processed, boolean ignored) {
	super(processed, ignored);
	if (subobjects == null || subobjects.isEmpty())
	    throw new IllegalArgumentException("Subobjects can't be null or empty.");
	this.subobjects = subobjects;
    }

    /**
     * Gets list of {@link ReportedRouteSubobject}
     *
     * @return List<ReportedRouteSubobject>. Can't be null or empty.
     */
    public List<ReportedRouteSubobject> getSubobjects() {
	return this.subobjects;
    }

    @Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("subobjects", this.subobjects);
		return super.addToStringAttributes(toStringHelper);
	}

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + ((this.subobjects == null) ? 0 : this.subobjects.hashCode());
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
	final PCEPSecondaryRecordRouteObject other = (PCEPSecondaryRecordRouteObject) obj;
	if (this.subobjects == null) {
	    if (other.subobjects != null)
		return false;
	} else if (!this.subobjects.equals(other.subobjects))
	    return false;
	return true;
    }

}
