package report;

import core.DTNHost;
import core.SimScenario;
import java.util.List;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.PeopleRankWithDecisionEngine;

public class PeopleRankPerNodeReport extends Report {

    public PeopleRankPerNodeReport() {
    }
    
    @Override
    public void done() {
        List<DTNHost> node = SimScenario.getInstance().getHosts();
        
        String teks = "";
        
        for (DTNHost n : node) {
            MessageRouter routerLain = n.getRouter();
            PeopleRankWithDecisionEngine de = (PeopleRankWithDecisionEngine) ((DecisionEngineRouter) routerLain).getDecisionEngine();
            de.updatePeopleRank(n);
            teks += "Node " + n + " punya teman " + de.getSizeFriendship() + " rankingnya " + de.getPeopleRank() + "\n dengan teman " + de.getTeman() + "\n\n" ;
        }
        
        write(teks);
        
        super.done();
    }
}
