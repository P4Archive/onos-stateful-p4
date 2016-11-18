package org.neatrchlab.service;

import org.onosproject.net.flow.TrafficSelector;

/**
 * Created by ubuntu on 16-11-17.
 */
public interface StatefulP4Service {

    public int startService(String service);
    public int stopService(String service);
    public int bindService(String service, int registerId, TrafficSelector trafficSelector);
}
