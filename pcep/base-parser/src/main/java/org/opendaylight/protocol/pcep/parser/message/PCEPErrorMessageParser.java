/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.message;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.RequestCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.SessionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.SessionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.request._case.request.Rps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.session._case.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.rp.object.Rp;

/**
 * Parser for {@link PcerrMessage}
 */
public class PCEPErrorMessageParser extends AbstractMessageParser {

    public static final int TYPE = 6;

    public PCEPErrorMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        checkArgument(message instanceof PcerrMessage,
            "Wrong instance of Message. Passed instance of %s. Need PcerrMessage.", message.getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message
            .PcerrMessage err = ((PcerrMessage) message).getPcerrMessage();
        checkArgument(err.getErrors() != null && !err.getErrors().isEmpty(), "Errors should not be empty.");
        final ByteBuf buffer = Unpooled.buffer();
        serializeCases(err, buffer);
        for (final Errors e : err.getErrors()) {
            serializeObject(e.getErrorObject(), buffer);
        }

        if (err.getErrorType() instanceof SessionCase) {
            serializeObject(((SessionCase) err.getErrorType()).getSession().getOpen(), buffer);
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    /**
     * If needed, subclasses can override this method.
     */
    protected void serializeCases(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types
            .rev181109.pcerr.message.PcerrMessage err, final ByteBuf buffer) {
        if (err.getErrorType() instanceof RequestCase) {
            final List<Rps> rps = ((RequestCase) err.getErrorType()).getRequest().getRps();
            for (final Rps r : rps) {
                serializeObject(r.getRp(), buffer);
            }
        }
    }

    @Override
    protected PcerrMessage validate(final List<Object> objects, final List<Message> errors)
            throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Error message is empty.");
        }
        final List<Rps> requestParameters = new ArrayList<>();
        final List<Errors> errorObjects = new ArrayList<>();
        final PcerrMessageBuilder b = new PcerrMessageBuilder();

        Object obj = objects.get(0);
        State state = State.INIT;

        if (obj instanceof ErrorObject) {
            final ErrorObject o = (ErrorObject) obj;
            errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
            state = State.ERROR_IN;
        } else if (obj instanceof Rp) {
            final Rp o = (Rp) obj;
            if (o.isProcessingRule()) {
                errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.empty()));
                return null;
            }
            requestParameters.add(new RpsBuilder().setRp(o).build());
            state = State.RP_IN;
        }
        if (state.equals(State.INIT)) {
            throw new PCEPDeserializerException("At least one PCEPErrorObject is mandatory.");
        }
        objects.remove(0);
        while (!objects.isEmpty() && !state.equals(State.END)) {
            obj = objects.get(0);
            if (obj instanceof UnknownObject) {
                return new PcerrBuilder().setPcerrMessage(b.setErrors(((UnknownObject) obj).getErrors()).build())
                        .build();
            }
            state = insertObject(state, errorObjects, obj, requestParameters, b);
            if (!state.equals(State.END)) {
                objects.remove(0);
            }
        }
        if (errorObjects.isEmpty()) {
            throw new PCEPDeserializerException("At least one PCEPErrorObject is mandatory.");
        }
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }
        if (!requestParameters.isEmpty()) {
            b.setErrorType(new RequestCaseBuilder().setRequest(new RequestBuilder().setRps(requestParameters).build())
                .build());
        }
        return new PcerrBuilder().setPcerrMessage(b.setErrors(errorObjects).build()).build();
    }

    private static State insertObject(final State state, final List<Errors> errorObjects, final Object obj,
            final List<Rps> requestParameters, final PcerrMessageBuilder b) {
        switch (state) {
            case RP_IN:
                if (obj instanceof Rp) {
                    final Rp o = (Rp) obj;
                    requestParameters.add(new RpsBuilder().setRp(o).build());
                    return State.RP_IN;
                }
            case ERROR_IN:
                if (obj instanceof ErrorObject) {
                    final ErrorObject o = (ErrorObject) obj;
                    errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
                    return State.ERROR_IN;
                }
            case OPEN:
                if (obj instanceof Open) {
                    b.setErrorType(new SessionCaseBuilder().setSession(new SessionBuilder().setOpen((Open) obj).build())
                        .build());
                    return State.OPEN_IN;
                }
            case ERROR:
                if (obj instanceof ErrorObject) {
                    final ErrorObject o = (ErrorObject) obj;
                    errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
                    return State.ERROR;
                }
            case OPEN_IN:
            case END:
                return State.END;
            default:
                return state;
        }
    }

    private enum State {
        INIT, ERROR_IN, RP_IN, OPEN, ERROR, OPEN_IN, END
    }
}
