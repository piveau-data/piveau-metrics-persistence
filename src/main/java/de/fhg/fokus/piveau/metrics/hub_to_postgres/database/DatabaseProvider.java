package de.fhg.fokus.piveau.metrics.hub_to_postgres.database;

import de.fhg.fokus.piveau.metrics.hub_to_postgres.model.Catalogue;
import de.fhg.fokus.piveau.metrics.hub_to_postgres.model.Dataset;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

public interface DatabaseProvider {

    void tearDown();

    void upsertCatalogue(Catalogue catalogue, Handler<AsyncResult<Long>> resultHandler);
    void upsertDataset(Long catalogueId, Dataset dataset, Handler<AsyncResult<Void>> resultHandler);
    void upsertKnownLicences(List<String> licences, Handler<AsyncResult<Void>> resultHandler);

    void deleteOutdatedEntities(String date, Handler<AsyncResult<Void>> resultHandler);
}
