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
            if (load < fewest) {
                // Strictly smaller loads win, so ties keep the earlier technician.
                fewest = load;
                chosen = technician;
            }
        }
        return chosen;
    }
}
