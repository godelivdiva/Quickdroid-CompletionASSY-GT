package com.quick.completionassygt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class dbHelp extends SQLiteOpenHelper {
    static final String TABLE_MOVEOH = "tb_moveoh";
    public final String _id = "_id";
    public final String JOB_NAME = "JOB_NAME";
    public final String JOB_ID = "JOB_ID";
    public final String SEGMENT1 = "SEGMENT1";
    public final String DESCRIPTION = "DESCRIPTION";
    public final String INVENTORY_ITEM_ID = "INVENTORY_ITEM_ID";
    public final String COMPLETION_SUBINV = "COMPLETION_SUBINV";
    public final String QTY = "QTY";
    public final String COMPLETION_DATE = "COMPLETION_DATE";

    String mQuery;

    public dbHelp(Context context) {
        super(context, "db_assygt", null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        mQuery = "CREATE TABLE " + TABLE_MOVEOH + " (" +
                _id + " INTEGER PRIMARY KEY," +
                JOB_NAME + " TEXT," +
                JOB_ID + " TEXT," +
                SEGMENT1 + " TEXT," +
                DESCRIPTION + " TEXT," +
                INVENTORY_ITEM_ID + " TEXT," +
                COMPLETION_SUBINV + " TEXT," +
                QTY + " TEXT," +
                COMPLETION_DATE + " TEXT" +
                ")";
        db.execSQL(mQuery);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("NEW", "" + newVersion);
        Log.d("OLD", "" + oldVersion);
        if (newVersion > oldVersion) {
            db.execSQL("DROP TABLE " + TABLE_MOVEOH);
            onCreate(db);
        }
    }

    public void insert(ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("insert", "" + values.toString());
        db.insert("tb_moveoh", null, values);
    }

    public Cursor select() {
        SQLiteDatabase db = this.getWritableDatabase();
        mQuery = "SELECT * FROM tb_moveoh";
        Cursor c = db.rawQuery(mQuery, null);
        return c;
    }

    public void delete() {
        //SQLite Delete ndes
        SQLiteDatabase db = this.getWritableDatabase();
        mQuery = "DELETE FROM tb_moveoh";
        db.execSQL(mQuery);
    }

}
