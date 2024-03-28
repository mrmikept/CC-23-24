package FS_Tracker_Protocol;

import FS_Tracker.FS_Tracker;
import FS_Tracker_Protocol.FS_Tracker_Protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

public class DataManagement
{
    public static int FRAGMENTSIZE = 1024;
    public static int MAXSIZE = 1281;
    public static int HEADERSIZE = 239;
    private Map<String,List<FS_Tracker_Protocol>> messages;
    private DataInputStream input;
    private DataOutputStream output;
    private boolean online;

    public DataManagement(DataInputStream input, DataOutputStream output, Queue<String> queue)
    {
        this.messages = new HashMap<>();
        this.input = input;
        this.output = output;
        this.online = true;
    }

    public int calcTotalFrag(byte[] data)
    {
        int currentIndex = 0;
        int dataSize = data.length;
        int fragments = 0;
        while (currentIndex < data.length)
        {
            int remainingBytes = dataSize - currentIndex;
            int bufferSize = Math.min(remainingBytes,FRAGMENTSIZE);
            currentIndex += bufferSize;
            fragments++;
        }
        return fragments;
    }

    public List<FS_Tracker_Protocol> createMessages(String UUID, int type, byte[] nodeAddr, byte[] data) throws UnknownHostException {
        List<FS_Tracker_Protocol> list = new ArrayList<>();
        int currentIndex = 0;
        int dataSize = data.length;
        int fragment = 0;
        int totalFragments = this.calcTotalFrag(data);
        while (currentIndex < data.length)
        {
            int remainingBytes = dataSize - currentIndex;
            int bufferSize = Math.min(remainingBytes,FRAGMENTSIZE);
            byte[] buffer = new byte[FRAGMENTSIZE];
            System.arraycopy(data,currentIndex,buffer,0,bufferSize);
            if (bufferSize < FRAGMENTSIZE)
            {
                for (int i = bufferSize; i < FRAGMENTSIZE; i++)
                {
                    buffer[i] = 1;
                }
            }
            list.add(new FS_Tracker_Protocol(UUID,type,nodeAddr,totalFragments,fragment,bufferSize,buffer));
            fragment++;
            currentIndex += bufferSize;
        }
        return list;
    }

    public void sendMessage(String UUID, int type, byte[] nodeAddr, byte[] data) throws IOException {
        if (data.length > FRAGMENTSIZE)
        {
            List<FS_Tracker_Protocol> messages = this.createMessages(UUID, type, nodeAddr, data);
            for (FS_Tracker_Protocol protocol : messages)
            {
                byte[] serializedProto = protocol.convertByteArray();
                this.output.write(serializedProto);
                this.output.flush();
            }
        }
        else
        {
            byte[] buffer = new byte[FRAGMENTSIZE];
            System.arraycopy(data,0,buffer,0,data.length);
            for (int i = data.length; i < FRAGMENTSIZE; i++)
            {
                buffer[i] = 1;
            }
            FS_Tracker_Protocol protocol = new FS_Tracker_Protocol(UUID,type,nodeAddr,1,0,data.length,buffer);
            byte[] serializedProto = protocol.convertByteArray();
            this.output.write(serializedProto);
            this.output.flush();
        }
    }

    public boolean getOnlineStatus()
    {
        return this.online;
    }

    public byte[] getData(String UUID)
    {
        if (this.messages.containsKey(UUID))
        {
            List<FS_Tracker_Protocol> list = this.messages.get(UUID);
            int totalMessageSize = 0;
            if (list.size() < list.get(0).getTotalFragments())
            {
                return null;
            }
            for (FS_Tracker_Protocol protocol : list)
            {
                totalMessageSize += protocol.getMessageSize();
            }
            byte[] data = new byte[totalMessageSize];
            for (FS_Tracker_Protocol protocol : list)
            {
                System.arraycopy(protocol.getMessage(),0,data,protocol.getIndex() * FRAGMENTSIZE, protocol.getMessageSize());
            }
            return data;
        } else return null;
    }

    public FS_Tracker_Protocol readMessage() throws IOException {
        byte[] buffer = new byte[MAXSIZE];
        int readedBytes = 0;
        int totalbytes = 0;
        FS_Tracker_Protocol protocol = null;
        if ((readedBytes = this.input.readNBytes(buffer,0,MAXSIZE)) > 0 && totalbytes < MAXSIZE)
        {
            totalbytes += readedBytes;
            byte[] data = new byte[readedBytes];
            System.arraycopy(buffer,0,data,0,readedBytes);
            if (totalbytes >= MAXSIZE)
            {
                protocol = new FS_Tracker_Protocol(data);
                if (!this.messages.containsKey(protocol.getUUID()))
                {
                    this.messages.put(protocol.getUUID(), new ArrayList<>());
                }
                this.messages.get(protocol.getUUID()).add(protocol);
            }
        }
        if (readedBytes <= 0)
        {
            this.online = false;
        }
        return protocol;
    }

}
