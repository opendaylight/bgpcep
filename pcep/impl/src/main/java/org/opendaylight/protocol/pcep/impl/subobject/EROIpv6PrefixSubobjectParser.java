/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.impl.object.EROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link IpPrefixCase}
 */
public class EROIpv6PrefixSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

	public static final int TYPE = 2;

	private static final int IP_F_LENGTH = 16;
	private static final int PREFIX_F_LENGTH = 1;

	private static final int IP_F_OFFSET = 0;
	private static final int PREFIX_F_OFFSET = IP_F_OFFSET + IP_F_LENGTH;

	private static final int CONTENT_LENGTH = PREFIX_F_OFFSET + PREFIX_F_LENGTH + 1;

	@Override
	public Subobjects parseSubobject(final byte[] buffer, final boolean loose) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		final SubobjectsBuilder builder = new SubobjectsBuilder();
		builder.setLoose(loose);

		if (buffer.length != CONTENT_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + ";");
		}
		final int length = UnsignedBytes.toInt(buffer[PREFIX_F_OFFSET]);
		builder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
				new IpPrefixBuilder().setIpPrefix(new IpPrefix(Ipv6Util.prefixForBytes(ByteArray.subByte(buffer, 0, IP_F_LENGTH), length))).build()).build());
		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {
		if (!(subobject.getSubobjectType() instanceof IpPrefixCase)) {
			throw new IllegalArgumentException("Unknown subobject instance. Passed " + subobject.getSubobjectType().getClass()
					+ ". Needed IpPrefixCase.");
		}
		final IpPrefixSubobject specObj = ((IpPrefixCase) subobject.getSubobjectType()).getIpPrefix();
		final IpPrefix prefix = specObj.getIpPrefix();

		final byte[] retBytes = new byte[CONTENT_LENGTH];
		ByteArray.copyWhole(Ipv6Util.bytesForPrefix(prefix.getIpv6Prefix()), retBytes, 0);
		retBytes[PREFIX_F_OFFSET] = UnsignedBytes.checkedCast(Ipv4Util.getPrefixLength(prefix));
		return EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), retBytes);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
