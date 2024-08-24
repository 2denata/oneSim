/* 
 * 
 * 
 */
package report;

/**
 * Records the average buffer occupancy and its variance with format:
 * <p>
 * <Simulation time> <average buffer occupancy % [0..100]> <variance>
 * </p>
 *
 *
 */
import java.util.*;
//import java.util.List;
//import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;
import core.UpdateListener;

public class BufferNode extends Report implements UpdateListener {

    /**
     * Record occupancy every nth second -setting id ({@value}). Defines the
     * interval how often (seconds) a new snapshot of buffer occupancy is taken
     * previous:5
     */
    public static final String BUFFER_REPORT_INTERVAL = "occupancyInterval";
    /**
     * Default value for the snapshot interval
     */
    public static final int DEFAULT_BUFFER_REPORT_INTERVAL = 10000;

    private double lastRecord = Double.MIN_VALUE;
    private int interval;

    private Map<DTNHost, Double> bufferCounts = new HashMap<DTNHost, Double>();
    private Map<DTNHost, List> bufferInterval = new HashMap<>();
    private List<Integer> timeRecord = new ArrayList<>();

    public BufferNode() {
        super();

        Settings settings = getSettings();
        if (settings.contains(BUFFER_REPORT_INTERVAL)) {
            interval = settings.getInt(BUFFER_REPORT_INTERVAL);
        } else {
            interval = -1;
            /* not found; use default */
        }

        if (interval < 0) {
            /* not found or invalid value -> use default */
            interval = DEFAULT_BUFFER_REPORT_INTERVAL;
        }
    }

    public void updated(List<DTNHost> hosts) {
        if (isWarmup()) {
            return;
        }

        for (DTNHost host : hosts) {
            bufferCounts.put(host, host.getBufferOccupancy());
        }

        if (SimClock.getTime() - lastRecord >= interval) {
            lastRecord = SimClock.getTime();

            // simpan time record ke HashMap
            timeRecord.add((int) lastRecord);

            for (Map.Entry<DTNHost, Double> entry : bufferCounts.entrySet()) {
                // ambil key untuk node DTNHost
                DTNHost host = entry.getKey();

                // ambil value dari key
                double droppedMessages = entry.getValue();

                // cek apakah di nodeDropInterval sudah ada data node belum
                if (bufferInterval.containsKey(host)) {
                    bufferInterval.get(host).add(droppedMessages);
                } // kalau belum, bikin list baru
                else {
                    List<Double> droppedList = new ArrayList<>();
                    droppedList.add(droppedMessages);
                    bufferInterval.put(host, droppedList);
                }
            }
            bufferCounts.clear();
        }
    }

    /**
     * Prints a snapshot of the average buffer occupancy
     *
     * @param hosts The list of hosts in the simulation
     */
    private void printLine(List<DTNHost> hosts) {}

    @Override
    public void done() {
        String intervalWaktu = "";
        for (int tr : timeRecord) {
            intervalWaktu += tr + "ms\t\t";
        }
        write("Node\t" + intervalWaktu);
        write("------------------------------------------------------------"
        + "-----------------------------");

        String output = "";
        
        for (Map.Entry<DTNHost, List> entry : bufferInterval.entrySet()) {
            DTNHost host = entry.getKey();
            List<Double> bufferIntervalList = entry.getValue();
            output = "" + host + "\t";
            
            for (Double bil : bufferIntervalList) {
                double bulat = Math.round(bil*100.0) / 100.0;
                output += bulat + "\t\t";
            }
            write(output);
        }
        
        super.done();
    }
}
