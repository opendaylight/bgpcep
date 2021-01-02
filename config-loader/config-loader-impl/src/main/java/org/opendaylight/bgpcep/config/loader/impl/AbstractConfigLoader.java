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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
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

        boolean matchesFile(final Path path) {
            return pattern.matcher(path.getFileName().toString()).matches();
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
        configServices.put(reg, context);

        final Stream<Path> files;
        try {
            files = Files.list(directory());
        } catch (IOException e) {
            LOG.info("Failed to access directory", e);
            return reg;
        }

        files.filter(Files::isRegularFile)
            .filter(context::matchesFile)
            .forEach(path -> handleConfigFile(context, path));
        return reg;
    }

    final synchronized void handleEvent(final Path path) {
        configServices.values().stream()
                .filter(context -> context.matchesFile(path))
                .forEach(context -> handleConfigFile(context, path));
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
    private void handleConfigFile(final ProcessorContext context, final Path path) {
        final EffectiveStatementInference schema = context.schema;
        if (schema == null) {
            LOG.info("No schema present for {}, ignoring file {}", context.processor.fileRootSchema(), path);
            return;
        }

        final NormalizedNode dto;
        try {
            dto = parseDefaultConfigFile(schema, path);
        } catch (final IOException | XMLStreamException e) {
            LOG.warn("Failed to parse config file {}", path, e);
            return;
        }
        LOG.info("Loading initial config {}", path);
        context.processor.loadConfiguration(dto);
    }

    @Holding("this")
    private static NormalizedNode parseDefaultConfigFile(final EffectiveStatementInference schema, final Path path)
            throws IOException, XMLStreamException {
        final NormalizedNodeResult result = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);

        try (InputStream resourceAsStream = openFile(path)) {
            final XMLStreamReader reader = UntrustedXML.createXMLStreamReader(resourceAsStream);

            try (XmlParserStream xmlParser = XmlParserStream.create(streamWriter, schema)) {
                xmlParser.parse(reader);
            } catch (final URISyntaxException | XMLStreamException | IOException | SAXException e) {
                LOG.warn("Failed to parse xml", e);
            } finally {
                reader.close();
            }
        }

        return result.getResult();
    }

    private static InputStream openFile(final Path filePath) throws FileNotFoundException, IOException {
        final File newFile;
        try {
            newFile = filePath.toFile();
        } catch (UnsupportedOperationException e) {
            LOG.info("File name is not backed by a file", e);
            return Files.newInputStream(filePath);
        }

        final FileChannel channel = new RandomAccessFile(newFile, READ).getChannel();
        final Stopwatch stopwatch = Stopwatch.createStarted();
        do {
            FileLock lock = null;
            try {
                lock = channel.tryLock();
            } catch (final IllegalStateException e) {
                //Ignore
            }
            if (lock != null) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOG.warn("Failed to lock xml", e);
            }
        } while (stopwatch.elapsed(TimeUnit.NANOSECONDS) > TIMEOUT_NANOS);

        return Channels.newInputStream(channel);
    }
}
