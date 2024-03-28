package FS_Node;

import FS_Transfer_Protocol.FS_Transfer_Protocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Node_Request_Handler implements Runnable
{
    Node_Listener listener;
    Node_File_Manager fileManager;
    FS_Transfer_Protocol request;

    public Node_Request_Handler(Node_Listener listener, Node_File_Manager fileManager, FS_Transfer_Protocol request)
    {
        this.listener = listener;
        this.fileManager = fileManager;
        this.request = request;
    }

    public boolean sendBlockData(byte[] blockData) throws UnknownHostException, InterruptedException {
        FS_Transfer_Protocol block = new FS_Transfer_Protocol(this.request.getUUID(),InetAddress.getLocalHost().getHostName(), this.request.getSenderNode(),FS_Transfer_Protocol.FILE_RESPONSE,0,blockData);
        return this.listener.sendMessageWithRetry(block,Node_Listener.DEFAULTTRIES,2000);
    }

    @Override
    public void run()
    {
        try {
            this.listener.sendAck(request.getUUID(),request.getSenderNode(),request.getData());
            if (this.request.getType() == FS_Transfer_Protocol.FILE_REQUEST)
            {
                String[] stringArray = new String(this.request.getData()).split("[;]");
                String fileId = stringArray[0];
                int blockIndex = Integer.parseInt(stringArray[1]);
                byte[] blockData = this.fileManager.getBlock(fileId,blockIndex);
                if (blockData != null)
                {
                    if (this.sendBlockData(blockData))
                    {
                        this.listener.node.addLog("Block " + blockIndex + " of File " + fileId + " was sent sucessfully with request with UUID " + this.request.getUUID());
                    } else this.listener.node.addLog("Failed to send Block " + blockIndex + " of File " + fileId + " for request with UUID " + this.request.getUUID());
                } else this.listener.node.addLog("Block " + blockIndex + " of File " + fileId + " not found to send for request with UUID " + this.request.getUUID());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
