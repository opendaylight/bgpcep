/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.FailureType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.operation.result.Error;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.operation.result.ErrorBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 *
 */
final class OperationResults implements OperationResult {
    static final OperationResults NOACK = new OperationResults(FailureType.NoAck);
    static final OperationResults SUCCESS = new OperationResults((FailureType)null);
    static final OperationResults UNSENT = new OperationResults(FailureType.Unsent);

    private static final Function<Errors, Error> CONVERT_ERRORS = new Function<Errors, Error>() {
        @Override
        public Error apply(final Errors input) {
            return new ErrorBuilder(input).build();
        }
    };

    private final FailureType failure;
    private final List<Error> error;
    private final String message;

    private OperationResults(final FailureType failure) {
        this.failure = failure;
        this.error = null;
        this.message = "";
    }

    private OperationResults(final FailureType failure, final List<Error> error, final String message) {
        this.failure = failure;
        this.error = error;
        this.message = Strings.nullToEmpty(message);
    }

    ListenableFuture<OperationResult> future() {
        return Futures.<OperationResult> immediateFuture(this);
    }

    public static OperationResults createFailed(final List<Errors> errors) {
        final List<Errors> e = errors != null ? errors : Collections.<Errors>emptyList();
        return new OperationResults(FailureType.Failed, Lists.transform(e, CONVERT_ERRORS), null);
    }

    public static OperationResults createUnsent(final PCEPErrors error) {
        final List<Errors> e = error != null ? Collections.singletonList(getErrorFor(error)) : Collections.<Errors>emptyList();
        return new OperationResults(FailureType.Unsent, Lists.transform(e, CONVERT_ERRORS), null);
    }

    public static OperationResults createUnsent(final String message) {
        return new OperationResults(FailureType.Unsent, Collections.<Error>emptyList(), message);
    }

    private static Errors getErrorFor(final PCEPErrors error) {
        final ErrorsBuilder builder = new ErrorsBuilder();
        builder.setErrorObject(new ErrorObjectBuilder().setType(error.getErrorType()).setValue(error.getErrorValue()).build());
        return builder.build();
    }

    @Override
    public FailureType getFailure() {
        return failure;
    }

    @Override
    public List<Error> getError() {
        return error;

    }
    @Override
    public Class<? extends DataContainer> getImplementedInterface() {
        return OperationResult.class;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
