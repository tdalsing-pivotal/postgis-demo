CREATE TABLE ADDRESSES
( OBJECTID INTEGER,
  FEATUREID INTEGER,
  ROADTYPE TEXT,
  ROADNUMBER TEXT,
  HOUSENUM INTEGER,
  UNITNUM TEXT,
  HNRANGE TEXT,
  UNITEXTRA TEXT,
  BUILDING TEXT,
  FLOOR TEXT,
  ST_PREFIX TEXT,
  ST_NAME TEXT,
  ST_TYPE TEXT,
  ST_SUFFIX TEXT,
  ST_SUFFIX2 TEXT,
  MUNI TEXT,
  ALTPREFIX TEXT,
  ALTNAME TEXT,
  ALTTYPE TEXT,
  ALTSUFFIX TEXT,
  ALTSUFFIX2 TEXT,
  SUBDIV TEXT,
  VILLAGE TEXT,
  SIDE INTEGER,
  ABSSIDE TEXT,
  ESRISIDE TEXT,
  STRUC_TYPE INTEGER,
  SOURCE INTEGER,
  COMMENT TEXT,
  FIELDNOTE TEXT,
  STATE TEXT,
  COUNTY TEXT,
  LSN TEXT,
  ALSN TEXT,
  LHN TEXT,
  NLFIDNEW TEXT,
  ZIPCODE TEXT,
  USPS_CITY TEXT,
  SEGID INTEGER,
  TSSEGID INTEGER,
  PT_LEN DOUBLE PRECISION,
  MPVAL DOUBLE PRECISION,
  FIPSCODE TEXT,
  COMM TEXT,
  CERT_NUM TEXT,
  X DOUBLE PRECISION,
  Y DOUBLE PRECISION,
  DATEMODIFI TIMESTAMP,
  FULLADDRES TEXT,
  ESN TEXT,
  STATUS TEXT,
  ADDPTKEY TEXT,
  created_us TEXT,
  created_da TIMESTAMP,
  last_edite TEXT,
  last_edi_1 TIMESTAMP,
  GlobalID TEXT,
  GEOM GEOMETRY(POINT)
)