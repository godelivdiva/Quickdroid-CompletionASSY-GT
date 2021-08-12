package com.quick.completionassygt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.stetho.Stetho;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.client.android.CaptureActivity;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MoveOHActivity extends AppCompatActivity {

    ImageView iv_scan;
    TextInputEditText et_code;
    Connection mConn;
    ManagerSessionUserOracle session;
    SweetAlertDialog sweetAlertDialog;
    ListView lv_job;
    String mQuery, person;
    CursorAdapterMoveOH adapter;
    dbHelp helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_oh);
        new ModuleTool().allowNetworkOnMainThread();
        Stetho.initializeWithDefaults(this);

        session = new ManagerSessionUserOracle(this);
        mConn = session.connectDb();
        helper = new dbHelp(this);
        helper.delete();

        if (session.getSID().equals("DEV")) {
            setTitle("Move OnHand [ DEV ]");
        } else {
            setTitle("Move OnHand [ PROD ]");
        }

        HashMap<String, String> userData = session.getUserData();
        person = userData.get(ManagerSessionUserOracle.KEY_PERSON);
        adapter = new CursorAdapterMoveOH(this, helper.select());

        iv_scan = findViewById(R.id.iv_scan);
        et_code = findViewById(R.id.et_code);
        lv_job = findViewById(R.id.lv_job);

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
                                String serial = code[1];
                                helper.delete();
                                insertDb(item_id);
                                showMessage();
                            } else if (isAnyMsib(et_code.getText().toString())) {
                                String item_id = getInvItemId(et_code.getText().toString());
                                helper.delete();
                                insertDb(item_id);
                                showMessage();
                            } else {
                                helper.delete();
                                insertDbJob(et_code.getText().toString());
                                showMessage();
                            }

                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });

        lv_job.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Cursor c = helper.select();
                    c.moveToPosition(position);
                    String job_name = c.getString(c.getColumnIndexOrThrow("JOB_NAME"));
                    confirmMoveOH(job_name);
            }

        });
    }

    void insertDb(String item_id) {
        Log.d("Conn", "." + mConn);
        if (mConn == null) {
            Toast.makeText(this, "No Connection", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Statement statement = mConn.createStatement();
            mQuery = "select distinct\n" +
                    "       we.WIP_ENTITY_NAME\n" +
                    "      ,we.WIP_ENTITY_ID\n" +
                    "      ,msib.SEGMENT1 assembly\n" +
                    "      ,msib.DESCRIPTION\n" +
                    "      ,msib.INVENTORY_ITEM_ID \n" +
                    "      ,wdj.completion_subinventory\n" +
                    "      ,mmt.TRANSACTION_QUANTITY \n" +
                    "      ,trunc (mmt.TRANSACTION_DATE) completion_date\n" +
                    "from wip_entities we\n" +
                    "    ,wip_discrete_jobs wdj\n" +
                    "    ,mtl_txn_request_headers mtrh\n" +
                    "    ,mtl_system_items_b msib\n" +
                    "    ,mtl_txn_request_lines mtrl\n" +
                    "    ,mtl_material_transactions mmt\n" +
                    "where we.WIP_ENTITY_ID = mtrh.ATTRIBUTE1\n" +
                    "  and we.WIP_ENTITY_ID = wdj.WIP_ENTITY_ID\n" +
                    "  and mtrh.HEADER_ID = mtrl.HEADER_ID\n" +
                    "  and mmt.TRANSACTION_SOURCE_ID = we.WIP_ENTITY_ID\n" +
                    "  --\n" +
                    "  and msib.INVENTORY_ITEM_ID = wdj.PRIMARY_ITEM_ID\n" +
                    "  and msib.ORGANIZATION_ID = wdj.ORGANIZATION_ID\n" +
                    "  --\n" +
                    "  and mmt.SUBINVENTORY_CODE = 'REJECT-DM'\n" +
                    "  and mmt.TRANSACTION_TYPE_ID = 44 \n" +
                    "  and wdj.STATUS_TYPE not in (7)\n" +
                    "  and wdj.PRIMARY_ITEM_ID = "+item_id+"\n" +
                    "  AND we.WIP_ENTITY_NAME NOT IN (select DISTINCT we.WIP_ENTITY_NAME  \n" +
                    "from mtl_txn_request_headers mtrh\n" +
                    "    ,mtl_txn_request_lines mtrl\n" +
                    "    ,mtl_system_items_b msib\n" +
                    "    ,wip_entities we\n" +
                    "    ,wip_discrete_jobs wdj\n" +
                    "    --\n" +
                    "    ,mtl_material_transactions mmt\n" +
                    "where mtrh.HEADER_ID = mtrl.HEADER_ID\n" +
                    "  and we.WIP_ENTITY_ID = mtrh.ATTRIBUTE1\n" +
                    "  and we.WIP_ENTITY_ID = wdj.WIP_ENTITY_ID\n" +
                    "  and msib.INVENTORY_ITEM_ID = wdj.PRIMARY_ITEM_ID\n" +
                    "  and msib.ORGANIZATION_ID = wdj.ORGANIZATION_ID\n" +
                    "  --\n" +
                    "  and mmt.TRANSACTION_TYPE_ID = 2\n" +
                    "  and mmt.TRANSACTION_SOURCE_ID = we.WIP_ENTITY_ID\n" +
                    "  )";
            Log.d("QUERY", mQuery);
            ResultSet result = statement.executeQuery(mQuery);
            while (result.next()) {
                ContentValues values = new ContentValues();
                values.put("JOB_NAME", result.getString(1));
                values.put("JOB_ID", result.getString(2));
                values.put("SEGMENT1", result.getString(3));
                values.put("DESCRIPTION", result.getString(4));
                values.put("INVENTORY_ITEM_ID", result.getString(5));
                values.put("COMPLETION_SUBINV", result.getString(6));
                values.put("QTY", result.getString(7));
                values.put("COMPLETION_DATE", result.getString(8));
                Log.d("VALUES", values.toString());
                helper.insert(values);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void insertDbJob(String job) {
        Log.d("Conn", "." + mConn);
        if (mConn == null) {
            Toast.makeText(this, "No Connection", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Statement statement = mConn.createStatement();
            mQuery = "select distinct\n" +
                    "       we.WIP_ENTITY_NAME\n" +
                    "      ,we.WIP_ENTITY_ID\n" +
                    "      ,msib.SEGMENT1 assembly\n" +
                    "      ,msib.DESCRIPTION\n" +
                    "      ,msib.INVENTORY_ITEM_ID \n" +
                    "      ,wdj.completion_subinventory\n" +
                    "      ,mmt.TRANSACTION_QUANTITY \n" +
                    "      ,trunc (mmt.TRANSACTION_DATE) completion_date\n" +
                    "from wip_entities we\n" +
                    "    ,wip_discrete_jobs wdj\n" +
                    "    ,mtl_txn_request_headers mtrh\n" +
                    "    ,mtl_system_items_b msib\n" +
                    "    ,mtl_txn_request_lines mtrl\n" +
                    "    ,mtl_material_transactions mmt\n" +
                    "where we.WIP_ENTITY_ID = mtrh.ATTRIBUTE1\n" +
                    "  and we.WIP_ENTITY_ID = wdj.WIP_ENTITY_ID\n" +
                    "  and mtrh.HEADER_ID = mtrl.HEADER_ID\n" +
                    "  and mmt.TRANSACTION_SOURCE_ID = we.WIP_ENTITY_ID\n" +
                    "  --\n" +
                    "  and msib.INVENTORY_ITEM_ID = wdj.PRIMARY_ITEM_ID\n" +
                    "  and msib.ORGANIZATION_ID = wdj.ORGANIZATION_ID\n" +
                    "  --\n" +
                    "  and mmt.SUBINVENTORY_CODE = 'REJECT-DM'\n" +
                    "  and mmt.TRANSACTION_TYPE_ID = 44 \n" +
                    "  and wdj.STATUS_TYPE not in (7)\n" +
                    "  AND we.WIP_ENTITY_NAME = '"+job+"'\n" +
                    "  AND we.WIP_ENTITY_NAME NOT IN (select DISTINCT we.WIP_ENTITY_NAME  \n" +
                    "from mtl_txn_request_headers mtrh\n" +
                    "    ,mtl_txn_request_lines mtrl\n" +
                    "    ,mtl_system_items_b msib\n" +
                    "    ,wip_entities we\n" +
                    "    ,wip_discrete_jobs wdj\n" +
                    "    --\n" +
                    "    ,mtl_material_transactions mmt\n" +
                    "where mtrh.HEADER_ID = mtrl.HEADER_ID\n" +
                    "  and we.WIP_ENTITY_ID = mtrh.ATTRIBUTE1\n" +
                    "  and we.WIP_ENTITY_ID = wdj.WIP_ENTITY_ID\n" +
                    "  and msib.INVENTORY_ITEM_ID = wdj.PRIMARY_ITEM_ID\n" +
                    "  and msib.ORGANIZATION_ID = wdj.ORGANIZATION_ID\n" +
                    "  --\n" +
                    "  and mmt.TRANSACTION_TYPE_ID = 2\n" +
                    "  and mmt.TRANSACTION_SOURCE_ID = we.WIP_ENTITY_ID\n" +
                    "  )";
            Log.d("QUERY", mQuery);
            ResultSet result = statement.executeQuery(mQuery);
            while (result.next()) {
                ContentValues values = new ContentValues();
                values.put("JOB_NAME", result.getString(1));
                values.put("JOB_ID", result.getString(2));
                values.put("SEGMENT1", result.getString(3));
                values.put("DESCRIPTION", result.getString(4));
                values.put("INVENTORY_ITEM_ID", result.getString(5));
                values.put("COMPLETION_SUBINV", result.getString(6));
                values.put("QTY", result.getString(7));
                values.put("COMPLETION_DATE", result.getString(8));
                Log.d("VALUES", values.toString());
                helper.insert(values);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void showMessage() {
        Cursor c = helper.select();
        adapter = new CursorAdapterMoveOH(this, c);
        lv_job.setAdapter(adapter);
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
                    String serial = code[1];
                    helper.delete();
                    insertDb(item_id);
                    showMessage();
                } else if (isAnyMsib(scanContent)) { //scan item
                    String item_id = getInvItemId(scanContent);
                    helper.delete();
                    insertDb(item_id);
                    showMessage();
                } else { //kemungkinan scan no job langsung
                    helper.delete();
                    insertDbJob(scanContent);
                    showMessage();
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "Canceled", Toast.LENGTH_LONG).show();
        }
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

    public void insertMTI(String job) {
        Statement stmt = null;
        Connection conn = null;
        try {
            conn = mConn;
            stmt = conn.createStatement();
            System.out
                    .println("insert into MTL_TRANSACTIONS_INTERFACE (\n" +
                            "ORGANIZATION_ID,\n" +
                            "SUBINVENTORY_CODE,\n" +
                            "LOCATOR_ID,\n" +
                            "TRANSFER_ORGANIZATION,\n" +
                            "TRANSFER_SUBINVENTORY,\n" +
                            "TRANSFER_LOCATOR,\n" +
                            "SHIPMENT_NUMBER,\n" +
                            "ITEM_SEGMENT1,\n" +
                            "TRANSACTION_QUANTITY,\n" +
                            "TRANSACTION_UOM,\n" +
                            "TRANSACTION_DATE,\n" +
                            "TRANSACTION_SOURCE_ID,\n" +
                            "TRANSACTION_SOURCE_TYPE_ID,\n" +
                            "TRANSACTION_ACTION_ID,\n" +
                            "TRANSACTION_TYPE_ID,\n" +
                            "SOURCE_CODE,\n" +
                            "TRANSACTION_MODE,\n" +
                            "LOCK_FLAG,\n" +
                            "SOURCE_HEADER_ID,\n" +
                            "SOURCE_LINE_ID,\n" +
                            "PROCESS_FLAG,\n" +
                            "LAST_UPDATE_DATE,\n" +
                            "LAST_UPDATED_BY,\n" +
                            "CREATION_DATE,\n" +
                            "CREATED_BY\n" +
                            "--TRANSACTION_REFERENCE\n" +
                            ")\n" +
                            "select distinct \n" +
                            "mmt.ORGANIZATION_ID --' organization_id\n" +
                            ",mmt.SUBINVENTORY_CODE --' subinventory_code\n" +
                            ",mmt.LOCATOR_ID --' locator_id\n" +
                            ",wdj.ORGANIZATION_ID --' trf_organization\n" +
                            ",wdj.COMPLETION_SUBINVENTORY --' trf_subinventory \n" +
                            ",wdj.COMPLETION_LOCATOR_ID --' trf_locator\n" +
                            ",NULL --' shipment_number\n" +
                            ",msib.SEGMENT1 --' item_segment1\n" +
                            ",mmt.TRANSACTION_QUANTITY --' transact_qty \n" +
                            ",mmt.TRANSACTION_UOM --' trx_uom\n" +
                            ",sysdate --' trx_date\n" +
                            ",we.WIP_ENTITY_ID --' trx_source_id\n" +
                            ",13 --' trx_source_type_id \n" +
                            ",2 --' trx_action_id\n" +
                            ",2 --' trx_type_id\n" +
                            ",'"+person+"' --' source_code\n" +
                            ",3 --' trx_mode\n" +
                            ",2 --' lock_flag\n" +
                            ",'-1' --' source_header_id \n" +
                            ",'-1' --' source_line_id\n" +
                            ",1 --' process_flag\n" +
                            ",sysdate --' last_udpate_date\n" +
                            ",'-1' --' last_udpated_by\n" +
                            ",sysdate --' creation_date\n" +
                            ",'-1' --' created_by\n" +
                            "from mtl_txn_request_headers mtrh\n" +
                            "    ,mtl_txn_request_lines mtrl\n" +
                            "    ,mtl_system_items_b msib\n" +
                            "    ,wip_entities we\n" +
                            "    ,wip_discrete_jobs wdj\n" +
                            "    --\n" +
                            "    ,mtl_material_transactions mmt\n" +
                            "where mtrh.HEADER_ID = mtrl.HEADER_ID\n" +
                            "  and we.WIP_ENTITY_ID = mtrh.ATTRIBUTE1\n" +
                            "  and we.WIP_ENTITY_ID = wdj.WIP_ENTITY_ID\n" +
                            "  and msib.INVENTORY_ITEM_ID = wdj.PRIMARY_ITEM_ID\n" +
                            "  and msib.ORGANIZATION_ID = wdj.ORGANIZATION_ID\n" +
                            "  --\n" +
                            "  and mmt.TRANSACTION_TYPE_ID = 44\n" +
                            "  and mmt.TRANSACTION_SOURCE_ID = we.WIP_ENTITY_ID\n" +
                            "  --\n" +
                            "  and we.WIP_ENTITY_NAME = '"+job+"'");

            stmt.executeUpdate("insert into MTL_TRANSACTIONS_INTERFACE (\n" +
                            "ORGANIZATION_ID,\n" +
                            "SUBINVENTORY_CODE,\n" +
                            "LOCATOR_ID,\n" +
                            "TRANSFER_ORGANIZATION,\n" +
                            "TRANSFER_SUBINVENTORY,\n" +
                            "TRANSFER_LOCATOR,\n" +
                            "SHIPMENT_NUMBER,\n" +
                            "ITEM_SEGMENT1,\n" +
                            "TRANSACTION_QUANTITY,\n" +
                            "TRANSACTION_UOM,\n" +
                            "TRANSACTION_DATE,\n" +
                            "TRANSACTION_SOURCE_ID,\n" +
                            "TRANSACTION_SOURCE_TYPE_ID,\n" +
                            "TRANSACTION_ACTION_ID,\n" +
                            "TRANSACTION_TYPE_ID,\n" +
                            "SOURCE_CODE,\n" +
                            "TRANSACTION_MODE,\n" +
                            "LOCK_FLAG,\n" +
                            "SOURCE_HEADER_ID,\n" +
                            "SOURCE_LINE_ID,\n" +
                            "PROCESS_FLAG,\n" +
                            "LAST_UPDATE_DATE,\n" +
                            "LAST_UPDATED_BY,\n" +
                            "CREATION_DATE,\n" +
                            "CREATED_BY\n" +
                            "--TRANSACTION_REFERENCE\n" +
                            ")\n" +
                            "select distinct \n" +
                            "mmt.ORGANIZATION_ID --' organization_id\n" +
                            ",mmt.SUBINVENTORY_CODE --' subinventory_code\n" +
                            ",mmt.LOCATOR_ID --' locator_id\n" +
                            ",wdj.ORGANIZATION_ID --' trf_organization\n" +
                            ",wdj.COMPLETION_SUBINVENTORY --' trf_subinventory \n" +
                            ",wdj.COMPLETION_LOCATOR_ID --' trf_locator\n" +
                            ",NULL --' shipment_number\n" +
                            ",msib.SEGMENT1 --' item_segment1\n" +
                            ",mmt.TRANSACTION_QUANTITY --' transact_qty \n" +
                            ",mmt.TRANSACTION_UOM --' trx_uom\n" +
                            ",sysdate --' trx_date\n" +
                            ",we.WIP_ENTITY_ID --' trx_source_id\n" +
                            ",13 --' trx_source_type_id \n" +
                            ",2 --' trx_action_id\n" +
                            ",2 --' trx_type_id\n" +
                            ",'"+person+"' --' source_code\n" +
                            ",3 --' trx_mode\n" +
                            ",2 --' lock_flag\n" +
                            ",'-1' --' source_header_id \n" +
                            ",'-1' --' source_line_id\n" +
                            ",1 --' process_flag\n" +
                            ",sysdate --' last_udpate_date\n" +
                            ",'-1' --' last_udpated_by\n" +
                            ",sysdate --' creation_date\n" +
                            ",'-1' --' created_by\n" +
                            "from mtl_txn_request_headers mtrh\n" +
                            "    ,mtl_txn_request_lines mtrl\n" +
                            "    ,mtl_system_items_b msib\n" +
                            "    ,wip_entities we\n" +
                            "    ,wip_discrete_jobs wdj\n" +
                            "    --\n" +
                            "    ,mtl_material_transactions mmt\n" +
                            "where mtrh.HEADER_ID = mtrl.HEADER_ID\n" +
                            "  and we.WIP_ENTITY_ID = mtrh.ATTRIBUTE1\n" +
                            "  and we.WIP_ENTITY_ID = wdj.WIP_ENTITY_ID\n" +
                            "  and msib.INVENTORY_ITEM_ID = wdj.PRIMARY_ITEM_ID\n" +
                            "  and msib.ORGANIZATION_ID = wdj.ORGANIZATION_ID\n" +
                            "  --\n" +
                            "  and mmt.TRANSACTION_TYPE_ID = 44\n" +
                            "  and mmt.TRANSACTION_SOURCE_ID = we.WIP_ENTITY_ID\n" +
                            "  --\n" +
                            "  and we.WIP_ENTITY_NAME = '"+job+"'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void commit() {
        try {
            Statement statement = mConn.createStatement();
            mQuery = "COMMIT";
            statement.executeUpdate(mQuery);
            Log.d("proses", "Commit " + mQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void runPlSql(){
        Connection dbConnection;
        try {
            dbConnection = mConn;

            mQuery = "BEGIN Fnd_Global.apps_initialize(5177,50630,20003); :x := apps.Fnd_Request.submit_request('INV','INCTCM','Interface Inventory Transactions'); END;";
            Log.d("API_log",mQuery);
            CallableStatement funcnone = dbConnection
                    .prepareCall(mQuery);

            funcnone.registerOutParameter(1, Types.INTEGER);
            funcnone.executeUpdate();
            int returnVal = funcnone.getInt(1);
            System.out.println("Return value is : " +returnVal );
            funcnone.close();

        } catch (SQLException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error :" + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void confirmMoveOH(final String jobname) {
        new MaterialAlertDialogBuilder(MoveOHActivity.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle("Perhatian!")
                .setMessage("Apakah anda yakin ingin memindah onhand ke Internal? ")
                .setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        prosesMoveOH(jobname);
                    }
                })
                .setNegativeButton("Tidak", null)
                .setCancelable(false)
                .create().show();
    }

    void prosesMoveOH(final String jobname) {
        sweetAlertDialog = new SweetAlertDialog(MoveOHActivity.this, SweetAlertDialog.PROGRESS_TYPE);
        sweetAlertDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        sweetAlertDialog.setTitleText("\nLoading Data. . .");
        sweetAlertDialog.setCancelable(false);
        sweetAlertDialog.show();
        final Handler handler = new Handler();

//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                insertMTI(jobname);
//                commit();
//            }
//        }, 1000);
//
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                runPlSql();
//                commit();
//            }
//        }, 2000);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runApi(jobname, person);
                commit();
            }
        }, 1000);


        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sweetAlertDialog.dismissWithAnimation();
                alertSuccess();
            }
        }, 3000);
    }

    void runApi(String job, String no_induk) {
        Connection dbConnection;
        Integer userId = 0;
        Statement statement;
        ResultSet theResultSet;
        try {
            dbConnection = mConn;
            statement = dbConnection.createStatement();


            System.out.println("BEGIN\n" +
                    "KHSAPICOMPLETIONJOB.subinv_transfer_completion('"+job+"','"+no_induk+"');\n" +
                    "END;");

            //API issue
            CallableStatement funcnone = dbConnection
                    .prepareCall("BEGIN\n" +
                    "KHSAPICOMPLETIONJOB.subinv_transfer_completion('"+job+"','"+no_induk+"');\n" +
                    "END;");
            funcnone.executeUpdate();
            funcnone.close();


        } catch (SQLException e) {

            Toast.makeText(getBaseContext(), "Error :" + e.toString(),
                    Toast.LENGTH_LONG).show();
            Log.d("ERROR:", e.toString());

        }
    }


    public void alertSuccess(){
        sweetAlertDialog = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Success")
                .setContentText("Proses berhasil!")
                .setConfirmText("OK")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        Intent i = new Intent(getApplicationContext(), MoveOHActivity.class);
                        startActivity(i);
                        sDialog.dismissWithAnimation();
                    }
                });
        sweetAlertDialog.show();
        sweetAlertDialog.setCanceledOnTouchOutside(false);
    }

    public void backMenu() {
        new MaterialAlertDialogBuilder(MoveOHActivity.this)
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
