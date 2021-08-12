package com.quick.completionassygt.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Login_Model {
    @SerializedName("error")
    @Expose
    private boolean error;

    @SerializedName("userid")
    @Expose
    private String user_id;

    @SerializedName("user")
    @Expose
    private String user;

    @SerializedName("employee")
    @Expose
    private String employee;

    @SerializedName("kodesie")
    @Expose
    private String kodesie;

    @SerializedName("kode_lokasi_kerja")
    @Expose
    private String lokasi;

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getEmployee() {
        return employee;
    }

    public void setEmployee(String employee) {
        this.employee = employee;
    }

    public String getKodesie() {
        return kodesie;
    }

    public void setKodesie(String kodesie) {
        this.kodesie = kodesie;
    }

    public String getLokasi() {
        return lokasi;
    }

    public void setLokasi(String lokasi) {
        this.lokasi = lokasi;
    }
}