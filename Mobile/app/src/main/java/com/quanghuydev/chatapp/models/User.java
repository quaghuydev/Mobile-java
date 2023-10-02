package com.quanghuydev.chatapp.models;

import java.io.Serializable;

public class User implements Serializable {
    public String name,image,email,token,id;



    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
