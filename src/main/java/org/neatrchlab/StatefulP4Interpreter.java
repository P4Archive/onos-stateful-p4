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

import com.google.common.collect.ImmutableBiMap;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.context.Bmv2Interpreter;
import org.onosproject.bmv2.api.context.Bmv2InterpreterException;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;


import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;
/**
 * Created by ubuntu on 16-11-17.
 */
public class StatefulP4Interpreter implements Bmv2Interpreter {

    protected  static final String CONTROLLER_TABLE = "controller_table";
    protected static final String FORWARD_TABLE = "forward_table";
    protected static final String STATE_TABLE = "state_table";
    protected static final String STATE_TRANSFER_TABLE = "state_transfer_table";
    protected static final String ACTION_TABLE = "action_table";


    private static final ImmutableBiMap<Integer, String> TABLE_ID_MAP = ImmutableBiMap.of(
            0, CONTROLLER_TABLE,
            1, FORWARD_TABLE,
            2, STATE_TABLE,
            3, STATE_TRANSFER_TABLE,
            4, ACTION_TABLE);

    protected static final String STATE_METADATA = "state_metadata";
    protected static final String TARGET_ID = "target_id";
    protected static final String REGISTER_ID = "register_id";
    protected static final String CUR_STATE = "cur_state";
    protected static final String NEXT_STATE = "next_state";
    protected static final String TRIGGER = "trigger";

    private static final String DROP = "_drop";
    private static final String ALERT = "alert";
    private static final String SEND_TO_CPU = "send_to_cpu";
    private static final String FORWARD = "forward";
    protected static final String STATE_TRANSFER = "state_transfer";

    protected static final String GET_STATE_WITH_TCP_FLAG = "get_state_with_tcp_flag";
    protected static final String GET_STATE_WITH_IP_ID = "get_state_with_ip_id";
    protected static final String GET_STATE_WITH_IP_TOS = "get_state_with_ip_tos";
    protected static final String GET_SATTE_WITH_NOTHING = "get_state_with_nothing";
    protected static final String GET_STATE_WITH_TCP_SRC = "get_state_with_tcp_src_port";
    protected static final String GET_STATE_WITH_TCP_DST = "get_state_with_tcp_dst_port";

    private static final String PORT = "port";


    @Override
    public ImmutableBiMap<Integer, String> tableIdMap() {
        return TABLE_ID_MAP;
    }

    @Override
    public ImmutableBiMap<Criterion.Type, String> criterionTypeMap() {
        ImmutableBiMap.Builder<Criterion.Type, String> builder = ImmutableBiMap.builder();
        builder.put(Criterion.Type.IN_PORT, "standard_metadata.ingress_port");
        builder.put(Criterion.Type.ETH_DST, "ethernet.dstAddr");
        builder.put(Criterion.Type.ETH_SRC, "ethernet.srcAddr");
        builder.put(Criterion.Type.ETH_TYPE, "ethernet.etherType");
        return builder.build();
    }

    @Override
    public Bmv2Action mapTreatment(TrafficTreatment trafficTreatment, Bmv2Configuration bmv2Configuration)
            throws Bmv2InterpreterException {
        if (trafficTreatment.allInstructions().size() == 0) {
            return buildActionWithName(DROP);
        }

        for (Instruction instruction : trafficTreatment.allInstructions()) {
            switch (instruction.type()) {
                case OUTPUT: {
                    Instructions.OutputInstruction outputInstruction =
                            (Instructions.OutputInstruction) instruction;
                    if (outputInstruction.port() == PortNumber.CONTROLLER) {
                        return buildActionWithName(SEND_TO_CPU);
                    }
                    return buildForwardAction(outputInstruction.port().toLong(), bmv2Configuration);
                }
                default:
                    return buildActionWithName(DROP);
            }
        }

        return buildActionWithName(DROP);
    }

    private static Bmv2Action buildForwardAction(long port, Bmv2Configuration configuration) {
        int portBitWidth = configuration.action(FORWARD).runtimeData(PORT).bitWidth();
        ImmutableByteSequence portBs = null;
        try {
            portBs = fitByteSequence(ImmutableByteSequence.copyFrom(port), portBitWidth);
        } catch (Exception e) {
            return buildActionWithName(DROP);
        }
        return Bmv2Action.builder().withName(FORWARD).addParameter(portBs).build();

    }
    private static Bmv2Action buildActionWithName(String name) {
        return Bmv2Action.builder().withName(name).build();
    }


}
