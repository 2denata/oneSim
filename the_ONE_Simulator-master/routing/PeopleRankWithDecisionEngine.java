/*
 * People Rank 
 * by JPD
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import routing.community.Duration;

public class PeopleRankWithDecisionEngine implements RoutingDecisionEngine, NodeRanking {

    /** identifier for damping factor */
    protected static final String DAMPING = "dampingFactor";
    
    /** identifier for threshold */
    protected static final String THRESHOLD = "threshold";

    protected static final double DAMPING_DEFAULT = 0.5;
    protected static final int THRESHOLD_DEFAULT = 3;
    
    protected double dampingFactor = 0.4;
    protected double peopleRank;
    protected int threshold;
    protected Map<DTNHost, Double> waktuAwal; 
    protected Map<DTNHost, List<Duration>> historiKoneksi;
    protected Map<DTNHost, Informasi<Double, Integer>> teman;

    public Map<DTNHost, List<Duration>> getHistoriKoneksi() {
        return historiKoneksi;
    }
    
    public PeopleRankWithDecisionEngine(Settings s) {
        if (s.contains(DAMPING)) {
            dampingFactor = s.getDouble(DAMPING);
        } else {
            dampingFactor = DAMPING_DEFAULT;
        }
        
        if (s.contains(THRESHOLD)) {
            threshold = s.getInt(THRESHOLD);
        } else {
            threshold = THRESHOLD_DEFAULT;
        }
    }

    public PeopleRankWithDecisionEngine(PeopleRankWithDecisionEngine proto) {
        this.peopleRank = 0.0;
        this.waktuAwal = new HashMap<>();
        this.historiKoneksi = new HashMap<>();
        this.teman = new HashMap<>();
    }

    
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        PeopleRankWithDecisionEngine prde = ambilDecisionRouterDari(peer);
        
        // catat waktu awal bertemu ke map
        this.waktuAwal.put(peer, SimClock.getTime());
        
        if (this.teman.containsKey(peer)) {

            // send informasi host ke peer
            int sizeBaru = getSizeFriendship();
            Informasi<Double, Integer> data = new Informasi<>(getPeopleRank(), sizeBaru);
            send(myHost, peer, data);
            
            // receive info dari peer, update map
            data = receive(peer);
            this.teman.put(peer, data);
            
            // update people rank dari host dan peer
            prde.updatePeopleRank(peer);
            updatePeopleRank(myHost);
        }
    }

    /**
     * Method untuk menghitung jumlah lamanya pertemuan antar host dan peer.
     * @param peer DTNHost lawan
     * @return total waktu pertemuan dalam second
     */
    private double hitungTotalWaktuPertemuanDengan(DTNHost peer) {
        // jika blum pernah ketemu, kasih nilai 0
        if (!historiKoneksi.containsKey(peer)) {
            return 0;
        }

        double total = 0;
        List<Duration> listPertemuan = historiKoneksi.get(peer);
        for (Duration duration : listPertemuan) {
            total += (duration.end - duration.start);
        }
        
        return total;
    }
    
    /**
     * Method dipakai untuk mengirim informasi people rank dan jumlah friendship
     * dari node host ke node peer.
     *
     * @param peer Node DTNHost peer tujuan
     * @param informasi Data peoplerank dan friendship dari host
     */
    private void send(DTNHost myHost, DTNHost peer, Informasi<Double, Integer> informasi) {
        PeopleRankWithDecisionEngine prde = ambilDecisionRouterDari(peer);
        
        // update info di peer
        prde.teman.put(myHost, informasi);
        prde.updatePeopleRank(peer);
    }

    /**
     * Method dipakai untuk menerima informasi people rank dan jumlah friendship
     * dari node peer.
     *
     * @param peer Node DTNHost peer
     */
    private Informasi receive(DTNHost peer) {
        PeopleRankWithDecisionEngine prde = ambilDecisionRouterDari(peer);
        double rankPeer = prde.getPeopleRank();
        int friendsPeer = prde.teman.size();

        return new Informasi(rankPeer, ++friendsPeer);
    }

    /**
     * Method dipakai untuk mengupdate nilai people rank setelah menerima
     * informasi terbaru dari peer
     *
     * @param myHost
     */
    public void updatePeopleRank(DTNHost myHost) {
        double sigma = 0;

        //hitung nilai sigma
        for (Map.Entry<DTNHost, Informasi<Double, Integer>> entry : teman.entrySet()) {
            double peopleRankTemp = entry.getValue().getPeopleRank();
            int size = entry.getValue().getSize();
            
            if (size == 0) {
                sigma += 0;
            } else {
                sigma += (peopleRankTemp / size);          
            }
        }
        // ubah/update nilai peoplerank
        this.peopleRank = (1 - dampingFactor) + dampingFactor * sigma;
        
        // update di setiap map teman peer yang simpan people rank ini
        for (Map.Entry<DTNHost, Informasi<Double, Integer>> e : teman.entrySet()) {
            PeopleRankWithDecisionEngine de = ambilDecisionRouterDari(e.getKey());
            
            de.teman.put(myHost, new Informasi<>(this.peopleRank, this.teman.size()));
        }
    }
    
    
    /**
     * Mengembalikan nilai PeR(i) dari node i
     * @return nilai people rank node
     */
    @Override
    public double getPeopleRank() {
        return this.peopleRank;
    }

    /**
     * Mengembalikan nilai F(i) dari node i
     * @return jumlah friendship dari node
     */
    public int getSizeFriendship() {
        return this.teman.size();
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        PeopleRankWithDecisionEngine prde = ambilDecisionRouterDari(otherHost);
        
        // forward pesan jika node tujuan = destination ATAU PeR(peer) >= PeR(host)
        return (prde.getPeopleRank() >= this.peopleRank || m.getTo().equals(otherHost));
    }

    // konversi DTNHost ke Router Decision Engine
    public PeopleRankWithDecisionEngine ambilDecisionRouterDari(DTNHost h) {
        MessageRouter routerLain = h.getRouter();
        assert routerLain instanceof DecisionEngineRouter : "Bukan Decision Engine Router";
        return (PeopleRankWithDecisionEngine) ((DecisionEngineRouter) routerLain).getDecisionEngine();
    }


    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // cek waktu awal koneksi
        double awal = cek(peer);
        double akhir = SimClock.getTime();
        
        /* mengecek atau buat list untuk historiKoneksi */ 
        List<Duration> histori;
        
        // jika baru pertama kali, buat list duration di map historiKoneksi
        if (!historiKoneksi.containsKey(peer)) {
            histori = new ArrayList<>();
            historiKoneksi.put(peer, histori);
        } 
        // jika sudah pernah, ambil list duration dari map 
        else {
            histori = historiKoneksi.get(peer);
        }
        // tambahkan informasi waktu awal dan akhir ke dalam list
        Duration pertemuan = new Duration(awal, akhir);
        histori.add(pertemuan);
        
        // hapus data waktu pertemuan sementara setelah selesai
        waktuAwal.remove(peer);
        
        // cek antara host-peer. Jika belum berteman...
        if (!this.teman.containsKey(peer)) {

            // cek total pertemuan untuk dibandingkan dengan threshold
            double totalPertemuan = hitungTotalWaktuPertemuanDengan(peer);
            
            //jika sudah memenuhi threshold
            if (totalPertemuan >= threshold) {
                PeopleRankWithDecisionEngine prde = ambilDecisionRouterDari(peer);
                
                int sizePeer = prde.getSizeFriendship();
                Informasi<Double, Integer> infoPeer = new Informasi<>(prde.peopleRank, ++sizePeer);
                
                // tambahkan peer ke map teman milik host
                this.teman.put(peer, infoPeer);
                
                // tambahkan host ke map teman milik peer
                Informasi<Double, Integer> infoKita = new Informasi<>(peopleRank, teman.size());
                prde.teman.put(thisHost, infoKita);
            }            
        } 
    }

    /**
     * Method untuk mengecek antar host dan peer pernah bertemu atau tidak
     * @param thisHost Host
     * @param peer Peer
     * @return waktu awal saat host-peer connection up
     */
    private double cek(DTNHost peer) {
        if (waktuAwal.containsKey(peer)) {
            return waktuAwal.get(peer);
        }
        return 0;
    }
    
    public Map<DTNHost, Informasi<Double, Integer>> getTeman() {
        return teman;
    }
    
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }
    
    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return m.getTo() == otherHost;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return m.getTo() == hostReportingOld;
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRankWithDecisionEngine(this);
    }
}
