/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.spi;

import java.util.Collection;
import java.util.Collections;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

public final class SuccessfulRpcResult<T> implements RpcResult<T> {
    private final T value;

    private SuccessfulRpcResult(final T value) {
        this.value = value;
    }

    public static <T> SuccessfulRpcResult<T> create(final T value) {
        return new SuccessfulRpcResult<>(value);
    }

    @Override
    public boolean isSuccessful() {
        return true;
    }

    @Override
    public T getResult() {
        return this.value;
    }

    @Override
    public Collection<RpcError> getErrors() {
        return Collections.emptyList();
    }
}