CREATE ROLE invoice_db_user LOGIN PASSWORD 'Omnixys12.05.2025';
CREATE DATABASE invoice_db;
GRANT ALL ON DATABASE invoice_db TO invoice_db_user;
CREATE TABLESPACE invoicespace OWNER invoice_db_user LOCATION '/var/lib/postgresql/tablespace/invoice';

-- psql --dbname=bitnami_keycloak --username=bn_keycloak --file=/sql/invoice/create-db-invoice.sql
-- psql --dbname=invoice_db --username=invoice_db_user --file=/sql/invoice/create-schema-invoice.sql