CREATE INDEX idx_booking_student     ON booking (student_id, start_time);
CREATE INDEX idx_booking_status_time ON booking (status, start_time);
CREATE INDEX idx_working_hours_tutor ON working_hours (tutor_id, day_of_week);
CREATE INDEX idx_time_off_tutor      ON time_off (tutor_id, start_at);