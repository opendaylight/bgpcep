/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import java.util.Arrays;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.protocol.bgp.parser.spi.ParameterUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for BGP Capability Parameter.
 */
public final class CapabilityParameterParser implements ParameterParser, ParameterSerializer {
	public static final int TYPE = 2;

	private static final Logger logger = LoggerFactory.getLogger(CapabilityParameterParser.class);
	private final CapabilityRegistry reg;

	public CapabilityParameterParser(final CapabilityRegistry reg) {
		this.reg = Preconditions.checkNotNull(reg);
	}

	@Override
	public BgpParameters parseParameter(final byte[] bytes) throws BGPParsingException, BGPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Byte array cannot be null or empty.");
		}
		logger.trace("Started parsing of BGP Capability: {}", Arrays.toString(bytes));
		int byteOffset = 0;
		final int capCode = UnsignedBytes.toInt(bytes[byteOffset++]);
		final int capLength = UnsignedBytes.toInt(bytes[byteOffset++]);
		final byte[] paramBody = ByteArray.subByte(bytes, byteOffset, capLength);

		final CParameters ret = reg.parseCapability(capCode, paramBody);
		if (ret == null) {
			logger.debug("Ignoring unsupported capability {}", capCode);
			return null;
		}

		return new BgpParametersBuilder().setCParameters(ret).build();
	}

	@Override
	public byte[] serializeParameter(final BgpParameters parameter) {
		final CParameters cap = parameter.getCParameters();

		logger.trace("Started serializing BGP Capability: {}", cap);

		byte[] bytes = reg.serializeCapability(cap);
		if (bytes == null) {
			throw new IllegalArgumentException("Unhandled capability class" + cap.getImplementedInterface());
		}

		logger.trace("BGP capability serialized to: {}", Arrays.toString(bytes));

		return ParameterUtil.formatParameter(TYPE, bytes);
	}
}
