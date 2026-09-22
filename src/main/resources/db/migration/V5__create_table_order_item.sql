CREATE TABLE order_item(
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity SMALLINT NOT NULL,
    total_amount INTEGER,
    paid_at TIMESTAMP WITH TIME ZONE,
    exchanged_at TIMESTAMP WITH TIME ZONE,
    exchanged_by UUID,
    status VARCHAR(30) NOT NULL,

    CONSTRAINT fk_pivot_orders FOREIGN KEY (order_id) REFERENCES orders(id)ON DELETE CASCADE,
    CONSTRAINT fk_pivot_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_exchanged_by FOREIGN KEY (exchanged_by) REFERENCES users(id) ON DELETE SET NULL
);