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
import java.util.Queue;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.RequestCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.SessionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.SessionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.request._case.request.Rps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.session._case.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.rp.object.Rp;

/**
 * Parser for {@link PcerrMessage}.
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
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message
            .PcerrMessage err = ((PcerrMessage) message).getPcerrMessage();
        checkArgument(err.getErrors() != null && !err.nonnullErrors().isEmpty(),
                "Errors should not be empty.");
        final ByteBuf buffer = Unpooled.buffer();
        serializeCases(err, buffer);
        for (final Errors e : err.nonnullErrors()) {
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
    protected void serializeCases(
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message
                .PcerrMessage err, final ByteBuf buffer) {
        if (err.getErrorType() instanceof RequestCase) {
            for (final Rps r : ((RequestCase) err.getErrorType()).getRequest().nonnullRps()) {
                serializeObject(r.getRp(), buffer);
            }
        }
    }

    @Override
    protected PcerrMessage validate(final Queue<Object> objects, final List<Message> errors)
            throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");

        final List<Rps> requestParameters = new ArrayList<>();
        final List<Errors> errorObjects = new ArrayList<>();

        final State initialState;
        final Object first = objects.poll();
        if (first instanceof ErrorObject) {
            errorObjects.add(new ErrorsBuilder().setErrorObject((ErrorObject) first).build());
            initialState = State.ERROR_IN;
        } else if (first instanceof Rp rp) {
            if (rp.getProcessingRule()) {
                errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.empty()));
                return null;
            }
            requestParameters.add(new RpsBuilder().setRp(rp).build());
            initialState = State.RP_IN;
        } else if (first == null) {
            throw new PCEPDeserializerException("Error message is empty.");
        } else {
            throw new PCEPDeserializerException("At least one PCEPErrorObject is mandatory.");
        }

        final PcerrMessageBuilder msgBuilder = new PcerrMessageBuilder();
        State state = initialState;
        for (Object obj = objects.peek(); obj != null; obj = objects.peek()) {
            if (obj instanceof UnknownObject) {
                return new PcerrBuilder()
                        .setPcerrMessage(msgBuilder.setErrors(((UnknownObject) obj).getErrors()).build())
                        .build();
            }

            state = insertObject(state, errorObjects, obj, requestParameters, msgBuilder);
            if (state == State.END) {
                break;
            }

            objects.remove();
        }

        if (errorObjects.isEmpty()) {
            throw new PCEPDeserializerException("At least one PCEPErrorObject is mandatory.");
        }
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }
        if (!requestParameters.isEmpty()) {
            msgBuilder.setErrorType(new RequestCaseBuilder()
                    .setRequest(new RequestBuilder().setRps(requestParameters).build()).build());
        }
        return new PcerrBuilder().setPcerrMessage(msgBuilder.setErrors(errorObjects).build()).build();
    }

    @SuppressWarnings("fallthrough")
    private static State insertObject(final State state, final List<Errors> errorObjects, final Object obj,
            final List<Rps> requestParameters, final PcerrMessageBuilder msgBuilder) {
        switch (state) {
            case RP_IN:
                if (obj instanceof Rp o) {
                    requestParameters.add(new RpsBuilder().setRp(o).build());
                    return State.RP_IN;
                }
                // fallthrough
            case ERROR_IN: // fall-through
                if (obj instanceof ErrorObject o) {
                    errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
                    return State.ERROR_IN;
                }
                // fallthrough
            case OPEN:
                if (obj instanceof Open) {
                    msgBuilder.setErrorType(
                        new SessionCaseBuilder().setSession(new SessionBuilder().setOpen((Open) obj).build()).build());
                    return State.OPEN_IN;
                }
                // fallthrough
            case ERROR:
                if (obj instanceof ErrorObject o) {
                    errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
                    return State.ERROR;
                }
                // fallthrough
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
