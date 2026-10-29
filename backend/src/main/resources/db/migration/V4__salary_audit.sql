ALTER TABLE compensation_history
    ADD COLUMN previous_amount NUMERIC(19, 2),
    ADD COLUMN previous_currency VARCHAR(3),
    ADD COLUMN actor VARCHAR(255),
    ADD CONSTRAINT chk_history_previous CHECK (
        (previous_amount IS NULL AND previous_currency IS NULL) OR
        (previous_amount > 0 AND previous_currency ~ '^[A-Z]{3}$'
            AND previous_amount IS NOT NULL AND previous_currency IS NOT NULL)
    );

CREATE INDEX idx_history_employee_chronology
    ON compensation_history (employee_id, created_at DESC, id DESC);

-- Existing rows keep their unknown actor/previous values. Audit is append-only,
-- including when application code accidentally attempts a repository mutation.
CREATE FUNCTION reject_compensation_history_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Compensation history is immutable' USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER compensation_history_immutable
    BEFORE UPDATE OR DELETE ON compensation_history
    FOR EACH ROW EXECUTE FUNCTION reject_compensation_history_mutation();
