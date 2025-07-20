/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.util;

import java.util.Queue;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.metrics.MetricPce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.metrics.MetricPceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.overload.object.Overload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pce.id.object.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.proc.time.object.ProcTime;

/**
 * Utilities used in pcep-base-parser.
 */
public final class Util {
    private Util() {
        // Hidden on purpose
    }

    private static State insertObject(final MetricPceBuilder metricPceBuilder, final State state, final Object obj) {
        switch (state) {
            case START :
                if (obj instanceof ProcTime) {
                    metricPceBuilder.setProcTime((ProcTime) obj);
                    return State.PROC_TIME;
                }
                // fallthrough
            case PROC_TIME :
                if (obj instanceof Overload) {
                    metricPceBuilder.setOverload((Overload) obj);
                    return State.OVERLOAD;
                }
                // fallthrough
            case OVERLOAD :
            case END :
                return State.END;
            default:
                return state;
        }
    }

    public static MetricPce validateMonitoringMetrics(final Queue<Object> objects) throws PCEPDeserializerException {
        if (!(objects.poll() instanceof PceId pceId)) {
            throw new PCEPDeserializerException("metric-pce-list must start with PCE-ID object.");
        }

        final MetricPceBuilder metricPceBuilder = new MetricPceBuilder().setPceId(pceId);
        State state = State.START;
        for (Object obj = objects.peek(); obj != null; obj = objects.peek()) {
            state = insertObject(metricPceBuilder, state, obj);
            if (state == State.END) {
                break;
            }

            objects.remove();
        }

        return metricPceBuilder.build();
    }

    private enum State {
        START, PROC_TIME, OVERLOAD, END
    }
}
