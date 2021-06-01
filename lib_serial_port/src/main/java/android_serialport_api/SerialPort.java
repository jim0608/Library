package android_serialport_api;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPort {
    private static final String TAG = "SerialPort";

    /*
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    //开启串口
    public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {
        /* Check access permission */
        if (!device.canRead() || !device.canWrite()) {
            try {
                /* Missing read/write permission, trying to chmod the file */
                Process su;
                su = Runtime.getRuntime().exec("/system/bin/su");
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
                        + "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead()
                        || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }

        mFd = open(device.getAbsolutePath(), baudrate, flags);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    // Getters and setters
    public InputStream getInputStream() {
        return mFileInputStream;
//        return new FileInputStream(mFd);
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
//        return new FileOutputStream(mFd);
    }

    public void closeSerial() {
            try {
                if (mFileInputStream!=null) {
                    mFileInputStream.close();
                }
                if (mFileOutputStream!=null) {
                    mFileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        close();
    }

    // JNI开启串口的方法
    private native static FileDescriptor open(String path, int baudrate, int flags);

    // JNI关闭串口的方法
    public native void close();

    private static native boolean _init();

    private static native int _read(byte[] data, int len);

    private static native int _write(byte[] data);


    static {
        System.loadLibrary("SerialPort");
    }

}
