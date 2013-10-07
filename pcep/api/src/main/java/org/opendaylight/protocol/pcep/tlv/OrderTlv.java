/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.tlv;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * The Order TLV is an optional TLV in the RP object, that indicates the order
 * in which the old TE LSP must be removed and the new TE LSP must be setup
 * during a reoptimization. It is carried in the PCRep message in response to a
 * reoptimization request.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5557#section-5.4">The Order
 *      Response [RFC5557]</a>
 */
public class OrderTlv implements Tlv {

	private final long deleteOrder;
	private final long setupOrder;

	/**
	 * Constructs new Order tlv with all mandatory objects.
	 * 
	 * @param deleteOrder
	 *            32-bit integer
	 * @param setupOrder
	 *            32-bit integer
	 */
	public OrderTlv(long deleteOrder, long setupOrder) {
		super();
		this.deleteOrder = deleteOrder;
		this.setupOrder = setupOrder;
	}

	/**
	 * Gets the delete order
	 * 
	 * @return the delete order
	 */
	public long getDeleteOrder() {
		return this.deleteOrder;
	}

	/**
	 * Gets the setup order
	 * 
	 * @return the setup order
	 */
	public long getSetupOrder() {
		return this.setupOrder;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("OrderTlv [deleteOrder=");
		builder.append(this.deleteOrder);
		builder.append(", setupOrder=");
		builder.append(this.setupOrder);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.deleteOrder ^ (this.deleteOrder >>> 32));
		result = prime * result + (int) (this.setupOrder ^ (this.setupOrder >>> 32));
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
		final OrderTlv other = (OrderTlv) obj;
		if (this.deleteOrder != other.deleteOrder)
			return false;
		if (this.setupOrder != other.setupOrder)
			return false;
		return true;
	}

}
