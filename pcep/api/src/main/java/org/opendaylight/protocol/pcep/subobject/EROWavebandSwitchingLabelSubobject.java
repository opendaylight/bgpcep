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
 * Structure of Generalized Label subobject
 *
 * @see <a href="http://tools.ietf.org/html/rfc3473#section-2.4">2.4. Waveband
 *      Switching Object </a>
 */
public class EROWavebandSwitchingLabelSubobject extends EROLabelSubobject {

    private final long wavebandId;

    private final long startLabel;

    private final long endLabel;

    public EROWavebandSwitchingLabelSubobject(long wavebandId, long startLabel, long endLabel, boolean upStream, boolean loose) {
	super(upStream);
	this.wavebandId = wavebandId;
	this.startLabel = startLabel;
	this.endLabel = endLabel;
    }

    public long getWavebandId() {
	return this.wavebandId;
    }

    public long getStartLabel() {
	return this.startLabel;
    }

    public long getEndLabel() {
	return this.endLabel;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + (int) (this.endLabel ^ (this.endLabel >>> 32));
	result = prime * result + (int) (this.startLabel ^ (this.startLabel >>> 32));
	result = prime * result + (int) (this.wavebandId ^ (this.wavebandId >>> 32));
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
	final EROWavebandSwitchingLabelSubobject other = (EROWavebandSwitchingLabelSubobject) obj;
	if (this.endLabel != other.endLabel)
	    return false;
	if (this.startLabel != other.startLabel)
	    return false;
	if (this.wavebandId != other.wavebandId)
	    return false;
	return true;
    }

    @Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("wavebandId", this.wavebandId);
		toStringHelper.add("startLabel", this.startLabel);
		toStringHelper.add("endLabel", this.endLabel);
		return super.addToStringAttributes(toStringHelper);
	}
}
