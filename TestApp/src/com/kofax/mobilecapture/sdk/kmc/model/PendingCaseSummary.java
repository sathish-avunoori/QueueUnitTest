// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.sdk.kmc.model;

import java.io.Serializable;

import org.json.JSONObject;

import android.util.Log;
/**
 * 
 * @author Sathish.Avunoori
 *Class to be used in importing KMC 1.2 pending cases into KMC 2.1 Items
 */
public class PendingCaseSummary implements Serializable {
	private static final long serialVersionUID = 5673605970839409034L;

	private long caseId;
	private String caseName;
	private String caseTypeName;
	private String kofaxCaseShortcutName;
	private String caseBaseDirectory;
	private String jobId;
	
	public long getCaseId() {
		return caseId;
	}

	public void setCaseId(long caseId) {
		this.caseId = caseId;
	}

	public String getCaseName() {
		return caseName;
	}

	public void setCaseName(String caseName) {
		this.caseName = caseName;
	}

	public String getCaseTypeName() {
		return caseTypeName;
	}

	public void setCaseTypeName(String caseTypeName) {
		this.caseTypeName = caseTypeName;
	}

	public String getKofaxCaseShortcutName() {
		return kofaxCaseShortcutName;
	}

	public void setKofaxCaseShortcutName(String kofaxCaseShortcutName) {
		this.kofaxCaseShortcutName = kofaxCaseShortcutName;
	}

	public String getCaseBaseDirectory() {
		return caseBaseDirectory;
	}

	public void setCaseBaseDirectory(String caseBaseDirectory) {
		this.caseBaseDirectory = caseBaseDirectory;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public JSONObject toJSON() {
		JSONObject jo = new JSONObject();

		try {
			jo.put("caseId", getCaseId());
			jo.put("caseName", getCaseName());
			jo.put("caseTypeName", getCaseTypeName());
			jo.put("kofaxCaseShortcutName", getKofaxCaseShortcutName());
			jo.put("caseBaseDirectory", getCaseBaseDirectory());
			jo.put("jobId", getJobId());
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "Error converting to JSONObject", e);
		}

		return jo;
	}

	public String toString() {
		return toJSON().toString();
	}

}
