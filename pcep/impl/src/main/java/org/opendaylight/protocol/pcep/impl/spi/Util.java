/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.spi;

import java.util.Collections;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.ErrorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.SessionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.session._case.SessionBuilder;

/**
 * Utilities used in pcep-impl.
 */
public final class Util {
    private Util() {
        // Hidden on purpose
    }

    public static Message createErrorMessage(final PCEPErrors error, final Open openObject) {
        final PcerrBuilder errMessageBuilder = new PcerrBuilder();
        final ErrorObject err =
            new ErrorObjectBuilder().setType(error.getErrorType()).setValue(error.getErrorValue()).build();
        if (openObject == null) {
            return errMessageBuilder.setPcerrMessage(new PcerrMessageBuilder().setErrors(Collections.singletonList(
                new ErrorsBuilder().setErrorObject(err).build())).build()).build();
        }

        final ErrorType type =
            new SessionCaseBuilder().setSession(new SessionBuilder().setOpen(openObject).build()).build();
        return errMessageBuilder.setPcerrMessage(
            new PcerrMessageBuilder()
                .setErrors(Collections.singletonList(new ErrorsBuilder().setErrorObject(err).build()))
                .setErrorType(type).build())
            .build();
    }
}
