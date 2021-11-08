const { Client } = require('pg')

console.log('creating client')
const client = new Client({
  host: 'gis-db-instance-1.cscw7gx3s7gm.us-east-2.rds.amazonaws.com',
  database: 'gis_test',
  user: 'postgres',
  password: 'postgres'
})

console.log('connecting client')
client.connect()

const sql = `select tl_2020_us_zcta520.zcta5ce20
  from nad join tl_2020_us_zcta520 on ST_Contains(tl_2020_us_zcta520.geom, nad.geom)
  where (nad.house_num,nad.street_name,nad.street_type,nad.zip_code)
  in (select house_num, name, suftype, postcode from standardize_address('us_lex','us_gaz', 'us_rules', '7313 West Gary Way Phoenix AZ 85339'))`

const handler = async (event) => {
  console.log('event = '+JSON.stringify(event, null, 2))
  const records = event.Records

  const promises = records.map(record => client.query(sql))
  return Promise.all(promises)
}
