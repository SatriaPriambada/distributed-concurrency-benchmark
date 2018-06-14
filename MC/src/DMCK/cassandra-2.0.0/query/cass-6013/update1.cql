UPDATE test.tests SET owner = 'user_2' WHERE name = 'testing' IF  owner = 'user_1';
