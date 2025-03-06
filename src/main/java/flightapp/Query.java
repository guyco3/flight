package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;
  private static final String CREATE_USER_F = "Failed to create user\n";

  //
  // Instance variables
  //

  protected Query() throws SQLException, IOException {
    this.isLoggedIn = false;
    prepareStatements();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      // TODO: YOUR CODE HERE
      PreparedStatement clearStatement = conn.prepareStatement(
         "TRUNCATE TABLE USERS, RESERVATIONS, RESERVATION_ITINERARIES CASCADE"
      );
      clearStatement.executeUpdate(); // might want to keep check some stuff here
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);

    // TODO: YOUR CODE HERE
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
    // TODO: YOUR CODE HERE
    if (this.isLoggedIn) return "User already logged in\n";

    byte[] hashedPassword = PasswordUtils.saltAndHashPassword(password);
    try {

      PreparedStatement isUniqueStatement = conn.prepareStatement(
         "SELECT 1 FROM USERS WHERE username = ? AND password = ?"
      );
      isUniqueStatement.clearParameters();  
      isUniqueStatement.setString(1, username.toLowerCase());
      isUniqueStatement.setBytes(2, hashedPassword);
      ResultSet res = isUniqueStatement.executeQuery(); // might want to keep check some stuff here
      if (!res.next()) return "Login failed\n"; 
      
    } catch (Exception e) {
      e.printStackTrace();
      return CREATE_USER_F;
    }
    this.isLoggedIn = true;
    return "Logged in as " + username + "\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    // TODO: YOUR CODE HERE

    if (initAmount < 0) return CREATE_USER_F;

    try {

      PreparedStatement isUniqueStatement = conn.prepareStatement(
         "SELECT 1 FROM USERS WHERE username = ?"
      );
      isUniqueStatement.clearParameters();  
      isUniqueStatement.setString(1, username.toLowerCase());
      ResultSet res = isUniqueStatement.executeQuery(); // might want to keep check some stuff here
      if (res.next()) return CREATE_USER_F;
      
    } catch (Exception e) {
      e.printStackTrace();
      return CREATE_USER_F;
    }

    byte[] hashedPassword = PasswordUtils.saltAndHashPassword(password);

    try {

      PreparedStatement clearStatement = conn.prepareStatement(
         "INSERT INTO USERS VALUES(?, ?, ?)"
      );
      clearStatement.clearParameters();
      clearStatement.setString(1,username.toLowerCase());
      clearStatement.setBytes(2, hashedPassword);
      clearStatement.setInt(3, initAmount);
      clearStatement.executeUpdate(); // might want to keep check some stuff here
      this.isLoggedIn = true;
      return "Created user " + username.toLowerCase() + "\n";

    } catch (Exception e) {
      e.printStackTrace();
    }
    return CREATE_USER_F;

  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // WARNING: the below code is insecure (it's susceptible to SQL injection attacks) AND only
    // handles searches for direct flights.  We are providing it *only* as an example of how
    // to use JDBC; you are required to replace it with your own secure implementation.
    //
    // TODO: YOUR CODE HERE

    StringBuffer sb = new StringBuffer();

    try {
      // one hop itineraries
      String unsafeSearchSQL = "SELECT TOP (" + numberOfItineraries
        + ") day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
        + "FROM Flights " + "WHERE origin_city = \'" + originCity + "\' AND dest_city = \'"
        + destinationCity + "\' AND day_of_month =  " + dayOfMonth + " "
        + "ORDER BY actual_time ASC";

      Statement searchStatement = conn.createStatement();
      ResultSet oneHopResults = searchStatement.executeQuery(unsafeSearchSQL);

      while (oneHopResults.next()) {
        int result_dayOfMonth = oneHopResults.getInt("day_of_month");
        String result_carrierId = oneHopResults.getString("carrier_id");
        String result_flightNum = oneHopResults.getString("flight_num");
        String result_originCity = oneHopResults.getString("origin_city");
        String result_destCity = oneHopResults.getString("dest_city");
        int result_time = oneHopResults.getInt("actual_time");
        int result_capacity = oneHopResults.getInt("capacity");
        int result_price = oneHopResults.getInt("price");

        sb.append("Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: "
                  + result_flightNum + " Origin: " + result_originCity + " Destination: "
                  + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity
                  + " Price: " + result_price + "\n");
      }
      oneHopResults.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return sb.toString();
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_book(int itineraryId) {
    // TODO: YOUR CODE HERE
    return "Booking failed\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_pay(int reservationId) {
    // TODO: YOUR CODE HERE
    return "Failed to pay for reservation " + reservationId + "\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE
    return "Failed to retrieve reservations\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return "40001".equals(e.getSQLState()) || "40P01".equals(e.getSQLState());
  }

  /**
   * A class to store information about a single flight
   *
   * TODO(hctang): move this into QueryAbstract
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }
    
    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }
}
