/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.spi;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Basic substrate for wiring ConfigFileProcessors through OSGi Declarative Services. These have to use
 * {@link AbstractConfigFileProcessor} due to tie-in with its lifecycle.
 */
public abstract class AbstractOSGiConfigFileProcessor extends ForwardingConfigFileProcessor {
    private AbstractConfigFileProcessor delegate;

    /**
     * Start this processor using specified delegate.
     *
     * @param delegate designated delegate
     * @throws NullPointerException if the delegate is null
     */
    protected final void start(final @NonNull AbstractConfigFileProcessor delegate) {
        this.delegate = requireNonNull(delegate);
        delegate.start();
    }

    /**
     * Stop the delegate and do not allow further requests to be made. If the delegate was already closed, this method
     * does nothing.
     */
    protected final void stop() {
        if (delegate != null) {
            delegate.close();
            delegate = null;
        }
    }

    @Override
    protected final AbstractConfigFileProcessor delegate() {
        return verifyNotNull(delegate);
    }
}
