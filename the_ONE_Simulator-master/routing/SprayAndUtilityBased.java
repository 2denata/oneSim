package routing;

import java.util.*;

import core.*;

public class SprayAndUtilityBased implements RoutingDecisionEngine {

    // number of copy
    public static final String NROF_COPIES_S = "nrofCopies";
    public static final String MSG_COUNT_PROP = "SprayAndFocus.copies";
    protected int initialNrofCopies;

    // P_init
    protected final static String P_INIT_SETTING = "initial_p";
    protected static final double DEFAULT_P_INIT = 0.75;
    protected double pinit;
    
    // beta
    protected final static String BETA_SETTING = "beta";
    protected static final double DEFAULT_BETA = 0.45;
    protected double beta;
    
    // secondtime unit
    protected final static String SECONDS_IN_UNIT_S = "secondsInTimeUnit";
    protected int secondsInTimeUnit;
    
    // default value
    protected static final double GAMMA = 0.92;
    protected static final int DEFAULT_UNIT = 30;

    protected double lastAgeUpdate;
    
    private Set<Message> msgStamp;
    private Map<DTNHost, Integer> relayed;
    private DTNHost meHost;
    private Map<DTNHost, Double> preds;
    

    public SprayAndUtilityBased(Settings s) {
        initialNrofCopies = s.getInt(NROF_COPIES_S);

        // settings for utility-based
        if (s.contains(BETA_SETTING)) {
            beta = s.getDouble(BETA_SETTING);
        } else {
            beta = DEFAULT_BETA;
        }

        if (s.contains(P_INIT_SETTING)) {
            pinit = s.getDouble(P_INIT_SETTING);
        } else {
            pinit = DEFAULT_P_INIT;
        }

        if (s.contains(SECONDS_IN_UNIT_S)) {
            secondsInTimeUnit = s.getInt(SECONDS_IN_UNIT_S);
        } else {
            secondsInTimeUnit = DEFAULT_UNIT;
        }

        preds = new HashMap<>();
        this.lastAgeUpdate = 0.0;
    }

    public SprayAndUtilityBased(SprayAndUtilityBased snf) {
        this.initialNrofCopies = snf.initialNrofCopies;
        
        // utility-based forward
        beta = snf.beta;
        pinit = snf.pinit;
        secondsInTimeUnit = snf.secondsInTimeUnit;
        meHost = snf.meHost;
        msgStamp = new HashSet<>();
        relayed = new HashMap<>();
        preds = new HashMap<>();
        this.lastAgeUpdate = snf.lastAgeUpdate;
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SprayAndUtilityBased(this);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        SprayAndUtilityBased de = getOtherSnFDecisionEngine(peer);
        
        calculatePredictability(myHost, peer, de);
    }

    void calculatePredictability(DTNHost myHost, DTNHost peer, SprayAndUtilityBased de) {
        // buat set baru dengan isi sebanyak jumlah tetangga host dan peer
        Set<DTNHost> hostSet = new HashSet<>(this.preds.size()
                + de.preds.size());
        
        // tambahkan 
        hostSet.addAll(this.preds.keySet());
        hostSet.addAll(de.preds.keySet());

        // lakukan pengumuran pada predictability host dan peer
        this.agePreds();
        de.agePreds();

        // Update preds for this connection
        double myOldValue = this.getPredFor(peer),
                peerOldValue = de.getPredFor(myHost),
                myPforHost = myOldValue + (1 - myOldValue) * pinit,
                peerPforMe = peerOldValue + (1 - peerOldValue) * de.pinit;
        preds.put(peer, myPforHost);
        de.preds.put(myHost, peerPforMe);

        // Update transistivities
        for (DTNHost h : hostSet) {
            myOldValue = 0.0;
            peerOldValue = 0.0;

            if (preds.containsKey(h)) {
                myOldValue = preds.get(h);
            }
            if (de.preds.containsKey(h)) {
                peerOldValue = de.preds.get(h);
            }

            if (h != myHost) {
                preds.put(h, myOldValue + (1 - myOldValue) * myPforHost * peerOldValue * beta);
            }
            if (h != peer) {
                de.preds.put(h, peerOldValue + (1 - peerOldValue) * peerPforMe * myOldValue * beta);
            }
        }
    }
    
    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        
        // Jika nrofCopies > 1, binary spray
        if (nrofCopies > 1) {
            nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
            m.updateProperty(MSG_COUNT_PROP, nrofCopies);
        } 
        
        return m.getTo() == aHost;
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROP, initialNrofCopies);
        return true;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);

        // jika nrofCopy masih > 1 maka copy pesan. Jika tidak, hapus pesan
        if (nrofCopies > 1) {
            nrofCopies /= 2;
        } else {
            return true;
        }

        m.updateProperty(MSG_COUNT_PROP, nrofCopies);

        return false;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        msgStamp.add(m);
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true;
        }

        int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        
        // jika nrofCopy tinggal 1, lakukan perbandingan predictability
        // antara host dan peer
        if (nrofCopies == 1) {
            SprayAndUtilityBased de = getOtherSnFDecisionEngine(otherHost);
            if (msgStamp.contains(m)) {
                relayed.put(meHost, !relayed.containsKey(meHost) ? 1 : relayed.get(meHost) + 1);
            }
            return de.getPredFor(m.getTo()) > this.getPredFor(m.getTo());
        }
        return nrofCopies > 1;
    }

    private SprayAndUtilityBased getOtherSnFDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        return (SprayAndUtilityBased) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    private void agePreds() {
        double timeDiff = (SimClock.getTime() - this.lastAgeUpdate)
                / secondsInTimeUnit;

        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(GAMMA, timeDiff);
        for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
            e.setValue(e.getValue() * mult);
        }

        this.lastAgeUpdate = SimClock.getTime();
    }
    
    /**
     * Returns the current prediction (P) value for a host or 0 if entry for the
     * host doesn't exist.
     *
     * @param host The host to look the P for
     * @return the current P value
     */
    private double getPredFor(DTNHost host) {
        agePreds(); // make sure preds are updated before getting
        if (preds.containsKey(host)) {
            return preds.get(host);
        } else {
            return 0;
        }
    }

    @Override
    public void update(DTNHost thisHost) {
    }
}
