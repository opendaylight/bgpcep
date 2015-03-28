/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.metrics.MetricPce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.metrics.MetricPceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.object.Overload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pce.id.object.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.proc.time.object.ProcTime;

final class MessageParserUtil {

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
            switch(state) {
            case START :
                state = State.PROC_TIME;
                if (obj instanceof ProcTime) {
                    metricPceBuilder.setProcTime((ProcTime) obj);
                    break;
                }
            case PROC_TIME :
                state = State.OVERLOAD;
                if (obj instanceof Overload) {
                    metricPceBuilder.setOverload((Overload) obj);
                    break;
                }
            case OVERLOAD :
                state = State.END;
                break;
            case END :
                break;
            default:
                break;
            }
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
