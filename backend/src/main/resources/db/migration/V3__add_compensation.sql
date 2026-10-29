-- Compensation management: current salary and immutable history per employee
CREATE TABLE compensation (
    id               BIGSERIAL PRIMARY KEY,
    employee_id      BIGINT          NOT NULL,
    currency         VARCHAR(3)      NOT NULL,
    amount           NUMERIC(19, 2)  NOT NULL,
    version          BIGINT          NOT NULL DEFAULT 0,
    effective_from   DATE            NOT NULL,
    created_at       TIMESTAMPTZ     NOT NULL,
    updated_at       TIMESTAMPTZ     NOT NULL,
    CONSTRAINT fk_compensation_employee FOREIGN KEY (employee_id) REFERENCES employee (id),
    CONSTRAINT uq_compensation_employee UNIQUE (employee_id),
    CONSTRAINT chk_compensation_amount CHECK (amount > 0),
    CONSTRAINT chk_compensation_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_compensation_employee ON compensation (employee_id);
CREATE INDEX idx_compensation_currency ON compensation (currency);

-- Immutable salary history: every change creates a new row, never updated or deleted
CREATE TABLE compensation_history (
    id               BIGSERIAL PRIMARY KEY,
    employee_id      BIGINT          NOT NULL,
    currency         VARCHAR(3)      NOT NULL,
    amount           NUMERIC(19, 2)  NOT NULL,
    change_reason    VARCHAR(500)    NOT NULL,
    effective_from   DATE            NOT NULL,
    created_at       TIMESTAMPTZ     NOT NULL,
    CONSTRAINT fk_comp_history_employee FOREIGN KEY (employee_id) REFERENCES employee (id),
    CONSTRAINT chk_history_amount CHECK (amount > 0),
    CONSTRAINT chk_history_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_comp_history_employee ON compensation_history (employee_id);
CREATE INDEX idx_comp_history_effective ON compensation_history (effective_from);
CREATE INDEX idx_comp_history_created ON compensation_history (created_at);

-- Demo-only FX snapshot for reporting currency conversion
-- Labeled to indicate it is not live, not updated, and reflects a specific moment
CREATE TABLE fx_rate_snapshot (
    id               BIGSERIAL PRIMARY KEY,
    from_currency    VARCHAR(3)      NOT NULL,
    to_currency      VARCHAR(3)      NOT NULL,
    rate             NUMERIC(19, 6)  NOT NULL,
    snapshot_date    DATE            NOT NULL,
    created_at       TIMESTAMPTZ     NOT NULL,
    CONSTRAINT uq_fx_rate UNIQUE (from_currency, to_currency),
    CONSTRAINT chk_fx_rate CHECK (rate > 0),
    CONSTRAINT chk_fx_currencies CHECK (from_currency ~ '^[A-Z]{3}$' AND to_currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_fx_rate_currencies ON fx_rate_snapshot (from_currency, to_currency);
