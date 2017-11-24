/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.FailureType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.operation.result.Error;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.operation.result.ErrorBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 *
 */
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

    ListenableFuture<OperationResult> future() {
        return Futures.immediateFuture(this);
    }

    public static OperationResults createFailed(final List<Errors> errors) {
        final List<Errors> e = errors != null ? errors : Collections.emptyList();
        return new OperationResults(FailureType.Failed, Lists.transform(e, CONVERT_ERRORS));
    }

    public static OperationResults createUnsent(final PCEPErrors error) {
        final List<Errors> e = error != null ? Collections.singletonList(getErrorFor(error)) : Collections.emptyList();
        return new OperationResults(FailureType.Unsent, Lists.transform(e, CONVERT_ERRORS));
    }

    private static Errors getErrorFor(final PCEPErrors error) {
        final ErrorsBuilder builder = new ErrorsBuilder();
        builder.setErrorObject(new ErrorObjectBuilder().setType(error.getErrorType()).setValue(error.getErrorValue()).build());
        return builder.build();
    }

    @Override
    public FailureType getFailure() {
        return this.failure;
    }

    @Override
    public List<Error> getError() {
        return this.error;

    }
    @Override
    public Class<? extends DataContainer> getImplementedInterface() {
        return OperationResult.class;
    }
}
