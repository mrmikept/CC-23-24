package FS_Transfer_Protocol;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FS_Transfer_Protocol implements Serializable
{
    private static final byte[] TOKEN = ".@.".getBytes();
    private static final byte[] ENDTOKEN = "!!".getBytes();
    public static final int ACKNOWLEDGE = 0;
    public static final int FILE_REQUEST = 1;
    public static final int FILE_RESPONSE = 2;
    public static final int HASH_REQUEST = 3;
    public static final int BANDWIDTH_REQUEST = 4;
    public static final int BANDWIDTH_REPLY = 5;
    public static final int ERROR = 10;
    private String UUID;
    private InetAddress senderNode;
    private InetAddress destinationNode;
    private int type;
    private int offset;
    private byte[] data;

    public FS_Transfer_Protocol(String UUID, String senderNode, String destinationNode, int type, int offset, byte[] data) throws UnknownHostException {
        this.UUID = UUID;
        this.senderNode = InetAddress.getByName(senderNode);
        this.destinationNode = InetAddress.getByName(destinationNode);
        this.type = type;
        this.offset = offset;
        this.data = data;
    }

    public FS_Transfer_Protocol(FS_Transfer_Protocol protocol) throws UnknownHostException {
        this.UUID = protocol.getUUID();
        this.senderNode = InetAddress.getByName(protocol.getSenderNode());
        this.destinationNode = InetAddress.getByName(protocol.getDestinationNode());
        this.type = protocol.getType();
        this.offset = protocol.getOffset();
        this.data = protocol.getData();
    }

    public FS_Transfer_Protocol(byte[] data)
    {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInput in = new ObjectInputStream(bais);
            FS_Transfer_Protocol buffer =  (FS_Transfer_Protocol) in.readObject();
            this.UUID = buffer.getUUID();
            this.senderNode = InetAddress.getByName(buffer.getSenderNode());
            this.destinationNode = InetAddress.getByName(buffer.getDestinationNode());
            this.type = buffer.getType();
            this.offset = buffer.getOffset();
            this.data = buffer.getData();
            bais.close();
            in.close();
        }
        catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public byte[] convertByteArray()
    {
        List<byte[]> list = new ArrayList<>();
        list.add(this.getUUID().getBytes());
        list.add(TOKEN);
        list.add(this.getSenderNode().getBytes());
        list.add(TOKEN);
        list.add(this.getDestinationNode().getBytes());
        list.add(TOKEN);
        list.add(String.valueOf(this.getType()).getBytes());
        list.add(TOKEN);
        list.add(String.valueOf(this.getOffset()).getBytes());
        list.add(TOKEN);
        list.add(this.getData());
        list.add(ENDTOKEN);

        int byteSize = 0, i = 0;

        for (byte[] bt : list)
        {
            byteSize += bt.length;
        }
        byte[] converted = new byte[byteSize];
        for (byte[] bt : list)
        {
            for (byte b : bt)
            {
                converted[i++] = b;
            }
        }
        return converted;
    }

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

    public byte[] serialize()
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(baos);
            out.writeObject(this);
            byte[] bufferdata = baos.toByteArray();
            byte[] data = new byte[bufferdata.length + ENDTOKEN.length];
            System.arraycopy(bufferdata,0,data,0,bufferdata.length);
            System.arraycopy(ENDTOKEN,0,data,bufferdata.length,ENDTOKEN.length);
            baos.close();
            out.close();
            return data;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getSenderNode() {
        return this.senderNode.getHostName();
    }

    public String getDestinationNode() {
        return this.destinationNode.getHostName();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public byte[] getData() {
        return data.clone();
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "FS_Transfer_Protocol{" +
                "UUID='" + UUID + '\'' +
                ", senderNode='" + senderNode + '\'' +
                ", destinationNode='" + destinationNode + '\'' +
                ", type=" + type +
                ", offset=" + offset +
                ", data=" + Arrays.toString(data) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FS_Transfer_Protocol that = (FS_Transfer_Protocol) o;
        return type == that.type && offset == that.offset && Objects.equals(UUID, that.UUID) && Objects.equals(senderNode, that.senderNode) && Objects.equals(destinationNode, that.destinationNode) && Arrays.equals(data, that.data);
    }

    public FS_Transfer_Protocol clone()
    {
        try {
            return new FS_Transfer_Protocol(this);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

}
