INSERT INTO mock_entity (id, name, description)
VALUES
       (1, 'test-1', 'description-1'),
       (2, 'test-2', 'description-2'),
       (3, 'test-3', 'description-3'),
       (4, 'test-4', 'description-4'),
       (5, 'test-5', 'description-5');
-- Advance H2 identity sequence past the explicitly-inserted IDs so that
-- subsequent auto-generated saves do not collide (H2 2.x does not
-- auto-advance the sequence for OVERRIDING USER VALUE inserts).
ALTER TABLE mock_entity ALTER COLUMN id RESTART WITH 100;



