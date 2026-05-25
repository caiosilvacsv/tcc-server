CREATE TABLE products(
    id UUID PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    image TEXT,
    price INTEGER NOT NULL,
    active BOOLEAN DEFAULT FALSE,
    code_service VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);
