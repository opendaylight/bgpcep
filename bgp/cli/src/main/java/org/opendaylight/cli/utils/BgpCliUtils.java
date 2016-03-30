/*
 * Copyright © 2016 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.cli.utils;

public final class BgpCliUtils{
    public static final String BGP_MBEANS_GET_STATS_OPERATION = "BgpPeerState";
    public static final String BGP_MBEANS_NAME = "org.opendaylight.controller:instanceName=example-bgp-peer";
    public static final String BGP_MBEANS_RESET_STATS_OPERATION = " resetStats";

    public static BGPStatisticsData parseMessage(Object message) {
        //TODO: parse message
        return new BGPStatisticsData();
    }

    public static final class BGPStatisticsData {
        private int holdtimeCurrent;
        private int keepAliveCurrent;
        private String sessionState;
        private String peerAddress;
        private String peerAs;
        private String peerGrCapability;
        private String speakerAddress;
        private int speakerPort;
        private long totalMsgsReceived;
        private long totalMsgsSent;

        public int getHoldtimeCurrent() {
            return holdtimeCurrent;
        }


        public int getKeepAliveCurrent() {
            return keepAliveCurrent;
        }

        public String getSessionState() {
            return sessionState;
        }

        public void setSessionState(String sessionState) {
            this.sessionState = sessionState;
        }

        public String getPeerAddress() {
            return peerAddress;
        }

        public void setPeerAddress(String peerAddress) {
            this.peerAddress = peerAddress;
        }

        public String getPeerAs() {
            return peerAs;
        }

        public void setPeerAs(String peerAs) {
            this.peerAs = peerAs;
        }

        public String getPeerGrCapability() {
            return peerGrCapability;
        }

        public void setPeerGrCapability(String peerGrCapability) {
            this.peerGrCapability = peerGrCapability;
        }

        public String getSpeakerAddress() {
            return speakerAddress;
        }

        public void setSpeakerAddress(String speakerAddress) {
            this.speakerAddress = speakerAddress;
        }

        public int getSpeakerPort() {
            return speakerPort;
        }

        public void setSpeakerPort(int speakerPort) {
            this.speakerPort = speakerPort;
        }

        public long getTotalMsgsReceived() {
            return totalMsgsReceived;
        }

        public void setTotalMsgsReceived(long totalMsgsReceived) {
            this.totalMsgsReceived = totalMsgsReceived;
        }

        public long getTotalMsgsSent() {
            return totalMsgsSent;
        }

        public void setTotalMsgsSent(long totalMsgsSent) {
            this.totalMsgsSent = totalMsgsSent;
        }

        public void setHoldtimeCurrent(int holdtimeCurrent) {
            this.holdtimeCurrent = holdtimeCurrent;
        }

        public void setKeepAliveCurrent(int keepAliveCurrent) {
            this.keepAliveCurrent = keepAliveCurrent;
        }
    }
}