package cn.sintoon.facedemo.arc.utils;

/**
 * Created by mxc on 2017/10/12.
 * description:
 */

public class ExtByteTools {

    public static byte[] convert_from_int(int val) {
        byte[] size = new byte[]{(byte)(val >> 24), (byte)(val >> 16), (byte)(val >> 8), (byte)(val >> 0)};
        return size;
    }

    public static int convert_to_int(byte[] val) {
        int size = 0;
        if(val.length >= 4) {
            size |= val[0] << 24;
            size |= val[1] << 16;
            size |= val[2] << 8;
            size |= val[3] << 0;
        }

        return size;
    }

}
