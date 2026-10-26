CREATE TABLE system_configuration (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_configuration (config_key, config_value, description) VALUES
('pagtesouro.notification_domains', '.gov.br,.edu.br', 'Domínios aceitos para urlNotificacao da STN'),
('pagtesouro.url_notificacao', 'https://pagamentos.ifnmg.edu.br/payments/webhook', 'URL de callback de notificação oficial do IFNMG'),
('order.abandoned_days', '10', 'Dias para cancelamento automático de pedidos abandonados'),
('payment.expiration_days', '2', 'Dias de validade para vencimento das guias geradas no PagTesouro'),
('pagtesouro.token', '', 'Token de autenticação Bearer emitido pela STN (usa o token do ambiente se vazio)'),
('auth.reset_token_expiration_minutes', '15', 'Tempo de validade em minutos do token de recuperação de senha'),
('app.frontend.reset_password_url', 'http://localhost:3000/reset-password', 'URL base do front-end para o link de redefinição de senha'),
('institution.name', 'IFNMG • Campus Salinas', 'Nome oficial da instituição e campus exibido no portal'),
('institution.portal_name', 'IFNMG.EXPRESS', 'Sigla de identificação do portal na barra de navegação'),
('institution.portal_subtitle', 'Portal Institucional de Pagamentos & Tíquetes', 'Subtítulo institucional exibido no cabeçalho'),
('institution.hero_title', 'Arrecadação Pública Digital & Tíquetes Acadêmicos', 'Título principal do banner da vitrine pública'),
('institution.hero_description', 'Adquira refeições para o refeitório institucional ou efetue a quitação de taxas com liquidação em tempo real via PIX, Cartão ou GRU diretamente pelo PagTesouro.', 'Texto descritivo do banner da vitrine pública'),
('institution.guiche_subtitle', 'Campus Salinas • Refeitório Universitário & Biblioteca Central', 'Subtítulo operacional exibido no Terminal de Guichê POS'),
('institution.address', 'Fazenda Desterro, Rodovia Salinas / Taiobeiras, Km 02 • CEP: 39560-000 • Salinas - MG', 'Endereço físico institucional exibido no rodapé'),
('system.version', 'v3.0', 'Versão atual do sistema exibida no cabeçalho'),
('ui.product_preset_colors', '#10B981,#F59E0B,#3B82F6,#EF4444,#8B5CF6,#EC4899', 'Paleta de cores hexadecimais sugeridas para os cards de produtos')
ON CONFLICT (config_key) DO NOTHING;
