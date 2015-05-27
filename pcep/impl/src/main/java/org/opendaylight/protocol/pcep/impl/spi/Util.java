/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.spi;

import java.util.Arrays;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.metrics.MetricPce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.metrics.MetricPceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.object.Overload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pce.id.object.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.session._case.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.proc.time.object.ProcTime;

/**
 * Utilities used in pcep-impl
 */
public final class Util {

    private Util() {
        throw new UnsupportedOperationException();
    }

    public static Message createErrorMessage(final PCEPErrors e, final Open t) {
        final PcerrBuilder errMessageBuilder = new PcerrBuilder();
        final ErrorObject err = new ErrorObjectBuilder().setType(e.getErrorType()).setValue(e.getErrorValue()).build();
        if (t == null) {
            return errMessageBuilder.setPcerrMessage(
                    new PcerrMessageBuilder().setErrors(Arrays.asList(new ErrorsBuilder().setErrorObject(err).build())).build()).build();
        } else {
            final ErrorType type = new SessionCaseBuilder().setSession(new SessionBuilder().setOpen(t).build()).build();
            return errMessageBuilder.setPcerrMessage(
                    new PcerrMessageBuilder().setErrors(Arrays.asList(new ErrorsBuilder().setErrorObject(err).build())).setErrorType(type).build()).build();
        }
    }

    private static State insertObject(final MetricPceBuilder metricPceBuilder, final State state, final Object obj) {
        switch(state) {
        case START :
            if (obj instanceof ProcTime) {
                metricPceBuilder.setProcTime((ProcTime) obj);
                return State.PROC_TIME;
            }
        case PROC_TIME :
            if (obj instanceof Overload) {
                metricPceBuilder.setOverload((Overload) obj);
                return State.OVERLOAD;
            }
        case OVERLOAD :
        case END :
            return State.END;
        default:
            return state;
        }
    }

    public static MetricPce validateMonitoringMetrics(final List<Object> objects) throws PCEPDeserializerException {
        final MetricPceBuilder metricPceBuilder = new MetricPceBuilder();
        if (!(objects.get(0) instanceof PceId)) {
            throw new PCEPDeserializerException("metric-pce-list must start with PCE-ID object.");
        }
        metricPceBuilder.setPceId((PceId) (objects.get(0)));
        objects.remove(0);
        State state = State.START;
        while (!objects.isEmpty() && !state.equals(State.END)) {
            final Object obj = objects.get(0);
            state = insertObject(metricPceBuilder, state, obj);
            if (!state.equals(State.END)) {
                objects.remove(0);
            }
        }
        return metricPceBuilder.build();
    }

    private enum State {
        START, PROC_TIME, OVERLOAD, END;
    }
}
