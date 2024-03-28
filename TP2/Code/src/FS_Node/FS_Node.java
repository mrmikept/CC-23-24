package FS_Node;

import Files.BlockInfo;
import Files.FileInfo;
import DataUtilities.Serializer;
import FS_Tracker_Protocol.FS_Tracker_Protocol;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FS_Node
{
    private static final String BACKUPPATH = "/var/Backups/FSNode/";
    private Node_TrackerConnector connector;
    private String nodeId;
    private String folderPath;
    private ConcurrentHashMap<String, FileInfo> files;
    private List<String> logs;
    private List<Node_FileTransfer_Manager> fileTransfers;
    public Node_Listener nodeListener;
    public List<Thread> threads;
    private ReentrantReadWriteLock.WriteLock writeLock;
    private ReentrantReadWriteLock.ReadLock readLock;
    private boolean isOnline;

    public FS_Node(Node_TrackerConnector connector, String folderPath) throws UnknownHostException {
        this.connector = connector;
        this.nodeId = InetAddress.getLocalHost().getHostName();
        this.folderPath = folderPath;
        this.files = new ConcurrentHashMap<>();
        this.logs = new ArrayList<>();
        this.fileTransfers = new ArrayList<>();
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        this.writeLock = lock.writeLock();
        this.readLock = lock.readLock();
        this.threads = new ArrayList<>();
        this.isOnline = true;
    }

    public void setNodeListener(Node_Listener listener)
    {
        this.nodeListener = listener;
    }

    public String getTrackerAddr()
    {
        return this.connector.getTrackerAddr();
    }

    public int getPort()
    {
        return this.connector.getPort();
    }

    public void createBackupFolder() {
        File backupFolder = new File(BACKUPPATH + this.nodeId + "/");
        if (!backupFolder.exists())
        {
            backupFolder.mkdirs();
            this.addLog("Backup folder created!");
        } else this.addLog("Backup folder found!");
    }

    public void saveLogs() {
        try {
            File backupFile = new File(BACKUPPATH + this.nodeId + "/logs.txt");
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

    public List<String> getLogs()
    {
        this.readLock.lock();
        try {
            List<String> logs = new ArrayList<>();
            for (String log : this.logs)
            {
                logs.add(log);
            }
            return logs;
        } finally {
            this.readLock.unlock();
        }
    }

    public void readConfig()
    {
        File configFile = new File(BACKUPPATH + this.nodeId + "/config.obj");
        if (configFile.exists())
        {
            try {
                FileInputStream fis = new FileInputStream(configFile);
                ObjectInputStream inputStream = new ObjectInputStream(fis);
                this.files = (ConcurrentHashMap<String, FileInfo>) inputStream.readObject();
                fis.close();
                inputStream.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else this.addLog("No configuration file found.");
    }

    public void saveConfig()
    {
        File configFile = new File(BACKUPPATH + this.nodeId + "/config.obj");
        this.addLog("Saving configuration...");
        try{
            FileOutputStream fos = new FileOutputStream(configFile);
            ObjectOutputStream outputStream = new ObjectOutputStream(fos);
            outputStream.writeObject(this.files);
            this.addLog("Configuration saved!");
            fos.close();
            outputStream.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getFileTransfers()
    {
        this.readLock.lock();
        try
        {
            List<String> list = new ArrayList<>();
            for (Node_FileTransfer_Manager transferManager : this.fileTransfers)
            {
                list.add(transferManager.getTransferInfo());
            }
            return list;
        } finally {
            this.readLock.unlock();
        }
    }

    public String getFolderPath()
    {
        return this.folderPath;
    }

    public void addFile(FileInfo file)
    {
        this.writeLock.lock();
        try {
            this.addLog("Adding file " + file.getFileName());
            this.files.put(file.getFileName(),file);
        } finally {
            this.writeLock.unlock();
        }
    }

    public void removeFile(String fileName)
    {
        this.writeLock.lock();
        try
        {

            this.addLog("Removing file " + fileName);
            this.files.remove(fileName);
        } finally {
            this.writeLock.unlock();
        }
    }

    public void sendFolder() throws IOException {
        this.addLog("Sending files information in shared folder.");
        if (!this.files.isEmpty())
        {
            List<FileInfo> files = new ArrayList<>();
            for (FileInfo file : this.files.values())
            {
                this.addLog("Send information of file \"" + file.getFileName() + "\"");
                files.add(file.clone());
            }
            byte[] filesData = Serializer.serializableFileInfoList(files);
            this.sendMessage(FS_Tracker_Protocol.NODE_FILES_LIST,filesData);
        } else this.addLog("No files found to send.");
    }

    public FileInfo getFileInfo(String fileId)
    {
        this.readLock.lock();
        try
        {
            if (this.files.containsKey(fileId))
            {
                return this.files.get(fileId);
            } else return null;
        } finally {
            this.readLock.unlock();
        }
    }

    public List<FileInfo> getFileList()
    {
        this.readLock.lock();
        try
        {
            List<FileInfo> files = new ArrayList<>();
            for (FileInfo file : this.files.values())
            {
                files.add(file.clone());
            }
            return files;
        } finally {
            this.readLock.unlock();
        }
    }

    public boolean existsFile(String filename)
    {
        return this.files.containsKey(filename);
    }

    public void updateTrackerFile(String fileId) throws IOException {
        this.readLock.lock();
        try
        {
            if (this.files.containsKey(fileId))
            {
                FileInfo fileInfo = this.files.get(fileId);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(fileInfo);
                byte[] data = baos.toByteArray();
                this.connector.sendMessage(FS_Tracker_Protocol.UPDATE_FILE,data);
                baos.close();
                oos.close();
            }
        } finally {
            this.readLock.unlock();
        }
    }

    public void sendMessage(int messagetype, byte[] data) throws IOException
    {
        this.connector.sendMessage(messagetype,data);
    }

    public byte[] sendMessageAndWaitResponse(int messageType, byte[] data) throws IOException, InterruptedException {
        return this.connector.sendMessageAndWaitResponse(messageType,data);
    }

    public void transferFile(Map<String,FileInfo> mapInfo)
    {
        Node_FileTransfer_Manager manager = new Node_FileTransfer_Manager(mapInfo,this.nodeListener.fileManager, this.nodeListener);
        this.addLog("Starting transfer for a file...");
        Thread thread = new Thread(manager);
        thread.setName("FileTransfer File");
        thread.start();
        this.threads.add(thread);
        this.fileTransfers.add(manager);
    }

    public boolean isOnline()
    {
        return this.isOnline;
    }

    public void addThread(Thread thread)
    {
        this.threads.add(thread);
    }

    public void stopThreads() throws InterruptedException {
        for (Thread thread : this.threads)
        {
            System.out.println("Waiting for thread " + thread.getName());
            thread.join();
            System.out.println("Done");
        }
    }

    public void stopProgram() throws IOException {
        this.sendMessage(FS_Tracker_Protocol.CLOSE_CONNECTION,"Bye!!!! :)".getBytes());
        this.isOnline = false;
        this.connector.stopConnection();
//        this.saveLogs();
//        this.saveConfig();
    }

    public void startServices() throws IOException {
        this.createBackupFolder();
        this.readConfig();
        Thread trackerListener = new Thread(this.connector);
        trackerListener.setName("TrackerListener");
        trackerListener.start();
        this.addThread(trackerListener);

        Node_File_Manager fileManager = new Node_File_Manager(this, this.files);
        Thread fileManagerThread = new Thread(fileManager);
        fileManagerThread.setName("Node_FileManager");
        fileManagerThread.start();
        this.addThread(fileManagerThread);

//        Thread menuInterface = new Thread(new Node_Interface(this));
//        menuInterface.setName("Node_Interface");
//        menuInterface.start();
//        this.addThread(menuInterface);

        Node_Listener listener = new Node_Listener(this, fileManager);
        Thread nodeListener = new Thread(listener);
        nodeListener.setName("Node Listener Thread");
        nodeListener.start();
        this.addThread(nodeListener);
        this.setNodeListener(listener);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 2)
        {
            System.out.println("Error, not enough arguments!\nInclude the path to the shared folder, the address to the FS_Tracker Server and optionally the port to the connection(Default value 9090)");
            return;
        }
        String shared_folder = args[0];
        File sharedFolder = new File(shared_folder);
        if (!sharedFolder.exists())
        {
            System.out.println("Couldn't find Shared Folder in " + shared_folder + ", closing program...");
            return;
        }
        String fstracker_addr = args[1];

        int port = 9090;

        if (args.length == 3)
        {
            try {
                port = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException e)
            {
                System.out.println("ERROR: " + e.getMessage());
                System.out.println("Using default port 9090.");
            }
        }

        FS_Node node = new FS_Node(new Node_TrackerConnector(fstracker_addr, InetAddress.getLocalHost().getHostAddress(), port), shared_folder);
        try {
            node.startServices();
            Node_Interface nodeInterface = new Node_Interface(node);
            nodeInterface.navigator();
        } finally {
            node.stopProgram();
            node.saveConfig();
            node.saveLogs();
        }

    }

}
