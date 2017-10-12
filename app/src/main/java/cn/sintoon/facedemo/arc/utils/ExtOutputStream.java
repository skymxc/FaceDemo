package cn.sintoon.facedemo.arc.utils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by mxc on 2017/10/12.
 * description:
 */

public class ExtOutputStream extends BufferedOutputStream {

    public ExtOutputStream(OutputStream out) {
        super(out);
    }

    public ExtOutputStream(OutputStream out, int size) {
        super(out, size);
    }

    public boolean writeString(String name) throws IOException {
        this.write(ExtByteTools.convert_from_int(name.getBytes().length));
        this.write(name.getBytes());
        return true;
    }

    public boolean writeBytes(byte[] data) throws IOException {
        this.write(ExtByteTools.convert_from_int(data.length));
        this.write(data);
        return false;
    }
}
