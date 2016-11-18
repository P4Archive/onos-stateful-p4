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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.neatrchlab.service.StatefulP4Service;
import org.neatrchlab.service.AbstractUpgradableFabricApp;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2DefaultConfiguration;
import org.onosproject.bmv2.api.context.Bmv2DeviceContext;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.bmv2.api.runtime.Bmv2ExtensionTreatment;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;

import org.onosproject.net.topology.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;i


import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
@Service
public class AppComponent extends AbstractUpgradableFabricApp implements StatefulP4Service {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String APP_NAME = "StatefulP4";
    private static final String MODEL_NAME = "Stateful";

    private static final String JSON_CONFIG_PATH = "/stateful.json";

    private static final Bmv2Configuration STATEFUL_CONFIGURATION = loadConfiguration();
    private static final StatefulP4Interpreter STATEFUL_INTERPRETER = new StatefulP4Interpreter();
    private static final Bmv2DeviceContext STATEFUL_CONTEXT =
            new Bmv2DeviceContext(STATEFUL_CONFIGURATION, STATEFUL_INTERPRETER);

    private DeviceId defaultDeviceId;

    public AppComponent() {
        super(APP_NAME, MODEL_NAME, STATEFUL_CONTEXT);
    }
    @Override
    public boolean initDevice(DeviceId deviceId) {
        if (deviceId.toString().endsWith("#1")) {
            defaultDeviceId = deviceId;
        }
        log.info(deviceId.toString());
        return false;
    }

    @Override
    public List<FlowRule> generateSpineRules(DeviceId deviceId, Collection<Host> collection, Topology topology)
            throws FlowRuleGeneratorException {
        return new ArrayList<FlowRule>();
    }

    @Override
    public List<FlowRule> generateLeafRules(DeviceId deviceId, Host host,
                                            Collection<Host> collection,
                                            Collection<DeviceId> collection1,
                                            Topology topology)
            throws FlowRuleGeneratorException {
        return new ArrayList<FlowRule>();
    }

    private static Bmv2Configuration loadConfiguration() {
        try {
            JsonObject json = Json.parse(new BufferedReader(new InputStreamReader(
                    AppComponent.class.getResourceAsStream(JSON_CONFIG_PATH)))).asObject();
            return Bmv2DefaultConfiguration.parse(json);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }
    }

    private Bmv2ExtensionTreatment buildGetStateTreatment(String actionName, short targetId, short registerId) {
        return Bmv2ExtensionTreatment.builder().forConfiguration(STATEFUL_CONFIGURATION)
                .setActionName(actionName)
                .addParameter(StatefulP4Interpreter.TARGET_ID, targetId)
                .addParameter(StatefulP4Interpreter.REGISTER_ID, registerId)
                .build();
    }

    private Bmv2ExtensionSelector buildStateTransferSlector(short targetId, byte curState, short trigger) {
        return Bmv2ExtensionSelector.builder().forConfiguration(STATEFUL_CONFIGURATION)
                .matchExact(StatefulP4Interpreter.STATE_METADATA, StatefulP4Interpreter.TARGET_ID, targetId)
                .matchExact(StatefulP4Interpreter.STATE_METADATA, StatefulP4Interpreter.CUR_STATE, curState)
                .matchExact(StatefulP4Interpreter.STATE_METADATA, StatefulP4Interpreter.TRIGGER, trigger)
                .build();
    }

    private Bmv2ExtensionTreatment buildStateTransferTreatment(byte nextState) {
        return Bmv2ExtensionTreatment.builder().forConfiguration(STATEFUL_CONFIGURATION)
                .setActionName(StatefulP4Interpreter.STATE_TRANSFER)
                .addParameter(StatefulP4Interpreter.NEXT_STATE, nextState)
                .build();
    }

    private Bmv2ExtensionSelector buildActionSelector(byte nextState) {
        return Bmv2ExtensionSelector.builder().forConfiguration(STATEFUL_CONFIGURATION)
                .matchExact(StatefulP4Interpreter.STATE_METADATA, StatefulP4Interpreter.NEXT_STATE, nextState)
                .build();
    }

    private void installStateTransfer(short targetId, short trigger, byte curState, byte nextState) {
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        treatmentBuilder.extension(buildStateTransferTreatment(nextState), defaultDeviceId);
        selectorBuilder.extension(buildStateTransferSlector(targetId, curState, targetId), defaultDeviceId);

        try {
            FlowRule rule = flowRuleBuilder(defaultDeviceId, StatefulP4Interpreter.STATE_TRANSFER_TABLE)
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatmentBuilder.build())
                    .build();
            installFlowRules(Collections.singleton(rule));
        } catch (Exception e) {

        }
    }

    private void installAction(byte nextState, TrafficTreatment treatment) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .extension(buildActionSelector(nextState), defaultDeviceId)
                .build();
        try {
            FlowRule rule = flowRuleBuilder(defaultDeviceId, StatefulP4Interpreter.ACTION_TABLE)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .build();
            installFlowRules(Collections.singleton(rule));
        } catch (Exception e) {

        }
    }

    private void installState(TrafficSelector trafficSelector, String actionName, int targetId, int registerId) {
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        try {
            FlowRule rule = flowRuleBuilder(defaultDeviceId, StatefulP4Interpreter.ACTION_TABLE)
                    .withSelector(trafficSelector)
                    .withTreatment(treatmentBuilder
                            .extension(buildGetStateTreatment(actionName, (short) targetId, (short) registerId), defaultDeviceId)
                            .build())
                    .build();
            installFlowRules(Collections.singleton(rule));
        } catch (Exception e) {

        }
    }

    private static final Map<String, Integer> TARGET_ID_MAP = new HashMap<>();
    private static final Map<String, String> ACTION_MAP = new HashMap<>();
    static {
        TARGET_ID_MAP.put("sfw", 1);
        TARGET_ID_MAP.put("slb", 2);

        ACTION_MAP.put("sfw", StatefulP4Interpreter.GET_STATE_WITH_TCP_FLAG);
        ACTION_MAP.put("slb", StatefulP4Interpreter.GET_SATTE_WITH_NOTHING);
    }

    /**
     * TCP Stateful Firewall
     * 0 + SYN --> 1
     * 1 + SYN --> ACK
     * */
    private void startStatefulFirewallService() {
        int targetId = TARGET_ID_MAP.get("slb");
    }

    private void startLoadBalancingService() {
        int targetId = TARGET_ID_MAP.get("slb");

        installStateTransfer((short) targetId, (short) 0, (byte) 0, (byte) 1);
        installStateTransfer((short) targetId, (short) 0, (byte) 1, (byte) 2);
        installStateTransfer((short) targetId, (short) 0,  (byte) 2, (byte) 1);

        TrafficTreatment treatment
                = DefaultTrafficTreatment.builder().setOutput(PortNumber.portNumber(2)).build();

        installAction((byte) 1, treatment);

        treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.portNumber(3)).build();
        installAction((byte) 2, treatment);

        treatment = DefaultTrafficTreatment.emptyTreatment();
        installAction((byte) 0, treatment);
    }

    @Override
    public int startService(String service) {
        if (service.equals("sfw")) {
            startStatefulFirewallService();
        } else if (service.equals("slb")) {
            startLoadBalancingService();
        } else {
            return 1;
        }
        return 0;
    }

    @Override
    public int stopService(String service) {
        return 0;
    }

    @Override
    public int bindService(String service, int registerId, TrafficSelector trafficSelector) {
        int targetId = TARGET_ID_MAP.get(service);
        String actionName = ACTION_MAP.get(service);
        try {
            installState(trafficSelector, actionName, targetId, registerId);
        } catch (Exception e) {
            return 1;
        }
        return 0;
    }
}
