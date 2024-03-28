package FS_Node;

import DataUtilities.UniqueIdGenerator;
import FS_Tracker_Protocol.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

public class Node_TrackerConnector implements Runnable
{
    private Socket tracker;
    private String trackerAddr;
    private int port;
    private String nodeAddr;
    private DataInputStream tracker_In;
    private DataOutputStream tracker_Out;
    private Map<String, byte[]> responses;
    private DataManagement dataManagement;
    private boolean isOnline;

    public Node_TrackerConnector(String trackerAddr, String nodeAddr, int port) throws IOException {
        try {
            this.tracker = new Socket(trackerAddr, port);
            this.trackerAddr = trackerAddr;
            this.port = port;
            this.nodeAddr = nodeAddr;
            this.tracker_In = new DataInputStream(this.tracker.getInputStream());
            this.tracker_Out = new DataOutputStream(this.tracker.getOutputStream());
            this.dataManagement = new DataManagement(this.tracker_In, this.tracker_Out, new PriorityQueue<>());
            this.responses = new HashMap<>();
            this.isOnline = true;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public String getTrackerAddr()
    {
        return this.trackerAddr;
    }

    public int getPort()
    {
        return this.port;
    }

    public String getNodeAddr() {
        return nodeAddr;
    }

    public void setNodeAddr(String nodeAddr) {
        this.nodeAddr = nodeAddr;
    }

    public String sendMessage(int messageType, byte[] data) throws IOException {
        String UUID = UniqueIdGenerator.generateUUID();
        this.dataManagement.sendMessage(UUID, messageType, InetAddress.getLocalHost().getAddress(), data);
        return UUID;
    }

    public byte[] sendMessageAndWaitResponse(int messageType, byte[] data) throws IOException, InterruptedException {
        String UUID = this.sendMessage(messageType, data);
        while (this.responses.get(UUID) == null)
        {
            Thread.sleep(100);
        }
        return this.responses.remove(UUID);
    }

//    public byte[] readStream() throws IOException {
//        int size, totalsize = 0, pos = 0;
//        ArrayList<byte[]> bufferbytes = new ArrayList<>();
//        byte[] buffer = new byte[1024];
//        while ((size = this.tracker_In.read(buffer, 0, 1024)) > 0)
//        {
//            totalsize += size;
//            byte[] buffcopy = new byte[size];
//            System.arraycopy(buffer, 0, buffcopy, 0, size);
//            bufferbytes.add(buffcopy);
//            if (size < 128)
//            {
//                break;
//            }
//        }
//        byte[] data = new byte[totalsize];
//        for (byte[] bt : bufferbytes)
//        {
//            for (byte b : bt)
//            {
//                data[pos++] = b;
//            }
//        }
//        return data;
//    }

//    public byte[] readStream() throws IOException {
//        int size = this.tracker_In.readInt();
//        byte[] data = new byte[size];
//        this.tracker_In.read(data,0,size);
//        return data;
//    }

    public void stopConnection()
    {
        try {
            this.isOnline = false;
            this.tracker.shutdownInput();
            this.tracker.shutdownOutput();
            this.tracker.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run()
    {
        while (this.isOnline && this.dataManagement.getOnlineStatus())
        {
            try {
                FS_Tracker_Protocol response = this.dataManagement.readMessage();
                if (response != null)
                {
                    if (response.getTotalFragments() > 1)
                    {
                        int i = 1;
                        while (i < response.getTotalFragments())
                        {
                            this.dataManagement.readMessage();
                            i++;
                        }
                    }
                    if (response.getType() == FS_Tracker_Protocol.CLOSE_CONNECTION)
                    {
                        this.stopConnection();
                        System.out.println("Tracker went offline, aborting program...");
                    }
                    this.responses.put(response.getUUID(),this.dataManagement.getData(response.getUUID()));
                }
            } catch (IOException e) {

            }
        }
    }
}
