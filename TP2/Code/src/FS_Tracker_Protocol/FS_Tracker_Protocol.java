package FS_Tracker_Protocol;

import FS_Tracker.FS_Tracker;
import FS_Transfer_Protocol.FS_Transfer_Protocol;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class FS_Tracker_Protocol implements Serializable
{
    private static final byte[] TOKEN = ".@.".getBytes();
    private static final byte[] END = "!!".getBytes();
    public static final int NODE_FILES_LIST = 1;
    public static final int NODE_FILE_ADD = 2;
    public static final int NODE_FILE_REMOVE = 3;
    public static final int FILE_LIST_REQUEST = 4;
    public static final int FILE_LIST_RESPONSE = 5;
    public static final int FILE_LOCATION_REQUEST = 6;
    public static final int FILE_LOCATION_RESPONSE = 7;
    public static final int CLOSE_CONNECTION = 8;
    public static final int ERROR = 9;
    public static final int UPDATE_FILE = 10;

    private String UUID;
    private int type;
    private byte[] nodeAddr;
    private int totalFragments;
    private int index;
    private int messageSize;
    private byte[] message;

    public FS_Tracker_Protocol(String UUID, int type, byte[] nodeAddr, int totalFragments, int index, int messageSize, byte[] message) {
        this.UUID = UUID;
        this.type = type;
        this.nodeAddr = nodeAddr;
        this.totalFragments = totalFragments;
        this.index = index;
        this.messageSize = messageSize;
        this.message = message.clone();
    }

//    public FS_Tracker_Protocol(byte[] bytes) throws UnknownHostException {
//        int byteSize = 0;
//        List<byte[]> endsep = FS_Tracker_Protocol.split(END,bytes);
//        List<byte[]> list = FS_Tracker_Protocol.split(TOKEN, endsep.get(0));
//        this.UUID = new String(list.get(0));
//        this.type = Integer.parseInt(new String(list.get(1)));
//        this.nodeAddr = InetAddress.getByName(new String(list.get(2)));
//        list.subList(0, 3).clear();
//        for (byte[] bt : list)
//        {
//            byteSize += bt.length;
//        }
//        this.message = new byte[byteSize];
//        int i = 0;
//        for (byte[] bt : list)
//        {
//            for (byte b : bt)
//            {
//                this.message[i++] = b;
//            }
//        }
//    }

    public FS_Tracker_Protocol(byte[] data)
    {
        try
        {
            FS_Tracker_Protocol buffer = FS_Tracker_Protocol.deserialize(data);
            this.UUID = buffer.getUUID();
            this.type = buffer.getType();
            this.nodeAddr = buffer.getNodeAddr();
            this.totalFragments = buffer.getTotalFragments();
            this.index = buffer.getIndex();
            this.messageSize = buffer.getMessageSize();
            this.message = buffer.getMessage();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public int getTotalFragments() {
        return totalFragments;
    }

    public void setTotalFragments(int totalFragments) {
        this.totalFragments = totalFragments;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(int messageSize) {
        this.messageSize = messageSize;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getNodeAddr() throws UnknownHostException {
        return this.nodeAddr;
    }

    public void setNodeAddr(byte[] nodeAddr) throws UnknownHostException {
        this.nodeAddr = nodeAddr;
    }
//
//    public byte[] getMessage() {
//        return message;
//    }
//
//    public void setMessage(byte[] message) {
//        this.message = message;
//    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public byte[] convertByteArray()
    {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            byte[] data = baos.toByteArray();
            baos.close();
            oos.close();
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FS_Tracker_Protocol deserialize(byte[] data)
    {
        try
        {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream in = new ObjectInputStream(bais);
            FS_Tracker_Protocol protocol = (FS_Tracker_Protocol) in.readObject();
            bais.close();
            in.close();
            return protocol;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

//    public byte[] convertByteArray()
//    {
//        List<byte[]> list = new ArrayList<>();
//        list.add(this.UUID.getBytes());
//        list.add(TOKEN);
//        list.add(String.valueOf(this.type).getBytes());
//        list.add(TOKEN);
//        list.add(this.nodeAddr.getHostAddress().getBytes());
//        list.add(TOKEN);
//        list.add(this.message);
//        list.add(END);
//
//        int bytesize = 0, i = 0;
//        for (byte[] bt : list)
//        {
//            bytesize += bt.length;
//        }
//        byte[] converted = new byte[bytesize];
//        for (byte[] bt : list)
//        {
//            for (byte b : bt)
//            {
//                converted[i++] = b;
//            }
//        }
//        System.out.println("Tracker header size is " + (converted.length - this.message.length));
//        return converted;
//    }

    public static boolean isToken(byte[] token, byte[] input, int index)
    {
        for (int i = 0; i < token.length; i++)
        {
            if (token[i] != input[index+i])
            {
                return false;
            }
        }
        return true;
    }

    public static List<byte[]> split(byte[] token, byte[] input)
    {
        List<byte[]> list = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < input.length; i++)
        {
            if (isToken(token,input,i))
            {
                list.add(Arrays.copyOfRange(input,start, i));
                start = i + token.length;
                i = start;
            }
        }
        list.add(Arrays.copyOfRange(input,start, input.length));
        return list;
    }

    @Override
    public String toString() {
        return "FS_Tracker_Protocol{" +
                "UUID='" + UUID + '\'' +
                ", type=" + type +
                ", nodeAddr=" + Arrays.toString(nodeAddr) +
                ", totalFragments=" + totalFragments +
                ", index=" + index +
                ", messageSize=" + messageSize +
                ", message=" + Arrays.toString(message) +
                '}';
    }
}
