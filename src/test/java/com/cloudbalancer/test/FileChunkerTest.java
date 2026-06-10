package com.cloudbalancer.test;

import com.cloudbalancer.security.FileEncryptor;
import com.cloudbalancer.service.FileChunker;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileChunkerTest {

    @Test
    void testChunkAndReassemble() throws Exception {
        SecretKey key = FileEncryptor.generateKey();
        byte[] iv = FileEncryptor.generateIV();
        byte[] original = new byte[600 * 1024];
        new java.util.Random(42).nextBytes(original);

        List<FileChunker.ChunkData> chunks = FileChunker.chunkAndEncrypt(original, key, iv);
        assertEquals(3, chunks.size());

        byte[] reassembled = FileChunker.reassembleAndDecrypt(chunks, key, iv);
        assertArrayEquals(original, reassembled);
    }

    @Test
    void testCRC32ValidationFailure() throws Exception {
        SecretKey key = FileEncryptor.generateKey();
        byte[] iv = FileEncryptor.generateIV();
        byte[] original = "Some test data".getBytes();

        List<FileChunker.ChunkData> chunks = FileChunker.chunkAndEncrypt(original, key, iv);
        FileChunker.ChunkData corrupted = chunks.get(0);
        corrupted.getData()[0] ^= 0xFF;

        assertThrows(java.io.IOException.class,
            () -> FileChunker.reassembleAndDecrypt(chunks, key, iv));
    }
}
