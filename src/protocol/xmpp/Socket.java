/*
 * Socket.java
 *
 * Created on 4 Февраль 2009 г., 15:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.xmpp;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZInputStream;
import com.jcraft.jzlib.ZOutputStream;
import protocol.net.TcpSocket;
import sawim.SawimException;
import sawim.modules.DebugLog;

import java.util.Vector;

/**
 * @author Vladimir Krukov
 */
final class Socket implements Runnable {
    private TcpSocket socket = new TcpSocket();
    private volatile boolean connected;
    private byte[] inputBuffer = new byte[1024];
    private int inputBufferLength = 0;
    public int inputBufferIndex = 0;
    private ZInputStream zIn;
    private ZOutputStream zOut;
    private boolean compressed;
    private boolean secured;
    private final Vector<Object> read = new Vector<Object>();

    /**
     * Creates a new instance of Socket
     */
    public Socket() {
    }

    public void startCompression() {
        zIn = new ZInputStream(socket);
        zOut = new ZOutputStream(socket, JZlib.Z_DEFAULT_COMPRESSION);
        zOut.setFlushMode(JZlib.Z_SYNC_FLUSH);
        compressed = true;
        DebugLog.println("zlib is working");
    }

    public void startTls(String host) {
        socket.startTls(host);
        secured = true;
    }

    public boolean isConnected() {
        return connected;
    }

    public void connectTo(String url) throws SawimException {
        System.out.println("url: " + url);
        socket.connectTo(url);
        connected = true;
    }

    private int read(byte[] data) throws SawimException {
        if (compressed) {
            int bRead = zIn.read(data);
            if (-1 == bRead) {
                throw new SawimException(120, 13);
            }
            return bRead;
        }
        int bRead = socket.read(data, 0, data.length);
        if (-1 == bRead) {
            throw new SawimException(120, 12);
        }
        return bRead;
    }

    public void write(byte[] data) throws SawimException {
        if (compressed) {
            zOut.write(data);
            zOut.flush();
            return;
        }
        socket.write(data, 0, data.length);
        socket.flush();
    }

    public void close() {
        connected = false;
        try {
            zIn.close();
            zOut.close();
        } catch (Exception ignored) {
        }
        socket.close();
        inputBufferLength = 0;
        inputBufferIndex = 0;
    }

    public void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    private void fillBuffer() throws SawimException {
        inputBufferIndex = 0;
        inputBufferLength = read(inputBuffer);
        while (0 == inputBufferLength) {
            sleep(100);
            inputBufferLength = read(inputBuffer);
        }
    }

    private byte readByte() throws SawimException {
        if (inputBufferIndex >= inputBufferLength) {
            fillBuffer();
        }
        return inputBuffer[inputBufferIndex++];
    }

    char readChar() throws SawimException {
        try {
            byte bt = readByte();
            if (0 <= bt) {
                return (char) bt;
            }
            if ((bt & 0xE0) == 0xC0) {
                byte bt2 = readByte();
                return (char) (((bt & 0x3F) << 6) | (bt2 & 0x3F));

            } else if ((bt & 0xF0) == 0xE0) {
                byte bt2 = readByte();
                byte bt3 = readByte();
                return (char) (((bt & 0x1F) << 12) | ((bt2 & 0x3F) << 6) | (bt3 & 0x3F));

            } else {
                int seqLen = 0;
                if ((bt & 0xF8) == 0xF0) seqLen = 3;
                else if ((bt & 0xFC) == 0xF8) seqLen = 4;
                else if ((bt & 0xFE) == 0xFC) seqLen = 5;
                for (; 0 < seqLen; --seqLen) {
                    readByte();
                }
                return '?';
            }
        } catch (SawimException e) {
            DebugLog.panic("readChar je ", e);
            throw e;
        } catch (Exception e) {
            DebugLog.panic("readChar e ", e);
            throw new SawimException(120, 7);
        }
    }

    boolean isSecured() {
        return secured;
    }

    private Object readObject() throws SawimException {
        Object readObject = null;
        while (connected && null == readObject) {
            readObject = XmlNode.parse(this);
            if (null == readObject) sleep(100);
        }
        return readObject;
    }

    @Override
    public void run() {
        Object readObject;
        while (connected) {
            try {
                readObject = readObject();
            } catch (SawimException e) {
                readObject = e;
            }
            if (null != readObject) {
                synchronized (read) {
                    read.addElement(readObject);
                }
            }
        }
    }

    public XmlNode readNode(boolean wait) throws SawimException {
        Object readObject = null;
        if (wait) {
            readObject = readObject();
        } else {
            synchronized (read) {
                if (0 < read.size()) {
                    readObject = read.elementAt(0);
                    read.removeElementAt(0);
                }
            }
        }
        if (readObject instanceof SawimException) throw (SawimException) readObject;
        return (XmlNode) readObject;
    }

    public void start() {
        new Thread(this).start();
    }
}
