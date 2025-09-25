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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.RequestCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.SessionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.SessionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.StatefulCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.StatefulCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.request._case.request.Rps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.session._case.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.stateful._case.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.stateful._case.stateful.Srps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.stateful._case.stateful.SrpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srp.object.Srp;

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
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message
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
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message
                .PcerrMessage err, final ByteBuf buffer) {
        if (err.getErrorType() instanceof RequestCase) {
            for (final Rps r : ((RequestCase) err.getErrorType()).getRequest().nonnullRps()) {
                serializeObject(r.getRp(), buffer);
            }
        }
        if (err.getErrorType() instanceof StatefulCase stateful) {
            for (final Srps s : stateful.getStateful().nonnullSrps()) {
                serializeObject(s.getSrp(), buffer);
            }
        }
    }

    @Override
    protected PcerrMessage validate(final Queue<Object> objects, final List<Message> errors)
            throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");

        final Object first = objects.poll();
        if (first == null) {
            throw new PCEPDeserializerException("Error message is empty.");
        }

        final var requestParameters = new ArrayList<Rps>();
        final var srps = new ArrayList<Srps>();
        final var errorObjects = new ArrayList<Errors>();

        final State initialState;
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
        } else if (first instanceof Srp) {
            srps.add(new SrpsBuilder().setSrp((Srp) first).build());
            initialState = State.SRP_IN;
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

            state = insertObject(state, obj, errorObjects, requestParameters, srps, msgBuilder);
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
        if (!srps.isEmpty()) {
            msgBuilder.setErrorType(new StatefulCaseBuilder()
                    .setStateful(new StatefulBuilder().setSrps(srps).build()).build());
        }

        return new PcerrBuilder().setPcerrMessage(msgBuilder.setErrors(errorObjects).build()).build();
    }

    private static State insertObject(final State state, final Object obj, final List<Errors> errorObjects,
            final List<Rps> requestParameters, final List<Srps> srps, final PcerrMessageBuilder builder) {
        switch (state) {
            case ERROR_IN:
                if (obj instanceof ErrorObject o) {
                    errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
                    return State.ERROR_IN;
                }
                // fall through
            case RP_IN:
                if (obj instanceof Rp o) {
                    requestParameters.add(new RpsBuilder().setRp(o).build());
                    return State.RP_IN;
                }
                // fall through
            case SRP_IN:
                if (obj instanceof Srp o) {
                    srps.add(new SrpsBuilder().setSrp(o).build());
                    return State.SRP_IN;
                }
                // fall through
            case OPEN:
                if (obj instanceof Open) {
                    builder.setErrorType(
                        new SessionCaseBuilder().setSession(new SessionBuilder().setOpen((Open) obj).build()).build());
                    return State.OPEN_IN;
                }
                // fall through
            case ERROR:
                if (obj instanceof ErrorObject o) {
                    errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
                    return State.ERROR;
                }
                // fall through
            case OPEN_IN:
            case END:
                return State.END;
            default:
                return state;
        }
    }

    private enum State {
        INIT, ERROR_IN, RP_IN, SRP_IN, OPEN, ERROR, OPEN_IN, END
    }
}
