package main.utils;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHelper
{
  private static final String URL = "jdbc:postgresql://localhost:5432/SEP2";
  private static final String USER = "postgres";
  private static final String PASSWORD = "1111";

  public static Connection connect() throws SQLException {
    return DriverManager.getConnection(URL, USER, PASSWORD);
  }
}
