create table nad (
  guid varchar(40) primary key,
  house_num varchar(80) not null,
  street_name varchar(80) not null,
  street_dir varchar(20) not null,
  street_type varchar(20) not null,
  city varchar(80) not null,
  state_usps char(2) not null,
  state_full char(100) not null,
  zip_code char(10) not null,
  geom geometry(point) not null
);

create index ndx_nad_addr on nad (house_num,street_name,city,zip_code);
create index ndx_nad_geom on nad using gist (geom);
