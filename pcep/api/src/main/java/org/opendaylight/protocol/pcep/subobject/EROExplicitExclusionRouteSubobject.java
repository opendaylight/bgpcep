/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.subobject;

import java.util.List;

import com.google.common.base.Objects.ToStringHelper;

public class EROExplicitExclusionRouteSubobject extends ExplicitRouteSubobject {
    private final List<ExcludeRouteSubobject> xroSubobjets;

    public EROExplicitExclusionRouteSubobject(List<ExcludeRouteSubobject> xroSubobjets) {
	super();
	this.xroSubobjets = xroSubobjets;
    }

    public List<ExcludeRouteSubobject> getXroSubobjets() {
	return this.xroSubobjets;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + ((this.xroSubobjets == null) ? 0 : this.xroSubobjets.hashCode());
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
	final EROExplicitExclusionRouteSubobject other = (EROExplicitExclusionRouteSubobject) obj;
	if (this.xroSubobjets == null) {
	    if (other.xroSubobjets != null)
		return false;
	} else if (!this.xroSubobjets.equals(other.xroSubobjets))
	    return false;
	return true;
    }

    @Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("xroSubobjets", this.xroSubobjets);
		return super.addToStringAttributes(toStringHelper);
	}
}
