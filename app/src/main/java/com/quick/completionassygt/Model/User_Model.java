package com.quick.completionassygt.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @SerializeName digunakan untuk mengatur key yang akan menyertakan objek json
 * @Expose digunakan untuk memutuskan apakah variabel akan diekspos untuk Serialisasi dan Deserialisasi, atau tidak.
 */
public class User_Model {
    @SerializedName("error")
    @Expose
    private Boolean error;

    public Boolean getError() {
        return error;
    }

    public void setError(Boolean error) {
        this.error = error;
    }
}

