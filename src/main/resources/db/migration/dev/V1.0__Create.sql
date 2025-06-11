
CREATE TYPE statusType AS ENUM ('PENDING', 'PAID', 'OVERDUE');

-- Erstelle die Tabelle im TABLESPACE 'invoicespace'
CREATE TABLE invoice (
                         id               UUID PRIMARY KEY USING INDEX TABLESPACE invoicespace,
                         version          integer NOT NULL DEFAULT 1,
                         amount          DECIMAL(10, 2),
                         status          text NOT NULL,
                         due_date         timestamp NOT NULL,
                         payments         text,
                         created          timestamp NOT NULL,
                         updated          timestamp NOT NULL,
                         account_id       UUID,
                         issued_by         UUID,
                         billed_to         UUID
) TABLESPACE invoicespace;

-- Optional: Falls du den Index auf user_id im invoicespace-Tablespace haben möchtest:
CREATE INDEX idx_invoice_issued_by ON invoice (issued_by) TABLESPACE invoicespace;
CREATE INDEX idx_invoice_billed_to ON invoice (billed_to) TABLESPACE invoicespace;