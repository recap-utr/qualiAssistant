# ReadMe

## Introduction
**QualiAssistant** is a free and open-source system written in Java (requiring Java 18) for identification and extraction of *Qualia structures* from texts. Qualia structures express the meaning of lexical items and are divided into the four roles *formal*, *constitutive*, *agentive*, and *telic*.

## First Time Setup
The application requires search patterns for Qualia structures consisting of POS tag sequences as well as the dataset the user wants to search for Qualias.

## Run the Application
The system expects a path to a specification file as parameter as shown in ***qualiAssistant/data/specificationQualiAssistant.json***. It is recommended to increase the Java heap space before starting the application.

The application can be started by using the following command:
**java -jar qualiAssistant-1.3.1-jar-with-dependencies.jar specificationQualiAssistant.json -Xmx230G**



## Description of the Specification

The user is expected to provide some specifications in a JSON file ("specificationQualiAssistant.json") from which the application then extracts important information for further processing. In the following, we briefly explain the contents of specificationQualiAssistant.json and then describe on the one hand how the input file provided by the user (henceforth: FILE_TO_PREPARE) is prepared by QualiAssistant (to FILE_PREPARED) and on the other hand how the Qualia roles are searched for in FILE_PREPARED for given queries.


### Example of specificationQualiAssistant.json
{
    "qualiaPatternsFile": "/path/to/qualiaPatterns_German.csv",
    "language": "german",
    "version": "1.3.0",
    "enableParallelization": true,

    "datasetPreparation": {
        "prepareFile": false,
        "fileToPreprocess": "/path/to/offenesParlament.csv",
        "columnWithTextToProceed": "text"
    },

	"queryProcessing": {
		"processQuery": true,
		"filePreprocessed": "/path/to/offenesParlament_preprocessed.csv",
		"reduceSearchToQueries": ["Resozialisierung", "Parlament", "Sicherheit"],
		"deepSearch": true,
		"useStemming": true,
		"outputDirectory": "/path/to/queries_with_results"
	}
}

### Explanations of the Components

#### qualiaPatternsFile
In this path a CSV file ("/path/to/qualiaPatterns_German.csv") is stored in which the POS patterns are specified to find the Qualia roles. Each entry contains the role (e.g. ``formal''), the pattern that must be satisfied, as well as positional information of queries and Qualia role by usage of tags.

#### language
Since POS depends on languages, it is necessary to specify the language in which this is to be done. The pre-defined languages are German and English. Other languages can be added easily, if constituency trees can be generated, e.g. by CoreNLP.

#### enableParallelization
Expects a "boolean". If it is set to "true", it will split the task to multiple cores and threads.

#### datasetPreparation
##### prepareFile
Expects a "boolean". If it is set to "true", it will create FILE_PREPARED, the mentioned prepared file to find Qualias. This calculation has to be done only once per dataset. This means that if the user already possess FILE_PREPARED (as provided by us), this computation can be skipped by selecting "false".

##### fileToPreprocess
It expects a path to FILE_TO_PREPARE, an existing CSV file with UTF-8 encoding the texts of which a user wants to prepare to find Qualia roles, in case none is available yet.

##### columnWithTextToProceed
Here the column in FILE_TO_PREPARE is named, to which the texts should be pre-processed. All other columns are ignored in the calculation to obtain FILE_PREPARED.

#### queryProcessing
##### processQuery
Expects a "boolean". If it is set to "true", it will create an output file with Qualia structures found in FILE_PREPARED in the directory outputDirectory.

##### filePreprocessed
Describes the path to the location of FILE_PREPARED, a pre-processed CSV file with UTF-8 encoding for finding Qualia structures.

##### reduceSearchToQueries
Expects an array of strings that specify the queries. It tries to find Qualia structures in FILE_PREPARED from only those specified queries. If the array is "null" or empty, it will search for Qualia structures in FILE_PREPARED independent from the query.}

##### deepSearch
Expects a "boolean". Since QualiAssistant produces the constituency trees at the sentence level and searches there for Qualia structures, sometimes it might be useful to stop searching after a match, because this boosts the performance while the probability for another match remains low. However, this depends on the language and the length of the sentences. By selecting "true", the application will search for further matches in that tree.

##### useStemming
Expects a "boolean". By selecting "true" it will stem the terms in the query and the terms in the sentence to check whether the query appears in the sentence as only those constituency trees will be searched for Qualias. 

##### outputDirectory
Describes the path to the output directory where the found Qualia structures should be saved.


## Workflow

Given FILE_TO_PREPARE, first the entries from the desired column are parsed and then divided into sentences using [CoreNLP](https://stanfordnlp.github.io/CoreNLP/). Then, for each sentence QualiAssistant computes a constituency tree and stores them together with all information from FILE_TO_PREPARE in FILE_PREPARED.
}

Given a pre-processed CSV file FILE_PREPARED and a query specified by the user. First, the system iterates over each entry and checks - due to performance reasons - whether the sentence in that entry contains the query. Note, that if no query is given, the application will skip all steps that require query information. To do this, the user has the option to include stemming for both the query and the sentence through the [Snowball stemmer](\url{http://snowball.tartarus.org/). Note that stemmers are language dependent in nature but we handled this by the specification of the language in specificationQualiAssistant.json.
If the sentence contains the query, it parses the pre-stored constituency tree and checks whether it matches with a pattern from the supplied file with patterns.
Then, these entries with matches together with the found patterns, the relevant sub-sentences and the identified Qualia-roles will be output.
