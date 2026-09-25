package resolveit.ticket;

import java.util.List;
import java.util.Map;

public final class TechnicianRouter {

    public String pickTechnician(List<String> technicians, Map<String, Integer> openCounts) {
        // Keep the least-loaded technician found so far.
        String chosen = null;
        int fewest = Integer.MAX_VALUE;
        for (String technician : technicians) {
            // A technician absent from the map currently has no open tickets.
            int load = openCounts.getOrDefault(technician, 0);
            // Strict comparison preserves the first technician in a tie.
            if (load < fewest) {
                fewest = load;
                chosen = technician;
            }
        }
        return chosen;
    }
}
