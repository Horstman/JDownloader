package org.jdownloader.extensions.webinterfaceng.data;

import java.io.Serializable;

import jd.plugins.FilePackage;

public class FilePackageDO implements Serializable {

    private String name;

    private String hoster;

    private double percent;

    private int    id;

    public FilePackageDO(FilePackage filePackage) {
        this.id = filePackage.getListOrderID();
        this.name = filePackage.getName();
        this.hoster = filePackage.getHoster();
        this.percent = filePackage.getPercent();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHoster() {
        return hoster;
    }

    public void setHoster(String hoster) {
        this.hoster = hoster;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return getName();
    }

}
