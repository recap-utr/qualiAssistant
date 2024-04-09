package preprocessor;

import edu.stanford.nlp.trees.Tree;

import java.util.regex.Pattern;

public class PreprocessQualiaPatternChecker {

    private static boolean treeChecker = true; //this value is used for a while loop to go through one tree multiple times

    //todo: make it possible to check for multiple pattern in one tree (see ideas in other comments)
    //create a list of Tree Patterns
    private static final Tree[] patternTrees = {
            Tree.valueOf("(NP (NN) (NN))"),
            Tree.valueOf("(NP (NN) (NNS))"),
            Tree.valueOf("(NP (NNP) (NNP))"),
            Tree.valueOf("(NP (NNP) (NNPS))"),
            Tree.valueOf("(NP (JJ) (NNP))")
    };

    //this takes a tree and checks if it matches any of the patterns
    public static Tree checkTreeForPattern(Tree tree) {
        boolean wasFound = false;
        Tree buff = tree.deepCopy(); //note: this isn't redundant, compiler might mark it as such
        for (Tree patternTree : patternTrees) {
            if (checkIfTreeContainsSubtree(tree, patternTree)) {

                // System.out.println("Found pattern: " + patternTree);
                // System.out.println("in tree: " + buff);
                // System.out.println("turned into: " + tree);
                wasFound = true;
            }
        }
        if (!wasFound) {
            // System.out.println("No pattern found in tree: " + tree);
            treeChecker = false;
            //tip: if you see an error of the algorithm here (new pattern eg), use .pennString() to get the tree structure and try to fix it
        }
        return tree;
    }

    //checks a tree for multiple patterns
    public static Tree checkForAllTreePattern(Tree tree){
        do{
            checkTreeForPattern(tree);
        }while (treeChecker);
        // System.out.println("########################end of tree check#######################################");
        return tree;
    }

    //this takes a tree and checks for multiple patterns via using the string method further down
    //it seems to have some issues, but should be faster than the previous method
    public static Tree checkForAllPattern(Tree tree){
        multiWordTree = tree.toString();
        Tree bufferTree = tree.deepCopy();
        int i = 0;

        while (checkForPattern(multiWordTree)){
            i++;
            multiWordTree = checkTreeForPattern(bufferTree).toString();
        }
        if(i>1){
            // System.out.println("Found " + i + " patterns in tree: " + tree.pennString());
        }
        return bufferTree;
    }


    //check if a tree contains specific given subtree
    public static boolean checkIfTreeContainsSubtree(Tree tree, Tree subtree) {
        if (tree == null || subtree == null) {
            return false;
        }
        if (tree.equals(subtree)) { //todo: might be redundant since this should never happen
            return true;
        }

        //a label is a string representation a node in the tree, so we check if the label of the tree is the same as the label of the subtree
        if (tree.label().equals(subtree.label())) {
            //tree patterns thus far have 2 children, so we check if the tree has as many
            if (tree.children().length == subtree.children().length) {
                //check if the subtree labels are equal to the tree labels
                //the labels created by Tree.valueOf() have a -<number> attached to them, which is why they have to be added here
                //this numbering system is however always similar, so we can just add it as a string to check for equality
                //this would now eg check if a subtree (NP (NN climate) (NN change)) has the same labels as (NP (NN) (NN))
                //if so, it extracts the words and changes the tree to (NP (NN climate change)) by removing one empty leaf
                if ((tree.getChildrenAsList().get(0).label() + "-1").equals(subtree.getChildrenAsList().get(0).label().toString())) {
                    if ((tree.getChildrenAsList().get(1).label() + "-2").equals(subtree.getChildrenAsList().get(1).label().toString())) {

                        //correct subtree found
                        String word1 = tree.getChildrenAsList().get(0).getChild(0).label().toString();
                        String word2 = tree.getChildrenAsList().get(1).getChild(0).label().toString();
                        word1 = word1.substring(0, word1.length() - 2);
                        word2 = word2.substring(0, word2.length() - 2);
                        String multiword = word1 + " " + word2;
                        multiword = multiword.replace("-", ""); //rarely the multiword contains a "-" -> likely caused by the subtree being found later than a double digit height

                        //found multiword, put it into the tree
                        Tree multiWordLeaf = Tree.valueOf("(NN)");
                        multiWordLeaf.addChild(Tree.valueOf("(" + multiword + ")"));
                        tree.setChild(0, multiWordLeaf);
                        if (tree.getChild(1) != null) { //todo: check if this is necessary
                            tree.removeChild(1);
                        }

                        return true;
                    }
                }
            }
        }

        //this walks through the entire tree and its children recursively
        //this however might lead to a slow down compared to a string based approach like further below
        //also note the extra comment above the string based approach, giving some ideas how to improve this algorithm
        for (Tree child : tree.children()) {
            if (checkIfTreeContainsSubtree(child, subtree)) {
                return true;
            }
        }
        return false;
    }

    //failure cases
   /*
   1)::::

   (ROOT
  (S
    (NP (DT Every) (NN child))
    (VP (VBZ has)
      (NP (DT a)
        (ADJP (JJ right)
          (S
            (VP (TO to)
              (VP (VB be)
                (ADVP (RB fully))
                (VP
                  (VP (VBN included))
                  (CC and)
                  (VP (VBP thrive)
                    (PP (IN in)
                      (NP (DT the) (NN education)))))))))
        (NN system)))
    (. .)))

    --> this tree should give back (NN education system) as well, not sure if it works with current code structure

    (ROOT (FRAG (NP (NN (address soil))) (NP (NML (NN degradation) (CC and) (NN soil)) (NN health)) (PP (IN as) (NP (NP (JJ key) (NN funding) (NNS priorities)) (PP (IN for) (NP (NML (JJ natural) (NN resource) (NN management)) (NNS programs))))) (. .)))
   doesnt find soil health here
    */
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
     * <p>
     * currently working (covers most cases):
     * (NP (NN climate) (NN change))
     * (NP (NN groundwater) (NNS systems))
     * (NP (NNP Climate) (NNP Change))
     * (NP (NNP Nuclear) (NNPS Issues))
     * (NP (JJ Genetic) (NNP Engineering) (CD 13))
     */

    //todo: #################### Potential improvements ################################################################################################
    //### below: code that searches for patterns via string operations --> might be faster
    //it might be clever to combine some parts of this with the above tree operations, so that you can check quick
    //...if a pattern exists, so you don't have to walk through the entire tree every time
    //...this also applies if you have more patterns in one tree

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
                            //System.out.println(txt);
                            bufferValue = true;
                        } else {
                            returnValue = false;
                        }
                        //System.out.println(multiWordTree);
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
            String multiword = words[0] + "_" + words[1]; //todo: figure out why this _ is needed, it shouldn't be, but it breaks the entire tree if its just a space
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
