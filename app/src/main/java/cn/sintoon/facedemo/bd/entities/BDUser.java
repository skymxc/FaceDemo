package cn.sintoon.facedemo.bd.entities;

import java.util.List;

/**
 * Created by mxc on 2017/9/12.
 * description:
 */

public class BDUser {
    protected String uid;
    protected String uinfo;
    protected List<String> groups;

    public BDUser(String uid, String uinfo, List<String> groups) {
        this.uid = uid;
        this.uinfo = uinfo;
        this.groups = groups;
    }

    public BDUser() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUinfo() {
        return uinfo;
    }

    public void setUinfo(String uinfo) {
        this.uinfo = uinfo;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
}
