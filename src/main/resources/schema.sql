create table "user"(
    username varchar(50),
    id serial primary key,
    role varchar(20)
);



CREATE TABLE boards (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    owner_id INTEGER REFERENCES "user"(id) ON DELETE CASCADE
);



CREATE TABLE boxes (
    id SERIAL PRIMARY KEY,
    board_id INTEGER REFERENCES boards(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES  "user"(id) on DELETE CASCADE ,
    title TEXT,
    description TEXT,
    pos_x INTEGER,
    pos_y INTEGER,
    center POINT ,
    boxWidth INTEGER
);




CREATE TABLE box_connections (
    id SERIAL PRIMARY KEY,
    from_box_id INTEGER REFERENCES boxes(id) ON DELETE CASCADE,
    to_box_id INTEGER REFERENCES boxes(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES  "user"(id) on DELETE CASCADE ,
    fromBoxCenter POINT ,
    targetBoxCenter POINT
    );



CREATE TABLE board_shares (
    board_id INTEGER REFERENCES boards(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES "user"(id) ON DELETE CASCADE,
    PRIMARY KEY (board_id, user_id)
);
