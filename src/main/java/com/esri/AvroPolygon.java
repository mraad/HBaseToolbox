/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.esri;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class AvroPolygon extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"AvroPolygon\",\"namespace\":\"com.esri\",\"fields\":[{\"name\":\"spatialReference\",\"type\":{\"type\":\"record\",\"name\":\"AvroSpatialReference\",\"fields\":[{\"name\":\"wkid\",\"type\":\"int\",\"default\":4326}]}},{\"name\":\"rings\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"AvroCoord\",\"fields\":[{\"name\":\"x\",\"type\":\"double\",\"default\":0.0},{\"name\":\"y\",\"type\":\"double\",\"default\":0.0}]}}}}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public com.esri.AvroSpatialReference spatialReference;
  @Deprecated public java.util.List<java.util.List<com.esri.AvroCoord>> rings;

  /**
   * Default constructor.
   */
  public AvroPolygon() {}

  /**
   * All-args constructor.
   */
  public AvroPolygon(com.esri.AvroSpatialReference spatialReference, java.util.List<java.util.List<com.esri.AvroCoord>> rings) {
    this.spatialReference = spatialReference;
    this.rings = rings;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return spatialReference;
    case 1: return rings;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: spatialReference = (com.esri.AvroSpatialReference)value$; break;
    case 1: rings = (java.util.List<java.util.List<com.esri.AvroCoord>>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'spatialReference' field.
   */
  public com.esri.AvroSpatialReference getSpatialReference() {
    return spatialReference;
  }

  /**
   * Sets the value of the 'spatialReference' field.
   * @param value the value to set.
   */
  public void setSpatialReference(com.esri.AvroSpatialReference value) {
    this.spatialReference = value;
  }

  /**
   * Gets the value of the 'rings' field.
   */
  public java.util.List<java.util.List<com.esri.AvroCoord>> getRings() {
    return rings;
  }

  /**
   * Sets the value of the 'rings' field.
   * @param value the value to set.
   */
  public void setRings(java.util.List<java.util.List<com.esri.AvroCoord>> value) {
    this.rings = value;
  }

  /** Creates a new AvroPolygon RecordBuilder */
  public static com.esri.AvroPolygon.Builder newBuilder() {
    return new com.esri.AvroPolygon.Builder();
  }
  
  /** Creates a new AvroPolygon RecordBuilder by copying an existing Builder */
  public static com.esri.AvroPolygon.Builder newBuilder(com.esri.AvroPolygon.Builder other) {
    return new com.esri.AvroPolygon.Builder(other);
  }
  
  /** Creates a new AvroPolygon RecordBuilder by copying an existing AvroPolygon instance */
  public static com.esri.AvroPolygon.Builder newBuilder(com.esri.AvroPolygon other) {
    return new com.esri.AvroPolygon.Builder(other);
  }
  
  /**
   * RecordBuilder for AvroPolygon instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<AvroPolygon>
    implements org.apache.avro.data.RecordBuilder<AvroPolygon> {

    private com.esri.AvroSpatialReference spatialReference;
    private java.util.List<java.util.List<com.esri.AvroCoord>> rings;

    /** Creates a new Builder */
    private Builder() {
      super(com.esri.AvroPolygon.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(com.esri.AvroPolygon.Builder other) {
      super(other);
    }
    
    /** Creates a Builder by copying an existing AvroPolygon instance */
    private Builder(com.esri.AvroPolygon other) {
            super(com.esri.AvroPolygon.SCHEMA$);
      if (isValidValue(fields()[0], other.spatialReference)) {
        this.spatialReference = data().deepCopy(fields()[0].schema(), other.spatialReference);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.rings)) {
        this.rings = data().deepCopy(fields()[1].schema(), other.rings);
        fieldSetFlags()[1] = true;
      }
    }

    /** Gets the value of the 'spatialReference' field */
    public com.esri.AvroSpatialReference getSpatialReference() {
      return spatialReference;
    }
    
    /** Sets the value of the 'spatialReference' field */
    public com.esri.AvroPolygon.Builder setSpatialReference(com.esri.AvroSpatialReference value) {
      validate(fields()[0], value);
      this.spatialReference = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'spatialReference' field has been set */
    public boolean hasSpatialReference() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'spatialReference' field */
    public com.esri.AvroPolygon.Builder clearSpatialReference() {
      spatialReference = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'rings' field */
    public java.util.List<java.util.List<com.esri.AvroCoord>> getRings() {
      return rings;
    }
    
    /** Sets the value of the 'rings' field */
    public com.esri.AvroPolygon.Builder setRings(java.util.List<java.util.List<com.esri.AvroCoord>> value) {
      validate(fields()[1], value);
      this.rings = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'rings' field has been set */
    public boolean hasRings() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'rings' field */
    public com.esri.AvroPolygon.Builder clearRings() {
      rings = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    @Override
    public AvroPolygon build() {
      try {
        AvroPolygon record = new AvroPolygon();
        record.spatialReference = fieldSetFlags()[0] ? this.spatialReference : (com.esri.AvroSpatialReference) defaultValue(fields()[0]);
        record.rings = fieldSetFlags()[1] ? this.rings : (java.util.List<java.util.List<com.esri.AvroCoord>>) defaultValue(fields()[1]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
