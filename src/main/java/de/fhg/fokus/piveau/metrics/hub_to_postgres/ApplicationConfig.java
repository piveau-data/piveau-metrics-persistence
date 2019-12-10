package de.fhg.fokus.piveau.metrics.hub_to_postgres;

import io.vertx.core.json.JsonArray;

public final class ApplicationConfig {

    static final String ADDRESS_FETCH = "addr.fetch";
    static final String ADDRESS_CLEAN = "addr.clean";

    static final String ENV_APPLICATION_PORT = "PORT";
    static final Integer DEFAULT_APPLICATION_PORT = 8087;

    static final String ENV_WORKER_COUNT = "WORKER_COUNT";
    static final Integer DEFAULT_WORKER_COUNT = 30;

    public static final String ENV_PIVEAU_HUB_SEARCH = "PIVEAU_HUB_SEARCH";
    public static final String DEFAULT_PIVEAU_HUB_SEARCH = "europeandataportal.eu"; // without https !!

    public static final String ENV_PIVEAU_HUB_HOST = "PIVEAU_HUB_HOST";
    public static final String DEFAULT_PIVEAU_HUB_HOST = "https://www.europeandataportal.eu/sparql";

    public static final String ENV_PIVEAU_HUB_PAGE_SIZE = "PIVEAU_HUB_PAGE_SIZE";
    public static final Integer DEFAULT_PIVEAU_HUB_PAGE_SIZE = 100;

    public static final String ENV_PGSQL_SERVER_HOST = "PGSQL_SERVER_HOST";
    public static final String DEFAULT_PGSQL_SERVER_HOST = "jdbc:postgresql://localhost:5432/mqa_hub";

    public static final String ENV_PGSQL_USERNAME = "PGSQL_USERNAME";
    public static final String DEFAULT_PGSQL_USERNAME = "postgres";

    public static final String ENV_PGSQL_PASSWORD = "PGSQL_PASSWORD";
    public static final String DEFAULT_PGSQL_PASSWORD = "postgres";

    public static final String ENV_APPLICATION_HOST = "HOST";
    public static final String DEFAULT_APPLICATION_HOST = "http://127.0.0.1:" + DEFAULT_APPLICATION_PORT;

    public static final String ENV_URL_CHECK_ENDPOINT = "URL_CHECK_ENDPOINT";
    public static final String DEFAULT_URL_CHECK_ENDPOINT = "http://127.0.0.1:8085/check";

    public static final String ENV_MACHINE_READABLE_FORMATS = "MACHINE_READABLE_FORMATS";
    public static final JsonArray DEFAULT_MACHINE_READABLE_FORMATS = new JsonArray()
            .add("cdf")
            .add("csv")
            .add("csv.zip")
            .add("esri shapefile")
            .add("geojson")
            .add("iati")
            .add("ical")
            .add("ics")
            .add("json")
            .add("kml")
            .add("kmz")
            .add("netcdf")
            .add("nt")
            .add("ods")
            .add("psv")
            .add("psv.zip")
            .add("rdf")
            .add("rdfa")
            .add("rss")
            .add("shapefile")
            .add("shp")
            .add("shp.zip")
            .add("sparql")
            .add("sparql web form")
            .add("tsv")
            .add("ttl")
            .add("wms")
            .add("xlb")
            .add("xls")
            .add("xls.zip")
            .add("xlsx")
            .add("xml")
            .add("xml.zip");
}
