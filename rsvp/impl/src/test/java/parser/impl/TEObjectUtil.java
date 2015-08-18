/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package parser.impl;

public class TEObjectUtil {

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(196)|   C-Type (1)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |R|                        Reserved                       |T|A|D|
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_ADMIN_STATUS = {
        0x00, (byte) 0x04, (byte) 0xc4, 0x01,  // Lenght, Class, Ctype
        (byte) 0x80, // Reflect
        0x00, 0x00, // Reserved
        0x07,};// Testing,  Administratively down, Deletion in progress (D) 1 bit-each

    /**
     * * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(207)|   C-Type (1)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         Exclude-any                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         Include-any                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         Include-all                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |   Setup Prio  | Holding Prio  |     Flags     |  Name Length  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //          Session Name      (NULL padded display string)      //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_SESSION_C1 = {
        0x00, (byte) 0x14, (byte) 0xcf, 0x01,  // Lenght, Class, Ctype
        0x01, 0x01, 0x01, 0x01,
        0x02, 0x02, 0x02, 0x02,
        0x03, 0x03, 0x03, 0x03,
        0x01, 0x02, 0x04, 0x04,
        (byte) 0x41, (byte) 0x41, (byte) 0x00,(byte) 0x00, };

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(207)|   C-Type (7)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |   Setup Prio  | Holding Prio  |     Flags     |  Name Length  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //          Session Name      (NULL padded display string)      //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_SESSION_C7 = {
        0x00, (byte) 0x08, (byte) 0xcf, 0x07,  // Lenght, Class, Ctype
        0x01, 0x02, 0x04, 0x04,
        (byte) 0x41, (byte) 0x41, (byte) 0x41, (byte) 0x41,};
    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(5)|   C-Type (1)    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                        Bandwidth                              |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_BANDWIDTH_1 = {
        0x00, (byte) 0x04, (byte) 0x05, 0x01,  // Lenght, Class, Ctype
        0x01, 0x02, 0x03, 0x04,};
    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(5)|   C-Type (2)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                        Bandwidth                              |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_BANDWIDTH_2 = {
        0x00, (byte) 0x04, (byte) 0x05, 0x02,  // Lenght, Class, Ctype
        0x01, 0x02, 0x03, 0x04,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(199)|   C-Type (1)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |       Association Type        |       Association ID          |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                  IPv4 Association Source                      |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_ASSOCIATION_1 = {
        0x00, (byte) 0x08, (byte) 0xc7, 0x01,  // Lenght, Class, Ctype
        0x00, 0x01, 0x00, 0x02,
        0x01, 0x02, 0x03, 0x04,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(199)|   C-Type (2)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(199)|  C-Type (2)   |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |       Association Type        |       Association ID          |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * |                  IPv6 Association Source                      |
     * |                                                               |
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_ASSOCIATION_2 = {
        0x00, (byte) 0x14, (byte) 0xc7, 0x02,  // Lenght, Class, Ctype
        0x00, 0x01, 0x00, 0x02,
        0x01, 0x02, 0x03, 0x04,
        0x05, 0x06, 0x07, 0x08,
        0x01, 0x02, 0x03, 0x04,
        0x05, 0x06, 0x07, 0x08,};

    /**
     * +-------------+-------------+-------------+-------------+
     * |       Length (bytes)      |  Class-Num  |   C-Type    |
     * +-------------+-------------+-------------+-------------+
     * | Setup Prio  | Hold Prio   | Hop-limit   |    Flags    |
     * +-------------+-------------+-------------+-------------+
     * |                  Bandwidth                            |
     * +-------------+-------------+-------------+-------------+
     * |                  Include-any                          |
     * +-------------+-------------+-------------+-------------+
     * |                  Exclude-any                          |
     * +-------------+-------------+-------------+-------------+
     * |                  Include-all                          |
     * +-------------+-------------+-------------+-------------+
     */
    public static final byte[] TE_LSP_FAST_REROUTE1 = {
        0x00, (byte) 0x14, (byte) 0xcd, 0x01,  // Lenght, Class, Ctype
        (byte) 0x01, (byte) 0x02, (byte) 0x10, (byte) 0x02,
        0x01, 0x01, 0x01, 0x01,
        0x02, 0x02, 0x02, 0x02,
        0x03, 0x03, 0x03, 0x03,
        0x04, 0x04, 0x04, 0x04,};

    /**
     * +-------------+-------------+-------------+-------------+
     * |       Length (bytes)      |  Class-Num  |   C-Type    |
     * +-------------+-------------+-------------+-------------+
     * | Setup Prio  | Hold Prio   | Hop-limit   | Reserved    |
     * +-------------+-------------+-------------+-------------+
     * |                  Bandwidth                            |
     * +-------------+-------------+-------------+-------------+
     * |                  Include-any                          |
     * +-------------+-------------+-------------+-------------+
     * |                  Exclude-any                          |
     * +-------------+-------------+-------------+-------------+
     */
    public static final byte[] TE_LSP_FAST_REROUTE7 = {
        0x00, (byte) 0x10, (byte) 0xcd, 0x07,  // Lenght, Class, Ctype
        (byte) 0x01, (byte) 0x02, (byte) 0x10, 0x00,
        0x01, 0x01, 0x01, 0x01,
        0x02, 0x02, 0x02, 0x02,
        0x03, 0x03, 0x03, 0x03,};


    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(6)|   C-Type (1)    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |          Reserved             |    Flags  |C|B|       T       |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                          metric-value                         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_METRIC = {
        0x00, (byte) 0x08, (byte) 0x06, 0x01,  // Lenght, Class, Ctype
        0x00, 0x00, 0x03, 0x02,
        0x01, 0x02, 0x03, 0x04,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(37)|   C-Type (1)    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |S|                  Reserved                       | Link Flags|
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_PROTECTION_C1 = {
        0x00, (byte) 0x04, (byte) 0x25, 0x01,  // Lenght, Class, Ctype
        (byte) 0x80, 0x00, 0x00, (byte) 0x0a,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(37)|   C-Type (1)    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |S|P|N|O| Reserved  | LSP Flags |     Reserved      | Link Flags|
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |I|R|   Reserved    | Seg.Flags |           Reserved            |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_PROTECTION_C2 = {
        0x00, (byte) 0x08, (byte) 0x25, 0x02,  // Lenght, Class, Ctype
        (byte) 0xf0, 0x08, 0x00, (byte) 0x0a,
        (byte) 0xc0, 0x04, 0x00, (byte) 0x00,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(197)|   C-Type (1)    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //                       Attributes TLVs                       //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * **Attributes TLVs
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |             Type              |           Length              |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //                            Value                            //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_ATTRIBUTES = {
        0x00, (byte) 0x0c, (byte) 0xc5, 0x01,  // Lenght, Class, Ctype
        0x00, 0x01, 0x00, 0x08,
        (byte) 0x09, 0x07, 0x03, 0x01,
        (byte) 0x15, 0x07, 0x03, 0x01,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(43)|   C-Type (1)    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //                       Attributes TLVs                       //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * **Attributes TLVs
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |             Type              |           Length              |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //                            Value                            //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_REQUIRED_ATTRIBUTES = {
        0x00, (byte) 0x0c, 0x43, 0x01,  // Lenght, Class, Ctype
        0x00, 0x01, 0x00, 0x08,
        (byte) 0x09, 0x07, 0x03, 0x01,
        (byte) 0x15, 0x07, 0x03, 0x01,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(63)|   C-Type (7)    |
     * +-------------+-------------+-------------+-------------+
     * |       Length (bytes)      |  Class-Num  |   C-Type    |
     * +-------------+-------------+-------------+-------------+
     * |                      PLR_ID  1                        |
     * +-------------+-------------+-------------+-------------+
     * |                    Avoid_Node_ID 1                    |
     * +-------------+-------------+-------------+-------------+
     * //                        ....                          //
     * +-------------+-------------+-------------+-------------+
     * |                      PLR_ID  n                        |
     * +-------------+-------------+-------------+-------------+
     * |                    Avoid_Node_ID  n                   |
     * +-------------+-------------+-------------+-------------+
     */
    public static final byte[] TE_LSP_DETOUR7 = {
        0x00, (byte) 0x20, (byte) 0x3f, 0x07,  // Lenght, Class, Ctype
        0x00, 0x00, 0x00, 0x01,
        0x01, 0x02, 0x03, 0x01,
        0x00, 0x00, 0x00, 0x02,
        0x01, 0x02, 0x03, 0x02,
        0x00, 0x00, 0x00, 0x03,
        0x01, 0x02, 0x03, 0x03,
        0x00, 0x00, 0x00, 0x04,
        0x01, 0x02, 0x03, 0x04,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |     Length           | Class-Num(63)|   C-Type (7)    |
     * +-------------+-------------+-------------+-------------+
     * |       Length (bytes)      |  Class-Num  |   C-Type    |
     * +-------------+-------------+-------------+-------------+
     * |                      PLR_ID  1                        |
     * +-------------+-------------+-------------+-------------+
     * |                      PLR_ID  1 (continued)            |
     * +-------------+-------------+-------------+-------------+
     * |                      PLR_ID  1 (continued)            |
     * +-------------+-------------+-------------+-------------+
     * |                      PLR_ID  1 (continued)            |
     * +-------------+-------------+-------------+-------------+
     * |                    Avoid_Node_ID 1                    |
     * +-------------+-------------+-------------+-------------+
     * |                    Avoid_Node_ID 1 (continued)        |
     * +-------------+-------------+-------------+-------------+
     * |                    Avoid_Node_ID 1 (continued)        |
     * +-------------+-------------+-------------+-------------+
     * |                    Avoid_Node_ID 1 (continued)        |
     * +-------------+-------------+-------------+-------------+
     * //                        ....                          //
     * +-------------+-------------+-------------+-------------+
     */
    public static final byte[] TE_LSP_DETOUR8 = {
        0x00, (byte) 0x40, (byte) 0x3f, 0x08,  // Lenght, Class, Ctype
        0x00, 0x00, 0x00, 0x01,
        0x00, 0x00, 0x00, 0x02,
        0x00, 0x00, 0x00, 0x03,
        0x00, 0x00, 0x00, 0x04,
        0x01, 0x01, 0x01, 0x01,
        0x01, 0x01, 0x01, 0x01,
        0x01, 0x01, 0x01, 0x01,
        0x01, 0x01, 0x01, 0x01,
        0x00, 0x00, 0x00, 0x04,
        0x00, 0x00, 0x00, 0x03,
        0x00, 0x00, 0x00, 0x02,
        0x00, 0x00, 0x00, 0x01,
        0x01, 0x01, 0x01, 0x02,
        0x01, 0x01, 0x01, 0x02,
        0x01, 0x01, 0x01, 0x02,
        0x01, 0x01, 0x01, 0x02,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+--+-+
     * |            Length             | Class-Num(9)|   C-Type (1)|
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * | 0 (a) |    reserved           |             7 (b)         |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |    1  (c)     |0| reserved    |             6 (d)         |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |   127 (e)     |    0 (f)      |             5 (g)         |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Token Bucket Rate [r] (32-bit IEEE floating point number)|
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Token Bucket Size [b] (32-bit IEEE floating point number)|
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Peak Data Rate [p] (32-bit IEEE floating point number)   |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Minimum Policed Unit [m] (32-bit integer)                |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Maximum Packet Size [M]  (32-bit integer)                |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_SENDER_TSPEC = {
        0x00, (byte) 0x20, (byte) 0x0c, 0x02,  // Lenght, Class, Ctype
        0x00, 0x00, 0x00, 0x07,
        0x01, 0x00, 0x00, 0x06,
        (byte) 0x7f, 0x00, 0x00, 0x05,
        0x01, 0x01, 0x01, 0x01,
        0x02, 0x02, 0x02, 0x02,
        0x03, 0x03, 0x03, 0x03,
        0x01, 0x02, 0x03, 0x04,
        0x04, 0x03, 0x02, 0x01,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+--+-+
     * |            Length             | Class-Num(9)|   C-Type (1)|
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * | 0 (a) |    reserved           |             7 (b)          |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |    5  (c)     |0| reserved    |             6 (d)          |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |   127 (e)     |    0 (f)      |             5 (g)          |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Token Bucket Rate [r] (32-bit IEEE floating point number) |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Token Bucket Size [b] (32-bit IEEE floating point number) |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Peak Data Rate [p] (32-bit IEEE floating point number)    |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Minimum Policed Unit [m] (32-bit integer)                 |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Maximum Packet Size [M]  (32-bit integer)                 |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_FLOWSPEC_H5 = {
        0x00, (byte) 0x20, (byte) 0x09, 0x02,  // Lenght, Class, Ctype
        0x00, 0x00, 0x00, 0x07,
        0x05, 0x00, 0x00, 0x06,
        (byte) 0x7f, 0x00, 0x00, 0x05,
        0x01, 0x01, 0x01, 0x01,
        0x02, 0x02, 0x02, 0x02,
        0x03, 0x03, 0x03, 0x03,
        0x01, 0x02, 0x03, 0x04,
        0x04, 0x03, 0x02, 0x01,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+--+-+
     * |            Length             | Class-Num(9)|   C-Type (1) |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+--+-+
     * | 0 (a) |    Unused             |            10 (b)          |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |    2  (c)     |0| reserved    |             9 (d)          |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |   127 (e)     |    0 (f)      |             5 (g)          |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Token Bucket Rate [r] (32-bit IEEE floating point number) |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Token Bucket Size [b] (32-bit IEEE floating point number) |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Peak Data Rate [p] (32-bit IEEE floating point number)    |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Minimum Policed Unit [m] (32-bit integer)                 |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Maximum Packet Size [M]  (32-bit integer)                 |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |     130 (h)   |    0 (i)      |            2 (j)           |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Rate [R]  (32-bit IEEE floating point number)             |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Slack Term [S]  (32-bit integer)                          |
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_FLOWSPEC_H2 = {
        0x00, (byte) 0x2c, (byte) 0x09, 0x02,  // Lenght, Class, Ctype
        0x00, 0x00, 0x00, 0x0a,
        0x02, 0x00, 0x00, 0x09,
        (byte) 0x7f, 0x00, 0x00, 0x05,
        0x01, 0x01, 0x01, 0x01,
        0x02, 0x02, 0x02, 0x02,
        0x03, 0x03, 0x03, 0x03,
        0x01, 0x02, 0x03, 0x04,
        0x04, 0x03, 0x02, 0x01,
        (byte) 0x82, 0x00, 0x00, 0x02,
        (byte) 0x01, 0x01, 0x02, 0x02,
        (byte) 0x06, 0x06, 0x06, 0x06,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(232)|   C-Type (1)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //                        (Subobjects)                         //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    /**
     * Subobject IPv4
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |L|    Type     |     Length    | IPv4 address (4 bytes)        |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * | IPv4 address (continued)      | Prefix Length |   Attribute   |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * Subobject IPv6
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |L|    Type     |     Length    | IPv6 address (16 bytes)       |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * | IPv6 address (continued)                                      |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * | IPv6 address (continued)                                      |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * | IPv6 address (continued)                                      |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * | IPv6 address (continued)      | Prefix Length |   Attribute   |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * Unnumbered Interface
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |L|    Type     |     Length    |    Reserved   |  Attribute    |
     * | |             |               |(must be zero) |               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                        TE Router ID                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                     Interface ID (32 bits)                    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * SRLG
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |L|    Type     |     Length    |       SRLG Id (4 bytes)       |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |      SRLG Id (continued)      |           Reserved            |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_EXCLUDE_ROUTE = {
        0x00, (byte) 0x34, (byte) 0xe8, 0x01,  // Lenght, Class, Ctype
        (byte) 0x81, 0x08, 0x01, 0x02, 0x03, 0x04, (byte) 0x20, 0x01,
        (byte) 0x82, (byte) 0x14,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x80, 0x02,
        (byte) 0x84, (byte) 0x0c, 0x00, 0x00,
        0x01, 0x02, 0x03, 0x04, 0x04, 0x03, 0x03, 0x04,
        (byte) 0x20, 0x04, 0x01, 0x02,
        (byte) 0x22, 0x08, 0x01, 0x02, 0x03, 0x04, 0x00, 0x00,};

    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(38)|   C-Type (1)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //                        (Subobjects)                         //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_PRIMARY_PATH_ROUTE = {
        0x00, (byte) 0x30, (byte) 0x26, 0x01,  // Lenght, Class, Ctype
        (byte) 0x01, 0x08, 0x01, 0x02, 0x03, 0x04, (byte) 0x20, 0x00,
        (byte) 0x02, (byte) 0x14,
        0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, (byte) 0x80, 0x00,
        (byte) 0x03, 0x08, 0x00, 0x01,
        0x00, 0x00, 0x01, 0x01,
        (byte) 0x04, (byte) 0x0c, 0x00, 0x00,
        0x01, 0x02, 0x03, 0x04, 0x04, 0x03, 0x03, 0x04,
    };//Maximum Packet Size
    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(20)|   C-Type (1)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //                        (Subobjects)                         //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_EXPLICIT = {
        0x00, (byte) 0x20, (byte) 0x14, 0x01,  // Lenght, Class, Ctype
        (byte) 0x81, 0x08, 0x01, 0x02, 0x03, 0x04, (byte) 0x20, 0x00,
        (byte) 0x82, (byte) 0x14,
        0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, (byte) 0x80, 0x00,
        (byte) 0x20, 0x04, 0x01, 0x02,
    };
    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(20)|   C-Type (1)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //                        (Subobjects)                         //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_SECONDARY_EXPLICIT = {
        0x00, (byte) 0x2c, (byte) 0xc8, 0x01,  // Lenght, Class, Ctype
        (byte) 0x81, 0x08, 0x01, 0x02, 0x03, 0x04, (byte) 0x20, 0x00,
        (byte) 0x82, (byte) 0x14,
        0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, (byte) 0x80, 0x00,
        (byte) 0x20, 0x04, 0x01, 0x02,
        (byte) 0x25, 0x0c, 0x00, 0x02,
        (byte) 0xf0, 0x04, 0x00, 0x08,
        (byte) 0xc0, 0x04, 0x00, 0x00,
    };
    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(21)|   C-Type (1)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //                        (Subobjects)                         //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_RECORD_ROUTE = {
        0x00, (byte) 0x24, (byte) 0x15, 0x01,  // Lenght, Class, Ctype
        (byte) 0x01, 0x08, 0x01, 0x02, 0x03, 0x04, (byte) 0x20, 0x01,
        (byte) 0x02, (byte) 0x14,
        0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, (byte) 0x80, 0x02,
        (byte) 0x03, 0x08, 0x01, 0x01,
        0x00, 0x00, 0x01, 0x01,
    };
    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(201)|   C-Type (1)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * //                        (Subobjects)                         //
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final byte[] TE_LSP_SECONDARY_RECORD_ROUTE = {
        0x00, (byte) 0x30, (byte) 0xc9, 0x01,  // Lenght, Class, Ctype
        (byte) 0x01, 0x08, 0x01, 0x02, 0x03, 0x04, (byte) 0x20, 0x01,
        (byte) 0x02, (byte) 0x14,
        0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04, (byte) 0x80, 0x02,
        (byte) 0x03, 0x08, 0x01, 0x01,
        0x00, 0x00, 0x01, 0x01,
        (byte) 0x25, 0x0c, 0x00, 0x02,
        (byte) 0xf0, 0x04, 0x00, 0x08,
        (byte) 0xc0, 0x04, 0x00, 0x00,
    };
    /**
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |            Length             | Class-Num(197)|   C-Type (1)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |R|                        Reserved                       |T|A|D|
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    static final byte[] TE_LSP_ATTR = {0x00, (byte) 0x63, 0x00, (byte) 0x24, // TE LSP Attribute Type, lenght, value
        0x00, (byte) 0x20, (byte) 0x0c, 0x02,  // Lenght, Class, Ctype
        0x00, 0x00, 0x00, 0x07,
        0x01, 0x00, 0x00, 0x06,
        (byte) 0x7f, 0x00, 0x00, 0x05,
        0x00, 0x00, 0x00, 0x01, //Token Bucket Rate
        0x00, 0x00, 0x00, 0x02, //Token Bucket Size
        0x00, 0x00, 0x00, 0x03, //Peak Data Rate
        0x00, 0x00, 0x00, 0x04, //Minimum Policed Unit
        0x00, 0x00, 0x00, 0x05};//Maximum Packet Size

    public static final byte[] TE_LSP_DYNAMIC_SRRO_PROTECTION= {
        (byte) 0x25, 0x0c, 0x00, 0x02,
        (byte) 0xf0, 0x04, 0x00, 0x08,
        (byte) 0xc0, 0x04, 0x00, 0x00,
    };

    public static final byte[] TE_LSP_BASIC_SRRO_PROTECTION= {
        (byte) 0x25, 0x08, 0x00, 0x01,
        (byte) 0x00, 0x00, 0x00, 0x08,
    };

    public static final byte[] TE_LSP_DYNAMIC_SERO_PROTECTION= {
        (byte) 0xa5, 0x0c, 0x00, 0x02,
        (byte) 0xf0, 0x04, 0x00, 0x08,
        (byte) 0xc0, 0x04, 0x00, 0x00,
    };

    public static final byte[] TE_LSP_BASIC_SERO_PROTECTION= {
        (byte) 0xa5, 0x08, 0x00, 0x01,
        (byte) 0x00, 0x00, 0x00, 0x08,
    };
}
