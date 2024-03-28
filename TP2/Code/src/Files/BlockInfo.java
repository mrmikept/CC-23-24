package Files;

import java.io.Serializable;
import java.util.Objects;

public class BlockInfo implements Serializable, Comparable<BlockInfo> {
    private int index;
    private int size;
    private String hashValue;

    public BlockInfo(int index, int size, String hashValue) {
        this.index = index;
        this.size = size;
        this.hashValue = hashValue;
    }

    public BlockInfo(BlockInfo blockInfo)
    {
        this.index = blockInfo.getIndex();
        this.size = blockInfo.getSize();
        this.hashValue = blockInfo.getHashValue();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getHashValue() {
        return hashValue;
    }

    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockInfo blockInfo = (BlockInfo) o;
        return this.getIndex() == blockInfo.getIndex() && this.getSize() == blockInfo.getSize() && this.getHashValue().equals(blockInfo.getHashValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, size, hashValue);
    }

    @Override
    public String toString() {
        return "BlockInfo{" +
                "index=" + index +
                ", size=" + size +
                ", hashValue='" + hashValue + '\'' +
                '}';
    }

    public BlockInfo clone()
    {
        return new BlockInfo(this);
    }

    @Override
    public int compareTo(BlockInfo o) {
        return Integer.compare(this.index,o.index);
    }
}
