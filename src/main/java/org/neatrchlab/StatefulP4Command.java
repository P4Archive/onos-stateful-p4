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
import org.onlab.packet.IpAddress;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
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

    private int registerId = 0;

    @Argument(index = 0, name = "opertion", description = "Operations: start, stop,",
            required = true, multiValued = false)
    private String operation;

    @Argument(index = 1, name = "service",
            description = "Stateful data plane service: Sateful Firwall (sfw) and Stateful LoadBalancer (slb)",
            required = true, multiValued = false)
    private String service;

    @Option(name = "-d", aliases = "--ip_dst",
            description = "IP source address", required = false, multiValued = false)
    private String ipDst;

    @Option(name = "-s", aliases = "--ip_src",
            description = "IP source address", required = false, multiValued = false)
    private String ipSrc;

    @Option(name = "-p", aliases = "--ip_proto",
            description = "IP protocol", required = false, multiValued = false)
    private String ipProto;

    @Option(name = "-D", aliases = "--tcp_src",
            description = "TCP source port", required = false, multiValued = false)
    private String tcpSrc;

    @Option(name = "-S", aliases = "--tcp_dst",
            description = "TCP destination port", required = false, multiValued = false)
    private String tcpDst;

    @Option(name = "-o", aliases = "--output",
            description = "Default output port", required = false, multiValued = false)
    private String port;

    @Option(name = "-r", aliases = "--register_id",
            description = "Register ID", required = false, multiValued = false)
    private String regId;

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
            if (regId == null) {
                p4Service.bindService(service, getNewRegisterId(), getTrafficSelector(), port);
            } else {
                p4Service.bindService(service, Integer.parseInt(regId), getTrafficSelector(), port);
            }

        } else {
            print(operation + " is not a valid operation");
        }
    }

    private TrafficSelector getTrafficSelector() {
        StatefulP4Service p4Service = getService(StatefulP4Service.class);
        TrafficSelector.Builder builder = DefaultTrafficSelector.builder();
        Bmv2ExtensionSelector.Builder extesionBuilder = Bmv2ExtensionSelector.builder();

        extesionBuilder.forConfiguration(AppComponent.STATEFUL_CONFIGURATION);

        extesionBuilder.matchExact("ipv4", "dstAddr", IpAddress.valueOf(ipDst).toOctets());
        extesionBuilder.matchExact("ipv4", "srcAddr", IpAddress.valueOf(ipSrc).toOctets());
        extesionBuilder.matchExact("ipv4", "protocol", Byte.valueOf(ipProto));
        // extesionBuilder.matchExact("tcp", "dstPort", (short) ((int) Integer.parseInt(tcpDst)));
        // extesionBuilder.matchExact("tcp", "srcPort", (short) ((int) Integer.parseInt(tcpSrc)));

        builder.extension(extesionBuilder.build(), p4Service.getDefaultDeviceId());

        return builder.build();
    }
}
