package preprocessor;

import edu.stanford.nlp.trees.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PreproQualiaPatternChecker {

    //############################## all this is the custom tree stuff ###############################

    //create a Tree test that has a root node with the label "NP"
    //and two children nodes with the labels "NN" and "NN"
    public static Tree trial = Tree.valueOf("(ROOT (S (NP (NN Climate) (NN change)) (VP (VBZ is) (PP (IN upon) (NP (PRP us)))) (. .)))");
    public static Tree test = Tree.valueOf("(NP (NN) (NN))");
    public static Tree buff = null;

    public static void blubber(){
        System.out.println("bla " + trial.pennString());
        System.out.println("blubber: " + checkIfTreeContainsSubtree(trial, test));




        System.out.println("trial: " + trial.pennString());
    }


    //check if test contains specific given subtree
    public static boolean checkIfTreeContainsSubtree(Tree tree, Tree subtree) {
        if (tree == null || subtree == null) {
            return false;
        }
        if (tree.equals(subtree)) {
            return true;
        }
        if(tree.label().equals(subtree.label())){
            if(tree.children().length == subtree.children().length){
                if((tree.getChildrenAsList().get(0).label() + "-1").equals(subtree.getChildrenAsList().get(0).label().toString())){
                    if((tree.getChildrenAsList().get(1).label() + "-2").equals(subtree.getChildrenAsList().get(1).label().toString())){

                        //correct subtree found
                        String word1 = tree.getChildrenAsList().get(0).getChild(0).label().toString();
                        String word2 = tree.getChildrenAsList().get(1).getChild(0).label().toString();
                        word1 = word1.replace("-1", "");
                        word2 = word2.replace("-2", "");
                        String multiword = word1 + " " + word2;

                        //found multiword, put it into the tree
                        Tree multiWordLeaf = Tree.valueOf("(NN)");
                        multiWordLeaf.addChild(Tree.valueOf("(" + multiword + ")"));
                        tree.setChild(0, multiWordLeaf);
                        tree.removeChild(1);

                        return true;
                    }
                }else{
                    return false;
                }
            }
        }
        for (Tree child : tree.children()) {
            if (checkIfTreeContainsSubtree(child, subtree)) {
                return true;
            }
        }
        return false;
    }

    /*
     public static boolean checkIfTreeContainsSubtree(Tree tree, Tree subtree) {
        if (tree == null || subtree == null) {
            return false;
        }
        if (tree.equals(subtree)) {
            return true;
        }
        if(tree.label().equals(subtree.label())){

            if(tree.children().length == subtree.children().length){
                for(int i = 0; i < tree.children().length; i++){
                    if(tree.getChild(i).label().equals(subtree.getChild(i).label())){
                       return true;
                    }
                }
                return true;
            }
        }
        for (Tree child : tree.children()) {
            if (checkIfTreeContainsSubtree(child, subtree)) {
                return true;
            }
        }
        return false;
    }
     */

    //############################## all this is the custom tree stuff ###############################

    /**
     * common patterns (list has to be updated when something new is found):
     * (NML (NN family) (NN planning)) (NNS programs)) -> one to many parentheses, parent node missing?
     * (NP (NML (NNP Care) (NNP Benefit)) (NN Guarantee))
     * (NP (DT the) (NN mass) (NN production))
     * (JJ Sustainable) (NNP World) (NNP Economy))) -> parent node missing
     * (NP (NML (JJR higher) (NN education)) (NN funding))
     * (NP (NN Sexuality) (CC and) (NN Gender)) (NP (NN Identity) -> two multiwords
     * (NP (JJ high) (NN conservation))) (NP (NP (NN value) (NNS forests)) -> ??? does this count -> parent node missing
     * ...(DT the) (NN mass) (NN production)) (PP (IN of) (NP (NML (NNS animals) (CC and) (NN animal)) (NNS products)))) -> (CC and) (NN animal)) (NNS products)... ?
     * (NP (NP (JJ major) (NN land) (NN degradation) (NNS problems)) --> multiword with adjective and additional word behind
     * (NP (NP (VBG rising) (NN sea) (NNS levels)) --> multiword with verb in front
     * (NP (ADJP (IN off) (HYPH -) (NN road)) (NNS vehicles))
     * (NP (NN address) (NN soil)) (NP (NML (NN degradation) (CC and) (NN soil)) (NN health)) -> first part wrongly assumed to be a multiword, 2nd part contains two
     * (NP (JJ Australian) (NML (NN animal) (NN welfare)) (NNS standards)))
     *
     * currently working (covers most cases):
     * (NP (NN climate) (NN change))
     * (NP (NN groundwater) (NNS systems))
     * (NP (NNP Climate) (NNP Change))
     * (NP (NNP Nuclear) (NNPS Issues))
     * (NP (JJ Genetic) (NNP Engineering) (CD 13))
     */

    public static String multiWordTree = ""; //todo access to new tree should be done differently
    private static final String[] patternList = {
            "NP,NN,#,NN,#",
            "NP,NN,#,NNS,#",
            "NP,NNP,#,NNP,#",
            "NP,NNP,#,NNPS,#",
            "NP,JJ,#,NNP,#"
    };
    //Erklärung der möglichen Patternformen:
    //Die Pattern sind möglich, da der Start des Regex immer ein "(" ist und die anderen Klammern ebenfalls immer identisch sind
    //# beudetet dort befindet sich ein Teil des Wortes
    //NP,NN,#,NN,# -> Standardpattern
    //2. Form: NML,NN,#,NN,#,),NNS,# -> ")" heißt hier ist extra schließklammer, sprich das danach ist anderer teilbaum/leaf aber wort geht weiter
    //3. : NP,NML,NNP,#,NNP,#,),NN,# -> extra Teilbaumsplit NML bevor wörter kommen
    //todo Pattern kann statt # auch $ enthalten, das wäre zwar ein Wort aber ungewollt, bspw.:
    //4. Form: NP,DT,$,NN,#,NN,#

    private static String createRegexFromPattern(String txt) {
        String returnValue = "";
        txt = txt + ",)"; //for convenience i add this, so the switch case is easier to handle
        String[] pattern = txt.split(",");

        StringBuilder buff = new StringBuilder("");
        for (int i = 0; i < pattern.length; i++) {
            switch (pattern[i]) {
                case "#":
                    buff.append(".+[)]"); //word
                    break;
                case ")":
                    buff.append("[)]"); //extra parentheses
                    break;
                default:
                    if (i == 0) {
                        buff.append(".*");//beginning of regex
                        buff.append("[(]").append(pattern[i]);
                    } else {
                        buff.append("[ ][(]").append(pattern[i]).append("[ ]");
                    }
            }
        }
        buff.append(".*"); //end of regex

        returnValue = buff.toString();
        return returnValue;
    }

    //todo: pattern regex not quite correct, the .+ also allows unwanted stuff in between eg. (NN <word> (CC bla)) (NN ...)
    //this should get filtered out by buildMultiWordTree since such a structure would get catched as an exception
    public static Boolean checkForPattern(String txt) {
        boolean returnValue = true;
        boolean bufferValue = false;
        String bufferText = txt;

        //try to find a multiword, if succesful take the new tree and check again until nothing more is found
        //this can handle multiple multiwords within a tree
        //todo: i dont think its possible to increase the algorithmic speed here, therefore any new patterns prolong the runtime
        while (returnValue) {
            for (String s : patternList) {
                if (Pattern.compile(createRegexFromPattern(s)).matcher(bufferText).find()) {
                    returnValue = buildMultiwordTree(bufferText, s);
                    if (returnValue) {
                        if (!bufferValue) {
                            System.out.println(txt);
                            bufferValue = true;
                        } else {
                            returnValue = false;
                        }
                        System.out.println(multiWordTree);
                        bufferText = multiWordTree;
                        break;
                    }
                }
            }
            returnValue = false;
        }

        /* //can print out anything without a multiword - use for testing
       if(bufferValue == false){
            System.out.println("###########none found:########## \n " + txt + "\n #########################");
        }
        */
        return bufferValue;
    }

    private static int getWordCount(String txt) {
        //coldheartedly taken from: https://www.baeldung.com/java-count-chars
        return (int) txt.chars().filter(ch -> ch == '#').count();
    }

    private static Boolean buildMultiwordTree(String txt, String pat) { //only works for bimultiwords like (NP (NNP Climate) (NNP Change))
        String[] words = new String[getWordCount(pat)];

        //extract the multiword and print
        String[] pattern = pat.split(",");

        String cursorText = "(" + pattern[0] + " (" + pattern[1] + " "; //build a string like "(NP (NN " to work the rest here
        int cursor = txt.indexOf(cursorText) + cursorText.length(); //move to the start of the first word
        String buff = txt.substring(cursor);
        int buffcursor = buff.indexOf(")"); //first closing bracket

        words[0] = buff.substring(0, buffcursor); //todo later this might need a check if pattern[x] contains '#' so you could handle more words

        buff = txt.substring(cursor + buffcursor); //move behind the first words ")"

        cursor = txt.indexOf((words[0] + ")")) + (words[0] + ")").length();
        buff = txt.substring(cursor);
        buffcursor = buff.indexOf(")");

        try {
            String buffText = "(" + pattern[3] + " "; //string like "(NN " for further search
            words[1] = buff.substring(buffText.length() + 1, buffcursor);

            //create the full partial tree e.g. (NP (NN firstword) ...)) to find and replace it

            if (words[0].contains("(")) { //something went wrong with splitting earlier
                char currentChar = '#'; //just a random init value
                int iter = words[0].length() - 1;

                //go backwards through the word until you hit " ", anything afterwards is the true first word
                do {
                    currentChar = words[0].charAt(iter);
                    iter--;
                } while (!(currentChar == ' '));
                words[0] = words[0].substring(iter + 2); //+2 because too lazy to change up the loop, currently produces eg "P Animal" without
            }

            //build up the new tree and replace old one
            String multiword = words[0] + "_" + words[1]; //todo: figure out why this _ is needed, it shouldnt be, but it breaks the entire tree if its just a space
            String fullTreePart = cursorText + words[0] + ") " + buffText + words[1] + "))";
            String newTreeRootText = "(" + pattern[0] + "(" + pattern[1] + " "; //this builds (NP (NN word word))
            //String newTreeRootText = "(" + pattern[0] + " "; -> this would build (NP word word)
            String newTree = newTreeRootText + multiword + "))";
            if (!txt.contains(fullTreePart)) {
                return false; //this means it used the wrong regex, eg NP,NN,NN instead of NP,NNP,NNP
            }
            multiWordTree = txt.replace(fullTreePart, newTree);
            return true;
        } catch (Exception e) {
            //anything that would result in this seems to be a tree that wasn't correctly filtered out by the regex
            //therefore one might find unknown structures for multiwords here
            return false;
        }
    }
}

/*

Failure cases:

(ROOT (FRAG (NP (NP (DT The) (NN war)) (PP (IN on) (NP (NN terrorism) (NN isn�)))) (NP (NP ($ �)) (SYM �) (NP (NN t) (NN working))) (. .)))
(ROOT (FRAG (NP (NP (DT The) (NN war)) (PP (IN on) (NP terrorism isn�))) (NP (NP ($ �)) (SYM �) (NP (NN t) (NN working))) (. .)))

--> issue with method that creates original tree i guess

 */
