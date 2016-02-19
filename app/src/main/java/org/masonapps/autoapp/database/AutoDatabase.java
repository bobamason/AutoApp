package org.masonapps.autoapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import org.masonapps.autoapp.database.AutoDatabaseHelper.AutoEntryColumns;

import java.util.ArrayList;

/**
 * Created by Bob on 11/27/2015.
 */
public class AutoDatabase {

    private AutoDatabaseHelper dbHelper;

    public AutoDatabase(Context context) {
        dbHelper = new AutoDatabaseHelper(context);
    }

    public void close() {
        dbHelper.close();
    }

    public AutoEntry insertEntry(AutoEntry entry){
        ContentValues cv = new ContentValues();
        cv.put(AutoEntryColumns.COLUMN_YEAR, entry.year);
        cv.put(AutoEntryColumns.COLUMN_MAKE, entry.make);
        cv.put(AutoEntryColumns.COLUMN_MODEL, entry.model);
        cv.put(AutoEntryColumns.COLUMN_MODEL_ID, entry.modelId);
        cv.put(AutoEntryColumns.COLUMN_TRIM, entry.trim);
        cv.put(AutoEntryColumns.COLUMN_DTC_ERROR, entry.getDtcErrors());
        cv.put(AutoEntryColumns.COLUMN_DTC_WARNING, entry.getDtcWarnings());
        cv.put(AutoEntryColumns.COLUMN_DTC_CLEARED, entry.getDtcsCleared());
        entry.id = dbHelper.getWritableDatabase().insertOrThrow(AutoEntryColumns.TABLE_NAME, null, cv);
        return entry;
    }

    public void updateEntry(AutoEntry entry) {
        ContentValues cv = new ContentValues();
        cv.put(AutoEntryColumns.COLUMN_YEAR, entry.year);
        cv.put(AutoEntryColumns.COLUMN_MAKE, entry.make);
        cv.put(AutoEntryColumns.COLUMN_MODEL, entry.model);
        cv.put(AutoEntryColumns.COLUMN_MODEL_ID, entry.modelId);
        cv.put(AutoEntryColumns.COLUMN_TRIM, entry.trim);
        cv.put(AutoEntryColumns.COLUMN_DTC_ERROR, entry.getDtcErrors());
        cv.put(AutoEntryColumns.COLUMN_DTC_WARNING, entry.getDtcWarnings());
        cv.put(AutoEntryColumns.COLUMN_DTC_CLEARED, entry.getDtcsCleared());
        dbHelper.getWritableDatabase().update(AutoEntryColumns.TABLE_NAME, cv, AutoEntryColumns._ID + " LIKE ?", new String[]{String.valueOf(entry.id)});
    }

    public void deleteEntry(AutoEntry entry) {
        dbHelper.getWritableDatabase().delete(AutoEntryColumns.TABLE_NAME, AutoEntryColumns._ID + " LIKE ?", new String[]{String.valueOf(entry.id)});
    }

    public ArrayList<AutoEntry> getAllEntries() {
        final ArrayList<AutoEntry> entries = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().query(AutoEntryColumns.TABLE_NAME, AutoEntryColumns.ALL_COLUMNS, null, null, null, null, AutoEntryColumns._ID + " DESC");
        if(cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                final AutoEntry entry = new AutoEntry();
                entry.id = cursor.getLong(cursor.getColumnIndexOrThrow(AutoEntryColumns._ID));
                entry.year = cursor.getInt(cursor.getColumnIndexOrThrow(AutoEntryColumns.COLUMN_YEAR));
                entry.make = cursor.getString(cursor.getColumnIndexOrThrow(AutoEntryColumns.COLUMN_MAKE));
                entry.model = cursor.getString(cursor.getColumnIndexOrThrow(AutoEntryColumns.COLUMN_MODEL));
                entry.setDTCsError(cursor.getString(cursor.getColumnIndexOrThrow(AutoEntryColumns.COLUMN_DTC_ERROR)));
                entry.setDTCsWarning(cursor.getString(cursor.getColumnIndexOrThrow(AutoEntryColumns.COLUMN_DTC_WARNING)));
                entry.setDTCsCleared(cursor.getString(cursor.getColumnIndexOrThrow(AutoEntryColumns.COLUMN_DTC_CLEARED)));
                entry.trim = cursor.getString(cursor.getColumnIndexOrThrow(AutoEntryColumns.COLUMN_TRIM));
                entry.modelId = cursor.getInt(cursor.getColumnIndexOrThrow(AutoEntryColumns.COLUMN_MODEL_ID));
                entries.add(entry);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return entries;
    }
}
