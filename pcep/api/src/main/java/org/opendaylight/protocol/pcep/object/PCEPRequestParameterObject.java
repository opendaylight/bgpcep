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

import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.tlv.OrderTlv;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure of PCEP Requested Parameter Object.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.4">PCEP Request
 *      Parameter Object [RFC5440]</a>
 * @see <a href="http://tools.ietf.org/html/rfc5541#section-6.2.4">RP Object
 *      Flag [RFC5541]</a>
 * @see <a href="http://tools.ietf.org/html/rfc6006#section-3.3.1">The Extension
 *      of the RP Object [RFC6006]</a>
 */
public class PCEPRequestParameterObject extends PCEPObject {

    private final boolean loose;

    private final boolean bidirectional;

    private final boolean reoptimized;

    private final boolean makeBeforeBreak;

    private final boolean reportRequestOrder;

    private final boolean suplyOFOnResponse;

    /*
     * RFC6006 flags
     */
    private final boolean fragmentation;

    private final boolean p2mp;

    private final boolean eroCompression;

    // End of flags

    private final short priority;

    private final long requestID;

    private final List<PCEPTlv> tlvs;

    /**
     * Constructs PCEP Requested Parameter Object only with mandatory values.
     *
     * @param loose
     *            boolean
     * @param bidirectional
     *            boolean
     * @param reoptimized
     *            boolean
     * @param makeBeforeBreak
     *            boolean
     * @param suplyOFOnResponse
     *            boolean
     * @param priority
     *            short
     * @param requestID
     *            long
     * @param processed
     *            boolean
     * @param ignored
     *            boolean;
     */
    public PCEPRequestParameterObject(boolean loose, boolean bidirectional, boolean reoptimized, boolean makeBeforeBreak, boolean suplyOFOnResponse,
	    boolean fragmentation, boolean p2mp, boolean eroCompression, short priority, long requestID, boolean processed, boolean ignored) {
	this(loose, bidirectional, reoptimized, makeBeforeBreak, false, suplyOFOnResponse, fragmentation, p2mp, eroCompression, priority, requestID, null,
		processed, ignored);
    }

    /**
     * Constructs PCEP Requested Parameter Object also with optional Objects.
     *
     * @param loose
     *            boolean
     * @param bidirectional
     *            boolean
     * @param reoptimized
     *            boolean
     * @param makeBeforeBreak
     *            boolean
     * @param reportRequestOrder
     *            boolean
     * @param suplyOFOnResponse
     *            boolean
     * @param priority
     *            short
     * @param requestID
     *            long
     * @param tlvs
     *            List<PCEPTlv>
     * @param processed
     *            boolean
     * @param ignored
     *            boolean
     */
    public PCEPRequestParameterObject(boolean loose, boolean bidirectional, boolean reoptimized, boolean makeBeforeBreak, boolean reportRequestOrder,
	    boolean suplyOFOnResponse, boolean fragmentation, boolean p2mp, boolean eroCompression, short priority, long requestID, List<PCEPTlv> tlvs,
	    boolean processed, boolean ignored) {
	super(processed, ignored);
	this.loose = loose;
	this.bidirectional = bidirectional;
	this.reoptimized = reoptimized;
	this.makeBeforeBreak = makeBeforeBreak;
	this.reportRequestOrder = reportRequestOrder;
	this.suplyOFOnResponse = suplyOFOnResponse;
	this.fragmentation = fragmentation;
	this.p2mp = p2mp;
	this.eroCompression = eroCompression;

	this.priority = priority;
	this.requestID = requestID;
	if (tlvs != null)
	    this.tlvs = tlvs;
	else
	    this.tlvs = Collections.emptyList();

	if (makeBeforeBreak && !reoptimized)
	    throw new IllegalArgumentException("M (make-before-break) can be set only if R (reoptimied) flag is set too.");

	if (reportRequestOrder && !this.validateOrderTlv())
	    throw new IllegalArgumentException("D (report request order) flag is set, but missing OrderTlv.");
    }

    /**
     * Gets Loose flag.
     *
     * @return boolean
     */
    public boolean isLoose() {
	return this.loose;
    }

    /**
     * Gets Bidirectional flag.
     *
     * @return boolean
     */
    public boolean isBidirectional() {
	return this.bidirectional;
    }

    /**
     * Gets Reoptimization flag.
     *
     * @return boolean
     */
    public boolean isReoptimized() {
	return this.reoptimized;
    }

    /**
     * Gets make-before-break flag
     *
     * @return boolean
     */
    public boolean isMakeBeforeBreak() {
	return this.makeBeforeBreak;
    }

    /**
     * Gets report requested order flag
     *
     * @return boolean
     */
    public boolean isReportRequestOrder() {
	return this.reportRequestOrder;
    }

    /**
     * Gets report supply objective function on response flag
     *
     * @return boolean
     */
    public boolean isSuplyOFOnResponse() {
	return this.suplyOFOnResponse;
    }

    /**
     * @return the fragmentation
     */
    public boolean isFragmentation() {
	return this.fragmentation;
    }

    /**
     * @return the p2mp
     */
    public boolean isP2mp() {
	return this.p2mp;
    }

    /**
     * @return the eroCompression
     */
    public boolean isEroCompression() {
	return this.eroCompression;
    }

    /**
     * Returns short representation of Priority.
     *
     * @return short
     */
    public short getPriority() {
	return this.priority;
    }

    /**
     * Returns long representation of Requested ID.
     *
     * @return long
     */
    public long getRequestID() {
	return this.requestID;
    }

    /**
     * Gets list of {@link PCEPTlv}
     *
     * @return List<PCEPTlv>. Can't be null, but may be empty.
     */
    public List<PCEPTlv> getTlvs() {
	return this.tlvs;
    }

    private boolean validateOrderTlv() {
	for (final PCEPTlv tlv : this.tlvs) {
	    if (tlv instanceof OrderTlv)
		return true;
	}

	return false;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + (this.bidirectional ? 1231 : 1237);
	result = prime * result + (this.eroCompression ? 1231 : 1237);
	result = prime * result + (this.fragmentation ? 1231 : 1237);
	result = prime * result + (this.loose ? 1231 : 1237);
	result = prime * result + (this.makeBeforeBreak ? 1231 : 1237);
	result = prime * result + (this.p2mp ? 1231 : 1237);
	result = prime * result + this.priority;
	result = prime * result + (this.reoptimized ? 1231 : 1237);
	result = prime * result + (this.reportRequestOrder ? 1231 : 1237);
	result = prime * result + (int) (this.requestID ^ (this.requestID >>> 32));
	result = prime * result + (this.suplyOFOnResponse ? 1231 : 1237);
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
	final PCEPRequestParameterObject other = (PCEPRequestParameterObject) obj;
	if (this.bidirectional != other.bidirectional)
	    return false;
	if (this.eroCompression != other.eroCompression)
	    return false;
	if (this.fragmentation != other.fragmentation)
	    return false;
	if (this.loose != other.loose)
	    return false;
	if (this.makeBeforeBreak != other.makeBeforeBreak)
	    return false;
	if (this.p2mp != other.p2mp)
	    return false;
	if (this.priority != other.priority)
	    return false;
	if (this.reoptimized != other.reoptimized)
	    return false;
	if (this.reportRequestOrder != other.reportRequestOrder)
	    return false;
	if (this.requestID != other.requestID)
	    return false;
	if (this.suplyOFOnResponse != other.suplyOFOnResponse)
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
		toStringHelper.add("loose", this.loose);
		toStringHelper.add("bidirectional", this.bidirectional);
		toStringHelper.add("reoptimized", this.reoptimized);
		toStringHelper.add("makeBeforeBreak", this.makeBeforeBreak);
		toStringHelper.add("reportRequestOrder", this.reportRequestOrder);
		toStringHelper.add("suplyOFOnResponse", this.suplyOFOnResponse);
		toStringHelper.add("fragmentation", this.fragmentation);
		toStringHelper.add("p2mp", this.p2mp);
		toStringHelper.add("eroCompression", this.eroCompression);
		toStringHelper.add("priority", this.priority);
		toStringHelper.add("requestID", this.requestID);
		toStringHelper.add("tlvs", this.tlvs);
		return super.addToStringAttributes(toStringHelper);
	}

}
