/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.Arrays;

import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;

/**
 * Parser for {@link Bandwidth}
 */
public class PCEPExistingBandwidthObjectParser extends AbstractBandwidthParser {

	public static final int CLASS = 5;

	public static final int TYPE = 2;

	private static final int BANDWIDTH_LENGTH = 4;

	public PCEPExistingBandwidthObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof Bandwidth)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed BandwidthObject.");
		}
		final byte[] retBytes = Arrays.copyOf(((Bandwidth) object).getBandwidth().getValue(), BANDWIDTH_LENGTH);
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
