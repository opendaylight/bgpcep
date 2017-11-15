/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.config.loader.spi;

import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

public interface ConfigLoader {
    /**
     * Register object model handler.
     *
     * @param config Config File Processor
     */
    @Nonnull
    AbstractRegistration registerConfigFile(@Nonnull ConfigFileProcessor config);

    /**
     * Provides BindingNormalizedNodeSerializer.
     *
     * @return Binding Normalized node serializer
     */
    @Nonnull
    BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer();
}
