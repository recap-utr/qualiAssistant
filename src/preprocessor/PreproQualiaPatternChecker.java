package preprocessor;

import java.util.regex.Pattern;
//(ROOT (S (NP (DT The) (NNS Greens)) (VP (VBP are) (VP (VBN committed) (PP (IN to) (S (VP (VBG providing) (NP (NP (JJ free) (, ,) (JJ public) (JJ early) (NN childhood) (NN education)) (CC and) (NP (NN childcare) (NNS places))) (PP (IN for) (NP (NP (DT all) (JJ Australian) (NNS children)) (PP (IN in) (NP (DT the) (ADJP (NN year) (VBG preceding)) (JJ compulsory) (NN schooling)))))))))) (. .)))
public class PreproQualiaPatternChecker {
    /**
     * common patterns (list has to be updated):
     * (NML (NN family) (NN planning)) (NNS programs))
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
     *
     */

    public static String multiWordTree = ""; //todo access to new tree should be done differently

    //todo: long term: give the pattern as a param and call this from another method
    //todo: pattern regex not quite correct, the .+ also allows unwanted stuff in between eg. (NN <word> (CC bla)) (NN ...)
    public static Boolean checkForPattern(String txt) {
        boolean returnValue = false;

        if (Pattern.compile(".*[(]NP[ ][(]NN[ ].+[)][ ][(]NN[ ].+[)]{2}.*").matcher(txt).find()){
            returnValue = buildMultiwordTree(txt);
        }

        return returnValue;
    }

    private static Boolean buildMultiwordTree(String txt){
        //extract the multiword and print
        int cursor = txt.indexOf("(NP (NN ") + "(NP (NN ".length();
        String buff = txt.substring(cursor);
        int buffcursor = buff.indexOf(")");

        String firstWord = buff.substring(0, buffcursor);

        buff = txt.substring(cursor+buffcursor); //move behind the first words ")"

        cursor = txt.indexOf((firstWord+")")) + (firstWord+")").length();
        buff = txt.substring(cursor);
        buffcursor = buff.indexOf(")");

        try{
            String secWord = buff.substring("(NN ".length()+1, buffcursor);

            String multiword = firstWord + " " + secWord;

            //create the full partial tree e.g. (NP (NN firstword) ...)) to find and replace it
            String fullTreePart = "(NP (NN " + firstWord + ") (NN " + secWord + "))";
            String newTree = "(NP " + multiword + ")";
            multiWordTree = txt.replace(fullTreePart, newTree);
            System.out.println("hellooo??? someone there???");
            return true;
        }catch (Exception e){
            //todo anything that would result in this seems to be a tree that wasn't correctly filtered out by the regex
            //therefore one might find unknown structures for multiwords here
            return false;
        }


    }
}
