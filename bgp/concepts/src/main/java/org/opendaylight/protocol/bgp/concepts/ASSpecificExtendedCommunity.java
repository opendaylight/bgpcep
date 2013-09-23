/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import java.io.Serializable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

/**
 * Class representing an <a href="http://tools.ietf.org/html/rfc4360#section-3.1">Two-Octet AS Specific Extended
 * Community</a>.
 */
public class ASSpecificExtendedCommunity extends ExtendedCommunity implements Serializable {
	private static final long serialVersionUID = 6490173838144366385L;
	private final AsNumber globalAdmin;
	private final byte[] localAdmin;
	private final int subType;

	/**
	 * Construct a new Two-Octet AS Specific Extended Community.
	 * 
	 * @param transitive True if this community is transitive
	 * @param subType Community subtype, has to be in range 0-255
	 * @param globalAdmin Globally-assigned namespace (AS number)
	 * @param localAdmin Locally-assigned value, has to be 4 bytes long
	 */
	public ASSpecificExtendedCommunity(final boolean transitive, final int subType, final AsNumber globalAdmin, final byte[] localAdmin) {
		super(false, transitive);
		Preconditions.checkArgument(subType > 0 && subType < 255, "Invalid Sub-Type");
		Preconditions.checkArgument(localAdmin.length == 4, "Invalid Local Administrator");
		this.subType = subType;
		this.globalAdmin = Preconditions.checkNotNull(globalAdmin);
		this.localAdmin = localAdmin;
	}

	/**
	 * @return Community subtype
	 */
	public final int getSubType() {
		return this.subType;
	}

	/**
	 * @return Globally-assigned namespace
	 */
	public final AsNumber getGlobalAdmin() {
		return this.globalAdmin;
	}

	/**
	 * @return Locally-assigned value
	 */
	public final byte[] getLocalAdmin() {
		return this.localAdmin;
	}

	@Override
	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("subType", this.subType);
		toStringHelper.add("globalAdmin", this.globalAdmin);
		toStringHelper.add("localAdmin", this.localAdmin);
		return super.addToStringAttributes(toStringHelper);
	}
}
