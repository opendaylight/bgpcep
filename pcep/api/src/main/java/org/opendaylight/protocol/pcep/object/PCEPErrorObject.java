/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.PCEPTlv;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure of PCEP Error Object.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.15">PCEP Error
 *      Object</a>
 */
public class PCEPErrorObject extends PCEPObject {

    private final PCEPErrors error;

    private final List<PCEPTlv> tlvs;

    /**
     * Constructs Error Object only with mandatory object.
     *
     * @param type
     *            PCEPErrors. Can't be null.
     */
    public PCEPErrorObject(PCEPErrors type) {
	this(type, null);
    }

    /**
     * Constructs Error Object also with optional objects.
     *
     * @param type
     *            PCEPErrors. Can't be null
     * @param tlvs
     *            List<PCEPTlv>
     */
    public PCEPErrorObject(PCEPErrors type, List<PCEPTlv> tlvs) {
	super(false, false);
	this.error = type;
	if (tlvs != null)
	    this.tlvs = tlvs;
	else
	    this.tlvs = Collections.emptyList();
    }

    /**
     * Gets {@link PCEPErrors}
     *
     * @return PCEPErrors. Can't be null.
     */
    public PCEPErrors getError() {
	return this.error;
    }

    /**
     * Gets list of {@link PCEPTlv}
     *
     * @return List<PCEPTlv>. Can't be null, but may be empty.
     */
    public List<PCEPTlv> getTlvs() {
	return this.tlvs;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + ((this.error == null) ? 0 : this.error.hashCode());
	result = prime * result + ((this.tlvs == null) ? 0 : this.tlvs.hashCode());
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
	final PCEPErrorObject other = (PCEPErrorObject) obj;
	if (this.error != other.error)
	    return false;
	if (this.tlvs == null) {
	    if (other.tlvs != null)
		return false;
	} else if (!this.tlvs.equals(other.tlvs))
	    return false;
	return true;
    }

	@Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("error", this.error);
		toStringHelper.add("tlvs", this.tlvs);
		return super.addToStringAttributes(toStringHelper);
	}
}
