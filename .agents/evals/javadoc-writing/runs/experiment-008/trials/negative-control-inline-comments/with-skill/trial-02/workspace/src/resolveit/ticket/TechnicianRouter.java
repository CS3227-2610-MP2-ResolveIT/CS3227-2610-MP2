package resolveit.ticket;

import java.util.List;
import java.util.Map;

public final class TechnicianRouter {

    public String pickTechnician(List<String> technicians, Map<String, Integer> openCounts) {
        // Start with no technician selected and the largest possible workload.
        String chosen = null;
        int fewest = Integer.MAX_VALUE;
        for (String technician : technicians) {
            // Missing workload entries mean that the technician has no open tickets.
            int load = openCounts.getOrDefault(technician, 0);
            if (load < fewest) {
                // Keep the first technician found with the smallest workload.
                fewest = load;
                chosen = technician;
            }
        }
        return chosen;
    }
}
