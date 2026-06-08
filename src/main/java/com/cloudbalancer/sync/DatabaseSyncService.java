package com.cloudbalancer.sync;

import com.cloudbalancer.database.LocalDatabase;
import com.cloudbalancer.database.RemoteDatabase;
import java.sql.*;

public class DatabaseSyncService {
    private final LocalDatabase localDb;
    private final RemoteDatabase remoteDb;

    private static final String[] SYNC_TABLES = {"users", "files", "file_permissions", "event_logs"};

    public DatabaseSyncService(LocalDatabase localDb, RemoteDatabase remoteDb) {
        this.localDb = localDb;
        this.remoteDb = remoteDb;
    }

    public void syncToRemote() {
        if (!remoteDb.isConnected()) {
            System.out.println("MySQL unavailable — sync deferred.");
            return;
        }
        for (String table : SYNC_TABLES) pushPendingRows(table);
    }

    private void pushPendingRows(String table) {
        String query = "SELECT * FROM " + table + " WHERE sync_status = 'pending'";
        try (Statement stmt = localDb.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                upsertToRemote(table, rs);
                markSynced(localDb.getConnection(), table, rs.getInt("id"));
            }
        } catch (SQLException e) {
            System.err.println("Sync push error [" + table + "]: " + e.getMessage());
        }
    }

    private void upsertToRemote(String table, ResultSet localRow) {
        try {
            ResultSetMetaData meta = localRow.getMetaData();
            int cols = meta.getColumnCount();

            StringBuilder columns = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            StringBuilder updates = new StringBuilder();

            for (int i = 1; i <= cols; i++) {
                String col = meta.getColumnName(i);
                if ("sync_status".equals(col)) continue;
                if (columns.length() > 0) { columns.append(", "); placeholders.append(", "); updates.append(", "); }
                columns.append(col);
                placeholders.append("?");
                updates.append(col).append(" = VALUES(").append(col).append(")");
            }

            String sql = "INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders
                + ") ON DUPLICATE KEY UPDATE " + updates;

            try (PreparedStatement pstmt = remoteDb.getConnection().prepareStatement(sql)) {
                int idx = 1;
                for (int i = 1; i <= cols; i++) {
                    if ("sync_status".equals(meta.getColumnName(i))) continue;
                    pstmt.setObject(idx++, localRow.getObject(i));
                }
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Upsert failed: " + e.getMessage());
        }
    }

    public void syncFromRemote() {
        if (!remoteDb.isConnected()) return;
        pullTable("users");
        pullTable("files");
    }

    private void pullTable(String table) {
        String sql = "SELECT * FROM " + table;
        try (Statement stmt = remoteDb.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int remoteId = rs.getInt("id");
                String remoteUpdated = rs.getString("updated_at");

                try (PreparedStatement localPs = localDb.getConnection()
                        .prepareStatement("SELECT updated_at, sync_status FROM " + table + " WHERE id = ?")) {
                    localPs.setInt(1, remoteId);
                    ResultSet localRs = localPs.executeQuery();

                    if (!localRs.next()) {
                        insertLocalFromRemote(table, rs);
                    } else {
                        String localUpdated = localRs.getString("updated_at");
                        String syncStatus   = localRs.getString("sync_status");
                        if ("conflict".equals(syncStatus)) {
                            resolveConflict(table, remoteId, localUpdated, remoteUpdated, rs);
                        } else if (remoteUpdated != null &&
                                (localUpdated == null || remoteUpdated.compareTo(localUpdated) > 0)) {
                            updateLocalFromRemote(table, rs);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Pull error [" + table + "]: " + e.getMessage());
        }
    }

    private void resolveConflict(String table, int id,
                                  String localUpdated, String remoteUpdated, ResultSet remoteRs) {
        try {
            if (remoteUpdated != null && localUpdated != null
                    && remoteUpdated.compareTo(localUpdated) > 0) {
                updateLocalFromRemote(table, remoteRs);
            } else {
                String localSql = "SELECT * FROM " + table + " WHERE id = ?";
                try (PreparedStatement ps = localDb.getConnection().prepareStatement(localSql)) {
                    ps.setInt(1, id);
                    ResultSet localRow = ps.executeQuery();
                    if (localRow.next()) upsertToRemote(table, localRow);
                }
            }
            markSynced(localDb.getConnection(), table, id);
        } catch (SQLException e) {
            System.err.println("Conflict resolution failed: " + e.getMessage());
        }
    }

    private void insertLocalFromRemote(String table, ResultSet remoteRow) {
        try {
            ResultSetMetaData meta = remoteRow.getMetaData();
            int cols = meta.getColumnCount();

            StringBuilder columns = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();

            for (int i = 1; i <= cols; i++) {
                String col = meta.getColumnName(i);
                if ("sync_status".equals(col)) continue;
                if (columns.length() > 0) { columns.append(", "); placeholders.append(", "); }
                columns.append(col);
                placeholders.append("?");
            }

            String sql = "INSERT OR IGNORE INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";
            try (PreparedStatement pstmt = localDb.getConnection().prepareStatement(sql)) {
                int idx = 1;
                for (int i = 1; i <= cols; i++) {
                    if ("sync_status".equals(meta.getColumnName(i))) continue;
                    pstmt.setObject(idx++, remoteRow.getObject(i));
                }
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Insert from remote failed: " + e.getMessage());
        }
    }

    private void updateLocalFromRemote(String table, ResultSet remoteRow) {
        try {
            ResultSetMetaData meta = remoteRow.getMetaData();
            int cols = meta.getColumnCount();

            StringBuilder setClauses = new StringBuilder();
            for (int i = 1; i <= cols; i++) {
                String col = meta.getColumnName(i);
                if ("id".equals(col) || "sync_status".equals(col)) continue;
                if (setClauses.length() > 0) setClauses.append(", ");
                setClauses.append(col).append(" = ?");
            }

            String sql = "UPDATE " + table + " SET " + setClauses + " WHERE id = ?";
            try (PreparedStatement pstmt = localDb.getConnection().prepareStatement(sql)) {
                int idx = 1;
                for (int i = 1; i <= cols; i++) {
                    String col = meta.getColumnName(i);
                    if ("id".equals(col) || "sync_status".equals(col)) continue;
                    pstmt.setObject(idx++, remoteRow.getObject(i));
                }
                pstmt.setInt(idx, remoteRow.getInt("id"));
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Update from remote failed: " + e.getMessage());
        }
    }

    private void markSynced(Connection conn, String table, int id) {
        String sql = "UPDATE " + table + " SET sync_status = 'synced' WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Mark synced failed: " + e.getMessage());
        }
    }
}
