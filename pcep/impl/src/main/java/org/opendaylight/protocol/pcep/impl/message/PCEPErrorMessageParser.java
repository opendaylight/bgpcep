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

import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.Request;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.Session;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request.Rps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

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
				buffer.writeBytes(serializeObject(r.getRp()));
			}
		}

		for (final Errors e : err.getErrors()) {
			buffer.writeBytes(serializeObject(e.getErrorObject()));
		}

		if (err.getErrorType() instanceof Session) {
			buffer.writeBytes(serializeObject(((Session) err.getErrorType()).getOpen()));
		}
	}

	@Override
	protected PcerrMessage validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
		if (objects == null) {
			throw new IllegalArgumentException("Passed list can't be null.");
		}

		if (objects.isEmpty()) {
			throw new PCEPDeserializerException("Error message is empty.");
		}

		final List<Rps> requestParameters = Lists.newArrayList();
		final List<Errors> errorObjects = Lists.newArrayList();
		final PcerrMessageBuilder b = new PcerrMessageBuilder();

		Object obj;
		State state = State.Init;
		obj = objects.get(0);

		if (obj instanceof ErrorObject) {
			final ErrorObject o = (ErrorObject) obj;
			errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
			state = State.ErrorIn;
			objects.remove(0);
		} else if (obj instanceof Rp) {
			final Rp o = ((Rp) obj);
			requestParameters.add(new RpsBuilder().setRp(o).build());
			state = State.RpIn;
			objects.remove(0);
		}

		while (!objects.isEmpty()) {
			obj = objects.get(0);

			if (obj instanceof UnknownObject) {
				return new PcerrBuilder().setPcerrMessage(b.setErrors(((UnknownObject) obj).getErrors()).build()).build();
			}

			switch (state) {
			case ErrorIn:
				state = State.Open;
				if (obj instanceof ErrorObject) {
					final ErrorObject o = (ErrorObject) obj;
					errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
					state = State.ErrorIn;
					break;
				}
			case RpIn:
				state = State.Error;
				if (obj instanceof Rp) {
					final Rp o = ((Rp) obj);
					requestParameters.add(new RpsBuilder().setRp(o).build());
					state = State.RpIn;
					break;
				}
			case Open:
				state = State.OpenIn;
				if (obj instanceof Open) {
					b.setErrorType(new SessionBuilder().setOpen((Open) obj).build());
					break;
				}
			case Error:
				state = State.OpenIn;
				if (obj instanceof ErrorObject) {
					final ErrorObject o = (ErrorObject) obj;
					errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
					state = State.Error;
					break;
				}
			case OpenIn:
				state = State.End;
				break;
			case End:
				break;
			default:
				break;
			}
			if (!state.equals(State.End)) {
				objects.remove(0);
			}
		}

		if (errorObjects.isEmpty() && errorObjects.isEmpty()) {
			throw new PCEPDeserializerException("At least one PCEPErrorObject is mandatory.");
		}

		if (!objects.isEmpty()) {
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
		}
		if (requestParameters != null && !requestParameters.isEmpty()) {
			b.setErrorType(new RequestBuilder().setRps(requestParameters).build());
		}

		return new PcerrBuilder().setPcerrMessage(b.setErrors(errorObjects).build()).build();
	}

	private enum State {
		Init, ErrorIn, RpIn, Open, Error, OpenIn, End
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
