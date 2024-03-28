package DataUtilities;

import Files.FileInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

public class Serializer {

    @SuppressWarnings("unchecked")
    public static List<FileInfo> byteArrayToArrayListFileInfo(byte[] files) {
        List<FileInfo> fileArray = null;
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(files);
            ObjectInputStream dataInputStream = new ObjectInputStream(byteArrayInputStream);
            fileArray = (ArrayList<FileInfo>) dataInputStream.readObject();
            byteArrayInputStream.close();
            dataInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return fileArray;
    }

    @SuppressWarnings("unchecked")
    public static List<String> byteArrayToArrayListString(byte[] files) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(files);
            ObjectInputStream dataInputStream = new ObjectInputStream(byteArrayInputStream);
            List<String> fileArray = (ArrayList<String>) dataInputStream.readObject();
            byteArrayInputStream.close();
            dataInputStream.close();

            return fileArray;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] serializableFileInfoList(List<FileInfo> fileInfoList){
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(fileInfoList);

            bos.close();
            out.close();
            return bos.toByteArray();
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] serializableStringList(List<String> fileInfoList){
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(fileInfoList);

            bos.close();
            out.close();
            return bos.toByteArray();
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] serializableMapStringFileInfo(Map<String,FileInfo> list)
    {
        try
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(list);
            bos.close();
            out.close();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String,FileInfo> serializableByteArrayToMapNodeFileInfo(byte[] data)
    {
        try {
            if (data.equals("NoFiles".getBytes()))
            {
                return null;
            }
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            ObjectInputStream dataInputStream = new ObjectInputStream(byteArrayInputStream);
            Map<String,FileInfo> map = (Map<String,FileInfo>) dataInputStream.readObject();
            byteArrayInputStream.close();
            dataInputStream.close();

            return map;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
