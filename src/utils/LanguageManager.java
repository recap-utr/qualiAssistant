package utils;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.StringUtils;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.germanStemmer;

import java.util.Locale;
import java.util.Properties;

public class LanguageManager {

    private StanfordCoreNLP posTagging_pipeline;
    private StanfordCoreNLP sentenceSplitting_pipeline;
    private SnowballStemmer snowballStemmer;
    private final String language;


    public LanguageManager (String language) {
        this.language = language;

        switch (language.toLowerCase(Locale.ROOT)) {
            case "german" -> {
                // POS tags
                String[] posTagging_args = new String[]{"-props", "StanfordCoreNLP-german.properties",
                        "-encoding", "UTF-8",
                        "-annotators",
                        "tokenize,ssplit,pos,parse"
                        };
                Properties posTagging_props = StringUtils.argsToProperties(posTagging_args);
                posTagging_pipeline = new StanfordCoreNLP(posTagging_props);

                // sentence splitting
                String[] sentenceSplitting_args = new String[]{"-props", "StanfordCoreNLP-german.properties", "-annotators", "tokenize,ssplit"};
                Properties sentenceSplitting_props = StringUtils.argsToProperties(sentenceSplitting_args);
                sentenceSplitting_pipeline = new StanfordCoreNLP(sentenceSplitting_props);

                snowballStemmer = new germanStemmer();
            }
            case "english" -> {
                // POS tags
                String[] posTagging_args = new String[]{
                        "-encoding", "UTF-8",
                        "-annotators",
                        "tokenize,ssplit,pos,parse"};
                Properties posTagging_props = StringUtils.argsToProperties(posTagging_args);
                posTagging_pipeline = new StanfordCoreNLP(posTagging_props);

                // sentence splitting
                String[] sentenceSplitting_args = new String[]{"-annotators", "tokenize,ssplit"};
                Properties sentenceSplitting_props = StringUtils.argsToProperties(sentenceSplitting_args);
                sentenceSplitting_pipeline = new StanfordCoreNLP(sentenceSplitting_props);

                snowballStemmer = new englishStemmer();
            }
        }
    }

    public StanfordCoreNLP getPosTagging_pipeline() {
        return posTagging_pipeline;
    }

    public StanfordCoreNLP getSentenceSplitting_pipeline() {
        return sentenceSplitting_pipeline;
    }

    public SnowballStemmer getStemmer() {
        return snowballStemmer;
    }

    public String getLanguage() { return language; }
}
