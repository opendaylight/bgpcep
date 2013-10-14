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
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;
import com.google.common.base.Objects.ToStringHelper;

/**
 *
 */
public abstract class PCEPBranchNodeObject extends PCEPObject {

    protected final List<ExplicitRouteSubobject> subobjects;

    /**
     * Constructs Branch Node list object.
     *
     * @param subobjects
     *            List<PCEPSubobject>
     * @param processed
     *            boolean
     * @param ignored
     *            boolean
     */
    public PCEPBranchNodeObject(List<ExplicitRouteSubobject> subobjects, boolean processed, boolean ignored) {
	super(processed, ignored);
	if (subobjects == null || subobjects.isEmpty())
	    throw new IllegalArgumentException("Subobjects can't be null or empty.");
	this.subobjects = subobjects;
    }

    /**
     * Gets list of {@link ExplicitRouteSubobject}
     *
     * @return List<ExplicitRouteSubobject>. Can't be null or empty.
     */
    public List<ExplicitRouteSubobject> getSubobjects() {
	return this.subobjects;
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
	final PCEPBranchNodeObject other = (PCEPBranchNodeObject) obj;
	if (this.subobjects == null) {
	    if (other.subobjects != null)
		return false;
	} else if (!this.subobjects.equals(other.subobjects))
	    return false;
	return true;
    }

	@Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("subobjects", this.subobjects);
		return super.addToStringAttributes(toStringHelper);
	}
}
