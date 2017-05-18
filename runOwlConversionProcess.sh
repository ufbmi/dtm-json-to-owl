#!/bin/sh

mvn exec:java -Dexec.mainClass="edu.ufl.bmi.ontology.SoftwareLicenseProcessor" -Dexec.arguments="GO PENS" > output-license-2017-05-18T1400.txt 2> output-license-2017-05-18T1400.err

echo "finished processing licenses..."

mvn exec:java -Dexec.mainClass="edu.ufl.bmi.ontology.DataFormatProcessor" -Dexec.arguments="GO PENS" > output-format-2017-05-18T1400.txt 2> output-format-2017-05-18T1400.err

echo "finished processing data formats..."

mvn exec:exec >output-software-2017-05-18T1400.txt 2>output-software-2017-05-18T1400.err

echo "finished processing software..."

mv developer_iris.txt src/main/resources/developer_iris-2017-05-18.txt

echo "finished moving developer iris..."

mvn exec:java -Dexec.mainClass="edu.ufl.bmi.ontology.DatasetProcessor" -Dexec.arguments="GO PENS" > output-dataset-2017-05-18T1400.txt 2> output-dataset-2017-05-18T1400.err

echo "finished processing datasets\n\nDone."
