-- Add all your SQL setup statements here. 

-- When we test your submission, you can assume that the following base
-- tables have been created and loaded with data.  However, before testing
-- your own code, you will need to create and populate them on your
-- Postgres instance
--
-- Do not alter the following tables' contents or schema in your code.
-- create table FLIGHTS(fid int primary key, 
--         month_id int REFERENCES MONTHS,        -- 1-12
--         day_of_month int,    -- 1-31 
--         day_of_week_id int REFERENCES WEEKDAYS,  -- 1-7, 1 = Monday, 2 = Tuesday, etc
--         carrier_id varchar(7) REFERENCES CARRIERS, 
--         flight_num int,
--         origin_city varchar(34), 
--         origin_state varchar(47), 
--         dest_city varchar(34), 
--         dest_state varchar(46), 
--         departure_delay int, -- in mins
--         taxi_out int,        -- in mins
--         arrival_delay int,   -- in mins
--         canceled int,        -- 1 means canceled
--         actual_time int,     -- in mins
--         distance int,        -- in miles
--         capacity int, 
--         price int            -- in $             
--         );

-- create table CARRIERS(cid varchar(7) primary key,
--          name varchar(83));

-- create table MONTHS(mid int primary key,
--        month varchar(9));	

-- create table WEEKDAYS(did int primary key,
--          day_of_week varchar(9));


create table USERS_gcohen3(username text primary key,
                password bytea,
                balance int
);

create table RESERVATIONS_gcohen3(
        rid text primary key,
        username text REFERENCES USERS_gcohen3,
        paid int
);

create table RESERVATION_INFO_gcohen3(
        rid text REFERENCES RESERVATIONS_gcohen3,
        fid int REFERENCES FLIGHTS
);

