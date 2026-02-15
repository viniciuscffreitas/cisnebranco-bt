-- Service types: 40% commission standard, 50% for tesoura
INSERT INTO service_types (code, name, commission_rate) VALUES
    ('BANHO',           'Banho',                  0.40),
    ('TOSA_MAQUINA_L4', 'Tosa Máquina Lâmina 4',  0.40),
    ('TOSA_MAQUINA_L7', 'Tosa Máquina Lâmina 7',  0.40),
    ('TOSA_MAQUINA_L10','Tosa Máquina Lâmina 10', 0.40),
    ('TOSA_TESOURA',    'Tosa Tesoura',            0.50),
    ('HIDRATACAO',      'Hidratação',              0.40),
    ('DESEMBOLO',       'Desembolo',               0.40);

-- Common breeds
INSERT INTO breeds (name, species) VALUES
    ('SRD',                'DOG'),
    ('Poodle',             'DOG'),
    ('Shih Tzu',           'DOG'),
    ('Yorkshire',          'DOG'),
    ('Maltês',             'DOG'),
    ('Lhasa Apso',         'DOG'),
    ('Golden Retriever',   'DOG'),
    ('Labrador',           'DOG'),
    ('Bulldog Francês',    'DOG'),
    ('Spitz Alemão',       'DOG'),
    ('Schnauzer',          'DOG'),
    ('Cocker Spaniel',     'DOG'),
    ('Border Collie',      'DOG'),
    ('Pastor Alemão',      'DOG'),
    ('Bichon Frisé',       'DOG'),
    ('SRD',                'CAT'),
    ('Persa',              'CAT'),
    ('Siamês',             'CAT'),
    ('Maine Coon',         'CAT'),
    ('Angorá',             'CAT');
