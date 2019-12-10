package de.fhg.fokus.piveau.metrics.hub_to_postgres.database;

// FIXME prepared statements did not work with date in format yyyy-mm-dd
final class PostgresQueries {

    static final String DELETE_CATALOGUES_WITH_TIMESTAMP = "DELETE FROM catalog WHERE db_update::date < date ";

    static final String DELETE_DATASETS_WITH_TIMESTAMP = "DELETE FROM dataset WHERE db_update::date < date ";

    static final String DELETE_DISTRIBUTIONS = "DELETE FROM distribution WHERE distribution_id = ?";
    static final String DELETE_DISTRIBUTIONS_WITH_TIMESTAMP = "DELETE FROM distribution WHERE db_update::date < date ";

    static final String DELETE_VIOLATIONS = "DELETE FROM violation WHERE violation_id = ?";
    static final String DELETE_VIOLATIONS_WITH_TIMESTAMP = "DELETE FROM violation WHERE db_update::date < date ";

    static final String DELETE_LICENCES = "DELETE FROM licence";



    static final String UPSERT_CATALOGUES = "INSERT INTO catalog (db_update, instance_id, name, title, spatial, dcat) " +
            "VALUES (to_timestamp(?), ?, ?, ?, ?, ?) ON CONFLICT (instance_id) DO UPDATE " +
            "SET db_update = excluded.db_update, name = excluded.name, title = excluded.title, spatial = excluded.spatial, dcat = excluded.dcat";

    static final String UPSERT_DATASETS = "INSERT INTO dataset (db_update, instance_id, licence_id, machine_readable, name, title, dataset_id) " +
            "VALUES (to_timestamp(?), ?, ?, ?, ?, ?, ?) ON CONFLICT (instance_id) DO UPDATE " +
            "SET db_update = excluded.db_update, licence_id = excluded.licence_id, machine_readable = excluded.machine_readable, name = excluded.name, title = excluded.title";


    static final String INSERT_DISTRIBUTIONS = "INSERT INTO distribution (access_error_message, access_url, db_update, download_error_message, download_url, format, machine_readable, instance_id, status_access_url, status_download_url, distribution_id) " +
            "VALUES (?, ?, to_timestamp(?), ?, ?, ?, ?, ?, ?, ?, ?)";

    static final String INSERT_VIOLATIONS = "INSERT INTO violation (db_update, violation_instance, violation_name, violation_type, violation_id) " +
            "VALUES (to_timestamp(?), ?, ?, ?, ?)";

    static final String INSERT_LICENCES = "INSERT INTO licence (db_update, licence_id) " +
            "VALUES (to_timestamp(?), ?)";
}
