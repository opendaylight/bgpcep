/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.message;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.object.CompositeStateReportObject;

/**
 * Structure of Report Message
 *
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-6.1">State
 *      Report Message</a>
 */
public class PCEPReportMessage extends PCEPMessage {

	private static final long serialVersionUID = -3319055709351802748L;

	private final List<CompositeStateReportObject> reports;

	/**
	 * Constructs {@link PCEPReportMessage}.
	 *
	 * @throws IllegalArgumentException
	 *             if there is not even one {@link CompositeStateReportObject}
	 *             in the list.
	 *
	 * @param reports
	 *            List<CompositeStateReportObject>. Can't be empty or null.
	 */
	public PCEPReportMessage(final List<CompositeStateReportObject> reports) {
		super(new ArrayList<PCEPObject>() {

			private static final long serialVersionUID = 2785287687806615951L;

			{
				if (reports != null)
					for (final CompositeStateReportObject csro : reports) {
						this.addAll(csro.getCompositeAsList());
					}
			}
		});
		if (reports == null || reports.isEmpty())
			throw new IllegalArgumentException("At least one CompositeStateReportObject is mandatory.");

		this.reports = reports;
	}

	/**
	 * Gets list of {@link CompositeStateReportObject}.
	 *
	 * @return List<CompositeStateReportObject>. Can't be null or empty.
	 */
	public List<CompositeStateReportObject> getStateReports() {
		return this.reports;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.reports == null) ? 0 : this.reports.hashCode());
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
		final PCEPReportMessage other = (PCEPReportMessage) obj;
		if (this.reports == null) {
			if (other.reports != null)
				return false;
		} else if (!this.reports.equals(other.reports))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PCEPReportMessage [reports=");
		builder.append(this.reports);
		builder.append("]");
		return builder.toString();
	}
}
