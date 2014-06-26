/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.FailureType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.operation.result.Errors;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 *
 */
final class OperationResults implements OperationResult {
    static final OperationResults NOACK = new OperationResults(FailureType.NoAck);
    static final OperationResults SUCCESS = new OperationResults(null);
    static final OperationResults UNSENT = new OperationResults(FailureType.Unsent);

    private final List<Errors> errors;
    private final FailureType failure;

    private OperationResults(final FailureType type) {
        this(type, Collections.<Errors>emptyList());
    }

    private OperationResults(final FailureType failure, final List<Errors> errors) {
        this.failure = failure;
        this.errors = Preconditions.checkNotNull(errors);
    }

    ListenableFuture<OperationResult> future() {
        return Futures.<OperationResult> immediateFuture(this);
    }

    public static OperationResults create(final FailureType failure, final List<Errors> errors) {
        Preconditions.checkNotNull(failure);
        return new OperationResults(failure, errors);
    }

    public static OperationResults create(final FailureType failure, final Errors... errors) {
        return create(failure, Arrays.asList(errors));
    }

    @Override
    public FailureType getFailure() {
        return failure;
    }

    @Override
    public List<Errors> getErrors() {
        return errors;

    }
    @Override
    public Class<? extends DataContainer> getImplementedInterface() {
        return OperationResult.class;
    }
}
