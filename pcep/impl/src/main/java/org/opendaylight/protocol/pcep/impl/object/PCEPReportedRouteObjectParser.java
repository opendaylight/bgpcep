/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithSubobjectsParser;
import org.opendaylight.protocol.pcep.spi.SubobjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ReportedRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.ReportedRouteBuilder;

/**
 * Parser for {@link ReportedRouteObject}
 */
public class PCEPReportedRouteObjectParser extends AbstractObjectWithSubobjectsParser<ReportedRouteBuilder> {

	public static final int CLASS = 8;

	public static final int TYPE = 1;

	public PCEPReportedRouteObjectParser(final SubobjectHandlerRegistry subobjReg) {
		super(subobjReg);
	}

	@Override
	public ReportedRouteObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
	PCEPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");
		}

		final ReportedRouteBuilder builder = new ReportedRouteBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		// FIXME: add subobjects
		return builder.build();
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof ReportedRouteObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass()
					+ ". Needed ReportedRouteObject.");
		}

		assert !(((ReportedRouteObject) object).getSubobjects().isEmpty()) : "Empty Reported Route Object.";
		// FIXME add subobjects
		// return PCEPRROSubobjectParser.put(((ReportedRouteObject) object).getSubobjects());
		return null;
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
