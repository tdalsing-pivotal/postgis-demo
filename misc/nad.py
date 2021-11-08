from csv import reader
import psycopg2

conn = psycopg2.connect(database='gis_test')
cur = conn.cursor()
count = 0

with open('/Users/tdalsing/projects/dominos/data/NAD_r6_AZ.txt', 'r') as f:
  c = reader(f)
  h = next(c)

  for r in c:

    if len(r) >= 34:
      guid = r[33]

      state_usps = r[1]
      house_num = r[20]
      street_name = r[15]
      street_dir = r[12]
      street_type = r[16]
      city = r[3]
      zip_code = r[7]
      lat = r[31]
      lon = r[30]

      if len(state_usps) > 0 and len(house_num) > 0 and len(street_name) > 0 and len(street_dir) > 0 and len(street_type) > 0 and len(city) > 0 and len(zip_code) > 0 and len(lat) > 0 and len(lon) > 0:

        sql = f"""insert into nad 
                  (guid,house_num,street_name,street_dir,street_type,city,state_usps,state_full,zip_code,geom) 
                  values (%s,%s,%s,%s,%s,%s,%s,%s,%s,ST_GeometryFromText(\'POINT({lon} {lat})\'))"""
        cur.execute(sql,(guid,house_num.upper(),street_name.upper(),street_dir.upper(),street_type.upper(),city.upper(),state_usps,state_usps,zip_code))

        count += 1
        if count % 10000 == 0:
          print(f'count = {count}')
          conn.commit()
          
print(f'final count = {count}')
conn.commit()