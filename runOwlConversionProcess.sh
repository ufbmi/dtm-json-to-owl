#!/bin/sh

mvn exec:java -Dexec.mainClass="edu.ufl.bmi.ontology.SoftwareLicenseProcessor" -Dexec.arguments="GO PENS" > output-license-2017-05-12T1645.txt 2> output-license-2017-05-12T1645.err

mvn exec:java -Dexec.mainClass="edu.ufl.bmi.ontology.DataFormatProcessor" -Dexec.arguments="GO PENS" > output-format-2017-05-12T1645.txt 2> output-format-2017-05-12T1645.err

mvn exec:exec >output-software-2017-05-12T1645.txt 2>output-software-2017-05-12T1645.err

mv developer_iris.txt src/main/resources/developer_iris-2017-05-12.txt

mvn exec:java -Dexec.mainClass="edu.ufl.bmi.ontology.DatasetProcessor" -Dexec.arguments="GO PENS" > output-dataset-2017-05-12T1645.txt 2> output-dataset-2017-05-12T1645.err
