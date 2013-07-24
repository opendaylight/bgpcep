/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.util;

import org.opendaylight.protocol.util.ByteArray;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.Immutable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Read text file, parse BGP messages. File can contain comments or other data. BGP messages are detected using 16 ff marker.
 * New lines and spaces are ignored. Use {@link ByteArray#bytesToHexString(byte[])} for serializing bytes to this format.
 */
@Immutable
public class HexDumpBGPFileParser {
	private static final int MINIMAL_LENGTH = 19;
	private static final Logger LOG = LoggerFactory.getLogger(HexDumpBGPFileParser.class);
	private static final String ff_16 = Strings.repeat("FF", 16);

	public static List<byte[]> parseMessages(File file) {
		Preconditions.checkArgument(file != null, "Filename cannot be null");
		Preconditions.checkArgument(file.exists() && file.canRead(), "File " + file + " does not exist or is not readable");
		String content;
		try {
			content = Files.toString(file, Charset.defaultCharset());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return parseMessages(content);
	}

	public static List<byte[]> parseMessages(String content) {
		content = clearWhiteSpace_toUpper(content);
		// search for 16 FFs

		List<byte[]> messages = Lists.newLinkedList();
		int idx = 0;
		while ((idx = content.indexOf(ff_16, idx)) > -1) {
			// next 2 bytes are length
			int lengthIdx = idx + 16 * 2;
			int messageIdx = lengthIdx + 4;
			String hexLength = content.substring(lengthIdx, messageIdx);
			byte[] byteLength = null;
			try {
				byteLength = Hex.decodeHex(hexLength.toCharArray());
			} catch (DecoderException e) {
				throw new RuntimeException(e);
			}
			int length = ByteArray.bytesToInt(byteLength);
			int messageEndIdx = idx + length * 2;

			// Assert that message is longer than minimum 19(header.length == 19)
			// If length in BGP message would be 0, loop would never end
			Preconditions.checkArgument(length >=  MINIMAL_LENGTH,
					"Invalid message at index " + idx
							+ ", length atribute is lower than " + MINIMAL_LENGTH);

			String hexMessage = content.substring(idx, messageEndIdx);
			byte[] message = null;
			try {
				message = Hex.decodeHex(hexMessage.toCharArray());
			} catch (DecoderException e) {
				new RuntimeException(e);
			}
			messages.add(message);
			idx = messageEndIdx;
		}
		LOG.info("Succesfully extracted " + messages.size() + " messages");
		return messages;
	}

	@VisibleForTesting
	static String clearWhiteSpace_toUpper(String line){
		return line.replaceAll("\\s", "").toUpperCase();
	}

}
