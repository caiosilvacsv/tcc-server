CREATE TABLE payments(
    id UUID PRIMARY KEY,
    pagtesouro_payment_id VARCHAR(100) UNIQUE,
    reference VARCHAR(50),
    competence INTEGER,
    expired_at DATE,
    amount INTEGER NOT NULL, -- Valor total consolidado em centavos
    status VARCHAR(30) NOT NULL, -- Status local (ex: 'PENDING', 'COMPLETED')
    active BOOLEAN DEFAULT TRUE,
    
    -- Novos campos para compatibilidade estrita com a API do PagTesouro (Auditoria & Conciliação)
    payment_method VARCHAR(20), -- PIX, CARTAO_CREDITO, SALDO_CARTEIRA, BOLETO
    psp_name VARCHAR(50), -- Nome do Banco Processador (ex: Simulador PSP, Banco do Brasil)
    psp_transaction_id VARCHAR(50), -- ID único gerado pelo banco para a transação
    
    -- Desmembramento de valores em centavos (Conformidade Fiscal e Biblioteca)
    principal_amount INTEGER NOT NULL DEFAULT 0,
    discount_amount INTEGER DEFAULT 0,
    deductions_amount INTEGER DEFAULT 0,
    fine_amount INTEGER DEFAULT 0, -- Permite calcular multas de atraso
    interest_amount INTEGER DEFAULT 0, -- Permite calcular juros de mora
    additions_amount INTEGER DEFAULT 0,
    
    -- Dados de quem efetuou o pagamento físico (pode diferir do estudante cadastrado)
    contributor_cpf_cnpj VARCHAR(14),
    contributor_name VARCHAR(45),
    
    -- Configurações de expiração do Pix e sessões transient
    pix_expiration_hours SMALLINT,
    next_url VARCHAR(2048),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP WITH TIME ZONE
);