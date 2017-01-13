package ru.neverdark.csm.db;

class Schema {
    static class Tables {
        static final String CREATE_GPSLOG = "CREATE TABLE `gpslog` (" +
                "`_id`INTEGER PRIMARY KEY AUTOINCREMENT," +
                "`train_id`INTEGER NOT NULL," +
                "`latitude`REAL," +
                "`longitude`REAL," +
                "`altitude`REAL," +
                "`speed`REAL," +
                "`timestamp`INTEGER," +
                "FOREIGN KEY(train_id) REFERENCES summary(_id) ON DELETE CASCADE" +
                ")";

        static final String CREATE_SUMMARY = "CREATE TABLE `summary` (" +
                "`_id`INTEGER PRIMARY KEY AUTOINCREMENT," +
                "`finish_date`INTEGER," +
                "`description`TEXT," +
                "`is_commited`INTEGER NOT NULL DEFAULT '0'," +
                "`distance`INTEGER," +
                "`total_time`TEXT," +
                "`average_speed`REAL," +
                "`max_speed`REAL," +
                "`up_distance`INTEGER," +
                "`down_distance`INTEGER," +
                "`max_altitude`INTEGER," +
                "`up_altitude` INTEGER," +
                "`down_altitude` INTEGER" +
                ")";

        static final String DROP_GPSLOG = "DROP TABLE IF EXISTS `gpslog`";
        static final String DROP_SUMMARY = "DROP TABLE IF EXISTS `summary`";
    }

    static class Indices {
        static final String CREATE_TRAIN_ID_IDX = "CREATE INDEX `train_id_idx` ON `gpslog` (`train_id` ASC)";
        static final String DROP_TRAIN_ID_IDX = "DROP INDEX `train_id_idx`";
    }
}
