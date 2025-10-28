-- Recria/garante alunos sempre que o app subir
DELETE FROM RESERVA;
DELETE FROM ALUNO;

INSERT INTO ALUNO (id, nome, email, senha_hash, semestre, tipo_trabalho)
VALUES (1, 'Fulano TFG',    'fulano@fau.br',   'abc123', 9, 'TFG');

INSERT INTO ALUNO (id, nome, email, senha_hash, semestre, tipo_trabalho)
VALUES (2, 'Ciclano Regular','ciclano@fau.br', 'abc123', 5, 'REGULAR');
