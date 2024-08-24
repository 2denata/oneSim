package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

public class SprayAndWaitDecisionEngine implements RoutingDecisionEngine {

    /**
     * identifier for the initial number of copies setting ({@value})
     */
    public static final String NROF_COPIES = "nrofCopies";
    /**
     * identifier for the binary-mode setting ({@value})
     */
    public static final String BINARY_MODE = "binaryMode";
    /**
     * SprayAndWait router's settings name space ({@value})
     */
    public static final String SPRAYANDWAIT_NS = "SprayAndWaitRouter";
    /**
     * Message property key
     */
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "."
            + "copies";


    protected int initialNrofCopies;
    protected boolean isBinary;

    public SprayAndWaitDecisionEngine(Settings s) {
        Settings snwSettings = new Settings(SPRAYANDWAIT_NS);
        initialNrofCopies = snwSettings.getInt(NROF_COPIES);
//        isBinary = snwSettings.getBoolean(BINARY_MODE);
    }

    public SprayAndWaitDecisionEngine(SprayAndWaitDecisionEngine proto) {
        this.initialNrofCopies = proto.initialNrofCopies;
//        this.isBinary = proto.isBinary;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROPERTY, new Integer(initialNrofCopies));
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
        assert nrofCopies != null : "Not a SnW message: " + m;
        nrofCopies = (int) Math.ceil(nrofCopies/2.0);
        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        
        return m.getTo() != aHost;
    }

    // Penerima
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
//        return true;
        return m.getTo() != thisHost;
    }

    // Pengirim
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        return true;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
        nrofCopies /= 2;
        m.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return false;
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SprayAndWaitDecisionEngine(this);
    }

    @Override
    public void update(DTNHost thisHost) {
    }

}
