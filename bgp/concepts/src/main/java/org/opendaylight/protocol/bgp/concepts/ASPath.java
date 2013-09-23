/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Conceptual representation of an AS path. This class distills the concept behind the encoding rules specified in RFC
 * 4271.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-5.1.2">AS Path</a>
 */
public final class ASPath implements Serializable {
	/**
	 * An empty AS Path (attribute in all UPDATE messages sent to internal peers).
	 */
	public static final ASPath EMPTY = new ASPath();
	private static final long serialVersionUID = 7951172606939897308L;
	private final Set<AsNumber> aggregatedAsPath;
	private final List<AsNumber> visibleAsPath;

	private ASPath() {
		this.visibleAsPath = Collections.emptyList();
		this.aggregatedAsPath = Collections.emptySet();
	}

	/**
	 * Construct an AS path object representing a fully-visible path.
	 * 
	 * @param visibleAsPath Ordered list of AS numbers in the path, corresponding to the concatenation of all
	 *        AS_SEQUENCE components.
	 */
	public ASPath(final List<AsNumber> visibleAsPath) {
		this.aggregatedAsPath = Collections.emptySet();
		this.visibleAsPath = Collections.unmodifiableList(Preconditions.checkNotNull(visibleAsPath));
	}

	/**
	 * Construct an AS path object representing a partially aggregated path.
	 * 
	 * @param visibleAsPath Ordered list of AS numbers in the path, corresponding to the concatenation of all
	 *        AS_SEQUENCE components.
	 * @param aggregatedAsPath Unordered set of AS numbers in the path, corresponding to the concatenation of all AS_SET
	 *        components.
	 */
	public ASPath(final List<AsNumber> visibleAsPath, final Set<AsNumber> aggregatedAsPath) {
		Preconditions.checkNotNull(aggregatedAsPath);
		Preconditions.checkNotNull(visibleAsPath);
		this.aggregatedAsPath = Collections.unmodifiableSet(aggregatedAsPath);
		this.visibleAsPath = Collections.unmodifiableList(visibleAsPath);
	}

	/**
	 * Return the visible part of an AS path. Note that an AS number may be present multiple times in a row due to
	 * "AS number prepend" feature used by many routers to affect route decisions by extending the length on the list
	 * while keeping the loop information intact.
	 * 
	 * @return Ordered list of AS numbers.
	 */
	public List<AsNumber> getVisibleAsPath() {
		return this.visibleAsPath;
	}

	/**
	 * Return the aggregated part of an AS path.
	 * 
	 * @return Unordered set of AS numbers.
	 */
	public Set<AsNumber> getAggregatedAsPath() {
		return this.aggregatedAsPath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.aggregatedAsPath == null) ? 0 : this.aggregatedAsPath.hashCode());
		result = prime * result + ((this.visibleAsPath == null) ? 0 : this.visibleAsPath.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final ASPath other = (ASPath) obj;
		if (this.aggregatedAsPath == null) {
			if (other.aggregatedAsPath != null)
				return false;
		} else if (!this.aggregatedAsPath.equals(other.aggregatedAsPath))
			return false;
		if (this.visibleAsPath == null) {
			if (other.visibleAsPath != null)
				return false;
		} else if (!this.visibleAsPath.equals(other.visibleAsPath))
			return false;
		return true;
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("aggregatedAsPath", this.aggregatedAsPath);
		toStringHelper.add("visibleAsPath", this.visibleAsPath);
		return toStringHelper;
	}
}
