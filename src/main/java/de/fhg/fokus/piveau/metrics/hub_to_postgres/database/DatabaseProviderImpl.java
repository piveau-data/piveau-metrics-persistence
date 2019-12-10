package de.fhg.fokus.piveau.metrics.hub_to_postgres.database;

import de.fhg.fokus.piveau.metrics.hub_to_postgres.model.Catalogue;
import de.fhg.fokus.piveau.metrics.hub_to_postgres.model.Dataset;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.fhg.fokus.piveau.metrics.hub_to_postgres.ApplicationConfig.*;

public class DatabaseProviderImpl implements DatabaseProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseProvider.class);

    private JDBCClient dbClient;

    public DatabaseProviderImpl(Vertx vertx) {
        JsonObject env = vertx.getOrCreateContext().config();

        JsonObject config = new JsonObject()
                .put("url", env.getString(ENV_PGSQL_SERVER_HOST, DEFAULT_PGSQL_SERVER_HOST))
                .put("driver_class", "org.postgresql.Driver")
                .put("user", env.getString(ENV_PGSQL_USERNAME, DEFAULT_PGSQL_USERNAME))
                .put("password", env.getString(ENV_PGSQL_PASSWORD, DEFAULT_PGSQL_PASSWORD))
                .put("max_pool_size", 30);

        LOG.debug("Postgres config: {}", config);

        dbClient = JDBCClient.createShared(vertx, config);
    }

    @Override
    public void tearDown() {
        dbClient.close();
    }

    @Override
    public void upsertCatalogue(Catalogue catalogue, Handler<AsyncResult<Long>> resultHandler) {
        dbClient.updateWithParams(PostgresQueries.UPSERT_CATALOGUES, catalogueToSqlParams(catalogue), catalogueUpsertHandler -> {
            if (catalogueUpsertHandler.succeeded()) {
                Long catalogueId = catalogueUpsertHandler.result().getKeys().getLong(0);
                resultHandler.handle(Future.succeededFuture(catalogueId));
            } else {
                resultHandler.handle(Future.failedFuture("Failed to upsert catalogues: " + catalogueUpsertHandler.cause()));
            }
        });
    }

    @Override
    public void upsertDataset(Long catalogueId, Dataset dataset, Handler<AsyncResult<Void>> resultHandler) {
        Future<Dataset> upsertDatasetFuture = Future.future();

        dbClient.updateWithParams(PostgresQueries.UPSERT_DATASETS, datasetToSqlParams(dataset, catalogueId), datasetUpsertHandler -> {
            if (datasetUpsertHandler.succeeded()) {
                dataset.setId(datasetUpsertHandler.result().getKeys().getLong(0));
                LOG.debug("Upserted dataset with ID [{}]", dataset.getId());
                upsertDatasetFuture.complete(dataset);
            } else {
                LOG.error("Failed to upsert dataset {}: {}", dataset.getId(), datasetToSqlParams(dataset, catalogueId), datasetUpsertHandler.cause());
                upsertDatasetFuture.fail(datasetUpsertHandler.cause());
            }
        });

        upsertDatasetFuture.compose(upsertedDataset -> {

            Future<Dataset> deleteDistributionsFuture = Future.future();

            dbClient.updateWithParams(PostgresQueries.DELETE_DISTRIBUTIONS, new JsonArray().add(upsertedDataset.getId()), deletionHandler -> {
                if (deletionHandler.succeeded()) {
                    LOG.debug("Deleted distributions for dataset with ID [{}]", upsertedDataset.getId());
                    deleteDistributionsFuture.complete(upsertedDataset);
                } else {
                    LOG.error("Failed to delete distributions for dataset with ID [{}] : {}", upsertedDataset.getId(), deletionHandler.cause());
                    deleteDistributionsFuture.fail("Failed to delete distributions for dataset with ID [" + upsertedDataset.getId() + "] : " + deletionHandler.cause());
                }
            });

            return deleteDistributionsFuture;

        }).compose(upsertedDataset -> {

            Future<Dataset> deleteViolationsFuture = Future.future();

            dbClient.updateWithParams(PostgresQueries.DELETE_VIOLATIONS, new JsonArray().add(upsertedDataset.getId()), deletionHandler -> {
                if (deletionHandler.succeeded()) {
                    LOG.debug("Deleted violations for dataset with ID [{}]", upsertedDataset.getId());
                    deleteViolationsFuture.complete(upsertedDataset);
                } else {
                    LOG.error("Failed to delete violations for dataset with ID [{}] : {}", upsertedDataset.getId(), deletionHandler.cause());
                    deleteViolationsFuture.fail("Failed to delete violations for dataset with ID [" + upsertedDataset.getId() + "] : " + deletionHandler.cause());
                }
            });

            return deleteViolationsFuture;

        }).compose(upsertedDataset -> {

            Future<Dataset> insertDistributionsFuture = Future.future();

            if (dataset.getDistributions() != null && !dataset.getDistributions().isEmpty()) {
                dbClient.getConnection(connectionHandler -> {
                    if (connectionHandler.succeeded()) {
                        connectionHandler.result().batchWithParams(PostgresQueries.INSERT_DISTRIBUTIONS, batchDistributions(upsertedDataset), batchHandler -> {
                            if (batchHandler.succeeded()) {
                                LOG.debug("Inserted [{}] distributions for catalogue with ID [{}]", upsertedDataset.getDistributions().size(), catalogueId);
                                insertDistributionsFuture.complete(upsertedDataset);
                            } else {
                                LOG.error("Failed to insert [{}] distributions for catalogue with ID [{}]: {}", upsertedDataset.getDistributions().size(), catalogueId, batchHandler.cause());
                                insertDistributionsFuture.fail("Failed to insert distributions for catalogue with ID " + catalogueId);
                            }

                            connectionHandler.result().close();
                        });
                    } else {
                        LOG.error("Failed to acquire JDBC connection: {}", connectionHandler.cause().getMessage());
                        insertDistributionsFuture.fail("Failed to acquire JDBC connection: " + connectionHandler.cause());
                    }
                });
            } else {
                insertDistributionsFuture.complete(dataset);
            }

            return insertDistributionsFuture;

        }).compose(upsertedDataset -> {

            Future<Void> insertViolationsFuture = Future.future();

            if (dataset.getViolations() != null && !dataset.getViolations().isEmpty()) {
                dbClient.getConnection(connectionHandler -> {
                    if (connectionHandler.succeeded()) {
                        connectionHandler.result().batchWithParams(PostgresQueries.INSERT_VIOLATIONS, batchViolations(upsertedDataset), batchHandler -> {
                            if (batchHandler.succeeded()) {
                                LOG.debug("Inserted [{}] violations for catalogue with ID [{}]", upsertedDataset.getViolations().size(), catalogueId);
                                insertViolationsFuture.complete();
                            } else {
                                LOG.error("Failed to insert [{}] violations for catalogue with ID [{}]: {}", upsertedDataset.getViolations().size(), catalogueId, batchHandler.cause());
                                insertViolationsFuture.fail("Failed to insert [" + upsertedDataset.getViolations().size() + "] violations: " + batchHandler.cause());
                            }

                            connectionHandler.result().close();
                        });
                    } else {
                        LOG.error("Failed to acquire JDBC connection: {}", connectionHandler.cause().getMessage());
                        insertViolationsFuture.fail("Failed to acquire JDBC connection: " + connectionHandler.cause());
                    }
                });
            } else {
                insertViolationsFuture.complete();
            }

            return insertViolationsFuture;

        }).setHandler(processDataset -> {
            if (processDataset.succeeded()) {
                LOG.debug("Successfully processed dataset [{}]", dataset.toString());
                resultHandler.handle(Future.succeededFuture());
            } else {
                resultHandler.handle(Future.failedFuture("Failed to process " + dataset.getId() + ": " + processDataset.cause()));
            }
        });
    }

    @Override
    public void upsertKnownLicences(List<String> licences, Handler<AsyncResult<Void>> resultHandler) {
        dbClient.query(PostgresQueries.DELETE_LICENCES, deleteHandler -> {
            if (deleteHandler.succeeded()) {

                List<JsonArray> batchParams = licences.stream()
                        .map(licence ->
                                new JsonArray().add(LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()).add(licence))
                        .collect(Collectors.toList());

                dbClient.getConnection(connectionHandler -> {
                    if (connectionHandler.succeeded()) {
                        connectionHandler.result().batchWithParams(PostgresQueries.INSERT_LICENCES, batchParams, batchHandler -> {
                            if (batchHandler.succeeded()) {
                                LOG.info("Inserted [{}] licences", licences.size());
                                resultHandler.handle(Future.succeededFuture());
                            } else {
                                LOG.error("Failed to insert [{}] licences: {}", licences.size(), batchHandler.cause());
                                resultHandler.handle(Future.failedFuture(batchHandler.cause()));
                            }

                            connectionHandler.result().close();
                        });
                    } else {
                        LOG.error("Failed to acquire JDBC connection: {}", connectionHandler.cause().getMessage());
                        resultHandler.handle(Future.failedFuture("Failed to acquire JDBC connection: " + connectionHandler.cause()));
                    }
                });
            } else {
                LOG.error("Failed to delete licences: {}", deleteHandler.cause().getMessage());
                resultHandler.handle(Future.failedFuture(deleteHandler.cause()));
            }
        });
    }

    @Override
    public void deleteOutdatedEntities(String date, Handler<AsyncResult<Void>> resultHandler) {

        Future<Void> deleteViolationsFuture = Future.future();
        deleteEntity(PostgresQueries.DELETE_VIOLATIONS_WITH_TIMESTAMP, date, deleteViolationsFuture);

        deleteViolationsFuture.compose(deleteViolationsResult -> {
            Future<Void> deleteDistributionsFuture = Future.future();
            deleteEntity(PostgresQueries.DELETE_DISTRIBUTIONS_WITH_TIMESTAMP, date, deleteDistributionsFuture);
            return deleteDistributionsFuture;

        }).compose(deleteDistributionsResult -> {
            Future<Void> deleteDatasetsFuture = Future.future();
            deleteEntity(PostgresQueries.DELETE_DATASETS_WITH_TIMESTAMP, date, deleteDatasetsFuture);
            return deleteDatasetsFuture;

        }).compose(deleteDatasetsResult -> {
            Future<Void> deleteCataloguesFuture = Future.future();
            deleteEntity(PostgresQueries.DELETE_CATALOGUES_WITH_TIMESTAMP, date, deleteCataloguesFuture);
            return deleteCataloguesFuture;

        }).setHandler(deletionHandler -> {
            if (deletionHandler.succeeded()) {
                resultHandler.handle(Future.succeededFuture());
            } else {
                resultHandler.handle(Future.failedFuture(deletionHandler.cause()));
            }
        });
    }

    private void deleteEntity(String query, String date, Future<Void> future) {
        // FIXME prepared statements did not work with date in format yyyy-mm-dd
        // dbClient.updateWithParams(query, new JsonArray().add(date), deletionHandler -> {
        dbClient.query(query + "'" + date + "'", deletionHandler -> {
            if (deletionHandler.succeeded()) {
                future.handle(Future.succeededFuture());
            } else {
                future.handle(Future.failedFuture(deletionHandler.cause()));
            }
        });
    }

    private JsonArray catalogueToSqlParams(Catalogue catalogue) {
        JsonArray result = new JsonArray()
                .add(LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond());

        addSafe(catalogue.getInstanceId(), result);
        addSafe(catalogue.getName(), result);

        // title has non-null constraint in db
        result.add(catalogue.getTitle() != null
                ? catalogue.getTitle()
                : catalogue.getName());

        addSafe(catalogue.getSpatial(), result);
        addSafe(catalogue.getDcat(), result);

        return result;
    }

    private JsonArray datasetToSqlParams(Dataset dataset, long catalogueId) {
        JsonArray result = new JsonArray()
                .add(LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond());

        addSafe(dataset.getInstanceId(), result);
        addSafe(dataset.getLicenceId(), result);
        addSafe(dataset.getMachineReadable(), result);
        addSafe(dataset.getName(), result);
        addSafe(dataset.getTitle(), result);
        addSafe(catalogueId, result);

        return result;
    }

    private List<JsonArray> batchDistributions(Dataset dataset) {
        List<JsonArray> result = new ArrayList<>();

        dataset.getDistributions().forEach(distribution -> {
            JsonArray distParams = new JsonArray();

            addSafe(distribution.getAccessErrorMessage(), distParams);
            addSafe(distribution.getAccessUrl(), distParams);

            distParams.add(LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond());

            addSafe(distribution.getDownloadErrorMessage(), distParams);
            addSafe(distribution.getDownloadUrl(), distParams);
            addSafe(distribution.getFormat(), distParams);
            addSafe(distribution.getMachineReadable(), distParams);
            addSafe(distribution.getInstanceId(), distParams);
            addSafe(distribution.getStatusAccessUrl(), distParams);
            addSafe(distribution.getStatusDownloadUrl(), distParams);

            distParams.add(dataset.getId());

            result.add(distParams);
        });

        return result;
    }

    private List<JsonArray> batchViolations(Dataset dataset) {
        List<JsonArray> result = new ArrayList<>();

        dataset.getViolations().forEach(violation -> {
            JsonArray violationParams = new JsonArray();
            violationParams.add(LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond());

            addSafe(violation.getInstance(), violationParams);
            addSafe(violation.getName(), violationParams);
            addSafe(violation.getType(), violationParams);

            violationParams.add(dataset.getId());

            result.add(violationParams);
        });

        return result;
    }

    private void addSafe(Object object, JsonArray jsonArray) {
        if (object != null) {
            jsonArray.add(object);
        } else {
            jsonArray.addNull();
        }
    }
}
