/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Placeholder object. This object should be injected by in positions where an object is either completely unknown or
 * has failed semantic validation.
 */
public final class UnknownObject implements Object {
    private final Object invalidObject;
    private final Errors error;
    private final PCEPErrors err;

    public UnknownObject(final PCEPErrors error) {
        this(error, null);
    }

    public UnknownObject(final PCEPErrors error, final Object invalidObject) {
        this.err = requireNonNull(error);

        this.error = new ErrorsBuilder().setErrorObject(
                new ErrorObjectBuilder().setType(error.getErrorType()).setValue(
                        error.getErrorValue()).build()).build();
        this.invalidObject = invalidObject;
    }

    public List<Errors> getErrors() {
        return Collections.singletonList(this.error);
    }

    public PCEPErrors getError() {
        return this.err;
    }

    public Object getInvalidObject() {
        return this.invalidObject;
    }

    @Override
    public Class<? extends DataContainer> getImplementedInterface() {
        return Object.class;
    }

    @Override
    public Boolean isIgnore() {
        return Boolean.FALSE;
    }

    @Override
    public Boolean isProcessingRule() {
        return Boolean.FALSE;
    }
}
