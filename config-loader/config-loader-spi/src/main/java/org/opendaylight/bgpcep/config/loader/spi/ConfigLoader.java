/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

@NonNullByDefault
// FIXME: this interface should probably go: for OSGi we want to use a whiteboard, for static contexts we want to use
//        an explicit list
public interface ConfigLoader {
    /**
     * Register object model handler.
     *
     * @param config Config File Processor
     */
    // FIXME: use plain Registration here to not leak implementation
    AbstractRegistration registerConfigFile(ConfigFileProcessor config);
}
