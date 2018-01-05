package com.fadejimi.savingsapp.model;

/**
 * Created by Fadejimi on 1/5/18.
 */

public class Outlet {
    public String outletName;
    public String address;
    public String classItem;
    public String contactName;
    public String contactMobile;
    public double qty;
    public double latitude;
    public double longitude;
    public String userId;

    public Outlet() {

    }

    public Outlet(String userId, String outletName, String address, String classItem, String contactName, String contactMobile,
                  double qty, double latitude, double longitude) {
        this.userId = userId;
        this.outletName = outletName;
        this.address = address;
        this.classItem = classItem;
        this.contactName = contactName;
        this.contactMobile = contactMobile;
        this.qty = qty;
        this.latitude = latitude;
        this.longitude = longitude;
    }

}
