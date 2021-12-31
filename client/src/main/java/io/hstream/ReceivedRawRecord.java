package io.hstream;

/** An object that represents a received raw record. */
public class ReceivedRawRecord {

  private RecordId recordId;

  private byte[] rawRecord;

  public ReceivedRawRecord(RecordId recordId, byte[] rawRecord) {
    this.recordId = recordId;
    this.rawRecord = rawRecord;
  }

  public RecordId getRecordId() {
    return recordId;
  }

  public byte[] getRawRecord() {
    return rawRecord;
  }
}