/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.auto.bandwidth.extension;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev181109.bandwidth.usage.object.BandwidthUsage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcrpt;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Singleton
@MetaInfServices
@Component(immediate = true)
@Designate(ocd = AutoBandwidthActivator.Configuration.class)
public class AutoBandwidthActivator implements PCEPExtensionProviderActivator {
    /**
     * This implementation is bound to
     * <a href="https://datatracker.ietf.org/doc/html/draft-dhody-pce-stateful-pce-auto-bandwidth-06#section-8.4">
     * draft-dhody-pce-stateful-pce-auto-bandwidth-06</a>. Since there may be varying success with experimenting with
     * compatibility, the type we bind to is configurable via {@link #bandwidthObjectClass()}.
     */
    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition(min = "3", max = "15", description = "Object Class Value as per RFC5440")
        byte bandwidthObjectClass() default DEFAULT_BANDWIDTH_OBJECT_TYPE;
    }

    /**
     * {@code BANDWITH object} as per {@code draft-dhody-pce-stateful-pce-auto-bandwidth-06 section 8.4}.
     */
    private static final byte DEFAULT_BANDWIDTH_OBJECT_TYPE = 5;

    private final int bandwidthObjectClass;

    @Inject
    public AutoBandwidthActivator() {
        this(DEFAULT_BANDWIDTH_OBJECT_TYPE);
    }

    @Activate
    public AutoBandwidthActivator(final Configuration config) {
        this(config.bandwidthObjectClass());
    }

    public AutoBandwidthActivator(final byte bandwidthObjectType) {
        checkArgument(bandwidthObjectType >= 3 && bandwidthObjectType <= 15,
            "Object type %s is not in range [[3..15]]", bandwidthObjectType);
        bandwidthObjectClass = bandwidthObjectType;
    }

    @Override
    public List<Registration> start(final PCEPExtensionProviderContext context) {
        final var bandwidthUsageObjectCodec = new BandwidthUsageObjectCodec(bandwidthObjectClass);
        final var pcRptMessageCodec = new PcRptMessageCodec(context.getObjectHandlerRegistry());

        return List.of(
            context.registerObjectParser(bandwidthUsageObjectCodec),
            context.registerObjectSerializer(BandwidthUsage.class, bandwidthUsageObjectCodec),
            context.registerMessageParser(PcRptMessageCodec.TYPE, pcRptMessageCodec),
            context.registerMessageSerializer(Pcrpt.class, pcRptMessageCodec));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("bandwithUsage", bandwidthObjectClass).toString();
    }
}
