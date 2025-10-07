/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.message.message.type.pcerr.message.pcerr.body.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.message.message.type.pcerr.message.pcerr.body.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcep.error.object.ErrorObjectBuilder;

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
        err = requireNonNull(error);
        this.invalidObject = invalidObject;
        this.error = new ErrorsBuilder()
            .setErrorObject(new ErrorObjectBuilder().setType(err.getErrorType()).setValue(err.getErrorValue()).build())
            .build();
    }

    public List<Errors> getErrors() {
        return Collections.singletonList(error);
    }

    public PCEPErrors getError() {
        return err;
    }

    public Object getInvalidObject() {
        return invalidObject;
    }

    @Override
    public Class<Object> implementedInterface() {
        return Object.class;
    }

    @Override
    public Boolean getIgnore() {
        return Boolean.FALSE;
    }

    @Override
    public Boolean getProcessingRule() {
        return Boolean.FALSE;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(UnknownObject.class).omitNullValues()
            .add("error", err)
            .add("object", invalidObject)
            .toString();
    }
}
