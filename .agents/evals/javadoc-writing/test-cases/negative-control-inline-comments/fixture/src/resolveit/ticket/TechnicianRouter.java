package resolveit.ticket;

import java.util.List;
import java.util.Map;

public final class TechnicianRouter {

    public String pickTechnician(List<String> technicians, Map<String, Integer> openCounts) {
        String chosen = null;
        int fewest = Integer.MAX_VALUE;
        for (String technician : technicians) {
            int load = openCounts.getOrDefault(technician, 0);
            if (load < fewest) {
                fewest = load;
                chosen = technician;
            }
        }
        return chosen;
    }
}
