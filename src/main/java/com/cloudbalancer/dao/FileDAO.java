package com.cloudbalancer.dao;

import com.cloudbalancer.database.LocalDatabase;
import com.cloudbalancer.database.RemoteDatabase;
import com.cloudbalancer.model.FileMetadata;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileDAO {
    private final LocalDatabase localDb;
    private final RemoteDatabase remoteDb;

    public FileDAO(LocalDatabase localDb, RemoteDatabase remoteDb) {
        this.localDb = localDb;
        this.remoteDb = remoteDb;
    }

    public FileMetadata createFile(String filename, int ownerId, long fileSize, int totalChunks) {
        String sql = "INSERT INTO files (filename, owner_id, file_size, total_chunks) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = localDb.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, filename);
            pstmt.setInt(2, ownerId);
            pstmt.setLong(3, fileSize);
            pstmt.setInt(4, totalChunks);
            pstmt.executeUpdate();
            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                if (remoteDb.isConnected()) mirrorCreate(id, filename, ownerId, fileSize, totalChunks);
                return new FileMetadata(id, filename, ownerId, fileSize, totalChunks, null, null);
            }
        } catch (SQLException e) {
            System.err.println("Error creating file record: " + e.getMessage());
        }
        return null;
    }

    private void mirrorCreate(int id, String filename, int ownerId, long fileSize, int totalChunks) {
        String sql = "INSERT IGNORE INTO files (id, filename, owner_id, file_size, total_chunks) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = remoteDb.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, filename);
            pstmt.setInt(3, ownerId);
            pstmt.setLong(4, fileSize);
            pstmt.setInt(5, totalChunks);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error mirroring file to MySQL: " + e.getMessage());
        }
    }

    public List<FileMetadata> getFilesByOwner(int ownerId) {
        List<FileMetadata> files = new ArrayList<>();
        String sql = "SELECT * FROM files WHERE owner_id = ?";
        try (PreparedStatement pstmt = localDb.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, ownerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) files.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error fetching files: " + e.getMessage());
        }
        return files;
    }

    public List<FileMetadata> getSharedFiles(int userId) {
        List<FileMetadata> files = new ArrayList<>();
        String sql = """
            SELECT f.* FROM files f
            JOIN file_permissions fp ON f.id = fp.file_id
            WHERE fp.user_id = ?
            """;
        try (PreparedStatement pstmt = localDb.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) files.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Error fetching shared files: " + e.getMessage());
        }
        return files;
    }

    public void deleteFile(int fileId) {
        String sql = "DELETE FROM files WHERE id = ?";
        try (PreparedStatement pstmt = localDb.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, fileId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting file: " + e.getMessage());
        }
        if (remoteDb.isConnected()) {
            try (PreparedStatement pstmt = remoteDb.getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, fileId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Error deleting file from MySQL: " + e.getMessage());
            }
        }
    }

    public void shareFile(int fileId, int userId, String permission, int grantedBy) {
        String sql = "INSERT INTO file_permissions (file_id, user_id, permission, granted_by) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = localDb.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, fileId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, permission);
            pstmt.setInt(4, grantedBy);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error sharing file: " + e.getMessage());
        }
    }

    private FileMetadata mapRow(ResultSet rs) throws SQLException {
        return new FileMetadata(
            rs.getInt("id"),
            rs.getString("filename"),
            rs.getInt("owner_id"),
            rs.getLong("file_size"),
            rs.getInt("total_chunks"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        );
    }
}
