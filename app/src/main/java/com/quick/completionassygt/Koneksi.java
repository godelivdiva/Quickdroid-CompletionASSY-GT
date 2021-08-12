package com.quick.completionassygt;

import android.util.Log;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by Admin on 8/21/2017.
 */

public class Koneksi {
    public java.sql.Connection getConnection(String serverName, String port, String sid, String username, String password){
        try{
            Class.forName("oracle.jdbc.driver.OracleDriver");
            String url = "jdbc:oracle:thin:@"+serverName+":"+port+":"+sid;
            Log.d("Koneksi", "Connected to database->"+url+username+password);
            return DriverManager.getConnection(url,username,password);
        }catch (ClassNotFoundException | SQLException e){
            e.printStackTrace();
        }
        return null;
    }
}
