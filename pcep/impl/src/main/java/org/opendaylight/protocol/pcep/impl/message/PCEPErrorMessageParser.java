/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrorMapping;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.UnknownObject;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcepErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RpObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.Request;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.Session;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request.Rps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.session.Open;

import com.google.common.collect.Lists;

/**
 * Parser for {@link PcerrMessage}
 */
public class PCEPErrorMessageParser extends AbstractMessageParser {

	public static final int TYPE = 6;

	public PCEPErrorMessageParser(final ObjectHandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(final Message message, final ByteBuf buffer) {
		if (!(message instanceof PcerrMessage)) {
			throw new IllegalArgumentException("Wrong instance of Message. Passed instance " + message.getClass()
					+ ". Nedded ErrorMessage.");
		}
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessage err = ((PcerrMessage) message).getPcerrMessage();

		if (err.getErrors() == null || err.getErrors().isEmpty()) {
			throw new IllegalArgumentException("Errors should not be empty.");
		}

		if (err.getErrorType() instanceof Request) {
			final List<Rps> rps = ((Request) err.getErrorType()).getRps();
			for (final Rps r : rps) {
				buffer.writeBytes(serializeObject(r));
			}
		}

		for (final Errors e : err.getErrors()) {
			buffer.writeBytes(serializeObject(e));
		}

		if (err.getErrorType() instanceof Session) {
			buffer.writeBytes(serializeObject(((Session) err.getErrorType()).getOpen()));
		}
	}

	@Override
	public PcerrMessage parseMessage(final byte[] buffer) throws PCEPDeserializerException, PCEPDocumentedException {
		if (buffer == null || buffer.length == 0) {
			throw new PCEPDeserializerException("Error message is empty.");
		}
		final List<Object> objs = parseObjects(buffer);
		PcerrMessage m = null;
		try {
			m = validate(objs);
		} catch (final PCEPDocumentedException e) {
			final PCEPErrorMapping maping = PCEPErrorMapping.getInstance();
			return new PcerrBuilder().setPcerrMessage(
					new PcerrMessageBuilder().setErrors(
							Arrays.asList(new ErrorsBuilder().setType(maping.getFromErrorsEnum(e.getError()).type).setValue(
									maping.getFromErrorsEnum(e.getError()).value).build())).build()).build();
		}
		return m;
	}

	private PcerrMessage validate(final List<Object> objects) throws PCEPDeserializerException, PCEPDocumentedException {
		if (objects == null) {
			throw new IllegalArgumentException("Passed list can't be null.");
		}

		Open openObj = null;
		final List<Rps> requestParameters = Lists.newArrayList();
		final List<Errors> errorObjects = Lists.newArrayList();
		final PcerrMessageBuilder b = new PcerrMessageBuilder();

		Object obj;
		int state = 1;
		while (!objects.isEmpty()) {
			obj = objects.get(0);

			if (obj instanceof UnknownObject) {
				return new PcerrBuilder().setPcerrMessage(b.setErrors(((UnknownObject) obj).getErrors()).build()).build();
			}

			switch (state) {
			case 1:
				if (obj instanceof PcepErrorObject) {
					final PcepErrorObject o = (PcepErrorObject) obj;
					errorObjects.add((Errors) o);
					break;
				}
				state = 2;
			case 2:
				state = 3;
				if (obj instanceof OpenObject) {
					openObj = (Open) obj;
					break;
				}
			case 3:
				while (!objects.isEmpty()) {
					switch (state) {
					case 1:
						state = 2;
						if (obj instanceof RpObject) {
							final RpObject o = ((RpObject) obj);
							if (o.isProcessingRule()) {
								throw new PCEPDocumentedException("Invalid setting of P flag.", PCEPErrors.P_FLAG_NOT_SET);
							}
							requestParameters.add((Rps) o);
							state = 1;
							break;
						}
					case 2:
						if (obj instanceof PcepErrorObject) {
							final PcepErrorObject o = (PcepErrorObject) obj;
							errorObjects.add((Errors) o);
							state = 2;
							break;
						}
						state = 3;
					}

					if (state == 3) {
						break;
					}

					objects.remove(0);
				}

				state = 4;
				break;
			}

			if (state == 4) {
				break;
			}

			objects.remove(0);
		}

		if (errorObjects.isEmpty() && errorObjects.isEmpty()) {
			throw new PCEPDeserializerException("At least one PCEPErrorObject is mandatory.");
		}

		if (!objects.isEmpty()) {
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
		}
		if (requestParameters != null) {
			b.setErrorType(new RequestBuilder().setRps(requestParameters).build());
		}
		if (openObj != null) {
			b.setErrorType(new SessionBuilder().setOpen(openObj).build());
		}

		return new PcerrBuilder().setPcerrMessage(b.setErrors(errorObjects).build()).build();
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
