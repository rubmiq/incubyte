CREATE SEQUENCE employee_code_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE employee (
    id            BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(20)  NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    country       VARCHAR(2)   NOT NULL,
    department    VARCHAR(50)  NOT NULL,
    job_title     VARCHAR(100) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    hired_on      DATE         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_employee_code UNIQUE (employee_code),
    CONSTRAINT uq_employee_email UNIQUE (email),
    CONSTRAINT chk_employee_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_employee_status ON employee (status);
CREATE INDEX idx_employee_country ON employee (country);
CREATE INDEX idx_employee_department ON employee (department);
CREATE INDEX idx_employee_last_name ON employee (last_name);
