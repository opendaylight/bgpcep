/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.impl.message.PCEPErrorMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcerr.pcerr.message.error.type.StatefulCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcerr.pcerr.message.error.type.StatefulCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcerr.pcerr.message.error.type.stateful._case.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcerr.pcerr.message.error.type.stateful._case.stateful.Srps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcerr.pcerr.message.error.type.stateful._case.stateful.SrpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.RequestCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.request.Rps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.session._case.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

/**
 * Parser for {@link PcerrMessage}
 */
public final class Stateful07ErrorMessageParser extends PCEPErrorMessageParser {

    public Stateful07ErrorMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        Preconditions.checkArgument(message instanceof PcerrMessage, "Wrong instance of Message. Passed instance of %s. Need ErrorMessage.", message.getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessage err = ((PcerrMessage) message).getPcerrMessage();

        if (err.getErrors() == null || err.getErrors().isEmpty()) {
            throw new IllegalArgumentException("Errors should not be empty.");
        }
        final ByteBuf buffer = Unpooled.buffer();

        if (err.getErrorType() instanceof RequestCase) {
            final List<Rps> rps = ((RequestCase) err.getErrorType()).getRequest().getRps();
            for (final Rps r : rps) {
                serializeObject(r.getRp(), buffer);
            }
        }
        if (err.getErrorType() instanceof StatefulCase) {
            final List<Srps> srps = ((StatefulCase) err.getErrorType()).getStateful().getSrps();
            for (final Srps s : srps) {
                serializeObject(s.getSrp(), buffer);
            }
        }
        for (final Errors e : err.getErrors()) {
            serializeObject(e.getErrorObject(), buffer);
        }
        if (err.getErrorType() instanceof SessionCase) {
            serializeObject(((SessionCase) err.getErrorType()).getSession().getOpen(), buffer);
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    @Override
    protected PcerrMessage validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        if (objects == null) {
            throw new IllegalArgumentException("Passed list can't be null.");
        }
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Error message is empty.");
        }
        final List<Rps> requestParameters = new ArrayList<>();
        final List<Srps> srps = new ArrayList<>();
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
                errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.<Rp>absent()));
                return null;
            }
            requestParameters.add(new RpsBuilder().setRp(o).build());
            state = State.RP_IN;
        } else if (obj instanceof Srp) {
            final Srp s = (Srp) obj;
            srps.add(new SrpsBuilder().setSrp(s).build());
            state = State.SRP_IN;
        }
        if (!state.equals(State.INIT)) {
            objects.remove(0);
        }
        while (!objects.isEmpty()) {
            obj = objects.get(0);
            if (obj instanceof UnknownObject) {
                return new PcerrBuilder().setPcerrMessage(b.setErrors(((UnknownObject) obj).getErrors()).build()).build();
            }
            switch (state) {
            case ERROR_IN:
                state = State.OPEN;
                if (obj instanceof ErrorObject) {
                    final ErrorObject o = (ErrorObject) obj;
                    errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
                    state = State.ERROR_IN;
                    break;
                }
            case RP_IN:
                state = State.ERROR;
                if (obj instanceof Rp) {
                    final Rp o = ((Rp) obj);
                    requestParameters.add(new RpsBuilder().setRp(o).build());
                    state = State.RP_IN;
                    break;
                }
            case SRP_IN:
                state = State.ERROR;
                if (obj instanceof Srp) {
                    final Srp o = ((Srp) obj);
                    srps.add(new SrpsBuilder().setSrp(o).build());
                    state = State.SRP_IN;
                    break;
                }
            case OPEN:
                state = State.OPEN_IN;
                if (obj instanceof Open) {
                    b.setErrorType(new SessionCaseBuilder().setSession(new SessionBuilder().setOpen((Open) obj).build()).build());
                    break;
                }
            case ERROR:
                state = State.OPEN_IN;
                if (obj instanceof ErrorObject) {
                    final ErrorObject o = (ErrorObject) obj;
                    errorObjects.add(new ErrorsBuilder().setErrorObject(o).build());
                    state = State.ERROR;
                    break;
                }
            case OPEN_IN:
                state = State.END;
                break;
            case END:
                break;
            default:
                break;
            }
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
            b.setErrorType(new RequestCaseBuilder().setRequest(new RequestBuilder().setRps(requestParameters).build()).build());
        }
        if (!srps.isEmpty()) {
            b.setErrorType(new StatefulCaseBuilder().setStateful(new StatefulBuilder().setSrps(srps).build()).build());
        }
        return new PcerrBuilder().setPcerrMessage(b.setErrors(errorObjects).build()).build();
    }

    private enum State {
        INIT, ERROR_IN, RP_IN, SRP_IN, OPEN, ERROR, OPEN_IN, END
    }
}
