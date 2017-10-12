package cn.sintoon.facedemo.arc.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by mxc on 2017/10/12.
 * description:
 */

public class ExtInputStream extends BufferedInputStream {

    public ExtInputStream(InputStream in) {
        super(in);
    }

    public ExtInputStream(InputStream in, int size) {
        super(in, size);
    }

    public String readString() throws IOException {
        byte[] size = new byte[4];
        if(this.read(size) > 0) {
            byte[] name = new byte[ExtByteTools.convert_to_int(size)];
            if(name.length == this.read(name)) {
                return new String(name);
            }
        }

        return null;
    }

    public byte[] readBytes() throws IOException {
        byte[] size = new byte[4];
        if(this.read(size) > 0) {
            byte[] data = new byte[ExtByteTools.convert_to_int(size)];
            if(data.length == this.read(data)) {
                return data;
            }
        }

        return null;
    }

    public boolean readBytes(byte[] data) throws IOException {
        byte[] size = new byte[4];
        return this.read(size) > 0 && ExtByteTools.convert_to_int(size) == data.length && data.length == this.read(data);
    }
}
