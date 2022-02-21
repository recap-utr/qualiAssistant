package qualiaIdentifier;

import java.util.List;

public class BooleanSubgraph {

    boolean subgraphFound;
    List<POSNode> foundPOSSequence;

    public BooleanSubgraph(boolean subgraphFound, List<POSNode> foundPOSSequence) {
        this.subgraphFound = subgraphFound;
        this.foundPOSSequence = foundPOSSequence;
    }
}
