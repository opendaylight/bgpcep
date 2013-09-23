/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.path.attributes.CommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;

/**
 * Object representation of a RFC1997 Community tag. Communities are a way for the advertising entity to attach semantic
 * meaning/policy to advertised objects.
 */
public final class CommunityUtil {
	/**
	 * NO_EXPORT community. All routes received carrying a communities attribute containing this value MUST NOT be
	 * advertised outside a BGP confederation boundary (a stand-alone autonomous system that is not part of a
	 * confederation should be considered a confederation itself).
	 */
	public static final Community NO_EXPORT = CommunityUtil.buildNoExport();
	/**
	 * NO_ADVERTISE community. All routes received carrying a communities attribute containing this value MUST NOT be
	 * advertised to other BGP peers.
	 */
	public static final Community NO_ADVERTISE = CommunityUtil.buildNoAdvertise();
	/**
	 * NO_EXPORT_SUBCONFED community. All routes received carrying a communities attribute containing this value MUST
	 * NOT be advertised to external BGP peers (this includes peers in other members autonomous systems inside a BGP
	 * confederation).
	 */
	public static final Community NO_EXPORT_SUBCONFED = CommunityUtil.buildNoExportSubconfed();

	private static Community buildNoExport() {
		final CommunitiesBuilder builder = new CommunitiesBuilder();
		builder.setAsNumber(new AsNumber((long) 0xFFFF));
		builder.setSemantics(0xFF01L);
		return builder.build();
	}

	private static Community buildNoAdvertise() {
		final CommunitiesBuilder builder = new CommunitiesBuilder();
		builder.setAsNumber(new AsNumber((long) 0xFFFF));
		builder.setSemantics(0xFF02L);
		return builder.build();
	}

	private static Community buildNoExportSubconfed() {
		final CommunitiesBuilder builder = new CommunitiesBuilder();
		builder.setAsNumber(new AsNumber((long) 0xFFFF));
		builder.setSemantics(0xFF03L);
		return builder.build();
	}

	/**
	 * Creates a new Community given AS number value and semantics using generated CommunitiesBuilder.
	 * 
	 * @param asn long
	 * @param semantics long
	 * @return new Community
	 */
	public static Community create(final long asn, final long semantics) {
		final CommunitiesBuilder builder = new CommunitiesBuilder();
		builder.setAsNumber(new AsNumber(asn));
		builder.setSemantics(semantics);
		return builder.build();
	}

	/**
	 * Creates a Community from its String representation.
	 * 
	 * @param string String representation of a community
	 * @return new Community
	 */
	public static Community valueOf(final String string) {
		final String[] parts = string.split(":", 2);
		final CommunitiesBuilder builder = new CommunitiesBuilder();
		builder.setAsNumber(new AsNumber(Long.valueOf(parts[0])));
		builder.setSemantics(Long.valueOf(parts[1]));
		return builder.build();
	}
}
