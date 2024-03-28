package FS_Tracker;

import DataUtilities.UniqueIdGenerator;
import FS_Tracker_Protocol.FS_Tracker_Protocol;
import Files.BlockInfo;
import Files.FileInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

public class FS_Tracker
{
    private final static String BACKUPPATH = "/var/Backups/FSTracker/";
    private ConcurrentHashMap<String, List<FileInfo>> nodesTable;
    private String trackerId;
    private ArrayList<String> logs;
    private int port;
    private ReentrantReadWriteLock.ReadLock readLock;
    private ReentrantReadWriteLock.WriteLock writeLock;
    private boolean serverActive;

    public FS_Tracker() throws UnknownHostException {
        this.nodesTable = new ConcurrentHashMap<>();
        this.trackerId = InetAddress.getLocalHost().getHostAddress();
        this.logs = new ArrayList<>();
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    public ConcurrentHashMap<String, ArrayList<FileInfo>> getNodesTable()
    {
        this.readLock.lock();
        try
        {
            if (this.nodesTable.isEmpty())
            {
                return new ConcurrentHashMap<>();
            }
            ConcurrentHashMap<String, ArrayList<FileInfo>> map = new ConcurrentHashMap<>();
            for (Map.Entry<String, List<FileInfo>> entry : this.nodesTable.entrySet())
            {
                ArrayList<FileInfo> list = new ArrayList<>();
                for (FileInfo file : entry.getValue())
                {
                    list.add(file.clone());
                }
                map.put(entry.getKey(),list);
            }
            return map;
        } finally {
            this.readLock.unlock();
        }
    }

    public void updateFileInfo(String nodeId, FileInfo fileInfo)
    {
        this.writeLock.lock();
        try
        {
            if (this.nodesTable.containsKey(nodeId))
            {
                this.nodesTable.get(nodeId).removeIf(file -> file.getFileName().equals(fileInfo.getFileName()));
                this.nodesTable.get(nodeId).add(fileInfo);
                this.addLog("Updated information of file " + fileInfo.getFileName() + " in node " + nodeId);
            } else this.addLog("Couldn't find node " + nodeId + " to update file " + fileInfo.getFileName());
        } finally {
            this.writeLock.unlock();
        }

    }

    public List<String> getConnectedNodes()
    {
        this.readLock.lock();
        try {
            return this.getNodesTable().entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
        } finally {
            this.readLock.unlock();
        }
    }

    public List<String> getNodesWithFile(String fileId) {
        this.readLock.lock();
        try {
            List<String> list = new ArrayList<>();
            ConcurrentHashMap<String,ArrayList<FileInfo>> map = this.getNodesTable();
            for (Map.Entry<String, ArrayList<FileInfo>> entry : map.entrySet())
            {
                ArrayList<FileInfo> nodefile = entry.getValue();
                for (FileInfo fileInfo : nodefile)
                {
                    if (fileInfo.getFileName().equals(fileId))
                    {
                        StringBuilder string = new StringBuilder();
                        string.append(entry.getKey());
                        string.append("; ");
                        string.append("Number of blocks: " + fileInfo.getBlockinfo().size());
                        list.add(string.toString());
                        break;
                    }
                }
            }
            return list;
        } finally {
            this.readLock.unlock();
        }
    }

    public Map<String,FileInfo> getFileLocation(String filename)
    {
        this.readLock.lock();
        try {
            Map<String,FileInfo> map = new HashMap<>(); // Key: NodeAddr; Value: List of blockinfo
            for (Map.Entry<String, ArrayList<FileInfo>> entry : this.getNodesTable().entrySet())
            {
                String nodeAddr = entry.getKey();
                List<FileInfo> nodeFiles = entry.getValue();
                for (FileInfo file : nodeFiles)
                {
                    if (file.getFileName().equals(filename))
                    {
                        map.put(nodeAddr,file.clone());
                        break;
                    }
                }
            }
            return map;
        } finally {
            this.readLock.unlock();
        }
    }

    public void setServerActive(boolean bool)
    {
        this.serverActive = bool;
    }

    public boolean getServerActive()
    {
        return this.serverActive;
    }

    public int getPort()
    {
        return this.port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void addNode(String nodeAddr)
    {
        this.writeLock.lock();
        try {

            if (!this.nodesTable.containsKey(nodeAddr))
            {
                this.nodesTable.put(nodeAddr, new ArrayList<>());
                this.addLog("Node " + nodeAddr + " added sucessufly!");
            } else this.addLog("Node " + nodeAddr + " is already known...");
        } finally {
            this.writeLock.unlock();
        }
    }

    public void removeNode(String nodeAddr)
    {
        this.writeLock.lock();
        try {
            if (this.nodesTable.containsKey(nodeAddr))
            {
                this.nodesTable.remove(nodeAddr);
                this.addLog("Node " + nodeAddr + " removed sucessfully!");
            } else this.addLog("Couldn't remove Node " + nodeAddr);
        } finally {
            this.writeLock.unlock();
        }
    }


    public void addFileToNode(String nodeAddr, List<FileInfo> files)
    {
        this.writeLock.lock();
        try {

            if (this.nodesTable.containsKey(nodeAddr))
            {
                for (FileInfo file : files)
                {
                    if (!this.nodesTable.get(nodeAddr).contains(file))
                    {
                        this.nodesTable.get(nodeAddr).add(file);
                        this.addLog("Added file " + file.getFileName() + " to node " + nodeAddr);
                    } else this.addLog("Node " + nodeAddr + " already has the file " + file.getFileName());
                }
            } else this.addLog("Couldn't add files to node " + nodeAddr + ", node not found!");
        } finally {
            this.writeLock.unlock();
        }
    }

    public void RemoveFileFromNode(String nodeAddr, List<FileInfo> files)
    {
        this.writeLock.lock();
        try {
            if (this.nodesTable.containsKey(nodeAddr))
            {
                for (FileInfo file : files)
                {
                    if (this.nodesTable.get(nodeAddr).contains(file))
                    {
                        this.nodesTable.get(nodeAddr).remove(file);
                        this.addLog("Removed file " + file.getFileName() + " from node " + nodeAddr);
                    } else this.addLog("Node " + nodeAddr + " don't contain the file " + file.getFileName());
                }

            } else this.addLog("Couldn't remove files from node " + nodeAddr + ", node not found!");
        } finally {
            this.writeLock.unlock();
        }

    }

    public void addNodeFilesList(String nodeAddr, List<FileInfo> fileInfos)
    {
        this.writeLock.lock();
        try {
            if (this.nodesTable.containsKey(nodeAddr))
            {
                this.nodesTable.put(nodeAddr,fileInfos);
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    public ArrayList<FileInfo> getAllFileList(String nodeAddr)
    {
        this.readLock.lock();
        try {
            ArrayList<FileInfo> fileList = new ArrayList<>();
            for (ArrayList<FileInfo> nodeFiles : this.getNodesTable().entrySet().stream().filter(entry -> !entry.getKey().equals(nodeAddr)).map(Map.Entry::getValue).collect(Collectors.toList()))
            {
                for (FileInfo file : nodeFiles)
                {
                    if (!fileList.contains(file))
                    {
                        fileList.add(file);
                    }
                }
            }
            return fileList;
        } finally {
            this.readLock.unlock();
        }
    }

    public List<String> getLogs()
    {
        this.readLock.lock();
        try {
            return this.logs.stream().collect(Collectors.toCollection(ArrayList::new));
        } finally {
            this.readLock.unlock();
        }
    }

    public void createBackupFolder() {
        File backupFolder = new File(BACKUPPATH + this.trackerId + "/");
        if (!backupFolder.exists())
        {
            backupFolder.mkdirs();
            this.addLog("Backup folder created!");
        } else this.addLog("Backup folder found!");
    }

    public void saveLogs() {
        try {
            File backupFile = new File(BACKUPPATH + this.trackerId + "/logs.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile,true));
            for (String log : this.logs)
            {
                writer.write(log);
                writer.newLine();
            }
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            e.getMessage();
        }
    }

    public void addLog(String log)
    {
        this.writeLock.lock();
        try {
            if (this.logs.size() > 30)
            {
                this.saveLogs();
                this.logs.clear();
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            this.logs.add(LocalDateTime.now().format(formatter) +  " - " + log);
        } finally {
            this.writeLock.unlock();
        }

    }

    public static void main(String[] args) throws IOException, InterruptedException, NoSuchAlgorithmException {
        //InetAddress hostIP = InetAddress.getLocalHost();
        int TCP_Port = 9090;
        if (args.length != 0)
        {
            try {
                TCP_Port = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException e)
            {
                System.out.println("ERROR: " + e.getMessage());
                System.out.println("Using default port 9090.");
            }
        }
        FS_Tracker tracker = new FS_Tracker();
        tracker.createBackupFolder();
        TrackerConnection_Manager ConnectionManager = new TrackerConnection_Manager(tracker, TCP_Port);
        Thread connectionManager = new Thread(ConnectionManager);
        Thread TrackerInterface = new Thread(new Tracker_Interface(tracker));
        try {
            connectionManager.start();
            TrackerInterface.start();
        } finally {
            TrackerInterface.join();
            tracker.serverActive = false;
            connectionManager.join();
            tracker.saveLogs();
        }
    }
}
