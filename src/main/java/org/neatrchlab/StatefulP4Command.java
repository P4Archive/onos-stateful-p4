/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neatrchlab;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.neatrchlab.service.StatefulP4Service;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;

/**
 * Created by ubuntu on 16-11-17.
 */
@Command(scope = "onos", name = "statefulp4",
        description = "Stateful P4 Data Plane")
public class StatefulP4Command extends AbstractShellCommand {

    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String BIND = "bind";

    private int registerId = 1;

    @Argument(index = 0, name = "opertion", description = "Operations: start, stop,",
            required = true, multiValued = false)
    private String operation;

    @Argument(index = 1, name = "service", description = "Operations: show, install, uninstall, update",
            required = true, multiValued = false)
    private String service;

    @Option(name = "-d", aliases = "--dl_dst",
            description = "Data link source address", required = false, multiValued = false)
    private String dlDst;

    @Option(name = "-s", aliases = "--dl_src",
            description = "Data link source address", required = false, multiValued = false)
    private String dlSrc;

    @Option(name = "-t", aliases = "--dl_type",
            description = "Data link source address", required = false, multiValued = false)
    private String dlType;

    @Option(name = "-D", aliases = "--ip_dst",
            description = "Data link source address", required = false, multiValued = false)
    private String ipDst;

    @Option(name = "-S", aliases = "--ip_src",
            description = "Data link source address", required = false, multiValued = false)
    private String ipSrc;

    @Option(name = "-p", aliases = "--ip_proto",
            description = "Data link source address", required = false, multiValued = false)
    private String ipProto;

    @Option(name = "-a", aliases = "--tcp_src",
            description = "Data link source address", required = false, multiValued = false)
    private String tcpSrc;

    @Option(name = "-b", aliases = "--ip_dst",
            description = "Data link source address", required = false, multiValued = false)
    private String tcpDst;

    private int getNewRegisterId() {
        return registerId++;
    }

    @Override
    protected void execute() {
        StatefulP4Service p4Service = getService(StatefulP4Service.class);
        if (operation.equals(START)) {
            p4Service.startService(service);
        } else if (operation.equals(STOP)) {
            p4Service.stopService(service);
        } else if (operation.equals(BIND)) {
            p4Service.bindService(service, getNewRegisterId(), getTrafficSelector());
        } else {
            print(operation + " is not a valid operation");
        }
    }

    private TrafficSelector getTrafficSelector() {
        TrafficSelector.Builder builder = DefaultTrafficSelector.builder();

        if (dlDst != null) {
            builder.matchEthDst(MacAddress.valueOf(dlDst));
        }

        if (dlSrc != null) {
            builder.matchEthSrc(MacAddress.valueOf(dlSrc));
        }

        if (dlType != null) {
            builder.matchEthType(Short.parseShort(dlType));
        }

        if (ipDst != null) {
            builder.matchIPDst(IpPrefix.valueOf(ipDst));
        }

        if (ipSrc != null) {
            builder.matchIPSrc(IpPrefix.valueOf(ipSrc));
        }

        if (ipProto != null) {
            builder.matchIPProtocol(Byte.parseByte(ipProto));
        }

        if (tcpDst != null) {
            builder.matchTcpDst(TpPort.tpPort(Integer.parseInt(tcpDst)));
        }

        if (tcpSrc != null) {
            builder.matchTcpSrc(TpPort.tpPort(Integer.parseInt(tcpSrc)));
        }

        return builder.build();
    }
}
