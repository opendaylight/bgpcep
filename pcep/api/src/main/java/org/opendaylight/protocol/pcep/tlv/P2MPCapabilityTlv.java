/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.tlv;

import org.opendaylight.protocol.pcep.PCEPTlv;

/**
 * Structure of P2MP Capability Tlv.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc6006#section-3.1.2">3.1.2. Open
 *      Message Extension [RFC6006]</a>
 */
public class P2MPCapabilityTlv implements PCEPTlv {
    private static final long serialVersionUID = -7959631526276210055L;

    private final int value;

    /**
     * Constructs new P2MP Capability Tlv.
     */
    public P2MPCapabilityTlv(int value) {
	if (value < 0 || value > 65535)
	    throw new IllegalArgumentException("Value (" + value + ") cannot be negative or bigger than 2^16 -1.");

	this.value = value;
    }

    /**
     * Constructs new P2MP Capability Tlv, with value defaultly set to zero as
     * mentioned in RFC6006.
     */
    public P2MPCapabilityTlv() {
	this.value = 0;
    }

    /**
     * Gets integer value of P2MP Capability Tlv.
     * 
     * @return int
     */
    public int getValue() {
	return this.value;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + this.value;
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
	final P2MPCapabilityTlv other = (P2MPCapabilityTlv) obj;
	if (this.value != other.value)
	    return false;
	return true;
    }

    @Override
    public String toString() {
	final StringBuilder builder = new StringBuilder();
	builder.append("P2MPCapablityTlv [value=");
	builder.append(this.value);
	builder.append("]");
	return builder.toString();
    }
}
