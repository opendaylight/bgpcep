/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;

public final class BmpMockSessionListenerFactory implements BmpSessionListenerFactory {
    @Nonnull
    @Override
    public BmpSessionListener getSessionListener() {
        return new BmpMockSessionListener();
    }
}
