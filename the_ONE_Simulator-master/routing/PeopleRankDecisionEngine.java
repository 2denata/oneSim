package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Tuple;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PeopleRankDecisionEngine implements RoutingDecisionEngine {

    public static final double DAMPING_FACTOR = 0.5;
    public static final int THRESSHOLD = 5;

    protected Set<DTNHost> teman;
    protected double peopleRank;
    protected Map<DTNHost, Integer> totalWaktuPertemuan;
    protected Map<DTNHost, Map<Double, Integer>> informasiTeman;

    public PeopleRankDecisionEngine() {
        this.teman = new HashSet<>();
        this.totalWaktuPertemuan = new HashMap<>();
        this.peopleRank = 0;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        // jika node peer belum jadi teman
        if (!teman.contains(peer)) {
            
            if (totalWaktuPertemuan.containsKey(peer)) {
                
                // ambil total waktu pertemuan sekarang
                int temp = totalWaktuPertemuan.get(peer);

                // jika pertemuan blum memenuhi threshold
                if (temp < THRESSHOLD) {
                   
                    // jika kurang, tambah pertemuan
                    totalWaktuPertemuan.put(peer, ++temp);
                    
                } else {
                    
                    // jika sudah memenuhi, tambah ke Set Friendship
                    teman.add(peer);
                }
            } else {
                totalWaktuPertemuan.put(peer, 1);
            }
            
        } // jika sudah jadi teman
        else {
            // ambil engine router dari node peer
            PeopleRankDecisionEngine prdeNodeLain = ambilDecisionRouterLain(peer);

            // ambil people rank dari node peer
            double peopleRankOtherNode = prdeNodeLain.peopleRank;

            // ambil jumlah friend dari node peer
            int friendOtherNode = prdeNodeLain.teman.size();

            // update PeopleRank node ini
            peopleRank += peopleRankOtherNode/friendOtherNode;

            /* peopleRank masih bentuk sigma saja, jika mau 
               mengambil value People Rank node, panggil method
               getPeopleRank()*/
        }
    }

    
    // PERLU MODIFIKASI, NILAI peopleRank HARUS SESUAI UPDATE TIAP NODENYA
    public double getPeopleRank() { 
        return (1 - DAMPING_FACTOR) + DAMPING_FACTOR * peopleRank;
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        // saat baru pertama kali bertemu, belum menjadi friendship lalu
        // akumulasi total waktu bertemu
        totalWaktuPertemuan.put(peer, 1);

    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        // ambil engine router dari node peer
        PeopleRankDecisionEngine prdeNodeLain = ambilDecisionRouterLain(otherHost);

        if (prdeNodeLain.getPeopleRank() >= getPeopleRank() || m.getTo().equals(otherHost)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return true;
        }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
        }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRankDecisionEngine();
    }

    private PeopleRankDecisionEngine ambilDecisionRouterLain(DTNHost h) {
        MessageRouter routerLain = h.getRouter();
        assert routerLain instanceof DecisionEngineRouter : "Bukan Decision Router";
        return (PeopleRankDecisionEngine) ((DecisionEngineRouter) routerLain).getDecisionEngine();
    }

}
