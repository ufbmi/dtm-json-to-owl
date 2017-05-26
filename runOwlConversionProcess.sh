#!/bin/sh

dttm=`date "+%Y-%m-%dT%H%M"`
dt=`date "+%Y-%m-%d"`

echo $dttm
echo $dt

mvn exec:java -Dexec.mainClass="edu.ufl.bmi.ontology.SoftwareLicenseProcessor" -Dexec.arguments="src/main/resources/process-config.txt" -Dexec.cleanupDaemonThreads=false > output-license-$dttm.txt 2> output-license-$dttm.err
mv websites.txt src/main/resources/websites-$dt.txt

echo "finished processing licenses..."

mvn exec:java -Dexec.mainClass="edu.ufl.bmi.ontology.DataFormatProcessor" -Dexec.arguments="src/main/resources/process-config.txt, $dt" -Dexec.cleanupDaemonThreads=false > output-format-$dttm.txt 2> output-format-$dttm.err
mv websites.txt src/main/resources/websites-$dt.txt

echo "finished processing data formats..."

mvn exec:java -Dexec.mainClass="edu.ufl.bmi.ontology.DtmJsonProcessor" -Dexec.arguments="src/main/resources/process-config.txt, $dt" -Dexec.cleanupDaemonThreads=false > output-software-$dttm.txt 2> output-software-$dttm.err
#mvn exec:exec >output-software-$dttm.txt 2>output-software-$dttm.err

echo "finished processing software..."

mv developer_iris.txt src/main/resources/developer_iris-$dt.txt

echo "finished moving developer iris..."

mvn exec:java -Dexec.mainClass="edu.ufl.bmi.ontology.DatasetProcessor" -Dexec.arguments="src/main/resources/process-config.txt, $dt" -Dexec.cleanupDaemonThreads=false > output-dataset-$dttm.txt 2> output-dataset-$dttm.err
mv websites.txt src/main/resources/websites-$dt.txt

echo "finished processing datasets...\n\t...done."
