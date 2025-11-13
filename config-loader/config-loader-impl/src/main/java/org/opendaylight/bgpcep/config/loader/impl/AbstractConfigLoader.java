/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.bgpcep.config.loader.spi.ConfigLoader;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.util.xml.UntrustedXML;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveStatementInference;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final @GuardedBy("this") HashMap<ProcessorRegistration, ProcessorContext> configServices = new HashMap<>();

    private @GuardedBy("this") EffectiveModelContext currentContext;

    @Override
    public final synchronized AbstractRegistration registerConfigFile(final ConfigFileProcessor config) {
        final var patternStr = INITIAL + config.fileRootSchema().lastNodeIdentifier().getLocalName() + EXTENSION;
        final var context = new ProcessorContext(config, Pattern.compile(patternStr));
        context.updateSchemaNode(currentContext);

        final var reg = new ProcessorRegistration();
        configServices.put(reg, context);

        final var dir = directory();
        try (var files = Files.list(dir).filter(Files::isRegularFile)) {
            files.filter(file -> context.matchesFile(file.getFileName().toString()))
                .forEach(file -> handleConfigFile(context, file));
        } catch (IOException e) {
            LOG.warn("Failed to perform initial scan of {}, attempting to continue", dir, e);
        }

        return reg;
    }

    final synchronized void handleEvent(final String filename) {
        final var dir = directory();
        configServices.values().stream()
                .filter(context -> context.matchesFile(filename))
                .forEach(context -> handleConfigFile(context, dir.resolve(filename)));
    }

    final synchronized void updateModelContext(final EffectiveModelContext newModelContext) {
        if (!Objects.equals(currentContext, newModelContext)) {
            currentContext = newModelContext;
            configServices.values().stream().forEach(context -> context.updateSchemaNode(newModelContext));
        }
    }

    abstract @NonNull Path directory();

    private synchronized void unregister(final ProcessorRegistration reg) {
        configServices.remove(reg);
    }

    @Holding("this")
    private static void handleConfigFile(final ProcessorContext context, final Path newFile) {
        final var schema = context.schema;
        if (schema == null) {
            LOG.info("No schema present for {}, ignoring file {}", context.processor.fileRootSchema(), newFile);
            return;
        }

        final NormalizedNode dto;
        try {
            dto = parseDefaultConfigFile(schema, newFile);
        } catch (final IOException | XMLStreamException e) {
            LOG.warn("Failed to parse config file {}", newFile, e);
            return;
        }
        LOG.info("Loading initial config {}", newFile);
        context.processor.loadConfiguration(dto);
    }

    @Holding("this")
    private static NormalizedNode parseDefaultConfigFile(final EffectiveStatementInference schema, final Path newFile)
            throws IOException, XMLStreamException {
        final var resultHolder = new NormalizationResultHolder();
        final var streamWriter = ImmutableNormalizedNodeStreamWriter.from(resultHolder);

        try (var raf = new RandomAccessFile(newFile.toFile(), READ)) {
            final var channel = raf.getChannel();

            FileLock lock = null;
            final var stopwatch = Stopwatch.createStarted();
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

            try (var resourceAsStream = Files.newInputStream(newFile)) {
                final var reader = UntrustedXML.createXMLStreamReader(resourceAsStream);

                try (var xmlParser = XmlParserStream.create(streamWriter, schema)) {
                    xmlParser.parse(reader);
                } catch (XMLStreamException | IOException e) {
                    LOG.warn("Failed to parse xml", e);
                } finally {
                    reader.close();
                }
            }
        }

        return resultHolder.getResult().data();
    }
}
