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

public class PeopleRankWithDecisionEngineBackup implements RoutingDecisionEngine {


    /** identifier for damping factor */
    protected static final String DAMPING = "dampingFactor";
    
    /** identifier for threshold */
    protected static final String THRESHOLD = "threshold";

    protected static final double DAMPING_DEFAULT = 0.9;
    protected static final int THRESHOLD_DEFAULT = 3;
    
    
    protected double dampingFactor, peopleRank;
    protected int threshold;
    protected Map<DTNHost, Double> waktuAwal; 
    protected Map<DTNHost, List<Duration>> historiKoneksi;
    protected Map<DTNHost, Informasi<Double, Integer>> teman;

    public PeopleRankWithDecisionEngineBackup(Settings s) {
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

    public PeopleRankWithDecisionEngineBackup(PeopleRankWithDecisionEngineBackup proto) {
        this.peopleRank = 0;
        this.waktuAwal = new HashMap<>();
        this.historiKoneksi = new HashMap<>();
        this.teman = new HashMap<>();
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        PeopleRankWithDecisionEngineBackup prde = ambilDecisionRouterDari(peer);
        
        // catat waktu awal bertemu ke map
        this.waktuAwal.put(peer, SimClock.getTime());
        
        // cek antara host-peer. Jika belum berteman...
        if (!this.teman.containsKey(peer)) {

            // cek total pertemuan untuk dibandingkan dengan threshold
            double totalPertemuan = hitungTotalWaktuPertemuanDengan(peer);
            
            //jika sudah memenuhi threshold
            if (totalPertemuan >= threshold) {
                
                /* di sini hanya melakukan penambahan teman saja, tapi
                   belum melakukan pertukaran informasi, jadi memakai
                   informasi sementara dengan nilai 0 
                */
                Informasi<Double, Integer> informasiSementara = new Informasi<>(0.0, 0);
                
                // tambahkan peer ke map teman milik host
                this.teman.put(peer, informasiSementara);
                
                // tambahkan host ke map teman milik peer
                prde.teman.put(myHost, informasiSementara);
            }            
        
        // Jika host-peer sudah berteman....
        } else {

            // send informasi host ke peer
            Informasi<Double, Integer> data = new Informasi<>(getPeopleRank(), getSizeFriendship());
            send(peer, data);

            // receive info dari peer, update map
            data = receive(peer);
            this.teman.put(peer, data);

            // update people rank dari host dan peer
            updatePeopleRank();
            prde.updatePeopleRank();
            
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
    private void send(DTNHost peer, Informasi<Double, Integer> informasi) {
        PeopleRankWithDecisionEngineBackup prde = ambilDecisionRouterDari(peer);
        // update info di peer
        prde.teman.put(peer, informasi);
    }

    /**
     * Method dipakai untuk menerima informasi people rank dan jumlah friendship
     * dari node peer.
     *
     * @param peer Node DTNHost peer
     */
    private Informasi receive(DTNHost peer) {
        PeopleRankWithDecisionEngineBackup prde = ambilDecisionRouterDari(peer);
        double rankPeer = prde.getPeopleRank();
        int friendsPeer = prde.teman.size();

        return new Informasi(rankPeer, friendsPeer);
    }

    /**
     * Method dipakai untuk mengupdate nilai people rank setelah menerima
     * informasi terbaru dari peer
     *
     */
    private void updatePeopleRank() {
        double sigma = 0;

        //hitung nilai sigma
        for (Map.Entry<DTNHost, Informasi<Double, Integer>> entry : teman.entrySet()) {
            
            // perbarui people rank peer dulu sebelum ambil people rank
            
            double peopleRankTemp = entry.getValue().getPeopleRank();
            int size = entry.getValue().getSize();
            sigma += (peopleRankTemp / size);
            
        }
        // ubah/update nilai peoplerank
        this.peopleRank = (1 - dampingFactor) + dampingFactor * sigma;
    }

    /**
     * Mengembalikan nilai PeR(i) dari node i
     * @return nilai people rank node
     */
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
        // ambil decision engine dari node peer
        PeopleRankWithDecisionEngineBackup prde = ambilDecisionRouterDari(otherHost);
        
        // forward pesan jika node tujuan = destination ATAU PeR(peer) >= PeR(host)
        return (prde.getPeopleRank() >= this.peopleRank || m.getTo().equals(otherHost));
    }

    // konversi DTNHost ke Router Decision Engine
    private PeopleRankWithDecisionEngineBackup ambilDecisionRouterDari(DTNHost h) {
        MessageRouter routerLain = h.getRouter();
        assert routerLain instanceof DecisionEngineRouter : "Bukan Decision Engine Router";
        return (PeopleRankWithDecisionEngineBackup) ((DecisionEngineRouter) routerLain).getDecisionEngine();
    }


    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // cek waktu awal koneksi
        double awal = cek(thisHost, peer);
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
    }

    /**
     * Method untuk mengecek antar host dan peer pernah bertemu atau tidak
     * @param thisHost Host
     * @param peer Peer
     * @return waktu awal saat host-peer connection up
     */
    private double cek(DTNHost thisHost, DTNHost peer) {
        if (waktuAwal.containsKey(peer)) {
            return waktuAwal.get(peer);
        }
        return 0;
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
        return new PeopleRankWithDecisionEngineBackup(this);
    }
}
