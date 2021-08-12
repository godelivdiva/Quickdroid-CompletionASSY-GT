package com.quick.completionassygt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ProcessActivity extends AppCompatActivity {

    ManagerSessionUserOracle session;
    TextView tv_job, tv_item, tv_desc, tv_subInv, tv_locName, tv_qtyAvail, tv_uom;
    TextInputEditText et_qty;
    Spinner sp_status, sp_locator;
    Button btn_completion, btn_proses;
    Connection mConn;
    String mQuery, jobId, orgId, itemId, subInv,
            userId, person, username;
    int maxSeqNum;
    private String array_status[];
    List<String> list_loc;
    SweetAlertDialog sweetAlertDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);
        new ModuleTool().allowNetworkOnMainThread();
        session = new ManagerSessionUserOracle(this);
        mConn = session.connectDb();

        if (session.getSID().equals("DEV")) {
            setTitle("Completion [ DEV ]");
        } else {
            setTitle("Completion [ PROD ]");
        }

        HashMap<String, String> userData = session.getUserData();
        userId = userData.get(ManagerSessionUserOracle.KEY_USEID);
        person = userData.get(ManagerSessionUserOracle.KEY_PERSON);

        Log.e("userId", userId);

        tv_job = (TextView) findViewById(R.id.tv_job);
        tv_item = (TextView) findViewById(R.id.tv_item);
        tv_desc = (TextView) findViewById(R.id.tv_desc);
        tv_subInv = (TextView) findViewById(R.id.tv_subInv);
//        tv_locName = (TextView) findViewById(R.id.tv_locName);
        tv_qtyAvail = (TextView) findViewById(R.id.tv_qtyAvail);
        tv_uom = (TextView) findViewById(R.id.tv_uom);
        et_qty = (TextInputEditText) findViewById(R.id.et_qty);
        btn_completion = (Button) findViewById(R.id.btn_completion);
        btn_proses = (Button) findViewById(R.id.btn_proses);
        sp_status = (Spinner) findViewById(R.id.sp_status);
        sp_locator = (Spinner) findViewById(R.id.sp_locator);

        setData();
        setStatus();
        list_loc = new ArrayList<String>();

        sp_status.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i==0){
//                    Toast.makeText(MainActivity.this, "silahkan pilih from subinventory", Toast.LENGTH_SHORT).show();
                } else {
                    if(i==1){ //pilih OK
                        tv_subInv.setText(subInv);
                        list_loc.clear();
                        getLoc(tv_subInv.getText().toString());
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                ProcessActivity.this, android.R.layout.simple_spinner_dropdown_item, list_loc);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        sp_locator.setAdapter(adapter);
                    } else if(i==2){//pilih repair
                        tv_subInv.setText("REJECT-DM");
                        list_loc.clear();
                        getLoc(tv_subInv.getText().toString());
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                ProcessActivity.this, android.R.layout.simple_spinner_dropdown_item, list_loc);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        sp_locator.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btn_completion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_completion.setVisibility(View.GONE);
                btn_proses.setVisibility(View.VISIBLE);
                if(sp_status.getSelectedItemPosition()==0){
                    //snackbar belum pilih status
                    btn_completion.setVisibility(View.VISIBLE);
                    btn_proses.setVisibility(View.GONE);
                    Snackbar.make(getWindow().getDecorView().getRootView(), "Status belom dipilih", Snackbar.LENGTH_LONG)
                            .show();
                } else if(et_qty.length()==0||et_qty.getText().toString().equals("0")){
                    //snackbar isi qty
                    btn_completion.setVisibility(View.VISIBLE);
                    btn_proses.setVisibility(View.GONE);
                    Snackbar.make(getWindow().getDecorView().getRootView(), "Isi quantity terlebih dahulu", Snackbar.LENGTH_LONG)
                            .show();
                } else if(Integer.parseInt(et_qty.getText().toString())>Integer.parseInt(tv_qtyAvail.getText().toString())){
                    //snackbar qty melebihi
                    btn_completion.setVisibility(View.VISIBLE);
                    btn_proses.setVisibility(View.GONE);
                    Snackbar.make(getWindow().getDecorView().getRootView(), "Qty input tidak boleh melebihi qty avail", Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    if(isNoLocator(tv_subInv.getText().toString())){
                        processCompletion();
                    } else {
                        if(sp_locator.getSelectedItemPosition()==0){
                            Snackbar.make(getWindow().getDecorView().getRootView(), "Pilih locator terlebih dahulu", Snackbar.LENGTH_LONG)
                                    .show();
                            btn_completion.setVisibility(View.VISIBLE);
                            btn_proses.setVisibility(View.GONE);
                        } else {
                            processCompletion();
                        }
                    }
                }
            }
        });
    }

    public boolean isNoLocator(String subinv) {
        Integer ver = 0;
        ResultSet theResultSet;
        try {
            Statement statement = mConn.createStatement();
            String mQuery = "select COUNT(*)\n" +
                    "                    from mtl_item_locations mil\n" +
                    "                    where mil.SUBINVENTORY_CODE = '"+subinv+"'\n" +
                    "                    and mil.DISABLE_DATE is null\n" +
                    "                    and mil.DESCRIPTION is not null\n" +
                    "                    --and mil.ORGANIZATION_ID = 102";
            theResultSet = statement.executeQuery(mQuery);
            Log.d("Cek isNoLocator :",mQuery);
            System.out.println(" "+theResultSet+" ");
            if (theResultSet.next()) {
                ver = Integer.parseInt(theResultSet.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (ver==0){
            return true;}
        else {
            return false;}
    }

    void processCompletion() {
        sweetAlertDialog = new SweetAlertDialog(ProcessActivity.this, SweetAlertDialog.PROGRESS_TYPE);
        sweetAlertDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        sweetAlertDialog.setTitleText("\nLoading Data. . .");
        sweetAlertDialog.setCancelable(false);
        sweetAlertDialog.show();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                insertToInterface();
            }
        }, 1000);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runInterfaceProcesor();
            }
        }, 1500);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                alertSuccess();
            }
        }, 3000);
    }

    public void alertSuccess() {
        sweetAlertDialog.dismissWithAnimation();
        sweetAlertDialog = new SweetAlertDialog(ProcessActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Success")
                .setContentText("Proses completion berhasil!")
                .setConfirmText("OK")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        Intent i = new Intent(getApplicationContext(), CompletionActivity.class);
                        startActivity(i);
                        sDialog.dismissWithAnimation();
                    }
                });
        sweetAlertDialog.show();
        sweetAlertDialog.setCanceledOnTouchOutside(false);
    }

    void getLoc(String sub) {
        list_loc.add("");
        Connection dbConnection = null;
        Statement stateuy = null;
        ResultSet theResultSet;
        String Query = "";
        dbConnection = mConn;
        try {
            stateuy = dbConnection.createStatement();
            Query = "select mil.INVENTORY_LOCATION_ID, mil.SEGMENT1\n" +
                    "from mtl_item_locations mil, mtl_parameters mp\n" +
                    "where mil.SUBINVENTORY_CODE = '" + sub + "' --sesuai subinv yang di pilih\n" +
                    "and mil.organization_id = mp.organization_id\n" +
                    "and mil.DISABLE_DATE is null\n" +
                    "order by 1 asc";
            theResultSet = stateuy.executeQuery(Query);
            while (theResultSet.next()) { //looping
                list_loc.add(theResultSet.getString(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void setData(){
        String jobName = getIntent().getStringExtra("jobName");
        String qtyAvail = getIntent().getStringExtra("qtyAvail");

        maxSeqNum = getMaxSeqNum(jobName);
        try {
            Statement statement = mConn.createStatement();
            ResultSet result;
            //get itemName, jobId, orgId, orgCode, itemId, itemName, desc, uom, subinv, locatorId, locatorName
            mQuery = "SELECT we.wip_entity_name, we.wip_entity_id, \n" +
                    "mp.organization_id, mp.organization_code, \n" +
                    "we.primary_item_id, msib.segment1, \n" +
                    "msib.description, msib.primary_uom_code,\n" +
                    "wdj.completion_subinventory, wdj.completion_locator_id,\n" +
                    "mil.segment1\n" +
                    "FROM wip_entities we,\n" +
                    "wip_discrete_jobs wdj,\n" +
                    "wip_operations wo,\n" +
                    "mtl_system_items_b msib,\n" +
                    "mtl_parameters mp,\n" +
                    "mtl_item_locations mil\n" +
                    "WHERE we.primary_item_id = msib.inventory_item_id\n" +
                    "AND we.wip_entity_id = wdj.wip_entity_id\n" +
                    "AND wo.wip_entity_id = wdj.wip_entity_id\n" +
                    "AND msib.organization_id = we.organization_id\n" +
                    "AND we.organization_id = mp.organization_id\n" +
                    "AND wdj.completion_locator_id = mil.INVENTORY_LOCATION_ID(+)\n" +
                    "AND wo.operation_seq_num = "+maxSeqNum+"\n" +
                    "AND wdj.status_type = 3\n" +
                    "AND we.wip_entity_name = '"+jobName+"'";
            Log.d("Query",mQuery);
            result= statement.executeQuery(mQuery);
            if(result.next()){
//                job      = result.getString(1);
                jobId    = result.getString(2);
                orgId    = result.getString(3);
//                orgCode  = result.getString(4);
                itemId   = result.getString(5);
//                itemName = result.getString(6);
//                desc     = result.getString(7);
//                uom      = result.getString(8);
                subInv   = result.getString(9);
//                locId    = result.getString(10);
//                locName  = result.getString(11);

                tv_job.setText(result.getString(1));
                tv_item.setText(result.getString(6));
                tv_desc.setText(result.getString(7));
//                tv_locName.setText(locName);
                tv_qtyAvail.setText(qtyAvail);
                et_qty.setText(qtyAvail);
                tv_uom.setText(result.getString(8));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setStatus() {
        array_status = new String[3];
        array_status[0] = "";
        array_status[1] = "OK";
        array_status[2] = "REPAIR";

        ArrayAdapter adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, array_status);
        sp_status.setAdapter(adapter);
    }
    int getMaxSeqNum(String jobName){
        try{
            Statement statement = mConn.createStatement();
            mQuery = "SELECT max(wo.operation_seq_num) \n" +
                    "FROM wip_entities we, wip_operations wo\n" +
                    "WHERE we.wip_entity_id = wo.wip_entity_id\n" +
                    "AND we.wip_entity_name = '"+jobName+"'";
            ResultSet result = statement.executeQuery(mQuery);
            if (result.next()){
                return result.getInt(1);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    void insertToInterface(){
        String uom = tv_uom.getText().toString();
        String qty = et_qty.getText().toString();
        String subInv = tv_subInv.getText().toString();
        String locator = getLocatorId(sp_locator.getSelectedItem().toString()); //getlocname
        String trasactionInterfaceId = null;
        try {
            Statement statement = mConn.createStatement();
            ResultSet result;

            //get trasactionInterfaceId
            mQuery = "SELECT MTL_MATERIAL_TRANSACTIONS_S.NEXTVAL FROM dual";
            result = statement.executeQuery(mQuery);
            if (result.next()){
                trasactionInterfaceId = result.getString(1);
            }

            mQuery = "INSERT INTO mtl_transactions_interface \n" +
                    "( source_code, \n" +
                    "source_line_id, \n" +
                    "source_header_id, \n" +
                    "process_flag, \n" +
                    "validation_required, \n" +
                    "transaction_mode, \n" +
                    "lock_flag, \n" +
                    "last_update_date, \n" +
                    "last_updated_by, \n" +
                    "creation_date, \n" +
                    "created_by, \n" +
                    "inventory_item_id, \n" +
                    "organization_id, \n" +
                    "transaction_quantity, \n" +
                    "primary_quantity, \n" +
                    "transaction_uom, \n" +
                    "transaction_date, \n" +
                    "subinventory_code, \n" +
                    "locator_id,\n" +
                    "transaction_source_id, \n" +
                    "transaction_source_type_id, \n" +
                    "transaction_action_id, \n" +
                    "transaction_type_id, \n" +
                    "transaction_reference, \n" +
                    "wip_entity_type, \n" +
                    "operation_seq_num, \n" +
                    "bom_revision_date, \n" +
                    "routing_revision_date, \n" +
                    "scheduled_flag, \n" +
                    "Flow_schedule,\n" +
                    "FINAL_COMPLETION_FLAG,\n" +
                    "TRANSACTION_INTERFACE_ID\n" +
                    ") \n" +
                    "VALUES \n" +
                    "( \n" +
                    "'"+person+"', --source code \n" + //COMPLETION
                    "1, -- source line id \n" +
                    "-1, -- source header id \n" +
                    "1, -- process flag \n" +
                    "1, -- validation required \n" +
                    "3, -- transaction mode \n" +
                    "2, -- lock flag \n" +
                    "sysdate, -- last update date \n" +
                    userId+", -- last updated by \n" +
                    "sysdate, -- creation date \n" +
                    userId+", -- created by \n" +
                    itemId +", -- inventory item id \n" +
                    orgId+", -- org id \n" +
                    qty+", -- transaction quantity \n" +
                    qty+", -- primary quantity \n" +
                    "'"+uom+"', -- transaction uom \n" +
                    "sysdate, -- transaction date \n" +
                    "'"+subInv+"', -- subinventory code\n" +
                    "'"+locator+"', -- locator_id \n" +
                    ""+jobId+", -- transaction source id \n" +
                    "5, -- transaction source type id \n" +
                    "31, -- transaction action id \n" +
                    "44, -- transaction type id \n" +
                    "'ANDROID', -- transaction reference \n" + //testing
                    "1, -- wip entity type \n" +
                    "2, -- operation seq num \n" +
                    "sysdate, -- bom revision date \n" +
                    "sysdate, -- routing revision date \n" +
                    "2, -- scheduled flag \n" +
                    "'N',\n" +
                    "'N',\n" +
                    trasactionInterfaceId+"\n" +
                    ")";
            Log.e("mQuery 1 ", mQuery);
            statement.executeUpdate(mQuery);


            for(int i = 10; i<= maxSeqNum; i+=10){
                mQuery = "INSERT INTO cst_comp_snap_interface \n" +
                        "(transaction_interface_id, \n" +
                        "wip_entity_id, \n" +
                        "operation_seq_num, \n" +
                        "last_update_date, \n" +
                        "last_updated_by, \n" +
                        "creation_date, \n" +
                        "created_by, \n" +
                        "quantity_completed, \n" +
                        "primary_quantity) \n" +
                        "VALUES \n" +
                        "( \n" +
                        trasactionInterfaceId+", \n" +
                        jobId+", --Job id \n" +
                        ""+ i +",--Operatoin Seq No \n" +
                        "sysdate, --Last update date \n" +
                        userId+", --Last updated by \n" +
                        "sysdate, --Creation date \n" +
                        userId+", --Created by \n" +
                        qty+", --Quantity Completed \n" +
                        qty+" --Priamry Quantity \n" +
                        ")";
                Log.e("mQuery 2 ", mQuery);
                statement.executeUpdate(mQuery);
                statement.executeUpdate("COMMIT");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getLocatorId(String loc) {
        String data = "";
        java.sql.Connection dbConnection = null;
        Statement statement = null;
        ResultSet theResultSet;


        try {
            statement = mConn.createStatement();

            mQuery = "select mil.INVENTORY_LOCATION_ID\n" +
                    "from mtl_item_locations mil, mtl_parameters mp\n" +
                    "where mil.SEGMENT1 = '" + loc + "' --locator name\n" +
                    "and mil.organization_id = mp.organization_id\n" +
                    "and mil.DISABLE_DATE is null\n" +
                    "order by 1 asc";

            Log.d("locator id", mQuery);
            ResultSet result = statement.executeQuery(mQuery);
            if (result.next()) {
                data = result.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return data;

    }

    public void runInterfaceProcesor(){
        java.sql.Connection dbConnection;
        try {
            dbConnection = mConn;

            //TODO nggak jalan

            mQuery = "BEGIN Fnd_Global.apps_initialize(5177,50630,20003); :x := apps.Fnd_Request.submit_request('INV','INCTCM','Interface Inventory Transactions'); END;";
            Log.d("API_log",mQuery);
            CallableStatement funcnone = dbConnection
                    .prepareCall(mQuery);

            funcnone.registerOutParameter(1, Types.INTEGER);
            funcnone.executeUpdate();
            int returnVal = funcnone.getInt(1);
            System.out.println("Return value is : " +returnVal );
            funcnone.close();

            if(returnVal>0){
//                finish();
//                Toast.makeText(this, "Completion Processed", Toast.LENGTH_SHORT).show();
                Snackbar.make(getWindow().getDecorView().getRootView(), "Completion Processed", Snackbar.LENGTH_LONG)
                        .show();
            }else{
//                Toast.makeText(this, "Interface processor tidak berjalan.", Toast.LENGTH_SHORT).show();
                Snackbar.make(getWindow().getDecorView().getRootView(), "Interface processor tidak berjalan", Snackbar.LENGTH_LONG)
                        .show();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error :" + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), CompletionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
