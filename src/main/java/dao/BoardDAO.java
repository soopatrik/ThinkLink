package main.dao;

import main.utils.DatabaseHelper;

import java.sql.*;

public class BoardDAO
{
  public static int createBoard(String name, int ownerId) {
    String sql = "INSERT INTO boards (name, owner_id) VALUES (?, ?) RETURNING id";
    try (Connection conn = DatabaseHelper.connect();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, name);
      stmt.setInt(2, ownerId);
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) return rs.getInt("id");

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return -1;
  }
}
