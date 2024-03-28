package FS_Node;

import DataUtilities.UniqueIdGenerator;
import FS_Transfer_Protocol.FS_Transfer_Protocol;
import Files.BlockInfo;
import Files.FileHash;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

public class Node_Request_Maker implements Runnable
{
    private String fileId;
    private BlockInfo block;
    private List<String> nodesWithFile;
    private Node_FileTransfer_Manager manager;
    private int selected;
    private boolean sucess;

    public Node_Request_Maker(String fileId, BlockInfo blockInfo, List<String> nodes, Node_FileTransfer_Manager manager)
    {
        this.fileId = fileId;
        this.block = blockInfo;
        this.nodesWithFile = nodes;
        this.manager = manager;
        this.selected = 0;
        this.sucess = false;
    }

    public double nodeLatencyCheck(String nodeId) throws IOException {
        InetAddress node = InetAddress.getByName(nodeId);
        long sumTimes = 0;
        int tries = 5;
        for (int i = 0; i < tries; i++)
        {
            long startTime = System.currentTimeMillis();
            if (node.isReachable(5000))
            {
                long endTime = System.currentTimeMillis();
                sumTimes += (endTime - startTime);
            } else return -1;
        }
        double latency = (double) (sumTimes / tries);
        this.manager.putLatency(nodeId,latency);
        return latency;
    }

    public void calcLatency() throws IOException {
        for (String nodeId : this.nodesWithFile)
        {
            if (this.manager.getLantency(nodeId) == -1)
            {
                if (this.nodeLatencyCheck(nodeId) == -1)
                {
                    this.nodesWithFile.remove(nodeId);
                }
            }
        }
        Collections.sort(this.nodesWithFile,(node1,node2) -> Double.compare(this.manager.getLantency(node1),this.manager.getLantency(node2)));
    }

    public String selectNode()
    {
        return this.nodesWithFile.get(this.selected);
    }

    public byte[] makeBlockRequest(String node) throws IOException, InterruptedException, NoSuchAlgorithmException {
        // TODO Logs aqui
        String fileInformation = this.fileId + ";" + this.block.getIndex();
        FS_Transfer_Protocol request = new FS_Transfer_Protocol(UniqueIdGenerator.generateUUID(), InetAddress.getLocalHost().getHostName(), node, FS_Transfer_Protocol.FILE_REQUEST, 0, fileInformation.getBytes());
        if (this.manager.sendMessageWithRetry(request,Node_Listener.DEFAULTTRIES, (long) (3 * this.manager.getLantency(node))))
        {
            FS_Transfer_Protocol response = null;
            while ((response = this.manager.getResponse(request.getUUID())) == null)
            {
                Thread.sleep((long) (3 * this.manager.getLantency(node)));
            }
            if (response != null)
            {
                if (verifyBlockIntegrity(response.getData()))
                {
                    return response.getData();
                }
            } else System.out.println("Didn't receive response for block " + this.block.getIndex());
        }
        return null;
    }

    public boolean verifyBlockIntegrity(byte[] data) throws NoSuchAlgorithmException {
        return FileHash.getBlockHash(data,data.length).equals(this.block.getHashValue());
    }

    @Override
    public void run()
    {
        try {
            this.calcLatency();
            while (!sucess)
            {
                byte[] fileBlock = this.makeBlockRequest(this.selectNode());
                if (fileBlock == null)
                {
                    this.manager.listener.node.addLog("Failed to trasfer block " + this.block.getIndex() + " of file " + this.fileId + " from node " + this.selectNode());
                    if (this.selected < this.nodesWithFile.size() - 1)
                    {
                        this.manager.listener.node.addLog("Asking another node for block " + this.block.getIndex() + " of file " + this.fileId);
                        this.selected++;
                    }
                    else
                    {
                        this.manager.listener.node.addLog("No more nodes available to transfer block " + this.block.getIndex() + " of file " + this.fileId);
                        break;
                    }
                }
                else
                {
                    this.manager.listener.node.addLog("Sucessfully transfered block " + this.block.getIndex() + " of file " + this.fileId);
                    this.manager.addReceivedBlock(this.block,fileBlock);
                    this.sucess = true;
                }
            }
        } catch (UnknownHostException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
