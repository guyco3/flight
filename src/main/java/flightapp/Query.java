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

  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT f.capacity AS capacity FROM Flights f INNER JOIN RESERVATION_INFO_gcohen3 r WHERE f.fid = ?";
  private static final String LOGIN_IS_UNIQUE_SQL = "SELECT password FROM USERS_gcohen3 WHERE username = ?";
  private static final String CREATE_IS_UNIQUE_SQL = "SELECT 1 FROM USERS_gcohen3 WHERE username = ?";
  private static final String CREATE_INSERT_SQL = "INSERT INTO USERS_gcohen3 VALUES(?, ?, ?)";
  private static final String SEARCH_DIRECT_FLIGHTS_SQL = "SELECT * FROM FLIGHTS WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? AND canceled = 0 ORDER BY actual_time ASC, fid ASC LIMIT ?";
  private static final String SEARCH_INDIRECT_FLIGHTS_SQL = "SELECT " +
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
    "    f1.actual_time + f2.actual_time as duration, " +
    "    f1.price AS p1, " +
    "    f2.price AS p2, " +
    "    f1.capacity AS c1, " +
    "    f2.capacity AS c2, " +
    "    2 AS num_flights " +
    "FROM FLIGHTS f1 " +
    "INNER JOIN FLIGHTS f2 ON f1.dest_city = f2.origin_city  AND f1.dest_state = f2.origin_state AND f1.day_of_month = f2.day_of_month  " +
    "WHERE f1.origin_city = ? AND f2.dest_city = ? AND f1.day_of_month = ? AND f1.canceled = 0 AND f2.canceled = 0 ORDER BY duration, fid1, fid2 LIMIT ?";
  // assuming indirect flights always on same day, if f2 => then f1 must exist
    private static final String BOOK_LIST_RESERVATIONS_SQL = "WITH rids AS (SELECT ri.fid1 as f1 "
  + "    FROM RESERVATIONS_gcohen3 r "
  + "    INNER JOIN USERS_gcohen3 u ON r.username = u.username "
  + "    INNER JOIN RESERVATION_INFO_gcohen3 ri ON r.rid = ri.rid "
  + "    WHERE u.username = ?"
  + ") "
  + "SELECT DISTINCT f.day_of_month "
  + "FROM FLIGHTS f "
  + "INNER JOIN rids ON f.fid = rids.f1";
  private static final String BOOK_GET_SEATS_F1_SQL = "SELECT COUNT(*) FROM RESERVATION_INFO_gcohen3 r WHERE r.fid1 = ?;";
  private static final String BOOK_GET_SEATS_F2_SQL = "SELECT COUNT(*) FROM RESERVATION_INFO_gcohen3 r WHERE r.fid2 = ?;";
  private static final String BOOK_GET_RID_SQL = "SELECT COUNT(*) FROM RESERVATIONS_gcohen3;";
  private static final String BOOK_INSERT_R_SQL = "INSERT INTO RESERVATIONS_gcohen3 VALUES (?, ?, ?);";
  private static final String BOOK_INSERT_R_INFO_SQL = "INSERT INTO RESERVATION_INFO_gcohen3 VALUES (?, ?, ?);";

  private static final String PAY_GET_BALANCE_SQL = "SELECT balance FROM USERS_gcohen3 WHERE username = ?;";
  private static final String PAY_GET_INFO_SQL = "WITH UNPAID as (SELECT rid FROM RESERVATIONS_gcohen3 WHERE paid = 0 AND username = ? AND rid = ?) " +
          "SELECT COUNT(r.rid), SUM(f.price) FROM FLIGHTS f INNER JOIN RESERVATION_INFO_gcohen3 r ON (r.fid1 = f.fid OR r.fid2 = f.fid) " +
          "WHERE r.rid IN (SELECT rid FROM UNPAID);";
  private static final String PAY_UPDATE_BALANCE_SQL = "UPDATE USERS_gcohen3 SET balance = ? WHERE username = ?;";
  private static final String PAY_UPDATE_PAID_SQL = "UPDATE RESERVATIONS_gcohen3 SET paid = 1 WHERE rid = ?;";

  private static final String RESERVE_GET_R_SQL = "WITH rids as (" +
    "  SELECT r.rid, r.paid, ri.fid1, ri.fid2 " +
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
    "ON (f.fid = r.fid1 OR f.fid = r.fid2);";
 
  private PreparedStatement flightCapacityStmt;
  private PreparedStatement loginIsUniqueStatement;
  private PreparedStatement createIsUniqueStatement;
  private PreparedStatement createInsertStatement;
  private PreparedStatement searchDirectFlightStatement;
  private PreparedStatement searchIndirectFlightStatement;
  private PreparedStatement bookListReservationStatement;
  private PreparedStatement bookGetSeatsF1Statement;
  private PreparedStatement bookGetSeatsF2Statement;
  private PreparedStatement bookGetRIDStatement;
  private PreparedStatement bookInsertRStatement;
  private PreparedStatement bookInsertRInfoStatement;
  private PreparedStatement payGetBalanceStatement;
  private PreparedStatement payGetInfoStatement;
  private PreparedStatement payUpdateBalanceStatement;
  private PreparedStatement payUpdatePaidStatement;
  private PreparedStatement reserveGetReservationsStatement;
  private String currentUser;
  private List<Itinerary> itins;
  //
  // Instance variables
  //

  protected Query() throws SQLException, IOException {
    currentUser = null;
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
    } catch (SQLException e) {
      if (isDeadlock(e)) clearTables();
      e.printStackTrace();
    }
  }
  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);
    loginIsUniqueStatement =conn.prepareStatement(LOGIN_IS_UNIQUE_SQL);
    createIsUniqueStatement = conn.prepareStatement(CREATE_IS_UNIQUE_SQL);
    createInsertStatement = conn.prepareStatement(CREATE_INSERT_SQL);
    searchDirectFlightStatement = conn.prepareStatement(SEARCH_DIRECT_FLIGHTS_SQL);
    searchIndirectFlightStatement = conn.prepareStatement(SEARCH_INDIRECT_FLIGHTS_SQL);
    bookListReservationStatement = conn.prepareStatement(BOOK_LIST_RESERVATIONS_SQL);
    bookGetSeatsF1Statement = conn.prepareStatement(BOOK_GET_SEATS_F1_SQL);
    bookGetSeatsF2Statement = conn.prepareStatement(BOOK_GET_SEATS_F2_SQL);
    bookGetRIDStatement = conn.prepareStatement(BOOK_GET_RID_SQL);
    bookInsertRStatement = conn.prepareStatement(BOOK_INSERT_R_SQL);
    bookInsertRInfoStatement = conn.prepareStatement(BOOK_INSERT_R_INFO_SQL);
    payGetBalanceStatement = conn.prepareStatement(PAY_GET_BALANCE_SQL);
    payGetInfoStatement = conn.prepareStatement(PAY_GET_INFO_SQL);
    payUpdateBalanceStatement = conn.prepareStatement(PAY_UPDATE_BALANCE_SQL);
    payUpdatePaidStatement = conn.prepareStatement(PAY_UPDATE_PAID_SQL);
    reserveGetReservationsStatement = conn.prepareStatement(RESERVE_GET_R_SQL);
    // TODO: YOUR CODE HERE
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
    // TODO: YOUR CODE HERE
    if (this.currentUser != null) return "User already logged in\n";

    try {      
      
      loginIsUniqueStatement.clearParameters();  
      loginIsUniqueStatement.setString(1, username.toLowerCase());
      ResultSet res = loginIsUniqueStatement.executeQuery(); // might want to keep check some stuff here

      // no user with username <username>
      if (!res.next()) return "Login failed\n";
      
      // check if passwords match
      if (!PasswordUtils.plaintextMatchesSaltedHash(password, res.getBytes(1))) return "Login failed\n";

    } catch (SQLException e) {
      e.printStackTrace();
      return "Login failed\n";
    }
    this.currentUser = username;
    itins.clear();
    return "Logged in as " + username + "\n";
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    // TODO: YOUR CODE HERE

    if (initAmount < 0) return "Failed to create user\n";

    try {
    
      conn.setAutoCommit(false);
      createIsUniqueStatement.clearParameters();  
      createIsUniqueStatement.setString(1, username.toLowerCase());
      ResultSet res = createIsUniqueStatement.executeQuery(); // might want to keep check some stuff here
      if (res.next()) {
        conn.rollback(); // Manually roll back the transaction
        conn.setAutoCommit(true); // Reset auto-commit mode
        return "Failed to create user\n";
      }
      
      byte[] hashedPassword = PasswordUtils.saltAndHashPassword(password);

      createInsertStatement.clearParameters();
      createInsertStatement.setString(1,username.toLowerCase());
      createInsertStatement.setBytes(2, hashedPassword);
      createInsertStatement.setInt(3, initAmount);
      createInsertStatement.executeUpdate(); // might want to keep check some stuff here
      conn.commit();
      conn.setAutoCommit(true);
      return "Created user " + username.toLowerCase() + "\n";

    } catch (Exception e) {
      if (e instanceof SQLException && isDeadlock((SQLException)e)) {
          try {
              conn.rollback(); // Manually roll back the transaction
              conn.setAutoCommit(true); // Reset auto-commit mode
              return transaction_createCustomer(username, password, initAmount);
          } catch (SQLException ex) {}
      }
      e.printStackTrace();
    }
    return "Failed to create user\n";
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

      searchDirectFlightStatement.clearParameters();
      searchDirectFlightStatement.setString(1, originCity);
      searchDirectFlightStatement.setString(2, destinationCity);
      searchDirectFlightStatement.setInt(3, dayOfMonth);
      searchDirectFlightStatement.setInt(4, numberOfItineraries);
      ResultSet oneHopResults = searchDirectFlightStatement.executeQuery();
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
          Flight flight = new Flight(fid, result_dayOfMonth, result_carrierId, result_flightNum, result_originCity, result_destCity, result_time, result_capacity, result_price);
          itins.add(new Itinerary(flight));
      }

      if (!directFlight) {

        searchIndirectFlightStatement.setString(1, originCity);
        searchIndirectFlightStatement.setString(2, destinationCity);
        searchIndirectFlightStatement.setInt(3, dayOfMonth);
        searchIndirectFlightStatement.setInt(4, numberOfItineraries - itins.size());

        ResultSet combined = searchIndirectFlightStatement.executeQuery();

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
          int result_capacity1 = combined.getInt("c1");
          int result_capacity2 = combined.getInt("c2");
          int result_price1 = combined.getInt("p1");
          int result_price2 = combined.getInt("p2");
          int at1 = combined.getInt("at1");
          int at2 = combined.getInt("at2");

          Flight f1 = new Flight(fid1, result_dayOfMonth, result_carrierId1, result_flightNum1, result_originCity1, result_destCity1, at1, result_capacity1, result_price1);
          Flight f2 = new Flight(fid2, result_dayOfMonth, result_carrierId2, result_flightNum2, result_originCity2, result_destCity2, at2, result_capacity2, result_price2);
          itins.add(new Itinerary(f1, f2));
        }
      }

    } catch (SQLException e) {
      e.printStackTrace();
      return "Failed to search\n";
    }

    if (itins.size() == 0) return "No flights match your selection\n";

    Collections.sort(itins);
    int ID = 0;
    for (Itinerary itinerary : itins) {
      // Header with itinerary id info
      sb.append("Itinerary ").append(ID++).append(": ").append(itinerary.numFlights).append(" flight(s), ").append(itinerary.time).append(" minutes\n");
      sb.append(itinerary.toString());
    }
  
    return sb.toString();
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_book(int itineraryId) {
    // TODO: YOUR CODE HERE
    if (this.currentUser == null) return "Cannot book reservations, not logged in\n";

    if (itins.size() <= itineraryId) return "No such itinerary " + itineraryId + "\n";

    Itinerary itinerary = itins.get(itineraryId);

    Set<Integer> bookedDays = new HashSet<>();
    int RID = 1;

    try {
      conn.setAutoCommit(false);
      ResultSet res;
    
      // get number of seats for current flight
      int seatsBookedF1 = 0;
      int seatsBookedF2 = 0;

      // error here!
      bookGetSeatsF1Statement.clearParameters();  
      bookGetSeatsF1Statement.setInt(1, itinerary.f1.fid);
      res = bookGetSeatsF1Statement.executeQuery(); // might want to keep check some stuff here
      while (res.next()) {
        seatsBookedF1 += res.getInt(1);
      } 
      if (itinerary.numFlights > 1) {
        bookGetSeatsF2Statement.clearParameters();  
        bookGetSeatsF2Statement.setInt(1, itinerary.f2.fid);
        res = bookGetSeatsF2Statement.executeQuery(); // might want to keep check some stuff here
        while (res.next()) {
          seatsBookedF2 += res.getInt(1);
        } 
      }

      if (itinerary.f1.capacity < seatsBookedF1 + 1 || ( itinerary.numFlights > 1 && itinerary.f2.capacity < seatsBookedF2 + 1)) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "Booking failed\n";
      }

      bookListReservationStatement.clearParameters();  
      bookListReservationStatement.setString(1, this.currentUser);
      
      res = bookListReservationStatement.executeQuery(); // might want to keep check some stuff here
      while (res.next()) {
        bookedDays.add(res.getInt(1));
      } 
      if (bookedDays.contains(itinerary.f1.dayOfMonth)) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "You cannot book two flights in the same day\n";
      }

      // get RID
      bookGetRIDStatement.clearParameters();
      res = bookGetRIDStatement.executeQuery();
      if (res.next())RID += res.getInt(1);

      // add reservation
      bookInsertRStatement.clearParameters();  
      bookInsertRStatement.setInt(1, RID);
      bookInsertRStatement.setString(2, currentUser);
      bookInsertRStatement.setInt(3, 0);
      if (bookInsertRStatement.executeUpdate() == 0) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "Booking failed\n";
      }

      // add reservation info
      bookInsertRInfoStatement.clearParameters();  
      bookInsertRInfoStatement.setInt(1, RID);
      bookInsertRInfoStatement.setInt(2, itinerary.f1.fid);
      if (itinerary.numFlights > 1) {
        bookInsertRInfoStatement.setInt(3, itinerary.f2.fid);
      } else {
        bookInsertRInfoStatement.setNull(3, java.sql.Types.INTEGER);
      }
    
      if(bookInsertRInfoStatement.executeUpdate() == 0) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "Booking failed\n";
      }
      
      conn.commit(); // Commit our query executions (make them permanent)
      conn.setAutoCommit(true); // End the transaction
      
    } catch (Exception e) {

      if (e instanceof SQLException && isDeadlock((SQLException)e)) {
        try {
            conn.rollback(); // Manually roll back the transaction
            conn.setAutoCommit(true); // Reset auto-commit mode
            return transaction_book(itineraryId);
        } catch (SQLException ex) {}
      }
      e.printStackTrace();
      return "Booking failed in catch\n";
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
      payGetBalanceStatement.setString(1, currentUser);
      ResultSet res = payGetBalanceStatement.executeQuery(); // might want to keep check some stuff here
      if (res.next()) balance += res.getInt(1);


      payGetInfoStatement.clearParameters();
      payGetInfoStatement.setString(1, currentUser);
      payGetInfoStatement.setInt(2, reservationId);
      res = payGetInfoStatement.executeQuery();
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
      payUpdateBalanceStatement.clearParameters();;
      payUpdateBalanceStatement.setInt(1, balance - totalPrice);
      payUpdateBalanceStatement.setString(2, currentUser);
      if (payUpdateBalanceStatement.executeUpdate() == 0) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "Failed to pay for reservation " + reservationId + "\n";
      }

      payUpdatePaidStatement.clearParameters();;
      payUpdatePaidStatement.setInt(1, reservationId);
      if (payUpdatePaidStatement.executeUpdate() == 0) {
        conn.rollback(); // Roll back any query executions in transaction thus far
        conn.setAutoCommit(true); // End the transaction
        return "Failed to pay for reservation " + reservationId + "\n";
      }

      conn.commit(); // Commit our query executions (make them permanent)
      conn.setAutoCommit(true); // End the transaction
      
    } catch (SQLException e) {
      if (e instanceof SQLException && isDeadlock((SQLException)e)) {
        try {
            conn.rollback(); // Manually roll back the transaction
            conn.setAutoCommit(true); // Reset auto-commit mode
            return transaction_pay(reservationId);
        } catch (SQLException ex) {}
      }
      e.printStackTrace();
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
      reserveGetReservationsStatement.setString(1, currentUser);
      ResultSet res = reserveGetReservationsStatement.executeQuery(); // might want to keep check some stuff here
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
      }
      
      conn.commit(); // Commit our query executions (make them permanent)
      conn.setAutoCommit(true); // End the transaction
      
    } catch (SQLException e) {
      if (e instanceof SQLException && isDeadlock((SQLException)e)) {
        try {
            conn.rollback(); // Manually roll back the transaction
            conn.setAutoCommit(true); // Reset auto-commit mode
            return transaction_reservations();
        } catch (SQLException ex) {}
      }
      e.printStackTrace();
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
          + " Capacity: " + capacity + " Price: " + price + "\n";
    }
  }

  public class Itinerary implements Comparable<Itinerary> {
    private int numFlights;
    private Flight f1;
    private Flight f2;
    private  int time;

    // Constructor
    public Itinerary(Flight f1) {
      this.f1 = f1;
      this.numFlights = 1;
      this.time = f1.time;
    }

    public Itinerary(Flight f1, Flight f2) {
      this.f1 = f1;
      this.f2 = f2;
      this.numFlights = 2;
      this.time = f1.time + f2.time;
    }

    // Implement compareTo method for sorting
    @Override
    public int compareTo(Itinerary other) {
        // Sort by actual time (assuming resultTime1 is the actual time)
        if (this.time != other.time) {
            return Integer.compare(this.time, other.time);
        } else if (this.f1.fid != other.f1.fid) {
            return Integer.compare(this.f1.fid, other.f1.fid);
        } else {
            if (this.f2 == null) {
              return -1;
            }
            if (other.f2 == null) {
              return 1;
            }
            return Integer.compare(this.f2.fid, other.f2.fid);
        }
    }

    // toString method for easy printing
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // Content
        sb.append(this.f1.toString());
        if (this.numFlights == 2) {              
            sb.append(this.f2.toString());
        }
        return sb.toString();
    }
  }
}
