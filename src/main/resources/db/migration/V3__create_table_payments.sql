CREATE SEQUENCE payment_ref_seq START WITH 100000;

CREATE TABLE payments(
    id UUID PRIMARY KEY,
    reference_number BIGINT DEFAULT nextval('payment_ref_seq') UNIQUE NOT NULL,
    pagtesouro_payment_id VARCHAR(100) UNIQUE,
    reference VARCHAR(50),
    competence INTEGER,
    expired_at DATE,
    amount INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    payment_method VARCHAR(20),
    psp_name VARCHAR(50),
    psp_transaction_id VARCHAR(50),
    principal_amount INTEGER NOT NULL DEFAULT 0,
    discount_amount INTEGER DEFAULT 0,
    deductions_amount INTEGER DEFAULT 0,
    fine_amount INTEGER DEFAULT 0,
    interest_amount INTEGER DEFAULT 0,
    additions_amount INTEGER DEFAULT 0,
    contributor_cpf_cnpj VARCHAR(14),
    contributor_name VARCHAR(45),
    pix_expiration_hours SMALLINT,
    next_url VARCHAR(2048),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP WITH TIME ZONE
);