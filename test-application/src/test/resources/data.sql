INSERT INTO tracker(id, imei, phone_number)
VALUES (1, '355234055650192', '+37257063997'),
       (2, '355026070842667', '+3197011460885'),
       (3, '355026070834532', '+3197011405848'),
       (4, '355026070840380', '37257591012'),
       (5, '358021082591268', '37257591222');
-- Advance H2 identity sequence past the explicitly-inserted IDs so that
-- subsequent auto-generated saves do not collide (H2 2.x does not
-- auto-advance the sequence for OVERRIDING USER VALUE inserts).
ALTER TABLE tracker ALTER COLUMN id RESTART WITH 100;
