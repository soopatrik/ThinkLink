package main.dao;



import main.utils.DatabaseHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BoxDAO
{
  public static int insertBox(int boardId, String title, String description,
      int posX, int posY, int centerX, int centerY, int boxWidth) {
    String sql = "INSERT INTO boxes (board_id, title, description, pos_x, pos_y, center, boxWidth) " +
        "VALUES (?, ?, ?, ?, ?, POINT(?, ?), ?) RETURNING id";

    try (Connection conn = DatabaseHelper.connect();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, boardId);
      stmt.setString(2, title);
      stmt.setString(3, description);
      stmt.setInt(4, posX);
      stmt.setInt(5, posY);
      stmt.setInt(6, centerX);
      stmt.setInt(7, centerY);
      stmt.setInt(8, boxWidth);

      ResultSet rs = stmt.executeQuery();
      if (rs.next()) return rs.getInt("id");

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return -1;
  }

  public static List<String> getBoxesByBoardId(int boardId) {
    List<String> boxTitles = new ArrayList<>();
    String sql = "SELECT title FROM boxes WHERE board_id = ?";

    try (Connection conn = DatabaseHelper.connect();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setInt(1, boardId);
      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        boxTitles.add(rs.getString("title"));
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return boxTitles;
  }
}
