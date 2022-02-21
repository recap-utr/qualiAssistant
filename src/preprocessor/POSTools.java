package preprocessor;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.trees.Tree;
import utils.LanguageManager;

import java.util.ArrayList;
import java.util.List;

public class POSTools {

    public static ArrayList<String> getSentences(String text, LanguageManager languageManager) {
        ArrayList<String> sentences = new ArrayList<>();

        CoreDocument doc = new CoreDocument(text);
        languageManager.getSentenceSplitting_pipeline().annotate(doc);
        for (CoreSentence sent : doc.sentences()) {
            sentences.add(sent.text());
        }

        return sentences;
    }


    public static String getPennStrings(List<Tree> constituency_trees) {
        StringBuilder pennStrings = new StringBuilder();
        List<String> subtreeList = new ArrayList<>();
        for (Tree tree : constituency_trees) {
            pennStrings.append(tree.pennString()).append("\n");
            Tree[] children = tree.children();
            for (Tree child : children) {
                traverseTree(child, subtreeList);
            }
        }

        return pennStrings.toString();
    }

    public static void traverseTree(Tree tree, List<String> subtreeList) {
        subtreeList.add(tree.toString());
        for (Tree child : tree.children()) {
            traverseTree(child, subtreeList);
        }
    }

}
