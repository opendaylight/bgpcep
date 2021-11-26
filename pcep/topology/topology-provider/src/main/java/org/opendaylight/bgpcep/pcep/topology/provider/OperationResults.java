/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.FailureType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.operation.result.Error;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.operation.result.ErrorBuilder;

final class OperationResults implements OperationResult {
    static final OperationResults NOACK = new OperationResults(FailureType.NoAck);
    static final OperationResults SUCCESS = new OperationResults((FailureType)null);
    static final OperationResults UNSENT = new OperationResults(FailureType.Unsent);

    private static final Function<Errors, Error> CONVERT_ERRORS = input -> new ErrorBuilder(input).build();

    private final FailureType failure;
    private final List<Error> error;

    private OperationResults(final FailureType failure) {
        this.failure = failure;
        this.error = null;
    }

    private OperationResults(final FailureType failure, final List<Error> error) {
        this.failure = failure;
        this.error = error;
    }

    @NonNull ListenableFuture<OperationResult> future() {
        return Futures.immediateFuture(this);
    }

    public static @NonNull OperationResults createFailed(final List<Errors> errors) {
        final List<Errors> e = errors != null ? errors : Collections.emptyList();
        return new OperationResults(FailureType.Failed, e.stream().map(CONVERT_ERRORS).collect(Collectors.toList()));
    }

    public static @NonNull OperationResults createUnsent(final PCEPErrors error) {
        final List<Errors> e = error != null ? Collections.singletonList(getErrorFor(error))
                : Collections.emptyList();
        return new OperationResults(FailureType.Unsent, e.stream().map(CONVERT_ERRORS)
                .collect(Collectors.toList()));
    }

    private static @NonNull Errors getErrorFor(final PCEPErrors error) {
        return new ErrorsBuilder()
                .setErrorObject(new ErrorObjectBuilder().setType(error.getErrorType())
                .setValue(error.getErrorValue()).build())
                .build();
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
    public Class<OperationResult> implementedInterface() {
        return OperationResult.class;
    }
}
