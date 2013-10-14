/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import org.opendaylight.protocol.concepts.AbstractMetric;
import org.opendaylight.protocol.pcep.PCEPObject;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Structure of PCEP Metric Object.
 *
 * @see <a href="http://tools.ietf.org/html/rfc5440#section-7.8">PCEP Metric
 *      Object</a>
 */
public class PCEPMetricObject extends PCEPObject {

    private final boolean bound;

    private final boolean computedMetric;

    private final AbstractMetric<?> metric;

    /**
     * Constructs PCEP Metric Object.
     *
     * @param bound
     *            boolean
     * @param computedMetric
     *            boolean
     * @param metric
     *            cannot be null
     * @param processed
     *            boolean
     * @param ignored
     *            boolean
     */
    public PCEPMetricObject(boolean bound, boolean computedMetric, AbstractMetric<?> metric, boolean processed, boolean ignored) {
	super(processed, ignored);
	this.bound = bound;
	this.computedMetric = computedMetric;
	if (metric == null)
	    throw new IllegalArgumentException("Mandatory parameter metric cannot be null.");
	this.metric = metric;
    }

    /**
     * Gets Bound flag.
     *
     * @return boolean
     */
    public boolean isBound() {
	return this.bound;
    }

    /**
     * Gets Computed Metric flag.
     *
     * @return boolean
     */
    public boolean isComputedMetric() {
	return this.computedMetric;
    }

    /**
     * Gets {@link AbstractMetric<?>}.
     *
     * @return AbstractMetric<?>
     */
    public AbstractMetric<?> getMetric() {
	return this.metric;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + (this.bound ? 1231 : 1237);
	result = prime * result + (this.computedMetric ? 1231 : 1237);
	result = prime * result + ((this.metric == null) ? 0 : this.metric.hashCode());
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
	final PCEPMetricObject other = (PCEPMetricObject) obj;
	if (this.bound != other.bound)
	    return false;
	if (this.computedMetric != other.computedMetric)
	    return false;
	if (this.metric == null) {
	    if (other.metric != null)
		return false;
	} else if (!this.metric.equals(other.metric))
	    return false;
	return true;
    }

	@Override
	protected ToStringHelper addToStringAttributes(ToStringHelper toStringHelper) {
		toStringHelper.add("bound", this.bound);
		toStringHelper.add("computedMetric", this.computedMetric);
		toStringHelper.add("metric", this.metric);
		return super.addToStringAttributes(toStringHelper);
	}
}
