package org.masonapps.autoapp.database;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by Bob on 11/27/2015.
 */
public class AutoDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "auto_database.db";
    private static final int DATABASE_VERSION = 1;
    private static final String COMMA_SEP = ", ";
    private static final String SPACE = " ";
    private static final String CREATE_TABLE_SQL = "CREATE TABLE " + AutoEntryColumns.TABLE_NAME + " ("
            + AutoEntryColumns._ID + " INTEGER PRIMARY KEY,"
            + AutoEntryColumns.COLUMN_YEAR + SPACE + AutoEntryColumns.TYPE_INTEGER + COMMA_SEP
            + AutoEntryColumns.COLUMN_MAKE + SPACE + AutoEntryColumns.TYPE_TEXT + COMMA_SEP
            + AutoEntryColumns.COLUMN_MODEL + SPACE + AutoEntryColumns.TYPE_TEXT + COMMA_SEP
            + AutoEntryColumns.COLUMN_TRIM + SPACE + AutoEntryColumns.TYPE_TEXT + COMMA_SEP
            + AutoEntryColumns.COLUMN_DTC_ERROR + SPACE + AutoEntryColumns.TYPE_TEXT + COMMA_SEP
            + AutoEntryColumns.COLUMN_DTC_WARNING + SPACE + AutoEntryColumns.TYPE_TEXT + COMMA_SEP
            + AutoEntryColumns.COLUMN_DTC_CLEARED + SPACE + AutoEntryColumns.TYPE_TEXT + COMMA_SEP
            + AutoEntryColumns.COLUMN_MODEL_ID + SPACE + AutoEntryColumns.TYPE_INTEGER
            + ");";
    private static final String DELETE_TABLE_SQL = "DROP TABLE IF EXISTS " + AutoEntryColumns.TABLE_NAME;

    public AutoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public AutoDatabaseHelper(Context context, DatabaseErrorHandler errorHandler) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DELETE_TABLE_SQL);
        onCreate(db);
    }

    public static class AutoEntryColumns implements BaseColumns {
        public static final String TABLE_NAME = "vehicle_table";
        public static final String TYPE_INTEGER = "INTEGER";
        public static final String TYPE_TEXT = "TEXT";
        public static final String COLUMN_YEAR = "year";
        public static final String COLUMN_MAKE = "make";
        public static final String COLUMN_MODEL = "model";
        public static final String COLUMN_DTC_ERROR = "dtc_error";
        public static final String COLUMN_DTC_WARNING = "dtc_warning";
        public static final String COLUMN_DTC_CLEARED = "dtc_cleared";
        public static final String COLUMN_MODEL_ID = "model_id";
        public static final String COLUMN_TRIM = "model_trim";
        public static final String[] ALL_COLUMNS = {AutoEntryColumns._ID, AutoEntryColumns.COLUMN_YEAR, AutoEntryColumns.COLUMN_MAKE, AutoEntryColumns.COLUMN_MODEL, AutoEntryColumns.COLUMN_TRIM, AutoEntryColumns.COLUMN_MODEL_ID, AutoEntryColumns.COLUMN_DTC_ERROR, AutoEntryColumns.COLUMN_DTC_WARNING, AutoEntryColumns.COLUMN_DTC_CLEARED};
    }
}
