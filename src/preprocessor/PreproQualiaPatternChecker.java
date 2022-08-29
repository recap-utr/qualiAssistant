package preprocessor;

import java.util.List;
import java.util.regex.Pattern;
//(ROOT (S (NP (DT The) (NNS Greens)) (VP (VBP are) (VP (VBN committed) (PP (IN to) (S (VP (VBG providing) (NP (NP (JJ free) (, ,) (JJ public) (JJ early) (NN childhood) (NN education)) (CC and) (NP (NN childcare) (NNS places))) (PP (IN for) (NP (NP (DT all) (JJ Australian) (NNS children)) (PP (IN in) (NP (DT the) (ADJP (NN year) (VBG preceding)) (JJ compulsory) (NN schooling)))))))))) (. .)))
public class PreproQualiaPatternChecker {
    /**
     * common patterns (list has to be updated):
     * (NML (NN family) (NN planning)) (NNS programs)) -> one to many parentheses, parent node missing?
     * (NP (NN climate) (NN change))
     * (NP (NN Child) (NN Care) (NN Tax) (NN rebate))
     * (NP (NML (NNP Care) (NNP Benefit)) (NN Guarantee))
     * (NP (NN groundwater) (NNS systems))
     * (NP (NNP Climate) (NNP Change))
     * (NP (DT the) (NN mass) (NN production))
     * (JJ Sustainable) (NNP World) (NNP Economy))) -> parent node missing
     * (NP (NML (JJR higher) (NN education)) (NN funding))
     * (NP (NNP National) (NNP Security))
     * (NP (NN Sexuality) (CC and) (NN Gender)) (NP (NN Identity) -> two multiwords
     * (NP (NNP Nuclear) (NNPS Issues))
     * (NP (JJ high) (NN conservation))) (NP (NP (NN value) (NNS forests)) -> ??? does this count -> parent node missing
     *  ...(DT the) (NN mass) (NN production)) (PP (IN of) (NP (NML (NNS animals) (CC and) (NN animal)) (NNS products)))) -> (CC and) (NN animal)) (NNS products)... ?
     */

    /*
    ############ CURRENT ISSUES #################
    -> multiple multiwords within a single tree are ignored, e.g.:  (NP permit extraction) (PP (IN from) (NP (NN groundwater) (NNS systems))
    -> only bi-multiwords work
     */

    public static String multiWordTree = ""; //todo access to new tree should be done differently
    public static String[] trialList = {
            "NP,NN,#,NN,#",
            "NP,NN,#,NNS,#",
            "NP,NNP,#,NNP,#",
            "NP,NNP,#,NNPS,#"
    };
    //NP,NN,#,NN,# -> Start ( ist immer identisch, ebenso Position der anderen Klammern/Leerzeichen || # = word
    //2. Form: NML,NN,#,NN,#,),NNS,# -> ")" heißt hier ist extra schließklammer, sprich das danach ist anderer teilbaum/leaf
    //3. : NP,NML,NNP,#,NNP,#,),NN,# -> extra Teilbaumsplit NML bevor wörter kommen
    //4. : JJ,#,DT,#,NN,#,NN,# -> auch erstes wort mit word

    public static String createRegexFromPattern(String txt){
        String returnValue = "";
        txt = txt + ",)"; //for convenience i add this, so the switch case is easier to handle
        String[] pattern = txt.split(",");

        StringBuilder buff = new StringBuilder("");
        for(int i = 0; i<pattern.length;i++){
            switch(pattern[i]){
                case "#":
                    buff.append(".+[)]"); //word
                    break;
                case ")":
                    buff.append("[)]"); //extra parentheses
                    break;
                default:
                    if (i==0){
                        buff.append(".*");//beginning of regex
                        buff.append("[(]").append(pattern[i]);
                    } else{
                        buff.append("[ ][(]").append(pattern[i]).append("[ ]");
                    }


            }

        }
        buff.append(".*"); //end of regex

        returnValue = buff.toString();
        return returnValue;
    }

    //todo: pattern regex not quite correct, the .+ also allows unwanted stuff in between eg. (NN <word> (CC bla)) (NN ...)
    public static Boolean checkForPattern(String txt) {
        boolean returnValue = false;

        for (String s : trialList) {
            if (Pattern.compile(createRegexFromPattern(s)).matcher(txt).find()) {
                returnValue = buildMultiwordTree(txt, s);
                if (returnValue) {
                    break;
                }
            }
        }

        return returnValue;
    }

    private static Boolean buildMultiwordTree(String txt, String pat){ //only works for bimultiwords, todo replace first/secWord with List (maybe use a count of # or something)
        //extract the multiword and print
        String[] pattern = pat.split(",");

        String cursorText = "(" + pattern[0] + " (" + pattern[1] + " "; //build a string like "(NP (NN " to work the rest here
        int cursor = txt.indexOf(cursorText) + cursorText.length(); //move to the start of the first word
        String buff = txt.substring(cursor);
        int buffcursor = buff.indexOf(")"); //first closing bracket

        String firstWord = buff.substring(0, buffcursor); //todo later this might need a check if pattern[x] contains '#'

        buff = txt.substring(cursor+buffcursor); //move behind the first words ")"

        cursor = txt.indexOf((firstWord+")")) + (firstWord+")").length();
        buff = txt.substring(cursor);
        buffcursor = buff.indexOf(")");

        try{
            String buffText = "(" + pattern[3] + " "; //string like "(NN " for further search
            String secWord = buff.substring(buffText.length()+1, buffcursor);

            //create the full partial tree e.g. (NP (NN firstword) ...)) to find and replace it

            if(firstWord.contains("(")){ //something went wrong with splitting earlier
                char currentChar = '#'; //just a random init value
                int iter = firstWord.length()-1;

                //go backwards through the word until you hit " ", anything afterwards is the true first word
                do{
                    currentChar = firstWord.charAt(iter);
                    iter--;
                }while(!(currentChar == ' '));
                firstWord = firstWord.substring(iter+2); //+2 because too lazy to change up the loop, currently produces eg "P Animal" without
            }

            //build up the new tree and replace old one

            String multiword = firstWord + " " + secWord;
            String fullTreePart = cursorText + firstWord + ") " + buffText + secWord + "))";
            String newTreeRootText = "(" + pattern[0] + " ";
            String newTree = newTreeRootText + multiword + ")";
            if(!txt.contains(fullTreePart)){
                return false; //this means it used the wrong regex, eg NP,NN,NN instead of NP,NNP,NNP
            }
            multiWordTree = txt.replace(fullTreePart, newTree);
            System.out.println(multiWordTree);
            return true;
        }catch (Exception e){
            //todo anything that would result in this seems to be a tree that wasn't correctly filtered out by the regex
            //therefore one might find unknown structures for multiwords here
            return false;
        }


    }
}
