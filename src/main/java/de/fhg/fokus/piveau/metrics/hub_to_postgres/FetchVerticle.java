package de.fhg.fokus.piveau.metrics.hub_to_postgres;

import de.fhg.fokus.piveau.metrics.hub_to_postgres.database.DatabaseProvider;
import de.fhg.fokus.piveau.metrics.hub_to_postgres.database.DatabaseProviderImpl;
import de.fhg.fokus.piveau.metrics.hub_to_postgres.model.Catalogue;
import de.fhg.fokus.piveau.metrics.hub_to_postgres.model.Dataset;
import de.fhg.fokus.piveau.metrics.hub_to_postgres.model.Distribution;
import de.fhg.fokus.piveau.metrics.hub_to_postgres.model.Violation;
import de.fhg.fokus.piveau.metrics.hub_to_postgres.model.rdf.SHACL;
import de.fhg.fokus.piveau.metrics.hub_to_postgres.model.url_check.UrlCheckRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;


public class FetchVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(FetchVerticle.class);

    private WebClient webClient;
    private DatabaseProvider databaseProvider;

    private String sparqlUrl;

    @Override
    public void start(Future<Void> startFuture) {

        WebClientOptions clientOptions = new WebClientOptions()
                .setDefaultHost(config().getString(ApplicationConfig.ENV_PIVEAU_HUB_SEARCH, ApplicationConfig.DEFAULT_PIVEAU_HUB_SEARCH))
                .setLogActivity(true);

        webClient = WebClient.create(vertx, clientOptions);
        databaseProvider = new DatabaseProviderImpl(vertx);

        sparqlUrl = config().getString(ApplicationConfig.ENV_PIVEAU_HUB_HOST, ApplicationConfig.DEFAULT_PIVEAU_HUB_HOST);

        vertx.eventBus().consumer(ApplicationConfig.ADDRESS_FETCH, handler -> handleFetchRequest());
        vertx.eventBus().consumer(ApplicationConfig.ADDRESS_CLEAN, this::cleanEntities);

        startFuture.complete();
    }

    private void handleFetchRequest() {

        getCatalogues().setHandler(catalogueHandler -> {
            if (catalogueHandler.succeeded()) {
                catalogueHandler.result().forEach(catalogue ->
                        isCatalogueDcatAp(catalogue.getInstanceId()).setHandler(dcatHandler -> {

                            catalogue.setDcat(dcatHandler.succeeded() && dcatHandler.result());

                            databaseProvider.upsertCatalogue(catalogue, catalogueUpsertHandler -> {
                                if (catalogueUpsertHandler.succeeded()) {
                                    // upsert result contains ID assigned by DB
                                    catalogue.setId(catalogueUpsertHandler.result());

                                    handleDatasets(catalogue, config().getInteger(ApplicationConfig.ENV_PIVEAU_HUB_PAGE_SIZE, ApplicationConfig.DEFAULT_PIVEAU_HUB_PAGE_SIZE), 0);

                                } else {
                                    LOG.error("{}", catalogueUpsertHandler.cause().getMessage());
                                }
                            });
                        }));
            } else {
                LOG.error("{}", catalogueHandler.cause().getMessage());
            }
        });

        getKnownLicences().setHandler(licenceHandler -> {
            if (licenceHandler.succeeded()) {
                databaseProvider.upsertKnownLicences(licenceHandler.result(), licenceUpsertHandler -> {
                    if (licenceUpsertHandler.failed())
                        LOG.error("{}", licenceUpsertHandler.cause().getMessage());
                });
            } else {
                LOG.error("{}", licenceHandler.cause().getMessage());
            }
        });
    }

    private Future<List<Catalogue>> getCatalogues() {
        Future<List<Catalogue>> completionFuture = Future.future();

        webClient.get("/data/search/search")
                .addQueryParam("filter", "catalogue")
                .addQueryParam("limit", "100")
                .expect(ResponsePredicate.SC_OK).send(requestHandler -> {

            if (requestHandler.succeeded() && requestHandler.result().bodyAsJsonObject().getBoolean("success")) {

                List<Catalogue> catalogues = requestHandler.result().bodyAsJsonObject().getJsonObject("result").getJsonArray("results")
                        .stream().map(c -> {
                            JsonObject jsonCatalogue = (JsonObject) c;

                            Catalogue catalogue = new Catalogue();

                            String id = getKeySave(jsonCatalogue, new String[]{"id"});
                            String instanceId = id.length() < 255
                                    ? id
                                    : String.valueOf(id.hashCode());

                            catalogue.setInstanceId(instanceId);
                            catalogue.setName(catalogue.getInstanceId());

                            catalogue.setTitle(StringUtils.truncate(getLanguageStringSave(jsonCatalogue, catalogue.getSpatial(), new String[]{"title"}), 300));
                            catalogue.setDescription(StringUtils.truncate(getLanguageStringSave(jsonCatalogue, catalogue.getSpatial(), new String[]{"description"}), 255));

                            catalogue.setSpatial(StringUtils.truncate(getKeySave(jsonCatalogue, new String[]{"country", "id"}), 300));

                            return catalogue;
                        })
                        .collect(Collectors.toList());

                LOG.info("Retrieved [{}] catalogues", catalogues.size());
                completionFuture.complete(catalogues);
            } else {
                completionFuture.fail("Failed to fetch catalogues: {}" + requestHandler.cause());
            }
        });

        return completionFuture;
    }

    private void handleDatasets(Catalogue catalogue, int limit, int page) {

        JsonObject facets = new JsonObject()
                .put("catalog", new JsonArray().add(catalogue.getName()));

        webClient.get("/data/search/search")
                .setQueryParam("filter", "dataset")
                .setQueryParam("facets", facets.encode())
                .setQueryParam("limit", String.valueOf(limit))
                .setQueryParam("page", String.valueOf(page))
                .expect(ResponsePredicate.SC_OK)
                .send(requestHandler -> {
                    LOG.debug("Attempting to fetch datasets [{} to {}] of catalogue [{}]", limit * page, limit * page + limit, catalogue.getName());

                    if (requestHandler.succeeded() && requestHandler.result().bodyAsJsonObject().getBoolean("success")) {
                        Long totalDatasetCount = requestHandler.result().bodyAsJsonObject().getJsonObject("result").getLong("count");
                        LOG.debug("Processing datasets [{} to {} out of {}] of catalogue [{}]", limit * page, limit * page + limit, totalDatasetCount, catalogue.getName());

                        List<Dataset> datasets = new ArrayList<>();
                        List<Future> violationFutures = new ArrayList<>();

                        JsonArray remoteDatasets = requestHandler.result().bodyAsJsonObject().getJsonObject("result").getJsonArray("results");

                        remoteDatasets.forEach(dset -> {
                            JsonObject jsonDataset = (JsonObject) dset;

                            Dataset dataset = new Dataset();

                            String id = getKeySave(jsonDataset, new String[]{"id"});
                            String instanceId = id.length() < 255
                                    ? id
                                    : String.valueOf(id.hashCode());

                            dataset.setInstanceId(instanceId);
                            dataset.setName(dataset.getInstanceId());

                            dataset.setTitle(StringUtils.truncate(getLanguageStringSave(jsonDataset, catalogue.getSpatial(), new String[]{"title"}), 255));

                            dataset.setDistributions(jsonDataset.getJsonArray("distributions").stream().map(d -> {
                                JsonObject jsonDistribution = (JsonObject) d;

                                Distribution distribution = new Distribution();
                                distribution.setFormat(StringUtils.truncate(getKeySave(jsonDistribution, new String[]{"format", "id"}), 255));
                                distribution.setMachineReadable(config().getJsonArray(ApplicationConfig.ENV_MACHINE_READABLE_FORMATS, ApplicationConfig.DEFAULT_MACHINE_READABLE_FORMATS).contains(distribution.getFormat()));

                                distribution.setLicenceId(StringUtils.truncate(getKeySave(jsonDistribution, new String[]{"licence", "id"}), 255));

                                String accessUrl = getKeySave(jsonDistribution, new String[]{"access_url"});
                                distribution.setAccessUrl(addGeoFormatParams(accessUrl, distribution.getFormat()));
                                distribution.setStatusAccessUrl(0);

                                distribution.setDownloadUrl(getKeySaveFromArray(jsonDistribution, new String[]{"download_urls"}));
                                distribution.setStatusDownloadUrl(0);

                                distribution.setInstanceId(UUID.randomUUID().toString());

                                return distribution;

                            }).collect(Collectors.toList()));

                            if (!dataset.getDistributions().isEmpty()) {

                                // a dataset is machine readable as long as at least one distribution is machine readable
                                dataset.setMachineReadable(dataset.getDistributions().stream().anyMatch(Distribution::getMachineReadable));

                                // dataset licence is set to the licence used by the majority of distributions
                                Map<String, Long> licenceOccurrences = dataset.getDistributions().stream()
                                        .filter(distribution -> distribution.getLicenceId() != null)
                                        .collect(Collectors.groupingBy(Distribution::getLicenceId, Collectors.counting()));

                                if (!licenceOccurrences.isEmpty())
                                    dataset.setLicenceId(Collections.max(licenceOccurrences.entrySet(), Map.Entry.comparingByValue()).getKey().toLowerCase());
                            } else {
                                dataset.setMachineReadable(false);
                            }

                            if (catalogue.getDcat()) {

                                Future<Void> violationFuture = Future.future();
                                violationFutures.add(violationFuture);

                                getViolations(dataset.getInstanceId()).setHandler(violationHandler -> {
                                    if (violationHandler.succeeded()) {
                                        LOG.debug("Successfully retrieved violations for dataset with ID [{}]", dataset.getId());
                                        dataset.setViolations(violationHandler.result());
                                    } else {
                                        dataset.setViolations(new ArrayList<>());
                                        LOG.error("Failed to get violations: {}", violationHandler.cause().getMessage());
                                    }

                                    violationFuture.complete();
                                });
                            }

                            datasets.add(dataset);
                        });

                        CompositeFuture.all(violationFutures).setHandler(violationHandler ->
                                datasets.forEach(dataset ->
                                        databaseProvider.upsertDataset(catalogue.getId(), dataset, upsertHandler -> {
                                            if (upsertHandler.succeeded()) {

                                                String applicationHostUrl = config().getString(ApplicationConfig.ENV_APPLICATION_HOST, ApplicationConfig.DEFAULT_APPLICATION_HOST);
                                                String urlCheckEndpoint = config().getString(ApplicationConfig.ENV_URL_CHECK_ENDPOINT, ApplicationConfig.DEFAULT_URL_CHECK_ENDPOINT);

                                                if (applicationHostUrl != null && !applicationHostUrl.isEmpty()
                                                        && urlCheckEndpoint != null && !urlCheckEndpoint.isEmpty()) {

                                                    if (dataset.getDistributions() != null)
                                                        dataset.getDistributions().forEach(distribution -> {
                                                            if (distribution.getAccessUrl() != null)
                                                                sendUrlCheckRequest(distribution, applicationHostUrl, urlCheckEndpoint);
                                                        });

                                                } else {
                                                    LOG.error("Failed to upsert datasets: {}", upsertHandler.cause().getMessage());
                                                }
                                            } else {
                                                LOG.error("Failed to upsert datasets: {}", upsertHandler.cause().getMessage());
                                            }
                                        })));


                        // recursively fetch more datasets
                        if (remoteDatasets.size() == limit) {
                            handleDatasets(catalogue, limit, page + 1);
                        } else {
                            LOG.info("Completed fetching catalogue [{}]", catalogue.getInstanceId());
                        }
                    } else {
                        LOG.error("Failed to fetch datasets: {}", requestHandler.cause().getMessage());
                    }
                });
    }

    private Future<List<Violation>> getViolations(String datasetId) {
        Future<List<Violation>> completionFuture = Future.future();

        List<Violation> violations = new ArrayList<>();

        try {
            String query = "CONSTRUCT WHERE {" +
                    "graph <https://europeandataportal.eu/id/validation/" + datasetId +
                    "> {?s ?p ?o}}";

            String request = sparqlUrl
                    + "?default-graph-uri=&query="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&format=text/plain&timeout=0&debug=off&run=+Run+Query+";

            LOG.debug("SPARQL request: {}", request);

            webClient.getAbs(request)
                    .send(sparqlHandler -> {

                        if (sparqlHandler.succeeded()) {

                            String shacleReport = sparqlHandler.result().bodyAsString();

                            if (shacleReport != null && !shacleReport.contains("Empty NT")) {

                                try (InputStream rdfStream = new ByteArrayInputStream(shacleReport.getBytes())) {

                                    LOG.debug("SHACL Report for dataset with ID [{}] : {}", datasetId, shacleReport);

                                    ModelFactory.createDefaultModel()
                                            .read(rdfStream, null, "N-TRIPLE")
                                            .listSubjectsWithProperty(RDF.type, SHACL.ValidationResult)
                                            .forEachRemaining(validationResult -> {
                                                Violation violation = new Violation();
                                                violation.setName(validationResult.getProperty(SHACL.resultPath).getResource().getURI());
                                                violation.setType(validationResult.getProperty(SHACL.sourceConstraintComponent).getResource().getURI());
                                                violation.setInstance(validationResult.getProperty(SHACL.resultMessage).getLiteral().getString());
                                                violations.add(violation);
                                            });
                                } catch (Exception e) {
                                    LOG.error("Failed to read SHACL report: {}", e.getMessage());
                                }
                            }

                            // previous failure cases would not be alleviated by retrying
                            completionFuture.complete(violations);
                        } else {
                            completionFuture.fail(sparqlHandler.cause());
                        }

                    });
        } catch (Exception e) {
            completionFuture.fail(e);
        }

        return completionFuture;
    }

    private Future<List<String>> getKnownLicences() {

        Future<List<String>> completionFuture = Future.future();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("licences.ttl")) {
            if (is != null) {
                List<String> licences = new ArrayList<>();

                Property dcElementsIdentifier = ModelFactory.createDefaultModel().createProperty("http://purl.org/dc/elements/1.1/", "identifier");
                ModelFactory.createDefaultModel()
                        .read(is, null, "TTL")
                        .listObjectsOfProperty(dcElementsIdentifier)
                        .forEachRemaining(id -> licences.add(id.asLiteral().getString().toLowerCase()));

                completionFuture.complete(licences);

            } else {
                throw new IOException("Licence file InputStream is null");
            }
        } catch (IOException e) {
            completionFuture.fail(e);
        }

        return completionFuture;
    }

    private Future<Boolean> isCatalogueDcatAp(String catalogueId) {
        Future<Boolean> completionFuture = Future.future();

        webClient.getAbs("https://www.europeandataportal.eu/data/api/catalogues/" + catalogueId)
                .putHeader("Accept", "application/n-triples")
                .expect(ResponsePredicate.SC_OK)
                .send(catalogueHandler -> {

                    boolean isDcat = false;

                    if (catalogueHandler.succeeded()) {

                        String catalogueDetails = catalogueHandler.result().bodyAsString();

                        if (catalogueDetails != null) {
                            try (InputStream rdfStream = new ByteArrayInputStream(catalogueDetails.getBytes())) {

                                Model model = ModelFactory.createDefaultModel()
                                        .read(rdfStream, null, "N-TRIPLE");

                                isDcat = model.contains(null, DCTerms.type, model.createLiteral("dcat-ap"));

                            } catch (Exception e) {
                                LOG.error("Failed to read catalogue RDF: {}", e.getMessage());
                            }
                        }
                    } else {
                        LOG.error("Failed to fetch details for catalogue [{}] : {}", catalogueId, catalogueHandler.cause());
                    }

                    LOG.debug("Catalogue with ID [{}] is of type DCAT: {}", catalogueId, isDcat);
                    completionFuture.complete(isDcat);
                });

        return completionFuture;
    }

    private void sendUrlCheckRequest(Distribution distribution, String applicationHostUrl, String urlCheckEndpoint) {
        LOG.debug("Sending URL check request for distribution with ID [{}]", distribution.getInstanceId());

        List<String> urls = new ArrayList<>();
        urls.add(distribution.getAccessUrl());

        if (distribution.getDownloadUrl() != null
                && !distribution.getDownloadUrl().isEmpty()
                && !distribution.getDownloadUrl().equals(distribution.getAccessUrl())) {

            urls.add(distribution.getDownloadUrl());
        }

        String callback = StringUtils.removeEnd(applicationHostUrl, "/")
                + "/callback/" + distribution.getInstanceId();

        UrlCheckRequest request = new UrlCheckRequest();
        request.setUrls(urls);
        request.setCallback(callback);

        webClient.postAbs(urlCheckEndpoint)
                .expect(ResponsePredicate.SC_ACCEPTED)
                .sendJson(request, handler -> {
                    if (handler.failed())
                        LOG.error("Failed to send UrlCheckRequest for distribution [{}]: {}", distribution.getId(), handler.cause());
                });
    }

    private void cleanEntities(Message<String> message) {
        LOG.info("Deleting entities last changed before [{}] ...", message.body());
        databaseProvider.deleteOutdatedEntities(message.body(), handler -> {
            if (handler.succeeded()) {
                LOG.info("Deleting outdated entities done");
            } else {
                LOG.error("Failed to delete entities: {}", handler.cause().getMessage());
            }
        });
    }

    @Override
    public void stop(Future<Void> future) {
        databaseProvider.tearDown();
        future.complete();
    }

    private String addGeoFormatParams(String url, String format) {
        if (format == null || url == null)
            return url;

        // assumes that query params must not contain a '/'
        boolean hasQueryParams = url.contains("?")
                && url.lastIndexOf("/") < url.lastIndexOf("?");

        final String regexService = ".*([?&])service=.*";
        final String regexRequest = ".*([?&])request=.*";

        if (format.toLowerCase().contains("wms")) {
            if (!url.toLowerCase().matches(regexService))
                url += hasQueryParams ? "&service=WMS" :
                        (url.endsWith("?") ? "service=WMS" : "?service=WMS");

            if (!url.toLowerCase().matches(regexRequest))
                url += "&request=GetCapabilities";

            LOG.debug("WMS: {}", url);
        } else if (format.toLowerCase().contains("wfs")) {
            if (!url.toLowerCase().matches(regexService))
                url += hasQueryParams ? "&service=WFS" :
                        (url.endsWith("?") ? "service=WFS" : "?service=WFS");

            if (!url.toLowerCase().matches(regexRequest))
                url += "&request=GetCapabilities";

            LOG.debug("WFS: {}", url);
        } else if (format.toLowerCase().contains("wcs")) {
            if (!url.toLowerCase().matches(regexService))
                url += hasQueryParams ? "&service=WCS" :
                        (url.endsWith("?") ? "service=WCS" : "?service=WCS");

            if (!url.toLowerCase().matches(regexRequest))
                url += "&request=GetCapabilities";

            LOG.debug("WCS: {}", url);
        }

        return url;
    }

    private String getLanguageStringSave(JsonObject root, String defaultLanguage, String[] keys) {
        JsonObject lastNode = getKeyRecursively(root, keys);

        if (lastNode != null && lastNode.fieldNames().size() > 0) {
            JsonObject languageRoot = lastNode.getJsonObject(keys[keys.length - 1]);

            if (languageRoot != null && languageRoot.fieldNames() != null && languageRoot.fieldNames().toArray().length > 0) {
                // get language string in the following order:
                // catalogue spatial, english, first available key
                String languageKey = defaultLanguage != null
                        ? (languageRoot.fieldNames().contains(defaultLanguage.toLowerCase())
                        ? defaultLanguage
                        : (languageRoot.fieldNames().contains("en")
                        ? "en"
                        : (String) languageRoot.fieldNames().toArray()[0]))
                        : (String) languageRoot.fieldNames().toArray()[0];

                return languageRoot.getString(languageKey);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private String getKeySave(JsonObject root, String[] keys) {
        JsonObject lastNode = getKeyRecursively(root, keys);

        return lastNode != null
                ? lastNode.getString(keys[keys.length - 1])
                : null;
    }

    private String getKeySaveFromArray(JsonObject root, String[] keys) {
        JsonObject lastNode = getKeyRecursively(root, keys);

        if (lastNode != null) {
            JsonArray result = lastNode.getJsonArray(keys[keys.length - 1]);
            return result != null
                    ? (result.size() > 0 ? result.getString(0) : null)
                    : null;
        } else {
            return null;
        }
    }

    private JsonObject getKeyRecursively(JsonObject root, String[] keys) {

        if (root == null)
            return null;

        if (keys.length == 1)
            return root;

        return getKeyRecursively(root.getJsonObject(keys[0]), Arrays.copyOfRange(keys, 1, keys.length));
    }
}
