package com.cloudbalancer.service;

import com.cloudbalancer.security.FileEncryptor;
import javax.crypto.SecretKey;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class FileChunker {

    private static final int CHUNK_SIZE = 256 * 1024;

    public static List<ChunkData> chunkAndEncrypt(byte[] fileData, SecretKey key, byte[] iv)
            throws Exception {
        List<ChunkData> chunks = new ArrayList<>();
        int totalChunks = (int) Math.ceil((double) fileData.length / CHUNK_SIZE);

        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, fileData.length);
            byte[] chunkBytes = new byte[end - start];
            System.arraycopy(fileData, start, chunkBytes, 0, end - start);

            byte[] encrypted = FileEncryptor.encrypt(chunkBytes, key, iv);

            CRC32 crc = new CRC32();
            crc.update(encrypted);
            chunks.add(new ChunkData(i, encrypted, crc.getValue()));
        }

        return chunks;
    }

    public static byte[] reassembleAndDecrypt(List<ChunkData> chunks, SecretKey key, byte[] iv)
            throws Exception {
        chunks.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (ChunkData chunk : chunks) {
            CRC32 crc = new CRC32();
            crc.update(chunk.getData());
            if (crc.getValue() != chunk.getChecksum()) {
                throw new IOException("CRC32 validation failed for chunk " + chunk.getIndex());
            }
            output.write(FileEncryptor.decrypt(chunk.getData(), key, iv));
        }

        return output.toByteArray();
    }

    public static class ChunkData {
        private final int index;
        private final byte[] data;
        private final long checksum;

        public ChunkData(int index, byte[] data, long checksum) {
            this.index = index;
            this.data = data;
            this.checksum = checksum;
        }

        public int getIndex() { return index; }
        public byte[] getData() { return data; }
        public long getChecksum() { return checksum; }
    }
}
