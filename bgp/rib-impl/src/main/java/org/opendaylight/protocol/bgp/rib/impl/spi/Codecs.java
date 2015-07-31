/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;

/**
 * Common interface for Codecs classes.
 *
 */
public interface Codecs {
    /**
     * Called when Codec tree is updated. Implementations should
     * refresh codec context.
     *
     * @param tree BindingCodecTree
     */
    void onCodecTreeUpdated(final BindingCodecTree tree);
}
