/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.UnknownObject;
import org.opendaylight.protocol.pcep.impl.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.EndpointsObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspaObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcinitiateMessage;

import com.google.common.collect.Lists;

/**
 * Parser for {@link PCCreateMessage}
 */
public class PCCreateMessageParser extends AbstractMessageParser {

	public static final int TYPE = 12;

	public PCCreateMessageParser(final ObjectHandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(final Message message, final ByteBuf buffer) {
		if (!(message instanceof PcinitiateMessage)) {
			throw new IllegalArgumentException("Wrong instance of Message. Passed instance of " + message.getClass()
					+ ". Needed PcinitiateMessage.");
		}
		// final PcinitiateMessage init =
	}

	@Override
	public Message parseMessage(final byte[] buffer) throws PCEPDeserializerException, PCEPDocumentedException {
		if (buffer == null || buffer.length == 0) {
			throw new PCEPDeserializerException("Initiate message cannot be empty.");
		}
		final List<Object> objs = parseObjects(buffer);
		return validate(objs);
	}

	public Message validate(final List<Object> objects) throws PCEPDeserializerException {
		if (objects == null) {
			throw new IllegalArgumentException("Passed list can't be null.");
		}
		return null;
		// final List<CompositeInstantiationObject> insts = new ArrayList<CompositeInstantiationObject>();
		//
		// CompositeInstantiationObject inst;
		// while (!objects.isEmpty()) {
		// try {
		// if ((inst = this.getValidInstantiationObject(objects)) == null) {
		// break;
		// }
		// } catch (final PCEPDocumentedException e) {
		// return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(e.getError())));
		// }
		//
		// insts.add(inst);
		// }
		//
		// if (insts.isEmpty()) {
		// throw new PCEPDeserializerException("At least one CompositeInstantiationObject is mandatory.");
		// }
		//
		// if (!objects.isEmpty()) {
		// throw new PCEPDeserializerException("Unprocessed objects: " + objects);
		// }
		//
		// return Arrays.asList((Message) new PCCreateMessage(insts));
	}

	private void getValidInstantiationObject(final List<Object> objects) throws PCEPDocumentedException {
		if (objects.get(0) instanceof UnknownObject) {
			throw new PCEPDocumentedException("Unknown object", ((UnknownObject) objects.get(0)).getError());
		}
		if (!(objects.get(0) instanceof EndpointsObject)) {
			return;
		}

		final EndpointsObject endPoints = ((EndpointsObject) objects.get(0));
		objects.remove(0);

		if (objects.get(0) instanceof UnknownObject) {
			throw new PCEPDocumentedException("Unknown object", ((UnknownObject) objects.get(0)).getError());
		}
		if (!(objects.get(0) instanceof LspaObject)) {
			// throw new PCEPDocumentedException("LSPA Object must be second.", PCEPErrors.LSPA_MISSING);
		}
		final LspaObject lspa = (LspaObject) objects.get(0);
		objects.remove(0);

		ExplicitRouteObject ero = null;
		BandwidthObject bandwidth = null;
		final List<MetricObject> metrics = Lists.newArrayList();

		Object obj;
		int state = 1;
		while (!objects.isEmpty()) {
			obj = objects.get(0);
			if (obj instanceof UnknownObject) {
				throw new PCEPDocumentedException("Unknown object", ((UnknownObject) obj).getError());
			}

			switch (state) {
			case 1:
				state = 2;
				if (obj instanceof ExplicitRouteObject) {
					ero = (ExplicitRouteObject) obj;
					break;
				}
			case 2:
				state = 3;
				if (obj instanceof BandwidthObject) {
					bandwidth = (BandwidthObject) obj;
					break;
				}
			case 3:
				state = 4;
				if (obj instanceof MetricObject) {
					metrics.add((MetricObject) obj);
					state = 3;
					break;
				}
			}

			if (state == 4) {
				break;
			}

			objects.remove(0);
		}

		// return new CompositeInstantiationObject(endPoints, lspa, ero, bandwidth, metrics);
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
