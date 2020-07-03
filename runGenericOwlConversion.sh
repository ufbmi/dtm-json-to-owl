mvn exec:java -Dhttps.protocols=TLSv1.3 -Djavax.net.debug=ssl,handshake -Dexec.mainClass="edu.ufl.bmi.ontology.GenericOwl2Converter" -Dexec.arguments="$1" -Dexec.cleanupDaemonThreads=false
