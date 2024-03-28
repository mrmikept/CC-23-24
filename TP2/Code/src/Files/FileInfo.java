package Files;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileInfo implements Serializable
{
    private String fileName;
    private long fileSize;
    private String fileHash;
    private List<BlockInfo> blockinfo;
    private int numberOfBlocks;
    public final static int BLOCKSIZE = 1024;

    public FileInfo(String filename, long size, String hash, List<BlockInfo> blocks, int numberOfBlocks) {
        this.fileName = filename;
        this.fileSize = size;
        this.fileHash = hash;
        this.blockinfo = new ArrayList<>();
        for (BlockInfo block : blocks)
        {
            this.blockinfo.add(block.clone());
        }
        Collections.sort(this.blockinfo);
        this.numberOfBlocks = numberOfBlocks;
    }

    public FileInfo(FileInfo fileInfo)
    {
        this.fileName = fileInfo.getFileName();
        this.fileSize = fileInfo.getFileSize();
        this.fileHash = fileInfo.getFileHash();
        this.blockinfo = fileInfo.getBlockinfo();
        this.numberOfBlocks = fileInfo.getNumberOfBlocks();
    }

    public FileInfo(File file) throws IOException, NoSuchAlgorithmException {
        this.fileName = file.getName();
        this.fileSize = file.length();
        this.fileHash = FileHash.calculateFileHash(file);
        this.blockinfo = FileHash.calculateBlockInfo(file, BLOCKSIZE);
        Collections.sort(this.blockinfo);
        this.numberOfBlocks = this.blockinfo.size();
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public void setNumberOfBlocks(int numberOfBlocks) {
        this.numberOfBlocks = numberOfBlocks;
    }

    public void addBlock(BlockInfo blockInfo)
    {
        this.blockinfo.add(blockInfo);
        Collections.sort(this.blockinfo);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public List<BlockInfo> getBlockinfo() {
        List<BlockInfo> list = new ArrayList<>();
        for (BlockInfo block : this.blockinfo)
        {
            list.add(block.clone());
        }
        return list;
    }

    public void setBlockinfo(List<BlockInfo> blockinfo) {
        this.blockinfo = new ArrayList<>();
        for (BlockInfo block : blockinfo)
        {
            this.blockinfo.add(block.clone());
        }
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", fileHash='" + fileHash + '\'' +
                ", blockinfo=" + blockinfo.toString() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return this.fileName.equals(fileInfo.getFileName()) &&
                this.fileSize == fileInfo.getFileSize() &&
                this.fileHash.equals(fileInfo.getFileHash());
    }

    public FileInfo clone()
    {
        return new FileInfo(this);
    }

}
