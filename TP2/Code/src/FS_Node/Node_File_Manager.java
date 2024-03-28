package FS_Node;

import Files.BlockInfo;
import Files.FileInfo;
import DataUtilities.Serializer;
import FS_Tracker_Protocol.FS_Tracker_Protocol;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class Node_File_Manager implements Runnable
{
    private static String TMPFOLDER = "/tmp/FSCache/";
    private FS_Node node;
    private File sharedFolder;
    private ConcurrentHashMap<String, FileInfo> files;
    private ReentrantReadWriteLock lock;
    private ReentrantReadWriteLock.ReadLock readLock;
    private ReentrantReadWriteLock.WriteLock writeLock;


    public Node_File_Manager(FS_Node node, ConcurrentHashMap<String, FileInfo> filesMap)
    {
        this.node = node;
        this.sharedFolder = new File(this.node.getFolderPath());
        this.files = filesMap;
        this.lock = new ReentrantReadWriteLock();
        this.readLock = this.lock.readLock();
        this.writeLock = this.lock.writeLock();
    }


    public void readFolder() throws IOException, NoSuchAlgorithmException {
        this.node.addLog("Reading folder...");
        File[] filesList = this.sharedFolder.listFiles();
        if (filesList != null)
        {
            for (File file : filesList)
            {
                if (file.isFile())
                {
                    this.node.addFile(new FileInfo(file));
                }
            }
        } else this.node.addLog("No files found in folder...");
    }

    public void updateFolder() throws IOException, NoSuchAlgorithmException
    {
        List<FileInfo> knwonFiles = this.node.getFileList().stream().map(FileInfo::clone).collect(Collectors.toList());
        List<File> folderInfo = Arrays.asList(this.sharedFolder.listFiles());

        for (File file : folderInfo)
        {
            if (!this.node.existsFile(file.getName()))
            {
                this.node.addLog("New file \"" + file.getName() + "\"");
                FileInfo info = new FileInfo(file);
                this.node.addFile(info);
            }
            else
            {
                knwonFiles.remove(this.node.getFileInfo(file.getName()));
            }
        }

        if (!knwonFiles.isEmpty())
        {
            for (FileInfo file : knwonFiles)
            {
                this.node.addLog("Deleted file \"" + file.getFileName() + "\"");
                this.node.removeFile(file.getFileName());
            }
        }
    }

    public byte[] getBlock(String fileId, int blockIndex) throws IOException {
        try
        {
            this.readLock.lock();
            if (this.files.containsKey(fileId))
            {
                Map<Integer, byte[]> fileBlocks = this.getFileBlocks(fileId);
                if (fileBlocks.containsKey(blockIndex))
                {
                    return fileBlocks.get(blockIndex).clone();
                }
            }
            return null;
        } finally {
            this.readLock.unlock();
        }
    }

    public Map<Integer, byte[]> getFileBlocks(String fileId) throws IOException {
        try {
            this.readLock.lock();
            Map<Integer, byte[]> receivedBlocks = new HashMap<>();
            FileInfo info = this.files.get(fileId).clone();
            List<BlockInfo> knownBlocks = info.getBlockinfo();
            Collections.sort(knownBlocks);

            byte[] fileBytes = Files.readAllBytes(Path.of(this.node.getFolderPath() + info.getFileName()));
            int startIndex = 0;

            for (BlockInfo blockInfo : knownBlocks)
            {
                byte[] blockData = new byte[blockInfo.getSize()];
                System.arraycopy(fileBytes,startIndex,blockData,0,blockData.length);
                receivedBlocks.put(blockInfo.getIndex(),blockData);
                startIndex += blockInfo.getSize();
            }
            return receivedBlocks;
        } finally {
            this.readLock.unlock();
        }
    }

    public void writeBlock(FileInfo info, BlockInfo blockInfo, byte[] block) throws IOException {
        try
        {
            this.writeLock.lock();
            File file = new File(this.node.getFolderPath() + info.getFileName());
                if (!this.files.containsKey(info.getFileName()))
                {
                    info.addBlock(blockInfo);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(block);
                    this.files.put(info.getFileName(),info);
                    this.node.updateTrackerFile(info.getFileName());
                }
                else
                {
                    //this.files.put(info.getFileName(),info);
                    Map<Integer, byte[]> receivedBlocks = this.getFileBlocks(info.getFileName());
                    receivedBlocks.put(blockInfo.getIndex(), block);
                    Map<Integer, byte[]> sortedMapBlock = new TreeMap<>(receivedBlocks);

                    File fileCache = new File("/tmp/FSCache/" + InetAddress.getLocalHost().getHostName() + "/" + info.getFileName() + "Cache");
                    FileOutputStream fos = new FileOutputStream("/tmp/FSCache/" + InetAddress.getLocalHost().getHostName() + "/" + info.getFileName() + "Cache", true);
                    for (byte[] blockData : sortedMapBlock.values())
                    {
                        fos.write(blockData);
                    }
                    fos.close();

                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(new FileInputStream(fileCache).readAllBytes());
                    fileOutputStream.close();
                    fileCache.delete();
                    this.files.get(info.getFileName()).addBlock(blockInfo);
                    this.node.updateTrackerFile(info.getFileName());
                }
        } finally {
            this.writeLock.unlock();
        }
    }

    private void createTmpFolder() throws IOException {
        File cache = new File(TMPFOLDER);
        if (!cache.exists())
        {
            if (cache.mkdir())
            {
                this.node.addLog("Created a new folder in " + cache.getPath());
            } else
            {
                System.out.println("Couldn't create temp folder in " + cache.getPath() + "\nAborting Program...");
                this.node.stopProgram();
            }
        } else this.node.addLog("Found FS temporary folder in " + cache.getPath());
        File nodeTempFolder = new File(TMPFOLDER + InetAddress.getLocalHost().getHostName() + "/");
        if (!nodeTempFolder.exists())
        {
            if (nodeTempFolder.mkdir())
            {
                this.node.addLog("Created folder for this node in temporary directory in " + nodeTempFolder.getPath());
            }
            else
            {
                System.out.println("Couldn't create folder in temporary direction for this Node in " + nodeTempFolder.getPath() + "\nAborting Program...");
                this.node.stopProgram();
            }
        } else this.node.addLog("Found Node temporary folder in " + nodeTempFolder.getPath());
    }

    @Override
    public void run()
    {
        try {
            this.node.addLog("Starting Node File Manager.");
            this.createTmpFolder();
            if (this.files.isEmpty())
            {
                this.node.addLog("Reading folder...");
                this.readFolder();
            }
            else
            {
                this.node.addLog("Updating File's Information...");
                this.updateFolder();
            }
            this.node.sendFolder();
        } catch (Exception e)
        {
            e.printStackTrace();
        }


    }
}
