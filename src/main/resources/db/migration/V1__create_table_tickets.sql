CREATE TABLE tickets(
    id BIGINT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    image TEXT,
    price BIGINT NOT NULL,
    active BOOL,
    code_service VARCHAR(20),
    
);
