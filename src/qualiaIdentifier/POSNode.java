package qualiaIdentifier;

import java.util.ArrayList;
import java.util.List;

public class POSNode {

    private final String POSTag;
    private final String term;
    private final int layer;

    public POSNode parent;
    public final List<POSNode> children;

    public POSNode (String POSTag, String term, int layer) {
        this.POSTag = POSTag;
        this.term = term;
        this.layer = layer;
        this.parent = null;

        this.children = new ArrayList<>();
    }



    public String getPOSTag() {
        return POSTag;
    }

    public String getTerm() {
        return term;
    }

    public int getLayer() {
        return layer;
    }


    @Override
    public String toString() {
        return layer + ": " + getPOSTag() + " " + getTerm();
    }

}
