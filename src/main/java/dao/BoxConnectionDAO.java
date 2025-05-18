package main.dao;

import main.utils.DatabaseHelper;

import java.sql.*;

public class BoxConnectionDAO
{
  public static int insertConnection(int fromBoxId, int toBoxId,
      int fromX, int fromY, int toX, int toY) {
    String sql = "INSERT INTO box_connections (from_box_id, to_box_id, fromBoxCenter, targetBoxCenter) " +
        "VALUES (?, ?, POINT(?, ?), POINT(?, ?)) RETURNING id";

    try (Connection conn = DatabaseHelper.connect();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, fromBoxId);
      stmt.setInt(2, toBoxId);
      stmt.setInt(3, fromX);
      stmt.setInt(4, fromY);
      stmt.setInt(5, toX);
      stmt.setInt(6, toY);

      ResultSet rs = stmt.executeQuery();
      if (rs.next()) return rs.getInt("id");

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return -1;
  }
}
