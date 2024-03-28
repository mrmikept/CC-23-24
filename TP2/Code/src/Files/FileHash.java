package Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class FileHash
{
    public static String calculateFileHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (DigestInputStream dis = new DigestInputStream(new FileInputStream(file), digest)) {

                byte[] buffer = new byte[(int) file.length()];
                while (dis.read(buffer) != -1) ;

                byte[] hashBytes = digest.digest();
                return bytesToHex(hashBytes);
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getBlockHash(byte[] data, int size) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(data,0,size);
        return bytesToHex(digest.digest());
    }

    static List<BlockInfo> calculateBlockInfo(File file, int blockSize) {
        List<BlockInfo> blockInfos = new ArrayList<>();
        int blockIndex = 0;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[blockSize];

            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {

                String blockHash = FileHash.getBlockHash(buffer,bytesRead);

                blockInfos.add(new BlockInfo(blockIndex, bytesRead, blockHash));
                blockIndex++;
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return blockInfos;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexStringBuilder = new StringBuilder();
        for (byte b : bytes) {
            hexStringBuilder.append(String.format("%02x", b));
        }
        return hexStringBuilder.toString();
    }


}
