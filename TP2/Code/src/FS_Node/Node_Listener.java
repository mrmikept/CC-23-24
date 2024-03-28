package FS_Node;

import FS_Transfer_Protocol.FS_Transfer_Protocol;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Node_Listener implements Runnable
{
    private static final byte[] ENDTOKEN = "!!".getBytes();
    public static final int DEFAULTTRIES = 3;
    FS_Node node;
    Node_File_Manager fileManager;
    DatagramSocket socket;
    Map<String,List<FS_Transfer_Protocol>> packets;
    Map<String, FS_Transfer_Protocol> packetAck;
    Map<String, FS_Transfer_Protocol> responses;
    ReentrantLock lock;

    public Node_Listener(FS_Node node, Node_File_Manager fileManager) throws IOException {
        try
        {
            this.node = node;
            this.fileManager = fileManager;
            this.socket = new DatagramSocket(this.node.getPort());
            this.packets = new ConcurrentHashMap<>();
            this.packetAck = new ConcurrentHashMap<>();
            this.responses = new ConcurrentHashMap<>();
            this.lock = new ReentrantLock();
        } catch (Exception e)
        {
            System.out.println("Failed to initiate Node Listener... Closing Program...");
            this.node.stopProgram();
        }
    }

    public boolean hasEndDelimiter(byte[] data, byte[] token)
    {
        if (data.length < token.length)
        {
            return false;
        }

        int offset = data.length - token.length;

        for (int i = 0; i < token.length; i++)
        {
            if (data[offset+i] != token[i])
            {
                return false;
            }
        }
        return true;
    }

    public void sendAck(String UUID, String destinationNode, byte[] data) throws IOException {
            FS_Transfer_Protocol ack = new FS_Transfer_Protocol(UUID,InetAddress.getLocalHost().getHostName(), destinationNode, FS_Transfer_Protocol.ACKNOWLEDGE, 0, data);
            this.sendMessage(ack);
    }

    public FS_Transfer_Protocol getACK(String UUID)
    {
        try {
            this.lock.lock();
            if (this.packetAck.containsKey(UUID))
            {
                return this.packetAck.remove(UUID);
            } else return null;
        } finally {
            this.lock.unlock();
        }
    }

    public boolean verifyACK(byte[] sentData, byte[] receivedData)
    {
        return Arrays.equals(sentData,receivedData);
    }

    public FS_Transfer_Protocol getResponse(String UUID) throws IOException {
        try {
            this.lock.lock();
            if (this.responses.containsKey(UUID))
            {
                FS_Transfer_Protocol response =  this.responses.remove(UUID);
                this.sendAck(response.getUUID(),response.getSenderNode(),response.getData());
                return response;
            } else return null;
        } finally {
            this.lock.unlock();
        }

    }

    public void sendMessage(FS_Transfer_Protocol protocol) throws IOException {
        try {
            this.lock.lock();
            byte[] data = protocol.serialize();
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(protocol.getDestinationNode()), this.node.getPort());
            this.socket.send(packet);
        } finally {
            this.lock.unlock();
        }
    }

    public boolean sendMessageWithRetry(FS_Transfer_Protocol protocol, int maxTries, long waitTime) throws UnknownHostException, InterruptedException {
        int tries = maxTries;
        boolean success = false;

        while (tries > 0 && !success)
        {
            try{
                this.sendMessage(protocol);
                Thread.sleep(waitTime);
                FS_Transfer_Protocol ack = null;
                if ((ack = this.getACK(protocol.getUUID())) != null)
                {
                    if (this.verifyACK(protocol.getData(),ack.getData()))
                    {
                        success = true;
                        node.addLog("Acknoledgment received with UUID " + protocol.getUUID() + " from Node " + ack.getSenderNode());
                    } else this.node.addLog("Received wrong acknoledgment with UUID " + protocol.getUUID() + " from Node " + ack.getSenderNode() + ". Retrying...");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            tries--;
        }
        return success;
    }

    @Override
    public void run()
    {
        try
        {
            List<Thread> threadList = new ArrayList<>();
            this.node.addLog("Listening for node requests...");
            ByteArrayOutputStream bufferReceivedData = new ByteArrayOutputStream();
            while (this.node.isOnline())
            {
                    byte[] receivedData = new byte[1400];
                    DatagramPacket packet = new DatagramPacket(receivedData,receivedData.length);

                    try {
                        this.socket.setSoTimeout(1000); // Setting a timeout of 1 second to periodiccaly check the node online status.
                        this.socket.receive(packet);
                    } catch (SocketTimeoutException e)
                    {
                        continue;
                    }
                    bufferReceivedData.write(packet.getData(), 0, packet.getLength());

                    if (this.hasEndDelimiter(bufferReceivedData.toByteArray(), ENDTOKEN))
                    {
                        byte[] databuffer = bufferReceivedData.toByteArray();
                        bufferReceivedData.reset();

                        byte[] data = new byte[databuffer.length - ENDTOKEN.length];
                        System.arraycopy(databuffer,0,data,0, databuffer.length - ENDTOKEN.length);

                        FS_Transfer_Protocol protocol = new FS_Transfer_Protocol(data);
                        if (protocol.getType() == FS_Transfer_Protocol.ACKNOWLEDGE)
                        {
                            this.node.addLog("Received an ACK with UUID " + protocol.getUUID());
                            this.packetAck.put(protocol.getUUID(),protocol);
                        }
                        else if (protocol.getType() == FS_Transfer_Protocol.FILE_REQUEST || protocol.getType() == FS_Transfer_Protocol.HASH_REQUEST)
                        {
                            this.node.addLog("Received a File Request with UUID " + protocol.getUUID() + " from Node " + protocol.getSenderNode());
                            if (!this.packets.containsKey(protocol.getUUID()))
                            {
                                this.packets.put(protocol.getUUID(),new ArrayList<>());
                                this.packets.get(protocol.getUUID()).add(protocol);
                            }
                            Thread thread = new Thread(new Node_Request_Handler(this,this.fileManager,protocol));
                            thread.setName("RequestHandler UUID " + protocol.getUUID());
                            thread.start();
                            threadList.add(thread);
                        }
                        else
                        {
                            this.responses.put(protocol.getUUID(),protocol);
                        }
                    }
            }
            for (Thread t : threadList)
            {
                System.out.println("Listener: Waiting for thread " + t.getName());
                t.join();
                System.out.println("Listener: Done!");
            }
            this.socket.close();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //this.socket.close();
        }
    }
}
