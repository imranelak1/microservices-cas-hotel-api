CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TABLE client(id BIGSERIAL PRIMARY KEY,nom VARCHAR(100) NOT NULL,prenom VARCHAR(100) NOT NULL,email VARCHAR(200) UNIQUE NOT NULL,telephone VARCHAR(30));
CREATE TABLE chambre(id BIGSERIAL PRIMARY KEY,type VARCHAR(50) NOT NULL,prix NUMERIC(10,2) NOT NULL CHECK(prix>0),disponible BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE reservation(id BIGSERIAL PRIMARY KEY,client_id BIGINT NOT NULL REFERENCES client(id),chambre_id BIGINT NOT NULL REFERENCES chambre(id),date_debut DATE NOT NULL,date_fin DATE NOT NULL,preferences TEXT NOT NULL DEFAULT '',CHECK(date_fin>date_debut),CHECK(length(preferences)<=102400),EXCLUDE USING gist(chambre_id WITH =,daterange(date_debut,date_fin,'[)') WITH &&));
CREATE OR REPLACE FUNCTION check_available_room() RETURNS TRIGGER AS $$
BEGIN
  IF NOT EXISTS(SELECT 1 FROM chambre WHERE id=NEW.chambre_id AND disponible) THEN RAISE EXCEPTION 'Chambre indisponible' USING ERRCODE='23514'; END IF;
  RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER available_room BEFORE INSERT OR UPDATE ON reservation FOR EACH ROW EXECUTE FUNCTION check_available_room();
INSERT INTO client(nom,prenom,email,telephone) SELECT 'Client '||n,'Prénom '||n,'client'||n||'@example.org','060000'||lpad(n::text,4,'0') FROM generate_series(1,100)n;
INSERT INTO chambre(type,prix,disponible) SELECT CASE WHEN n%2=0 THEN 'DOUBLE' ELSE 'SIMPLE' END,300+n%10*50,TRUE FROM generate_series(1,1000)n;
INSERT INTO reservation(client_id,chambre_id,date_debut,date_fin,preferences) VALUES(1,1,'2030-01-01','2030-01-03','Étage calme');
