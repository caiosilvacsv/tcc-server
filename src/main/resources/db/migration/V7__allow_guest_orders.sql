-- Migration V7: Suporte a pedidos anônimos/convidados (guest)
-- Torna o relacionamento com users opcional para permitir compras diretas sem conta
ALTER TABLE orders ALTER COLUMN user_id DROP NOT NULL;

-- Registra dados cadastrais de quem comprou de forma avulsa
ALTER TABLE orders ADD COLUMN guest_cpf VARCHAR(14);
ALTER TABLE orders ADD COLUMN guest_name VARCHAR(45);

-- Índice para acelerar a busca de pedidos por CPF de visitantes no balcão e na consulta pública
CREATE INDEX idx_orders_guest_cpf ON orders(guest_cpf);
