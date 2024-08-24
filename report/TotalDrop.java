/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.UpdateListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P>
 * <strong>Note:</strong> if some statistics could not be created (e.g. overhead
 * ratio if no messages were delivered) "NaN" is reported for double values and
 * zero for integer median(s).
 */
public class TotalDrop extends Report implements MessageListener, UpdateListener {

    private int nrofDropped;
    private Map<DTNHost, Integer> nodeDrop;

    /**
     * Constructor.
     */
    public TotalDrop() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.nodeDrop = new HashMap<DTNHost, Integer>();
        this.nrofDropped = 0;
    }

    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (dropped) {
            if (nodeDrop.containsKey(where)) {
                int nodeDropNow = nodeDrop.get(where);
                nodeDrop.put(where, ++nodeDropNow);
            } else {
                nodeDrop.put(where, 1);
            }
            nrofDropped++;
        }
    }

    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }

    public void messageTransferred(Message m, DTNHost from, DTNHost to,
            boolean finalTarget) {
    }

    public void newMessage(Message m) {
    }

    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void done() {
        String statsText = "dropped: " + this.nrofDropped
                + "\n" + nodeDrop;
        write(statsText);
        
        Integer total = 0;
        for (Integer value : nodeDrop.values()) {
            total += value;
        }
        write("Total : " + total);
        super.done();
    }

    
    public void updated(List<DTNHost> hosts) {
    }

}
