# ReadMe

## Introduction
**QualiAssistant** is a free and open-source system written in Java (requires Java 17) for identification and extraction of *Qualia structures* from texts.
Qualia structures express the meaning of lexical items and are divided into the four roles *formal*, *constitutive*, *agentive*, and *telic*.

## First Time Setup
The application requires search patterns for Qualia structures consisting of POS tag sequences as well as the dataset the user wants to search for Qualias.
Samples for both as well as a runnable JAR are available on request, until the corresponding paper is published.

## Run the Application
The system expects a path to a specification file as parameter as shown in ***qualiAssistant/data/specificationQualiAssistant.json***.
It is recommended to increase the Java heap space before starting the application.

The application can be started by using the following command:<br>
**java -jar qualiAssistant-1.0.0-jar-with-dependencies.jar specificationQualiAssistant.json -Xmx12G**
