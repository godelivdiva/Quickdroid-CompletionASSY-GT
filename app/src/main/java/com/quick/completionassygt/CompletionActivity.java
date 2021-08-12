package com.quick.completionassygt;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.stetho.Stetho;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.client.android.CaptureActivity;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class CompletionActivity extends AppCompatActivity implements RecyclerCompletion.ItemClickListener{

    ImageView iv_scan;
    TextInputEditText et_code;
    Connection mConn;
    ManagerSessionUserOracle session;
    SweetAlertDialog sweetAlertDialog;
    RecyclerView rv_job;
    String mQuery, seqNum = "10";
    String jobName, jobId;
    LinearLayoutManager layoutManager;
    RecyclerCompletion adapter;
    Boolean stateRunnable=false;
    String serial = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completion);
        new ModuleTool().allowNetworkOnMainThread();
        Stetho.initializeWithDefaults(this);

        session = new ManagerSessionUserOracle(this);
        mConn = session.connectDb();

        if (session.getSID().equals("DEV")) {
            setTitle("Completion [ DEV ]");
        } else {
            setTitle("Completion [ PROD ]");
        }

        iv_scan = findViewById(R.id.iv_scan);
        et_code = findViewById(R.id.et_code);
        rv_job = findViewById(R.id.rv_job);

        et_code.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (i) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            //if else isAny
                            if (et_code.getText().toString().contains(",")) {
                                String[] code = et_code.getText().toString().split(",");
                                String item_id = code[0];
                                serial = code[1];
                                jobName = getJobByItemId(item_id);
                            } else if (isAnyMsib(et_code.getText().toString())) {
                                String item_id = getInvItemId(et_code.getText().toString());
                                jobName = getJobByItemId(item_id);
                            } else {
                                jobName = cekJob(et_code.getText().toString());
                            }
                            if (jobName.equals("0")) {
                                if (jobType(jobName)) {
                                    Snackbar.make(getWindow().getDecorView().getRootView(), "Job Non-Standard", Snackbar.LENGTH_LONG)
                                            .show();
                                } else {
                                    Log.e("cek job", "data null");
                                    Snackbar.make(getWindow().getDecorView().getRootView(), "Data tidak tersedia", Snackbar.LENGTH_LONG)
                                            .show();
                                }
                            } else {
                                et_code.setText(jobName);
                                jobId = getJobId(jobName);
                                stateRunnable = isProcessed(jobId);
                                if(stateRunnable){
                                    createRecyclerView(jobName, true);
                                } else {
                                    createRecyclerView(jobName, false);
                                }
                                Log.e("cek job", "ada data");
                            }
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
    }

    public void scan(View view) {
        runtimePermission();
    }

    void runtimePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 26);
        } else {
            //normal
            Intent i = new Intent(this, CaptureActivity.class);
            i.putExtra("TITLE_SCAN", "Scan");
            i.putExtra("SAVE_HISTORY", false);
            i.setAction("com.google.zxing.client.android.SCAN");
            startActivityForResult(i, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 26 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Izinkan aplikasi mengakses kamera untuk melakukan SCANN", Toast.LENGTH_SHORT).show();
        } else {
            runtimePermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            final String scanContent = data.getStringExtra("SCAN_RESULT");
            Log.d("Scan", scanContent);
            if (scanContent != null) {
                if (scanContent.contains(",")) { //scan kartu body
                    String[] code = scanContent.split(",");
                    String item_id = code[0];
                    serial = code[1];
                    jobName = getJobByItemId(item_id);
                } else if (isAnyMsib(scanContent)) { //scan item
                    String item_id = getInvItemId(scanContent);
                    jobName = getJobByItemId(item_id);
                } else { //kemungkinan scan no job langsung
                    jobName = cekJob(scanContent);
                }
                if (jobName.equals("0")) {
                    if (jobType(jobName)) {
                        Snackbar.make(getWindow().getDecorView().getRootView(), "Job Non-Standard", Snackbar.LENGTH_LONG)
                                .show();
                    } else {
                        Log.e("cek job", "data null");
                        Snackbar.make(getWindow().getDecorView().getRootView(), "Data tidak tersedia", Snackbar.LENGTH_LONG)
                                .show();
                    }
                } else {
                    et_code.setText(jobName);
                    jobId = getJobId(jobName);
                    stateRunnable = isProcessed(jobId);
                    if(stateRunnable){
                        createRecyclerView(jobName, true);
                    } else {
                        createRecyclerView(jobName, false);
                    }
                    Log.e("cek job", "ada data");
                }
            }

        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "Canceled", Toast.LENGTH_LONG).show();
        }
    }

    String translateSerial(String itemId, String serial){
        try{
            Statement statement = mConn.createStatement();
            mQuery = "SELECT serial_format \n" +
                    "FROM khs_serial_code\n" +
                    "WHERE item_id = "+itemId;
            ResultSet result = statement.executeQuery(mQuery);
            if (result.next()){
                String format =  result.getString(1);
                return format.replace("-",serial);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return "";
    }

    public boolean jobType(String job) {
        Boolean ver;
        ResultSet theResultSet;
        try {
            Statement statement = mConn.createStatement();
            String mQuery = "select count(*)\n" +
                    "from wip_operations wo\n" +
                    "    ,wip_entities we\n" +
                    "where wo.WIP_ENTITY_ID = we.WIP_ENTITY_ID\n" +
                    "  and we.WIP_ENTITY_NAME = '" + job + "'";
            theResultSet = statement.executeQuery(mQuery);
            Log.e("jobType :", mQuery);
            System.out.println(" " + theResultSet + " ");
            ver = true;
        } catch (SQLException e) {
            e.printStackTrace();
            ver = false;
        }
        return ver;
    }

    public boolean isAnyMsib(String item) {
        Integer ver = 0;
        ResultSet theResultSet;
        try {
            Statement statement = mConn.createStatement();
            String mQuery = "SELECT count(DISTINCT msib.SEGMENT1)\n" +
                    "FROM mtl_system_items_b msib\n" +
                    "WHERE msib.SEGMENT1 = '" + item + "'";
            theResultSet = statement.executeQuery(mQuery);
            Log.e("isAnyMsib :", mQuery);
            System.out.println(" " + theResultSet + " ");
            if (theResultSet.next()) {
                ver = Integer.parseInt(theResultSet.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (ver > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isSerial(String item) {
        Integer ver = 0;
        ResultSet theResultSet;
        try {
            Statement statement = mConn.createStatement();
            String mQuery = "select count(*)\n" +
                    "from\n" +
                    "mtl_system_items_b msib\n" +
                    ",mtl_serial_numbers msn\n" +
                    "where\n" +
                    "msib.inventory_item_id = msn.INVENTORY_ITEM_ID\n" +
                    "and msib.SEGMENT1 = '"+item+"'";
            theResultSet = statement.executeQuery(mQuery);
            Log.e("isAnySerial :", mQuery);
            System.out.println(" " + theResultSet + " ");
            if (theResultSet.next()) {
                ver = Integer.parseInt(theResultSet.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (ver > 0) {
            return true;
        } else {
            return false;
        }
    }

    public String getJobByItemId(String item_id) {
        String data = "";
        Connection dbConnection = null;
        Statement statement = null;
        ResultSet theResultSet;

        dbConnection = mConn;
        try {
            statement = dbConnection.createStatement();
            mQuery = "SELECT NVL(min(we.wip_entity_name), 0) \n" +
                    "                    FROM wip_entities we,\n" +
                    "                    wip_discrete_jobs wdj,\n" +
                    "                    wip_operations wo,\n" +
                    "                    mtl_system_items_b msib\n" +
                    "                    WHERE we.primary_item_id = msib.inventory_item_id\n" +
                    "                    AND we.wip_entity_id = wdj.wip_entity_id\n" +
                    "                    AND wo.wip_entity_id = wdj.wip_entity_id\n" +
                    "                    AND msib.organization_id = we.organization_id\n" +
                    "                    AND wdj.status_type = 3\n" +
                    "                    and msib.INVENTORY_ITEM_ID = '" + item_id + "'\n" +
                    "                    ORDER BY wdj.scheduled_start_date";

            Log.e("getJob ItemId", mQuery);
            ResultSet result = statement.executeQuery(mQuery);
            if (result.next()) {
                data = result.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public String cekJob(String code) {
        String data = "";
        Connection dbConnection = null;
        Statement statement = null;
        ResultSet theResultSet;

        dbConnection = mConn;
        try {
            statement = dbConnection.createStatement();
            mQuery = "SELECT NVL(min(we.wip_entity_name), 0) \n" +
                    "                    FROM wip_entities we,\n" +
                    "                    wip_discrete_jobs wdj,\n" +
                    "                    wip_operations wo,\n" +
                    "                    mtl_system_items_b msib\n" +
                    "                    WHERE we.primary_item_id = msib.inventory_item_id\n" +
                    "                    AND we.wip_entity_id = wdj.wip_entity_id\n" +
                    "                    AND wo.wip_entity_id = wdj.wip_entity_id\n" +
                    "                    AND msib.organization_id = we.organization_id\n" +
                    "                    AND wdj.status_type = 3\n" +
                    "                    and we.wip_entity_name = '" + code + "'\n" +
                    "                    ORDER BY wdj.scheduled_start_date";

            Log.e("getJob ItemId", mQuery);
            ResultSet result = statement.executeQuery(mQuery);
            if (result.next()) {
                data = result.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public String getInvItemId(String code) {
        String data = "";
        Connection dbConnection = null;
        Statement statement = null;
        ResultSet theResultSet;

        dbConnection = mConn;
        try {
            statement = dbConnection.createStatement();
            mQuery = "SELECT DISTINCT msib.INVENTORY_ITEM_ID \n" +
                    "FROM mtl_system_items_b msib\n" +
                    "WHERE msib.SEGMENT1 = '" + code + "'";

            Log.e("get ItemId", mQuery);
            ResultSet result = statement.executeQuery(mQuery);
            if (result.next()) {
                data = result.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    boolean isProcessed(String jobId) {
        try {
            Statement statement = mConn.createStatement();
            mQuery = "SELECT * FROM mtl_transactions_interface \n" +
                    "WHERE TRANSACTION_SOURCE_ID = " + jobId + " \n" +
                    "AND PROCESS_FLAG=1";
            ResultSet result = statement.executeQuery(mQuery);
            Log.d("isProcessed", mQuery);
            if (result.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    String getJobId(String code){
        try{
            Statement statement = mConn.createStatement();
            mQuery = "SELECT we.wip_entity_name, we.wip_entity_id\n" +
                    "    FROM wip_entities we,\n" +
                    "    wip_discrete_jobs wdj,\n" +
                    "    wip_operations wo,\n" +
                    "    mtl_system_items_b msib\n" +
                    "    WHERE we.primary_item_id = msib.inventory_item_id\n" +
                    "    AND we.wip_entity_id = wdj.wip_entity_id\n" +
                    "    AND wo.wip_entity_id = wdj.wip_entity_id\n" +
                    "    AND msib.organization_id = we.organization_id\n" +
                    "    AND we.WIP_ENTITY_NAME = '"+code+"'\n" +
                    "    AND wo.operation_seq_num = \n" +
                    "        (SELECT max(wo2.OPERATION_SEQ_NUM) FROM WIP_OPERATIONS wo2\n" +
                    "        WHERE wo2.WIP_ENTITY_ID = we.WIP_ENTITY_ID)\n" +
                    "    AND wdj.status_type in (3,4)\n" +
                    "    ORDER BY wdj.scheduled_completion_date";

            Log.d("getjobid : ",mQuery);
            ResultSet result = statement.executeQuery(mQuery);
            if (result.next()){
                String jobId = result.getString(2);
                return jobId;
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return "";
    }

    void createRecyclerView(String code, Boolean state) {
        Log.d("Conn", "." + mConn);
        if (mConn == null) {
            Toast.makeText(this, "No Connection", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Statement statement = mConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            mQuery = "SELECT we.wip_entity_name, TO_CHAR(wdj.scheduled_completion_date,'dd MON YYYY  HH24:mi'), \n" +
                    "nvl(wo.QUANTITY_WAITING_TO_MOVE,0), wdj.QUANTITY_COMPLETED, wo.QUANTITY_WAITING_TO_MOVE available,\n" +
                    "CASE WHEN wdj.scheduled_completion_date < SYSDATE THEN 0\n" +
                    "ELSE 1 END status, we.wip_entity_id, msib.SEGMENT1 , msib.DESCRIPTION \n" +
                    "FROM wip_entities we,\n" +
                    "wip_discrete_jobs wdj,\n" +
                    "wip_operations wo,\n" +
                    "mtl_system_items_b msib\n" +
                    "WHERE we.primary_item_id = msib.inventory_item_id\n" +
                    "AND we.wip_entity_id = wdj.wip_entity_id\n" +
                    "AND wo.wip_entity_id = wdj.wip_entity_id\n" +
                    "AND msib.organization_id = we.organization_id\n" +
                    "AND we.WIP_ENTITY_NAME = '" + code + "'\n" +
                    "AND wo.operation_seq_num = \n" +
                    "(SELECT max(wo2.OPERATION_SEQ_NUM) FROM WIP_OPERATIONS wo2\n" +
                    "WHERE wo2.WIP_ENTITY_ID = we.WIP_ENTITY_ID)\n" +
                    "AND wdj.status_type in (3,4)\n" +
                    "ORDER BY wdj.scheduled_completion_date";
            Log.d("QUERY", mQuery);
            ResultSet result = statement.executeQuery(mQuery);
            if (result.next()) {
                layoutManager = new LinearLayoutManager(getApplicationContext());
                adapter = new RecyclerCompletion(getApplicationContext(), result, this, state);
                rv_job.setLayoutManager(layoutManager);
                rv_job.hasFixedSize();
                rv_job.setAdapter(adapter);
            } else {
                Snackbar.make(getWindow().getDecorView().getRootView(), "Data tidak tersedia", Snackbar.LENGTH_LONG)
                        .show();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void OnItemClick(String jobName, String jobId, String item, String qtyAvail, String qtyComplete) {
        if(isProcessed(jobId)){
            dialogProcces();
        }else {
            int complete = Integer.parseInt(qtyComplete);
            int move = Integer.parseInt(qtyAvail);
            if (move==0){
                dialogQty();
            }else {
                if(isSerial(item)){
                    if(serial.length()==0){
                        Snackbar.make(getWindow().getDecorView().getRootView(), "Item serial, silahkan ulang scan/input dengan kartu body", Snackbar.LENGTH_LONG)
                                .show();
                    } else {
                        Intent i = new Intent(this, ProcessSerialActivity.class);
                        i.putExtra("jobName",jobName);
                        i.putExtra("qtyAvail",qtyAvail);
                        i.putExtra("serial",translateSerial(getInvItemId(item), serial));
                        startActivity(i);
                    }

                } else {
                    Intent i = new Intent(this, ProcessActivity.class);
                    i.putExtra("jobName",jobName);
                    i.putExtra("qtyAvail",qtyAvail);
                    startActivity(i);
                }


            }
        }
    }
    void dialogProcces(){
        new MaterialAlertDialogBuilder(this)
                .setTitle("Alert!")
                .setMessage("Job ini sedang diproses!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }

    void dialogQty(){
        new MaterialAlertDialogBuilder(this)
                .setTitle("Alert!")
                .setMessage("Tidak ada qty yang bisa dicompletion!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }

    public void backMenu() {
        new MaterialAlertDialogBuilder(CompletionActivity.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle("Perhatian!")
                .setMessage("Anda yakin ingin kembali ke menu? ")
                .setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("Tidak", null)
                .create().show();
    }

    @Override
    public void onBackPressed() {
        backMenu();
    }
}
