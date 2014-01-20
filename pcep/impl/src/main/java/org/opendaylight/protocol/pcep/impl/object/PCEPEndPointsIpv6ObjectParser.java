/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for IPv6 {@link EndpointsObj}
 */
public class PCEPEndPointsIpv6ObjectParser extends AbstractObjectWithTlvsParser<EndpointsObjBuilder> {

	private static final Logger LOG = LoggerFactory.getLogger(PCEPEndPointsIpv4ObjectParser.class);

	public static final int CLASS = 4;
	public static final int TYPE = 2;

	/*
	 * fields lengths and offsets for IPv4 in bytes
	 */
	private static final int SRC6_F_LENGTH = 16;
	private static final int DEST6_F_LENGTH = 16;

	private static final int SRC6_F_OFFSET = 0;
	private static final int DEST6_F_OFFSET = SRC6_F_OFFSET + SRC6_F_LENGTH;

	public PCEPEndPointsIpv6ObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public Object parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null) {
			throw new IllegalArgumentException("Array of bytes is mandatory");
		}
		final EndpointsObjBuilder builder = new EndpointsObjBuilder();
		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		if (!builder.isProcessingRule()) {
			LOG.debug("Processed bit not set on Endpoints OBJECT, ignoring it.");
			return new UnknownObject(PCEPErrors.P_FLAG_NOT_SET, builder.build());
		}
		if (bytes.length != SRC6_F_LENGTH + DEST6_F_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes.");
		}
		final Ipv6Builder b = new Ipv6Builder();
		b.setSourceIpv6Address(Ipv6Util.addressForBytes(ByteArray.subByte(bytes, SRC6_F_OFFSET, SRC6_F_LENGTH)));
		b.setDestinationIpv6Address((Ipv6Util.addressForBytes(ByteArray.subByte(bytes, DEST6_F_OFFSET, DEST6_F_LENGTH))));
		builder.setAddressFamily(new Ipv6CaseBuilder().setIpv6(b.build()).build());
		return builder.build();
	}

	@Override
	public void addTlv(final EndpointsObjBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof EndpointsObj)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed EndpointsObject.");
		}
		final EndpointsObj ePObj = (EndpointsObj) object;

		final AddressFamily afi = ePObj.getAddressFamily();

		if (!(afi instanceof Ipv6Case)) {
			throw new IllegalArgumentException("Wrong instance of NetworkAddress. Passed " + afi.getClass() + ". Needed IPv4");
		}
		final byte[] retBytes = new byte[SRC6_F_LENGTH + DEST6_F_LENGTH];
		ByteArray.copyWhole(Ipv6Util.bytesForAddress((((Ipv6Case) afi).getIpv6()).getSourceIpv6Address()), retBytes, SRC6_F_OFFSET);
		ByteArray.copyWhole(Ipv6Util.bytesForAddress((((Ipv6Case) afi).getIpv6()).getDestinationIpv6Address()), retBytes, DEST6_F_OFFSET);
		return ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), retBytes);
	}

	@Override
	public int getObjectType() {
		return TYPE;
	}

	@Override
	public int getObjectClass() {
		return CLASS;
	}
}
