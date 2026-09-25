package resolveit.ticket;

import java.util.List;
import java.util.Map;

public final class TechnicianRouter {

    public String pickTechnician(List<String> technicians, Map<String, Integer> openCounts) {
        // Start without a candidate so the first technician can become the baseline.
        String chosen = null;
        int fewest = Integer.MAX_VALUE;
        for (String technician : technicians) {
            // Technicians missing from the map currently have no open tickets.
            int load = openCounts.getOrDefault(technician, 0);
            // Keep the first technician seen with the lowest load.
            if (load < fewest) {
                fewest = load;
                chosen = technician;
            }
        }
        return chosen;
    }
}
