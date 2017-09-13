package cn.sintoon.facedemo.bd.entities;

/**
 * Created by mxc on 2017/9/11.
 * description:
 */

public class IdentifyUser {

    private String uid;
    private String groupid;
    private String uInfo;
    private int [] scores;

    public IdentifyUser(String uid, String groupid, String uInfo, int[] scores) {
        this.uid = uid;
        this.groupid = groupid;
        this.uInfo = uInfo;
        this.scores = scores;
    }

    public String getuInfo() {
        return uInfo;
    }

    public void setuInfo(String uInfo) {
        this.uInfo = uInfo;
    }

    public IdentifyUser() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getGroupid() {
        return groupid;
    }

    public void setGroupid(String groupid) {
        this.groupid = groupid;
    }

    public int[] getScores() {
        return scores;
    }

    public void setScores(int[] scores) {
        this.scores = scores;
    }
}
