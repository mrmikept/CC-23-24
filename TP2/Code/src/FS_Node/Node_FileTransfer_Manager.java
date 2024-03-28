package FS_Node;

import FS_Transfer_Protocol.FS_Transfer_Protocol;
import Files.BlockInfo;
import Files.FileInfo;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Node_FileTransfer_Manager implements Runnable
{
    private static final int MAXBLOCKREQUESTS = 5;
    private Map<BlockInfo, List<String>> blockInfoToNode;
    private ConcurrentHashMap<String,Double> nodesLatency;
    private Node_File_Manager fileManager;
    public Node_Listener listener;
    private FileInfo fileInfo;
    private List<Thread> threads;
    private List<BlockInfo> receivedBlocks;
    private ReentrantReadWriteLock.WriteLock writeLock;
    private ReentrantReadWriteLock.ReadLock readLock;

    public Node_FileTransfer_Manager(Map<String,FileInfo> mapinfo, Node_File_Manager fileManager, Node_Listener listener)
    {
        this.blockInfoToNode = new HashMap<>();
        this.nodesLatency = new ConcurrentHashMap<>();
        this.fileManager = fileManager;
        this.listener = listener;
        this.threads = new ArrayList<>();
        this.receivedBlocks = new ArrayList<>();
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        this.writeLock = lock.writeLock();
        this.readLock = lock.readLock();

        for (Map.Entry<String,FileInfo> entry : mapinfo.entrySet())
        {
            if (this.fileInfo == null)
            {
                this.fileInfo = new FileInfo(entry.getValue().getFileName(),entry.getValue().getFileSize(),entry.getValue().getFileHash(),new ArrayList<>(), entry.getValue().getNumberOfBlocks());
            }
            String node = entry.getKey();
            for (BlockInfo blockInfo : entry.getValue().getBlockinfo())
            {
                if (!this.blockInfoToNode.containsKey(blockInfo))
                {
                    this.blockInfoToNode.put(blockInfo,new ArrayList<>());
                }
                this.blockInfoToNode.get(blockInfo).add(node);
            }
            if (this.listener.node.existsFile(this.fileInfo.getFileName()))
            {
                List<BlockInfo> blockInfos = this.listener.node.getFileInfo(this.fileInfo.getFileName()).getBlockinfo();
                if (!blockInfos.isEmpty())
                {
                    for (BlockInfo blockInfo : blockInfos)
                    {
                        this.blockInfoToNode.remove(blockInfo);
                    }
                }
            }
        }
    }

    public double getLantency(String nodeId)
    {
        this.readLock.lock();
        try{
            if (this.nodesLatency.containsKey(nodeId))
            {
                return this.nodesLatency.get(nodeId);
            } else return -1;
        } finally {
            this.readLock.unlock();
        }
    }

    public void putLatency(String nodeId, double latency)
    {
        this.writeLock.lock();
        try {
            this.nodesLatency.put(nodeId,latency);
        } finally {
            this.writeLock.unlock();
        }
    }


    public boolean sendMessageWithRetry(FS_Transfer_Protocol protocol, int tries, long waitTime) throws UnknownHostException, InterruptedException {
        return this.listener.sendMessageWithRetry(protocol,Node_Listener.DEFAULTTRIES,2000);
    }

    public FS_Transfer_Protocol getResponse(String UUID) throws IOException {
        return this.listener.getResponse(UUID);
    }

    public void addReceivedBlock(BlockInfo blockinfo, byte[] blockData) throws IOException {
        this.writeLock.lock();
        try {
            this.receivedBlocks.add(blockinfo);
            Collections.sort(this.receivedBlocks, Comparator.comparingInt(BlockInfo::getIndex));
            this.fileManager.writeBlock(this.fileInfo,blockinfo,blockData);
        } finally {
            this.writeLock.unlock();
        }
    }

    public int getTransferPercentage()
    {
        int totalTransferedSize = 0;
        for (BlockInfo block : this.receivedBlocks)
        {
            totalTransferedSize += block.getSize();
        }
        return (int) ((totalTransferedSize / this.fileInfo.getFileSize()) * 100);
    }

    public String getTransferInfo()
    {
        return "File: " + this.fileInfo.getFileName() + "; Total size: " + this.fileInfo.getFileSize() + "; Transfered percentage: " + this.getTransferPercentage() + "%";
    }

    @Override
    public void run()
    {
        this.listener.node.addLog("Starting transfer of file " + this.fileInfo.getFileName());
        int i = 0;
        for (Map.Entry<BlockInfo, List<String>> entry : this.blockInfoToNode.entrySet())
        {
            Thread thread = new Thread(new Node_Request_Maker(this.fileInfo.getFileName(), entry.getKey(),entry.getValue(),this));
            thread.setName("BlockTransferFile " + this.fileInfo.getFileName() + " index " + entry.getKey().getIndex());
            thread.start();
            this.threads.add(thread);
            if (i >= MAXBLOCKREQUESTS) // Ask for a limited number of blocks at a time.
            {
                for (Thread t : this.threads)
                {
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                this.threads.clear();
                i = 0;
            }
            i++;
        }
        if (this.receivedBlocks.size() == this.fileInfo.getNumberOfBlocks())
        {
            this.listener.node.addLog("Successfully transfered all blocks for file " + this.fileInfo.getFileName() + ".");
        } else this.listener.node.addLog("Couldn't transfer all blocks for file " + this.fileInfo.getFileName() + ", file incomplete!");
    }
}
