package de.cronn.reflection.util.testclasses;

import java.util.List;

public class TestBeanWithListOfRecordHavingList {
	private String data;
	private List<RecordWithList> recordsWithList;

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public List<RecordWithList> getRecordsWithList() {
		return recordsWithList;
	}

	public void setRecordsWithList(List<RecordWithList> recordsWithList) {
		this.recordsWithList = recordsWithList;
	}
}
