package org.neatrchlab.service;

/**
 * Created by ubuntu on 16-11-17.
 */
public interface StatefulP4Service {

    public int startService(String service);
    public int stopService(String service);

}
