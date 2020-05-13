/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.annotations.Beta;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Beta
@Component(immediate = true, service = ConfigLoader.class)
public final class OSGiConfigLoader extends AbstractConfigLoader {
    @Reference
    volatile BindingDOMCodecServices codecServices;

    @Override
    public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
        return verifyNotNull(codecServices);
    }

    @Override
    EffectiveModelContext modelContext() {
        return codecServices.getRuntimeContext().getEffectiveModelContext();
    }

    @Activate
    void activate() {
        start();
    }

    @Deactivate
    void deactivate() {
        stop();
    }
}
