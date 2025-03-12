-- Create CUSTOMER table
CREATE TABLE CUSTOMER (
    customer_id INT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    age INT,
    email VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL
);

-- Create PRODUCT table
CREATE TABLE PRODUCT (
    product_code VARCHAR(20) PRIMARY KEY,
    cost DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL
);

-- Create CUSTOMER_TRANSACTION table with JSON/BSON support
CREATE TABLE CUSTOMER_TRANSACTION (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_time TIMESTAMP NOT NULL,
    customer_id INT NOT NULL,
    product_code VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    data_format VARCHAR(10),
    json_data CLOB,
    bson_data BLOB,
    processed_time TIMESTAMP,
    status VARCHAR(20),
    FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id),
    FOREIGN KEY (product_code) REFERENCES PRODUCT(product_code)
);

-- Create index on transaction_time for better query performance
CREATE INDEX idx_transaction_time ON CUSTOMER_TRANSACTION(transaction_time);

-- Insert sample customer data
INSERT INTO CUSTOMER (customer_id, first_name, last_name, age, email, location) VALUES
(10001, 'Tony', 'Stark', 34, 'tony.stark@gmail.com', 'Australia'),
(10002, 'Bruce', 'Banner', 54, 'bruce.banner@gmail.com', 'US'),
(10003, 'Steve', 'Rogers', 43, 'steve.rogers@hotmail.com', 'Australia'),
(10004, 'Wanda', 'Maximoff', 67, 'wanda.maximoff@gmail.com', 'US'),
(10005, 'Natasha', 'Romanoff', 57, 'natasha.romanoff@gmail.com', 'Canada');

-- Insert sample product data
INSERT INTO PRODUCT (product_code, cost, status) VALUES
('PRODUCT_001', 50, 'Active'),
('PRODUCT_002', 100, 'Inactive'),
('PRODUCT_003', 200, 'Active'),
('PRODUCT_004', 10, 'Inactive'),
('PRODUCT_005', 500, 'Active');


-- JSON Transactions for Australian Customers
-- Transaction 1
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, json_data, processed_time, status)
VALUES ('2025-03-15 09:00:00', 10001, 'PRODUCT_001', 2, 'JSON',
'{"payment_method":"credit_card","card_type":"VISA","transaction_reference":"TX-001-2025","currency":"AUD","promocode":"SUMMER25","channel":"mobile"}',
'2025-03-15 09:01:00', 'PROCESSED');

-- Transaction 2
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, json_data, processed_time, status)
VALUES ('2025-03-15 10:30:00', 10003, 'PRODUCT_003', 1, 'JSON',
'{"payment_method":"paypal","transaction_reference":"TX-002-2025","currency":"AUD","promocode":"WELCOME10","channel":"web","device_id":"MAC-10003"}',
'2025-03-15 10:31:00', 'PROCESSED');

-- Transaction 3
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, json_data, processed_time, status)
VALUES ('2025-03-16 14:15:00', 10001, 'PRODUCT_005', 1, 'JSON',
'{"payment_method":"apple_pay","transaction_reference":"TX-003-2025","currency":"AUD","promocode":"","channel":"mobile","device_id":"IPHONE-12345"}',
'2025-03-16 14:16:00', 'PROCESSED');

-- Transaction 4
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, json_data, processed_time, status)
VALUES ('2025-03-17 11:45:00', 10003, 'PRODUCT_001', 3, 'JSON',
'{"payment_method":"credit_card","card_type":"MASTERCARD","transaction_reference":"TX-004-2025","currency":"AUD","promocode":"","channel":"tablet","loyalty_points":150}',
'2025-03-17 11:46:00', 'PROCESSED');

-- Transaction 5
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, json_data, processed_time, status)
VALUES ('2025-03-18 16:20:00', 10001, 'PRODUCT_003', 2, 'JSON',
'{"payment_method":"google_pay","transaction_reference":"TX-005-2025","currency":"AUD","promocode":"FLASH20","channel":"mobile","device_id":"ANDROID-54321","loyalty_points":200}',
'2025-03-18 16:21:00', 'PROCESSED');

-- Transaction 6
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, json_data, processed_time, status)
VALUES ('2025-03-19 09:10:00', 10003, 'PRODUCT_005', 1, 'JSON',
'{"payment_method":"debit_card","card_type":"VISA_DEBIT","transaction_reference":"TX-006-2025","currency":"AUD","promocode":"","channel":"web","is_gift":true}',
'2025-03-19 09:11:00', 'PROCESSED');

-- Transaction 7
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, json_data, processed_time, status)
VALUES ('2025-03-20 13:50:00', 10001, 'PRODUCT_001', 4, 'JSON',
'{"payment_method":"credit_card","card_type":"AMEX","transaction_reference":"TX-007-2025","currency":"AUD","promocode":"AMEXOFFER","channel":"mobile","loyalty_points":400,"is_corporate":true}',
'2025-03-20 13:51:00', 'PROCESSED');

-- Transaction 8
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, json_data, processed_time, status)
VALUES ('2025-03-21 15:30:00', 10003, 'PRODUCT_003', 2, 'JSON',
'{"payment_method":"paypal","transaction_reference":"TX-008-2025","currency":"AUD","promocode":"","channel":"web","is_gift":false,"loyalty_points":250}',
'2025-03-21 15:31:00', 'PROCESSED');

-- Transaction 9
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, json_data, processed_time, status)
VALUES ('2025-03-22 10:20:00', 10001, 'PRODUCT_005', 1, 'JSON',
'{"payment_method":"bank_transfer","transaction_reference":"TX-009-2025","currency":"AUD","promocode":"BANK10","channel":"web","is_rush_delivery":true}',
'2025-03-22 10:21:00', 'PROCESSED');

-- Transaction 10
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, json_data, processed_time, status)
VALUES ('2025-03-23 17:45:00', 10003, 'PRODUCT_001', 2, 'JSON',
'{"payment_method":"credit_card","card_type":"VISA","transaction_reference":"TX-010-2025","currency":"AUD","promocode":"ENDOFMONTH","channel":"mobile","device_id":"IPHONE-67890","loyalty_points":175}',
'2025-03-23 17:46:00', 'PROCESSED');


-- BSON Transactions for Australian Customers
-- Transaction 1
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, bson_data, processed_time, status)
VALUES ('2025-03-24 08:15:00', 10001, 'PRODUCT_003', 2, 'BSON',
CAST('{"payment_method":"credit_card","card_type":"VISA","transaction_reference":"BT-001-2025","currency":"AUD","promocode":"AUTUMN10","channel":"mobile","nested_data":{"device":"iPhone","os_version":"15.4"}}' AS BINARY),
'2025-03-24 08:16:00', 'PROCESSED');

-- Transaction 2
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, bson_data, processed_time, status)
VALUES ('2025-03-24 12:30:00', 10003, 'PRODUCT_005', 1, 'BSON',
CAST('{"payment_method":"apple_pay","transaction_reference":"BT-002-2025","currency":"AUD","promocode":"","channel":"tablet","device_id":"IPAD-12345","loyalty_points":220}' AS BINARY),
'2025-03-24 12:31:00', 'PROCESSED');

-- Transaction 3
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, bson_data, processed_time, status)
VALUES ('2025-03-25 11:45:00', 10001, 'PRODUCT_001', 3, 'BSON',
CAST('{"payment_method":"paypal","transaction_reference":"BT-003-2025","currency":"AUD","promocode":"PAYPAL15","channel":"web","nested_data":{"browser":"Chrome","version":"99.0"}}' AS BINARY),
'2025-03-25 11:46:00', 'PROCESSED');

-- Transaction 4
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, bson_data, processed_time, status)
VALUES ('2025-03-26 14:20:00', 10003, 'PRODUCT_003', 2, 'BSON',
CAST('{"payment_method":"debit_card","card_type":"MASTERCARD_DEBIT","transaction_reference":"BT-004-2025","currency":"AUD","promocode":"","channel":"mobile","device_id":"SAMSUNG-54321","loyalty_points":175}' AS BINARY),
'2025-03-26 14:21:00', 'PROCESSED');

-- Transaction 5
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, bson_data, processed_time, status)
VALUES ('2025-03-27 09:30:00', 10001, 'PRODUCT_005', 1, 'BSON',
CAST('{"payment_method":"google_pay","transaction_reference":"BT-005-2025","currency":"AUD","promocode":"GOOGLE25","channel":"mobile","device_id":"PIXEL-67890","is_gift":true,"gift_message":"Happy Birthday!"}' AS BINARY),
'2025-03-27 09:31:00', 'PROCESSED');

-- Transaction 6
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, bson_data, processed_time, status)
VALUES ('2025-03-28 16:15:00', 10003, 'PRODUCT_001', 4, 'BSON',
CAST('{"payment_method":"credit_card","card_type":"AMEX","transaction_reference":"BT-006-2025","currency":"AUD","promocode":"AMEX20","channel":"web","loyalty_points":400,"is_corporate":true,"department":"Sales"}' AS BINARY),
'2025-03-28 16:16:00', 'PROCESSED');

-- Transaction 7
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, bson_data, processed_time, status)
VALUES ('2025-03-29 10:45:00', 10001, 'PRODUCT_003', 2, 'BSON',
CAST('{"payment_method":"bank_transfer","transaction_reference":"BT-007-2025","currency":"AUD","promocode":"","channel":"web","is_rush_delivery":true,"shipping_address":{"street":"123 Marvel St","city":"Sydney","state":"NSW"}}' AS BINARY),
'2025-03-29 10:46:00', 'PROCESSED');

-- Transaction 8
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, bson_data, processed_time, status)
VALUES ('2025-03-30 13:20:00', 10003, 'PRODUCT_005', 1, 'BSON',
CAST('{"payment_method":"apple_pay","transaction_reference":"BT-008-2025","currency":"AUD","promocode":"WEEKEND10","channel":"mobile","device_id":"IPHONE-54321","loyalty_points":150,"is_weekend_purchase":true}' AS BINARY),
'2025-03-30 13:21:00', 'PROCESSED');

-- Transaction 9
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, bson_data, processed_time, status)
VALUES ('2025-03-31 15:10:00', 10001, 'PRODUCT_001', 2, 'BSON',
CAST('{"payment_method":"credit_card","card_type":"VISA","transaction_reference":"BT-009-2025","currency":"AUD","promocode":"MONTHEND","channel":"tablet","device_id":"IPAD-67890","loyalty_points":200,"subscription_renewal":true}' AS BINARY),
'2025-03-31 15:11:00', 'PROCESSED');

-- Transaction 10
INSERT INTO CUSTOMER_TRANSACTION (transaction_time, customer_id, product_code, quantity, data_format, bson_data, processed_time, status)
VALUES ('2025-04-01 09:00:00', 10003, 'PRODUCT_003', 3, 'BSON',
CAST('{"payment_method":"paypal","transaction_reference":"BT-010-2025","currency":"AUD","promocode":"NEWMONTH","channel":"web","loyalty_points":300,"is_first_purchase_of_month":true,"nested_data":{"browser":"Firefox","version":"98.0"}}' AS BINARY),
'2025-04-01 09:01:00', 'PROCESSED');