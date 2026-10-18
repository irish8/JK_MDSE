# J&K MDSE Store (999) POS — ERD

```mermaid
erDiagram
    USERS ||--o{ SALES : processes
    SALES ||--|{ SALE_ITEMS : contains
    PRODUCTS o|--o{ SALE_ITEMS : referenced_by

    USERS {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar full_name
        enum role
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }
    PRODUCTS {
        bigint id PK
        varchar name
        decimal price
        varchar category
        int stock_qty
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }
    SALES {
        bigint id PK
        varchar transaction_no UK
        bigint user_id FK
        decimal subtotal
        decimal discount
        decimal total
        decimal amount_paid
        decimal change_due
        timestamp created_at
    }
    SALE_ITEMS {
        bigint id PK
        bigint sale_id FK
        bigint product_id FK "nullable; retained as snapshot after product removal"
        varchar product_name
        int quantity
        decimal unit_price
        decimal line_total
    }
```

`sale_items` retains the product name and price as sold, so printed receipts remain accurate if the product is later edited. Deleting a catalog item is implemented as a soft delete; if it is physically removed from the database, its historical item reference becomes null.
