
`create database gis_test`

```
CREATE EXTENSION postgis;
CREATE EXTENSION postgis_raster;
CREATE EXTENSION postgis_topology;
CREATE EXTENSION postgis_sfcgal;
CREATE EXTENSION fuzzystrmatch;
CREATE EXTENSION address_standardizer;
CREATE EXTENSION address_standardizer_data_us;
CREATE EXTENSION postgis_tiger_geocoder;
```

`shp2pgsql -e -I tl_2020_us_zcta520.shp >tl_2020_us_zcta520.sql`

`psql -f tl_2020_us_zcta520.sql gis_test`

`psql -f nad.ddl gis_test`

`python3 nad.py`

`curl http://localhost:8517/7317/Gary/Way/West/Phoenix/AZ`

`curl -X POST -H "Content-type: text/plain" -d "7317 West Gary Way Phoenix AZ, 85339" http://localhost:8517`