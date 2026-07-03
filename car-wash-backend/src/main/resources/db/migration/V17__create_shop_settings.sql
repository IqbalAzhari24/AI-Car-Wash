-- Key-value store for public shop info shown on the landing page
-- (contact details, operating hours). Managed by the owner; read publicly.
CREATE TABLE shop_settings (
    setting_key   VARCHAR(50) PRIMARY KEY,
    setting_value TEXT        NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP   NOT NULL DEFAULT NOW()
);

INSERT INTO shop_settings (setting_key, setting_value) VALUES
    ('contact_phone', '+6016 922 0499'),
    ('contact_email', 'hello@aicarwash.my'),
    ('hours_mon_thu', '8:00 AM – 6:00 PM'),
    ('hours_fri',     '8:00 AM – 12:00 PM, 2:30 PM – 6:00 PM'),
    ('hours_sat',     '8:00 AM – 4:00 PM'),
    ('hours_sun',     'Closed');
