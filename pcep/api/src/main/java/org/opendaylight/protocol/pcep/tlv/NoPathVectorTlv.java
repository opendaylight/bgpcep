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
 * Structure of No Path Vector TLV. Extended to conform RFC5557.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.5">NO-PATH Object
 *      [RFC5440]</a> - defined in text
 * @see <a href="http://tools.ietf.org/html/rfc5557#section-5.7">NO-PATH
 *      Indicator [RFC5557]</a>
 */
public class NoPathVectorTlv implements PCEPTlv {

	private static final long serialVersionUID = -4993945476359800826L;

	private final boolean pceUnavailable;

	private final boolean unknownDest;

	private final boolean unknownSrc;

	private final boolean noGCOSolution;

	private final boolean noGCOMigrationPath;

	private final boolean reachablityProblem;

	/**
	 * Constructs new No Path Vector Tlv.
	 * 
	 * @param pceUnavailable
	 *            boolean
	 * @param unknownDest
	 *            boolean
	 * @param unknownSrc
	 *            boolean
	 * @param noGCOSolution
	 *            boolean
	 * @param noGCOMigrationPath
	 *            boolean
	 */
	public NoPathVectorTlv(boolean pceUnavailable, boolean unknownDest, boolean unknownSrc, boolean noGCOSolution, boolean noGCOMigrationPath,
			boolean reachabilityProblem) {
		super();
		this.pceUnavailable = pceUnavailable;
		this.unknownDest = unknownDest;
		this.unknownSrc = unknownSrc;
		this.noGCOSolution = noGCOSolution;
		this.noGCOMigrationPath = noGCOMigrationPath;
		this.reachablityProblem = reachabilityProblem;
	}

	/**
	 * Returns true if PCE currently unavailable
	 * 
	 * @return boolean
	 */
	public boolean isPceUnavailable() {
		return this.pceUnavailable;
	}

	/**
	 * Returns true if unknown destination
	 * 
	 * @return boolean
	 */
	public boolean isUnknownDest() {
		return this.unknownDest;
	}

	/**
	 * Returns true if unknown source
	 * 
	 * @return boolean
	 */
	public boolean isUnknownSrc() {
		return this.unknownSrc;
	}

	/**
	 * If returns true the PCE indicates that no migration path was found.
	 * 
	 * @return boolean
	 */
	public boolean isNoGCOSolution() {
		return this.noGCOSolution;
	}

	/**
	 * If returns true the PCE indicates no feasible solution was found that
	 * meets all the constraints associated with global concurrent path
	 * optimization in the PCRep message
	 * 
	 * @return boolean
	 */
	public boolean isNoGCOMigrationPath() {
		return this.noGCOMigrationPath;
	}

	/**
	 * @return the reachablityProblem
	 */
	public boolean isReachablityProblem() {
		return this.reachablityProblem;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.noGCOMigrationPath ? 1231 : 1237);
		result = prime * result + (this.noGCOSolution ? 1231 : 1237);
		result = prime * result + (this.pceUnavailable ? 1231 : 1237);
		result = prime * result + (this.reachablityProblem ? 1231 : 1237);
		result = prime * result + (this.unknownDest ? 1231 : 1237);
		result = prime * result + (this.unknownSrc ? 1231 : 1237);
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
		final NoPathVectorTlv other = (NoPathVectorTlv) obj;
		if (this.noGCOMigrationPath != other.noGCOMigrationPath)
			return false;
		if (this.noGCOSolution != other.noGCOSolution)
			return false;
		if (this.pceUnavailable != other.pceUnavailable)
			return false;
		if (this.reachablityProblem != other.reachablityProblem)
			return false;
		if (this.unknownDest != other.unknownDest)
			return false;
		if (this.unknownSrc != other.unknownSrc)
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("NoPathVectorTlv [pceUnavailable=");
		builder.append(this.pceUnavailable);
		builder.append(", unknownDest=");
		builder.append(this.unknownDest);
		builder.append(", unknownSrc=");
		builder.append(this.unknownSrc);
		builder.append(", noGCOSolution=");
		builder.append(this.noGCOSolution);
		builder.append(", noGCOMigrationPath=");
		builder.append(this.noGCOMigrationPath);
		builder.append(", reachablityProblem=");
		builder.append(this.reachablityProblem);
		builder.append("]");
		return builder.toString();
	}

}
