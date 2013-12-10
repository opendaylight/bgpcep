/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;

/**
 * Parser for {@link Bandwidth}
 */
public class PCEPExistingBandwidthObjectParser extends AbstractBandwidthParser {

	public static final int CLASS = 5;

	public static final int TYPE = 2;

	public PCEPExistingBandwidthObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
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
