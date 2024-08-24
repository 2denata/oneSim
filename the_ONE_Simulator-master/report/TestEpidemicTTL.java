/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;
import core.UpdateListener;
import java.util.ArrayList;
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
public class TestEpidemicTTL extends Report implements MessageListener, UpdateListener {

    private Map<DTNHost, Integer> nodeDrop;
    private Map<DTNHost, List> nodeDropInterval;
    private Map<DTNHost, List> messageTtlDesc;
    private List<Integer> timeRecord;
    private int interval;
    private double lastRecord = Double.MIN_VALUE;

    /**
     * Constructor.
     */
    public TestEpidemicTTL() {
        init();
        interval = 10000;
    }

    @Override
    protected void init() {
        super.init();
        this.nodeDrop = new HashMap<>();
        this.nodeDropInterval = new HashMap<>();
        this.messageTtlDesc = new HashMap<>();
        this.timeRecord = new ArrayList<>();
    }

    /*
    bikinan denata 
    smoga prof liat
    siapa tau bisa lulus cepat
     */
    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {

        if (isWarmup()) {
            return;
        }

        if (dropped) {
            // cek ada key atau ngga
            if (this.nodeDrop.containsKey(where)) {
                int nodeDropNow = nodeDrop.get(where);
                nodeDrop.put(where, ++nodeDropNow);
            } else {
                nodeDrop.put(where, 1);
            }
        }
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        
        
        // jika simulasi melewati batas interval
        if (SimClock.getTime() - lastRecord >= interval) {
            lastRecord = SimClock.getTime();

            // simpan data record waktu
            timeRecord.add((int) lastRecord);

            //rekap tiap dropped message ke HashMap dropMapInterval
            for (Map.Entry<DTNHost, Integer> entry : nodeDrop.entrySet()) {

                // ambil key untuk node DTNHost
                DTNHost host = entry.getKey();

                // ambil value dari key
                int droppedMessages = entry.getValue();

                // cek apakah di nodeDropInterval sudah ada data node belum
                if (nodeDropInterval.containsKey(host)) {
                    nodeDropInterval.get(host).add(droppedMessages);
                } // kalau belum, bikin list baru
                else {
                    List<Integer> droppedList = new ArrayList<>();
                    droppedList.add(droppedMessages);
                    nodeDropInterval.put(host, droppedList);
                }
            }
            // hapus data drop message tiap selesai direkap
            nodeDrop.clear();
        }
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
        if (messageTtlDesc.containsKey(from)) {
            messageTtlDesc.get(from).add((m.getTtl()- (SimClock.getTime()-m.getCreationTime())));
        } 
        else {
            List<Double> messageTTL = new ArrayList<>();
            messageTTL.add(m.getTtl()- (SimClock.getTime()-m.getCreationTime()));
            messageTtlDesc.put(from, messageTTL);
        }
    }

    @Override
    public void newMessage(Message m) {
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void done() {
        String intervalWaktu = "";
        for (int tr : timeRecord) {
            intervalWaktu += tr + "ms\t\t";
        }
        write("Node\t" + intervalWaktu);
        write("------------------------------------------------------------------");

        String output = "";

        // print data node & dropped message dari HashMap ke String
        for (Map.Entry<DTNHost, List> entry : nodeDropInterval.entrySet()) {
            DTNHost host = entry.getKey();
            List<Integer> droppedMessagesList = entry.getValue();
            output = "" + host + "\t";

            for (Integer droppedMessages : droppedMessagesList) {
                output += droppedMessages + "\t\t";
            }
            write(output);
        }
        
        // cek list message ttl
        // print data node & dropped message dari HashMap ke String
        for (Map.Entry<DTNHost, List> ttl : messageTtlDesc.entrySet()) {
            DTNHost host = ttl.getKey();
            List<Double> ttlList = ttl.getValue();
            output = "" + host + "\t";

            for (Double ttlL : ttlList) {
                output += ttlL + "\t\t";
            }
            write(output);
        }
        super.done();
    }
}
