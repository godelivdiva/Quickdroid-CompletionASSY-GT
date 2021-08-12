package com.quick.completionassygt;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.quick.completionassygt.Model.Login_Model;
import com.quick.completionassygt.Model.User_Model;
import com.quick.completionassygt.Rest.API_Client;
import com.quick.completionassygt.Rest.API_Link;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    TextInputLayout til_username, til_password;
    TextInputEditText et_username, et_password;
    RadioButton rb_dev, rb_prod;
    Button b_login;
    Koneksi konek;
    ManagerSessionUserOracle session;
    String cName, cPort, cSid, cUsername, cPassword, res = "0";
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        progressDialog = new ProgressDialog(this);
        new ModuleTool().allowNetworkOnMainThread();
        konek = new Koneksi();
        getSupportActionBar().hide();
        session = new ManagerSessionUserOracle(this);
        if (session.isUserLogin()) {
            Intent i = new Intent(LoginActivity.this, MenuActivity.class);
            startActivity(i);
            finish();
        }

        til_username = (TextInputLayout) findViewById(R.id.username);
        til_password = (TextInputLayout) findViewById(R.id.password);
        et_username = (TextInputEditText) findViewById(R.id.et_username);
        et_password = (TextInputEditText) findViewById(R.id.et_password);
        rb_dev = (RadioButton) findViewById(R.id.rb_dev);
        rb_prod = (RadioButton) findViewById(R.id.rb_prod);
        b_login = (Button) findViewById(R.id.btn_login);

        rb_dev.setChecked(true);

        b_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username, password;
                username = et_username.getText().toString().toUpperCase();
                password = et_password.getText().toString();
                if (et_username.getText().length() > 0) {
                    if (et_password.getText().length() > 0) {
                        loguser(username, password); //jalanin api select username di database
                        til_username.setError(null);
                        til_password.setError(null);
                    } else {
                        til_password.setError("password kosong");
                    }
                } else {
                    til_username.setError("username kosong");
                }
            }
        });
    }

    void loguser(final String user, final String passwd) {
        Log.d("username", user);
        API_Link a = API_Client.getClient().create(API_Link.class);
        Call<User_Model> call = a.loguser(user);

        call.enqueue(new Callback<User_Model>() {
            @Override
            public void onResponse(Call<User_Model> call, Response<User_Model> response) {
                if (response.body().getError() == false) {
                    Log.d("error1", response.body().getError().toString());
                    login(user, passwd); //function jika username terdaftar, menjalankan api untuk mengecek password
                } else {
                    Log.d("error2", response.body().getError().toString());
                    erroruser(); //function alert user tidak terdaftar
                }
            }

            @Override
            public void onFailure(Call<User_Model> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(LoginActivity.this, "login gagal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void erroruser() {
        new MaterialAlertDialogBuilder(LoginActivity.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle("Alert!")
                .setMessage(
                        "Username tidak terdaftar! Silahkan daftar akun erp terlebih dahulu")
                .setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        et_username.setText("");
                        et_password.setText("");
                    }

                }).show();
    }

    void login(final String user, String password) {
        Log.d("1", user);
        Log.d("2", password);
        API_Link a = API_Client.getClient().create(API_Link.class);
        Call<Login_Model> call = a.login(user, password);

        call.enqueue(new Callback<Login_Model>() {
            @Override
            public void onResponse(Call<Login_Model> call, Response<Login_Model> response) {
                if (!response.body().isError()) {
                    Log.d("coba", response.body().getUser());
                    checkkoneksi(user);
                    Toast.makeText(LoginActivity.this, "Selamat datang " + response.body().getUser(), Toast.LENGTH_SHORT).show();
                } else {
                    errorpasswd(); //function alert password salah
                }
            }

            @Override
            public void onFailure(Call<Login_Model> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(LoginActivity.this, "login gagal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void errorpasswd() {
        new MaterialAlertDialogBuilder(LoginActivity.this)
                .setIcon(R.drawable.ic_warning)
                .setTitle("Alerta")
                .setMessage("password yang ada masukan salah")
                .setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        et_password.setText("");
                    }
                }).show();
    }

    void checkkoneksi(String username) {
        if (rb_dev.isChecked()) {
            cName = "192.168.7.3";
            cPort = "1522";
            cSid = "DEV";
            cUsername = "APPS";
            cPassword = "APPS";
            String usernamedev = "AA TECH TSR 01";
            String passwddev = "PARIS2020";
            session.createUserSession(usernamedev, passwddev, "5177", cName, cPort, cSid, cUsername, cPassword, username);
            recreate();
        } else {
            cName = "192.168.7.1";
            cPort = "1521";
            cSid = "PROD";
            cUsername = "APPS";
            cPassword = "APPS";
            String usernameprod = "AA TECH TSR 01";
            String passwdprod = "TOKYO2020";
            session.createUserSession(usernameprod, passwdprod, "5177", cName, cPort, cSid, cUsername, cPassword, username);
            recreate();
        }
    }

    String getUserId(String username) {
        Connection conn = new Koneksi().getConnection(cName, cPort, cSid, cUsername, cPassword);
        Statement statement;
        ResultSet result;
        try {
            statement = conn.createStatement();
            String Query = "SELECT user_id FROM fnd_user \n" +
                    "WHERE user_name = '" + username + "'";
            result = statement.executeQuery(Query);
            if (result.next()) return result.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}

