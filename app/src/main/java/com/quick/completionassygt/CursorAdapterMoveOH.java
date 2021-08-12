package com.quick.completionassygt;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.sql.Connection;

public class CursorAdapterMoveOH extends android.widget.CursorAdapter {
    Connection mConn;
    dbHelp helper;
    Connection mConPostgre;
    ManagerSessionUserOracle session;

    public CursorAdapterMoveOH(Context context, Cursor cursor) {
        super(context, cursor, 0);
        mConn = new ManagerSessionUserOracle(context).connectDb();
        helper = new dbHelp(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        //Inflate view
        return LayoutInflater.from(context).inflate(R.layout.row_list_moveoh, viewGroup, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        //set data
        CardView cv_job = (CardView) view.findViewById(R.id.cv_job);
        TextView tv_job = (TextView) view.findViewById(R.id.tv_job);
        TextView tv_tgl = (TextView) view.findViewById(R.id.tv_tgl);
        TextView tv_item = (TextView) view.findViewById(R.id.tv_item);
        TextView tv_desc = (TextView) view.findViewById(R.id.tv_desc);
        TextView tv_completion = (TextView) view.findViewById(R.id.tv_completion);

        String job_name = cursor.getString(cursor.getColumnIndexOrThrow("JOB_NAME"));
        String completion_date = cursor.getString(cursor.getColumnIndexOrThrow("COMPLETION_DATE"));
        String segment1 = cursor.getString(cursor.getColumnIndexOrThrow("SEGMENT1"));
        String description = cursor.getString(cursor.getColumnIndexOrThrow("DESCRIPTION"));
        String qty = cursor.getString(cursor.getColumnIndexOrThrow("QTY"));

        tv_job.setText(job_name);
        tv_tgl.setText(completion_date);
        tv_item.setText(segment1);
        tv_desc.setText(description);
        tv_completion.setText(qty);


    }
}
