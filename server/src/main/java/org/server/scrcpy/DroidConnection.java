package org.server.scrcpy;


import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public final class DroidConnection implements Closeable {


    private static Socket socket = null;
    private OutputStream outputStream;
    private InputStream inputStream;

    private DroidConnection(Socket socket) throws IOException {
        this.socket = socket;

        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }


    private static Socket listenAndAccept() throws IOException {
        ServerSocket serverSocket = new ServerSocket(7007);
        Socket sock = null;
        try {
            sock = serverSocket.accept();
        } finally {
            serverSocket.close();
        }
        return sock;
    }

    public static DroidConnection open(String ip) throws IOException {

        socket = listenAndAccept();
        DroidConnection connection = null;
//        if (socket.getInetAddress().toString().equals(ip)) {
//            connection = new DroidConnection(socket);
//        }
        if (!socket.getInetAddress().toString().equals(ip)) {
            Ln.w("socket connect address != " + ip);
        }
        // 判断 socket 有一个正确的地址
        if (!socket.getInetAddress().toString().isEmpty()) {
            connection = new DroidConnection(socket);
        }
        return connection;
    }

    public void close() throws IOException {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public int fillBufferCompletely(InputStream is, byte[] bytes, int size) throws IOException {
        int offset = 0;
        while (offset < size) {
            int read = is.read(bytes, offset, size - offset);
            if (read == -1) {
                if ( offset == 0 ) {
                    return -1;
                } else {
                    return offset;
                }
            } else {
                offset += read;
            }
        }

        return size;
    }


    public int[] NewreceiveControlEvent(byte[][] rawCtrlEventBytesWrapper) throws IOException {

        byte[] bufLen = new byte[1];
        byte[] buf = new byte[24];
        int n = inputStream.read(bufLen, 0, 1);
        if (n != 1) {
            throw new EOFException("Event controller socket closed");
        }
        if (bufLen[0] < 4 || bufLen[0] > 24) {
            throw new EOFException("Bad bufLen");
        }
        n =  fillBufferCompletely(inputStream, buf, bufLen[0]);
        if (n == -1) {
            throw new EOFException("Event controller socket closed");
        }

        final int[] array = new int[buf.length / 4];
        for (int i = 0; i < array.length; i++)
            array[i] = (((int) (buf[i * 4]) << 24) & 0xFF000000) |
                    (((int) (buf[i * 4 + 1]) << 16) & 0xFF0000) |
                    (((int) (buf[i * 4 + 2]) << 8) & 0xFF00) |
                    ((int) (buf[i * 4 + 3]) & 0xFF);
        rawCtrlEventBytesWrapper[0] = buf;
        return array;


    }

}

