package qualiaIdentifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class QualiaPattern {

    static final String EMPTY_POS_TAG = String.valueOf(UUID.nameUUIDFromBytes("QualiAssistant".getBytes(StandardCharsets.UTF_8)));


    String role;
    String inputPattern;
    String inputPattern_withTags;
    List<String> queryEnvironmentDelimiterPOSTags;

    List<List<String>> listOfPossiblePOSSequences_withTags;
    List<List<String>> listOfPossiblePOSSequences;


    public QualiaPattern(String role, String inputPattern, String inputPattern_withTags, List<String> queryEnvironmentDelimiterPOSTags) {
        this.role = role;
        this.inputPattern = inputPattern;
        this.inputPattern_withTags = inputPattern_withTags;
        this.queryEnvironmentDelimiterPOSTags = queryEnvironmentDelimiterPOSTags;

        List<List<String>> possiblePOSSequencesInOneList_withTags = parseEnteredRequiredPOSSequence(inputPattern_withTags);
        this.listOfPossiblePOSSequences_withTags = extractListWithPossiblePOSSequences(possiblePOSSequencesInOneList_withTags).stream().distinct().collect(Collectors.toList());


        List<List<String>> possiblePOSSequencesInOneList = parseEnteredRequiredPOSSequence(inputPattern);
        this.listOfPossiblePOSSequences = extractListWithPossiblePOSSequences(possiblePOSSequencesInOneList).stream().distinct().collect(Collectors.toList());
    }


    private List<List<String>> parseEnteredRequiredPOSSequence (String requiredPOSSequence) {

        String[] requiredPOSSequenceArray = requiredPOSSequence.split("\\s+");
        List<List<String>> possiblePOSSequencesInOneList = new ArrayList<>();
        for (String statement : requiredPOSSequenceArray) {

            List<String> possiblePOSTagsAtThisPosition = new ArrayList<>();

            if (statement.matches(".*?\\[.*?].*?")) {
                possiblePOSTagsAtThisPosition.addAll(Arrays.asList(statement
                        .replace("[", "")
                        .replace("]", "")
                        .split(",")));
            } else if (statement.matches(".*?\\((.*?)\\).*?")) {
                possiblePOSTagsAtThisPosition.add(statement
                        .replace(")", "")
                        .replace("(", ""));
                possiblePOSTagsAtThisPosition.add(EMPTY_POS_TAG);
            } else if (statement.matches(".*?(.*)/(.+).*?.*?")) {
                possiblePOSTagsAtThisPosition.add(statement.trim());
            } else if (statement.matches(".*?([A-Z]+).*?")) {
                possiblePOSTagsAtThisPosition.add(statement.trim());
            } else {
                System.err.println("Error in method \"parseEnteredRequiredPOSSequence\"");
            }

            possiblePOSSequencesInOneList.add(possiblePOSTagsAtThisPosition);
        }
        return possiblePOSSequencesInOneList;
    }


    private List<List<String>> extractListWithPossiblePOSSequences (List<List<String>> possiblePOSSequencesInOneList) {

        List<List<String>> listOfPossiblePOSSequences = new ArrayList<>();

        List<String> POSTags = new ArrayList<>();
        for (List<String> listOfPossiblePOSSequence : possiblePOSSequencesInOneList) {
            POSTags.addAll(listOfPossiblePOSSequence);
        }

        for (int i=0; i<=(int)Math.pow(2, POSTags.size()); i++) {
            StringBuilder binaryString = new StringBuilder(Integer.toBinaryString(i));
            while (binaryString.length() < POSTags.size()) {
                binaryString.insert(0, "0");
            }
            List<String> possiblePOSSequence = new ArrayList<>();
            for (int j=0; j<binaryString.length(); j++) {
                if (binaryString.charAt(j) == '1') {
                    possiblePOSSequence.add(POSTags.get(j));
                }
            }

            if (possiblePOSSequence.size() == possiblePOSSequencesInOneList.size()) {
                boolean legalSequence = true;
                for (int j=0; j<possiblePOSSequence.size(); j++) {
                    if (!possiblePOSSequencesInOneList.get(j).contains(possiblePOSSequence.get(j))) {
                        legalSequence = false;
                        break;
                    }
                }

                if (legalSequence) {
                    listOfPossiblePOSSequences.add(possiblePOSSequence);
                }
            }

        }

        for (int i=listOfPossiblePOSSequences.size()-1; i>=0; i--) {
            for (int j=listOfPossiblePOSSequences.get(i).size()-1; j>=0; j--) {
                if (listOfPossiblePOSSequences.get(i).get(j).equals(EMPTY_POS_TAG)) {
                    listOfPossiblePOSSequences.get(i).remove(j);
                }
            }
        }

        return listOfPossiblePOSSequences;
    }

}
