package qualiaIdentifier;

public class PatternMatch {

    String immutableMatchingPattern_withTags;
    String immutableMatchingPattern;

    String onlyLeafsOfImmutableMatchingPattern_withTags;
    String onlyLeafsOfImmutableMatchingPattern;

    String subSentenceMatchingPattern_withTags;
    String subSentenceMatchingPattern;

    String expandedQueries;

    public PatternMatch(
            String immutableMatchingPattern_withTags,
            String immutableMatchingPattern,
            String onlyLeafsOfImmutableMatchingPattern_withTags,
            String onlyLeafsOfImmutableMatchingPattern,
            String subSentenceMatchingPattern_withTags,
            String subSentenceMatchingPattern,
            String expandedQueries) {

        this.immutableMatchingPattern_withTags = immutableMatchingPattern_withTags;
        this.immutableMatchingPattern = immutableMatchingPattern;
        this.onlyLeafsOfImmutableMatchingPattern_withTags = onlyLeafsOfImmutableMatchingPattern_withTags;
        this.onlyLeafsOfImmutableMatchingPattern = onlyLeafsOfImmutableMatchingPattern;
        this.subSentenceMatchingPattern_withTags = subSentenceMatchingPattern_withTags;
        this.subSentenceMatchingPattern = subSentenceMatchingPattern;
        this.expandedQueries = expandedQueries;
    }
}
