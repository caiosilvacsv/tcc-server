CREATE TABLE payment_items(
    payment_id UUID NOT NULL,
    order_item_id UUID NOT NULL,

    PRIMARY KEY (payment_id, order_item_id),
    
    CONSTRAINT fk_pivot_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT fk_pivot_orders FOREIGN KEY (order_item_id) REFERENCES order_item(id) ON DELETE CASCADE
);