/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveStatementInference;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Reference implementation of configuration loading bits, without worrying where files are actually coming from.
 */
abstract class AbstractConfigLoader implements ConfigLoader {
    private final class ProcessorRegistration extends AbstractRegistration {
        @Override
        protected void removeRegistration() {
            unregister(this);
        }
    }

    private static final class ProcessorContext {
        private final ConfigFileProcessor processor;
        private final Pattern pattern;

        private EffectiveStatementInference schema;

        ProcessorContext(final ConfigFileProcessor processor, final Pattern pattern) {
            this.processor = processor;
            this.pattern = pattern;
        }

        boolean matchesFile(final String filename) {
            return pattern.matcher(filename).matches();
        }

        void updateSchemaNode(final EffectiveModelContext newContext) {
            if (newContext != null) {
                schema = SchemaInferenceStack.of(newContext, processor.fileRootSchema()).toInference();
            } else {
                schema = null;
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigLoader.class);
    private static final String EXTENSION = "-.*\\.xml";
    private static final String INITIAL = "^";
    private static final String READ = "rw";
    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);

    @GuardedBy("this")
    private final Map<ProcessorRegistration, ProcessorContext> configServices = new HashMap<>();

    @GuardedBy("this")
    private EffectiveModelContext currentContext;

    @Override
    public final synchronized AbstractRegistration registerConfigFile(final ConfigFileProcessor config) {
        final String patternStr = INITIAL + config.fileRootSchema().lastNodeIdentifier().getLocalName() + EXTENSION;
        final ProcessorContext context = new ProcessorContext(config, Pattern.compile(patternStr));
        context.updateSchemaNode(currentContext);

        final ProcessorRegistration reg = new ProcessorRegistration();
        this.configServices.put(reg, context);

        final File[] fList = directory().listFiles();
        if (fList != null) {
            final List<String> newFiles = new ArrayList<>();
            for (final File newFile : fList) {
                if (newFile.isFile()) {
                    final String filename = newFile.getName();
                    if (context.matchesFile(filename)) {
                        newFiles.add(filename);
                    }
                }
            }

            for (final String filename : newFiles) {
                handleConfigFile(context, filename);
            }
        }
        return reg;
    }

    final synchronized void handleEvent(final String filename) {
        configServices.values().stream()
                .filter(context -> context.matchesFile(filename))
                .forEach(context -> handleConfigFile(context, filename));
    }

    final synchronized void updateModelContext(final EffectiveModelContext newModelContext) {
        if (!Objects.equals(currentContext, newModelContext)) {
            currentContext = newModelContext;
            configServices.values().stream().forEach(context -> context.updateSchemaNode(newModelContext));
        }
    }

    abstract @NonNull File directory();

    private synchronized void unregister(final ProcessorRegistration reg) {
        configServices.remove(reg);
    }

    @Holding("this")
    private void handleConfigFile(final ProcessorContext context, final String filename) {
        final EffectiveStatementInference schema = context.schema;
        if (schema == null) {
            LOG.info("No schema present for {}, ignoring file {}", context.processor.fileRootSchema(), filename);
            return;
        }

        final NormalizedNode dto;
        try {
            dto = parseDefaultConfigFile(schema, filename);
        } catch (final IOException | XMLStreamException e) {
            LOG.warn("Failed to parse config file {}", filename, e);
            return;
        }
        LOG.info("Loading initial config {}", filename);
        context.processor.loadConfiguration(dto);
    }

    @Holding("this")
    private NormalizedNode parseDefaultConfigFile(final EffectiveStatementInference schema, final String filename)
            throws IOException, XMLStreamException {
        final NormalizedNodeResult result = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);

        final File newFile = new File(directory(), filename);
        try (RandomAccessFile raf = new RandomAccessFile(newFile, READ)) {
            final FileChannel channel = raf.getChannel();

            FileLock lock = null;
            final Stopwatch stopwatch = Stopwatch.createStarted();
            while (lock == null || stopwatch.elapsed(TimeUnit.NANOSECONDS) > TIMEOUT_NANOS) {
                try {
                    lock = channel.tryLock();
                } catch (final IllegalStateException e) {
                    //Ignore
                }
                if (lock == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LOG.warn("Failed to lock xml", e);
                    }
                }
            }

            try (InputStream resourceAsStream = new FileInputStream(newFile)) {
                final XMLStreamReader reader = UntrustedXML.createXMLStreamReader(resourceAsStream);

                try (XmlParserStream xmlParser = XmlParserStream.create(streamWriter, schema)) {
                    xmlParser.parse(reader);
                } catch (final URISyntaxException | XMLStreamException | IOException | SAXException e) {
                    LOG.warn("Failed to parse xml", e);
                } finally {
                    reader.close();
                }
            }
        }

        return result.getResult();
    }
}
