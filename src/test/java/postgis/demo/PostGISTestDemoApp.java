package postgis.demo;

import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.junit.jupiter.api.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
class PostGISTestDemoApp {

    @Test
    void generateSql() throws Exception {
        log.info("starting...");
        File file = new File("data/Stark_County_Addresses.shp");
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        log.info("loading file...");
        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        log.info("typeName = {}", typeName);

        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE;

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        SimpleFeatureType schema = collection.getSchema();

        String fields = schema.getAttributeDescriptors().stream()
                .filter(desc -> !desc.getLocalName().equalsIgnoreCase("the_geom"))
                .map(desc -> {
                    String name = desc.getLocalName();
                    Class<?> type = desc.getType().getBinding();

                    if (type == String.class) {
                        name += " TEXT";
                    } else if (type == Integer.class) {
                        name += " INTEGER";
                    } else if (type == Double.class) {
                        name += " DOUBLE PRECISION";
                    } else if (type == Date.class) {
                        name += " TIMESTAMP";
                    } else {
                        log.warn("type {} for {} not handled", type.getName(), name);
                    }

                    return name;
                })
                .collect(Collectors.joining(","));

        String ddl = "CREATE TABLE "+typeName+" ("+ fields+")";
        log.info("ddl = {}", ddl);
    }

    @Test
    void load() throws Exception {
        log.info("starting...");
        File file = new File("data/Stark_County_Addresses.shp");
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        log.info("loading file...");
        DataStore dataStore = DataStoreFinder.getDataStore(map);

        String[] typeNames = dataStore.getTypeNames();
        log.info("load: typeNames = {}", Arrays.toString(typeNames));

        String typeName = typeNames[0];
        log.info("typeName = {}", typeName);

        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE;

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        int size = collection.size();
        log.info("size = {}", size);

        SimpleFeatureType schema = collection.getSchema();
        List<String> attrNames = schema.getAttributeDescriptors().stream()
                .map(desc -> desc.getName().toString())
                .collect(Collectors.toList());
        log.info("attrNames = {}", attrNames);

        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            for (int i = 0; i < 10 && features.hasNext(); ++i) {
                SimpleFeature feature = features.next();

                attrNames.stream()
                        .filter(name -> !name.equalsIgnoreCase("the_geom"))
                        .forEach(name -> {
                            Object attribute = feature.getAttribute(name);
                            log.info("feature.getID = {}, {}  =  {}", feature.getID(), name, attribute);
                        });

            }
        }
    }

    private static final String[] FIELD_NAMES = {"OBJECTID", "FEATUREID", "ROADTYPE", "ROADNUMBER", "HOUSENUM", "UNITNUM", "BUILDING", "ST_PREFIX", "ST_NAME", "ST_TYPE", "ST_SUFFIX", "ST_SUFFIX2", "STRUC_TYPE", "COMMENT", "STATE", "COUNTY", "ZIPCODE", "USPS_CITY", "COMM", "FULLADDRES", "STATUS", "GlobalID"};
    private static final String SQL = "INSERT INTO ADDRESSES "
            + "(" + Arrays.stream(FIELD_NAMES).collect(Collectors.joining(",")) + ") "
            + "VALUES (" + IntStream.range(1, FIELD_NAMES.length).mapToObj(i -> "?").collect(Collectors.joining(",")) + ")";
}