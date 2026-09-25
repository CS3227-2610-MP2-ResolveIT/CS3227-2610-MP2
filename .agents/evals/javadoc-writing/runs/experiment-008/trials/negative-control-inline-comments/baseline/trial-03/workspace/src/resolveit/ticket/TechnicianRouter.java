package resolveit.ticket;

import java.util.List;
import java.util.Map;

public final class TechnicianRouter {

    public String pickTechnician(List<String> technicians, Map<String, Integer> openCounts) {
        String chosen = null;
        int fewest = Integer.MAX_VALUE;
        for (String technician : technicians) {
            // Treat technicians without an entry as having no open tickets.
            int load = openCounts.getOrDefault(technician, 0);
            // Keep the first technician seen at the lowest load.
            if (load < fewest) {
                fewest = load;
                chosen = technician;
            }
        }
        // Return null when the list is empty; otherwise return the least-loaded technician.
        return chosen;
    }
}
