package de.cronn.reflection.util.testclasses;

public class TestBeanWithRecordHavingAtomicLong {
	private String data;
	private RecordWithAtomicLong recordWithAtomicLong;

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public RecordWithAtomicLong getRecordWithAtomicLong() {
		return recordWithAtomicLong;
	}

	public void setRecordWithAtomicLong(RecordWithAtomicLong recordWithAtomicLong) {
		this.recordWithAtomicLong = recordWithAtomicLong;
	}
}
