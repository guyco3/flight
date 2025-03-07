package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

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
  protected static boolean isLoggedIn;
  //
  // Instance variables
  //

  protected Query() throws SQLException, IOException {
    Query.isLoggedIn = false;
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
         "TRUNCATE TABLE USERS, RESERVATIONS, RESERVATION_INFO CASCADE"
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
    if (Query.isLoggedIn) return "User already logged in\n";

    try {

      PreparedStatement isUniqueStatement = conn.prepareStatement(
         "SELECT password FROM USERS WHERE username = ?"
      );
      isUniqueStatement.clearParameters();  
      isUniqueStatement.setString(1, username.toLowerCase());
      ResultSet res = isUniqueStatement.executeQuery(); // might want to keep check some stuff here

      // no user with username <username>
      if (!res.next()) return "Login failed\n";

      // check if passwords match
      if (!PasswordUtils.plaintextMatchesSaltedHash(password, res.getBytes(1))) return "Login failed\n";
      
    } catch (Exception e) {
      e.printStackTrace();
      return CREATE_USER_F;
    }
    Query.isLoggedIn = true;
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
    int resCounts = 0;

    try {
      PreparedStatement searchStatementDirect = conn.prepareStatement(
        "SELECT TOP (?) day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price FROM FLIGHTS WHERE WHERE origin_city = ? AND dest_city ? AND day_of_month = ? ORDER BY actual_time ASC, fid ASC"
      );
      searchStatementDirect.clearParameters();
      searchStatementDirect.setInt(1, numberOfItineraries);
      searchStatementDirect.setString(2, originCity);
      searchStatementDirect.setString(3, destinationCity);
      searchStatementDirect.setInt(4, dayOfMonth);
      ResultSet oneHopResults = searchStatementDirect.executeQuery();
      while (oneHopResults.next()) {
        int result_dayOfMonth = oneHopResults.getInt("day_of_month");
        String result_carrierId = oneHopResults.getString("carrier_id");
        String result_flightNum = oneHopResults.getString("flight_num");
        String result_originCity = oneHopResults.getString("origin_city");
        String result_destCity = oneHopResults.getString("dest_city");
        int result_time = oneHopResults.getInt("actual_time");
        int result_capacity = oneHopResults.getInt("capacity");
        int result_price = oneHopResults.getInt("price");

        // header with itinerary id info
        sb.append("Itinerary " + resCounts + ": 1 flight(s), " + result_time + " minutes\n");
        // content
        sb.append("Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: "
                  + result_flightNum + " Origin: " + result_originCity + " Destination: "
                  + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity
                  + " Price: " + result_price + "\n");
        resCounts++;
      }

      if (!directFlight) {
        PreparedStatement searchStatementIndirect = conn.prepareStatement(
          "SELECT TOP (?) f1.day_of_month, f1.carrier_id, f2.carrier_id, f1.flight_num, f2.flight_num, f1.origin_city, f2.dest_city, f1.actual_time, f2.actual_time, f1.capacity, f2.capacity, f1.price, f2.price FROM FLIGHTS f1, FLIGHTS f2 WHERE WHERE f1.origin_city = ? AND f2.dest_city ? AND f1.dest_city = f2.origin_city AND f1.day_of_month = ? AND f1.day_of_month = f2.day_of_month ORDER BY actual_time ASC, f1.fid ASC, f2.fid ASC"
        );
        searchStatementIndirect.clearParameters();
        searchStatementIndirect.setInt(1, numberOfItineraries - resCounts);
        searchStatementIndirect.setString(2, originCity);
        searchStatementIndirect.setString(3, destinationCity);
        searchStatementIndirect.setInt(4, dayOfMonth);
        ResultSet twoHopResults = searchStatementIndirect.executeQuery();
        while (twoHopResults.next()) {
          int result_dayOfMonth = oneHopResults.getInt("f1.day_of_month");
          String result_carrierId1 = oneHopResults.getString("f1.carrier_id");
          String result_carrierId2 = oneHopResults.getString("f2.carrier_id");
          String result_flightNum1 = oneHopResults.getString("f1.flight_num");
          String result_flightNum2 = oneHopResults.getString("f2.flight_num");
          String result_originCity = oneHopResults.getString("f1.origin_city");
          String result_destCity = oneHopResults.getString("f2.dest_city");
          int result_time1 = oneHopResults.getInt("f1.actual_time");
          int result_time2 = oneHopResults.getInt("f2.actual_time");
          int result_capacity1 = oneHopResults.getInt("f1.capacity");
          int result_capacity2 = oneHopResults.getInt("f2.capacity");
          int result_price1 = oneHopResults.getInt("f1.price");
          int result_price2 = oneHopResults.getInt("f2.price");

          // header with itinerary id info
          sb.append("Itinerary " + resCounts + ": 2 flight(s), " + result_time1 + result_time2 + " minutes\n");
          // content
          sb.append("Day: " + result_dayOfMonth + " Carrier: " + result_carrierId1 + " Number: "
                    + result_flightNum1 + " Origin: " + result_originCity + " Destination: "
                    + result_destCity + " Duration: " + result_time1 + " Capacity: " + result_capacity1
                    + " Price: " + result_price1 + "\n");
          sb.append("Day: " + result_dayOfMonth + " Carrier: " + result_carrierId2 + " Number: "
                    + result_flightNum2 + " Origin: " + result_originCity + " Destination: "
                    + result_destCity + " Duration: " + result_time2 + " Capacity: " + result_capacity2
                    + " Price: " + result_price2 + "\n");
          resCounts++;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "Failed to search\n";
    }

    if (resCounts == 0) return "No flights match your selection\n";

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
