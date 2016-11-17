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
import org.neatrchlab.service.StatefulP4Service;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Created by ubuntu on 16-11-17.
 */
@Command(scope = "onos", name = "statefulp4",
        description = "Stateful P4 Data Plane")
public class StatefulP4Command extends AbstractShellCommand {

    private static final String START = "start";
    private static final String STOP = "stop";

    @Argument(index = 0, name = "opertion", description = "Operations: start, stop,",
            required = true, multiValued = false)
    private String operation;

    @Argument(index = 1, name = "service", description = "Operations: show, install, uninstall, update",
            required = true, multiValued = false)
    private String service;

    @Override
    protected void execute() {
        StatefulP4Service p4Service = getService(StatefulP4Service.class);
        if (operation.equals(START)) {
            p4Service.startService(service);
        } else if (operation.equals(STOP)) {
            p4Service.stopService(service);
        } else {
            print(operation + " is not a valid operation");
        }
    }
}
