package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {

  
  public class Itinerary implements Comparable<Itinerary> {
    private int fid1;
    private int fid2;
    private int resultDayOfMonth;
    private String resultCarrierId1;
    private String resultCarrierId2;
    private String resultFlightNum1;
    private String resultFlightNum2;
    private String resultOriginCity1;
    private String resultOriginCity2;
    private String resultDestCity1;
    private String resultDestCity2;
    private int resultTime1;
    private int resultTime2;
    private int total_time;
    private int resultCapacity1;
    private int resultCapacity2;
    private int resultPrice1;
    private int resultPrice2;
    private int numFlights;

    // Constructor
    public Itinerary(int fid1, int fid2, int resultDayOfMonth, String resultCarrierId1, String resultCarrierId2,
                    String resultFlightNum1, String resultFlightNum2, String resultOriginCity1, String resultOriginCity2,
                    String resultDestCity1, String resultDestCity2, int resultTime1, int resultTime2,
                    int resultCapacity1, int resultCapacity2, int resultPrice1, int resultPrice2, int numFlights) {
        this.fid1 = fid1;
        this.fid2 = fid2;
        this.resultDayOfMonth = resultDayOfMonth;
        this.resultCarrierId1 = resultCarrierId1;
        this.resultCarrierId2 = resultCarrierId2;
        this.resultFlightNum1 = resultFlightNum1;
        this.resultFlightNum2 = resultFlightNum2;
        this.resultOriginCity1 = resultOriginCity1;
        this.resultOriginCity2 = resultOriginCity2;
        this.resultDestCity1 = resultDestCity1;
        this.resultDestCity2 = resultDestCity2;
        this.resultTime1 = resultTime1;
        this.resultTime2 = resultTime2;
        this.total_time = resultTime1 + resultTime2;
        this.resultCapacity1 = resultCapacity1;
        this.resultCapacity2 = resultCapacity2;
        this.resultPrice1 = resultPrice1;
        this.resultPrice2 = resultPrice2;
        this.numFlights = numFlights;
    }

    // Getters
    public int getTime() { return total_time; }; 
    public int getFid1() { return fid1; }
    public int getFid2() { return fid2; }
    public int getResultDayOfMonth() { return resultDayOfMonth; }
    public String getResultCarrierId1() { return resultCarrierId1; }
    public String getResultCarrierId2() { return resultCarrierId2; }
    public String getResultFlightNum1() { return resultFlightNum1; }
    public String getResultFlightNum2() { return resultFlightNum2; }
    public String getResultOriginCity1() { return resultOriginCity1; }
    public String getResultOriginCity2() { return resultOriginCity2; }
    public String getResultDestCity1() { return resultDestCity1; }
    public String getResultDestCity2() { return resultDestCity2; }
    public int getResultTime1() { return resultTime1; }
    public int getResultTime2() { return resultTime2; }
    public int getResultCapacity1() { return resultCapacity1; }
    public int getResultCapacity2() { return resultCapacity2; }
    public int getResultPrice1() { return resultPrice1; }
    public int getResultPrice2() { return resultPrice2; }
    public int getNumFlights() { return numFlights; }

    // Implement compareTo method for sorting
    @Override
    public int compareTo(Itinerary other) {
        // Sort by actual time (assuming resultTime1 is the actual time)
        if (this.resultTime1 != other.resultTime1) {
            return Integer.compare(this.resultTime1, other.resultTime1);
        } else if (this.fid1 != other.fid1) {
            return Integer.compare(this.fid1, other.fid1);
        } else {
            return Integer.compare(this.fid2, other.fid2);
        }
    }

    // toString method for easy printing
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // Content
        sb.append("ID: ").append(fid1).append(" Day: ").append(resultDayOfMonth).append(" Carrier: ")
          .append(resultCarrierId1).append(" Number: ").append(resultFlightNum1).append(" Origin: ")
          .append(resultOriginCity1).append(" Dest: ").append(resultDestCity1).append(" Duration: ")
          .append(resultTime1).append(" Capacity: ").append(resultCapacity1).append(" Price: ")
          .append(resultPrice1).append("\n");
        if (numFlights == 2) {              
            sb.append("ID: ").append(fid2).append(" Day: ").append(resultDayOfMonth).append(" Carrier: ")
              .append(resultCarrierId2).append(" Number: ").append(resultFlightNum2).append(" Origin: ")
              .append(resultOriginCity2).append(" Dest: ").append(resultDestCity2).append(" Duration: ")
              .append(resultTime2).append(" Capacity: ").append(resultCapacity2).append(" Price: ")
              .append(resultPrice2).append("\n");
        }
        return sb.toString();
    }
  };
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;
  private static final String CREATE_USER_F = "Failed to create user\n";
  private static String currentUser;
  private List<Itinerary> itins;
  //
  // Instance variables
  //

  protected Query() throws SQLException, IOException {
    Query.currentUser = null;
    itins = new ArrayList<Itinerary>();
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
         "TRUNCATE TABLE USERS_gcohen3, RESERVATIONS_gcohen3, RESERVATION_INFO_gcohen3 CASCADE;"
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
    if (Query.currentUser != null) return "User already logged in\n";

    try {

      PreparedStatement isUniqueStatement = conn.prepareStatement(
         "SELECT password FROM USERS_gcohen3 WHERE username = ?"
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
    Query.currentUser = username;
    return "Logged in as " + username + "\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    // TODO: YOUR CODE HERE

    if (initAmount < 0) return CREATE_USER_F;

    try {

      PreparedStatement isUniqueStatement = conn.prepareStatement(
         "SELECT 1 FROM USERS_gcohen3 WHERE username = ?"
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
         "INSERT INTO USERS_gcohen3 VALUES(?, ?, ?)"
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
    itins.clear();

    try {
      PreparedStatement searchStatementDirect = conn.prepareStatement(
          "SELECT fid, day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price FROM FLIGHTS WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? ORDER BY actual_time ASC, fid ASC LIMIT ?"
      );
      searchStatementDirect.clearParameters();
      searchStatementDirect.setString(1, originCity);
      searchStatementDirect.setString(2, destinationCity);
      searchStatementDirect.setInt(3, dayOfMonth);
      searchStatementDirect.setInt(4, numberOfItineraries);
      ResultSet oneHopResults = searchStatementDirect.executeQuery();
      while (oneHopResults.next()) {
          int fid = oneHopResults.getInt("fid");
          int result_dayOfMonth = oneHopResults.getInt("day_of_month");
          String result_carrierId = oneHopResults.getString("carrier_id");
          String result_flightNum = oneHopResults.getString("flight_num");
          String result_originCity = oneHopResults.getString("origin_city");
          String result_destCity = oneHopResults.getString("dest_city");
          int result_time = oneHopResults.getInt("actual_time");
          int result_capacity = oneHopResults.getInt("capacity");
          int result_price = oneHopResults.getInt("price");

          itins.add(new Itinerary(fid, -1, result_dayOfMonth, result_carrierId, "",
                        result_flightNum, "", result_originCity, "",
                        result_destCity, "", result_time, 0,
                        result_capacity, 0, result_price, 0, 1));
      }

      if (!directFlight) {

          PreparedStatement combinedStatement = conn.prepareStatement(
            "SELECT " +
            "    f1.fid AS fid1, " +
            "    f2.fid AS fid2, " +
            "    f1.day_of_month AS day, " +
            "    f1.carrier_id AS carrier1, " +
            "    f1.flight_num AS number1, " +
            "    f1.origin_city AS origin, " +
            "    f1.dest_city AS intermediate, " +
            "    f2.carrier_id AS carrier2, " +
            "    f2.flight_num AS number2, " +
            "    f2.dest_city AS dest, " +
            "    f1.actual_time AS at1, " +
            "    f2.actual_time AS at2, " +
            "    f1.actual_time + f2.actual_time AS duration, " +
            "    f1.price AS p1, " +
            "    f2.price AS p2, " +
            "    f1.capacity AS c1, " +
            "    f2.capacity AS c2, " +
            "    2 AS num_flights " +
            "FROM FLIGHTS f1 " +
            "JOIN FLIGHTS f2 ON f1.dest_city = f2.origin_city  AND f1.dest_state = f2.origin_state AND f1.day_of_month = f2.day_of_month  " +
            "WHERE f1.origin_city = ? AND f2.dest_city = ? AND f1.day_of_month = ? AND f1.canceled = 0 AND f2.canceled = 0 ORDER BY duration, fid1, fid2 LIMIT ?"
        );
        
        combinedStatement.setString(1, originCity);
        combinedStatement.setString(2, destinationCity);
        combinedStatement.setInt(3, dayOfMonth);
        combinedStatement.setInt(4, numberOfItineraries - itins.size());

        ResultSet combined = combinedStatement.executeQuery();

        while (combined.next()) {
          int fid1 = combined.getInt("fid1");
          int fid2 = combined.getInt("fid2");
          int result_dayOfMonth = combined.getInt("day");
          String result_carrierId1 = combined.getString("carrier1");
          String result_carrierId2 = combined.getString("carrier2");
          String result_flightNum1 = combined.getString("number1");
          String result_flightNum2 = combined.getString("number2");
          String result_originCity1 = combined.getString("origin");
          String result_originCity2 = combined.getString("intermediate");
          String result_destCity1 = combined.getString("intermediate");
          String result_destCity2 = combined.getString("dest");
          int result_time1 = combined.getInt("at1");
          int result_time2 = combined.getInt("at2");
          int result_capacity1 = combined.getInt("c1");
          int result_capacity2 = combined.getInt("c2");
          int result_price1 = combined.getInt("p1");
          int result_price2 = combined.getInt("p2");

          itins.add(new Itinerary(fid1, fid2, result_dayOfMonth, result_carrierId1, result_carrierId2,
                        result_flightNum1, result_flightNum2, result_originCity1, result_originCity2,
                        result_destCity1, result_destCity2, result_time1, result_time2,
                        result_capacity1, result_capacity2, result_price1, result_price2, 2));
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      return "Failed to search\n";
    }

    if (itins.size() == 0) return "No flights match your selection\n";

    Collections.sort(itins);
    int ID = 0;
    for (Itinerary itinerary : itins) {
      // Header with itinerary id info
      sb.append("Itinerary ").append(ID++).append(": ").append(itinerary.getNumFlights()).append(" flight(s), ").append(itinerary.getTime()).append(" minutes\n");
      sb.append(itinerary.toString());
    }
  
    return sb.toString();
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_book(int itineraryId) {
    // TODO: YOUR CODE HERE
    if (currentUser == null) return "Cannot book reservations, not logged in\n";

    if (itins.size() <= itineraryId) return "No such itinerary " + itineraryId + "\n";

    Itinerary itinerary = itins.get(itineraryId);

    Set<Integer> bookedDays = new HashSet<>();
    int RID = 1;

    try {
      conn.setAutoCommit(false);
      PreparedStatement userReservationsStatement = conn.prepareStatement(
         "WITH rids AS (SELECT ri.fid "
    + "    FROM RESERVATIONS_gcohen3 r "
    + "    INNER JOIN USERS_gcohen3 u ON r.username = u.username "
    + "    INNER JOIN RESERVATION_INFO_gcohen3 ri ON r.rid = ri.rid "
    + "    WHERE u.username = ?"
    + ") "
    + "SELECT f.day_of_month "
    + "FROM FLIGHTS f "
    + "INNER JOIN rids ON f.fid = rids.fid;"
      );
      userReservationsStatement.clearParameters();  
      userReservationsStatement.setString(1, currentUser);
      
      ResultSet res = userReservationsStatement.executeQuery(); // might want to keep check some stuff here
      while (res.next()) {
        bookedDays.add(res.getInt(1));
      } 

      if (bookedDays.contains(itinerary.resultDayOfMonth)) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "You cannot book two flights in the same day\n";
      }

      // get number of seats for current flight
      int seatsBookedF1 = 0;
      int seatsBookedF2 = 0;

      PreparedStatement bookedSeatsStatement = conn.prepareStatement(
         "SELECT COUNT(r.rid) FROM RESERVATION_INFO_gcohen3 r INNER JOIN FLIGHTS f ON r.fid = f.fid WHERE f.fid = ?;"
      );
      bookedSeatsStatement.clearParameters();  
      bookedSeatsStatement.setInt(1, itinerary.fid1);
      res = bookedSeatsStatement.executeQuery(); // might want to keep check some stuff here
      while (res.next()) {
        seatsBookedF1 += res.getInt(1);
      } 
      if (itinerary.numFlights > 1) {
        bookedSeatsStatement.clearParameters();  
        bookedSeatsStatement.setInt(1, itinerary.fid2);
        res = bookedSeatsStatement.executeQuery(); // might want to keep check some stuff here
        while (res.next()) {
          seatsBookedF2 += res.getInt(1);
        } 
      }

      if (itinerary.resultCapacity1 < seatsBookedF1 + 1 || ( itinerary.numFlights > 1 && itinerary.resultCapacity2 < seatsBookedF2 + 1)) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "Booking failed\n";
      }

      // get RID
      PreparedStatement ridStatement = conn.prepareStatement(
         "SELECT COUNT(*) FROM RESERVATIONS_gcohen3;"
      );
      ridStatement.clearParameters();
      res = ridStatement.executeQuery();
      if (res.next())RID += res.getInt(1);

      // add reservation
      PreparedStatement reserveStatement = conn.prepareStatement(
         "INSERT INTO RESERVATIONS_gcohen3 VALUES (?, ?, ?);"
      );
      reserveStatement.clearParameters();  
      reserveStatement.setInt(1, RID);
      reserveStatement.setString(2, currentUser);
      reserveStatement.setInt(3, 0);
      if(reserveStatement.executeUpdate() == 0){
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "Booking failed\n";
      }

      // add flights to reservations info
      PreparedStatement addFlightsStatement = conn.prepareStatement(
         "INSERT INTO RESERVATION_INFO_gcohen3 VALUES (?, ?);"
      );
      addFlightsStatement.clearParameters();  
      addFlightsStatement.setInt(1, RID);
      addFlightsStatement.setInt(2, itinerary.fid1);
      if(addFlightsStatement.executeUpdate() == 0) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "Booking failed\n";
      }

      if (itinerary.numFlights > 1) {
        addFlightsStatement.clearParameters();  
        addFlightsStatement.setInt(1, RID);
        addFlightsStatement.setInt(2, itinerary.fid2);
        if(addFlightsStatement.executeUpdate() == 0){
          conn.rollback(); // Roll back any query executions in transaction thus far
          conn.setAutoCommit(true); // End the transaction
          return "Booking failed\n";
        }
      }
      conn.commit(); // Commit our query executions (make them permanent)
      conn.setAutoCommit(true); // End the transaction
      
    } catch (SQLException e) {
      e.printStackTrace();
      if (isDeadlock(e)) return transaction_pay(itineraryId);
      return "Booking failed\n";
    }
    return "Booked flight(s), reservation ID: "+ RID + "\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_pay(int reservationId) {
    // TODO: YOUR CODE HERE
    if (currentUser == null) return "Cannot pay, not logged in\n";

    int balance = 0;
    int totalPrice = -1;
    int found = 0;
    try {
      conn.setAutoCommit(false);
      PreparedStatement getBalanceStatement = conn.prepareStatement(
        "SELECT balance FROM USERS_gcohen3 WHERE username = ?;"
      );
      getBalanceStatement.setString(1, currentUser);
      ResultSet res = getBalanceStatement.executeQuery(); // might want to keep check some stuff here
      if (res.next()) balance += res.getInt(1);

      PreparedStatement getInfoStatement = conn.prepareStatement(
          "WITH UNPAID as (SELECT rid FROM RESERVATIONS_gcohen3 WHERE paid = 0 AND username = ? AND rid = ?) " +
          "SELECT COUNT(r.rid), SUM(f.price) FROM FLIGHTS f INNER JOIN RESERVATION_INFO_gcohen3 r ON r.fid = f.fid " +
          "WHERE r.rid IN (SELECT rid FROM UNPAID);"
      );
      getInfoStatement.clearParameters();
      getInfoStatement.setString(1, currentUser);
      getInfoStatement.setInt(2, reservationId);
      res = getInfoStatement.executeQuery();
      if(res.next()) {
        found = res.getInt(1);
        totalPrice = res.getInt(2);
      }

      if (found == 0) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "Cannot find unpaid reservation " + reservationId + " under user: " + currentUser + "\n";
      }
      
      if (balance < totalPrice) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "User has only " + balance + " in account but itinerary costs " + totalPrice + "\n";
      }

      // make payment: blanance -= totalPrice and change paid to 1 in reservations table
      PreparedStatement updateBalanceStatement = conn.prepareStatement(
        "UPDATE USERS_gcohen3 SET balance = ? WHERE username = ?;"
      );
      updateBalanceStatement.clearParameters();;
      updateBalanceStatement.setInt(1, balance - totalPrice);
      updateBalanceStatement.setString(2, currentUser);
      if (updateBalanceStatement.executeUpdate() == 0) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "Failed to pay for reservation " + reservationId + "\n";
      }

      PreparedStatement updatePaidStatement = conn.prepareStatement(
        "UPDATE RESERVATIONS_gcohen3 SET paid = 1 WHERE rid = ?;"
      );
      updatePaidStatement.clearParameters();;
      updatePaidStatement.setInt(1, reservationId);
      if (updatePaidStatement.executeUpdate() == 0) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "Failed to pay for reservation " + reservationId + "\n";
      }

      conn.commit(); // Commit our query executions (make them permanent)
      conn.setAutoCommit(true); // End the transaction
      
    } catch (SQLException e) {
      e.printStackTrace();
      if (isDeadlock(e)) return transaction_pay(reservationId);
      return "Failed to pay for reservation " + reservationId + "\n";
    }
    return "Paid reservation: " + reservationId + " remaining balance: " + (balance - totalPrice) + "\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE
    if (currentUser == null) return "Cannot view reservations, not logged in\n";

    Map<Integer, ArrayList<Flight>> reservationsMap = new TreeMap<>();
    Map<Integer, Integer> paidMap = new HashMap<>();
    StringBuffer sb = new StringBuffer();

    try {
      conn.setAutoCommit(false);
      PreparedStatement getReservationsStatement = conn.prepareStatement(
        "WITH rids as (" +
        "  SELECT r.rid, r.paid, ri.fid " +
        "  FROM RESERVATIONS_gcohen3 r " +
        "  INNER JOIN RESERVATION_INFO_gcohen3 ri " +
        "  ON r.rid = ri.rid " +
        "  WHERE r.username = ?" +
        ") " +
        "SELECT " +
        "  r.rid, " +
        "  r.paid, " +
        "  f.fid, " +
        "  f.day_of_month, " +
        "  f.carrier_id, " +
        "  f.flight_num, " +
        "  f.origin_city, " +
        "  f.dest_city, " +
        "  f.actual_time, " +
        "  f.capacity, " +
        "  f.price " +
        "FROM FLIGHTS f " +
        "INNER JOIN rids r " +
        "ON f.fid = r.fid;"
      );
      getReservationsStatement.setString(1, currentUser);
      ResultSet res = getReservationsStatement.executeQuery(); // might want to keep check some stuff here
      while (res.next()) {
          int paid = res.getInt("paid");
          int rid = res.getInt("rid");
          int fid = res.getInt("fid");
          int result_dayOfMonth = res.getInt("day_of_month");
          String result_carrierId = res.getString("carrier_id");
          String result_flightNum = res.getString("flight_num");
          String result_originCity = res.getString("origin_city");
          String result_destCity = res.getString("dest_city");
          int result_time = res.getInt("actual_time");
          int result_capacity = res.getInt("capacity");
          int result_price = res.getInt("price");

          if (!reservationsMap.containsKey(rid)) {
            reservationsMap.put(rid, new ArrayList<>());
          }
          paidMap.put(rid, paid);
          reservationsMap.get(rid).add(new Flight(fid, result_dayOfMonth, result_carrierId, result_flightNum, result_originCity, result_destCity, result_time, result_capacity, result_price));
      }

      if (reservationsMap.size() == 0) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "No reservations found\n";
      }

      for (int rid : reservationsMap.keySet()) {
        ArrayList<Flight> tmp = reservationsMap.get((rid));
        sb.append("Reservation "  + rid + " paid: " + (paidMap.get(rid) == 0 ? "false" : "true") + ":\n");
        for (Flight flight : tmp) {
          sb.append(flight.toString());
        }
        sb.append("\n");
      }
      
      conn.commit(); // Commit our query executions (make them permanent)
      conn.setAutoCommit(true); // End the transaction
      
    } catch (SQLException e) {
      e.printStackTrace();
      if (isDeadlock(e)) return transaction_reservations();
      return "Failed to retrieve reservations\n";
    }
    return sb.toString();
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
