/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.spi;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

/**
 * Takes care of obtain object schema, schema QName is used as base to create a pattern
 * (QName + "-" + * + ".xml") to recognize which files needs to be processed by each
 * e.g. ProtocolsConfigFileProcessor will process any file containing the naming protocols-*.xml
 */
public interface ConfigFileProcessor {
    /**
     * Return the {@link Absolute SchemaNodeIdentifier.Absolute} path to the expected root node of the document
     * contained in the file.
     *
     * @return An absolute schema node identifier.
     */
    @NonNull Absolute fileRootSchema();

    /**
     * Load the information contained on the normalized node.
     *
     * @param dto normalizedNode
     */
    void loadConfiguration(@NonNull NormalizedNode<?, ?> dto);
}
