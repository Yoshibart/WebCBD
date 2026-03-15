ALTER TABLE `carts`
MODIFY COLUMN `user_id` BIGINT NULL;

UPDATE `carts`
SET `user_id` = NULL;

DELETE FROM `users`
WHERE `id` <> 1;

INSERT INTO `users` (id, username, email, password, role)
VALUES
    (1, 'admin', 'admin@example.com', '{noop}admin', 'ADMIN')
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    email = VALUES(email),
    password = VALUES(password),
    role = VALUES(role);

INSERT IGNORE INTO `products` (id, name, category, price, description)
VALUES
    (1, 'Wireless Mouse', 'Accessories', 29.99, 'Ergonomic wireless mouse with USB receiver'),
    (2, 'Mechanical Keyboard', 'Accessories', 89.99, 'Backlit mechanical keyboard with blue switches'),
    (3, 'Gaming Monitor', 'Electronics', 249.99, '27-inch monitor with 165Hz refresh rate'),
    (4, 'USB-C Hub', 'Accessories', 49.99, 'Multi-port USB-C hub with HDMI and Ethernet'),
    (5, 'Laptop Stand', 'Accessories', 39.99, 'Adjustable aluminum stand for laptops and tablets'),
    (6, 'Bluetooth Earbuds', 'Accessories', 59.99, 'Compact wireless earbuds with charging case');
