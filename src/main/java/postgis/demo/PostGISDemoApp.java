package postgis.demo;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static lombok.AccessLevel.PRIVATE;
import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

@SpringBootApplication
@RestController
@Slf4j
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class PostGISDemoApp {

    JdbcTemplate jdbcTemplate;
    Timer queryTimer;
    Timer insertTimer;

    private static final ColumnMapRowMapper mapper = new ColumnMapRowMapper();
    private static final WKTWriter writer = new WKTWriter();

    private static final String[] FIELD_NAMES = {"OBJECTID", "FEATUREID", "ROADTYPE", "ROADNUMBER", "HOUSENUM", "UNITNUM", "BUILDING", "ST_PREFIX", "ST_NAME", "ST_TYPE", "ST_SUFFIX", "ST_SUFFIX2", "STRUC_TYPE", "COMMENT", "STATE", "COUNTY", "ZIPCODE", "USPS_CITY", "COMM", "FULLADDRES", "STATUS", "GlobalID"};
    private static final String INSERT_SQL = "INSERT INTO ADDRESSES "
            + "(" + Arrays.stream(FIELD_NAMES).collect(Collectors.joining(",")) + ",GEOM) "
            + "VALUES (" + IntStream.range(0, FIELD_NAMES.length).mapToObj(i -> "?").collect(Collectors.joining(",")) + ",ST_GeometryFromText(?))";

    private static final String SELECT_SQL = "select tl_2020_us_zcta520.zcta5ce20"
            + " from nad join tl_2020_us_zcta520 on ST_Contains(tl_2020_us_zcta520.geom, nad.geom)"
            + " where nad.house_num = ? and nad.street_name = ? and nad.street_type = ? and nad.street_dir = ? and nad.city = ? and nad.state_usps = ?";

    private static final String SELECT_PARSE_SQL = "select tl_2020_us_zcta520.zcta5ce20"
            + " from nad join tl_2020_us_zcta520 on ST_Contains(tl_2020_us_zcta520.geom, nad.geom)"
            + " where (nad.house_num,nad.street_name,nad.street_type,nad.zip_code)"
            + " in (select house_num, name, suftype, postcode from standardize_address('us_lex','us_gaz', 'us_rules', '%s'))";

    public PostGISDemoApp(JdbcTemplate jdbcTemplate, Timer queryTimer, Timer insertTimer) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryTimer = queryTimer;
        this.insertTimer = insertTimer;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(PostGISDemoApp.class, args);
    }

    @Bean
    public static Timer queryTimer(MeterRegistry registry) {
        return registry.timer("gis_test_query_timer");
    }

    @Bean
    public static Timer insertTimer(MeterRegistry registry) {
        return registry.timer("gis_test_insert_timer");
    }

    @GetMapping("/{addr}/{stName}/{stType}/{stDir}/{city}/{state}")
    public List<Map<String, Object>> findByAddressComps(
            @PathVariable("addr") String addr,
            @PathVariable("stName") String stName,
            @PathVariable("stType") String stType,
            @PathVariable("stDir") String stDir,
            @PathVariable("city") String city,
            @PathVariable("state") String state) throws Exception {
        log.info("findByAddressComps: addr = {}, stName = {}, stType = {}, stDir = {}, city = {}, state = {}", addr, stName, stType, stDir, city, state);
        List<Map<String, Object>> list = queryTimer.recordCallable(() -> jdbcTemplate.query(SELECT_SQL, mapper, addr, stName, stType, stDir, city, state));
        return list;
    }

    @PostMapping(value = "/", consumes = "text/plain", produces = "application/json")
    public List<Map<String, Object>> findByAddress(@RequestBody String address) throws Exception {
        log.info("findByAddress: address = {}", address);
        String sql = String.format(SELECT_PARSE_SQL, address);
        log.info("findByAddress: sql = {}", sql);
        List<Map<String, Object>> list = queryTimer.recordCallable(() -> jdbcTemplate.query(sql, mapper));
        return list;
    }

    @GetMapping("/{lat}/{lon}")
    public List<Map<String, Object>> findByLatLon(@PathVariable("lat") double lat, @PathVariable("lon") double lon) throws Exception {
        log.info("findByLatLon: lat = {}, lon = {}", lat, lon);
        String pt = String.format("POINT(%e %e)", lon, lat);
        String sql = String.format("select gid,zcta5ce20,geoid20,classfp20,mtfcc20,funcstat20,aland20,awater20,intptlat20,intptlon20 from tl_2020_us_zcta520 where ST_Contains(envelope, '%s') and ST_Contains(geom, '%s')", pt, pt);
        log.info("findByLatLon: lat = {}, lon = {}, sql = {}", lat, lon, sql);

        List<Map<String, Object>> list = queryTimer.recordCallable(() -> jdbcTemplate.query(sql, mapper));
        return list;
    }

    @GetMapping("/load")
    public void load() throws Exception {
        log.info("load: SQL = {}", INSERT_SQL);
        File file = new File("data/Stark_County_Addresses.shp");
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        log.info("load: reading file...");
        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String[] typeNames = dataStore.getTypeNames();
        log.info("load: typeNames = {}", Arrays.toString(typeNames));
        String typeName = typeNames[0];
        log.info("load: typeName = {}", typeName);

        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE;

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        int size = collection.size();
        log.info("load: size = {}", size);

        FeatureIterator<SimpleFeature> features = collection.features();

        log.info("load: creating sink...");
        Sinks.Many<SimpleFeature> sink = Sinks.many().unicast().onBackpressureBuffer();
        Flux<SimpleFeature> flux = sink.asFlux();

        flux
                .map(feature -> {
                    List<Object> attrValues = Arrays.stream(FIELD_NAMES).map(feature::getAttribute).collect(Collectors.toList());
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    String wkt = writer.write(geometry);
                    attrValues.add(wkt);
                    return attrValues.toArray();
                })
                .bufferTimeout(100, Duration.ofSeconds(10))
                .subscribe(list -> insertTimer.record(() -> jdbcTemplate.batchUpdate(INSERT_SQL, list)));

        log.info("load: writing data...");
        int count = 0;
        while (features.hasNext()) {
            SimpleFeature feature = features.next();
            sink.emitNext(feature, FAIL_FAST);

            if (++count % 1000 == 0) {
                log.info("load: count = {}", count);
            }
        }

        log.info("load: final count = {}", count);
        log.info("load: completing...");
        Sinks.EmitResult emitResult = sink.tryEmitComplete();
        boolean failure = emitResult.isFailure();
        log.info("load: emitResult = {}", emitResult);
    }

}

/*
select tl_2020_us_zcta520.zcta5ce20
  from nad join tl_2020_us_zcta520 on ST_Contains(tl_2020_us_zcta520.geom, nad.geom)
  where (nad.house_num,nad.street_name,nad.street_type,nad.zip_code)
  in (select house_num, name, suftype, postcode from standardize_address('us_lex','us_gaz', 'us_rules', '7313 W Gary Way Phoenix, AZ 85339'))
*/