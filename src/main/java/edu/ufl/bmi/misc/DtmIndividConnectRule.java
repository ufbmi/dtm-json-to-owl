package edu.ufl.bmi.misc;


public class DtmIndividConnectRule {

    String sourceKey;
    String objPropKey;
    String targetKey;

    public DtmIndividConnectRule(String sourceKey, String objPropKey, String targetKey) {
	this.sourceKey = sourceKey;
	this.objPropKey = objPropKey;
	this.targetKey = targetKey;
    }

    public String getSourceKey() {
	return sourceKey;
    }
    
    public String getObjPropKey() {
	return objPropKey;
    }

    public String getTargetKey() {
	return targetKey;
    }
}