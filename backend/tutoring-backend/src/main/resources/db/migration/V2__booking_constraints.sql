CREATE EXTENSION IF NOT EXISTS btree_gist;

-- у репетитора нет пересекающихся занятых интервалов
ALTER TABLE booking ADD CONSTRAINT no_tutor_overlap
    EXCLUDE USING gist (tutor_id WITH =, tstzrange(start_time, end_time) WITH &&)
    WHERE (status IN ('PENDING','CONFIRMED','COMPLETED'));

-- ученик не может быть на двух занятиях одновременно
ALTER TABLE booking ADD CONSTRAINT no_student_overlap
    EXCLUDE USING gist (student_id WITH =, tstzrange(start_time, end_time) WITH &&)
    WHERE (status IN ('PENDING','CONFIRMED','COMPLETED'));

-- одно вводное занятие на пару ученик-репетитор
CREATE UNIQUE INDEX uq_intro_per_pair ON booking (student_id, tutor_id)
    WHERE intro AND status IN ('PENDING','CONFIRMED','COMPLETED');