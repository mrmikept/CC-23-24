package FS_Tracker;

import DataUtilities.UniqueIdGenerator;
import Files.FileInfo;
import FS_Tracker_Protocol.*;
import DataUtilities.Serializer;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

public class TrackerConnection_Handler implements Runnable
{
    private FS_Tracker tracker;
    private String nodeId;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private Socket client;

    public TrackerConnection_Handler(FS_Tracker tracker, String nodeId, DataOutputStream outputStream, DataInputStream inputStream, Socket socket)
    {
        this.tracker = tracker;
        this.nodeId = nodeId;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.client = socket;
    }

    @Override
    public void run()
    {
        Queue<String> queue = new PriorityQueue<>(); // TODO REMOVE QUEUE
        DataManagement dataManagement = new DataManagement(this.inputStream,this.outputStream,queue);
        try {
            try {
                while (this.tracker.getServerActive() && dataManagement.getOnlineStatus())
                {
                    FS_Tracker_Protocol protocol = null;
                    while (protocol == null && dataManagement.getOnlineStatus())
                    {
                        protocol = dataManagement.readMessage();
                    }
                    this.tracker.addLog("Received a new message from Node " + InetAddress.getByAddress(protocol.getNodeAddr()).getHostName() + " with " + protocol.getTotalFragments() + "fragments.");
                    if (protocol.getTotalFragments() > 1)
                    {
                        this.tracker.addLog("Reading other fragments...");
                        int i = 1;
                        while (i < protocol.getTotalFragments())
                        {
                            dataManagement.readMessage();
                            i++;
                        }
                        this.tracker.addLog("All fragments read.");
                    }
                    if (protocol.getType() == FS_Tracker_Protocol.NODE_FILES_LIST) // Received the file in the shared folder of the node
                    {
                        this.tracker.addLog("[Files Info] Node " + InetAddress.getByAddress(protocol.getNodeAddr()).getHostName() + " sent information about his shared files.");
                        List<FileInfo> files = Serializer.byteArrayToArrayListFileInfo(dataManagement.getData(protocol.getUUID()));
                        this.tracker.addNodeFilesList(InetAddress.getByAddress(protocol.getNodeAddr()).getHostName(),files); // TODO IP DO NODE AQUI!!!!
                    }
                    if (protocol.getType() == FS_Tracker_Protocol.NODE_FILE_ADD)
                    {
                        this.tracker.addFileToNode(InetAddress.getByAddress(protocol.getNodeAddr()).getHostName(),Serializer.byteArrayToArrayListFileInfo(dataManagement.getData(protocol.getUUID())));
                    }
                    if (protocol.getType() == FS_Tracker_Protocol.NODE_FILE_REMOVE)
                    {
                        this.tracker.RemoveFileFromNode(InetAddress.getByAddress(protocol.getNodeAddr()).getHostName(),Serializer.byteArrayToArrayListFileInfo(dataManagement.getData(protocol.getUUID())));
                    }
                    if (protocol.getType() == FS_Tracker_Protocol.FILE_LIST_REQUEST)
                    {
                        ArrayList<FileInfo> files = this.tracker.getAllFileList(InetAddress.getByAddress(protocol.getNodeAddr()).getHostName());
                        if (!files.isEmpty())
                        {
                            this.tracker.addLog("[File List] Sending all files information to node " + InetAddress.getByAddress(protocol.getNodeAddr()).getHostName());
                            dataManagement.sendMessage(protocol.getUUID(), FS_Tracker_Protocol.FILE_LIST_RESPONSE, protocol.getNodeAddr(), Serializer.serializableFileInfoList(files));
                        }
                        else
                        {
                            this.tracker.addLog("[File List] Error, no files found to send to node " + InetAddress.getByAddress(protocol.getNodeAddr()).getHostName());
                            dataManagement.sendMessage(protocol.getUUID(), FS_Tracker_Protocol.ERROR, protocol.getNodeAddr(), "NoFiles".getBytes());
                        }
                    }
                    if (protocol.getType() == FS_Tracker_Protocol.FILE_LOCATION_REQUEST)
                    {
                        this.tracker.addLog("[File Location] Sending location of a file to node " + InetAddress.getByAddress(protocol.getNodeAddr()).getHostName());
                        String filename = new String(dataManagement.getData(protocol.getUUID()));
                        Map<String,FileInfo> nodes = tracker.getFileLocation(filename);
                        if (!nodes.isEmpty())
                        {
                            dataManagement.sendMessage(protocol.getUUID(), FS_Tracker_Protocol.FILE_LOCATION_RESPONSE, protocol.getNodeAddr(), Serializer.serializableMapStringFileInfo(nodes));
                        }
                        else
                        {
                            dataManagement.sendMessage(protocol.getUUID(), FS_Tracker_Protocol.ERROR, protocol.getNodeAddr(), "NoFiles".getBytes());
                            this.tracker.addLog("[File Location] Error sent to node " + InetAddress.getByAddress(protocol.getNodeAddr()).getHostName());
                        }
                    }
                    if (protocol.getType() == FS_Tracker_Protocol.UPDATE_FILE)
                    {
                        this.tracker.addLog("[File Info] Updating information of a node file...");
                        ByteArrayInputStream bais = new ByteArrayInputStream(dataManagement.getData(protocol.getUUID()));
                        ObjectInputStream ois = new ObjectInputStream(bais);
                        FileInfo fileInfo = (FileInfo) ois.readObject();
                        this.tracker.updateFileInfo(InetAddress.getByAddress(protocol.getNodeAddr()).getHostName(), fileInfo);
                        bais.close();
                        ois.close();
                    }
                    if (protocol.getType() == FS_Tracker_Protocol.CLOSE_CONNECTION)
                    {
                        break;
                    }
                }
                if (!this.tracker.getServerActive())
                {
                    dataManagement.sendMessage(UniqueIdGenerator.generateUUID(),FS_Tracker_Protocol.CLOSE_CONNECTION,this.client.getInetAddress().getAddress(),"byeee!".getBytes());
                }
            }
            catch (Exception e)
            {
                this.tracker.addLog("Something went wrong in the connection with node " + this.nodeId);
            }
        } finally {
            try
            {
                this.tracker.addLog("Closing connection with node " + this.nodeId);
                this.tracker.removeNode(this.nodeId);
                client.close();
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
