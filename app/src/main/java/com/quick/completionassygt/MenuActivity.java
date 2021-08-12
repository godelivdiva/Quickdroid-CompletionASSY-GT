package com.quick.completionassygt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.sql.Connection;

public class MenuActivity extends AppCompatActivity {

    ManagerSessionUserOracle session;
    Connection mConn;
    BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    LinearLayout ll_infoBottom;
    ImageButton ib_infoCompletion, ib_infoMoveOH, ib_closeInfo;
    MaterialTextView mt_titleInfo, mt_descInfo;
    MaterialButton mb_menuCompletion, mb_menuMoveOH;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        new ModuleTool().allowNetworkOnMainThread();
        session = new ManagerSessionUserOracle(this);
        mConn = session.connectDb();

        ib_infoCompletion = findViewById(R.id.ib_infoCompletion);
        mb_menuCompletion = findViewById(R.id.mb_menuCompletion);
        ib_infoMoveOH = findViewById(R.id.ib_infoMoveOH);
        mb_menuMoveOH = findViewById(R.id.mb_menuMoveOH);
        ib_closeInfo = findViewById(R.id.ib_closeInfo);
        mt_titleInfo = findViewById(R.id.mt_titleInfo);
        mt_descInfo = findViewById(R.id.mt_descInfo);
        ll_infoBottom = findViewById(R.id.ll_infoBottom);

        bottomSheetBehavior = BottomSheetBehavior.from(ll_infoBottom);

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        ib_infoCompletion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else {
                    mt_titleInfo.setText("Information");
                    mt_descInfo.setText("Menu ini digunakan untuk melakukan completion job.");
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });

        ib_infoMoveOH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else {
                    mt_titleInfo.setText("Information");
                    mt_descInfo.setText("Menu ini digunakan untuk memindahkan onhand dari REJECT-DM ke INT-ASSYGT.");
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });

        ib_closeInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        mb_menuCompletion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else {
                    Intent i = new Intent(getApplicationContext(), CompletionActivity.class);
                    startActivity(i);
                }
            }
        });

        mb_menuMoveOH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else {
                    Intent i = new Intent(getApplicationContext(), MoveOHActivity.class);
                    startActivity(i);
                }
            }
        });

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                logout();
                break;
        }
        return true;
    }

    public void logout() {
        new MaterialAlertDialogBuilder(MenuActivity.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle("Perhatian!")
                .setMessage("Apakah anda yakin ingin logout? ")
                .setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        session.logoutUser();
                        finish();
                    }
                })
                .setNegativeButton("Tidak", null)
                .create().show();
    }

    public void closeapk() {
        new MaterialAlertDialogBuilder(MenuActivity.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle("Perhatian!")
                .setMessage("Anda yakin ingin keluar aplikasi? ")
                .setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        moveTaskToBack(true);
                    }
                })
                .setNegativeButton("Tidak", null)
                .create().show();
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        closeapk();
    }
}
