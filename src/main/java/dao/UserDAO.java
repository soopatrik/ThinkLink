package main.dao;


import main.utils.DatabaseHelper;

import java.sql.*;

public class UserDAO
{
  public static int createUser(String username, String role) {
    String sql = "INSERT INTO \"user\" (username, role) VALUES (?, ?) RETURNING id";
    try (Connection conn = DatabaseHelper.connect();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, username);
      stmt.setString(2, role);
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) return rs.getInt("id");

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return -1;
  }
}
